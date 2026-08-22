import { useState, useEffect } from "react";
import {
  Sparkles,
  CheckCircle2,
  XCircle,
  Loader2,
  HelpCircle,
  RotateCcw,
  Check,
} from "lucide-react";
import Topbar from "../components/layout/Topbar";
import Pagination from "../components/ui/Pagination";
import SearchableCombobox from "../components/ui/SearchableCombobox";
import { aiApi } from "../api";
import { useAuth } from "../context/AuthContext";
import { useFormDraft } from "../hooks/useFormDraft";

const CONFIDENCE_COLOR = (c) =>
  c >= 0.85 ? "var(--color-good)" : c >= 0.6 ? "var(--color-warn)" : "var(--color-bad)";

const STANDARD_SCHEMA_FIELDS = [
  {
    fieldName: "credit_card_number",
    sampleValue: "4111-2222-3333-4444",
    classification: "PCI",
    description: "Primary Account Number (PAN)",
  },
  {
    fieldName: "customer_email",
    sampleValue: "alex.smith@example.com",
    classification: "PII",
    description: "Personal Email Address",
  },
  {
    fieldName: "customer_ssn",
    sampleValue: "987-65-4321",
    classification: "PII",
    description: "US Social Security Number",
  },
  {
    fieldName: "patient_prescription",
    sampleValue: "Amoxicillin 500mg daily",
    classification: "PHI",
    description: "Protected Health Information",
  },
  {
    fieldName: "medical_diagnosis",
    sampleValue: "Hypertension (ICD-10 I10)",
    classification: "PHI",
    description: "Clinical Diagnosis / ICD Code",
  },
  {
    fieldName: "bank_account_number",
    sampleValue: "987654321098",
    classification: "PCI",
    description: "Direct Deposit Financial Account",
  },
  {
    fieldName: "phone_number",
    sampleValue: "+1-555-0199",
    classification: "PII",
    description: "Mobile Phone Number",
  },
  {
    fieldName: "user_full_name",
    sampleValue: "Dr. Sarah Connor",
    classification: "PII",
    description: "Full Legal Identity Name",
  },
  {
    fieldName: "passport_number",
    sampleValue: "A12345678",
    classification: "PII",
    description: "Government Travel Document",
  },
  {
    fieldName: "ip_address",
    sampleValue: "192.168.1.1",
    classification: "PII",
    description: "Client IPv4 / IPv6 Address",
  },
  {
    fieldName: "product_inventory_sku",
    sampleValue: "SKU-9921-EU",
    classification: "NON_SENSITIVE",
    description: "E-Commerce Product SKU",
  },
];

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

  function handleSelectField(val, opt) {
    setForm((prev) => ({
      ...prev,
      fieldName: val,
      sampleValue: opt?.sampleValue || prev.sampleValue,
    }));
    setFormError(null);
  }

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
                  Field Name * <span className="text-[var(--color-text-faint)]">(select standard field or type custom)</span>
                </label>
                <SearchableCombobox
                  value={form.fieldName}
                  onChange={handleSelectField}
                  options={STANDARD_SCHEMA_FIELDS}
                  getOptionLabel={(opt) => opt.fieldName}
                  getOptionValue={(opt) => opt.fieldName}
                  placeholder="Select schema field or type custom..."
                  searchPlaceholder="Search standard fields or type..."
                  allowCustom={true}
                  renderOption={(opt, { isSelected, isHighlighted }) => (
                    <div className="px-3 py-2.5 rounded-lg flex items-center justify-between gap-3 text-xs">
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2">
                          <span className="font-semibold font-mono text-white truncate">{opt.fieldName}</span>
                          <span
                            className={`text-[10px] font-mono px-1.5 py-0.5 rounded border ${
                              opt.classification === "PII"
                                ? "bg-blue-500/15 text-blue-400 border-blue-500/30"
                                : opt.classification === "PCI"
                                ? "bg-amber-500/15 text-amber-400 border-amber-500/30"
                                : opt.classification === "PHI"
                                ? "bg-emerald-500/15 text-emerald-400 border-emerald-500/30"
                                : "bg-slate-500/15 text-slate-400 border-slate-500/30"
                            }`}
                          >
                            {opt.classification}
                          </span>
                        </div>
                        {opt.description && (
                          <p className="text-[11px] text-[var(--color-text-faint)] truncate mt-0.5">
                            {opt.description}
                          </p>
                        )}
                        {opt.sampleValue && (
                          <p className="text-[10px] text-[var(--color-text-dim)] font-mono truncate mt-0.5">
                            ex: {opt.sampleValue}
                          </p>
                        )}
                      </div>
                      {isSelected && <Check size={14} className="text-[var(--color-brand)] shrink-0" />}
                    </div>
                  )}
                />
              </div>

              <div>
                <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">
                  Sample Value <span className="text-[var(--color-text-faint)]">(optional, enhances LLM accuracy)</span>
                </label>
                <input
                  value={form.sampleValue}
                  onChange={(e) => setForm((prev) => ({ ...prev, sampleValue: e.target.value }))}
                  placeholder="e.g. 4111-XXXX-XXXX-1111"
                  className="field-input text-xs font-mono"
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
                { tag: "PII", desc: "Personally Identifiable Info (GDPR Art. 4)" },
                { tag: "PCI", desc: "Payment Card Data (PCI-DSS Req. 3)" },
                { tag: "PHI", desc: "Protected Health Info (HIPAA § 164.514)" },
                { tag: "NON_SENSITIVE", desc: "Public or internal operational data" },
              ].map(({ tag, desc }) => (
                <div key={tag} className="flex gap-2">
                  <span className="font-mono text-white font-semibold w-24 shrink-0">{tag}</span>
                  <span>{desc}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Results Column */}
        <div className="xl:col-span-3">
          <div className="card overflow-hidden">
            <div className="px-5 py-4 border-b border-[var(--color-border)] flex items-center justify-between">
              <div>
                <h2 className="font-semibold text-white text-sm">Classification Results & Human Review</h2>
                <p className="text-xs text-[var(--color-text-faint)]">
                  Approve or reject automated sensitivity tags. Approved classifications feed active policy ASTs.
                </p>
              </div>
              {results.length > 0 && (
                <button
                  onClick={clearHistory}
                  className="text-xs text-[var(--color-text-faint)] hover:text-white transition-colors"
                >
                  Clear
                </button>
              )}
            </div>

            {results.length === 0 && (
              <div className="px-5 py-16 text-center">
                <Sparkles size={32} className="mx-auto mb-3 text-[var(--color-text-faint)]" />
                <p className="text-sm text-[var(--color-text-dim)]">No schema fields classified in this session.</p>
                <p className="text-xs text-[var(--color-text-faint)] mt-1">
                  Select a standard field from the dropdown or type a custom field name on the left to classify.
                </p>
              </div>
            )}

            {results.length > 0 && (
              <>
                <div className="divide-y divide-[var(--color-border)]">
                  {paginatedResults.map((r) => (
                    <div key={r.id} className="p-5 space-y-3 hover:bg-[var(--color-surface-2)] transition-colors">
                      <div className="flex items-start justify-between gap-4">
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="font-mono text-sm font-semibold text-white">{r.fieldName}</span>
                            <span className={statusBadge(r.status)}>{r.status}</span>
                            <span className="text-[10px] px-1.5 py-0.5 rounded bg-[var(--color-surface-2)] text-[var(--color-text-faint)] font-mono">
                              via {r.provider || "heuristics"}
                            </span>
                          </div>
                          {r.sampleValue && (
                            <p className="text-xs text-[var(--color-text-faint)] mt-0.5 font-mono truncate max-w-sm">
                              sample: {r.sampleValue}
                            </p>
                          )}
                        </div>

                        {canApprove && r.status === "PENDING" && (
                          <div className="flex items-center gap-2 shrink-0">
                            <button
                              onClick={() => handleApprove(r.id)}
                              className="btn-ghost flex items-center gap-1 text-xs text-[var(--color-good)] hover:bg-[var(--color-good)]/10"
                              title="Approve tag"
                            >
                              <CheckCircle2 size={14} /> Approve
                            </button>
                            <button
                              onClick={() => handleReject(r.id)}
                              className="btn-ghost flex items-center gap-1 text-xs text-[var(--color-bad)] hover:bg-[var(--color-bad)]/10"
                              title="Reject tag"
                            >
                              <XCircle size={14} /> Reject
                            </button>
                          </div>
                        )}
                      </div>

                      <div className="flex flex-wrap items-center gap-4 text-xs">
                        <div>
                          <span className="text-[var(--color-text-faint)]">Classification: </span>
                          <span className="font-semibold text-white">{r.classification}</span>
                        </div>
                        <div>
                          <span className="text-[var(--color-text-faint)]">Confidence: </span>
                          <span
                            className="font-semibold font-mono"
                            style={{ color: CONFIDENCE_COLOR(r.confidence) }}
                          >
                            {(r.confidence * 100).toFixed(1)}%
                          </span>
                        </div>
                        {r.reviewedBy && (
                          <div>
                            <span className="text-[var(--color-text-faint)]">Reviewer: </span>
                            <span className="text-[var(--color-text-dim)]">{r.reviewedBy}</span>
                          </div>
                        )}
                        {r.explanation && (
                          <div className="w-full text-xs text-[var(--color-text-dim)] bg-[var(--color-surface)] p-2.5 rounded-lg border border-[var(--color-border)]">
                            {r.explanation}
                          </div>
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
