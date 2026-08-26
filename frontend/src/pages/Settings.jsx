import { useEffect, useState } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
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
  GitBranch,
  GitCommit,
  ExternalLink,
  Trash2,
  Search,
  ShieldCheck,
  Radio,
  ArrowRight,
} from "lucide-react";
import Topbar from "../components/layout/Topbar";
import Tabs from "../components/ui/Tabs";
import Button from "../components/ui/Button";
import Badge from "../components/ui/Badge";
import { settingsApi, githubApi } from "../api";
import { useAuth } from "../context/AuthContext";
import { useTheme } from "../context/ThemeContext";

export default function Settings() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const { user: authUser } = useAuth();
  const { theme, setTheme, isDark } = useTheme();

  const [profile, setProfile] = useState(null);
  const [systemSettings, setSystemSettings] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Active tab (supports deep-linking via ?tab=github)
  const initialTab = searchParams.get("tab") || "profile";
  const [activeTab, setActiveTab] = useState(initialTab);

  // Status/Errors from OAuth Redirect
  const oauthStatus = searchParams.get("status");
  const oauthError = searchParams.get("error");
  const oauthUsername = searchParams.get("username");

  // GitHub Integration State
  const [githubAccount, setGithubAccount] = useState(null);
  const [githubRepos, setGithubRepos] = useState([]);
  const [githubLoading, setGithubLoading] = useState(false);
  const [repoSearch, setRepoSearch] = useState("");
  const [actionLoadingId, setActionLoadingId] = useState(null);
  const [githubMessage, setGithubMessage] = useState(null);
  const [githubErrorMsg, setGithubErrorMsg] = useState(oauthError || null);

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

  async function loadGitHubData() {
    setGithubLoading(true);
    try {
      const acc = await githubApi.getAccount();
      setGithubAccount(acc);
      if (acc?.connected) {
        const repos = await githubApi.listRepositories();
        setGithubRepos(repos || []);
      }
    } catch (err) {
      console.warn("Failed loading GitHub data:", err);
    } finally {
      setGithubLoading(false);
    }
  }

  useEffect(() => {
    loadData();
    loadGitHubData();
  }, []);

  useEffect(() => {
    const tab = searchParams.get("tab");
    if (tab && tab !== activeTab) {
      setActiveTab(tab);
    }
  }, [searchParams]);

  function handleTabChange(newTab) {
    setActiveTab(newTab);
    setSearchParams({ tab: newTab });
  }

  async function handleConnectGitHub() {
    try {
      setGithubErrorMsg(null);
      const res = await githubApi.getConnectUrl();
      if (res?.authorizationUrl) {
        window.location.href = res.authorizationUrl;
      }
    } catch (err) {
      setGithubErrorMsg(err.message || "Failed to start GitHub authorization.");
    }
  }

  async function handleDisconnectGitHub() {
    if (!window.confirm("Are you sure you want to disconnect your GitHub account? Monitored repositories will be disabled.")) {
      return;
    }
    try {
      setGithubLoading(true);
      await githubApi.disconnect();
      setGithubAccount({ connected: false });
      setGithubRepos([]);
      setGithubMessage("GitHub account disconnected successfully.");
    } catch (err) {
      setGithubErrorMsg(err.message || "Failed to disconnect GitHub account.");
    } finally {
      setGithubLoading(false);
    }
  }

  async function handleToggleMonitoring(repo) {
    setActionLoadingId(repo.id);
    setGithubMessage(null);
    setGithubErrorMsg(null);
    try {
      if (repo.isMonitored) {
        await githubApi.disableMonitoring(repo.id);
        setGithubRepos((prev) =>
          prev.map((r) => (r.id === repo.id ? { ...r, isMonitored: false } : r))
        );
        setGithubMessage("Monitoring disabled for " + repo.fullName);
      } else {
        await githubApi.enableMonitoring(repo.id, {
          fullName: repo.fullName,
          name: repo.name,
          ownerLogin: repo.ownerLogin,
          defaultBranch: repo.defaultBranch,
          isPrivate: repo.isPrivate,
        });
        setGithubRepos((prev) =>
          prev.map((r) => (r.id === repo.id ? { ...r, isMonitored: true } : r))
        );
        setGithubMessage("Monitoring enabled for " + repo.fullName);
      }
    } catch (err) {
      setGithubErrorMsg(err.message || "Failed updating repository monitoring state.");
    } finally {
      setActionLoadingId(null);
    }
  }

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

  const filteredRepos = githubRepos.filter((r) =>
    r.fullName?.toLowerCase().includes(repoSearch.toLowerCase()) ||
    r.name?.toLowerCase().includes(repoSearch.toLowerCase())
  );

  return (
    <div>
      <Topbar
        title="Settings & Preferences"
        subtitle="Manage personal account credentials, GitHub repository monitoring, appearance modes, and system configurations."
      />

      <div className="px-4 sm:px-6 lg:px-8 py-4 sm:py-6 space-y-4 sm:space-y-6 pb-12">
        {/* Navigation Tabs (scrollable on mobile) */}
        <div className="overflow-x-auto min-w-0 pb-1 -mx-4 px-4 sm:mx-0 sm:px-0">
          <Tabs
            tabs={[
              { id: "profile", label: "Profile & Security", icon: User },
              { id: "github", label: "GitHub Integration", icon: GitBranch },
              { id: "appearance", label: "Appearance & Themes", icon: Sun },
              { id: "diagnostics", label: "Engine Integrations", icon: Server },
            ]}
            activeTab={activeTab}
            onChange={handleTabChange}
          />
        </div>

        {/* Tab 1: Profile & Security */}
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

        {/* Tab 2: GitHub Integration */}
        {activeTab === "github" && (
          <div className="space-y-6">
            {/* Status alerts */}
            {oauthStatus === "connected" && (
              <div className="p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-500 text-xs flex items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <CheckCircle2 size={16} />
                  <span>GitHub account connected successfully{oauthUsername ? " as @" + oauthUsername : ""}!</span>
                </div>
                <button
                  onClick={() => setSearchParams({ tab: "github" })}
                  className="text-xs hover:underline cursor-pointer"
                >
                  Dismiss
                </button>
              </div>
            )}

            {githubMessage && (
              <div className="p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-500 text-xs flex items-center gap-2">
                <CheckCircle2 size={16} />
                <span>{githubMessage}</span>
              </div>
            )}

            {githubErrorMsg && (
              <div className="p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-500 text-xs flex items-center gap-2">
                <AlertCircle size={16} />
                <span>{githubErrorMsg}</span>
              </div>
            )}

            {/* Connection Banner / Status */}
            <div className="card p-6">
              {githubLoading ? (
                <div className="flex items-center justify-center py-8 gap-3 text-xs text-[var(--color-text-faint)]">
                  <Loader2 size={18} className="animate-spin text-[var(--color-brand)]" />
                  <span>Loading GitHub connection status...</span>
                </div>
              ) : githubAccount?.connected ? (
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                  <div className="flex items-center gap-4">
                    {githubAccount.avatarUrl ? (
                      <img
                        src={githubAccount.avatarUrl}
                        alt={githubAccount.username}
                        className="w-14 h-14 rounded-full border-2 border-[var(--color-brand)] shadow-sm"
                      />
                    ) : (
                      <div className="w-14 h-14 rounded-full bg-[var(--color-surface-2)] border border-[var(--color-border)] flex items-center justify-center text-lg font-bold">
                        {githubAccount.username?.slice(0, 2).toUpperCase()}
                      </div>
                    )}
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <h3 className="text-base font-bold text-[var(--color-text)]">
                          @{githubAccount.username}
                        </h3>
                        <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-500 border border-emerald-500/20">
                          Connected & Active
                        </span>
                      </div>
                      <p className="text-xs text-[var(--color-text-muted)]">
                        Connected on {new Date(githubAccount.connectedAt).toLocaleDateString()} • Least-privilege OAuth scopes: <span className="font-mono">{githubAccount.scope || "read:user,repo"}</span>
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-3">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={loadGitHubData}
                      icon={RefreshCw}
                    >
                      Sync Repositories
                    </Button>
                    <Button
                      variant="danger"
                      size="sm"
                      onClick={handleDisconnectGitHub}
                      icon={Trash2}
                    >
                      Disconnect GitHub
                    </Button>
                  </div>
                </div>
              ) : (
                <div className="space-y-4">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-6">
                    <div className="space-y-2 max-w-xl">
                      <div className="flex items-center gap-2">
                        <div className="w-8 h-8 rounded-lg bg-[var(--color-surface-2)] border border-[var(--color-border)] flex items-center justify-center text-[var(--color-brand)]">
                          <GitBranch size={18} />
                        </div>
                        <h3 className="text-base font-bold text-[var(--color-text)]">
                          Connect GitHub Account
                        </h3>
                      </div>
                      <p className="text-xs text-[var(--color-text-muted)] leading-relaxed">
                        Connect your GitHub organization or personal profile to enable automated data residency and compliance scanning on git push.
                      </p>
                      <div className="flex flex-wrap items-center gap-3 pt-1 text-[11px] text-[var(--color-text-faint)]">
                        <span className="flex items-center gap-1">
                          <ShieldCheck size={13} className="text-emerald-500" /> Read-only access
                        </span>
                        <span className="flex items-center gap-1">
                          <ShieldCheck size={13} className="text-emerald-500" /> AES-256 encrypted at rest
                        </span>
                        <span className="flex items-center gap-1">
                          <ShieldCheck size={13} className="text-emerald-500" /> Zero push / admin access
                        </span>
                      </div>
                    </div>

                    <div className="shrink-0">
                      <Button
                        variant="primary"
                        size="lg"
                        onClick={handleConnectGitHub}
                        icon={GitBranch}
                      >
                        Connect with GitHub
                      </Button>
                    </div>
                  </div>

                  {githubAccount?.oauthConfigured === false && (
                    <div className="p-4 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-300 text-xs space-y-2">
                      <div className="flex items-center gap-2 font-semibold">
                        <AlertCircle size={15} className="shrink-0 text-amber-400" />
                        <span>OAuth App Credentials Setup Required</span>
                      </div>
                      <p className="text-[11px] text-amber-200/80 leading-relaxed">
                        To enable 1-click GitHub authorization, register an OAuth App in GitHub Developer Settings and configure the client credentials in your environment:
                      </p>
                      <div className="p-2.5 rounded-lg bg-black/40 border border-amber-500/20 font-mono text-[10px] text-amber-100 space-y-1">
                        <div>GITHUB_CLIENT_ID=&lt;your_github_client_id&gt;</div>
                        <div>GITHUB_CLIENT_SECRET=&lt;your_github_client_secret&gt;</div>
                        <div>GITHUB_REDIRECT_URI=&lt;your_backend_url&gt;/api/v1/github/callback</div>
                      </div>
                      <div className="pt-1">
                        <a
                          href="https://github.com/settings/applications/new"
                          target="_blank"
                          rel="noreferrer"
                          className="inline-flex items-center gap-1 text-xs font-semibold text-amber-400 hover:underline"
                        >
                          Create GitHub OAuth App <ExternalLink size={12} />
                        </a>
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Repositories Selection & Monitoring Table */}
            {githubAccount?.connected && (
              <div className="card p-5 space-y-4">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-[var(--color-border)]/50 pb-3">
                  <div>
                    <h3 className="font-bold text-sm text-[var(--color-text)]">
                      Monitored Repositories ({githubRepos.filter((r) => r.isMonitored).length} of {githubRepos.length} active)
                    </h3>
                    <p className="text-xs text-[var(--color-text-faint)] mt-0.5">
                      Select which repositories PolicyMesh should automatically analyze upon git push events.
                    </p>
                  </div>

                  <div className="w-full sm:w-64 relative">
                    <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--color-text-faint)] pointer-events-none" />
                    <input
                      type="text"
                      value={repoSearch}
                      onChange={(e) => setRepoSearch(e.target.value)}
                      placeholder="Search repositories..."
                      className="field-input field-input-search !pl-9 text-xs py-1.5"
                    />
                  </div>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-xs text-left">
                    <thead>
                      <tr className="border-b border-[var(--color-border)] text-[var(--color-text-faint)] font-semibold uppercase tracking-wider text-[10px]">
                        <th className="py-2.5 px-3">Repository</th>
                        <th className="py-2.5 px-3">Visibility</th>
                        <th className="py-2.5 px-3">Default Branch</th>
                        <th className="py-2.5 px-3">Last Scan</th>
                        <th className="py-2.5 px-3 text-right">Monitoring</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[var(--color-border)]/40">
                      {filteredRepos.length > 0 ? (
                        filteredRepos.map((repo) => {
                          const isActing = actionLoadingId === repo.id;
                          return (
                            <tr key={repo.id} className="hover:bg-[var(--color-surface-2)] transition-colors">
                              <td className="py-3 px-3">
                                <div className="font-semibold text-[var(--color-text)] flex items-center gap-1.5">
                                  <span>{repo.fullName}</span>
                                  {repo.htmlUrl && (
                                    <a
                                      href={repo.htmlUrl}
                                      target="_blank"
                                      rel="noreferrer"
                                      className="text-[var(--color-text-faint)] hover:text-[var(--color-brand)]"
                                    >
                                      <ExternalLink size={11} />
                                    </a>
                                  )}
                                </div>
                                {repo.description && (
                                  <p className="text-[11px] text-[var(--color-text-faint)] line-clamp-1 max-w-md">
                                    {repo.description}
                                  </p>
                                )}
                              </td>
                              <td className="py-3 px-3">
                                <span className="text-[10px] font-medium px-2 py-0.5 rounded bg-[var(--color-surface-2)] border border-[var(--color-border)] text-[var(--color-text-faint)]">
                                  {repo.isPrivate ? "Private" : "Public"}
                                </span>
                              </td>
                              <td className="py-3 px-3 font-mono text-[11px]">
                                {repo.defaultBranch || "main"}
                              </td>
                              <td className="py-3 px-3">
                                {repo.lastScanStatus ? (
                                  <span
                                    className={"text-[10px] font-bold px-2 py-0.5 rounded " + (
                                      repo.lastScanStatus === "PASS" || repo.lastScanStatus === "PASSED"
                                        ? "bg-emerald-500/10 text-emerald-500 border border-emerald-500/20"
                                        : "bg-rose-500/10 text-rose-500 border border-rose-500/20"
                                    )}
                                  >
                                    {repo.lastScanStatus}
                                  </span>
                                ) : (
                                  <span className="text-[11px] text-[var(--color-text-faint)]">No scans yet</span>
                                )}
                              </td>
                              <td className="py-3 px-3 text-right">
                                <div className="flex items-center justify-end gap-2">
                                  {repo.isMonitored && (
                                    <button
                                      type="button"
                                      onClick={() => navigate("/ci-check")}
                                      className="text-xs text-[var(--color-brand)] hover:underline font-medium flex items-center gap-1 cursor-pointer"
                                    >
                                      CI Gate <ArrowRight size={12} />
                                    </button>
                                  )}
                                  <button
                                    type="button"
                                    disabled={isActing}
                                    onClick={() => handleToggleMonitoring(repo)}
                                    className={"text-xs font-semibold px-3 py-1 rounded-lg transition-all cursor-pointer " + (
                                      repo.isMonitored
                                        ? "bg-emerald-500/10 text-emerald-500 border border-emerald-500/30 hover:bg-rose-500/10 hover:text-rose-500 hover:border-rose-500/30"
                                        : "bg-[var(--color-surface-2)] text-[var(--color-text)] border border-[var(--color-border)] hover:border-[var(--color-brand)]"
                                    )}
                                  >
                                    {isActing ? (
                                      <Loader2 size={12} className="animate-spin inline" />
                                    ) : repo.isMonitored ? (
                                      "✓ Monitored"
                                    ) : (
                                      "+ Monitor"
                                    )}
                                  </button>
                                </div>
                              </td>
                            </tr>
                          );
                        })
                      ) : (
                        <tr>
                          <td colSpan={5} className="py-8 text-center text-xs text-[var(--color-text-faint)]">
                            No repositories found matching "{repoSearch}".
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Tab 3: Appearance & Themes */}
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
                className={"p-4 rounded-xl border text-left transition-all relative " + (
                  theme === "light"
                    ? "border-[var(--color-brand)] ring-2 ring-[var(--color-brand)] bg-white text-slate-900 shadow-md"
                    : "border-[var(--color-border)] hover:border-[var(--color-border-strong)] bg-[var(--color-surface)] text-[var(--color-text)]"
                )}
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
                className={"p-4 rounded-xl border text-left transition-all relative " + (
                  theme === "dark"
                    ? "border-[var(--color-brand)] ring-2 ring-[var(--color-brand)] bg-[#12161f] text-white shadow-md"
                    : "border-[var(--color-border)] hover:border-[var(--color-border-strong)] bg-[var(--color-surface)] text-[var(--color-text)]"
                )}
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
                className={"p-4 rounded-xl border text-left transition-all relative " + (
                  theme === "system"
                    ? "border-[var(--color-brand)] ring-2 ring-[var(--color-brand)] bg-[var(--color-surface)] text-[var(--color-text)] shadow-md"
                    : "border-[var(--color-border)] hover:border-[var(--color-border-strong)] bg-[var(--color-surface)] text-[var(--color-text)]"
                )}
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

        {/* Tab 4: Diagnostics */}
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

              <div className="py-3 flex items-center justify-between">
                <div>
                  <strong className="text-[var(--color-text)]">GitHub Webhook & OAuth Engine</strong>
                  <p className="text-[11px] text-[var(--color-text-faint)]">HMAC-SHA256 authenticated event receiver and commit compliance pipeline</p>
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
