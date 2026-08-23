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
  Sun,
  Moon,
  Monitor,
  Check,
} from "lucide-react";
import Topbar from "../components/layout/Topbar";
import Tabs from "../components/ui/Tabs";
import Button from "../components/ui/Button";
import Badge from "../components/ui/Badge";
import { settingsApi } from "../api";
import { useAuth } from "../context/AuthContext";
import { useTheme } from "../context/ThemeContext";

export default function Settings() {
  const { user: authUser } = useAuth();
  const { theme, setTheme, isDark } = useTheme();
  const [profile, setProfile] = useState(null);
  const [systemSettings, setSystemSettings] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState("profile"); // "profile" | "appearance" | "diagnostics"

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

  const initials = (authUser?.email || "CO").slice(0, 2).toUpperCase();

  return (
    <div>
      <Topbar
        title="Settings & Preferences"
        subtitle="Manage personal account credentials, appearance modes, and system configurations."
      />

      <div className="px-6 lg:px-8 py-6 space-y-6 pb-12">
        {/* Navigation Tabs */}
        <div className="flex items-center justify-between gap-3">
          <Tabs
            tabs={[
              { id: "profile", label: "Profile & Security", icon: User },
              { id: "appearance", label: "Appearance & Themes", icon: Sun },
              { id: "diagnostics", label: "Engine Integrations", icon: Server },
            ]}
            activeTab={activeTab}
            onChange={setActiveTab}
          />
        </div>

        {activeTab === "profile" && (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Account Card */}
            <div className="card p-5 space-y-4">
              <h3 className="font-bold text-sm text-[var(--color-text)]">Account Identity</h3>
              <div className="flex items-center gap-3 pt-1">
                <div className="w-12 h-12 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-sm font-bold text-white shadow-sm shrink-0">
                  {initials}
                </div>
                <div className="min-w-0">
                  <p className="text-sm font-semibold text-[var(--color-text)] truncate">{authUser?.email}</p>
                  <p className="text-xs text-[var(--color-text-faint)] mt-0.5">
                    User ID: {profile?.id || authUser?.id || "usr_001"}
                  </p>
                </div>
              </div>

              <div className="space-y-2 pt-2 border-t border-[var(--color-border)] text-xs font-mono">
                <div className="flex items-center justify-between py-1">
                  <span className="text-[var(--color-text-dim)]">Role:</span>
                  <Badge variant="brand" size="sm">{authUser?.role || "COMPLIANCE_OFFICER"}</Badge>
                </div>
                <div className="flex items-center justify-between py-1">
                  <span className="text-[var(--color-text-dim)]">Account Status:</span>
                  <Badge variant="good" size="sm" dot>Active</Badge>
                </div>
                <div className="flex items-center justify-between py-1">
                  <span className="text-[var(--color-text-dim)]">Member Since:</span>
                  <span className="text-[var(--color-text)]">
                    {profile?.createdAt ? new Date(profile.createdAt).toLocaleDateString() : "March 2026"}
                  </span>
                </div>
              </div>
            </div>

            {/* Change Password Card */}
            <div className="card p-5 space-y-4">
              <h3 className="font-bold text-sm text-[var(--color-text)] flex items-center gap-2">
                <Lock size={15} className="text-[var(--color-brand)]" />
                Change Password
              </h3>

              {passwordMessage && (
                <div className="text-xs text-[var(--color-good)] bg-[var(--color-good-light)] border border-[var(--color-good)]/30 rounded-lg p-2.5 flex items-center gap-2">
                  <CheckCircle2 size={14} className="shrink-0" />
                  <span>{passwordMessage}</span>
                </div>
              )}

              {passwordError && (
                <div className="text-xs text-[var(--color-bad)] bg-[var(--color-bad-light)] border border-[var(--color-bad)]/30 rounded-lg p-2.5 flex items-center gap-2">
                  <AlertCircle size={14} className="shrink-0" />
                  <span>{passwordError}</span>
                </div>
              )}

              <form onSubmit={handlePasswordChange} className="space-y-3">
                <div>
                  <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
                    Current Password
                  </label>
                  <input
                    type="password"
                    value={passwordForm.currentPassword}
                    onChange={(e) => setPasswordForm((f) => ({ ...f, currentPassword: e.target.value }))}
                    placeholder="••••••••••••"
                    className="field-input text-xs"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
                    New Password <span className="text-[var(--color-text-faint)]">(Min 8 characters)</span>
                  </label>
                  <input
                    type="password"
                    value={passwordForm.newPassword}
                    onChange={(e) => setPasswordForm((f) => ({ ...f, newPassword: e.target.value }))}
                    placeholder="••••••••••••"
                    className="field-input text-xs"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
                    Confirm New Password
                  </label>
                  <input
                    type="password"
                    value={passwordForm.confirmPassword}
                    onChange={(e) => setPasswordForm((f) => ({ ...f, confirmPassword: e.target.value }))}
                    placeholder="••••••••••••"
                    className="field-input text-xs"
                    required
                  />
                </div>

                <div className="pt-2">
                  <Button type="submit" variant="primary" size="md" loading={passwordSubmitting}>
                    Update Password
                  </Button>
                </div>
              </form>
            </div>
          </div>
        )}

        {activeTab === "appearance" && (
          <div className="card p-5 space-y-6">
            <div>
              <h3 className="font-bold text-sm text-[var(--color-text)]">Theme Mode</h3>
              <p className="text-xs text-[var(--color-text-dim)] mt-0.5">
                Choose how PolicyMesh looks on your screen. Preferences are automatically persisted.
              </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              {/* Light Mode */}
              <button
                type="button"
                onClick={() => setTheme("light")}
                className={`p-4 rounded-xl border text-left transition-all relative ${
                  theme === "light"
                    ? "border-[var(--color-brand)] ring-2 ring-[var(--color-brand)] bg-white text-slate-900 shadow-md"
                    : "border-[var(--color-border)] hover:border-[var(--color-border-strong)] bg-[var(--color-surface)] text-[var(--color-text)]"
                }`}
              >
                <div className="flex items-center justify-between mb-3">
                  <div className="w-8 h-8 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center border border-amber-200">
                    <Sun size={18} />
                  </div>
                  {theme === "light" && <Check size={16} className="text-[var(--color-brand)]" />}
                </div>
                <h4 className="font-bold text-xs">Light Mode</h4>
                <p className="text-[11px] text-slate-500 dark:text-[var(--color-text-faint)] mt-0.5">
                  Clean, high-contrast white & slate enterprise interface.
                </p>
              </button>

              {/* Dark Mode */}
              <button
                type="button"
                onClick={() => setTheme("dark")}
                className={`p-4 rounded-xl border text-left transition-all relative ${
                  theme === "dark"
                    ? "border-[var(--color-brand)] ring-2 ring-[var(--color-brand)] bg-[#12161f] text-white shadow-md"
                    : "border-[var(--color-border)] hover:border-[var(--color-border-strong)] bg-[var(--color-surface)] text-[var(--color-text)]"
                }`}
              >
                <div className="flex items-center justify-between mb-3">
                  <div className="w-8 h-8 rounded-lg bg-indigo-950 text-indigo-400 flex items-center justify-center border border-indigo-800">
                    <Moon size={18} />
                  </div>
                  {theme === "dark" && <Check size={16} className="text-[var(--color-brand)]" />}
                </div>
                <h4 className="font-bold text-xs">Dark Mode</h4>
                <p className="text-[11px] text-[var(--color-text-faint)] mt-0.5">
                  Deep charcoal background tailored for low-light environments.
                </p>
              </button>

              {/* System Mode */}
              <button
                type="button"
                onClick={() => setTheme("system")}
                className={`p-4 rounded-xl border text-left transition-all relative ${
                  theme === "system"
                    ? "border-[var(--color-brand)] ring-2 ring-[var(--color-brand)] bg-[var(--color-surface)] text-[var(--color-text)] shadow-md"
                    : "border-[var(--color-border)] hover:border-[var(--color-border-strong)] bg-[var(--color-surface)] text-[var(--color-text)]"
                }`}
              >
                <div className="flex items-center justify-between mb-3">
                  <div className="w-8 h-8 rounded-lg bg-[var(--color-surface-2)] text-[var(--color-text)] flex items-center justify-center border border-[var(--color-border)]">
                    <Monitor size={18} />
                  </div>
                  {theme === "system" && <Check size={16} className="text-[var(--color-brand)]" />}
                </div>
                <h4 className="font-bold text-xs">System Synchronized</h4>
                <p className="text-[11px] text-[var(--color-text-faint)] mt-0.5">
                  Matches your operating system's light or dark appearance.
                </p>
              </button>
            </div>
          </div>
        )}

        {activeTab === "diagnostics" && (
          <div className="card p-5 space-y-4">
            <h3 className="font-bold text-sm text-[var(--color-text)]">Engine Integrations</h3>
            <div className="divide-y divide-[var(--color-border)] text-xs">
              <div className="py-3 flex items-center justify-between">
                <div>
                  <strong className="text-[var(--color-text)]">Policy AST Compiler</strong>
                  <p className="text-[11px] text-[var(--color-text-faint)]">Compiles declarative YAML rules into in-memory evaluators</p>
                </div>
                <Badge variant="good" size="sm" dot>Active</Badge>
              </div>

              <div className="py-3 flex items-center justify-between">
                <div>
                  <strong className="text-[var(--color-text)]">Cryptographic Ledger Emitter</strong>
                  <p className="text-[11px] text-[var(--color-text-faint)]">Generates immutable SHA-256 hash chains for all enforcement events</p>
                </div>
                <Badge variant="good" size="sm" dot>Active</Badge>
              </div>

              <div className="py-3 flex items-center justify-between">
                <div>
                  <strong className="text-[var(--color-text)]">AI Sensitivity Tokenizer</strong>
                  <p className="text-[11px] text-[var(--color-text-faint)]">FastAPI Python microservice for schema classification</p>
                </div>
                <Badge variant="good" size="sm" dot>Active</Badge>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
