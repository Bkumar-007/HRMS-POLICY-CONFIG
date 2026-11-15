package com.acme.hrassistant.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HelpdeskTicketRepository extends JpaRepository<HelpdeskTicket, Long> {

    Optional<HelpdeskTicket> findByPendingActionId(String pendingActionId);

    List<HelpdeskTicket> findAllByOrderByCreatedAtDesc();

    List<HelpdeskTicket> findByEmployeeIdInOrderByCreatedAtDesc(List<Long> employeeIds);
}
