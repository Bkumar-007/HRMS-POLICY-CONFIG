import { apiClient } from "@utils/api";
import { routesMapping, Route } from "@utils/apiRoutes";
import { commonRequest } from "@utils/helpers";

export async function listPoliciesAction(token) {
  return commonRequest(
    apiClient,
    Route(routesMapping.policies.List),
    {},
    "GET",
  )(token);
}

export async function getPolicyAction(token, id) {
  return commonRequest(
    apiClient,
    Route(`${routesMapping.policies.Get}/${id}`),
    {},
    "GET",
  )(token);
}

export async function uploadPolicyAction(token, { name, file, effectiveDate, grade, location }) {
  const form = new FormData();
  form.append("name", name);
  form.append("file", file);
  if (effectiveDate) {
    form.append("effectiveDate", effectiveDate);
  }
  if (grade) {
    form.append("grade", grade);
  }
  if (location) {
    form.append("location", location);
  }

  return commonRequest(
    apiClient,
    Route(routesMapping.policies.Upload),
    form,
    "POST",
  )(token);
}

export async function deletePolicyAction(token, id) {
  return commonRequest(
    apiClient,
    Route(`${routesMapping.policies.Delete}/${id}`),
    {},
    "DELETE",
  )(token);
}

export async function disablePolicyAction(token, id) {
  return commonRequest(
    apiClient,
    Route(`${routesMapping.policies.Disable}/${id}/disable`),
    {},
    "POST",
  )(token);
}

export async function enablePolicyAction(token, id) {
  return commonRequest(
    apiClient,
    Route(`${routesMapping.policies.Enable}/${id}/enable`),
    {},
    "POST",
  )(token);
}

export async function ingestPolicyAction(token, id) {
  return commonRequest(
    apiClient,
    Route(`${routesMapping.policies.Ingest}/${id}/ingest`),
    {},
    "POST",
  )(token);
}

export async function ingestAllPoliciesAction(token) {
  return commonRequest(
    apiClient,
    Route(routesMapping.policies.IngestAll),
    {},
    "POST",
  )(token);
}
