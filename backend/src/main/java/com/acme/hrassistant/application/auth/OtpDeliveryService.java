package com.acme.hrassistant.application.auth;

import com.acme.hrassistant.config.AppProperties;
import com.acme.hrassistant.domain.OtpChannel;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class OtpDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(OtpDeliveryService.class);
    private static final String FAST2SMS_URL = "https://www.fast2sms.com/dev/bulkV2";

    private final AppProperties appProperties;
    private final JavaMailSender mailSender;
    private final RestClient restClient;
    private boolean twilioReady;
    private boolean fast2smsReady;

    public OtpDeliveryService(AppProperties appProperties, JavaMailSender mailSender) {
        this.appProperties = appProperties;
        this.mailSender = mailSender;
        this.restClient = RestClient.create();
    }

    @PostConstruct
    void initProviders() {
        AppProperties.Twilio twilio = appProperties.getTwilio();
        if (StringUtils.hasText(twilio.getAccountSid())
                && StringUtils.hasText(twilio.getAuthToken())
                && StringUtils.hasText(twilio.getFromNumber())) {
            Twilio.init(twilio.getAccountSid(), twilio.getAuthToken());
            twilioReady = true;
            log.info("Twilio SMS delivery configured");
        }

        fast2smsReady = StringUtils.hasText(appProperties.getFast2sms().getApiKey());
        if (fast2smsReady) {
            log.info("Fast2SMS delivery configured (preferred for India SMS)");
        }

        if (!twilioReady && !fast2smsReady) {
            log.warn("No SMS provider configured — OTP SMS uses demo code 123456 until FAST2SMS_API_KEY or Twilio is set");
        }
    }

    public boolean canDeliver(OtpChannel channel) {
        if (channel == OtpChannel.SMS) {
            return fast2smsReady || twilioReady;
        }
        return StringUtils.hasText(appProperties.getMail().getFrom())
                && StringUtils.hasText(System.getenv().getOrDefault("SPRING_MAIL_PASSWORD", ""));
    }

    public OtpDeliveryResult send(OtpChannel channel, String destination, String otp, String displayName) {
        if (channel == OtpChannel.EMAIL) {
            return sendEmail(destination, otp, displayName);
        }
        return sendSms(destination, otp);
    }

    private OtpDeliveryResult sendEmail(String to, String otp, String displayName) {
        if (!StringUtils.hasText(appProperties.getMail().getFrom())) {
            return demoSkip("EMAIL", to);
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(appProperties.getMail().getFrom());
            message.setTo(to);
            message.setSubject("Recoil HR — password reset code");
            message.setText(
                    "Hi "
                            + (StringUtils.hasText(displayName) ? displayName : "there")
                            + ",\n\n"
                            + "Your one-time password reset code is: "
                            + otp
                            + "\n\n"
                            + "This code expires in "
                            + appProperties.getOtp().getTtlMinutes()
                            + " minutes. If you did not request a reset, ignore this email.\n\n"
                            + "— Recoil HR Portal");
            mailSender.send(message);
            log.info("Password reset OTP emailed to {}", maskEmail(to));
            return new OtpDeliveryResult(true, "email");
        } catch (Exception ex) {
            log.warn("Email OTP send failed ({}), using demo OTP", ex.getMessage());
            return demoSkip("EMAIL", to);
        }
    }

    private OtpDeliveryResult sendSms(String to, String otp) {
        String digits = toDigits(to);
        if (fast2smsReady) {
            try {
                sendViaFast2Sms(digits, otp);
                log.info("Password reset OTP SMS sent via Fast2SMS to {}", maskPhone(digits));
                return new OtpDeliveryResult(true, "fast2sms");
            } catch (Exception ex) {
                log.warn("Fast2SMS send failed ({}), trying Twilio/demo", ex.getMessage());
            }
        }
        if (twilioReady) {
            try {
                String e164 = toE164(to);
                Message.creator(
                                new PhoneNumber(e164),
                                new PhoneNumber(appProperties.getTwilio().getFromNumber()),
                                "Recoil HR reset code: "
                                        + otp
                                        + ". Valid for "
                                        + appProperties.getOtp().getTtlMinutes()
                                        + " minutes.")
                        .create();
                log.info("Password reset OTP SMS sent via Twilio to {}", maskPhone(e164));
                return new OtpDeliveryResult(true, "twilio");
            } catch (Exception ex) {
                log.warn("Twilio SMS send failed ({}), using demo OTP", ex.getMessage());
            }
        }
        return demoSkip("SMS", to);
    }

    private void sendViaFast2Sms(String tenDigitNumber, String otp) {
        AppProperties.Fast2sms cfg = appProperties.getFast2sms();
        String bodyText = "Recoil HR password reset code: "
                + otp
                + ". Valid for "
                + appProperties.getOtp().getTtlMinutes()
                + " minutes.";

        String form = "route="
                + url(cfg.getRoute())
                + "&sender_id="
                + url(cfg.getSenderId())
                + "&message="
                + url(bodyText)
                + "&language=english&flash=0&numbers="
                + url(tenDigitNumber);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient
                .post()
                .uri(URI.create(FAST2SMS_URL))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("authorization", cfg.getApiKey())
                .body(form)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("Empty Fast2SMS response");
        }
        Object returnValue = response.get("return");
        boolean ok = Boolean.TRUE.equals(returnValue)
                || "true".equalsIgnoreCase(String.valueOf(returnValue))
                || Integer.valueOf(1).equals(returnValue);
        if (!ok) {
            throw new IllegalStateException("Fast2SMS rejected request: " + response);
        }
    }

    private OtpDeliveryResult demoSkip(String channel, String destination) {
        if (!appProperties.getOtp().isDevFallback()) {
            throw new IllegalStateException(
                    channel.equals("SMS")
                            ? "SMS OTP is not configured. Set FAST2SMS_API_KEY (recommended) or Twilio credentials."
                            : "Email OTP is not configured. Set MAIL_FROM / spring.mail credentials.");
        }
        String masked = channel.equals("SMS") ? maskPhone(destination) : maskEmail(destination);
        log.info("Demo OTP mode [{}] for {} — use fixed code 123456", channel, masked);
        return new OtpDeliveryResult(false, "demo");
    }

    private static String url(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String toDigits(String phone) {
        String digits = phone == null ? "" : phone.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) {
            return digits.substring(2);
        }
        if (digits.length() == 10) {
            return digits;
        }
        throw new IllegalArgumentException("Enter a valid 10-digit Indian mobile number");
    }

    static String toE164(String phone) {
        String digits = toDigits(phone);
        return "+91" + digits;
    }

    static String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "***";
        }
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) {
            return "*".repeat(local.length()) + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }

    static String maskPhone(String phone) {
        String digits = phone == null ? "" : phone.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "****";
        }
        return "******" + digits.substring(digits.length() - 4);
    }
}
