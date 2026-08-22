import { useEffect, useState } from "react";
import { Bell, ShieldAlert, ShieldCheck, RefreshCw, Loader2 } from "lucide-react";
import Topbar from "../components/layout/Topbar";
import { auditApi } from "../api";

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
  const [filter, setFilter] = useState("DENY");

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const data = await auditApi.recent(50);
      setDecisions(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  const filtered = filter === "ALL" ? decisions : decisions.filter((d) => d.decision === filter);
  const denyCount = decisions.filter((d) => d.decision === "DENY").length;

  return (
    <div>
      <Topbar
        title="Alerts"
        subtitle="Policy enforcement events that need your attention."
      />

      <div className="px-6 lg:px-8 mt-4 flex flex-wrap items-center gap-3 justify-between">
        <div className="flex items-center gap-3">
          {denyCount > 0 && (
            <span className="flex items-center gap-1.5 text-sm font-medium px-3 py-1.5 rounded-xl bg-[var(--color-bad)]/10 border border-[var(--color-bad)]/30 text-[var(--color-bad)]">
              <ShieldAlert size={14} />
              {denyCount} blocked flow{denyCount !== 1 ? "s" : ""}
            </span>
          )}
          <button
            onClick={load}
            disabled={loading}
            className="flex items-center gap-1.5 text-xs text-[var(--color-text-dim)] hover:text-white border border-[var(--color-border)] rounded-xl px-3 py-1.5 transition-colors disabled:opacity-50"
          >
            <RefreshCw size={13} className={loading ? "animate-spin" : ""} />
            Refresh
          </button>
        </div>

        <div className="flex items-center gap-2">
          {["ALL", "DENY", "ALLOW"].map((f) => (
            <button
              key={f}
              onClick={() => setFilter(f)}
              className={`text-xs px-3 py-1.5 rounded-lg border transition-colors ${
                filter === f
                  ? "bg-[var(--color-brand)] border-[var(--color-brand)] text-white font-medium"
                  : "border-[var(--color-border)] text-[var(--color-text-dim)] hover:text-white"
              }`}
            >
              {f}
            </button>
          ))}
        </div>
      </div>

      <div className="px-6 lg:px-8 mt-4 pb-8">
        {error && (
          <div className="rounded-xl bg-[var(--color-bad)]/10 border border-[var(--color-bad)]/30 px-4 py-3 text-sm text-[var(--color-bad)] mb-4 flex items-center justify-between">
            {error}
            <button onClick={load} className="underline ml-4 text-xs">Retry</button>
          </div>
        )}

        {loading && (
          <div className="card px-5 py-12 text-center text-[var(--color-text-faint)]">
            <Loader2 size={18} className="animate-spin inline mr-2" />Loading events…
          </div>
        )}

        {!loading && filtered.length === 0 && (
          <div className="card px-5 py-14 text-center">
            <Bell size={32} className="mx-auto mb-3 text-[var(--color-text-faint)]" />
            <p className="text-[var(--color-text-faint)] text-sm">
              {decisions.length === 0
                ? "No enforcement events recorded yet."
                : `No ${filter} events in the recent history.`}
            </p>
          </div>
        )}

        {!loading && filtered.length > 0 && (
          <div className="space-y-3">
            {filtered.map((d) => (
              <div
                key={d.id}
                className={`card px-5 py-4 flex items-start gap-4 border-l-4 ${
                  d.decision === "DENY"
                    ? "border-l-[var(--color-bad)]"
                    : "border-l-[var(--color-good)]"
                }`}
              >
                <div className="shrink-0 mt-0.5">
                  {d.decision === "DENY"
                    ? <ShieldAlert size={18} className="text-[var(--color-bad)]" />
                    : <ShieldCheck size={18} className="text-[var(--color-good)]" />}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className={`text-xs font-bold px-2 py-0.5 rounded ${
                      d.decision === "DENY"
                        ? "bg-[var(--color-bad)]/15 text-[var(--color-bad)]"
                        : "bg-[var(--color-good)]/15 text-[var(--color-good)]"
                    }`}>
                      {d.decision}
                    </span>
                    <span className="font-medium text-white text-sm">
                      {d.sourceService} → {d.destinationService}
                    </span>
                  </div>
                  <p className="text-xs text-[var(--color-text-dim)] mt-1">{d.reason}</p>
                  <div className="flex flex-wrap gap-3 mt-1.5 text-xs text-[var(--color-text-faint)]">
                    {d.sourceRegion && <span>{d.sourceRegion} → {d.destinationRegion}</span>}
                    <span>{d.dataClass}</span>
                    {d.policyId && <span>· {d.policyId}</span>}
                  </div>
                </div>
                <div className="shrink-0 text-right">
                  <p className="text-xs text-[var(--color-text-faint)]">{relativeTime(d.createdAt)}</p>
                  <p className="text-xs text-[var(--color-text-faint)] mt-0.5">#{d.id}</p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
