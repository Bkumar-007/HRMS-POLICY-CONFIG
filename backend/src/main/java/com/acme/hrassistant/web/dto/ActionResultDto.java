package com.acme.hrassistant.web.dto;

import com.acme.hrassistant.domain.PendingAction;
import com.acme.hrassistant.domain.PendingActionStatus;

public record ActionResultDto(
        String id,
        String type,
        String status,
        String summary,
        String message) {

    public static ActionResultDto confirmed(PendingAction pending, String message) {
        return new ActionResultDto(
                pending.getId(),
                pending.getType().name(),
                PendingActionStatus.CONFIRMED.name(),
                pending.getSummary(),
                message);
    }

    public static ActionResultDto alreadyConfirmed(PendingAction pending) {
        return new ActionResultDto(
                pending.getId(),
                pending.getType().name(),
                PendingActionStatus.CONFIRMED.name(),
                pending.getSummary(),
                "Action was already confirmed");
    }

    public static ActionResultDto cancelled(PendingAction pending) {
        return new ActionResultDto(
                pending.getId(),
                pending.getType().name(),
                PendingActionStatus.CANCELLED.name(),
                pending.getSummary(),
                "Action cancelled");
    }
}
