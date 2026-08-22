import { apiClient } from "./client";

export const usersApi = {
  list: () => apiClient.get("/users").then((r) => r.data),
  get: (id) => apiClient.get(`/users/${id}`).then((r) => r.data),
  create: (user) => apiClient.post("/users", user).then((r) => r.data),
  update: (id, updates) => apiClient.put(`/users/${id}`, updates).then((r) => r.data),
  remove: (id) => apiClient.delete(`/users/${id}`),
  roles: () => apiClient.get("/users/roles").then((r) => r.data),
};
