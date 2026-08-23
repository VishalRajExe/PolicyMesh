import { useEffect, useState, useCallback } from "react";
import {
  Plus,
  Trash2,
  ShieldCheck,
  ShieldAlert,
  GitBranch,
  Loader2,
  RefreshCw,
  X,
  Search,
  CheckCircle2,
  AlertTriangle,
  Info,
} from "lucide-react";
import Topbar from "../components/layout/Topbar";
import Pagination from "../components/ui/Pagination";
import SearchableCombobox from "../components/ui/SearchableCombobox";
import { servicesApi, edgesApi, graphApi } from "../api";
import { useAuth } from "../context/AuthContext";
import { useFormDraft } from "../hooks/useFormDraft";
import { useQueryState } from "../hooks/useQueryState";

const DATA_CLASSES = ["PII", "PCI", "PHI", "NON_SENSITIVE", "UNKNOWN"];

const DEFAULT_EDGE_FORM = {
  sourceServiceId: "",
  destinationServiceId: "",
  dataClasses: ["PII"],
};

export default function DataFlows() {
  const { user } = useAuth();
  const [services, setServices] = useState([]);
  const [edges, setEdges] = useState([]);
  const [violations, setViolations] = useState([]);
  const [validationResult, setValidationResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [validating, setValidating] = useState(false);
  const [error, setError] = useState(null);
  const [toast, setToast] = useState(null);

  // URL query state
  const [search, setSearch] = useQueryState("search", "");
  const [dataClassFilter, setDataClassFilter] = useQueryState("dataClass", "ALL");
  const [statusFilter, setStatusFilter] = useQueryState("status", "ALL");
  const [page, setPage] = useQueryState("page", 1);
  const [pageSize, setPageSize] = useQueryState("size", 10);

  // Form draft state
  const [showForm, setShowForm] = useState(false);
  const { values: form, setValues: setForm, clearDraft } = useFormDraft(
    "dataflow-edge",
    DEFAULT_EDGE_FORM
  );
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);

  const canWrite = user?.role === "ADMIN" || user?.role === "ENGINEER";
  const svcMap = Object.fromEntries(services.map((s) => [s.id, s]));

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [svcs, edg] = await Promise.all([servicesApi.list(), edgesApi.list()]);
      setServices(svcs || []);
      setEdges(edg || []);
    } catch (err) {
      setError(err.message || "Failed to load data flows");
    } finally {
      setLoading(false);
    }
  }, []);

  const runValidation = useCallback(async (isManual = false) => {
    setValidating(true);
    setError(null);
    try {
      const [result, svcs, edg] = await Promise.all([
        graphApi.reEvaluate(),
        servicesApi.list(),
        edgesApi.list(),
      ]);
      setValidationResult(result);
      setViolations(result.violations || []);
      setServices(svcs || []);
      setEdges(edg || []);

      if (isManual) {
        const vCount = result.violationCount != null ? result.violationCount : (result.violations || []).length;
        const total = result.totalFlows != null ? result.totalFlows : (edg || []).length;
        const compliant = result.compliantFlows != null ? result.compliantFlows : Math.max(0, total - vCount);

        setToast({
          type: vCount === 0 ? "success" : "warning",
          text: `Graph evaluated: ${total} flows, ${compliant} compliant, ${vCount} violation${vCount !== 1 ? "s" : ""}.`,
        });
        setTimeout(() => setToast(null), 3500);
      }
    } catch (err) {
      setError(err.message || "Unable to re-evaluate graph.");
    } finally {
      setValidating(false);
    }
  }, []);

  useEffect(() => {
    loadData().then(() => runValidation(false));
  }, [loadData, runValidation]);

  function toggleDataClass(dc) {
    setForm((f) => ({
      ...f,
      dataClasses: f.dataClasses.includes(dc)
        ? f.dataClasses.filter((x) => x !== dc)
        : [...f.dataClasses, dc],
    }));
  }

  async function handleCreate(e) {
    e.preventDefault();
    if (!form.sourceServiceId || !form.destinationServiceId) {
      setFormError("Both source and destination services are required.");
      return;
    }
    if (form.dataClasses.length === 0) {
      setFormError("Select at least one data class.");
      return;
    }
    if (String(form.sourceServiceId) === String(form.destinationServiceId)) {
      setFormError("Source and destination must be different services.");
      return;
    }
    setSubmitting(true);
    setFormError(null);
    try {
      await edgesApi.create({
        sourceServiceId: Number(form.sourceServiceId),
        destinationServiceId: Number(form.destinationServiceId),
        dataClasses: form.dataClasses,
      });
      setShowForm(false);
      clearDraft();
      await runValidation(false);
    } catch (err) {
      setFormError(err.message || "Failed to create data flow edge");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(id) {
    try {
      await edgesApi.remove(id);
      await runValidation(false);
    } catch (err) {
      setError(err.message);
    }
  }

  function getEdgeViolation(edge) {
    return violations.find((v) => {
      if (v.edgeId && edge.id && Number(v.edgeId) === Number(edge.id)) {
        return true;
      }
      const edgeSrc = edge.sourceServiceName || svcMap[edge.sourceServiceId]?.name;
      const edgeDst = edge.destinationServiceName || svcMap[edge.destinationServiceId]?.name;
      return v.sourceService === edgeSrc && v.destinationService === edgeDst;
    });
  }

  // Filtered edges
  const filteredEdges = edges.filter((edge) => {
    const src = (edge.sourceServiceName || svcMap[edge.sourceServiceId]?.name || "").toLowerCase();
    const dst = (edge.destinationServiceName || svcMap[edge.destinationServiceId]?.name || "").toLowerCase();
    const q = search.trim().toLowerCase();
    const matchSearch = !q || src.includes(q) || dst.includes(q);

    const matchDataClass =
      dataClassFilter === "ALL" || (edge.dataClasses && edge.dataClasses.includes(dataClassFilter));

    const violation = getEdgeViolation(edge);
    const matchStatus =
      statusFilter === "ALL" ||
      (statusFilter === "VIOLATION" && Boolean(violation)) ||
      (statusFilter === "COMPLIANT" && !violation);

    return matchSearch && matchDataClass && matchStatus;
  });

  const paginatedEdges = filteredEdges.slice((page - 1) * pageSize, page * pageSize);
  const violationCount = violations.length;

  return (
    <div>
      <Topbar
        title="Data Flows"
        subtitle="Manage and analyze cross-service data pipelines and residency compliance boundaries."
      />

      <div className="px-6 lg:px-8 mt-4 space-y-3 pb-12">
        {/* Floating Toast Notification (bottom-right) */}
        {toast && (
          <div
            className={`fixed bottom-6 right-6 z-50 flex items-center gap-2.5 px-4 py-2.5 rounded-xl shadow-xl border text-xs backdrop-blur-md animate-in slide-in-from-bottom-3 duration-200 ${
              toast.type === "success"
                ? "bg-[var(--color-surface)] border-[var(--color-good)]/40 text-[var(--color-good)]"
                : "bg-[var(--color-surface)] border-amber-500/40 text-amber-300"
            }`}
          >
            {toast.type === "success" ? (
              <CheckCircle2 size={15} className="shrink-0" />
            ) : (
              <AlertTriangle size={15} className="shrink-0" />
            )}
            <span className="font-medium text-white">{toast.text}</span>
            <button
              onClick={() => setToast(null)}
              className="text-[var(--color-text-faint)] hover:text-white p-0.5 ml-1"
            >
              <X size={13} />
            </button>
          </div>
        )}

        {/* Controls Bar */}
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3 flex-1 max-w-2xl">
            <div className="relative flex-1 min-w-[200px]">
              <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--color-text-faint)]" />
              <input
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(1);
                }}
                placeholder="Search flows by source or destination service..."
                className="field-input pl-9 text-xs"
              />
            </div>

            <select
              value={dataClassFilter}
              onChange={(e) => {
                setDataClassFilter(e.target.value);
                setPage(1);
              }}
              className="field-input py-1.5 text-xs w-36"
            >
              <option value="ALL">All Sensitivity</option>
              {DATA_CLASSES.map((dc) => (
                <option key={dc} value={dc}>
                  {dc}
                </option>
              ))}
            </select>

            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(1);
              }}
              className="field-input py-1.5 text-xs w-36"
            >
              <option value="ALL">All Statuses</option>
              <option value="COMPLIANT">Compliant Only</option>
              <option value="VIOLATION">Violations Only</option>
            </select>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={() => runValidation(true)}
              disabled={validating}
              className="btn-ghost flex items-center gap-1.5 text-xs text-[var(--color-text-dim)] hover:text-white border-[var(--color-border)] hover:bg-[var(--color-surface-2)]"
              title="Perform a fresh compliance evaluation of all current services and data flow edges"
            >
              <RefreshCw size={13} className={validating ? "animate-spin text-[var(--color-brand)]" : ""} />
              <span>{validating ? "Re-evaluating…" : "Re-evaluate Graph"}</span>
            </button>

            {canWrite && (
              <button
                onClick={() => setShowForm(true)}
                className="btn-primary flex items-center gap-1.5 text-xs"
              >
                <Plus size={15} />
                Add Flow Edge
              </button>
            )}
          </div>
        </div>

        {error && (
          <div className="rounded-lg bg-[var(--color-bad)]/10 border border-[var(--color-bad)]/30 px-3 py-2 text-xs text-[var(--color-bad)] flex items-center justify-between">
            <span>{error}</span>
            <button onClick={() => runValidation(false)} className="underline ml-4">
              Retry
            </button>
          </div>
        )}

        {/* Table */}
        <div className="card overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-[var(--color-text-faint)] border-b border-[var(--color-border)] bg-[var(--color-surface)]/50">
                <th className="px-5 py-3 font-medium text-xs">Source Service</th>
                <th className="px-5 py-3 font-medium text-xs">Destination Service</th>
                <th className="px-5 py-3 font-medium text-xs">Data Classification</th>
                <th className="px-5 py-3 font-medium text-xs">Compliance Status</th>
                {canWrite && <th className="px-5 py-3 text-right text-xs">Actions</th>}
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr>
                  <td colSpan={5} className="px-5 py-10 text-center text-[var(--color-text-faint)] text-xs">
                    <Loader2 size={16} className="animate-spin inline mr-2 text-[var(--color-brand)]" /> Loading data flow edges…
                  </td>
                </tr>
              )}
              {!loading && filteredEdges.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-5 py-12 text-center">
                    <GitBranch size={28} className="mx-auto mb-2 text-[var(--color-text-faint)] opacity-60" />
                    <p className="text-[var(--color-text-faint)] text-xs">No data flows match the current filters.</p>
                  </td>
                </tr>
              )}
              {!loading &&
                paginatedEdges.map((edge) => {
                  const src = svcMap[edge.sourceServiceId];
                  const dst = svcMap[edge.destinationServiceId];
                  const srcName = edge.sourceServiceName || src?.name || `Service #${edge.sourceServiceId}`;
                  const dstName = edge.destinationServiceName || dst?.name || `Service #${edge.destinationServiceId}`;
                  const violation = getEdgeViolation(edge);

                  return (
                    <tr
                      key={edge.id}
                      className="border-b border-[var(--color-border)] last:border-0 hover:bg-[var(--color-surface-2)] transition-colors"
                    >
                      <td className="px-5 py-3">
                        <div className="flex items-center gap-2">
                          <span className="font-semibold text-white text-xs font-mono">{srcName}</span>
                          {src && (
                            <span className="text-[10px] px-1.5 py-0.5 rounded bg-blue-500/10 text-blue-400 font-mono">
                              {src.region}
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="px-5 py-3">
                        <div className="flex items-center gap-2">
                          <span className="font-semibold text-white text-xs font-mono">{dstName}</span>
                          {dst && (
                            <span className="text-[10px] px-1.5 py-0.5 rounded bg-blue-500/10 text-blue-400 font-mono">
                              {dst.region}
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="px-5 py-3">
                        <div className="flex flex-wrap gap-1">
                          {(edge.dataClasses || []).map((dc) => (
                            <span
                              key={dc}
                              className="text-[10px] font-semibold px-2 py-0.5 rounded bg-amber-500/15 text-amber-400 border border-amber-500/30 font-mono"
                            >
                              {dc}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td className="px-5 py-3">
                        {violation ? (
                          <div className="flex items-center gap-1.5 text-[var(--color-bad)] text-xs font-medium">
                            <ShieldAlert size={14} className="shrink-0" />
                            <span>
                              Violation: {violation.policyCode ? `[${violation.policyCode}] ` : ""}{violation.reason}
                            </span>
                          </div>
                        ) : (
                          <div className="flex items-center gap-1.5 text-[var(--color-good)] text-xs font-medium">
                            <ShieldCheck size={14} className="shrink-0" />
                            <span>Compliant</span>
                          </div>
                        )}
                      </td>
                      {canWrite && (
                        <td className="px-5 py-3 text-right">
                          <button
                            onClick={() => handleDelete(edge.id)}
                            className="text-[var(--color-text-faint)] hover:text-[var(--color-bad)] p-1 transition-colors"
                            title="Delete Flow Edge"
                          >
                            <Trash2 size={14} />
                          </button>
                        </td>
                      )}
                    </tr>
                  );
                })}
            </tbody>
          </table>

          <Pagination
            currentPage={page}
            totalItems={filteredEdges.length}
            pageSize={pageSize}
            onPageChange={setPage}
            onPageSizeChange={(sz) => {
              setPageSize(sz);
              setPage(1);
            }}
          />
        </div>
      </div>

      {/* Form Modal */}
      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm px-4">
          <form
            onSubmit={handleCreate}
            className="card w-full max-w-lg p-6 space-y-4 animate-in fade-in zoom-in-95"
          >
            <div className="flex items-center justify-between mb-1">
              <h2 className="font-semibold text-white text-base">Add Data Flow Edge</h2>
              <button
                type="button"
                onClick={() => {
                  setShowForm(false);
                  setFormError(null);
                }}
                className="text-[var(--color-text-faint)] hover:text-white"
              >
                <X size={18} />
              </button>
            </div>

            <div>
              <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">Source Service *</label>
              <SearchableCombobox
                value={form.sourceServiceId}
                onChange={(val) => setForm((prev) => ({ ...prev, sourceServiceId: val }))}
                options={services}
                getOptionLabel={(s) => s.name || s.id}
                getOptionValue={(s) => String(s.id)}
                placeholder="Select source service..."
                searchPlaceholder="Search services..."
              />
            </div>

            <div>
              <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">Destination Service *</label>
              <SearchableCombobox
                value={form.destinationServiceId}
                onChange={(val) => setForm((prev) => ({ ...prev, destinationServiceId: val }))}
                options={services}
                getOptionLabel={(s) => s.name || s.id}
                getOptionValue={(s) => String(s.id)}
                placeholder="Select destination service..."
                searchPlaceholder="Search services..."
              />
            </div>

            <div>
              <label className="block text-xs text-[var(--color-text-dim)] mb-2">Data Sensitivity Classes *</label>
              <div className="flex flex-wrap gap-2">
                {DATA_CLASSES.map((dc) => (
                  <button
                    key={dc}
                    type="button"
                    onClick={() => toggleDataClass(dc)}
                    className={`text-xs font-medium px-3 py-1.5 rounded-lg border transition-colors ${
                      form.dataClasses.includes(dc)
                        ? "bg-[var(--color-brand)] border-[var(--color-brand)] text-white"
                        : "border-[var(--color-border)] text-[var(--color-text-dim)] hover:border-[var(--color-brand)] hover:text-white"
                    }`}
                  >
                    {dc}
                  </button>
                ))}
              </div>
            </div>

            {formError && (
              <p className="text-xs text-[var(--color-bad)] bg-[var(--color-bad)]/10 rounded-lg px-3 py-2">
                {formError}
              </p>
            )}

            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => {
                  setShowForm(false);
                  setFormError(null);
                }}
                className="btn-ghost text-xs"
              >
                Cancel
              </button>
              <button type="submit" disabled={submitting} className="btn-primary text-xs">
                {submitting ? <Loader2 size={14} className="animate-spin" /> : <Plus size={14} />}
                Add Flow Edge
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
