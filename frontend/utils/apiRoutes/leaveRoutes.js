export const Requests = "api/leave/requests";
export const Approve = (id) => `api/leave/requests/${id}/approve`;
export const Reject = (id) => `api/leave/requests/${id}/reject`;
export const Cancel = (id) => `api/leave/requests/${id}/cancel`;
