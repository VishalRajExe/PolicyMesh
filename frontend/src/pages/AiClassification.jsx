import { useState, useEffect, useCallback } from "react";
import {
  Sparkles,
  CheckCircle2,
  XCircle,
  Loader2,
  HelpCircle,
  RotateCcw,
  Check,
  RefreshCw,
  ShieldCheck,
  AlertTriangle,
  Info,
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
  const [loadingList, setLoadingList] = useState(false);
  const [formError, setFormError] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [statusFilter, setStatusFilter] = useState("ALL");

  // Classification results
  const [results, setResults] = useState([]);

  // Pagination for results list
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const fetchClassifications = useCallback(async () => {
    setLoadingList(true);
    try {
      const data = await aiApi.list();
      if (Array.isArray(data)) {
        setResults(data);
      }
    } catch {
      // Fallback
    } finally {
      setLoadingList(false);
    }
  }, []);

  useEffect(() => {
    fetchClassifications();
  }, [fetchClassifications]);

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
      setFormError("Field name must be alphanumeric with dots, underscores, or hyphens.");
      return;
    }
    setFormError(null);
    setActionError(null);
    setSubmitting(true);
    try {
      const res = await aiApi.classify(name, form.sampleValue.trim() || undefined);
      setResults((prev) => [res, ...prev.filter((r) => r.id !== res.id)]);
      clearDraft();
    } catch (err) {
      setFormError(err.message || "Failed to classify schema field.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleApprove(id) {
    setActionError(null);
    try {
      const updated = await aiApi.approve(id);
      setResults((prev) => prev.map((r) => (r.id === id ? updated : r)));
    } catch (err) {
      setActionError(err.message || "Failed to approve classification.");
    }
  }

  async function handleReject(id) {
    setActionError(null);
    try {
      const updated = await aiApi.reject(id);
      setResults((prev) => prev.map((r) => (r.id === id ? updated : r)));
    } catch (err) {
      setActionError(err.message || "Failed to reject classification.");
    }
  }

  const statusBadge = (status) => {
    const map = {
      PENDING: { bg: "bg-amber-500/15 border-amber-500/30", text: "text-amber-400" },
      APPROVED: { bg: "bg-[var(--color-good)]/15 border-[var(--color-good)]/30", text: "text-[var(--color-good)]" },
      REJECTED: { bg: "bg-[var(--color-bad)]/15 border-[var(--color-bad)]/30", text: "text-[var(--color-bad)]" },
    };
    const s = map[status] || map.PENDING;
    return `text-[10px] font-bold px-2 py-0.5 rounded border font-mono ${s.bg} ${s.text}`;
  };

  const filteredResults = results.filter((r) => {
    if (statusFilter === "ALL") return true;
    return r.status === statusFilter;
  });

  const paginatedResults = filteredResults.slice((page - 1) * pageSize, page * pageSize);

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
                <div className="text-xs text-[var(--color-bad)] bg-[var(--color-bad)]/10 border border-[var(--color-bad)]/30 rounded-lg p-2.5 flex items-start gap-2">
                  <AlertTriangle size={14} className="shrink-0 mt-0.5" />
                  <div>
                    <p className="font-semibold">Validation Error</p>
                    <p className="mt-0.5">{formError}</p>
                  </div>
                </div>
              )}

              <button type="submit" disabled={submitting} className="btn-primary w-full justify-center text-xs">
                {submitting ? <Loader2 size={15} className="animate-spin" /> : <Sparkles size={15} />}
                {submitting ? "Analyzing Sensitivity via AI…" : "Classify Field"}
              </button>
            </form>

            {/* Supported Tags Info */}
            <div className="bg-[var(--color-surface-2)] rounded-xl p-3 text-xs text-[var(--color-text-faint)] space-y-1.5 border border-[var(--color-border)]/50">
              <p className="font-semibold text-white flex items-center gap-1.5">
                <Info size={13} className="text-[var(--color-brand)]" /> Supported Sensitivity Tags
              </p>
              {[
                { tag: "PII", desc: "Personally Identifiable Info (GDPR Art. 4)" },
                { tag: "PCI", desc: "Payment Card Data (PCI-DSS Req. 3)" },
                { tag: "PHI", desc: "Protected Health Info (HIPAA § 164.514)" },
                { tag: "NON_SENSITIVE", desc: "Public or operational metadata" },
              ].map(({ tag, desc }) => (
                <div key={tag} className="flex items-center gap-2 text-[11px]">
                  <span className="font-mono text-white font-semibold w-24 shrink-0">{tag}</span>
                  <span className="text-[var(--color-text-dim)]">{desc}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Results Column */}
        <div className="xl:col-span-3">
          <div className="card overflow-hidden">
            <div className="px-5 py-4 border-b border-[var(--color-border)] flex flex-wrap items-center justify-between gap-3">
              <div>
                <h2 className="font-semibold text-white text-sm">Classification Results & Human Review</h2>
                <p className="text-xs text-[var(--color-text-faint)]">
                  Approve or reject automated sensitivity tags. Approved classifications feed active policy ASTs.
                </p>
              </div>

              <div className="flex items-center gap-2">
                {/* Status Filter Tabs */}
                <div className="flex bg-[var(--color-surface-2)] rounded-lg p-0.5 border border-[var(--color-border)] text-xs">
                  {["ALL", "PENDING", "APPROVED", "REJECTED"].map((s) => (
                    <button
                      key={s}
                      onClick={() => {
                        setStatusFilter(s);
                        setPage(1);
                      }}
                      className={`px-2.5 py-1 rounded-md text-[11px] font-medium transition-colors ${
                        statusFilter === s
                          ? "bg-[var(--color-brand)] text-white font-semibold"
                          : "text-[var(--color-text-dim)] hover:text-white"
                      }`}
                    >
                      {s}
                    </button>
                  ))}
                </div>

                <button
                  onClick={fetchClassifications}
                  disabled={loadingList}
                  className="p-1.5 rounded-lg border border-[var(--color-border)] text-[var(--color-text-faint)] hover:text-white hover:border-[var(--color-brand)] transition-colors"
                  title="Refresh from Database"
                >
                  <RefreshCw size={13} className={loadingList ? "animate-spin" : ""} />
                </button>
              </div>
            </div>

            {actionError && (
              <div className="m-4 text-xs text-[var(--color-bad)] bg-[var(--color-bad)]/10 border border-[var(--color-bad)]/30 rounded-lg p-3 flex items-start gap-2">
                <AlertTriangle size={14} className="shrink-0 mt-0.5" />
                <p>{actionError}</p>
              </div>
            )}

            {filteredResults.length === 0 && !loadingList && (
              <div className="px-5 py-16 text-center">
                <Sparkles size={32} className="mx-auto mb-3 text-[var(--color-text-faint)] opacity-60" />
                <p className="text-sm font-semibold text-white">No {statusFilter !== "ALL" ? statusFilter.toLowerCase() : ""} classifications found.</p>
                <p className="text-xs text-[var(--color-text-faint)] mt-1">
                  Select a schema field on the left and click "Classify Field" to start an automated sensitivity check.
                </p>
              </div>
            )}

            {loadingList && filteredResults.length === 0 && (
              <div className="px-5 py-16 text-center">
                <Loader2 size={28} className="animate-spin mx-auto mb-3 text-[var(--color-brand)]" />
                <p className="text-xs text-[var(--color-text-dim)]">Loading classification records from database…</p>
              </div>
            )}

            {filteredResults.length > 0 && (
              <>
                <div className="divide-y divide-[var(--color-border)]">
                  {paginatedResults.map((r) => {
                    const tag = r.classification || r.suggestedClass || "UNKNOWN";
                    return (
                      <div key={r.id} className="p-5 space-y-3 hover:bg-[var(--color-surface-2)] transition-colors">
                        <div className="flex items-start justify-between gap-4">
                          <div>
                            <div className="flex items-center gap-2 flex-wrap">
                              <span className="font-mono text-sm font-bold text-white">{r.fieldName}</span>
                              <span className={statusBadge(r.status)}>{r.status}</span>
                              <span className="text-[10px] px-1.5 py-0.5 rounded bg-[var(--color-surface-2)] text-[var(--color-text-faint)] font-mono border border-[var(--color-border)]">
                                via {r.provider || "heuristics"}
                              </span>
                            </div>
                            {r.sampleValue && (
                              <p className="text-xs text-[var(--color-text-faint)] mt-1 font-mono truncate max-w-md">
                                sample: {r.sampleValue}
                              </p>
                            )}
                          </div>

                          {r.status === "PENDING" && canApprove && (
                            <div className="flex items-center gap-2 shrink-0">
                              <button
                                onClick={() => handleApprove(r.id)}
                                className="px-2.5 py-1 rounded-lg bg-[var(--color-good)]/15 border border-[var(--color-good)]/40 hover:bg-[var(--color-good)]/25 text-[var(--color-good)] text-xs font-semibold flex items-center gap-1 transition-colors"
                                title="Approve this tag into policy enforcement"
                              >
                                <CheckCircle2 size={13} /> Approve
                              </button>
                              <button
                                onClick={() => handleReject(r.id)}
                                className="px-2.5 py-1 rounded-lg bg-[var(--color-bad)]/15 border border-[var(--color-bad)]/40 hover:bg-[var(--color-bad)]/25 text-[var(--color-bad)] text-xs font-semibold flex items-center gap-1 transition-colors"
                                title="Reject this suggestion"
                              >
                                <XCircle size={13} /> Reject
                              </button>
                            </div>
                          )}

                          {r.status === "PENDING" && !canApprove && (
                            <span className="text-[11px] text-[var(--color-text-faint)] italic">
                              Review requires Admin/Compliance role
                            </span>
                          )}
                        </div>

                        <div className="flex flex-wrap items-center gap-4 text-xs pt-1">
                          <div className="flex items-center gap-1.5">
                            <span className="text-[var(--color-text-faint)]">Classification:</span>
                            <span
                              className={`font-mono font-bold px-2 py-0.5 rounded border text-xs ${
                                tag === "PII"
                                  ? "bg-blue-500/15 text-blue-400 border-blue-500/30"
                                  : tag === "PCI"
                                  ? "bg-amber-500/15 text-amber-400 border-amber-500/30"
                                  : tag === "PHI"
                                  ? "bg-emerald-500/15 text-emerald-400 border-emerald-500/30"
                                  : "bg-slate-500/15 text-slate-300 border-slate-500/30"
                              }`}
                            >
                              {tag}
                            </span>
                          </div>

                          <div className="flex items-center gap-1.5">
                            <span className="text-[var(--color-text-faint)]">Confidence:</span>
                            <span
                              className="font-bold font-mono"
                              style={{ color: CONFIDENCE_COLOR(r.confidence) }}
                            >
                              {(r.confidence * 100).toFixed(1)}%
                            </span>
                          </div>

                          {r.reviewedBy && (
                            <div className="flex items-center gap-1.5">
                              <span className="text-[var(--color-text-faint)]">Reviewer:</span>
                              <span className="text-white font-mono text-[11px]">{r.reviewedBy}</span>
                            </div>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>

                <Pagination
                  currentPage={page}
                  totalItems={filteredResults.length}
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
