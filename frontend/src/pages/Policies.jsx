import { useEffect, useState } from "react";
import {
  Plus,
  Trash2,
  Loader2,
  ToggleLeft,
  ToggleRight,
  Search,
  Filter,
  RotateCcw,
  Zap,
  FileText,
} from "lucide-react";
import Topbar from "../components/layout/Topbar";
import Pagination from "../components/ui/Pagination";
import { policiesApi } from "../api";
import { useAuth } from "../context/AuthContext";
import { useFormDraft } from "../hooks/useFormDraft";
import { useQueryState } from "../hooks/useQueryState";

const JURISDICTIONS = ["EU", "US", "IN", "CN", "UK", "GLOBAL", "AP"];
const DATA_CLASSES = ["PII", "PCI", "PHI", "NON_SENSITIVE", "UNKNOWN"];

const EMPTY_FORM = {
  policyCode: "",
  name: "",
  jurisdiction: "",
  dataClass: "",
  allowedRegions: "",
  deniedRegions: "",
};

const POLICY_PRESETS = [
  {
    label: "EU GDPR PII Protection",
    policyCode: "EU-PII-002",
    name: "EU PII Cross-Border Restriction",
    jurisdiction: "EU",
    dataClass: "PII",
    allowedRegions: "EU, UK",
    deniedRegions: "US, CN, IN",
  },
  {
    label: "India DPDPA Protection",
    policyCode: "IN-PII-002",
    name: "India Digital Personal Data Act",
    jurisdiction: "IN",
    dataClass: "PII",
    allowedRegions: "IN",
    deniedRegions: "GLOBAL",
  },
  {
    label: "Global PCI DSS Payment Security",
    policyCode: "GL-PCI-001",
    name: "Global Cardholder Data Protection",
    jurisdiction: "GLOBAL",
    dataClass: "PCI",
    allowedRegions: "EU, US, IN",
    deniedRegions: "CN",
  },
];

const STATUS_STYLE = {
  ACTIVE: "bg-[#22c55e]/15 text-[#4ade80] border border-[#22c55e]/30",
  DRAFT: "bg-[#3b82f6]/15 text-[#60a5fa] border border-[#3b82f6]/30",
  UNDER_REVIEW: "bg-[#f59e0b]/15 text-[#fbbf24] border border-[#f59e0b]/30",
  INACTIVE: "bg-[#5b6478]/15 text-[#8b93a7] border border-[#5b6478]/30",
};

