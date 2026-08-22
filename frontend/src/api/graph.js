import { apiClient } from "./client";

export const graphApi = {
  get: () => apiClient.get("/graph").then((r) => r.data),
  validate: () => apiClient.post("/graph/validate").then((r) => r.data),
};
