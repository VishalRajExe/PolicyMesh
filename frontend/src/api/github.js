import { apiClient } from "./client";

export const githubApi = {
  // OAuth & Connection
  getConnectUrl: () =>
    apiClient.get("/github/connect").then((r) => r.data),

  connect: () =>
    apiClient.get("/github/connect").then((r) => r.data),

  getAccount: () =>
    apiClient.get("/github/account").then((r) => r.data),

  disconnect: () =>
    apiClient.delete("/github/disconnect").then((r) => r.data),

  // Repositories & Monitoring
  listRepositories: () =>
    apiClient.get("/github/repositories").then((r) => r.data),

  enableMonitoring: (repoId, details = {}) =>
    apiClient.post(`/github/repositories/${repoId}/monitor`, details).then((r) => r.data),

  disableMonitoring: (repoId) =>
    apiClient.delete(`/github/repositories/${repoId}/monitor`).then((r) => r.data),

  // Scans & Deliveries
  listCommits: (page = 0, size = 10) =>
    apiClient.get(`/github/commits?page=${page}&size=${size}`).then((r) => r.data),

  getCommit: (sha) =>
    apiClient.get(`/github/commits/${sha}`).then((r) => r.data),

  getCommitViolations: (sha) =>
    apiClient.get(`/github/commits/${sha}/violations`).then((r) => r.data),

  listDeliveries: (page = 0, size = 10) =>
    apiClient.get(`/github/webhook-deliveries?page=${page}&size=${size}`).then((r) => r.data),
};