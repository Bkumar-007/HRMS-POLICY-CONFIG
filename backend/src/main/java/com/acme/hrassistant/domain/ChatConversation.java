package com.acme.hrassistant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Ownership row for a conversationId. Message bodies live in Spring AI JDBC chat memory.
 */
@Entity
@Table(name = "chat_conversations")
public class ChatConversation {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ChatConversation() {
    }

    public ChatConversation(String id, Long employeeId, Instant createdAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void touch(Instant at) {
        this.updatedAt = at;
    }
}
