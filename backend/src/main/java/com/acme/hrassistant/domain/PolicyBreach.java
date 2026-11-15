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
@Table(name = "policy_breaches")
public class PolicyBreach {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private Long reportedById;

    @Column(length = 36)
    private String policyDocumentId;

    @Column(nullable = false, length = 200)
    private String policyName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PolicyBreachSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PolicyBreachStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;

    protected PolicyBreach() {
    }

    public PolicyBreach(
            Long employeeId,
            Long reportedById,
            String policyDocumentId,
            String policyName,
            String notes,
            PolicyBreachSeverity severity) {
        this.employeeId = employeeId;
        this.reportedById = reportedById;
        this.policyDocumentId = policyDocumentId;
        this.policyName = policyName;
        this.notes = notes;
        this.severity = severity;
        this.status = PolicyBreachStatus.OPEN;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public Long getReportedById() {
        return reportedById;
    }

    public String getPolicyDocumentId() {
        return policyDocumentId;
    }

    public String getPolicyName() {
        return policyName;
    }

    public String getNotes() {
        return notes;
    }

    public PolicyBreachSeverity getSeverity() {
        return severity;
    }

    public PolicyBreachStatus getStatus() {
        return status;
    }

    public void setStatus(PolicyBreachStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
