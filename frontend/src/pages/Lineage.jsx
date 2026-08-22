import { useEffect, useState } from "react";
import {
  Link2,
  ShieldCheck,
  ShieldAlert,
  RefreshCw,
  Loader2,
  ChevronDown,
  ChevronUp,
  CheckCircle,
  XCircle,
  Search,
} from "lucide-react";
import Topbar from "../components/layout/Topbar";
import Pagination from "../components/ui/Pagination";
import { lineageApi } from "../api";
import { useQueryState } from "../hooks/useQueryState";

export default function Lineage() {
  const [records, setRecords] = useState([]);
  const [verification, setVerification] = useState(null);
  const [loading, setLoading] = useState(true);
  const [verifying, setVerifying] = useState(false);
  const [error, setError] = useState(null);
  const [expanded, setExpanded] = useState(null);

  // URL query state
  const [search, setSearch] = useQueryState("search", "");
  const [filter, setFilter] = useQueryState("filter", "ALL");
  const [page, setPage] = useQueryState("page", 1);
  const [pageSize, setPageSize] = useQueryState("size", 10);

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

  const filtered = records.filter((r) => {
    const matchFilter = filter === "ALL" || r.decision === filter;
    const q = search.trim().toLowerCase();
    const matchSearch =
      !q ||
      r.sourceService.toLowerCase().includes(q) ||
      r.destinationService.toLowerCase().includes(q) ||
      (r.currentHash && r.currentHash.toLowerCase().includes(q)) ||
      (r.policyId && r.policyId.toLowerCase().includes(q));
    return matchFilter && matchSearch;
  });

  const paginatedRecords = filtered.slice((page - 1) * pageSize, page * pageSize);

  return (
    <div>
      <Topbar
        title="Lineage"
        subtitle="Cryptographic SHA-256 hash-chain audit evidence for every enforcement decision."
      />

      <div className="px-6 lg:px-8 mt-4 flex flex-wrap items-center gap-3 justify-between">
        {/* Verification status & search */}
        <div className="flex items-center gap-3 flex-1 max-w-2xl">
          {verification && (
            <div
              className={`flex items-center gap-2 text-xs font-semibold px-3.5 py-2 rounded-xl border shrink-0 ${
                verification.valid
                  ? "bg-[var(--color-good)]/10 border-[var(--color-good)]/30 text-[var(--color-good)]"
                  : "bg-[var(--color-bad)]/10 border-[var(--color-bad)]/30 text-[var(--color-bad)]"
              }`}
            >
              {verification.valid ? <CheckCircle size={14} /> : <XCircle size={14} />}
              {verification.valid
                ? `Chain valid (${verification.recordsChecked} blocks)`
                : `Chain broken at #${verification.brokenAt}`}
            </div>
          )}

          <div className="relative flex-1 min-w-[180px]">
            <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--color-text-faint)]" />
            <input
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(1);
              }}
              placeholder="Search hash or service..."
              className="field-input pl-9 text-xs"
            />
          </div>

          <button
            onClick={verify}
            disabled={verifying}
            className="flex items-center gap-1.5 text-xs text-[var(--color-text-dim)] hover:text-white border border-[var(--color-border)] rounded-xl px-3 py-2 transition-colors disabled:opacity-50"
          >
            <RefreshCw size={13} className={verifying ? "animate-spin" : ""} />
            Verify Chain
          </button>
        </div>

        {/* Filter buttons */}
        <div className="flex items-center gap-2">
          {["ALL", "ALLOW", "DENY"].map((f) => (
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
            <Loader2 size={20} className="animate-spin inline mr-2" /> Loading lineage records…
          </div>
        )}

        {!loading && filtered.length === 0 && (
          <div className="card px-5 py-12 text-center">
            <Link2 size={32} className="mx-auto mb-3 text-[var(--color-text-faint)]" />
            <p className="text-[var(--color-text-faint)] text-sm">
              {records.length === 0
                ? "No lineage records yet. Run enforcement checks to create audit entries."
                : `No ${filter} decisions match the filter.`}
            </p>
          </div>
        )}

        {!loading && filtered.length > 0 && (
          <div className="card overflow-hidden">
            <div className="divide-y divide-[var(--color-border)]">
              {paginatedRecords.map((r) => (
                <div key={r.id} className="hover:bg-[var(--color-surface-2)] transition-colors">
                  <div
                    className="px-5 py-3.5 flex items-center gap-4 cursor-pointer"
                    onClick={() => setExpanded(expanded === r.id ? null : r.id)}
                  >
                    {/* Decision badge */}
                    <span
                      className={`text-xs font-bold px-2.5 py-1 rounded shrink-0 ${
                        r.decision === "ALLOW"
                          ? "bg-[var(--color-good)]/15 text-[var(--color-good)] border border-[var(--color-good)]/30"
                          : "bg-[var(--color-bad)]/15 text-[var(--color-bad)] border border-[var(--color-bad)]/30"
                      }`}
                    >
                      {r.decision}
                    </span>

                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-semibold text-white truncate font-mono">
                          {r.sourceService} [{r.sourceRegion ?? "?"}]
                        </span>
                        <span className="text-[var(--color-text-faint)]">→</span>
                        <span className="text-sm font-semibold text-white truncate font-mono">
                          {r.destinationService} [{r.destinationRegion ?? "?"}]
                        </span>
                      </div>
                      <div className="flex items-center gap-3 mt-0.5 text-xs text-[var(--color-text-faint)]">
                        <span className="font-semibold text-amber-400">{r.dataClass}</span>
                        {r.policyId && <span>• {r.policyId}</span>}
                        <span>• Block #{r.id}</span>
                        <span>• {new Date(r.createdAt).toLocaleString()}</span>
                      </div>
                    </div>

                    {/* Hash preview */}
                    <div className="hidden lg:flex items-center gap-2 shrink-0">
                      <span className="font-mono text-xs text-[var(--color-text-faint)]">
                        {r.currentHash?.slice(0, 14)}…
                      </span>
                      {expanded === r.id ? (
                        <ChevronUp size={14} className="text-[var(--color-text-faint)]" />
                      ) : (
                        <ChevronDown size={14} className="text-[var(--color-text-faint)]" />
                      )}
                    </div>
                    <div className="lg:hidden">
                      {expanded === r.id ? (
                        <ChevronUp size={14} className="text-[var(--color-text-faint)]" />
                      ) : (
                        <ChevronDown size={14} className="text-[var(--color-text-faint)]" />
                      )}
                    </div>
                  </div>

                  {expanded === r.id && (
                    <div className="px-5 pb-4 grid grid-cols-1 sm:grid-cols-2 gap-4 bg-[var(--color-surface-2)]/50 border-t border-[var(--color-border)] pt-3">
                      <div className="space-y-2 text-xs">
                        <HashRow label="Decision ID" value={`#${r.decisionId}`} />
                        <HashRow label="Lineage Block" value={`#${r.id}`} />
                        <HashRow label="Triggered Policy" value={r.policyId || "—"} />
                        <HashRow label="Enforcement Reason" value={r.reason} />
                      </div>
                      <div className="space-y-2 text-xs">
                        <div>
                          <p className="text-[var(--color-text-faint)] mb-1">Previous Block Hash</p>
                          <p className="font-mono text-[var(--color-text-dim)] break-all bg-[var(--color-surface)] p-2 rounded-lg border border-[var(--color-border)] leading-relaxed">
                            {r.previousHash || "GENESIS_BLOCK_00000000000000000000000000000000"}
                          </p>
                        </div>
                        <div>
                          <p className="text-[var(--color-text-faint)] mb-1">Current SHA-256 Hash</p>
                          <p className="font-mono text-white break-all bg-[var(--color-surface)] p-2 rounded-lg border border-[var(--color-border)] leading-relaxed">
                            {r.currentHash}
                          </p>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>

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
        )}
      </div>
    </div>
  );
}

function HashRow({ label, value }) {
  return (
    <div className="flex gap-2 text-xs">
      <span className="text-[var(--color-text-faint)] shrink-0 w-28">{label}:</span>
      <span className="text-[var(--color-text-dim)]">{value}</span>
    </div>
  );
}
