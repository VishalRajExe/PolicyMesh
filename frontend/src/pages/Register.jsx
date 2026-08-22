import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { Shield } from "lucide-react";
import { useAuth } from "../context/AuthContext";

const ROLES = ["ADMIN", "COMPLIANCE_OFFICER", "ENGINEER", "VIEWER"];

export default function Register() {
  const { register, loading, error } = useAuth();
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
    <div className="min-h-screen w-full flex items-center justify-center bg-[var(--color-bg)] px-4">
      <div className="w-full max-w-sm">
        <div className="flex flex-col items-center mb-8">
          <img
            src="/logo.png"
            alt="PolicyMesh"
            className="w-16 h-16 object-contain mb-3 drop-shadow-xl animate-in fade-in zoom-in-90"
          />
          <h1 className="text-xl font-semibold text-white">Create your account</h1>
          <p className="text-sm text-[var(--color-text-faint)]">Join PolicyMesh</p>
        </div>

        <form onSubmit={handleSubmit} className="card p-6 space-y-4">
          <Field label="Name" value={form.name} onChange={(v) => update("name", v)} placeholder="Alice Smith" />
          <Field label="Email" type="email" value={form.email} onChange={(v) => update("email", v)} placeholder="you@company.com" />
          <Field label="Password" type="password" value={form.password} onChange={(v) => update("password", v)} placeholder="At least 8 characters" />

          <div>
            <label className="block text-sm text-[var(--color-text-dim)] mb-1.5">Role</label>
            <select
              value={form.role}
              onChange={(e) => update("role", e.target.value)}
              className="w-full rounded-xl bg-[var(--color-surface-2)] border border-[var(--color-border)] px-3.5 py-2.5 text-sm text-white outline-none focus:border-[var(--color-brand)] transition-colors"
            >
              {ROLES.map((r) => (
                <option key={r} value={r}>
                  {r.replace("_", " ")}
                </option>
              ))}
            </select>
          </div>

          {error && <p className="text-sm text-[var(--color-bad)]">{error}</p>}
          {done && <p className="text-sm text-[var(--color-good)]">Account created — redirecting to sign in...</p>}

          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-xl bg-[var(--color-brand)] text-white font-medium py-2.5 hover:bg-[var(--color-brand-dim)] transition-colors disabled:opacity-60"
          >
            {loading ? "Creating account..." : "Create account"}
          </button>
        </form>

        <p className="text-center text-sm text-[var(--color-text-faint)] mt-6">
          Already have an account?{" "}
          <Link to="/login" className="text-[var(--color-brand)] hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}

function Field({ label, value, onChange, type = "text", placeholder }) {
  return (
    <div>
      <label className="block text-sm text-[var(--color-text-dim)] mb-1.5">{label}</label>
      <input
        type={type}
        required
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="w-full rounded-xl bg-[var(--color-surface-2)] border border-[var(--color-border)] px-3.5 py-2.5 text-sm text-white outline-none focus:border-[var(--color-brand)] transition-colors"
      />
    </div>
  );
}
