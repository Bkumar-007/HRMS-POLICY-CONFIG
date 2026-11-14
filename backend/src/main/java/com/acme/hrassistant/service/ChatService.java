package com.acme.hrassistant.service;

import com.acme.hrassistant.agent.HrToolContextKeys;
import com.acme.hrassistant.security.EmployeePrincipal;
import com.acme.hrassistant.web.dto.ChatRequest;
import com.acme.hrassistant.web.dto.ChatResponse;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

/**
 * Phase 2: Gemini chat with read-only HR tools, JDBC conversation memory, and tool audit log.
 */
@Service
public class ChatService {

    private final ChatClient chatClient;
    private final ChatConversationService chatConversationService;

    public ChatService(ChatClient chatClient, ChatConversationService chatConversationService) {
        this.chatClient = chatClient;
        this.chatConversationService = chatConversationService;
    }

    public ChatResponse chat(EmployeePrincipal user, ChatRequest request) {
        String conversationId = chatConversationService.resolveConversationId(
                user.getEmployeeId(), request.conversationId());

        String reply = chatClient.prompt()
                .user(u -> u.text("""
                        Employee: %s (grade=%s, location=%s, role=%s)
                        Message: %s
                        """.formatted(
                        user.getDisplayName(),
                        user.getGrade(),
                        user.getLocation(),
                        user.getRole(),
                        request.message())))
                .toolContext(Map.of(
                        HrToolContextKeys.EMPLOYEE_ID, user.getEmployeeId(),
                        HrToolContextKeys.CONVERSATION_ID, conversationId,
                        HrToolContextKeys.EMPLOYEE_LOCATION, user.getLocation()))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        return new ChatResponse(
                conversationId,
                reply == null ? "" : reply,
                List.of(),
                List.of());
    }
}
