import { useEffect, useState } from "react";
import {
  User,
  Shield,
  Key,
  Server,
  Database,
  Cpu,
  Layers,
  CheckCircle2,
  AlertCircle,
  Lock,
  Loader2,
  RefreshCw,
  Sliders,
} from "lucide-react";
import Topbar from "../components/layout/Topbar";
import { settingsApi } from "../api";
import { useAuth } from "../context/AuthContext";

export default function Settings() {
  const { user: authUser } = useAuth();
  const [profile, setProfile] = useState(null);
  const [systemSettings, setSystemSettings] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState("profile"); // "profile" | "system" | "governance"

  // Password Change Form
  const [passwordForm, setPasswordForm] = useState({ currentPassword: "", newPassword: "", confirmPassword: "" });
  const [passwordSubmitting, setPasswordSubmitting] = useState(false);
  const [passwordMessage, setPasswordMessage] = useState(null);
  const [passwordError, setPasswordError] = useState(null);

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const [profData, sysData] = await Promise.allSettled([
        settingsApi.getProfile(),
        settingsApi.getSystemSettings(),
      ]);
      if (profData.status === "fulfilled") setProfile(profData.value);
      if (sysData.status === "fulfilled") setSystemSettings(sysData.value);
    } catch (err) {
      setError(err.message || "Failed to load settings");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
  }, []);

  async function handlePasswordChange(e) {
    e.preventDefault();
    setPasswordError(null);
    setPasswordMessage(null);

    if (!passwordForm.currentPassword || !passwordForm.newPassword) {
      setPasswordError("All password fields are required.");
      return;
    }
    if (passwordForm.newPassword.length < 8) {
      setPasswordError("New password must be at least 8 characters.");
      return;
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setPasswordError("New passwords do not match.");
      return;
    }

    setPasswordSubmitting(true);
    try {
      await settingsApi.changePassword({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      });
      setPasswordMessage("Password changed successfully.");
      setPasswordForm({ currentPassword: "", newPassword: "", confirmPassword: "" });
    } catch (err) {
      setPasswordError(err.message || "Failed to change password");
    } finally {
      setPasswordSubmitting(false);
    }
  }

  const components = systemSettings
    ? [
        systemSettings.api,
        systemSettings.database,
        systemSettings.redis,
        systemSettings.aiService,
        systemSettings.kafka,
      ].filter(Boolean)
    : [];

  return (
    <div>
      <Topbar
        title="Platform Settings & Diagnostics"
        subtitle="Manage account credentials, inspect live engine diagnostics, and monitor infrastructure integrations."
      />

      <div className="px-6 lg:px-8 mt-4 space-y-6 pb-12">
        {/* Navigation Tabs */}
        <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-3">
          <div className="flex items-center gap-2">
            <button
              onClick={() => setActiveTab("profile")}
              className={`px-4 py-2 text-xs font-medium rounded-xl transition-colors flex items-center gap-2 ${
                activeTab === "profile"
                  ? "bg-[var(--color-brand)] text-white"
                  : "bg-[var(--color-surface)] text-[var(--color-text-dim)] hover:text-white border border-[var(--color-border)]"
              }`}
            >
              <User size={14} /> Profile & Security
            </button>
            <button
              onClick={() => setActiveTab("system")}
              className={`px-4 py-2 text-xs font-medium rounded-xl transition-colors flex items-center gap-2 ${
                activeTab === "system"
                  ? "bg-[var(--color-brand)] text-white"
                  : "bg-[var(--color-surface)] text-[var(--color-text-dim)] hover:text-white border border-[var(--color-border)]"
              }`}
            >
              <Server size={14} /> Infrastructure Health
            </button>
            <button
              onClick={() => setActiveTab("governance")}
              className={`px-4 py-2 text-xs font-medium rounded-xl transition-colors flex items-center gap-2 ${
                activeTab === "governance"
                  ? "bg-[var(--color-brand)] text-white"
                  : "bg-[var(--color-surface)] text-[var(--color-text-dim)] hover:text-white border border-[var(--color-border)]"
              }`}
            >
              <Sliders size={14} /> Engine Parameters
            </button>
          </div>

          <button
            onClick={loadData}
            disabled={loading}
            className="btn-ghost flex items-center gap-1.5 text-xs"
          >
            <RefreshCw size={14} className={loading ? "animate-spin" : ""} /> Refresh
          </button>
        </div>

        {error && (
          <div className="rounded-xl bg-[var(--color-bad)]/10 border border-[var(--color-bad)]/30 px-4 py-3 text-sm text-[var(--color-bad)] flex items-center justify-between">
            {error}
            <button onClick={loadData} className="underline text-xs ml-4">Retry</button>
          </div>
        )}

        {/* Tab 1: Profile & Security */}
        {activeTab === "profile" && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Account Info */}
            <div className="card p-6 space-y-4">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-[#7c6cf9] to-[#5b3df0] flex items-center justify-center text-base font-bold text-white shadow-lg">
                  {profile?.email?.slice(0, 2).toUpperCase() || "PM"}
                </div>
                <div>
                  <h3 className="text-base font-semibold text-white">{profile?.email || authUser?.email}</h3>
                  <p className="text-xs text-[var(--color-text-faint)]">Account ID #{profile?.id || "—"}</p>
                </div>
              </div>

              <div className="border-t border-[var(--color-border)] pt-4 space-y-3 text-xs">
                <div className="flex justify-between py-1">
                  <span className="text-[var(--color-text-dim)]">Assigned Role:</span>
                  <span className="font-semibold text-purple-400 bg-purple-500/10 px-2.5 py-0.5 rounded-lg border border-purple-500/20">
                    {profile?.role || authUser?.role}
                  </span>
                </div>
                <div className="flex justify-between py-1">
                  <span className="text-[var(--color-text-dim)]">Account Status:</span>
                  <span className="font-semibold text-[var(--color-good)] bg-[var(--color-good)]/10 px-2 py-0.5 rounded-md">
                    {profile?.status || "ACTIVE"}
                  </span>
                </div>
                <div className="flex justify-between py-1">
                  <span className="text-[var(--color-text-dim)]">Member Since:</span>
                  <span className="text-white font-mono">
                    {profile?.createdAt ? new Date(profile.createdAt).toLocaleDateString() : "—"}
                  </span>
                </div>
                <div className="flex justify-between py-1">
                  <span className="text-[var(--color-text-dim)]">Authentication Type:</span>
                  <span className="text-white">HMAC-SHA256 JWT Token</span>
                </div>
              </div>
            </div>

            {/* Change Password Form */}
            <div className="card p-6 space-y-4">
              <div className="flex items-center gap-2">
                <Lock size={18} className="text-[var(--color-brand)]" />
                <h3 className="text-base font-semibold text-white">Change Password</h3>
              </div>
              <p className="text-xs text-[var(--color-text-dim)]">
                Update your account password with BCrypt cryptographic hashing.
              </p>

              <form onSubmit={handlePasswordChange} className="space-y-3 pt-1">
                <div>
                  <label className="block text-xs text-[var(--color-text-dim)] mb-1">Current Password *</label>
                  <input
                    type="password"
                    value={passwordForm.currentPassword}
                    onChange={(e) => setPasswordForm({ ...passwordForm, currentPassword: e.target.value })}
                    placeholder="••••••••••••"
                    className="field-input text-xs"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs text-[var(--color-text-dim)] mb-1">New Password * (min 8 chars)</label>
                  <input
                    type="password"
                    value={passwordForm.newPassword}
                    onChange={(e) => setPasswordForm({ ...passwordForm, newPassword: e.target.value })}
                    placeholder="••••••••••••"
                    className="field-input text-xs"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs text-[var(--color-text-dim)] mb-1">Confirm New Password *</label>
                  <input
                    type="password"
                    value={passwordForm.confirmPassword}
                    onChange={(e) => setPasswordForm({ ...passwordForm, confirmPassword: e.target.value })}
                    placeholder="••••••••••••"
                    className="field-input text-xs"
                    required
                  />
                </div>

                {passwordError && (
                  <p className="text-xs text-[var(--color-bad)] bg-[var(--color-bad)]/10 rounded-lg px-3 py-2">
                    {passwordError}
                  </p>
                )}

                {passwordMessage && (
                  <p className="text-xs text-[var(--color-good)] bg-[var(--color-good)]/10 rounded-lg px-3 py-2 flex items-center gap-1.5">
                    <CheckCircle2 size={14} /> {passwordMessage}
                  </p>
                )}

                <div className="pt-2">
                  <button
                    type="submit"
                    disabled={passwordSubmitting}
                    className="btn-primary w-full flex items-center justify-center gap-1.5 text-xs py-2.5"
                  >
                    {passwordSubmitting ? <Loader2 size={14} className="animate-spin" /> : <Key size={14} />}
                    Update Password
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Tab 2: Infrastructure Health */}
        {activeTab === "system" && (
          <div className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {components.map((c) => {
                const isHealthy = c.status === "HEALTHY" || c.status === "CONNECTED";
                return (
                  <div key={c.name} className="card p-5 space-y-3">
                    <div className="flex items-start justify-between">
                      <div>
                        <h4 className="text-sm font-semibold text-white">{c.name}</h4>
                        <p className="text-xs text-[var(--color-text-faint)] mt-0.5">{c.type}</p>
                      </div>
                      <span
                        className={`text-xs px-2.5 py-1 rounded-lg font-semibold flex items-center gap-1 ${
                          isHealthy
                            ? "bg-[var(--color-good)]/15 text-[var(--color-good)] border border-[var(--color-good)]/30"
                            : "bg-amber-500/15 text-amber-400 border border-amber-500/30"
                        }`}
                      >
                        {isHealthy ? <CheckCircle2 size={12} /> : <AlertCircle size={12} />}
                        {c.status}
                      </span>
                    </div>
                    <p className="text-xs text-[var(--color-text-dim)] border-t border-[var(--color-border)] pt-3 leading-relaxed">
                      {c.details}
                    </p>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* Tab 3: Governance Parameters */}
        {activeTab === "governance" && (
          <div className="card p-6 space-y-5">
            <div>
              <h3 className="text-base font-semibold text-white">Active Policy Engine Configuration</h3>
              <p className="text-xs text-[var(--color-text-dim)]">
                Runtime parameters governing policy evaluation, hash-chain auditing, and fallback mechanisms.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
              <div className="p-4 rounded-xl bg-[var(--color-surface-2)] border border-[var(--color-border)] space-y-1">
                <span className="text-[var(--color-text-faint)]">Enforcement Mode</span>
                <p className="text-sm font-semibold text-white">STRICT_ENFORCE</p>
                <p className="text-[11px] text-[var(--color-text-dim)]">
                  All cross-border and cross-service data transfers must satisfy compiled policy ASTs before authorization.
                </p>
              </div>

              <div className="p-4 rounded-xl bg-[var(--color-surface-2)] border border-[var(--color-border)] space-y-1">
                <span className="text-[var(--color-text-faint)]">Default Policy Fallback</span>
                <p className="text-sm font-semibold text-amber-400">DENY (Zero-Trust)</p>
                <p className="text-[11px] text-[var(--color-text-dim)]">
                  Unclassified or unmapped data flows lacking explicit ALLOW policy rules are automatically rejected.
                </p>
              </div>

              <div className="p-4 rounded-xl bg-[var(--color-surface-2)] border border-[var(--color-border)] space-y-1">
                <span className="text-[var(--color-text-faint)]">Lineage Ledger Cryptography</span>
                <p className="text-sm font-semibold text-emerald-400">SHA-256 Genesis Chain</p>
                <p className="text-[11px] text-[var(--color-text-dim)]">
                  Cryptographically binds each runtime decision to previous hash for complete audit trail immutability.
                </p>
              </div>

              <div className="p-4 rounded-xl bg-[var(--color-surface-2)] border border-[var(--color-border)] space-y-1">
                <span className="text-[var(--color-text-faint)]">Human Review Protocol</span>
                <p className="text-sm font-semibold text-purple-400">Mandatory for PII/PHI</p>
                <p className="text-[11px] text-[var(--color-text-dim)]">
                  AI-suggested classifications require approval from COMPLIANCE_OFFICER or ADMIN before enforcement activation.
                </p>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
