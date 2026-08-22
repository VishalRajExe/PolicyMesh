import { apiClient } from "./client";

export const auditApi = {
  recent: (limit = 20) => apiClient.get(`/audit/recent?limit=${limit}`).then((r) => r.data),
};
