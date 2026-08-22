import { apiClient } from "./client";

export const lineageApi = {
  list: () => apiClient.get("/lineage").then((r) => r.data),
  get: (id) => apiClient.get(`/lineage/${id}`).then((r) => r.data),
  verify: () => apiClient.get("/lineage/verify").then((r) => r.data),
};
