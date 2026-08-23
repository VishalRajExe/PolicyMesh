import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { Shield, Sun, Moon, AlertTriangle, CheckCircle2 } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { useTheme } from "../context/ThemeContext";
import Button from "../components/ui/Button";

const ROLES = ["ADMIN", "COMPLIANCE_OFFICER", "ENGINEER", "VIEWER"];

export default function Register() {
  const { register, loading, error } = useAuth();
  const { theme, setTheme } = useTheme();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: "", email: "", password: "", role: "ENGINEER" });
  const [done, setDone] = useState(false);

  function update(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    try {
      await register(form);
      setDone(true);
      setTimeout(() => navigate("/login"), 1200);
    } catch {
      // error surfaced via context
    }
  }

  return (
    <div className="min-h-screen w-full flex items-center justify-center bg-[var(--color-bg)] px-4 relative">
      {/* Theme Toggle */}
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
        {/* Header */}
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
          <h1 className="text-xl font-bold tracking-tight text-[var(--color-text)]">Create your account</h1>
          <p className="text-xs text-[var(--color-text-dim)] mt-0.5">Join the PolicyMesh governance network</p>
        </div>

        {/* Register Form */}
        <form onSubmit={handleSubmit} className="card p-6 space-y-4 shadow-xl">
          <div>
            <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
              Full Name
            </label>
            <input
              type="text"
              required
              value={form.name}
              onChange={(e) => update("name", e.target.value)}
              placeholder="Dr. Sarah Connor"
              className="field-input text-xs"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
              Email Address
            </label>
            <input
              type="email"
              required
              value={form.email}
              onChange={(e) => update("email", e.target.value)}
              placeholder="sconnor@company.com"
              className="field-input text-xs"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
              Password <span className="text-[var(--color-text-faint)]">(Min 8 characters)</span>
            </label>
            <input
              type="password"
              required
              value={form.password}
              onChange={(e) => update("password", e.target.value)}
              placeholder="••••••••••••"
              className="field-input text-xs"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
              Role
            </label>
            <select
              value={form.role}
              onChange={(e) => update("role", e.target.value)}
              className="field-input text-xs"
            >
              {ROLES.map((r) => (
                <option key={r} value={r}>
                  {r.replace("_", " ")}
                </option>
              ))}
            </select>
          </div>

          {error && (
            <div className="text-xs text-[var(--color-bad)] bg-[var(--color-bad-light)] border border-[var(--color-bad)]/30 rounded-lg p-2.5 flex items-center gap-2">
              <AlertTriangle size={14} className="shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {done && (
            <div className="text-xs text-[var(--color-good)] bg-[var(--color-good-light)] border border-[var(--color-good)]/30 rounded-lg p-2.5 flex items-center gap-2">
              <CheckCircle2 size={14} className="shrink-0" />
              <span>Account created — redirecting to sign in...</span>
            </div>
          )}

          <Button
            type="submit"
            variant="primary"
            size="md"
            className="w-full justify-center"
            loading={loading}
          >
            {loading ? "Creating Account..." : "Create Account"}
          </Button>
        </form>

        <p className="text-center text-xs text-[var(--color-text-dim)] mt-5">
          Already have an account?{" "}
          <Link to="/login" className="text-[var(--color-brand)] hover:underline font-semibold">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
