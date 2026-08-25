import { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  GitBranch,
  GitCommit,
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  Search,
  ExternalLink,
  Trash2,
  Loader2,
  ArrowRight,
  Radio,
  CheckSquare,
  Activity,
  Layers,
} from "lucide-react";
import Topbar from "../components/layout/Topbar";
import Button from "../components/ui/Button";
import { githubApi } from "../api";

export default function GitHubScans() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [githubAccount, setGithubAccount] = useState(null);
  const [githubRepos, setGithubRepos] = useState([]);
  const [recentCommits, setRecentCommits] = useState([]);
  const [webhookDeliveries, setWebhookDeliveries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoadingId, setActionLoadingId] = useState(null);
  const [repoSearch, setRepoSearch] = useState("");
  const [filterVisibility, setFilterVisibility] = useState("all");
  const [message, setMessage] = useState(null);
  const [errorMsg, setErrorMsg] = useState(null);

  const oauthStatus = searchParams.get("status");
  const oauthError = searchParams.get("error");
  const oauthUsername = searchParams.get("username");

  useEffect(() => {
    if (oauthStatus === "connected") {
      setMessage(`GitHub account connected successfully${oauthUsername ? " as @" + oauthUsername : ""}!`);
    } else if (oauthError) {
      setErrorMsg(`GitHub authorization failed: ${oauthError}`);
    }
  }, [oauthStatus, oauthError, oauthUsername]);

  async function loadData() {
    setLoading(true);
    try {
      const [accRes, reposRes, commitsRes, deliveriesRes] = await Promise.allSettled([
        githubApi.getAccount(),
        githubApi.listRepositories(),
        githubApi.listCommits(0, 15),
        githubApi.listDeliveries(0, 10),
      ]);

      if (accRes.status === "fulfilled") setGithubAccount(accRes.value);
      if (reposRes.status === "fulfilled") setGithubRepos(Array.isArray(reposRes.value) ? reposRes.value : []);
      if (commitsRes.status === "fulfilled") setRecentCommits(Array.isArray(commitsRes.value) ? commitsRes.value : []);
      if (deliveriesRes.status === "fulfilled") setWebhookDeliveries(Array.isArray(deliveriesRes.value) ? deliveriesRes.value : []);
    } catch (e) {
      setErrorMsg(e.message || "Failed loading GitHub integration data.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
  }, []);

  async function handleConnectGitHub() {
    try {
      const res = await githubApi.connect();
      if (res.authorizationUrl) {
        window.location.href = res.authorizationUrl;
      }
    } catch (err) {
      setErrorMsg(err.message || "Failed to initialize GitHub OAuth flow.");
    }
  }

  async function handleDisconnectGitHub() {
    if (!window.confirm("Are you sure you want to disconnect GitHub? Monitored repositories will be unlinked.")) {
      return;
    }
    try {
      await githubApi.disconnect();
      setGithubAccount({ connected: false });
      setGithubRepos([]);
      setMessage("GitHub account disconnected and tokens securely deleted.");
    } catch (err) {
      setErrorMsg(err.message || "Failed disconnecting GitHub account.");
    }
  }

  async function handleToggleMonitor(repo) {
    setActionLoadingId(repo.id);
    try {
      if (repo.isMonitored) {
        await githubApi.disableMonitoring(repo.id);
        setGithubRepos((prev) =>
          prev.map((r) => (r.id === repo.id ? { ...r, isMonitored: false } : r))
        );
        setMessage(`Disabled monitoring for ${repo.fullName}.`);
      } else {
        await githubApi.enableMonitoring(repo.id, {
          fullName: repo.fullName,
          name: repo.name,
          ownerLogin: repo.ownerLogin,
          defaultBranch: repo.defaultBranch,
          isPrivate: String(repo.isPrivate),
        });
        setGithubRepos((prev) =>
          prev.map((r) => (r.id === repo.id ? { ...r, isMonitored: true } : r))
        );
        setMessage(`Enabled automated compliance monitoring on ${repo.fullName}. Webhook is active.`);
      }
    } catch (err) {
      setErrorMsg(err.message || "Failed updating repository monitoring status.");
    } finally {
      setActionLoadingId(null);
    }
  }

  const filteredRepos = githubRepos.filter((r) => {
    const matchesSearch =
      r.fullName?.toLowerCase().includes(repoSearch.toLowerCase()) ||
      r.name?.toLowerCase().includes(repoSearch.toLowerCase()) ||
      (r.description && r.description.toLowerCase().includes(repoSearch.toLowerCase()));

    if (filterVisibility === "monitored") return matchesSearch && r.isMonitored;
    if (filterVisibility === "private") return matchesSearch && r.isPrivate;
    if (filterVisibility === "public") return matchesSearch && !r.isPrivate;
    return matchesSearch;
  });

  const monitoredCount = githubRepos.filter((r) => r.isMonitored).length;

  return (
    <div className="space-y-6 pb-12">
      <Topbar
        title="GitHub Integration & Scans"
        subtitle="Manage GitHub connections, configure automated repository monitoring, and inspect push compliance decisions."
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="secondary"
              size="sm"
              onClick={loadData}
              loading={loading}
              icon={RefreshCw}
            >
              Sync
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={() => navigate("/ci-check")}
              icon={CheckSquare}
            >
              CI Gate
            </Button>
          </div>
        }
      />

      <div className="px-6 space-y-6 max-w-7xl">
        {/* Status Alerts */}
        {message && (
          <div className="p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-500 text-xs flex items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <CheckCircle2 size={16} />
              <span>{message}</span>
            </div>
            <button
              onClick={() => {
                setMessage(null);
                setSearchParams({});
              }}
              className="text-xs hover:underline cursor-pointer"
            >
              Dismiss
            </button>
          </div>
        )}

        {errorMsg && (
          <div className="p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-500 text-xs flex items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <AlertCircle size={16} />
              <span>{errorMsg}</span>
            </div>
            <button
              onClick={() => {
                setErrorMsg(null);
                setSearchParams({});
              }}
              className="text-xs hover:underline cursor-pointer"
            >
              Dismiss
            </button>
          </div>
        )}

        {/* Top Overview KPI Cards */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="card p-4 space-y-1">
            <p className="text-[11px] font-semibold text-[var(--color-text-faint)] uppercase tracking-wider">GitHub Connection</p>
            <div className="flex items-center justify-between">
              <p className="text-base font-bold text-[var(--color-text)]">
                {githubAccount?.connected ? `@${githubAccount.username}` : "Disconnected"}
              </p>
              <span className={`w-2.5 h-2.5 rounded-full ${githubAccount?.connected ? "bg-emerald-500 animate-pulse" : "bg-zinc-600"}`} />
            </div>
            <p className="text-[11px] text-[var(--color-text-faint)]">
              {githubAccount?.connected ? "OAuth 2.0 Authenticated" : "Not connected"}
            </p>
          </div>

          <div className="card p-4 space-y-1">
            <p className="text-[11px] font-semibold text-[var(--color-text-faint)] uppercase tracking-wider">Monitored Repos</p>
            <p className="text-2xl font-bold text-[var(--color-text)]">{monitoredCount}</p>
            <p className="text-[11px] text-[var(--color-text-faint)]">of {githubRepos.length} accessible repositories</p>
          </div>

          <div className="card p-4 space-y-1">
            <p className="text-[11px] font-semibold text-[var(--color-text-faint)] uppercase tracking-wider">Recent Commits Scanned</p>
            <p className="text-2xl font-bold text-[var(--color-text)]">{recentCommits.length}</p>
            <p className="text-[11px] text-[var(--color-text-faint)]">Automated push scans recorded</p>
          </div>

          <div className="card p-4 space-y-1">
            <p className="text-[11px] font-semibold text-[var(--color-text-faint)] uppercase tracking-wider">Webhook Engine</p>
            <div className="flex items-center gap-1.5 text-emerald-500 font-bold text-sm">
              <Radio size={14} className="animate-pulse" />
              <span>Live & Active</span>
            </div>
            <p className="text-[11px] text-[var(--color-text-faint)]">HMAC-SHA256 auto-verification</p>
          </div>
        </div>

        {/* Main Connect Banner / Account Card */}
        <div className="card p-6">
          {loading ? (
            <div className="flex items-center justify-center py-8 gap-3 text-xs text-[var(--color-text-faint)]">
              <Loader2 size={18} className="animate-spin text-[var(--color-brand)]" />
              <span>Loading GitHub connection status...</span>
            </div>
          ) : githubAccount?.connected ? (
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
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
                  variant="secondary"
                  size="sm"
                  onClick={loadData}
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
                  Disconnect Account
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
                    Authorize PolicyMesh to discover your repositories and automatically evaluate git push events against active data residency policies.
                  </p>
                  <div className="flex flex-wrap items-center gap-3 pt-1 text-[11px] text-[var(--color-text-faint)]">
                    <span className="flex items-center gap-1">
                      <ShieldCheck size={13} className="text-emerald-500" /> Read-only access
                    </span>
                    <span className="flex items-center gap-1">
                      <ShieldCheck size={13} className="text-emerald-500" /> AES-256 encrypted at rest
                    </span>
                    <span className="flex items-center gap-1">
                      <ShieldCheck size={13} className="text-emerald-500" /> Zero manual webhook setup
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
                    <div>GITHUB_REDIRECT_URI=https://policymesh-komp.onrender.com/api/v1/github/callback</div>
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

        {/* Monitored Repositories Table */}
        {githubAccount?.connected && (
          <div className="card p-5 space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-[var(--color-border)]/50 pb-3">
              <div>
                <h3 className="font-bold text-sm text-[var(--color-text)]">
                  Monitored Repositories ({monitoredCount} of {githubRepos.length} active)
                </h3>
                <p className="text-xs text-[var(--color-text-faint)] mt-0.5">
                  PolicyMesh automatically provisions push webhooks on GitHub when you enable monitoring on a repository.
                </p>
              </div>

              <div className="flex items-center gap-2">
                <select
                  value={filterVisibility}
                  onChange={(e) => setFilterVisibility(e.target.value)}
                  className="field-input text-xs py-1.5 px-2.5 w-32"
                >
                  <option value="all">All ({githubRepos.length})</option>
                  <option value="monitored">Monitored ({monitoredCount})</option>
                  <option value="public">Public</option>
                  <option value="private">Private</option>
                </select>

                <div className="w-48 sm:w-60 relative">
                  <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--color-text-faint)]" />
                  <input
                    type="text"
                    value={repoSearch}
                    onChange={(e) => setRepoSearch(e.target.value)}
                    placeholder="Search repositories..."
                    className="field-input pl-8 text-xs py-1.5"
                  />
                </div>
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
                              <div className="space-y-0.5">
                                <span
                                  className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                                    repo.lastScanStatus === "PASS" || repo.lastScanStatus === "PASSED"
                                      ? "bg-emerald-500/10 text-emerald-500 border border-emerald-500/20"
                                      : "bg-rose-500/10 text-rose-500 border border-rose-500/20"
                                  }`}
                                >
                                  {repo.lastScanStatus}
                                </span>
                                {repo.lastCommitSha && (
                                  <p className="text-[10px] font-mono text-[var(--color-text-faint)]">
                                    {repo.lastCommitSha.slice(0, 7)}
                                  </p>
                                )}
                              </div>
                            ) : (
                              <span className="text-[11px] text-[var(--color-text-faint)]">No scans yet</span>
                            )}
                          </td>
                          <td className="py-3 px-3 text-right">
                            <button
                              disabled={isActing}
                              onClick={() => handleToggleMonitor(repo)}
                              className={`px-3 py-1 rounded-lg text-xs font-semibold transition-all cursor-pointer ${
                                repo.isMonitored
                                  ? "bg-emerald-500/15 text-emerald-500 border border-emerald-500/30 hover:bg-rose-500/15 hover:text-rose-500 hover:border-rose-500/30"
                                  : "bg-[var(--color-surface-2)] hover:bg-[var(--color-brand-light)] text-[var(--color-text)] hover:text-[var(--color-brand-text)] border border-[var(--color-border)]"
                              }`}
                            >
                              {isActing ? (
                                <Loader2 size={12} className="animate-spin inline mr-1" />
                              ) : repo.isMonitored ? (
                                "✓ Monitored"
                              ) : (
                                "+ Enable"
                              )}
                            </button>
                          </td>
                        </tr>
                      );
                    })
                  ) : (
                    <tr>
                      <td colSpan={5} className="py-8 text-center text-xs text-[var(--color-text-faint)]">
                        No repositories matching "{repoSearch}".
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* Automated Push Scans Feed */}
        <div className="card p-5 space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-[var(--color-border)]/50 pb-3">
            <div className="flex items-center gap-2">
              <GitCommit size={17} className="text-[var(--color-brand)]" />
              <h3 className="font-bold text-sm text-[var(--color-text)]">
                Recent Automated GitHub Push Scans
              </h3>
            </div>
            <button
              onClick={() => navigate("/ci-check")}
              className="text-xs text-[var(--color-brand)] hover:underline font-medium flex items-center gap-1 cursor-pointer"
            >
              Open CI Gate <ArrowRight size={13} />
            </button>
          </div>

          {recentCommits.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3.5">
              {recentCommits.map((c, i) => {
                const isPassed = c.status === "PASS" || c.status === "PASSED";
                const isAllowed = c.finalDecision?.mergeAllowed ?? isPassed;
                const shortSha = c.commitHash ? (c.commitHash.length > 7 ? c.commitHash.slice(0, 7) : c.commitHash) : "HEAD";

                return (
                  <div
                    key={c.id || i}
                    onClick={() => navigate(`/ci-check?commit=${c.commitHash || ""}`)}
                    className="p-4 rounded-xl bg-[var(--color-surface-2)] hover:bg-[var(--color-surface-3)] border border-[var(--color-border)]/60 transition-all cursor-pointer space-y-3 group shadow-xs"
                  >
                    <div className="flex items-center justify-between gap-2">
                      <div className="flex items-center gap-1.5 font-mono text-xs font-semibold text-[var(--color-text)]">
                        <GitCommit size={13} className="text-[var(--color-brand)]" />
                        <span>{shortSha}</span>
                        <span className="text-[10px] font-sans font-medium px-1.5 py-0.5 rounded bg-[var(--color-surface)] border border-[var(--color-border)] text-[var(--color-text-faint)]">
                          {c.branch || "main"}
                        </span>
                      </div>
                      <span
                        className={`text-[10px] font-bold px-2 py-0.5 rounded-md ${
                          isAllowed
                            ? "bg-emerald-500/10 text-emerald-500 border border-emerald-500/20"
                            : "bg-rose-500/10 text-rose-500 border border-rose-500/20"
                        }`}
                      >
                        {isAllowed ? "MERGE ALLOWED" : "MERGE BLOCKED"}
                      </span>
                    </div>

                    <p className="text-xs text-[var(--color-text)] line-clamp-2 group-hover:text-[var(--color-brand)] transition-colors font-medium">
                      {c.commitMessage || `Commit @ ${shortSha}`}
                    </p>

                    <div className="flex items-center justify-between text-[11px] text-[var(--color-text-faint)] pt-2 border-t border-[var(--color-border)]/40">
                      <span>{c.author || "Developer"}</span>
                      <span>{c.startedAt ? new Date(c.startedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : "Just now"}</span>
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="p-8 rounded-xl bg-[var(--color-surface-2)] border border-[var(--color-border)]/40 text-center space-y-1">
              <GitCommit size={28} className="mx-auto text-[var(--color-text-faint)]" />
              <p className="text-xs font-semibold text-[var(--color-text)]">Awaiting GitHub Push Events</p>
              <p className="text-xs text-[var(--color-text-muted)] max-w-md mx-auto">
                Push commits to your monitored GitHub repository to trigger instant cryptographic webhook analysis and compliance decisions.
              </p>
            </div>
          )}
        </div>

        {/* Webhook Deliveries Audit Log */}
        {webhookDeliveries.length > 0 && (
          <div className="card p-5 space-y-4">
            <h3 className="font-bold text-sm text-[var(--color-text)] flex items-center gap-2">
              <Activity size={16} className="text-[var(--color-brand)]" />
              Recent Webhook Deliveries Audit Trail
            </h3>

            <div className="overflow-x-auto">
              <table className="w-full text-xs text-left">
                <thead>
                  <tr className="border-b border-[var(--color-border)] text-[var(--color-text-faint)] font-semibold uppercase tracking-wider text-[10px]">
                    <th className="py-2.5 px-3">Delivery ID</th>
                    <th className="py-2.5 px-3">Event</th>
                    <th className="py-2.5 px-3">Repository / Branch</th>
                    <th className="py-2.5 px-3">Status</th>
                    <th className="py-2.5 px-3">Received</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--color-border)]/40">
                  {webhookDeliveries.map((d) => (
                    <tr key={d.id} className="hover:bg-[var(--color-surface-2)]">
                      <td className="py-2.5 px-3 font-mono text-[11px] text-[var(--color-text)]">
                        {d.deliveryId ? d.deliveryId.slice(0, 8) + "..." : "—"}
                      </td>
                      <td className="py-2.5 px-3">
                        <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-[var(--color-surface-2)] border border-[var(--color-border)]">
                          {d.eventType}
                        </span>
                      </td>
                      <td className="py-2.5 px-3">
                        {d.repository ? `${d.repository} (${d.branchRef || "main"})` : "—"}
                      </td>
                      <td className="py-2.5 px-3">
                        <span
                          className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                            d.status === "COMPLETED" || d.status === "ACCEPTED"
                              ? "bg-emerald-500/10 text-emerald-500 border border-emerald-500/20"
                              : d.status === "FAILED"
                              ? "bg-rose-500/10 text-rose-500 border border-rose-500/20"
                              : "bg-zinc-500/10 text-zinc-400 border border-zinc-500/20"
                          }`}
                        >
                          {d.status}
                        </span>
                      </td>
                      <td className="py-2.5 px-3 text-[var(--color-text-faint)]">
                        {d.receivedAt ? new Date(d.receivedAt).toLocaleTimeString() : "—"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
