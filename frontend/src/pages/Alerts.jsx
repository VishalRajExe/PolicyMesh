import { useEffect, useState } from "react";
import { Bell, ShieldAlert, ShieldCheck, RefreshCw, Loader2, Search, AlertTriangle } from "lucide-react";
import Topbar from "../components/layout/Topbar";
import Pagination from "../components/ui/Pagination";
import Button from "../components/ui/Button";
import Badge from "../components/ui/Badge";
import EmptyState from "../components/ui/EmptyState";
import { auditApi } from "../api";

function relativeTime(ts) {
  if (!ts) return "recently";
  const diff = Date.now() - new Date(ts).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return "just now";
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  return `${Math.floor(hrs / 24)}d ago`;
}

export default function Alerts() {
  const [decisions, setDecisions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filters
  const [filter, setFilter] = useState("ALL");
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const data = await auditApi.recent(100);
      setDecisions(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || "Failed to load alerts");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  const filtered = decisions.filter((d) => {
    const matchFilter = filter === "ALL" || d.decision === filter;
    const q = search.trim().toLowerCase();
    const matchSearch =
      !q ||
      (d.sourceService || "").toLowerCase().includes(q) ||
      (d.destinationService || "").toLowerCase().includes(q) ||
      (d.reason && d.reason.toLowerCase().includes(q)) ||
      (d.policyId && d.policyId.toLowerCase().includes(q));
    return matchFilter && matchSearch;
  });

  const denyCount = decisions.filter((d) => d.decision === "DENY").length;
  const paginatedDecisions = filtered.slice((page - 1) * pageSize, page * pageSize);

  const topActions = (
    <Button
      variant="secondary"
      size="md"
      icon={loading ? Loader2 : RefreshCw}
      onClick={load}
      disabled={loading}
    >
      {loading ? "Refreshing..." : "Refresh Alerts"}
    </Button>
  );

  return (
    <div>
      <Topbar
        title="Security & Compliance Alerts"
        subtitle="Real-time notifications and telemetry for blocked cross-border transfers and policy exceptions."
        actions={topActions}
      />

      <div className="px-6 lg:px-8 py-6 space-y-4 pb-12">
        {/* Controls & Search */}
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3 flex-1 max-w-xl">
            {denyCount > 0 && (
              <span className="flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded-xl bg-[var(--color-bad-light)] border border-[var(--color-bad)]/30 text-[var(--color-bad-text)] shrink-0 shadow-2xs">
                <ShieldAlert size={14} className="text-[var(--color-bad)]" />
                {denyCount} blocked flow{denyCount !== 1 ? "s" : ""}
              </span>
            )}

            <div className="relative flex-1 min-w-[180px]">
              <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--color-text-faint)]" />
              <input
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(1);
                }}
                placeholder="Search alerts by service or reason..."
                className="field-input pl-8 text-xs"
              />
            </div>
          </div>

          <div className="flex items-center gap-1.5 bg-[var(--color-surface-2)] p-1 rounded-xl border border-[var(--color-border)]">
            {[
              { id: "ALL", label: "All Events" },
              { id: "DENY", label: "Blocked" },
              { id: "ALLOW", label: "Allowed" },
            ].map(({ id, label }) => (
              <button
                key={id}
                onClick={() => {
                  setFilter(id);
                  setPage(1);
                }}
                className={`px-3 py-1 text-xs rounded-lg font-medium transition-all ${
                  filter === id
                    ? "bg-[var(--color-surface)] text-[var(--color-text)] font-semibold shadow-xs"
                    : "text-[var(--color-text-dim)] hover:text-[var(--color-text)]"
                }`}
              >
                {label}
              </button>
            ))}
          </div>
        </div>

        {/* Alerts Stream Card */}
        <div className="card overflow-hidden">
          {loading ? (
            <div className="p-8 text-center text-xs text-[var(--color-text-faint)] flex items-center justify-center gap-2">
              <Loader2 size={16} className="animate-spin text-[var(--color-brand)]" />
              <span>Loading real-time alerts...</span>
            </div>
          ) : filtered.length === 0 ? (
            <EmptyState
              icon={Bell}
              title="No alerts found"
              description="No runtime policy violations match your current filters."
            />
          ) : (
            <div className="divide-y divide-[var(--color-border)]">
              {paginatedDecisions.map((d, idx) => {
                const isDeny = d.decision === "DENY";

                return (
                  <div
                    key={d.id || idx}
                    className={`p-4 flex items-start justify-between gap-4 transition-colors ${
                      isDeny
                        ? "bg-rose-500/5 hover:bg-rose-500/10"
                        : "hover:bg-[var(--color-surface-2)]/50"
                    }`}
                  >
                    <div className="flex items-start gap-3 min-w-0">
                      <div
                        className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 mt-0.5 shadow-2xs ${
                          isDeny ? "icon-box-red" : "icon-box-green"
                        }`}
                      >
                        {isDeny ? <ShieldAlert size={18} /> : <ShieldCheck size={18} />}
                      </div>

                      <div className="min-w-0">
                        <div className="flex items-center gap-2 text-xs">
                          <span className="font-bold text-[var(--color-text)] font-mono">
                            {d.sourceService} → {d.destinationService}
                          </span>
                          <Badge variant="warn" size="sm">
                            {d.dataClass || "PII"}
                          </Badge>
                        </div>

                        <p className="text-xs text-[var(--color-text-dim)] mt-1 font-mono text-[11px] leading-relaxed">
                          {d.reason || (isDeny ? "Blocked by policy gate" : "Authorized transfer")}
                        </p>

                        <div className="flex items-center gap-3 mt-1.5 text-[11px] text-[var(--color-text-faint)]">
                          <span>Policy: <strong className="text-[var(--color-text-dim)]">{d.policyId || "DEFAULT_GATE"}</strong></span>
                          <span>•</span>
                          <span>{d.createdAt ? new Date(d.createdAt).toLocaleString() : "recently"}</span>
                        </div>
                      </div>
                    </div>

                    <div className="flex flex-col items-end gap-1.5 shrink-0">
                      <Badge variant={isDeny ? "bad" : "good"} size="sm" dot>
                        {d.decision}
                      </Badge>
                      <span className="text-[11px] text-[var(--color-text-faint)] font-medium">
                        {relativeTime(d.createdAt)}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          <Pagination
            currentPage={page}
            totalItems={filtered.length}
            pageSize={pageSize}
            onPageChange={setPage}
            onPageSizeChange={(sz) => {
              setPageSize(sz);
              setPage(1);
            }}
          />
        </div>
      </div>
    </div>
  );
}
