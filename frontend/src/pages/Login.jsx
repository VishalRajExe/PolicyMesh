import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { Shield, Sun, Moon, Monitor, AlertTriangle } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { useTheme } from "../context/ThemeContext";
import Button from "../components/ui/Button";

export default function Login() {
  const { login, loading, error } = useAuth();
  const { theme, setTheme } = useTheme();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  async function handleSubmit(e) {
    e.preventDefault();
    try {
      await login(email, password);
      navigate("/");
    } catch {
      // error handled in auth context
    }
  }

  return (
    <div className="min-h-screen w-full flex items-center justify-center bg-[var(--color-bg)] px-4 relative">
      {/* Theme Toggle Top Right */}
      <div className="absolute top-5 right-5">
        <button
          type="button"
          onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
          className="w-9 h-9 rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] hover:bg-[var(--color-surface-2)] text-[var(--color-text-dim)] hover:text-[var(--color-text)] flex items-center justify-center transition-colors shadow-2xs"
          title={`Theme: ${theme}`}
        >
          {theme === "dark" ? <Moon size={15} /> : <Sun size={15} />}
        </button>
      </div>

      <div className="w-full max-w-sm">
        {/* Brand Header */}
        <div className="flex flex-col items-center mb-6">
          <div className="w-12 h-12 rounded-2xl bg-[var(--color-brand-light)] border border-[var(--color-brand)]/20 flex items-center justify-center mb-3 shadow-sm overflow-hidden p-1.5">
            <img
              src="/logo.png"
              alt="PolicyMesh Logo"
              className="w-full h-full object-contain"
              onError={(e) => {
                e.currentTarget.style.display = "none";
                if (e.currentTarget.nextElementSibling) {
                  e.currentTarget.nextElementSibling.style.display = "flex";
                }
              }}
            />
            <div style={{ display: "none" }} className="w-full h-full items-center justify-center text-[var(--color-brand)]">
              <Shield size={24} className="fill-[var(--color-brand)]/20 stroke-[var(--color-brand)]" />
            </div>
          </div>
          <h1 className="text-xl font-bold tracking-tight text-[var(--color-text)]">PolicyMesh</h1>
          <p className="text-xs text-[var(--color-text-dim)] mt-0.5">Govern. Enforce. Trust.</p>
        </div>

        {/* Login Form */}
        <form onSubmit={handleSubmit} className="card p-6 space-y-4 shadow-xl">
          <div>
            <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
              Email Address
            </label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="field-input text-xs"
              placeholder="admin@policymesh.io"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
              Password
            </label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="field-input text-xs"
              placeholder="••••••••"
            />
          </div>

          {error && (
            <div className="text-xs text-[var(--color-bad)] bg-[var(--color-bad-light)] border border-[var(--color-bad)]/30 rounded-lg p-2.5 flex items-center gap-2">
              <AlertTriangle size={14} className="shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <Button
            type="submit"
            variant="primary"
            size="md"
            className="w-full justify-center"
            loading={loading}
          >
            Sign In to Platform
          </Button>
        </form>

        <p className="text-center text-xs text-[var(--color-text-dim)] mt-5">
          Don't have an account?{" "}
          <Link to="/register" className="text-[var(--color-brand)] hover:underline font-semibold">
            Register new workspace
          </Link>
        </p>
      </div>
    </div>
  );
}
