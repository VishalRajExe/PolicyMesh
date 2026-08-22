import { apiClient } from "./client";

export const enforcementApi = {
  check: (payload) => apiClient.post("/enforce/check", payload).then((r) => r.data),
};
