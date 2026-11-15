package com.acme.hrassistant.mockhr;

import com.acme.hrassistant.domain.HelpdeskTicket;
import com.acme.hrassistant.domain.HelpdeskTicketRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HelpdeskService {

    private final HelpdeskTicketRepository helpdeskTicketRepository;

    public HelpdeskService(HelpdeskTicketRepository helpdeskTicketRepository) {
        this.helpdeskTicketRepository = helpdeskTicketRepository;
    }

    @Transactional
    public HelpdeskTicket openTicket(Long employeeId, String subject, String description, String pendingActionId) {
        if (subject == null || subject.isBlank()) {
            throw new IllegalStateException("Ticket subject is required");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalStateException("Ticket description is required");
        }
        return helpdeskTicketRepository.save(new HelpdeskTicket(
                employeeId,
                subject.trim(),
                description.trim(),
                pendingActionId));
    }

    @Transactional(readOnly = true)
    public Optional<HelpdeskTicket> findByPendingActionId(String pendingActionId) {
        return helpdeskTicketRepository.findByPendingActionId(pendingActionId);
    }
}
