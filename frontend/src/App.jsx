import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import ProtectedRoute from "./components/layout/ProtectedRoute";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Dashboard from "./pages/Dashboard";
import Policies from "./pages/Policies";
import Services from "./pages/Services";
import DataFlows from "./pages/DataFlows";
import RuntimeMonitor from "./pages/RuntimeMonitor";
import Lineage from "./pages/Lineage";
import AiClassification from "./pages/AiClassification";
import CiCheck from "./pages/CiCheck";
import Alerts from "./pages/Alerts";
import ComingSoon from "./pages/ComingSoon";

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public routes */}
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

          {/* Protected routes — all require a valid JWT */}
          <Route path="/" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
          <Route path="/policies" element={<ProtectedRoute><Policies /></ProtectedRoute>} />
          <Route path="/services" element={<ProtectedRoute><Services /></ProtectedRoute>} />
          <Route path="/data-flows" element={<ProtectedRoute><DataFlows /></ProtectedRoute>} />
          <Route path="/runtime-monitor" element={<ProtectedRoute><RuntimeMonitor /></ProtectedRoute>} />
          <Route path="/lineage" element={<ProtectedRoute><Lineage /></ProtectedRoute>} />
          <Route path="/ai-classification" element={<ProtectedRoute><AiClassification /></ProtectedRoute>} />
          <Route path="/ci-check" element={<ProtectedRoute><CiCheck /></ProtectedRoute>} />
          <Route path="/alerts" element={<ProtectedRoute><Alerts /></ProtectedRoute>} />

          {/* Coming soon — no backend API available yet */}
          <Route
            path="/reports"
            element={
              <ProtectedRoute>
                <ComingSoon
                  title="Reports"
                  subtitle="Scheduled compliance exports, audit summaries, and regulatory reports are on the roadmap."
                />
              </ProtectedRoute>
            }
          />
          <Route
            path="/users-roles"
            element={
              <ProtectedRoute>
                <ComingSoon
                  title="Users & Roles"
                  subtitle="Team member management and fine-grained role assignment will be available in a future release."
                />
              </ProtectedRoute>
            }
          />
          <Route
            path="/settings"
            element={
              <ProtectedRoute>
                <ComingSoon
                  title="Settings"
                  subtitle="Workspace configuration, integrations, and notification settings are coming soon."
                />
              </ProtectedRoute>
            }
          />

          {/* 404 catch-all */}
          <Route
            path="*"
            element={
              <div className="flex flex-col items-center justify-center h-screen text-center px-6">
                <p className="text-6xl font-bold text-[var(--color-text-faint)] mb-4">404</p>
                <p className="text-lg text-[var(--color-text-dim)] mb-6">Page not found</p>
                <a href="/" className="text-sm text-[var(--color-brand)] hover:underline">← Back to Dashboard</a>
              </div>
            }
          />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
