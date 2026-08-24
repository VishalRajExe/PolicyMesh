import { apiClient } from "./client";

export const ciApi = {
  runCheck: (payload) => apiClient.post("/ci/check", payload).then((r) => r.data),
  getScan: (id) => apiClient.get(`/ci/scans/${id}`).then((r) => r.data),
  listScans: (page = 0, size = 10) => apiClient.get(`/ci/scans?page=${page}&size=${size}`).then((r) => r.data),
  listBranches: () => apiClient.get("/ci/branches").then((r) => r.data),
  clearAllScans: () => apiClient.delete("/ci/scans").then((r) => r.data),
  deleteScan: (id) => apiClient.delete(`/ci/scans/${id}`).then((r) => r.data),
};
