import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import {
  Plus,
  Trash2,
  Loader2,
  ToggleLeft,
  ToggleRight,
  Search,
  FileText,
  Upload,
  RotateCcw,
  CheckCircle2,
  AlertTriangle,
  Code,
  Shield,
} from "lucide-react";
import Topbar, { TopbarActions } from "../components/layout/Topbar";
import Pagination from "../components/ui/Pagination";
import Button from "../components/ui/Button";
import Badge from "../components/ui/Badge";
import Modal from "../components/ui/Modal";
import EmptyState from "../components/ui/EmptyState";
import { TableSkeleton } from "../components/ui/LoadingSkeleton";
import { policiesApi } from "../api";
import { useAuth } from "../context/AuthContext";
import { useFormDraft } from "../hooks/useFormDraft";

const JURISDICTIONS = ["EU", "US", "IN", "CN", "UK", "GLOBAL", "AP", "ME"];
const DATA_CLASSES = ["PII", "PCI", "PHI", "NON_SENSITIVE", "UNKNOWN"];

const EMPTY_FORM = {
  policyCode: "",
  name: "",
  jurisdiction: "EU",
  dataClass: "PII",
  allowedRegions: "EU",
  deniedRegions: "US",
};

export default function Policies() {
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  const [policies, setPolicies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filter state
  const [search, setSearch] = useState(() => searchParams.get("search") || "");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [jurisdictionFilter, setJurisdictionFilter] = useState("ALL");
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Modals
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showImportModal, setShowImportModal] = useState(false);
  const [yamlContent, setYamlContent] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [togglingId, setTogglingId] = useState(null);

  // Form draft
  const { values: form, setValues: setForm, clearDraft, resetForm } = useFormDraft(
    "policy-editor",
    EMPTY_FORM
  );

  const canWrite = user?.role === "ADMIN" || user?.role === "COMPLIANCE_OFFICER";

  useEffect(() => {
    const action = searchParams.get("action");
    if (action === "new") setShowCreateModal(true);
    if (action === "import") setShowImportModal(true);
  }, [searchParams]);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const data = await policiesApi.list();
      setPolicies(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || "Failed to load policies");
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
      setShowCreateModal(false);
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleImportYaml(e) {
    e.preventDefault();
    if (!yamlContent.trim()) return;
    setSubmitting(true);
    setError(null);
    try {
      await policiesApi.importYaml(yamlContent);
      setShowImportModal(false);
      setYamlContent("");
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(id) {
    if (!window.confirm("Are you sure you want to delete this policy?")) return;
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

  const setField = (k) => (v) => setForm((f) => ({ ...f, [k]: v }));

  // Filtering
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

  const topActions = canWrite && (
    <TopbarActions
      onImport={() => setShowImportModal(true)}
      onCreate={() => setShowCreateModal(true)}
      createLabel="New Policy"
      importLabel="Import YAML"
    />
  );

  return (
    <div>
      <Topbar
        title="Data Governance Policies"
        subtitle="Manage jurisdictional constraints and cross-border data transfer rules governing runtime traffic."
        actions={topActions}
      />

      <div className="px-6 lg:px-8 py-6 space-y-4 pb-12">
        {/* Error Notification */}
        {error && (
          <div className="text-xs text-[var(--color-bad)] bg-[var(--color-bad-light)] border border-[var(--color-bad)]/20 rounded-xl p-3 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <AlertTriangle size={15} />
              <span>{error}</span>
            </div>
            <button onClick={() => setError(null)} className="text-xs font-semibold hover:underline">
              Dismiss
            </button>
          </div>
        )}

        {/* Filter Controls Bar */}
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2.5 flex-1 max-w-2xl">
            {/* Search Input */}
            <div className="relative flex-1 min-w-[200px]">
              <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--color-text-faint)]" />
              <input
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(1);
                }}
                placeholder="Search policies by code, name, or class..."
                className="field-input pl-8 text-xs"
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
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
              <option value="DRAFT">Draft</option>
            </select>
          </div>

          <span className="text-xs text-[var(--color-text-faint)] font-mono">
            {filteredPolicies.length} {filteredPolicies.length === 1 ? "policy" : "policies"}
          </span>
        </div>

        {/* Policies Table Card */}
        <div className="card overflow-hidden">
          {loading ? (
            <TableSkeleton rows={5} cols={7} />
          ) : filteredPolicies.length === 0 ? (
            <EmptyState
              icon={FileText}
              title="No policies found"
              description={
                search || statusFilter !== "ALL" || jurisdictionFilter !== "ALL"
                  ? "Try adjusting your filters or search terms to find matching policies."
                  : "No data governance policies have been created yet. Add your first policy to enforce zero-trust data residency."
              }
              actionLabel={canWrite ? "Create Policy" : null}
              onAction={() => setShowCreateModal(true)}
              actionIcon={Plus}
            />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead className="bg-[var(--color-surface-2)] text-[var(--color-text-dim)] border-b border-[var(--color-border)] uppercase tracking-wider font-semibold text-[10px]">
                  <tr>
                    <th className="px-5 py-3 font-semibold">Policy Code</th>
                    <th className="px-5 py-3 font-semibold">Name</th>
                    <th className="px-5 py-3 font-semibold">Jurisdiction</th>
                    <th className="px-5 py-3 font-semibold">Data Class</th>
                    <th className="px-5 py-3 font-semibold">Allowed Regions</th>
                    <th className="px-5 py-3 font-semibold">Denied Regions</th>
                    <th className="px-5 py-3 font-semibold">Status</th>
                    <th className="px-5 py-3 font-semibold text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--color-border)]">
                  {paginatedPolicies.map((p) => (
                    <tr
                      key={p.id || p.policyCode}
                      className="hover:bg-[var(--color-surface-2)]/60 transition-colors"
                    >
                      <td className="px-5 py-3 font-mono text-xs font-semibold text-[var(--color-text)]">
                        {p.policyCode}
                      </td>
                      <td className="px-5 py-3 text-xs text-[var(--color-text)] font-medium max-w-xs truncate">
                        {p.name}
                      </td>
                      <td className="px-5 py-3">
                        <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-blue-500/10 text-blue-600 dark:text-blue-400 border border-blue-500/20">
                          {p.jurisdiction}
                        </span>
                      </td>
                      <td className="px-5 py-3">
                        <span className="text-[11px] font-semibold px-2 py-0.5 rounded bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20">
                          {p.dataClass}
                        </span>
                      </td>
                      <td className="px-5 py-3 font-mono text-[11px] text-[var(--color-text-dim)]">
                        {[...p.allowedRegions].join(", ") || "GLOBAL"}
                      </td>
                      <td className="px-5 py-3 font-mono text-[11px] text-[var(--color-text-dim)]">
                        {[...p.deniedRegions].join(", ") || "NONE"}
                      </td>
                      <td className="px-5 py-3">
                        <Badge variant={p.status === "ACTIVE" ? "good" : "neutral"} dot>
                          {p.status}
                        </Badge>
                      </td>
                      <td className="px-5 py-3 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          {canWrite && (
                            <button
                              onClick={() => toggleStatus(p)}
                              disabled={togglingId === p.id}
                              title={p.status === "ACTIVE" ? "Deactivate policy" : "Activate policy"}
                              className={`p-1 rounded-lg transition-colors hover:bg-[var(--color-surface-2)] disabled:opacity-50 ${
                                p.status === "ACTIVE"
                                  ? "text-[var(--color-good)]"
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
                              className="p-1 rounded-lg text-[var(--color-text-faint)] hover:text-[var(--color-bad)] hover:bg-[var(--color-bad-light)] transition-colors"
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
          )}
        </div>
      </div>

      {/* Create Policy Modal */}
      <Modal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        title="Create New Data Governance Policy"
        subtitle="Define jurisdictional constraints, data classes, and allowed/denied destination regions."
      >
        <form onSubmit={handleCreate} className="space-y-4">
          <div>
            <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
              Policy Code * <span className="text-[var(--color-text-faint)]">(e.g. EU-PII-001)</span>
            </label>
            <input
              value={form.policyCode}
              onChange={(e) => setField("policyCode")(e.target.value.toUpperCase())}
              placeholder="EU-PII-001"
              className="field-input text-xs font-mono"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
              Policy Name *
            </label>
            <input
              value={form.name}
              onChange={(e) => setField("name")(e.target.value)}
              placeholder="EU General Personal Data Residency Gate"
              className="field-input text-xs"
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
                Jurisdiction *
              </label>
              <select
                value={form.jurisdiction}
                onChange={(e) => setField("jurisdiction")(e.target.value)}
                className="field-input text-xs"
              >
                {JURISDICTIONS.map((j) => (
                  <option key={j} value={j}>
                    {j}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
                Data Class *
              </label>
              <select
                value={form.dataClass}
                onChange={(e) => setField("dataClass")(e.target.value)}
                className="field-input text-xs font-mono"
              >
                {DATA_CLASSES.map((dc) => (
                  <option key={dc} value={dc}>
                    {dc}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
                Allowed Regions <span className="text-[var(--color-text-faint)]">(CSV)</span>
              </label>
              <input
                value={form.allowedRegions}
                onChange={(e) => setField("allowedRegions")(e.target.value)}
                placeholder="EU, UK"
                className="field-input text-xs font-mono"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
                Denied Regions <span className="text-[var(--color-text-faint)]">(CSV)</span>
              </label>
              <input
                value={form.deniedRegions}
                onChange={(e) => setField("deniedRegions")(e.target.value)}
                placeholder="US, CN"
                className="field-input text-xs font-mono"
              />
            </div>
          </div>

          <div className="flex items-center justify-between pt-3 border-t border-[var(--color-border)]">
            <button
              type="button"
              onClick={resetForm}
              className="text-xs text-[var(--color-text-faint)] hover:text-[var(--color-text)] flex items-center gap-1"
            >
              <RotateCcw size={12} /> Reset Draft
            </button>
            <div className="flex items-center gap-2">
              <Button variant="ghost" size="md" onClick={() => setShowCreateModal(false)}>
                Cancel
              </Button>
              <Button variant="primary" size="md" type="submit" loading={submitting}>
                Save Policy
              </Button>
            </div>
          </div>
        </form>
      </Modal>

      {/* Import YAML Modal */}
      <Modal
        isOpen={showImportModal}
        onClose={() => setShowImportModal(false)}
        title="Import Policy YAML"
        subtitle="Paste standard PolicyMesh declarative YAML specifications to register policies in bulk."
      >
        <form onSubmit={handleImportYaml} className="space-y-4">
          <div>
            <textarea
              rows={8}
              value={yamlContent}
              onChange={(e) => setYamlContent(e.target.value)}
              placeholder={`policyCode: EU-PII-002\nname: Strict EU Cross-Border Residency\njurisdiction: EU\ndataClass: PII\nallowedRegions:\n  - EU\ndeniedRegions:\n  - US\nstatus: ACTIVE`}
              className="field-input text-xs font-mono resize-none leading-relaxed"
              required
            />
          </div>

          <div className="flex items-center justify-end gap-2 pt-2">
            <Button variant="ghost" size="md" onClick={() => setShowImportModal(false)}>
              Cancel
            </Button>
            <Button variant="primary" size="md" type="submit" loading={submitting} icon={Upload}>
              Import Policy
            </Button>
          </div>
        </form>
      </Modal>
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
