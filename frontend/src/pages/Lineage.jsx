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
  Copy,
  Check,
  Lock,
} from "lucide-react";
import Topbar from "../components/layout/Topbar";
import Pagination from "../components/ui/Pagination";
import Button from "../components/ui/Button";
import Badge from "../components/ui/Badge";
import EmptyState from "../components/ui/EmptyState";
import { TableSkeleton } from "../components/ui/LoadingSkeleton";
import { lineageApi } from "../api";

export default function Lineage() {
  const [records, setRecords] = useState([]);
  const [verification, setVerification] = useState(null);
  const [loading, setLoading] = useState(true);
  const [verifying, setVerifying] = useState(false);
  const [error, setError] = useState(null);
  const [expanded, setExpanded] = useState(null);
  const [copiedHash, setCopiedHash] = useState(null);

  // Filters
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState("ALL");
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const data = await lineageApi.list();
      setRecords(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || "Failed to load cryptographic lineage");
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

  function copyToClipboard(text) {
    navigator.clipboard.writeText(text);
    setCopiedHash(text);
    setTimeout(() => setCopiedHash(null), 2000);
  }

  const filtered = records.filter((r) => {
    const matchFilter = filter === "ALL" || r.decision === filter;
    const q = search.trim().toLowerCase();
    const matchSearch =
      !q ||
      (r.sourceService || "").toLowerCase().includes(q) ||
      (r.destinationService || "").toLowerCase().includes(q) ||
      (r.currentHash && r.currentHash.toLowerCase().includes(q)) ||
      (r.policyId && r.policyId.toLowerCase().includes(q));
    return matchFilter && matchSearch;
  });

  const paginatedRecords = filtered.slice((page - 1) * pageSize, page * pageSize);

  const topActions = (
    <Button
      variant="secondary"
      size="md"
      icon={verifying ? Loader2 : RefreshCw}
      onClick={verify}
      disabled={verifying}
    >
      {verifying ? "Verifying..." : "Verify Hash Chain"}
    </Button>
  );

  return (
    <div>
      <Topbar
        title="Cryptographic Lineage"
        subtitle="Immutable SHA-256 hash-chain audit ledger proving mathematical integrity of all enforcement decisions."
        actions={topActions}
      />

      <div className="px-6 lg:px-8 py-6 space-y-4 pb-12">
        {/* Verification Banner & Controls */}
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3 flex-1 max-w-2xl">
            {/* Search */}
            <div className="relative flex-1 min-w-[200px]">
              <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--color-text-faint)] pointer-events-none" />
              <input
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(1);
                }}
                placeholder="Search..."
                className="field-input field-input-search !pl-9 text-xs"
              />
              {search && (
                <button
                  type="button"
                  onClick={() => setSearch("")}
                  className="absolute right-2.5 top-1/2 -translate-y-1/2 text-[var(--color-text-faint)] hover:text-[var(--color-text)] p-0.5"
                  title="Clear search"
                >
                  <X size={12} />
                </button>
              )}
            </div>

            {/* Filter */}
            <select
              value={filter}
              onChange={(e) => {
                setFilter(e.target.value);
                setPage(1);
              }}
              className="field-input py-1.5 text-xs w-32"
            >
              <option value="ALL">All Decisions</option>
              <option value="ALLOW">ALLOW</option>
              <option value="DENY">DENY</option>
            </select>
          </div>

          {/* Verification Status Pill */}
          {verification && (
            <div
              className={`flex items-center gap-2 text-xs font-semibold px-3 py-1.5 rounded-xl border shrink-0 ${
                verification.valid
                  ? "bg-[var(--color-good-light)] border-[var(--color-good)]/30 text-[var(--color-good-text)]"
                  : "bg-[var(--color-bad-light)] border-[var(--color-bad)]/30 text-[var(--color-bad-text)]"
              }`}
            >
              {verification.valid ? <CheckCircle size={14} className="text-[var(--color-good)]" /> : <XCircle size={14} className="text-[var(--color-bad)]" />}
              <span>
                {verification.valid
                  ? `Hash Chain Valid (${verification.recordsChecked || records.length} blocks verified)`
                  : `Hash Chain Broken at #${verification.brokenAt}`}
              </span>
            </div>
          )}
        </div>

        {/* Table Card */}
        <div className="card overflow-hidden">
          {loading ? (
            <TableSkeleton rows={5} cols={6} />
          ) : filtered.length === 0 ? (
            <EmptyState
              icon={Link2}
              title="No cryptographic lineage records"
              description="Lineage records are cryptographically generated automatically whenever runtime decisions are made."
            />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead className="bg-[var(--color-surface-2)] text-[var(--color-text-dim)] border-b border-[var(--color-border)] uppercase tracking-wider font-semibold text-[10px]">
                  <tr>
                    <th className="px-5 py-3 font-semibold"># Sequence</th>
                    <th className="px-5 py-3 font-semibold">Data Flow Direction</th>
                    <th className="px-5 py-3 font-semibold">Decision</th>
                    <th className="px-5 py-3 font-semibold">Policy Applied</th>
                    <th className="px-5 py-3 font-semibold">Current Hash (SHA-256)</th>
                    <th className="px-5 py-3 font-semibold">Timestamp</th>
                    <th className="px-5 py-3 font-semibold text-right">Details</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--color-border)]">
                  {paginatedRecords.map((r, idx) => {
                    const isAllow = r.decision === "ALLOW";
                    const isExp = expanded === (r.id || idx);

                    return (
                      <tr
                        key={r.id || idx}
                        className="hover:bg-[var(--color-surface-2)]/60 transition-colors"
                      >
                        <td className="px-5 py-3 font-mono font-bold text-xs text-[var(--color-text)]">
                          #{r.sequenceNumber != null ? r.sequenceNumber : idx + 1}
                        </td>

                        <td className="px-5 py-3">
                          <div className="flex items-center gap-1.5 font-mono text-xs">
                            <span className="font-semibold text-[var(--color-text)]">{r.sourceService}</span>
                            <span className="text-[var(--color-text-faint)]">→</span>
                            <span className="font-semibold text-[var(--color-text)]">{r.destinationService}</span>
                            {r.dataClass && (
                              <span className="text-[10px] font-semibold px-1.5 py-0.2 rounded bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20 ml-1">
                                {r.dataClass}
                              </span>
                            )}
                          </div>
                        </td>

                        <td className="px-5 py-3">
                          <Badge variant={isAllow ? "good" : "bad"} size="sm" dot>
                            {r.decision}
                          </Badge>
                        </td>

                        <td className="px-5 py-3 font-mono text-xs text-[var(--color-text-dim)]">
                          {r.policyId || "DEFAULT_GATE"}
                        </td>

                        <td className="px-5 py-3 font-mono text-[11px]">
                          <div className="flex items-center gap-1.5 text-[var(--color-text-dim)]">
                            <span className="truncate max-w-[140px]">{r.currentHash || "GENESIS"}</span>
                            {r.currentHash && (
                              <button
                                onClick={() => copyToClipboard(r.currentHash)}
                                className="text-[var(--color-text-faint)] hover:text-[var(--color-text)] p-0.5"
                                title="Copy full SHA-256 hash"
                              >
                                {copiedHash === r.currentHash ? (
                                  <Check size={12} className="text-[var(--color-good)]" />
                                ) : (
                                  <Copy size={12} />
                                )}
                              </button>
                            )}
                          </div>
                        </td>

                        <td className="px-5 py-3 text-[11px] text-[var(--color-text-faint)] whitespace-nowrap">
                          {r.timestamp ? new Date(r.timestamp).toLocaleString() : "just now"}
                        </td>

                        <td className="px-5 py-3 text-right">
                          <button
                            onClick={() => setExpanded(isExp ? null : (r.id || idx))}
                            className="p-1 rounded text-[var(--color-text-faint)] hover:text-[var(--color-text)] hover:bg-[var(--color-surface-2)]"
                          >
                            {isExp ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>

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
    </div>
  );
}
