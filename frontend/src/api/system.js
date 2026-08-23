import { apiClient } from "./client";

export const systemApi = {
  getStatus: () => apiClient.get("/settings/system").then((r) => r.data),
  runDiagnostics: async () => {
    const start = performance.now();
    const data = await apiClient.get("/settings/system").then((r) => r.data);
    const latency = Math.round(performance.now() - start);
    return { ...data, latency };
  },
};
