import { useState, useEffect } from "react";
import {
  Sparkles,
  CheckCircle2,
  XCircle,
  Loader2,
  HelpCircle,
  RotateCcw,
} from "lucide-react";
import Topbar from "../components/layout/Topbar";
import Pagination from "../components/ui/Pagination";
import { aiApi } from "../api";
import { useAuth } from "../context/AuthContext";
import { useFormDraft } from "../hooks/useFormDraft";

const CONFIDENCE_COLOR = (c) =>
  c >= 0.85 ? "var(--color-good)" : c >= 0.6 ? "var(--color-warn)" : "var(--color-bad)";

export default function AiClassification() {
  const { user } = useAuth();
  const { values: form, setValues: setForm, clearDraft, resetForm } = useFormDraft(
    "ai-classification",
    { fieldName: "", sampleValue: "" }
  );

  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);

  // Restore classification results from sessionStorage if present
  const [results, setResults] = useState(() => {
    try {
      const saved = sessionStorage.getItem("policymesh:ai-classification:results");
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  // Pagination for results list
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);

  useEffect(() => {
    try {
      sessionStorage.setItem("policymesh:ai-classification:results", JSON.stringify(results));
    } catch {}
  }, [results]);

  const canApprove = user?.role === "ADMIN" || user?.role === "COMPLIANCE_OFFICER";
  const isValidFieldName = /^[a-zA-Z0-9_.-]+$/.test(form.fieldName.trim());

  async function handleClassify(e) {
    e.preventDefault();
    const name = form.fieldName.trim();
    if (!name) {
      setFormError("Field name is required.");
      return;
    }
    if (!isValidFieldName) {
      setFormError("Field name must be alphanumeric (dots, underscores, hyphens allowed).");
      return;
    }
    setFormError(null);
    setSubmitting(true);
    try {
      const res = await aiApi.classify(name, form.sampleValue.trim() || undefined);
      setResults((prev) => [res, ...prev]);
      clearDraft();
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleApprove(id) {
    try {
      const updated = await aiApi.approve(id);
      setResults((prev) => prev.map((r) => (r.id === id ? updated : r)));
    } catch (err) {
      setFormError(err.message);
    }
  }

  async function handleReject(id) {
    try {
      const updated = await aiApi.reject(id);
      setResults((prev) => prev.map((r) => (r.id === id ? updated : r)));
    } catch (err) {
      setFormError(err.message);
    }
  }

  function clearHistory() {
    setResults([]);
    try {
      sessionStorage.removeItem("policymesh:ai-classification:results");
    } catch {}
  }

  const statusBadge = (status) => {
    const map = {
      PENDING: { bg: "bg-amber-500/15 border-amber-500/30", text: "text-amber-400" },
      APPROVED: { bg: "bg-[var(--color-good)]/15 border-[var(--color-good)]/30", text: "text-[var(--color-good)]" },
      REJECTED: { bg: "bg-[var(--color-bad)]/15 border-[var(--color-bad)]/30", text: "text-[var(--color-bad)]" },
    };
    const s = map[status] || map.PENDING;
    return `text-xs font-semibold px-2 py-0.5 rounded-lg border ${s.bg} ${s.text}`;
  };

  const paginatedResults = results.slice((page - 1) * pageSize, page * pageSize);

  return (
    <div>
      <Topbar
        title="AI Schema Classification"
        subtitle="Automated data sensitivity tagging and human-in-the-loop compliance review."
      />

      <div className="px-6 lg:px-8 mt-6 grid grid-cols-1 xl:grid-cols-5 gap-6 pb-12">
        {/* Form Column */}
        <div className="xl:col-span-2 space-y-4">
          <div className="card p-5 space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Sparkles size={17} className="text-[var(--color-brand)]" />
                <h2 className="font-semibold text-white text-sm">Classify Schema Field</h2>
              </div>
              <button
                type="button"
                onClick={resetForm}
                className="text-xs text-[var(--color-text-faint)] hover:text-white flex items-center gap-1 transition-colors"
                title="Reset Form"
              >
                <RotateCcw size={12} /> Reset
              </button>
            </div>

            {/* Form */}
            <form onSubmit={handleClassify} className="space-y-4 pt-1">
              <div>
                <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">
                  Field Name * <span className="text-[var(--color-text-faint)]">(alphanumeric, dots, underscores)</span>
                </label>
                <input
                  value={form.fieldName}
                  onChange={(e) => setForm((prev) => ({ ...prev, fieldName: e.target.value }))}
                  placeholder="credit_card_number"
                  className="field-input text-xs"
                />
              </div>

              <div>
                <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">
                  Sample Value <span className="text-[var(--color-text-faint)]">(optional, enhances LLM accuracy)</span>
                </label>
                <input
                  value={form.sampleValue}
                  onChange={(e) => setForm((prev) => ({ ...prev, sampleValue: e.target.value }))}
                  placeholder="4111-XXXX-XXXX-1111"
                  className="field-input text-xs"
                />
              </div>

              {formError && (
                <p className="text-xs text-[var(--color-bad)] bg-[var(--color-bad)]/10 rounded-lg px-3 py-2">
                  {formError}
                </p>
              )}

              <button type="submit" disabled={submitting} className="btn-primary w-full justify-center">
                {submitting ? <Loader2 size={15} className="animate-spin" /> : <Sparkles size={15} />}
                {submitting ? "Analyzing Sensitivity…" : "Classify Field"}
              </button>
            </form>

            {/* Info */}
            <div className="bg-[var(--color-surface-2)] rounded-xl p-3 text-xs text-[var(--color-text-faint)] space-y-1">
              <p className="font-medium text-[var(--color-text-dim)]">Supported Sensitivity Tags</p>
              {[
                "PII — Personally Identifiable Information",
                "PCI — Payment Card Industry Data",
                "PHI — Protected Health Information",
                "NON_SENSITIVE — Freely shareable operational data",
              ].map((l) => (
                <p key={l}>• {l}</p>
              ))}
            </div>

            {!canApprove && (
              <div className="flex items-start gap-2 text-xs text-[var(--color-text-faint)] bg-[var(--color-surface-2)] rounded-xl p-3">
                <HelpCircle size={13} className="shrink-0 mt-0.5 text-amber-400" />
                <span>
                  Approval workflows require <strong className="text-white">Admin</strong> or{" "}
                  <strong className="text-white">Compliance Officer</strong> role.
                </span>
              </div>
            )}
          </div>
        </div>

        {/* Results Column */}
        <div className="xl:col-span-3">
          <div className="card overflow-hidden">
            <div className="px-5 py-4 border-b border-[var(--color-border)] flex items-center justify-between">
              <div>
                <h2 className="font-semibold text-white text-sm">Classification & Human Review Ledger</h2>
                <p className="text-xs text-[var(--color-text-faint)]">Pending and approved sensitivity tags.</p>
              </div>
              {results.length > 0 && (
                <button
                  onClick={clearHistory}
                  className="text-xs text-[var(--color-text-faint)] hover:text-white transition-colors"
                >
                  Clear Results
                </button>
              )}
            </div>

            {results.length === 0 && (
              <div className="px-5 py-16 text-center">
                <Sparkles size={32} className="mx-auto mb-3 text-[var(--color-text-faint)]" />
                <p className="text-sm text-[var(--color-text-dim)]">No schema fields classified in this session.</p>
                <p className="text-xs text-[var(--color-text-faint)] mt-1">
                  Submit a field name and optional sample value on the left to classify.
                </p>
              </div>
            )}

            {results.length > 0 && (
              <>
                <div className="divide-y divide-[var(--color-border)]">
                  {paginatedResults.map((r) => (
                    <div key={r.id} className="px-5 py-4 hover:bg-[var(--color-surface-2)] transition-colors">
                      <div className="flex items-start justify-between gap-4 flex-wrap sm:flex-nowrap">
                        <div className="min-w-0 space-y-1.5 flex-1">
                          <div className="flex items-center gap-2 flex-wrap">
                            <span className="font-mono text-sm font-semibold text-white">{r.fieldName}</span>
                            <span className="text-xs font-bold px-2 py-0.5 rounded bg-[var(--color-brand)]/15 text-[var(--color-brand)] border border-[var(--color-brand)]/30">
                              {r.suggestedClass}
                            </span>
                            <span className={statusBadge(r.status)}>{r.status}</span>
                          </div>

                          <div className="flex items-center gap-4 text-xs text-[var(--color-text-faint)]">
                            <span>
                              Confidence:{" "}
                              <span style={{ color: CONFIDENCE_COLOR(r.confidence) }} className="font-semibold">
                                {Math.round(r.confidence * 100)}%
                              </span>
                            </span>
                            <span>Engine: {r.provider}</span>
                            {r.reviewedBy && <span>Reviewer: {r.reviewedBy}</span>}
                          </div>

                          {/* Confidence Bar */}
                          <div className="h-1.5 w-full max-w-40 bg-[var(--color-surface-2)] rounded-full overflow-hidden">
                            <div
                              className="h-full rounded-full transition-all"
                              style={{
                                width: `${Math.round(r.confidence * 100)}%`,
                                backgroundColor: CONFIDENCE_COLOR(r.confidence),
                              }}
                            />
                          </div>
                        </div>

                        {/* Action Buttons */}
                        {canApprove && r.status === "PENDING" && (
                          <div className="flex gap-2 shrink-0 pt-1">
                            <button
                              onClick={() => handleApprove(r.id)}
                              className="flex items-center gap-1 text-xs px-3 py-1.5 rounded-lg bg-[var(--color-good)]/15 text-[var(--color-good)] hover:bg-[var(--color-good)]/25 transition-colors border border-[var(--color-good)]/30 font-medium"
                            >
                              <CheckCircle2 size={13} /> Approve
                            </button>
                            <button
                              onClick={() => handleReject(r.id)}
                              className="flex items-center gap-1 text-xs px-3 py-1.5 rounded-lg bg-[var(--color-bad)]/15 text-[var(--color-bad)] hover:bg-[var(--color-bad)]/25 transition-colors border border-[var(--color-bad)]/30 font-medium"
                            >
                              <XCircle size={13} /> Reject
                            </button>
                          </div>
                        )}

                        {r.status === "APPROVED" && (
                          <span className="flex items-center gap-1 text-xs text-[var(--color-good)] shrink-0 font-medium pt-1">
                            <CheckCircle2 size={14} /> Approved
                          </span>
                        )}
                        {r.status === "REJECTED" && (
                          <span className="flex items-center gap-1 text-xs text-[var(--color-bad)] shrink-0 font-medium pt-1">
                            <XCircle size={14} /> Rejected
                          </span>
                        )}
                      </div>
                    </div>
                  ))}
                </div>

                <Pagination
                  currentPage={page}
                  totalItems={results.length}
                  pageSize={pageSize}
                  onPageChange={setPage}
                  onPageSizeChange={(sz) => {
                    setPageSize(sz);
                    setPage(1);
                  }}
                />
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
