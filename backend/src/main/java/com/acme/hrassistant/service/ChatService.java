package com.acme.hrassistant.service;

import com.acme.hrassistant.security.EmployeePrincipal;
import com.acme.hrassistant.web.dto.ChatRequest;
import com.acme.hrassistant.web.dto.ChatResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Phase 1: simple Gemini chat (no tools / RAG yet).
 * Later phases add tool calling, citations, and pending actions.
 */
@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You are Acme Corp's HR Policy Assistant (demo).
                        Be concise and helpful. Policies and leave actions will be wired in later phases.
                        If asked about leave balance or applying leave, say Phase 1 is chat-only
                        and tools will arrive in Phase 2.
                        Always address the employee by their display name when greeting.
                        """)
                .build();
    }

    public ChatResponse chat(EmployeePrincipal user, ChatRequest request) {
        String conversationId = request.conversationId() == null || request.conversationId().isBlank()
                ? UUID.randomUUID().toString()
                : request.conversationId();

        String reply = chatClient.prompt()
                .user(u -> u.text("""
                        Employee: %s (id=%s, grade=%s, location=%s, role=%s)
                        Message: %s
                        """.formatted(
                        user.getDisplayName(),
                        user.getEmployeeId(),
                        user.getGrade(),
                        user.getLocation(),
                        user.getRole(),
                        request.message())))
                .call()
                .content();

        return new ChatResponse(
                conversationId,
                reply == null ? "" : reply,
                List.of(),
                List.of());
    }
}