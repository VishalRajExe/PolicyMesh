import { Navigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import AppShell from "./AppShell";

export default function ProtectedRoute({ children }) {
  const { user } = useAuth();
  const hasToken = typeof window !== "undefined" && localStorage.getItem("policymesh_token");

  if (!user && !hasToken) {
    return <Navigate to="/login" replace />;
  }

  return <AppShell>{children}</AppShell>;
}
