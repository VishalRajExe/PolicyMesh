import { apiClient } from "./client";

export const policiesApi = {
  list: () => apiClient.get("/policies").then((r) => r.data),
  get: (id) => apiClient.get(`/policies/${id}`).then((r) => r.data),
  create: (payload) => apiClient.post("/policies", payload).then((r) => r.data),
  createFromYaml: (yaml) => apiClient.post("/policies/yaml", { yaml }).then((r) => r.data),
  importYaml: (yaml) => apiClient.post("/policies/yaml", { yaml }).then((r) => r.data),
  update: (id, payload) => apiClient.put(`/policies/${id}`, payload).then((r) => r.data),
  remove: (id) => apiClient.delete(`/policies/${id}`).then((r) => r.data),
};
