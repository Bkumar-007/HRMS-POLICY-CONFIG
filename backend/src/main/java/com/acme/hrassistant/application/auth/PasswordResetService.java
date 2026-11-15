package com.acme.hrassistant.application.auth;

import com.acme.hrassistant.config.AppProperties;
import com.acme.hrassistant.domain.Employee;
import com.acme.hrassistant.domain.EmployeeRepository;
import com.acme.hrassistant.domain.OtpChannel;
import com.acme.hrassistant.domain.PasswordResetOtp;
import com.acme.hrassistant.domain.PasswordResetOtpRepository;
import com.acme.hrassistant.web.dto.ForgotPasswordRequest;
import com.acme.hrassistant.web.dto.ForgotPasswordResponse;
import com.acme.hrassistant.web.dto.ResetPasswordRequest;
import com.acme.hrassistant.web.dto.VerifyOtpRequest;
import com.acme.hrassistant.web.dto.VerifyOtpResponse;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PasswordResetService {

    /** Fixed demo OTP when Twilio/SMTP are not configured. */
    public static final String DEMO_OTP = "123456";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmployeeRepository employeeRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpDeliveryService otpDeliveryService;
    private final AppProperties appProperties;

    public PasswordResetService(
            EmployeeRepository employeeRepository,
            PasswordResetOtpRepository otpRepository,
            PasswordEncoder passwordEncoder,
            OtpDeliveryService otpDeliveryService,
            AppProperties appProperties) {
        this.employeeRepository = employeeRepository;
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpDeliveryService = otpDeliveryService;
        this.appProperties = appProperties;
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String username = normalizeUsername(request.username());
        OtpChannel channel = parseChannel(request.channel());

        Optional<Employee> employeeOpt = employeeRepository.findByUsername(username);
        // Always return a generic success shape to avoid username enumeration,
        // but only send when the user and channel destination exist.
        if (employeeOpt.isEmpty()) {
            return new ForgotPasswordResponse(
                    true,
                    "If an account exists, a one-time code has been sent.",
                    channel.name(),
                    null);
        }

        Employee employee = employeeOpt.get();
        String destination = channel == OtpChannel.EMAIL ? employee.getEmail() : employee.getPhone();
        if (!StringUtils.hasText(destination)) {
            throw new IllegalStateException(
                    channel == OtpChannel.EMAIL
                            ? "No email is on file for this account."
                            : "No mobile number is on file for this account.");
        }

        otpRepository.consumeAllActiveForUsername(username);

        boolean realChannel = otpDeliveryService.canDeliver(channel);
        String otp = realChannel ? generateOtp() : DEMO_OTP;
        Instant expiresAt = Instant.now().plusSeconds(appProperties.getOtp().getTtlMinutes() * 60L);
        PasswordResetOtp entity = new PasswordResetOtp(
                employee.getId(),
                username,
                channel,
                passwordEncoder.encode(otp),
                expiresAt);
        otpRepository.save(entity);

        otpDeliveryService.send(channel, destination, otp, employee.getDisplayName());

        String masked = channel == OtpChannel.EMAIL
                ? OtpDeliveryService.maskEmail(destination)
                : OtpDeliveryService.maskPhone(destination);

        return new ForgotPasswordResponse(
                true,
                "A one-time code has been sent to " + masked + ".",
                channel.name(),
                masked);
    }

    @Transactional
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        String username = normalizeUsername(request.username());
        String otp = request.otp() == null ? "" : request.otp().trim();

        PasswordResetOtp active = otpRepository
                .findFirstByUsernameAndConsumedFalseOrderByCreatedAtDesc(username)
                .orElseThrow(() -> new IllegalStateException("No active reset request found. Request a new code."));

        if (active.getExpiresAt().isBefore(Instant.now())) {
            active.setConsumed(true);
            throw new IllegalStateException("This code has expired. Request a new one.");
        }

        if (active.getAttemptCount() >= appProperties.getOtp().getMaxAttempts()) {
            active.setConsumed(true);
            throw new IllegalStateException("Too many invalid attempts. Request a new code.");
        }

        if (!passwordEncoder.matches(otp, active.getOtpHash())) {
            active.incrementAttemptCount();
            throw new IllegalArgumentException("Invalid verification code.");
        }

        String resetToken = generateResetToken();
        active.setResetTokenHash(passwordEncoder.encode(resetToken));
        active.setResetTokenExpiresAt(
                Instant.now().plusSeconds(appProperties.getOtp().getResetTokenTtlMinutes() * 60L));

        return new VerifyOtpResponse(true, "Code verified. Set a new password.", resetToken);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String username = normalizeUsername(request.username());
        String resetToken = request.resetToken() == null ? "" : request.resetToken().trim();
        String newPassword = request.newPassword() == null ? "" : request.newPassword();

        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }

        PasswordResetOtp active = otpRepository
                .findFirstByUsernameAndConsumedFalseOrderByCreatedAtDesc(username)
                .orElseThrow(() -> new IllegalStateException("No active reset request found. Start again."));

        if (!StringUtils.hasText(active.getResetTokenHash())
                || active.getResetTokenExpiresAt() == null
                || active.getResetTokenExpiresAt().isBefore(Instant.now())) {
            throw new IllegalStateException("Reset session expired. Verify the OTP again.");
        }

        if (!passwordEncoder.matches(resetToken, active.getResetTokenHash())) {
            throw new IllegalArgumentException("Invalid reset token.");
        }

        Employee employee = employeeRepository
                .findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Account not found."));
        employee.setPasswordHash(passwordEncoder.encode(newPassword));
        employeeRepository.save(employee);

        active.setConsumed(true);
        otpRepository.consumeAllActiveForUsername(username);
    }

    private static String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    private static OtpChannel parseChannel(String channel) {
        try {
            return OtpChannel.valueOf(channel == null ? "" : channel.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Channel must be EMAIL or SMS.");
        }
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, appProperties.getOtp().getLength());
        int value = RANDOM.nextInt(bound / 10, bound);
        return String.valueOf(value);
    }

    private static String generateResetToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
