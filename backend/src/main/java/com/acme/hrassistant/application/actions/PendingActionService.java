package com.acme.hrassistant.application.actions;

import com.acme.hrassistant.application.leave.LeaveRules.ApplyLeaveRequest;
import com.acme.hrassistant.config.AppProperties;
import com.acme.hrassistant.domain.HelpdeskTicket;
import com.acme.hrassistant.domain.LeaveRequest;
import com.acme.hrassistant.domain.PendingAction;
import com.acme.hrassistant.domain.PendingActionRepository;
import com.acme.hrassistant.domain.PendingActionStatus;
import com.acme.hrassistant.domain.PendingActionType;
import com.acme.hrassistant.mockhr.HelpdeskService;
import com.acme.hrassistant.mockhr.LeaveApplicationService;
import com.acme.hrassistant.web.dto.ActionResultDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PendingActionService {

    private final PendingActionRepository pendingActionRepository;
    private final LeaveApplicationService leaveApplicationService;
    private final HelpdeskService helpdeskService;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    public PendingActionService(
            PendingActionRepository pendingActionRepository,
            LeaveApplicationService leaveApplicationService,
            HelpdeskService helpdeskService,
            ObjectMapper objectMapper,
            AppProperties appProperties) {
        this.pendingActionRepository = pendingActionRepository;
        this.leaveApplicationService = leaveApplicationService;
        this.helpdeskService = helpdeskService;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
    }

    @Transactional
    public PendingAction createLeavePending(
            Long employeeId,
            String conversationId,
            String employeeLocation,
            ApplyLeaveRequest request) {
        var validation = leaveApplicationService.validateApply(
                employeeId, employeeLocation, request, LocalDate.now());
        if (!validation.valid()) {
            throw new IllegalStateException(validation.message());
        }

        LeaveApplyPayload payload = new LeaveApplyPayload(
                request.leaveType(),
                request.startDate(),
                request.endDate(),
                request.days(),
                request.reason(),
                request.medicalCertificateProvided(),
                employeeLocation);
        String summary = "Apply " + request.leaveType() + " leave "
                + request.startDate() + " → " + request.endDate()
                + " (" + request.days() + " day(s))";

        PendingAction pending = PendingAction.create(
                employeeId,
                conversationId,
                PendingActionType.APPLY_LEAVE,
                summary,
                toJson(payload),
                Instant.now().plus(appProperties.getActions().getPendingTtlMinutes(), ChronoUnit.MINUTES));
        return pendingActionRepository.save(pending);
    }

    @Transactional
    public PendingAction createHelpdeskPending(
            Long employeeId,
            String conversationId,
            String subject,
            String description) {
        if (subject == null || subject.isBlank()) {
            throw new IllegalStateException("Ticket subject is required");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalStateException("Ticket description is required");
        }
        HelpdeskPayload payload = new HelpdeskPayload(subject.trim(), description.trim());
        String summary = "Raise helpdesk ticket: " + payload.subject();
        PendingAction pending = PendingAction.create(
                employeeId,
                conversationId,
                PendingActionType.RAISE_HELPDESK_TICKET,
                summary,
                toJson(payload),
                Instant.now().plus(appProperties.getActions().getPendingTtlMinutes(), ChronoUnit.MINUTES));
        return pendingActionRepository.save(pending);
    }

    @Transactional
    public ActionResultDto confirm(Long employeeId, String actionId) {
        PendingAction pending = loadOwned(employeeId, actionId);

        if (pending.getStatus() == PendingActionStatus.CONFIRMED) {
            return ActionResultDto.alreadyConfirmed(pending);
        }
        if (pending.getStatus() == PendingActionStatus.CANCELLED) {
            throw new IllegalStateException("Pending action was cancelled");
        }
        if (pending.getStatus() == PendingActionStatus.EXPIRED
                || pending.getExpiresAt().isBefore(Instant.now())) {
            if (pending.getStatus() == PendingActionStatus.PENDING) {
                pending.markExpired();
                pendingActionRepository.save(pending);
            }
            throw new IllegalStateException("Pending action has expired");
        }
        if (pending.getStatus() != PendingActionStatus.PENDING) {
            throw new IllegalStateException("Pending action is not confirmable: " + pending.getStatus());
        }

        return switch (pending.getType()) {
            case APPLY_LEAVE -> confirmLeave(pending);
            case RAISE_HELPDESK_TICKET -> confirmHelpdesk(pending);
        };
    }

    @Transactional
    public ActionResultDto cancel(Long employeeId, String actionId) {
        PendingAction pending = loadOwned(employeeId, actionId);
        if (pending.getStatus() == PendingActionStatus.CANCELLED) {
            return ActionResultDto.cancelled(pending);
        }
        if (pending.getStatus() == PendingActionStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel an already confirmed action");
        }
        if (pending.getStatus() == PendingActionStatus.EXPIRED
                || pending.getExpiresAt().isBefore(Instant.now())) {
            if (pending.getStatus() == PendingActionStatus.PENDING) {
                pending.markExpired();
                pendingActionRepository.save(pending);
            }
            throw new IllegalStateException("Pending action has expired");
        }
        pending.markCancelled();
        pendingActionRepository.save(pending);
        return ActionResultDto.cancelled(pending);
    }

    private ActionResultDto confirmLeave(PendingAction pending) {
        LeaveApplyPayload payload = fromJson(pending.getPayloadJson(), LeaveApplyPayload.class);
        ApplyLeaveRequest request = new ApplyLeaveRequest(
                payload.leaveType(),
                payload.startDate(),
                payload.endDate(),
                payload.days(),
                payload.reason(),
                payload.medicalCertificateProvided());

        // Idempotency: if leave already written for this pending id, treat as confirmed
        var existing = leaveApplicationService.findByPendingActionId(pending.getId());
        if (existing.isPresent()) {
            LeaveRequest lr = existing.get();
            if (pending.getStatus() != PendingActionStatus.CONFIRMED) {
                pending.markConfirmed(toJson(Map.of("leaveRequestId", lr.getId())));
                pendingActionRepository.save(pending);
            }
            return ActionResultDto.confirmed(
                    pending, "Leave already submitted for approval (id=" + lr.getId() + ")");
        }

        LeaveRequest leaveRequest = leaveApplicationService.submit(
                pending.getEmployeeId(),
                payload.employeeLocation(),
                request,
                pending.getId(),
                LocalDate.now());
        String result = toJson(Map.of(
                "leaveRequestId", leaveRequest.getId(),
                "leaveType", leaveRequest.getLeaveType().name(),
                "days", leaveRequest.getDays(),
                "status", leaveRequest.getStatus().name()));
        pending.markConfirmed(result);
        pendingActionRepository.save(pending);
        return ActionResultDto.confirmed(
                pending,
                "Leave submitted for manager approval: " + leaveRequest.getLeaveType()
                        + " " + leaveRequest.getStartDate() + " → " + leaveRequest.getEndDate()
                        + " (" + leaveRequest.getDays() + " day(s)). "
                        + "Your manager will approve or reject it from the Leave screen.");
    }

    private ActionResultDto confirmHelpdesk(PendingAction pending) {
        HelpdeskPayload payload = fromJson(pending.getPayloadJson(), HelpdeskPayload.class);
        var existing = helpdeskService.findByPendingActionId(pending.getId());
        if (existing.isPresent()) {
            HelpdeskTicket ticket = existing.get();
            if (pending.getStatus() != PendingActionStatus.CONFIRMED) {
                pending.markConfirmed(toJson(Map.of("ticketId", ticket.getId())));
                pendingActionRepository.save(pending);
            }
            return ActionResultDto.confirmed(pending, "Ticket already opened (id=" + ticket.getId() + ")");
        }
        HelpdeskTicket ticket = helpdeskService.openTicket(
                pending.getEmployeeId(),
                payload.subject(),
                payload.description(),
                pending.getId());
        pending.markConfirmed(toJson(Map.of("ticketId", ticket.getId())));
        pendingActionRepository.save(pending);
        return ActionResultDto.confirmed(pending, "Helpdesk ticket opened (id=" + ticket.getId() + ")");
    }

    private PendingAction loadOwned(Long employeeId, String actionId) {
        PendingAction pending = pendingActionRepository.findById(actionId)
                .orElseThrow(() -> new IllegalStateException("Pending action not found"));
        if (!pending.getEmployeeId().equals(employeeId)) {
            throw new AccessDeniedException("Pending action does not belong to the authenticated user");
        }
        return pending;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize payload", ex);
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse pending action payload", ex);
        }
    }
}
