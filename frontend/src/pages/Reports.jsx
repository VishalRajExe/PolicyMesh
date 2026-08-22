import { useEffect, useState } from "react";
import {
  FileText,
  Download,
  ShieldCheck,
  AlertTriangle,
  Layers,
  Sparkles,
  Database,
  CheckCircle2,
  XCircle,
  RefreshCw,
  Loader2,
  Filter,
} from "lucide-react";
import Topbar from "../components/layout/Topbar";
import { reportsApi } from "../api";

export default function Reports() {
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [exporting, setExporting] = useState(false);
  const [violationFilter, setViolationFilter] = useState("ALL");

  async function loadReport() {
    setLoading(true);
    setError(null);
    try {
      const data = await reportsApi.getComplianceReport();
      setReport(data);
    } catch (err) {
      setError(err.message || "Failed to load compliance report");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadReport();
  }, []);

  async function handleExportCsv() {
    setExporting(true);
    try {
      await reportsApi.downloadCsv();
    } catch (err) {
      alert("Failed to download CSV export: " + err.message);
    } finally {
      setExporting(false);
    }
  }

  const violations = report?.recentViolations || [];
  const filteredViolations = violations.filter((v) => {
    if (violationFilter === "ALL") return true;
    return v.dataClass === violationFilter;
  });

  return (
    <div>
      <Topbar
        title="Compliance & Governance Reports"
        subtitle="Aggregated data residency audits, runtime violation logs, and cryptographic lineage integrity."
      />

      <div className="px-6 lg:px-8 mt-4 space-y-6 pb-12">
        {/* Actions Bar */}
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2 text-xs text-[var(--color-text-dim)]">
            <span>Report Generated:</span>
            <span className="font-mono text-white">
              {report?.generatedAt ? new Date(report.generatedAt).toLocaleString() : "—"}
            </span>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={loadReport}
              disabled={loading}
              className="btn-ghost flex items-center gap-1.5 text-xs"
            >
              <RefreshCw size={14} className={loading ? "animate-spin" : ""} /> Refresh
            </button>

            <button
              onClick={handleExportCsv}
              disabled={exporting || loading}
              className="btn-primary flex items-center gap-1.5 text-xs"
            >
              {exporting ? <Loader2 size={14} className="animate-spin" /> : <Download size={14} />}
              Export Audit Log (CSV)
            </button>
          </div>
        </div>

        {error && (
          <div className="rounded-xl bg-[var(--color-bad)]/10 border border-[var(--color-bad)]/30 px-4 py-3 text-sm text-[var(--color-bad)] flex items-center justify-between">
            {error}
            <button onClick={loadReport} className="underline text-xs ml-4">Retry</button>
          </div>
        )}

        {/* Top Metric Cards */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="card p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs text-[var(--color-text-faint)]">Compliance Score</span>
              <ShieldCheck size={18} className="text-[var(--color-good)]" />
            </div>
            <div className="flex items-baseline gap-2">
              <span className="text-2xl font-bold text-white">
                {report?.summary?.complianceScore ?? 100}%
              </span>
              <span className="text-[11px] text-[var(--color-good)] font-medium">Real-time</span>
            </div>
          </div>

          <div className="card p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs text-[var(--color-text-faint)]">Allowed / Blocked Transfers</span>
              <Layers size={18} className="text-blue-400" />
            </div>
            <div className="flex items-baseline gap-2">
              <span className="text-xl font-bold text-[var(--color-good)]">
                {report?.summary?.allowedTransfers ?? 0}
              </span>
              <span className="text-xs text-[var(--color-text-faint)]">/</span>
              <span className="text-xl font-bold text-[var(--color-bad)]">
                {report?.summary?.blockedTransfers ?? 0}
              </span>
            </div>
          </div>

          <div className="card p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs text-[var(--color-text-faint)]">Active Policy Rules</span>
              <FileText size={18} className="text-purple-400" />
            </div>
            <div className="flex items-baseline gap-2">
              <span className="text-2xl font-bold text-white">
                {report?.summary?.activePolicies ?? 0}
              </span>
              <span className="text-xs text-[var(--color-text-faint)]">
                of {report?.summary?.totalPolicies ?? 0} total
              </span>
            </div>
          </div>

          <div className="card p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs text-[var(--color-text-faint)]">Lineage Integrity</span>
              <Database size={18} className="text-emerald-400" />
            </div>
            <div className="flex items-center gap-1.5 mt-1">
              <CheckCircle2 size={16} className="text-[var(--color-good)]" />
              <span className="text-sm font-semibold text-white">
                {report?.lineageStatus?.status ?? "SECURE & VERIFIED"}
              </span>
            </div>
          </div>
        </div>

        {/* Section 1: Policy Governance Breakdown */}
        <div className="card p-5 space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-base font-semibold text-white">Policy Audit & Residency Scope</h3>
              <p className="text-xs text-[var(--color-text-dim)]">Jurisdictional enforcement parameters and live evaluation metrics.</p>
            </div>
            <span className="text-xs bg-[var(--color-surface-2)] text-[var(--color-text-dim)] px-2.5 py-1 rounded-lg border border-[var(--color-border)]">
              {report?.policyBreakdown?.length ?? 0} Policies
            </span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-[var(--color-text-faint)] border-b border-[var(--color-border)]">
                  <th className="pb-3 font-medium">Policy ID & Name</th>
                  <th className="pb-3 font-medium">Jurisdiction</th>
                  <th className="pb-3 font-medium">Target Class</th>
                  <th className="pb-3 font-medium">Allowed Regions</th>
                  <th className="pb-3 font-medium">Status</th>
                  <th className="pb-3 text-right font-medium">Evaluations</th>
                </tr>
              </thead>
              <tbody>
                {loading && (
                  <tr>
                    <td colSpan={6} className="py-8 text-center text-[var(--color-text-faint)]">
                      <Loader2 size={18} className="animate-spin inline mr-2" /> Loading policy audits...
                    </td>
                  </tr>
                )}
                {!loading && (report?.policyBreakdown?.length ?? 0) === 0 && (
                  <tr>
                    <td colSpan={6} className="py-8 text-center text-[var(--color-text-faint)]">
                      No policies defined in repository.
                    </td>
                  </tr>
                )}
                {!loading &&
                  report?.policyBreakdown?.map((p) => (
                    <tr
                      key={p.id || p.policyCode}
                      className="border-b border-[var(--color-border)] last:border-0 hover:bg-[var(--color-surface-2)] transition-colors"
                    >
                      <td className="py-3">
                        <p className="font-semibold text-white text-xs">{p.policyCode}</p>
                        <p className="text-[11px] text-[var(--color-text-dim)]">{p.name}</p>
                      </td>
                      <td className="py-3">
                        <span className="text-xs font-mono px-2 py-0.5 rounded bg-blue-500/15 text-blue-400 border border-blue-500/30">
                          {p.jurisdiction}
                        </span>
                      </td>
                      <td className="py-3">
                        <span className="text-xs font-semibold px-2 py-0.5 rounded bg-amber-500/15 text-amber-400 border border-amber-500/30">
                          {p.dataClass}
                        </span>
                      </td>
                      <td className="py-3 text-xs text-[var(--color-text-dim)] font-mono">
                        {p.allowedRegions && p.allowedRegions.length > 0 ? Array.from(p.allowedRegions).join(", ") : "GLOBAL"}
                      </td>
                      <td className="py-3">
                        <span
                          className={`text-xs px-2 py-0.5 rounded-md font-medium ${
                            p.status === "ACTIVE"
                              ? "bg-[var(--color-good)]/15 text-[var(--color-good)]"
                              : "bg-zinc-500/15 text-zinc-300"
                          }`}
                        >
                          {p.status}
                        </span>
                      </td>
                      <td className="py-3 text-right text-xs">
                        <span className="text-[var(--color-good)] font-medium">{p.allowedEvaluations} ALLOW</span>
                        {p.blockedEvaluations > 0 && (
                          <span className="text-[var(--color-bad)] font-medium ml-2">{p.blockedEvaluations} DENY</span>
                        )}
                      </td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Section 2: AI Sensitivity Audit & Lineage Cryptography */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* AI Sensitivity */}
          <div className="card p-5 space-y-4">
            <div className="flex items-center gap-2">
              <Sparkles size={18} className="text-purple-400" />
              <h3 className="text-base font-semibold text-white">AI Schema Sensitivity Audit</h3>
            </div>
            <p className="text-xs text-[var(--color-text-dim)]">Automated PII/PHI schema classifications reviewed by compliance teams.</p>

            <div className="grid grid-cols-3 gap-3 pt-2">
              <div className="bg-[var(--color-surface-2)] p-3 rounded-xl border border-[var(--color-border)] text-center">
                <p className="text-xs text-[var(--color-text-faint)]">Total Classified</p>
                <p className="text-xl font-bold text-white mt-1">{report?.aiSummary?.totalClassified ?? 0}</p>
              </div>
              <div className="bg-[var(--color-surface-2)] p-3 rounded-xl border border-[var(--color-border)] text-center">
                <p className="text-xs text-[var(--color-good)]">Approved</p>
                <p className="text-xl font-bold text-[var(--color-good)] mt-1">{report?.aiSummary?.approved ?? 0}</p>
              </div>
              <div className="bg-[var(--color-surface-2)] p-3 rounded-xl border border-[var(--color-border)] text-center">
                <p className="text-xs text-amber-400">Pending</p>
                <p className="text-xl font-bold text-amber-400 mt-1">{report?.aiSummary?.pending ?? 0}</p>
              </div>
            </div>
          </div>

          {/* Lineage Hash-Chain */}
          <div className="card p-5 space-y-4">
            <div className="flex items-center gap-2">
              <Database size={18} className="text-emerald-400" />
              <h3 className="text-base font-semibold text-white">Cryptographic Lineage Ledger</h3>
            </div>
            <p className="text-xs text-[var(--color-text-dim)]">Tamper-evident SHA-256 block ledger recording every enforcement decision.</p>

            <div className="space-y-2 pt-1 text-xs">
              <div className="flex justify-between py-1.5 border-b border-[var(--color-border)]">
                <span className="text-[var(--color-text-dim)]">Ledger Integrity:</span>
                <span className="text-[var(--color-good)] font-medium flex items-center gap-1">
                  <CheckCircle2 size={13} /> {report?.lineageStatus?.status ?? "SECURE & VERIFIED"}
                </span>
              </div>
              <div className="flex justify-between py-1.5 border-b border-[var(--color-border)]">
                <span className="text-[var(--color-text-dim)]">Hashing Algorithm:</span>
                <span className="font-mono text-white">{report?.lineageStatus?.algorithm ?? "SHA-256"}</span>
              </div>
              <div className="flex justify-between py-1.5">
                <span className="text-[var(--color-text-dim)]">Verified Audit Records:</span>
                <span className="font-semibold text-white">{report?.lineageStatus?.recordsChecked ?? 0}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Section 3: Recent Violations Log */}
        <div className="card p-5 space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h3 className="text-base font-semibold text-white">Runtime Blocked Violations Log</h3>
              <p className="text-xs text-[var(--color-text-dim)]">Direct jurisdictional access violations intercepted and prevented at runtime.</p>
            </div>

            <div className="flex items-center gap-2">
              <Filter size={14} className="text-[var(--color-text-faint)]" />
              <select
                value={violationFilter}
                onChange={(e) => setViolationFilter(e.target.value)}
                className="field-input py-1 text-xs"
              >
                <option value="ALL">All Sensitivity Classes</option>
                <option value="PII">PII</option>
                <option value="PCI">PCI</option>
                <option value="PHI">PHI</option>
              </select>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-[var(--color-text-faint)] border-b border-[var(--color-border)]">
                  <th className="pb-3 font-medium">Timestamp</th>
                  <th className="pb-3 font-medium">Source Service</th>
                  <th className="pb-3 font-medium">Destination Service</th>
                  <th className="pb-3 font-medium">Classification</th>
                  <th className="pb-3 font-medium">Triggered Policy</th>
                  <th className="pb-3 font-medium">Violation Reason</th>
                </tr>
              </thead>
              <tbody>
                {loading && (
                  <tr>
                    <td colSpan={6} className="py-8 text-center text-[var(--color-text-faint)]">
                      <Loader2 size={18} className="animate-spin inline mr-2" /> Loading violations...
                    </td>
                  </tr>
                )}
                {!loading && filteredViolations.length === 0 && (
                  <tr>
                    <td colSpan={6} className="py-8 text-center text-[var(--color-text-faint)]">
                      No runtime violations recorded.
                    </td>
                  </tr>
                )}
                {!loading &&
                  filteredViolations.map((v) => (
                    <tr
                      key={v.id}
                      className="border-b border-[var(--color-border)] last:border-0 hover:bg-[var(--color-surface-2)] transition-colors"
                    >
                      <td className="py-3 text-xs text-[var(--color-text-dim)] font-mono">
                        {new Date(v.timestamp).toLocaleTimeString()}
                      </td>
                      <td className="py-3">
                        <span className="font-semibold text-white text-xs">{v.sourceService}</span>
                        <span className="text-[11px] text-[var(--color-text-faint)] ml-1.5">[{v.sourceRegion}]</span>
                      </td>
                      <td className="py-3">
                        <span className="font-semibold text-white text-xs">{v.destinationService}</span>
                        <span className="text-[11px] text-[var(--color-text-faint)] ml-1.5">[{v.destinationRegion}]</span>
                      </td>
                      <td className="py-3">
                        <span className="text-xs font-semibold px-2 py-0.5 rounded bg-[var(--color-bad)]/15 text-[var(--color-bad)] border border-[var(--color-bad)]/30">
                          {v.dataClass}
                        </span>
                      </td>
                      <td className="py-3 text-xs font-mono text-white">
                        {v.policyId}
                      </td>
                      <td className="py-3 text-xs text-[var(--color-text-dim)]">
                        {v.reason}
                      </td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
