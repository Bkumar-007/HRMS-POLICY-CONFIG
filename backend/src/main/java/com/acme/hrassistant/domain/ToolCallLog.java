package com.acme.hrassistant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "tool_call_logs")
public class ToolCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false, length = 36)
    private String conversationId;

    @Column(nullable = false, length = 80)
    private String toolName;

    @Column(nullable = false, length = 2000)
    private String inputJson;

    @Column(nullable = false, length = 4000)
    private String outputJson;

    @Column(nullable = false)
    private Instant createdAt;

    protected ToolCallLog() {
    }

    public ToolCallLog(
            Long employeeId,
            String conversationId,
            String toolName,
            String inputJson,
            String outputJson,
            Instant createdAt) {
        this.employeeId = employeeId;
        this.conversationId = conversationId;
        this.toolName = toolName;
        this.inputJson = inputJson;
        this.outputJson = outputJson;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getToolName() {
        return toolName;
    }

    public String getInputJson() {
        return inputJson;
    }

    public String getOutputJson() {
        return outputJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
