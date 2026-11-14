package com.acme.hrassistant.service;

import com.acme.hrassistant.domain.ChatConversation;
import com.acme.hrassistant.domain.ChatConversationRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatConversationService {

    private final ChatConversationRepository chatConversationRepository;

    public ChatConversationService(ChatConversationRepository chatConversationRepository) {
        this.chatConversationRepository = chatConversationRepository;
    }

    /**
     * Resolves a conversation owned by the employee. Creates a new one when id is blank or unknown.
     * Foreign conversations are rejected (do not leak another user's memory).
     */
    @Transactional
    public String resolveConversationId(Long employeeId, String requestedId) {
        Instant now = Instant.now();
        if (requestedId == null || requestedId.isBlank()) {
            return create(employeeId, now).getId();
        }

        return chatConversationRepository.findById(requestedId)
                .map(existing -> {
                    if (!existing.getEmployeeId().equals(employeeId)) {
                        throw new IllegalStateException("Conversation does not belong to the authenticated employee");
                    }
                    existing.touch(now);
                    return existing.getId();
                })
                .orElseGet(() -> {
                    ChatConversation created = new ChatConversation(requestedId, employeeId, now);
                    return chatConversationRepository.save(created).getId();
                });
    }

    private ChatConversation create(Long employeeId, Instant now) {
        return chatConversationRepository.save(new ChatConversation(UUID.randomUUID().toString(), employeeId, now));
    }
}
