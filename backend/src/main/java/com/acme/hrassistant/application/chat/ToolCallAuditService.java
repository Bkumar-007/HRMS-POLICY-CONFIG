package com.acme.hrassistant.application.chat;

import com.acme.hrassistant.domain.ToolCallLog;
import com.acme.hrassistant.domain.ToolCallLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ToolCallAuditService {

    private static final Logger log = LoggerFactory.getLogger(ToolCallAuditService.class);

    private final ToolCallLogRepository toolCallLogRepository;
    private final ObjectMapper objectMapper;

    public ToolCallAuditService(ToolCallLogRepository toolCallLogRepository, ObjectMapper objectMapper) {
        this.toolCallLogRepository = toolCallLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(Long employeeId, String conversationId, String toolName, Map<String, ?> input, Object output) {
        String inputJson = toJson(input);
        String outputJson = truncate(toJson(output), 4000);
        toolCallLogRepository.save(new ToolCallLog(
                employeeId,
                conversationId,
                toolName,
                truncate(inputJson, 2000),
                outputJson,
                Instant.now()));
        log.info("tool={} employeeId={} conversationId={} input={}", toolName, employeeId, conversationId, inputJson);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }
}
