import { useState } from "react";
import { Sparkles, CheckCircle2, XCircle, Loader2, HelpCircle } from "lucide-react";
import Topbar from "../components/layout/Topbar";
import { aiApi } from "../api";
import { useAuth } from "../context/AuthContext";

const CONFIDENCE_COLOR = (c) =>
  c >= 0.85 ? "var(--color-good)" : c >= 0.6 ? "var(--color-warn)" : "var(--color-bad)";

export default function AiClassification() {
  const { user } = useAuth();
  const [fieldName, setFieldName] = useState("");
  const [sampleValue, setSampleValue] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);
  const [results, setResults] = useState([]);

  const canApprove = user?.role === "ADMIN" || user?.role === "COMPLIANCE_OFFICER";
  const isValidFieldName = /^[a-zA-Z0-9_.-]+$/.test(fieldName.trim());

  async function handleClassify(e) {
    e.preventDefault();
    const name = fieldName.trim();
    if (!name) { setFormError("Field name is required."); return; }
    if (!isValidFieldName) { setFormError("Field name must be alphanumeric (dots, underscores, hyphens allowed)."); return; }
    setFormError(null);
    setSubmitting(true);
    try {
      const res = await aiApi.classify(name, sampleValue.trim() || undefined);
      setResults((prev) => [res, ...prev]);
      setFieldName("");
      setSampleValue("");
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

  const statusBadge = (status) => {
    const map = {
      PENDING: { bg: "bg-[var(--color-warn)]/15", text: "text-[var(--color-warn)]" },
      APPROVED: { bg: "bg-[var(--color-good)]/15", text: "text-[var(--color-good)]" },
      REJECTED: { bg: "bg-[var(--color-bad)]/15", text: "text-[var(--color-bad)]" },
    };
    const s = map[status] || map.PENDING;
    return `text-xs font-medium px-2 py-0.5 rounded-lg ${s.bg} ${s.text}`;
  };

  return (
    <div>
      <Topbar
        title="AI Classification"
        subtitle="Automatically classify schema fields by data sensitivity using the AI service."
      />

      <div className="px-6 lg:px-8 mt-6 grid grid-cols-1 xl:grid-cols-5 gap-6 pb-8">
        {/* Form */}
        <div className="xl:col-span-2">
          <div className="card p-5 space-y-4">
            <div className="flex items-center gap-2 mb-1">
              <Sparkles size={16} className="text-[var(--color-brand)]" />
              <h2 className="font-semibold text-white text-sm">Classify a Field</h2>
            </div>

            <form onSubmit={handleClassify} className="space-y-4">
              <div>
                <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">
                  Field Name *
                  <span className="ml-1 text-[var(--color-text-faint)]">(alphanumeric, dots, underscores)</span>
                </label>
                <input
                  value={fieldName}
                  onChange={(e) => setFieldName(e.target.value)}
                  placeholder="credit_card_number"
                  className="field-input"
                />
              </div>

              <div>
                <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">
                  Sample Value <span className="text-[var(--color-text-faint)]">(optional, aids classification)</span>
                </label>
                <input
                  value={sampleValue}
                  onChange={(e) => setSampleValue(e.target.value)}
                  placeholder="4111-XXXX-XXXX-1111"
                  className="field-input"
                />
              </div>

              {formError && (
                <p className="text-xs text-[var(--color-bad)] bg-[var(--color-bad)]/10 rounded-lg px-3 py-2">{formError}</p>
              )}

              <button type="submit" disabled={submitting} className="btn-primary w-full justify-center">
                {submitting ? <Loader2 size={15} className="animate-spin" /> : <Sparkles size={15} />}
                {submitting ? "Classifying…" : "Classify Field"}
              </button>
            </form>

            {/* Info */}
            <div className="bg-[var(--color-surface-2)] rounded-xl p-3 text-xs text-[var(--color-text-faint)] space-y-1">
              <p className="font-medium text-[var(--color-text-dim)]">Classification labels</p>
              {["PII — Personally Identifiable Information", "PCI — Payment Card Industry", "PHI — Protected Health Information", "NON_SENSITIVE — Safe to transfer freely", "UNKNOWN — Could not determine class"].map((l) => (
                <p key={l}>{l}</p>
              ))}
            </div>

            {!canApprove && (
              <div className="flex items-start gap-2 text-xs text-[var(--color-text-faint)] bg-[var(--color-surface-2)] rounded-xl p-3">
                <HelpCircle size={13} className="shrink-0 mt-0.5" />
                <span>Approve/reject actions require <strong className="text-[var(--color-text-dim)]">Admin</strong> or <strong className="text-[var(--color-text-dim)]">Compliance Officer</strong> role.</span>
              </div>
            )}
          </div>
        </div>

        {/* Results */}
        <div className="xl:col-span-3">
          <div className="card overflow-hidden">
            <div className="px-5 py-4 border-b border-[var(--color-border)]">
              <h2 className="font-semibold text-white text-sm">
                Classification Results
                <span className="ml-2 text-xs font-normal text-[var(--color-text-faint)]">
                  (this session)
                </span>
              </h2>
            </div>

            {results.length === 0 && (
              <div className="px-5 py-12 text-center">
                <Sparkles size={28} className="mx-auto mb-3 text-[var(--color-text-faint)]" />
                <p className="text-[var(--color-text-faint)] text-sm">
                  Submit a field name above to see AI classification results.
                </p>
              </div>
            )}

            {results.length > 0 && (
              <div className="divide-y divide-[var(--color-border)]">
                {results.map((r) => (
                  <div key={r.id} className="px-5 py-4 hover:bg-[var(--color-surface-2)] transition-colors">
                    <div className="flex items-start justify-between gap-4">
                      <div className="min-w-0 space-y-1">
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="font-mono text-sm font-medium text-white">{r.fieldName}</span>
                          <span className="text-xs font-bold px-2 py-0.5 rounded bg-[var(--color-brand)]/15 text-[var(--color-brand)]">
                            {r.suggestedClass}
                          </span>
                          <span className={statusBadge(r.status)}>{r.status}</span>
                        </div>
                        <div className="flex items-center gap-4 text-xs text-[var(--color-text-faint)]">
                          <span>
                            Confidence:{" "}
                            <span style={{ color: CONFIDENCE_COLOR(r.confidence) }} className="font-medium">
                              {Math.round(r.confidence * 100)}%
                            </span>
                          </span>
                          <span>Provider: {r.provider}</span>
                          {r.reviewedBy && <span>Reviewed by: {r.reviewedBy}</span>}
                        </div>
                        {/* Confidence bar */}
                        <div className="h-1 w-full max-w-32 bg-[var(--color-surface-2)] rounded-full overflow-hidden">
                          <div
                            className="h-full rounded-full transition-all"
                            style={{ width: `${Math.round(r.confidence * 100)}%`, backgroundColor: CONFIDENCE_COLOR(r.confidence) }}
                          />
                        </div>
                      </div>

                      {canApprove && r.status === "PENDING" && (
                        <div className="flex gap-2 shrink-0">
                          <button
                            onClick={() => handleApprove(r.id)}
                            className="flex items-center gap-1 text-xs px-3 py-1.5 rounded-lg bg-[var(--color-good)]/15 text-[var(--color-good)] hover:bg-[var(--color-good)]/25 transition-colors border border-[var(--color-good)]/30"
                          >
                            <CheckCircle2 size={12} /> Approve
                          </button>
                          <button
                            onClick={() => handleReject(r.id)}
                            className="flex items-center gap-1 text-xs px-3 py-1.5 rounded-lg bg-[var(--color-bad)]/15 text-[var(--color-bad)] hover:bg-[var(--color-bad)]/25 transition-colors border border-[var(--color-bad)]/30"
                          >
                            <XCircle size={12} /> Reject
                          </button>
                        </div>
                      )}

                      {r.status === "APPROVED" && (
                        <span className="flex items-center gap-1 text-xs text-[var(--color-good)] shrink-0">
                          <CheckCircle2 size={13} /> Approved
                        </span>
                      )}
                      {r.status === "REJECTED" && (
                        <span className="flex items-center gap-1 text-xs text-[var(--color-bad)] shrink-0">
                          <XCircle size={13} /> Rejected
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