export default function Policies() {
  const { user } = useAuth();
  const [policies, setPolicies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // URL query state
  const [search, setSearch] = useQueryState("search", "");
  const [statusFilter, setStatusFilter] = useQueryState("status", "ALL");
  const [jurisdictionFilter, setJurisdictionFilter] = useQueryState("jurisdiction", "ALL");
  const [page, setPage] = useQueryState("page", 1);
  const [pageSize, setPageSize] = useQueryState("size", 10);

  // Form draft state
  const { values: form, setValues: setForm, clearDraft, resetForm } = useFormDraft(
    "policy-editor",
    EMPTY_FORM
  );
  const [showForm, setShowForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [togglingId, setTogglingId] = useState(null);

  const canWrite = user?.role === "ADMIN" || user?.role === "COMPLIANCE_OFFICER";

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const data = await policiesApi.list();
      setPolicies(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function handleCreate(e) {
    e.preventDefault();
    const trimmed = {
      policyCode: form.policyCode.trim().toUpperCase(),
      name: form.name.trim(),
      jurisdiction: form.jurisdiction.trim(),
      dataClass: form.dataClass.trim(),
      allowedRegions: splitCsv(form.allowedRegions),
      deniedRegions: splitCsv(form.deniedRegions),
    };
    if (!trimmed.policyCode || !trimmed.name || !trimmed.jurisdiction || !trimmed.dataClass) {
      setError("All required fields must be filled.");
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await policiesApi.create(trimmed);
      clearDraft();
      setShowForm(false);
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(id) {
    try {
      await policiesApi.remove(id);
      await load();
    } catch (err) {
      setError(err.message);
    }
  }

  async function toggleStatus(p) {
    setTogglingId(p.id);
    try {
      const nextStatus = p.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
      await policiesApi.update(p.id, {
        policyCode: p.policyCode,
        name: p.name,
        jurisdiction: p.jurisdiction,
        dataClass: p.dataClass,
        allowedRegions: [...p.allowedRegions],
        deniedRegions: [...p.deniedRegions],
        status: nextStatus,
      });
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setTogglingId(null);
    }
  }

  function applyPreset(p) {
    setForm({
      policyCode: p.policyCode,
      name: p.name,
      jurisdiction: p.jurisdiction,
      dataClass: p.dataClass,
      allowedRegions: p.allowedRegions,
      deniedRegions: p.deniedRegions,
    });
  }

  const setField = (k) => (v) => setForm((f) => ({ ...f, [k]: v }));

  // Filtered policies
  const filteredPolicies = policies.filter((p) => {
    const q = search.trim().toLowerCase();
    const matchSearch =
      !q ||
      p.policyCode.toLowerCase().includes(q) ||
      p.name.toLowerCase().includes(q) ||
      p.dataClass.toLowerCase().includes(q);
    const matchStatus = statusFilter === "ALL" || p.status === statusFilter;
    const matchJurisdiction = jurisdictionFilter === "ALL" || p.jurisdiction === jurisdictionFilter;
    return matchSearch && matchStatus && matchJurisdiction;
  });

  const paginatedPolicies = filteredPolicies.slice((page - 1) * pageSize, page * pageSize);

  return (
    <div>
      <Topbar
        title="Policies"
        subtitle="Manage the data-residency rules and jurisdictional constraints that govern every transfer."
      />

      <div className="px-6 lg:px-8 mt-4 space-y-4 pb-12">
        {/* Controls Bar */}
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3 flex-1 max-w-2xl">
            {/* Search */}
            <div className="relative flex-1 min-w-[200px]">
              <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--color-text-faint)]" />
              <input
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(1);
                }}
                placeholder="Search policies by code, name, or class..."
                className="field-input pl-9 text-xs"
              />
            </div>

            {/* Jurisdiction filter */}
            <select
              value={jurisdictionFilter}
              onChange={(e) => {
                setJurisdictionFilter(e.target.value);
                setPage(1);
              }}
              className="field-input py-1.5 text-xs w-36"
            >
              <option value="ALL">All Jurisdictions</option>
              {JURISDICTIONS.map((j) => (
                <option key={j} value={j}>
                  {j}
                </option>
              ))}
            </select>

            {/* Status filter */}
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(1);
              }}
              className="field-input py-1.5 text-xs w-32"
            >
              <option value="ALL">All Statuses</option>
              <option value="ACTIVE">ACTIVE</option>
              <option value="INACTIVE">INACTIVE</option>
              <option value="DRAFT">DRAFT</option>
            </select>
          </div>

          {canWrite && (
            <button
              onClick={() => setShowForm((s) => !s)}
              className="btn-primary flex items-center gap-1.5 text-xs"
            >
              <Plus size={15} />
              {showForm ? "Close Form" : "New Policy"}
            </button>
          )}
        </div>

        {/* Create Form Modal / Card */}
        {showForm && (
          <div className="card p-5 space-y-4 border-[var(--color-brand)]/40 animate-in fade-in zoom-in-95">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <FileText size={17} className="text-[var(--color-brand)]" />
                <h3 className="text-sm font-semibold text-white">Create New Data Governance Policy</h3>
              </div>
              <button
                type="button"
                onClick={resetForm}
                className="text-xs text-[var(--color-text-faint)] hover:text-white flex items-center gap-1"
              >
                <RotateCcw size={12} /> Reset Draft
              </button>
            </div>

            {/* Presets */}
            <div className="space-y-1.5">
              <label className="block text-[11px] text-[var(--color-text-faint)] font-medium uppercase tracking-wider flex items-center gap-1">
                <Zap size={12} className="text-[var(--color-brand)]" /> Quick Presets
              </label>
              <div className="flex flex-wrap gap-2">
                {POLICY_PRESETS.map((p, i) => (
                  <button
                    key={i}
                    type="button"
                    onClick={() => applyPreset(p)}
                    className="text-xs px-2.5 py-1 rounded-lg bg-[var(--color-surface-2)] border border-[var(--color-border)] hover:border-[var(--color-brand)]/50 hover:bg-[var(--color-surface)] text-[var(--color-text-dim)] hover:text-white transition-colors"
                  >
                    {p.label}
                  </button>
                ))}
              </div>
            </div>

            <form onSubmit={handleCreate} className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-1">
              <div>
                <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">
                  Policy Code * <span className="text-[var(--color-text-faint)]">(e.g. EU-PII-001)</span>
                </label>
                <input
                  value={form.policyCode}
                  onChange={(e) => setField("policyCode")(e.target.value.toUpperCase())}
                  placeholder="EU-PII-001"
                  className="field-input text-xs"
                  required
                />
              </div>

              <div>
                <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">Policy Name *</label>
                <input
                  value={form.name}
                  onChange={(e) => setField("name")(e.target.value)}
                  placeholder="EU PII Data Residency"
                  className="field-input text-xs"
                  required
                />
              </div>

              <div>
                <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">Jurisdiction *</label>
                <select
                  value={form.jurisdiction}
                  onChange={(e) => setField("jurisdiction")(e.target.value)}
                  className="field-input text-xs"
                  required
                >
                  <option value="">Select Jurisdiction…</option>
                  {JURISDICTIONS.map((j) => (
                    <option key={j} value={j}>
                      {j}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">Data Sensitivity Class *</label>
                <select
                  value={form.dataClass}
                  onChange={(e) => setField("dataClass")(e.target.value)}
                  className="field-input text-xs"
                  required
                >
                  <option value="">Select Class…</option>
                  {DATA_CLASSES.map((d) => (
                    <option key={d} value={d}>
                      {d}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">
                  Allowed Regions <span className="text-[var(--color-text-faint)]">(comma-separated)</span>
                </label>
                <input
                  value={form.allowedRegions}
                  onChange={(e) => setField("allowedRegions")(e.target.value)}
                  placeholder="EU, UK"
                  className="field-input text-xs"
                />
              </div>

              <div>
                <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">
                  Denied Regions <span className="text-[var(--color-text-faint)]">(comma-separated)</span>
                </label>
                <input
                  value={form.deniedRegions}
                  onChange={(e) => setField("deniedRegions")(e.target.value)}
                  placeholder="US, CN"
                  className="field-input text-xs"
                />
              </div>

              <div className="md:col-span-3 flex justify-end gap-3 pt-2">
                <button type="button" onClick={() => setShowForm(false)} className="btn-ghost text-xs">
                  Cancel
                </button>
                <button type="submit" disabled={submitting} className="btn-primary text-xs">
                  {submitting ? <Loader2 size={14} className="animate-spin" /> : <Plus size={14} />}
                  Save Policy
                </button>
              </div>
            </form>
          </div>
        )}

        {error && (
          <div className="rounded-xl bg-[var(--color-bad)]/10 border border-[var(--color-bad)]/30 px-4 py-3 text-sm text-[var(--color-bad)] flex items-center justify-between">
            {error}
            <button onClick={load} className="underline ml-4 text-xs">
              Retry
            </button>
          </div>
        )}

        {/* Table */}
        <div className="card overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-[var(--color-text-faint)] border-b border-[var(--color-border)]">
                <th className="px-5 py-3 font-medium">Policy Code</th>
                <th className="px-5 py-3 font-medium">Name</th>
                <th className="px-5 py-3 font-medium">Jurisdiction</th>
                <th className="px-5 py-3 font-medium">Data Class</th>
                <th className="px-5 py-3 font-medium">Allowed Regions</th>
                <th className="px-5 py-3 font-medium">Denied Regions</th>
                <th className="px-5 py-3 font-medium">Status</th>
                <th className="px-5 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr>
                  <td colSpan={8} className="px-5 py-10 text-center text-[var(--color-text-faint)]">
                    <Loader2 size={16} className="animate-spin inline mr-2" /> Loading policies...
                  </td>
                </tr>
              )}
              {!loading && filteredPolicies.length === 0 && (
                <tr>
                  <td colSpan={8} className="px-5 py-12 text-center text-[var(--color-text-faint)]">
                    No policies match the search and filter criteria.
                  </td>
                </tr>
              )}
              {!loading &&
                paginatedPolicies.map((p) => (
                  <tr
                    key={p.id}
                    className="border-b border-[var(--color-border)] last:border-0 hover:bg-[var(--color-surface-2)] transition-colors"
                  >
                    <td className="px-5 py-3 text-white font-mono text-xs font-semibold">{p.policyCode}</td>
                    <td className="px-5 py-3 text-white text-xs">{p.name}</td>
                    <td className="px-5 py-3">
                      <span className="text-xs font-mono px-2 py-0.5 rounded bg-blue-500/15 text-blue-400 border border-blue-500/30">
                        {p.jurisdiction}
                      </span>
                    </td>
                    <td className="px-5 py-3">
                      <span className="text-xs font-semibold px-2 py-0.5 rounded bg-amber-500/15 text-amber-400 border border-amber-500/30">
                        {p.dataClass}
                      </span>
                    </td>
                    <td className="px-5 py-3 text-xs text-[var(--color-text-dim)] font-mono">
                      {[...p.allowedRegions].join(", ") || "GLOBAL"}
                    </td>
                    <td className="px-5 py-3 text-xs text-[var(--color-text-dim)] font-mono">
                      {[...p.deniedRegions].join(", ") || "NONE"}
                    </td>
                    <td className="px-5 py-3">
                      <span
                        className={`text-xs font-medium px-2 py-0.5 rounded-lg ${
                          STATUS_STYLE[p.status] || STATUS_STYLE.INACTIVE
                        }`}
                      >
                        {p.status}
                      </span>
                    </td>
                    <td className="px-5 py-3 text-right">
                      <div className="flex items-center justify-end gap-2">
                        {canWrite && (
                          <button
                            onClick={() => toggleStatus(p)}
                            disabled={togglingId === p.id}
                            title={p.status === "ACTIVE" ? "Deactivate" : "Activate"}
                            className={`p-1 transition-colors disabled:opacity-50 ${
                              p.status === "ACTIVE"
                                ? "text-[var(--color-good)] hover:text-white"
                                : "text-[var(--color-text-faint)] hover:text-[var(--color-good)]"
                            }`}
                          >
                            {togglingId === p.id ? (
                              <Loader2 size={15} className="animate-spin" />
                            ) : p.status === "ACTIVE" ? (
                              <ToggleRight size={18} />
                            ) : (
                              <ToggleLeft size={18} />
                            )}
                          </button>
                        )}
                        {canWrite && (
                          <button
                            onClick={() => handleDelete(p.id)}
                            className="text-[var(--color-text-faint)] hover:text-[var(--color-bad)] p-1 transition-colors"
                            title="Delete Policy"
                          >
                            <Trash2 size={14} />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>

          <Pagination
            currentPage={page}
            totalItems={filteredPolicies.length}
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

function splitCsv(value) {
  if (!value) return [];
  return value
    .split(",")
    .map((v) => v.trim())
    .filter(Boolean);
}
