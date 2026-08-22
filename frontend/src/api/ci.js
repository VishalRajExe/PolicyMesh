import { apiClient } from "./client";

export const ciApi = {
  runCheck: (payload) => apiClient.post("/ci/check", payload).then((r) => r.data),
  getScan: (id) => apiClient.get(`/ci/scans/${id}`).then((r) => r.data),
};
