import { useEffect, useState, useCallback } from "react";
import {
  Plus,
  Trash2,
  ShieldCheck,
  ShieldAlert,
  GitBranch,
  Loader2,
  RefreshCw,
  Search,
  CheckCircle2,
  AlertTriangle,
  RotateCcw,
} from "lucide-react";
import Topbar, { TopbarActions } from "../components/layout/Topbar";
import Pagination from "../components/ui/Pagination";
import Button from "../components/ui/Button";
import Badge from "../components/ui/Badge";
import Modal from "../components/ui/Modal";
import EmptyState from "../components/ui/EmptyState";
import { TableSkeleton } from "../components/ui/LoadingSkeleton";
import SearchableCombobox from "../components/ui/SearchableCombobox";
import { servicesApi, edgesApi, graphApi } from "../api";
import { useAuth } from "../context/AuthContext";
import { useFormDraft } from "../hooks/useFormDraft";

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

  // Filter state
  const [search, setSearch] = useState("");
  const [dataClassFilter, setDataClassFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Form modal
  const [showModal, setShowModal] = useState(false);
  const { values: form, setValues: setForm, clearDraft, resetForm } = useFormDraft(
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
      setServices(Array.isArray(svcs) ? svcs : []);
      setEdges(Array.isArray(edg) ? edg : []);
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
      setServices(Array.isArray(svcs) ? svcs : []);
      setEdges(Array.isArray(edg) ? edg : []);

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

  async function handleAddEdge(e) {
    e.preventDefault();
    if (!form.sourceServiceId || !form.destinationServiceId) {
      setFormError("Both source and destination services are required.");
      return;
    }
    if (form.sourceServiceId === form.destinationServiceId) {
      setFormError("Source and destination services cannot be identical.");
      return;
    }
    if (form.dataClasses.length === 0) {
      setFormError("Select at least one data class.");
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
      clearDraft();
      setShowModal(false);
      await loadData();
      await runValidation(false);
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDeleteEdge(id) {
    if (!window.confirm("Remove this data flow edge?")) return;
    try {
      await edgesApi.remove(id);
      await loadData();
      await runValidation(false);
    } catch (err) {
      setError(err.message);
    }
  }

  function getViolation(srcId, dstId) {
    return violations.find(
      (v) =>
        (v.sourceServiceId === srcId && v.destinationServiceId === dstId) ||
        (v.sourceService === svcMap[srcId]?.name && v.destinationService === svcMap[dstId]?.name)
    );
  }

  // Filtering
  const filteredEdges = edges.filter((e) => {
    const src = svcMap[e.sourceServiceId];
    const dst = svcMap[e.destinationServiceId];
    const srcName = src?.name?.toLowerCase() || "";
    const dstName = dst?.name?.toLowerCase() || "";
    const q = search.trim().toLowerCase();
    const matchSearch =
      !q ||
      srcName.includes(q) ||
      dstName.includes(q) ||
      e.dataClasses.some((dc) => dc.toLowerCase().includes(q));

    const matchDataClass =
      dataClassFilter === "ALL" || e.dataClasses.includes(dataClassFilter);

    const viol = getViolation(e.sourceServiceId, e.destinationServiceId);
    const matchStatus =
      statusFilter === "ALL" ||
      (statusFilter === "VIOLATION" && viol) ||
      (statusFilter === "COMPLIANT" && !viol);

    return matchSearch && matchDataClass && matchStatus;
  });

  const paginatedEdges = filteredEdges.slice((page - 1) * pageSize, page * pageSize);
  const totalViolationsCount = validationResult?.violationCount != null ? validationResult.violationCount : violations.length;

  const topActions = (
    <div className="flex items-center gap-2">
      <Button
        variant="secondary"
        size="md"
        icon={validating ? Loader2 : RefreshCw}
        onClick={() => runValidation(true)}
        disabled={validating}
      >
        {validating ? "Evaluating..." : "Re-evaluate Graph"}
      </Button>

      {canWrite && (
        <Button
          variant="primary"
          size="md"
          icon={Plus}
          onClick={() => setShowModal(true)}
        >
          Add Data Flow
        </Button>
      )}
    </div>
  );

  return (
    <div>
      <Topbar
        title="Data Flow Graph & Topology"
        subtitle="Graph topology mapping cross-service egress pipelines and automated compliance verification."
        actions={topActions}
      />

      <div className="px-6 lg:px-8 py-6 space-y-4 pb-12">
        {/* Toast feedback */}
        {toast && (
          <div
            className={`text-xs p-3 rounded-xl border flex items-center justify-between animate-in fade-in ${
              toast.type === "success"
                ? "bg-[var(--color-good-light)] border-[var(--color-good)]/30 text-[var(--color-good-text)]"
                : "bg-[var(--color-warn-light)] border-[var(--color-warn)]/30 text-[var(--color-warn-text)]"
            }`}
          >
            <div className="flex items-center gap-2">
              <CheckCircle2 size={15} />
              <span>{toast.text}</span>
            </div>
            <button onClick={() => setToast(null)} className="text-xs font-semibold hover:underline">
              Dismiss
            </button>
          </div>
        )}

        {/* Error notification */}
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

            {/* Data Class Filter */}
            <select
              value={dataClassFilter}
              onChange={(e) => {
                setDataClassFilter(e.target.value);
                setPage(1);
              }}
              className="field-input py-1.5 text-xs w-36"
            >
              <option value="ALL">All Data Classes</option>
              {DATA_CLASSES.map((dc) => (
                <option key={dc} value={dc}>
                  {dc}
                </option>
              ))}
            </select>

            {/* Status Filter */}
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(1);
              }}
              className="field-input py-1.5 text-xs w-32"
            >
              <option value="ALL">All Statuses</option>
              <option value="COMPLIANT">Compliant</option>
              <option value="VIOLATION">Violation</option>
            </select>
          </div>

          {/* Violations Badge */}
          <div className="flex items-center gap-2">
            <span className="text-xs text-[var(--color-text-faint)] font-mono">
              Total Violations Found:
            </span>
            <span
              className={`text-xs font-bold px-2.5 py-0.5 rounded-lg border font-mono ${
                totalViolationsCount > 0
                  ? "bg-[var(--color-bad-light)] text-[var(--color-bad-text)] border-[var(--color-bad)]/30"
                  : "bg-[var(--color-good-light)] text-[var(--color-good-text)] border-[var(--color-good)]/30"
              }`}
            >
              {totalViolationsCount}
            </span>
          </div>
        </div>

        {/* Data Flows Table Card */}
        <div className="card overflow-hidden">
          {loading ? (
            <TableSkeleton rows={5} cols={6} />
          ) : filteredEdges.length === 0 ? (
            <EmptyState
              icon={GitBranch}
              title="No data flows configured"
              description={
                search || dataClassFilter !== "ALL" || statusFilter !== "ALL"
                  ? "Try adjusting your search criteria or status filters."
                  : "Connect your service nodes to establish egress boundaries and real-time compliance gates."
              }
              actionLabel={canWrite ? "Add Data Flow" : null}
              onAction={() => setShowModal(true)}
              actionIcon={Plus}
            />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead className="bg-[var(--color-surface-2)] text-[var(--color-text-dim)] border-b border-[var(--color-border)] uppercase tracking-wider font-semibold text-[10px]">
                  <tr>
                    <th className="px-5 py-3 font-semibold">Source Service</th>
                    <th className="px-5 py-3 font-semibold">Destination Service</th>
                    <th className="px-5 py-3 font-semibold">Data Classes</th>
                    <th className="px-5 py-3 font-semibold">Compliance Status</th>
                    <th className="px-5 py-3 font-semibold">Violation Reason</th>
                    <th className="px-5 py-3 font-semibold text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--color-border)]">
                  {paginatedEdges.map((edge) => {
                    const src = svcMap[edge.sourceServiceId];
                    const dst = svcMap[edge.destinationServiceId];
                    const viol = getViolation(edge.sourceServiceId, edge.destinationServiceId);

                    return (
                      <tr
                        key={edge.id}
                        className={`transition-colors ${
                          viol
                            ? "bg-rose-500/5 hover:bg-rose-500/10"
                            : "hover:bg-[var(--color-surface-2)]/60"
                        }`}
                      >
                        <td className="px-5 py-3">
                          <div className="flex items-center gap-2">
                            <span className="font-mono font-semibold text-[var(--color-text)]">
                              {src?.name || `Service #${edge.sourceServiceId}`}
                            </span>
                            {src?.region && (
                              <span className="text-[10px] font-mono px-1.5 py-0.2 rounded bg-blue-500/10 text-blue-600 dark:text-blue-400 border border-blue-500/20">
                                {src.region}
                              </span>
                            )}
                          </div>
                        </td>

                        <td className="px-5 py-3">
                          <div className="flex items-center gap-2">
                            <span className="font-mono font-semibold text-[var(--color-text)]">
                              {dst?.name || `Service #${edge.destinationServiceId}`}
                            </span>
                            {dst?.region && (
                              <span className="text-[10px] font-mono px-1.5 py-0.2 rounded bg-blue-500/10 text-blue-600 dark:text-blue-400 border border-blue-500/20">
                                {dst.region}
                              </span>
                            )}
                          </div>
                        </td>

                        <td className="px-5 py-3">
                          <div className="flex flex-wrap gap-1">
                            {edge.dataClasses.map((dc) => (
                              <span
                                key={dc}
                                className="text-[10px] font-semibold px-1.5 py-0.2 rounded bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20"
                              >
                                {dc}
                              </span>
                            ))}
                          </div>
                        </td>

                        <td className="px-5 py-3">
                          {viol ? (
                            <Badge variant="bad" dot icon={ShieldAlert}>
                              VIOLATION
                            </Badge>
                          ) : (
                            <Badge variant="good" dot icon={ShieldCheck}>
                              COMPLIANT
                            </Badge>
                          )}
                        </td>

                        <td className="px-5 py-3 text-xs text-[var(--color-bad)] max-w-xs truncate font-mono text-[11px]">
                          {viol?.reason || (viol?.policyCode ? `Blocked by ${viol.policyCode}` : "—")}
                        </td>

                        <td className="px-5 py-3 text-right">
                          {canWrite && (
                            <button
                              onClick={() => handleDeleteEdge(edge.id)}
                              className="p-1 rounded-lg text-[var(--color-text-faint)] hover:text-[var(--color-bad)] hover:bg-[var(--color-bad-light)] transition-colors"
                              title="Delete Flow Edge"
                            >
                              <Trash2 size={14} />
                            </button>
                          )}
                        </td>
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
          )}
        </div>
      </div>

      {/* Add Data Flow Modal */}
      <Modal
        isOpen={showModal}
        onClose={() => setShowModal(false)}
        title="Add Data Flow Edge"
        subtitle="Define an egress boundary between two services and associate transferred data classifications."
      >
        <form onSubmit={handleAddEdge} className="space-y-4">
          {formError && (
            <div className="text-xs text-[var(--color-bad)] bg-[var(--color-bad-light)] border border-[var(--color-bad)]/30 rounded-lg p-2.5 flex items-center gap-2">
              <AlertTriangle size={14} className="shrink-0" />
              <span>{formError}</span>
            </div>
          )}

          <div>
            <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
              Source Service *
            </label>
            <SearchableCombobox
              value={form.sourceServiceId}
              onChange={(val) => setForm((f) => ({ ...f, sourceServiceId: val }))}
              options={services.map((s) => ({
                id: String(s.id),
                name: s.name,
                region: s.region,
                environment: s.environment,
              }))}
              placeholder="Select source service..."
              getOptionLabel={(o) => o.name}
              getOptionValue={(o) => o.id}
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
              Destination Service *
            </label>
            <SearchableCombobox
              value={form.destinationServiceId}
              onChange={(val) => setForm((f) => ({ ...f, destinationServiceId: val }))}
              options={services.map((s) => ({
                id: String(s.id),
                name: s.name,
                region: s.region,
                environment: s.environment,
              }))}
              placeholder="Select destination service..."
              getOptionLabel={(o) => o.name}
              getOptionValue={(o) => o.id}
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1.5">
              Data Classes Transferred *
            </label>
            <div className="flex flex-wrap gap-2">
              {DATA_CLASSES.map((dc) => {
                const isSelected = form.dataClasses.includes(dc);
                return (
                  <button
                    key={dc}
                    type="button"
                    onClick={() => toggleDataClass(dc)}
                    className={`px-2.5 py-1 rounded-lg text-xs font-medium transition-all ${
                      isSelected
                        ? "bg-[var(--color-brand-light)] text-[var(--color-brand-text)] border border-[var(--color-brand)]/40 font-semibold"
                        : "bg-[var(--color-surface-2)] text-[var(--color-text-dim)] border border-[var(--color-border)] hover:text-[var(--color-text)]"
                    }`}
                  >
                    {dc}
                  </button>
                );
              })}
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
              <Button variant="ghost" size="md" onClick={() => setShowModal(false)}>
                Cancel
              </Button>
              <Button variant="primary" size="md" type="submit" loading={submitting}>
                Save Edge
              </Button>
            </div>
          </div>
        </form>
      </Modal>
    </div>
  );
}
