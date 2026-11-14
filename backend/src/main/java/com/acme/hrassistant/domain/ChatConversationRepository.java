package com.acme.hrassistant.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, String> {
    Optional<ChatConversation> findByIdAndEmployeeId(String id, Long employeeId);
}
