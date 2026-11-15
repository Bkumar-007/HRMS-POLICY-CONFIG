package com.acme.hrassistant.domain;

public enum LeaveRequestStatus {
    /** @deprecated legacy value — treat like PENDING_APPROVAL */
    SUBMITTED,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    CANCELLED
}
