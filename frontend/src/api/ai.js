import { apiClient } from "./client";

export const aiApi = {
  /** classify a single field — backend accepts { fieldName, sampleValue } */
  classify: (fieldName, sampleValue) =>
    apiClient.post("/ai/classify", { fieldName, sampleValue }).then((r) => r.data),
  list: () => apiClient.get("/ai/classifications").then((r) => r.data),
  get: (id) => apiClient.get(`/ai/classifications/${id}`).then((r) => r.data),
  approve: (id) => apiClient.post(`/ai/classify/${id}/approve`).then((r) => r.data),
  reject: (id) => apiClient.post(`/ai/classify/${id}/reject`).then((r) => r.data),
};
