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
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "policy_documents")
public class PolicyDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 255)
    private String sourceFileName;

    @Column(nullable = false, length = 100)
    private String contentType;

    /**
     * Full uploaded/seeded document text — stored in Postgres as TEXT.
     * Do not use {@code @Lob}: on PostgreSQL that maps to OID large-objects and stores
     * numeric OIDs instead of the markdown body.
     */
    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PolicyStatus status;

    @Column
    private LocalDate effectiveDate;

    @Column(length = 20)
    private String grade;

    @Column(length = 40)
    private String location;

    @Column(nullable = false)
    private Long uploadedBy;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(length = 1000)
    private String lastError;

    protected PolicyDocument() {
    }

    public PolicyDocument(
            String name,
            String sourceFileName,
            String contentType,
            String content,
            PolicyStatus status,
            LocalDate effectiveDate,
            String grade,
            String location,
            Long uploadedBy,
            Instant createdAt) {
        this.name = name;
        this.sourceFileName = sourceFileName;
        this.contentType = contentType;
        this.content = content;
        this.status = status;
        this.effectiveDate = effectiveDate;
        this.grade = grade;
        this.location = location;
        this.uploadedBy = uploadedBy;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public PolicyStatus getStatus() {
        return status;
    }

    public void setStatus(PolicyStatus status) {
        this.status = status;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public String getGrade() {
        return grade;
    }

    public String getLocation() {
        return location;
    }

    public Long getUploadedBy() {
        return uploadedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}
