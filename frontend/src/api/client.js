import axios from "axios";

// Base URL for the PolicyMesh backend. Configure via .env (VITE_API_BASE_URL)
// so the same build can point at localhost during development and a real
// host in production. See design.md for the full wiring explanation.
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api/v1";

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// Attach the JWT (if present) to every outgoing request.
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("policymesh_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Centralized handling for expired/invalid sessions and RFC 7807 error bodies.
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem("policymesh_token");
      localStorage.removeItem("policymesh_user");
      if (typeof window !== "undefined" && !window.location.pathname.startsWith("/login")) {
        window.location.href = "/login";
      }
    }

    const problem = error.response?.data;
    const message = problem?.detail || problem?.title || error.message || "Something went wrong";
    return Promise.reject(new Error(message));
  }
);
