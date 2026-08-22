import { apiClient } from "./client";

export const reportsApi = {
  getComplianceReport: () => apiClient.get("/reports/compliance").then((r) => r.data),
  downloadCsvUrl: () => `${apiClient.defaults.baseURL}/reports/export/csv`,
  downloadCsv: async () => {
    const response = await apiClient.get("/reports/export/csv", { responseType: "blob" });
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", `policymesh-compliance-report-${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },
};
