package com.acme.hrassistant.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToolCallLogRepository extends JpaRepository<ToolCallLog, Long> {

    List<ToolCallLog> findByConversationIdOrderByIdAsc(String conversationId);
}
