package com.acme.hrassistant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pending_actions")
public class PendingAction {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "conversation_id", length = 36)
    private String conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PendingActionType type;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PendingActionStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;

    protected PendingAction() {
    }

    public PendingAction(
            String id,
            Long employeeId,
            String conversationId,
            PendingActionType type,
            String summary,
            String payloadJson,
            Instant expiresAt,
            Instant createdAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.conversationId = conversationId;
        this.type = type;
        this.summary = summary;
        this.payloadJson = payloadJson;
        this.status = PendingActionStatus.PENDING;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public static PendingAction create(
            Long employeeId,
            String conversationId,
            PendingActionType type,
            String summary,
            String payloadJson,
            Instant expiresAt) {
        return new PendingAction(
                UUID.randomUUID().toString(),
                employeeId,
                conversationId,
                type,
                summary,
                payloadJson,
                expiresAt,
                Instant.now());
    }

    public String getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public PendingActionType getType() {
        return type;
    }

    public String getSummary() {
        return summary;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public PendingActionStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public String getResultJson() {
        return resultJson;
    }

    public void markConfirmed(String resultJson) {
        this.status = PendingActionStatus.CONFIRMED;
        this.resolvedAt = Instant.now();
        this.resultJson = resultJson;
    }

    public void markCancelled() {
        this.status = PendingActionStatus.CANCELLED;
        this.resolvedAt = Instant.now();
    }

    public void markExpired() {
        this.status = PendingActionStatus.EXPIRED;
        this.resolvedAt = Instant.now();
    }
}
