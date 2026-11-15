package com.acme.hrassistant.config.ratelimit;

import com.acme.hrassistant.config.AppProperties;
import com.acme.hrassistant.security.EmployeePrincipal;
import com.acme.hrassistant.web.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final AppProperties appProperties;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(AppProperties appProperties, MeterRegistry meterRegistry, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        LimitSpec spec = resolve(request);
        if (spec != null) {
            Bucket bucket = buckets.computeIfAbsent(spec.key(), k -> newBucket(spec.capacity()));
            if (!bucket.tryConsume(1)) {
                meterRegistry.counter("hr.ratelimit.rejected", "route", spec.route()).increment();
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(
                        response.getOutputStream(),
                        ApiResponse.fail("Rate limit exceeded for " + spec.route() + ". Please try again shortly."));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private LimitSpec resolve(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (HttpMethod.POST.matches(method) && path.equals("/api/auth/login")) {
            return new LimitSpec(
                    "login:" + clientIp(request),
                    appProperties.getRatelimit().getLoginPerMinute(),
                    "login");
        }
        if (HttpMethod.POST.matches(method)
                && (path.equals("/api/auth/forgot-password")
                        || path.equals("/api/auth/verify-otp")
                        || path.equals("/api/auth/reset-password"))) {
            return new LimitSpec(
                    "password-reset:" + clientIp(request),
                    appProperties.getRatelimit().getPasswordResetPerMinute(),
                    "password-reset");
        }
        if (HttpMethod.POST.matches(method) && path.equals("/api/chat")) {
            return new LimitSpec(
                    "chat:" + identityOrIp(request),
                    appProperties.getRatelimit().getChatPerMinute(),
                    "chat");
        }
        if (isPolicyWrite(method, path)) {
            return new LimitSpec(
                    "policy-write:" + identityOrIp(request),
                    appProperties.getRatelimit().getPolicyWritePerMinute(),
                    "policy-write");
        }
        return null;
    }

    private boolean isPolicyWrite(String method, String path) {
        if (!path.startsWith("/api/policies") && !path.startsWith("/api/admin/policies")) {
            return false;
        }
        return HttpMethod.POST.matches(method) || HttpMethod.DELETE.matches(method);
    }

    private String identityOrIp(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof EmployeePrincipal principal) {
            return "emp:" + principal.getEmployeeId();
        }
        return "ip:" + clientIp(request);
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private static Bucket newBucket(int perMinute) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(perMinute)
                .refillGreedy(perMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private record LimitSpec(String key, int capacity, String route) {
    }
}
