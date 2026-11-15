package com.acme.hrassistant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "helpdesk_tickets")
public class HelpdeskTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "pending_action_id", length = 36)
    private String pendingActionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected HelpdeskTicket() {
    }

    public HelpdeskTicket(Long employeeId, String subject, String description, String pendingActionId) {
        this.employeeId = employeeId;
        this.subject = subject;
        this.description = description;
        this.status = "OPEN";
        this.pendingActionId = pendingActionId;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getSubject() {
        return subject;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPendingActionId() {
        return pendingActionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
