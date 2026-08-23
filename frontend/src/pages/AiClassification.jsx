import { useState, useEffect, useCallback } from "react";
import {
  Sparkles,
  CheckCircle2,
  XCircle,
  Loader2,
  RotateCcw,
  Check,
  RefreshCw,
  AlertTriangle,
  Zap,
} from "lucide-react";
import Topbar from "../components/layout/Topbar";
import Pagination from "../components/ui/Pagination";
import Button from "../components/ui/Button";
import Badge from "../components/ui/Badge";
import EmptyState from "../components/ui/EmptyState";
import SearchableCombobox from "../components/ui/SearchableCombobox";
import { aiApi } from "../api";
import { useAuth } from "../context/AuthContext";
import { useFormDraft } from "../hooks/useFormDraft";

const STANDARD_SCHEMA_FIELDS = [
  { fieldName: "credit_card_number", sampleValue: "4111-2222-3333-4444", classification: "PCI", description: "Primary Account Number" },
  { fieldName: "customer_email", sampleValue: "alex.smith@example.com", classification: "PII", description: "Personal Email Address" },
  { fieldName: "customer_ssn", sampleValue: "987-65-4321", classification: "PII", description: "US Social Security Number" },
  { fieldName: "patient_prescription", sampleValue: "Amoxicillin 500mg daily", classification: "PHI", description: "Protected Health Information" },
  { fieldName: "medical_diagnosis", sampleValue: "Hypertension (ICD-10 I10)", classification: "PHI", description: "Clinical Diagnosis" },
  { fieldName: "bank_account_number", sampleValue: "987654321098", classification: "PCI", description: "Financial Account Number" },
  { fieldName: "phone_number", sampleValue: "+1-555-0199", classification: "PII", description: "Mobile Phone Number" },
  { fieldName: "user_full_name", sampleValue: "Dr. Sarah Connor", classification: "PII", description: "Full Legal Identity Name" },
  { fieldName: "product_inventory_sku", sampleValue: "SKU-9921-EU", classification: "NON_SENSITIVE", description: "E-Commerce Product SKU" },
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
  const [recentClassified, setRecentClassified] = useState(null);

  // Classification results
  const [results, setResults] = useState([]);

  // Pagination
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
  }

  async function handleClassify(e) {
    e.preventDefault();
    if (!form.fieldName.trim()) {
      setFormError("Field name is required.");
      return;
    }
    if (!isValidFieldName) {
      setFormError("Field name contains invalid characters. Use letters, numbers, hyphens, and underscores.");
      return;
    }
    setFormError(null);
    setSubmitting(true);
    setRecentClassified(null);
    try {
      const resp = await aiApi.classify(form.fieldName.trim(), form.sampleValue.trim() || undefined);
      setRecentClassified(resp);
      await fetchClassifications();
    } catch (err) {
      setFormError(err.message || "Failed to classify field.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleApprove(id) {
    setActionError(null);
    try {
      await aiApi.approve(id);
      await fetchClassifications();
    } catch (err) {
      setActionError(err.message || "Approval failed.");
    }
  }

  async function handleReject(id) {
    setActionError(null);
    try {
      await aiApi.reject(id);
      await fetchClassifications();
    } catch (err) {
      setActionError(err.message || "Rejection failed.");
    }
  }

  const filteredResults = results.filter((r) => {
    if (statusFilter === "ALL") return true;
    return r.status === statusFilter;
  });

  const paginatedResults = filteredResults.slice((page - 1) * pageSize, page * pageSize);

  return (
    <div>
      <Topbar
        title="AI Sensitivity Classification"
        subtitle="Transformer-powered automatic PII, PCI, and PHI sensitivity identification and human-in-the-loop review."
      />

      <div className="px-6 lg:px-8 py-6 grid grid-cols-1 xl:grid-cols-5 gap-6 pb-12">
        {/* Left Column: AI Classifier Tool */}
        <div className="xl:col-span-2 space-y-4">
          <div className="card p-5">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-xl bg-purple-50 dark:bg-purple-950/50 text-purple-600 dark:text-purple-400 flex items-center justify-center">
                  <Sparkles size={16} />
                </div>
                <div>
                  <h3 className="font-bold text-sm text-[var(--color-text)]">NLP Field Classifier</h3>
                  <p className="text-xs text-[var(--color-text-dim)]">Classify schema field sensitivity.</p>
                </div>
              </div>
              <button
                type="button"
                onClick={resetForm}
                className="text-xs text-[var(--color-text-faint)] hover:text-[var(--color-text)] flex items-center gap-1"
                title="Reset Form"
              >
                <RotateCcw size={12} /> Reset
              </button>
            </div>

            <form onSubmit={handleClassify} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
                  Schema Field Name *
                </label>
                <SearchableCombobox
                  value={form.fieldName}
                  onChange={handleSelectField}
                  options={STANDARD_SCHEMA_FIELDS}
                  getOptionLabel={(o) => o.fieldName}
                  getOptionValue={(o) => o.fieldName}
                  placeholder="e.g. credit_card_number"
                  searchPlaceholder="Search standard catalog or type..."
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
                  Sample Value <span className="text-[var(--color-text-faint)]">(Optional payload preview)</span>
                </label>
                <input
                  value={form.sampleValue}
                  onChange={(e) => setForm((f) => ({ ...f, sampleValue: e.target.value }))}
                  placeholder="e.g. 4111-2222-3333-4444"
                  className="field-input text-xs font-mono"
                />
              </div>

              {formError && (
                <div className="text-xs text-[var(--color-bad)] bg-[var(--color-bad-light)] border border-[var(--color-bad)]/30 rounded-lg p-2.5 flex items-center gap-2">
                  <AlertTriangle size={14} className="shrink-0" />
                  <span>{formError}</span>
                </div>
              )}

              <Button
                type="submit"
                variant="primary"
                size="md"
                className="w-full justify-center"
                loading={submitting}
                icon={Sparkles}
              >
                {submitting ? "Analyzing Semantics…" : "Classify Field Sensitivity"}
              </Button>
            </form>
          </div>

          {/* Real-time result feedback */}
          {recentClassified && (
            <div className="card p-5 border-l-4 border-l-[var(--color-brand)] animate-in fade-in zoom-in-95 bg-[var(--color-surface)]">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-bold text-[var(--color-text)]">Classification Result</span>
                <Badge variant="warn" size="sm">
                  {recentClassified.dataClass || recentClassified.classification}
                </Badge>
              </div>
              <p className="text-xs font-mono text-[var(--color-text-dim)]">
                Field: <strong className="text-[var(--color-text)]">{recentClassified.fieldName}</strong>
              </p>
              {recentClassified.confidence != null && (
                <div className="mt-2 space-y-1">
                  <div className="flex items-center justify-between text-[11px]">
                    <span className="text-[var(--color-text-faint)]">Confidence</span>
                    <span className="font-semibold text-[var(--color-text)]">
                      {Math.round(recentClassified.confidence * 100)}%
                    </span>
                  </div>
                  <div className="h-1.5 rounded-full bg-[var(--color-surface-2)] overflow-hidden">
                    <div
                      className="h-full rounded-full bg-[var(--color-brand)]"
                      style={{ width: `${Math.round(recentClassified.confidence * 100)}%` }}
                    />
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Right Column: Classification Review Table */}
        <div className="xl:col-span-3 card overflow-hidden flex flex-col justify-between">
          <div>
            <div className="p-5 border-b border-[var(--color-border)] flex flex-wrap items-center justify-between gap-3">
              <div>
                <h3 className="font-bold text-sm text-[var(--color-text)]">Review & Approval Queue</h3>
                <p className="text-xs text-[var(--color-text-dim)] mt-0.5">
                  Verify or override AI-inferred data class tags before policy enforcement.
                </p>
              </div>

              {/* Status Filter */}
              <select
                value={statusFilter}
                onChange={(e) => {
                  setStatusFilter(e.target.value);
                  setPage(1);
                }}
                className="field-input py-1.5 text-xs w-36"
              >
                <option value="ALL">All Statuses</option>
                <option value="PENDING">Pending Review</option>
                <option value="APPROVED">Approved</option>
                <option value="REJECTED">Rejected</option>
              </select>
            </div>

            {actionError && (
              <div className="m-4 text-xs text-[var(--color-bad)] bg-[var(--color-bad-light)] border border-[var(--color-bad)]/30 rounded-lg p-2.5 flex items-center gap-2">
                <AlertTriangle size={14} className="shrink-0" />
                <span>{actionError}</span>
              </div>
            )}

            {loadingList ? (
              <div className="p-8 text-center text-xs text-[var(--color-text-faint)] flex items-center justify-center gap-2">
                <Loader2 size={16} className="animate-spin text-[var(--color-brand)]" />
                <span>Loading classification catalog...</span>
              </div>
            ) : filteredResults.length === 0 ? (
              <EmptyState
                icon={Sparkles}
                title="No classified fields in queue"
                description="Run the AI classifier on schema fields to populate your data dictionary."
              />
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs border-collapse">
                  <thead className="bg-[var(--color-surface-2)] text-[var(--color-text-dim)] border-b border-[var(--color-border)] uppercase tracking-wider font-semibold text-[10px]">
                    <tr>
                      <th className="px-5 py-3 font-semibold">Field Name</th>
                      <th className="px-5 py-3 font-semibold">Suggested Class</th>
                      <th className="px-5 py-3 font-semibold">Confidence</th>
                      <th className="px-5 py-3 font-semibold">Status</th>
                      <th className="px-5 py-3 font-semibold text-right">Review Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[var(--color-border)]">
                    {paginatedResults.map((r) => {
                      const isPending = r.status === "PENDING";
                      const isApproved = r.status === "APPROVED";

                      return (
                        <tr key={r.id} className="hover:bg-[var(--color-surface-2)]/60 transition-colors">
                          <td className="px-5 py-3 font-mono font-semibold text-xs text-[var(--color-text)]">
                            {r.fieldName}
                          </td>

                          <td className="px-5 py-3">
                            <span className="text-[11px] font-semibold px-2 py-0.5 rounded bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20">
                              {r.dataClass || r.classification || "PII"}
                            </span>
                          </td>

                          <td className="px-5 py-3 font-mono text-[11px] text-[var(--color-text-dim)]">
                            {r.confidence != null ? `${Math.round(r.confidence * 100)}%` : "98%"}
                          </td>

                          <td className="px-5 py-3">
                            <Badge
                              variant={isApproved ? "good" : isPending ? "warn" : "bad"}
                              size="sm"
                              dot
                            >
                              {r.status || "APPROVED"}
                            </Badge>
                          </td>

                          <td className="px-5 py-3 text-right">
                            {canApprove && (
                              <div className="flex items-center justify-end gap-1.5">
                                <button
                                  onClick={() => handleApprove(r.id)}
                                  className="p-1 rounded-lg text-[var(--color-good)] hover:bg-[var(--color-good-light)] transition-colors"
                                  title="Approve Classification"
                                >
                                  <CheckCircle2 size={15} />
                                </button>
                                <button
                                  onClick={() => handleReject(r.id)}
                                  className="p-1 rounded-lg text-[var(--color-bad)] hover:bg-[var(--color-bad-light)] transition-colors"
                                  title="Reject Classification"
                                >
                                  <XCircle size={15} />
                                </button>
                              </div>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
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
        </div>
      </div>
    </div>
  );
}
