import { useEffect, useState } from "react";
import { Link2, ShieldCheck, ShieldAlert, RefreshCw, Loader2, ChevronDown, ChevronUp, CheckCircle, XCircle } from "lucide-react";
import Topbar from "../components/layout/Topbar";
import { lineageApi } from "../api";

export default function Lineage() {
  const [records, setRecords] = useState([]);
  const [verification, setVerification] = useState(null);
  const [loading, setLoading] = useState(true);
  const [verifying, setVerifying] = useState(false);
  const [error, setError] = useState(null);
  const [filter, setFilter] = useState("ALL");
  const [expanded, setExpanded] = useState(null);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const data = await lineageApi.list();
      setRecords(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function verify() {
    setVerifying(true);
    try {
      const v = await lineageApi.verify();
      setVerification(v);
    } catch (err) {
      setError(err.message);
    } finally {
      setVerifying(false);
    }
  }

  useEffect(() => {
    load();
    verify();
  }, []);

  const filtered = filter === "ALL" ? records : records.filter((r) => r.decision === filter);

  return (
    <div>
      <Topbar
        title="Lineage"
        subtitle="Cryptographic hash-chain audit evidence for every enforcement decision."
      />

      <div className="px-6 lg:px-8 mt-4 flex flex-wrap items-center gap-3 justify-between">
        {/* Verification status */}
        <div className="flex items-center gap-3">
          {verification && (
            <div className={`flex items-center gap-2 text-sm font-medium px-4 py-2 rounded-xl border ${
              verification.valid
                ? "bg-[var(--color-good)]/10 border-[var(--color-good)]/30 text-[var(--color-good)]"
                : "bg-[var(--color-bad)]/10 border-[var(--color-bad)]/30 text-[var(--color-bad)]"
            }`}>
              {verification.valid
                ? <CheckCircle size={15} />
                : <XCircle size={15} />}
              {verification.valid
                ? `Chain valid · ${verification.recordsChecked} records`
                : `Chain broken at record #${verification.brokenAt}`}
            </div>
          )}
          <button
            onClick={verify}
            disabled={verifying}
            className="flex items-center gap-1.5 text-xs text-[var(--color-text-dim)] hover:text-white border border-[var(--color-border)] rounded-xl px-3 py-1.5 transition-colors disabled:opacity-50"
          >
            <RefreshCw size={13} className={verifying ? "animate-spin" : ""} />
            Verify Chain
          </button>
        </div>

        {/* Filter */}
        <div className="flex items-center gap-2">
          {["ALL", "ALLOW", "DENY"].map((f) => (
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
            <Loader2 size={20} className="animate-spin inline mr-2" />Loading lineage records…
          </div>
        )}

        {!loading && filtered.length === 0 && (
          <div className="card px-5 py-12 text-center">
            <Link2 size={32} className="mx-auto mb-3 text-[var(--color-text-faint)]" />
            <p className="text-[var(--color-text-faint)]">
              {records.length === 0
                ? "No lineage records yet. Run enforcement checks to create audit entries."
                : `No ${filter} decisions in the lineage chain.`}
            </p>
          </div>
        )}

        {!loading && filtered.length > 0 && (
          <div className="card overflow-hidden divide-y divide-[var(--color-border)]">
            {filtered.map((r) => (
              <div key={r.id} className="hover:bg-[var(--color-surface-2)] transition-colors">
                <div
                  className="px-5 py-3.5 flex items-center gap-4 cursor-pointer"
                  onClick={() => setExpanded(expanded === r.id ? null : r.id)}
                >
                  {/* Decision badge */}
                  <span className={`text-xs font-bold px-2 py-0.5 rounded shrink-0 ${
                    r.decision === "ALLOW"
                      ? "bg-[var(--color-good)]/15 text-[var(--color-good)]"
                      : "bg-[var(--color-bad)]/15 text-[var(--color-bad)]"
                  }`}>
                    {r.decision}
                  </span>

                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-medium text-white truncate">
                        {r.sourceService} [{r.sourceRegion ?? "?"}]
                      </span>
                      <span className="text-[var(--color-text-faint)]">→</span>
                      <span className="text-sm font-medium text-white truncate">
                        {r.destinationService} [{r.destinationRegion ?? "?"}]
                      </span>
                    </div>
                    <div className="flex items-center gap-3 mt-0.5">
                      <span className="text-xs text-[var(--color-text-faint)]">{r.dataClass}</span>
                      {r.policyId && <span className="text-xs text-[var(--color-text-faint)]">· {r.policyId}</span>}
                      <span className="text-xs text-[var(--color-text-faint)]">
                        · #{r.id} · {new Date(r.createdAt).toLocaleString()}
                      </span>
                    </div>
                  </div>

                  {/* Hash preview */}
                  <div className="hidden lg:flex items-center gap-2 shrink-0">
                    <span className="font-mono text-xs text-[var(--color-text-faint)]">
                      {r.currentHash?.slice(0, 12)}…
                    </span>
                    {expanded === r.id ? <ChevronUp size={14} className="text-[var(--color-text-faint)]" /> : <ChevronDown size={14} className="text-[var(--color-text-faint)]" />}
                  </div>
                  <div className="lg:hidden">
                    {expanded === r.id ? <ChevronUp size={14} className="text-[var(--color-text-faint)]" /> : <ChevronDown size={14} className="text-[var(--color-text-faint)]" />}
                  </div>
                </div>

                {expanded === r.id && (
                  <div className="px-5 pb-4 grid grid-cols-1 sm:grid-cols-2 gap-4 bg-[var(--color-surface-2)]/50">
                    <div className="space-y-2 text-xs">
                      <HashRow label="Decision ID" value={`#${r.decisionId}`} />
                      <HashRow label="Lineage ID" value={`#${r.id}`} />
                      <HashRow label="Policy" value={r.policyId || "—"} />
                      <HashRow label="Reason" value={r.reason} />
                    </div>
                    <div className="space-y-2 text-xs">
                      <div>
                        <p className="text-[var(--color-text-faint)] mb-1">Previous Hash</p>
                        <p className="font-mono text-[var(--color-text-dim)] break-all leading-relaxed">
                          {r.previousHash || "GENESIS"}
                        </p>
                      </div>
                      <div>
                        <p className="text-[var(--color-text-faint)] mb-1">Current Hash</p>
                        <p className="font-mono text-[var(--color-text-dim)] break-all leading-relaxed">
                          {r.currentHash}
                        </p>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function HashRow({ label, value }) {
  return (
    <div className="flex gap-2">
      <span className="text-[var(--color-text-faint)] shrink-0 w-24">{label}</span>
      <span className="text-[var(--color-text-dim)]">{value}</span>
    </div>
  );
}
