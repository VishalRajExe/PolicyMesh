import { apiClient } from "./client";

export const settingsApi = {
  getProfile: () => apiClient.get("/settings/profile").then((r) => r.data),
  changePassword: (data) => apiClient.post("/settings/change-password", data).then((r) => r.data),
  getSystemSettings: () => apiClient.get("/settings/system").then((r) => r.data),
};
