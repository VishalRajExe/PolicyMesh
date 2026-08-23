import { apiClient } from "./client";

export const ciApi = {
  runCheck: (payload) => apiClient.post("/ci/check", payload).then((r) => r.data),
  getScan: (id) => apiClient.get(`/ci/scans/${id}`).then((r) => r.data),
  listBranches: () => apiClient.get("/ci/branches").then((r) => r.data),
};
