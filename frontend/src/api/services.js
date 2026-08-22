import { apiClient } from "./client";

export const servicesApi = {
  list: () => apiClient.get("/services").then((r) => r.data),
  get: (id) => apiClient.get(`/services/${id}`).then((r) => r.data),
  create: (payload) => apiClient.post("/services", payload).then((r) => r.data),
  update: (id, payload) => apiClient.put(`/services/${id}`, payload).then((r) => r.data),
  remove: (id) => apiClient.delete(`/services/${id}`).then((r) => r.data),
};

export const edgesApi = {
  list: () => apiClient.get("/edges").then((r) => r.data),
  create: (payload) => apiClient.post("/edges", payload).then((r) => r.data),
  remove: (id) => apiClient.delete(`/edges/${id}`).then((r) => r.data),
};
