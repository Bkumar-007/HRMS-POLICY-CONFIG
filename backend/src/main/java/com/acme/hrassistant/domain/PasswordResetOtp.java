package com.acme.hrassistant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "password_reset_otps")
public class PasswordResetOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false, length = 80)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OtpChannel channel;

    @Column(nullable = false, length = 100)
    private String otpHash;

    @Column(length = 100)
    private String resetTokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column
    private Instant resetTokenExpiresAt;

    @Column(nullable = false)
    private boolean consumed;

    @Column(nullable = false)
    private int attemptCount;

    @Column(nullable = false)
    private Instant createdAt;

    protected PasswordResetOtp() {
    }

    public PasswordResetOtp(
            Long employeeId,
            String username,
            OtpChannel channel,
            String otpHash,
            Instant expiresAt) {
        this.employeeId = employeeId;
        this.username = username;
        this.channel = channel;
        this.otpHash = otpHash;
        this.expiresAt = expiresAt;
        this.consumed = false;
        this.attemptCount = 0;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getUsername() {
        return username;
    }

    public OtpChannel getChannel() {
        return channel;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public String getResetTokenHash() {
        return resetTokenHash;
    }

    public void setResetTokenHash(String resetTokenHash) {
        this.resetTokenHash = resetTokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getResetTokenExpiresAt() {
        return resetTokenExpiresAt;
    }

    public void setResetTokenExpiresAt(Instant resetTokenExpiresAt) {
        this.resetTokenExpiresAt = resetTokenExpiresAt;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void setConsumed(boolean consumed) {
        this.consumed = consumed;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void incrementAttemptCount() {
        this.attemptCount += 1;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
