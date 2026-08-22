import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { Shield } from "lucide-react";
import { useAuth } from "../context/AuthContext";

export default function Login() {
  const { login, loading, error } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  async function handleSubmit(e) {
    e.preventDefault();
    try {
      await login(email, password);
      navigate("/");
    } catch {
      // error is surfaced via auth context state
    }
  }

  return (
    <div className="min-h-screen w-full flex items-center justify-center bg-[var(--color-bg)] px-4">
      <div className="w-full max-w-sm">
        <div className="flex flex-col items-center mb-8">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-[#7c6cf9] to-[#5b3df0] flex items-center justify-center mb-4">
            <Shield size={24} className="text-white" strokeWidth={2.5} />
          </div>
          <h1 className="text-xl font-semibold text-white">PolicyMesh</h1>
          <p className="text-sm text-[var(--color-text-faint)]">Govern. Enforce. Trust.</p>
        </div>

        <form onSubmit={handleSubmit} className="card p-6 space-y-4">
          <div>
            <label className="block text-sm text-[var(--color-text-dim)] mb-1.5">Email</label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full rounded-xl bg-[var(--color-surface-2)] border border-[var(--color-border)] px-3.5 py-2.5 text-sm text-white outline-none focus:border-[var(--color-brand)] transition-colors"
              placeholder="you@company.com"
            />
          </div>
          <div>
            <label className="block text-sm text-[var(--color-text-dim)] mb-1.5">Password</label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full rounded-xl bg-[var(--color-surface-2)] border border-[var(--color-border)] px-3.5 py-2.5 text-sm text-white outline-none focus:border-[var(--color-brand)] transition-colors"
              placeholder="••••••••"
            />
          </div>

          {error && <p className="text-sm text-[var(--color-bad)]">{error}</p>}

          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-xl bg-[var(--color-brand)] text-white font-medium py-2.5 hover:bg-[var(--color-brand-dim)] transition-colors disabled:opacity-60"
          >
            {loading ? "Signing in..." : "Sign in"}
          </button>
        </form>

        <p className="text-center text-sm text-[var(--color-text-faint)] mt-6">
          Don't have an account?{" "}
          <Link to="/register" className="text-[var(--color-brand)] hover:underline">
            Register
          </Link>
        </p>
      </div>
    </div>
  );
}
