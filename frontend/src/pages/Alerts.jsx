import { useEffect, useState } from "react";
import { Bell, ShieldAlert, ShieldCheck, RefreshCw, Loader2, Search } from "lucide-react";
import Topbar from "../components/layout/Topbar";
import Pagination from "../components/ui/Pagination";
import { auditApi } from "../api";
import { useQueryState } from "../hooks/useQueryState";

function relativeTime(ts) {
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

  // URL query state
  const [filter, setFilter] = useQueryState("filter", "ALL");
  const [search, setSearch] = useQueryState("search", "");
  const [page, setPage] = useQueryState("page", 1);
  const [pageSize, setPageSize] = useQueryState("size", 10);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const data = await auditApi.recent(100);
      setDecisions(data);
    } catch (err) {
      setError(err.message);
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
      d.sourceService.toLowerCase().includes(q) ||
      d.destinationService.toLowerCase().includes(q) ||
      (d.reason && d.reason.toLowerCase().includes(q)) ||
      (d.policyId && d.policyId.toLowerCase().includes(q));
    return matchFilter && matchSearch;
  });

  const denyCount = decisions.filter((d) => d.decision === "DENY").length;
  const paginatedDecisions = filtered.slice((page - 1) * pageSize, page * pageSize);

  return (
    <div>
      <Topbar title="Alerts" subtitle="Policy enforcement events that need your attention." />

      <div className="px-6 lg:px-8 mt-4 flex flex-wrap items-center gap-3 justify-between">
        <div className="flex items-center gap-3 flex-1 max-w-xl">
          {denyCount > 0 && (
            <span className="flex items-center gap-1.5 text-xs font-semibold px-3 py-2 rounded-xl bg-[var(--color-bad)]/10 border border-[var(--color-bad)]/30 text-[var(--color-bad)] shrink-0">
              <ShieldAlert size={14} />
              {denyCount} blocked flow{denyCount !== 1 ? "s" : ""}
            </span>
          )}

          <div className="relative flex-1 min-w-[180px]">
            <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--color-text-faint)]" />
            <input
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(1);
              }}
              placeholder="Search alert events..."
              className="field-input pl-9 text-xs"
            />
          </div>

          <button
            onClick={load}
            disabled={loading}
            className="flex items-center gap-1.5 text-xs text-[var(--color-text-dim)] hover:text-white border border-[var(--color-border)] rounded-xl px-3 py-2 transition-colors disabled:opacity-50"
          >
            <RefreshCw size={13} className={loading ? "animate-spin" : ""} />
            Refresh
          </button>
        </div>

        <div className="flex items-center gap-2">
          {["ALL", "DENY", "ALLOW"].map((f) => (
            <button
              key={f}
              onClick={() => {
                setFilter(f);
                setPage(1);
              }}
              className={`text-xs px-3 py-2 rounded-lg border transition-colors ${
                filter === f
                  ? "bg-[var(--color-brand)] border-[var(--color-brand)] text-white font-medium"
                  : "border-[var(--color-border)] text-[var(--color-text-dim)] hover:text-white bg-[var(--color-surface)]"
              }`}
            >
              {f}
            </button>
          ))}
        </div>
      </div>

      <div className="px-6 lg:px-8 mt-4 pb-12 space-y-4">
        {error && (
          <div className="rounded-xl bg-[var(--color-bad)]/10 border border-[var(--color-bad)]/30 px-4 py-3 text-sm text-[var(--color-bad)] flex items-center justify-between">
            {error}
            <button onClick={load} className="underline ml-4 text-xs">
              Retry
            </button>
          </div>
        )}

        {loading && (
          <div className="card px-5 py-12 text-center text-[var(--color-text-faint)]">
            <Loader2 size={18} className="animate-spin inline mr-2" /> Loading events…
          </div>
        )}

        {!loading && filtered.length === 0 && (
          <div className="card px-5 py-14 text-center">
            <Bell size={32} className="mx-auto mb-3 text-[var(--color-text-faint)]" />
            <p className="text-[var(--color-text-faint)] text-sm">
              {decisions.length === 0 ? "No enforcement events recorded yet." : `No ${filter} events in recent history.`}
            </p>
          </div>
        )}

        {!loading && filtered.length > 0 && (
          <div className="space-y-3">
            {paginatedDecisions.map((d) => (
              <div
                key={d.id}
                className={`card px-5 py-4 flex items-start gap-4 border-l-4 hover:bg-[var(--color-surface-2)] transition-colors ${
                  d.decision === "DENY" ? "border-l-[var(--color-bad)]" : "border-l-[var(--color-good)]"
                }`}
              >
                <div className="shrink-0 mt-0.5">
                  {d.decision === "DENY" ? (
                    <ShieldAlert size={18} className="text-[var(--color-bad)]" />
                  ) : (
                    <ShieldCheck size={18} className="text-[var(--color-good)]" />
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span
                      className={`text-xs font-bold px-2 py-0.5 rounded ${
                        d.decision === "DENY"
                          ? "bg-[var(--color-bad)]/15 text-[var(--color-bad)] border border-[var(--color-bad)]/30"
                          : "bg-[var(--color-good)]/15 text-[var(--color-good)] border border-[var(--color-good)]/30"
                      }`}
                    >
                      {d.decision}
                    </span>
                    <span className="font-semibold text-white text-sm font-mono">
                      {d.sourceService} → {d.destinationService}
                    </span>
                  </div>
                  <p className="text-xs text-[var(--color-text-dim)] mt-1">{d.reason}</p>
                  <div className="flex flex-wrap gap-3 mt-1.5 text-xs text-[var(--color-text-faint)]">
                    {d.sourceRegion && (
                      <span>
                        [{d.sourceRegion}] → [{d.destinationRegion}]
                      </span>
                    )}
                    <span className="font-semibold text-amber-400">{d.dataClass}</span>
                    {d.policyId && <span>• Policy: {d.policyId}</span>}
                  </div>
                </div>
                <div className="shrink-0 text-right">
                  <p className="text-xs text-[var(--color-text-faint)] font-mono">{relativeTime(d.createdAt)}</p>
                  <p className="text-xs text-[var(--color-text-faint)] mt-0.5">#{d.id}</p>
                </div>
              </div>
            ))}

            <div className="card overflow-hidden">
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
        )}
      </div>
    </div>
  );
}
