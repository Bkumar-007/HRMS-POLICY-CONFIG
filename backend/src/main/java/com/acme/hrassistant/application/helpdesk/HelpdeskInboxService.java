package com.acme.hrassistant.application.helpdesk;

import com.acme.hrassistant.application.people.PeopleService;
import com.acme.hrassistant.domain.Employee;
import com.acme.hrassistant.domain.EmployeeRepository;
import com.acme.hrassistant.domain.HelpdeskTicket;
import com.acme.hrassistant.domain.HelpdeskTicketRepository;
import com.acme.hrassistant.domain.Role;
import com.acme.hrassistant.security.EmployeePrincipal;
import com.acme.hrassistant.web.dto.HelpdeskTicketDto;
import com.acme.hrassistant.web.dto.UpdateTicketStatusRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HelpdeskInboxService {

    private static final Set<String> ALLOWED =
            Set.of("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED");

    private final HelpdeskTicketRepository helpdeskTicketRepository;
    private final EmployeeRepository employeeRepository;
    private final PeopleService peopleService;

    public HelpdeskInboxService(
            HelpdeskTicketRepository helpdeskTicketRepository,
            EmployeeRepository employeeRepository,
            PeopleService peopleService) {
        this.helpdeskTicketRepository = helpdeskTicketRepository;
        this.employeeRepository = employeeRepository;
        this.peopleService = peopleService;
    }

    @Transactional(readOnly = true)
    public List<HelpdeskTicketDto> list(EmployeePrincipal viewer) {
        peopleService.requireManagerOrHr(viewer);
        List<HelpdeskTicket> tickets;
        if (viewer.getRole() == Role.HR_ADMIN) {
            tickets = helpdeskTicketRepository.findAllByOrderByCreatedAtDesc();
        } else {
            List<Long> teamIds = employeeRepository
                    .findByManagerIdOrderByDisplayNameAsc(viewer.getEmployeeId())
                    .stream()
                    .map(Employee::getId)
                    .toList();
            tickets = teamIds.isEmpty()
                    ? List.of()
                    : helpdeskTicketRepository.findByEmployeeIdInOrderByCreatedAtDesc(teamIds);
        }
        Map<Long, String> names = nameMap();
        return tickets.stream().map(t -> toDto(t, names)).toList();
    }

    @Transactional
    public HelpdeskTicketDto updateStatus(
            EmployeePrincipal actor, Long ticketId, UpdateTicketStatusRequest request) {
        if (actor.getRole() != Role.HR_ADMIN) {
            throw new AccessDeniedException("HR admins only");
        }
        HelpdeskTicket ticket = helpdeskTicketRepository
                .findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found."));
        String status = request.status().trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED.contains(status)) {
            throw new IllegalArgumentException("Status must be OPEN, IN_PROGRESS, RESOLVED, or CLOSED.");
        }
        ticket.setStatus(status);
        return toDto(helpdeskTicketRepository.save(ticket), nameMap());
    }

    private HelpdeskTicketDto toDto(HelpdeskTicket ticket, Map<Long, String> names) {
        return new HelpdeskTicketDto(
                ticket.getId(),
                ticket.getEmployeeId(),
                names.getOrDefault(ticket.getEmployeeId(), "Unknown"),
                ticket.getSubject(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getCreatedAt());
    }

    private Map<Long, String> nameMap() {
        return employeeRepository.findAll().stream()
                .collect(Collectors.toMap(Employee::getId, Employee::getDisplayName, (a, b) -> a));
    }
}
