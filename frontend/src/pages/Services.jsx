import { useEffect, useState } from "react";
import {
  Plus,
  Trash2,
  Pencil,
  Loader2,
  Boxes,
  Search,
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
import { servicesApi } from "../api";
import { useAuth } from "../context/AuthContext";
import { useFormDraft } from "../hooks/useFormDraft";

const REGIONS = ["EU", "US", "IN", "CN", "GLOBAL", "AP", "ME"];
const ENVIRONMENTS = ["production", "staging", "development", "qa"];

const EMPTY_FORM = {
  name: "",
  region: "EU",
  environment: "production",
  meshZone: "",
  description: "",
};

export default function Services() {
  const { user } = useAuth();
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filters
  const [search, setSearch] = useState("");
  const [regionFilter, setRegionFilter] = useState("ALL");
  const [envFilter, setEnvFilter] = useState("ALL");
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Form & Modals
  const [showModal, setShowModal] = useState(false);
  const [editId, setEditId] = useState(null);
  const [deleteConfirm, setDeleteConfirm] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);

  const { values: form, setValues: setForm, clearDraft, resetForm } = useFormDraft(
    "service-editor",
    EMPTY_FORM
  );

  const canWrite = user?.role === "ADMIN" || user?.role === "ENGINEER";

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const data = await servicesApi.list();
      setServices(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || "Failed to load services");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  function openCreate() {
    setEditId(null);
    setFormError(null);
    setShowModal(true);
  }

  function openEdit(svc) {
    setEditId(svc.id);
    setForm({
      name: svc.name,
      region: svc.region,
      environment: svc.environment,
      meshZone: svc.meshZone || "",
      description: svc.description || "",
    });
    setFormError(null);
    setShowModal(true);
  }

  function closeModal() {
    setShowModal(false);
    setEditId(null);
    setFormError(null);
    clearDraft();
  }

  async function handleSubmit(e) {
    e.preventDefault();
    const trimmed = {
      name: form.name.trim(),
      region: form.region.trim(),
      environment: form.environment.trim(),
      meshZone: form.meshZone.trim() || undefined,
      description: form.description.trim() || undefined,
    };
    if (!trimmed.name || !trimmed.region || !trimmed.environment) {
      setFormError("Name, region, and environment are required.");
      return;
    }
    setSubmitting(true);
    setFormError(null);
    try {
      if (editId) {
        await servicesApi.update(editId, trimmed);
      } else {
        await servicesApi.create(trimmed);
      }
      closeModal();
      await load();
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function confirmDelete() {
    if (!deleteConfirm) return;
    try {
      await servicesApi.remove(deleteConfirm.id);
      setDeleteConfirm(null);
      await load();
    } catch (err) {
      setError(err.message);
    }
  }

  const setField = (k) => (v) => setForm((f) => ({ ...f, [k]: v }));

  // Filtering
  const filteredServices = services.filter((s) => {
    const q = search.trim().toLowerCase();
    const matchSearch =
      !q ||
      s.name.toLowerCase().includes(q) ||
      (s.description || "").toLowerCase().includes(q) ||
      (s.meshZone || "").toLowerCase().includes(q);
    const matchRegion = regionFilter === "ALL" || s.region === regionFilter;
    const matchEnv = envFilter === "ALL" || s.environment === envFilter;
    return matchSearch && matchRegion && matchEnv;
  });

  const paginatedServices = filteredServices.slice((page - 1) * pageSize, page * pageSize);

  const topActions = canWrite && (
    <Button variant="primary" size="md" icon={Plus} onClick={openCreate}>
      Add Service
    </Button>
  );

  return (
    <div>
      <Topbar
        title="Service Topology Registry"
        subtitle="Catalog and monitor active service nodes, deployment regions, and mesh zones."
        actions={topActions}
      />

      <div className="px-6 lg:px-8 py-6 space-y-4 pb-12">
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

            {/* Region Filter */}
            <select
              value={regionFilter}
              onChange={(e) => {
                setRegionFilter(e.target.value);
                setPage(1);
              }}
              className="field-input py-1.5 text-xs w-32"
            >
              <option value="ALL">All Regions</option>
              {REGIONS.map((r) => (
                <option key={r} value={r}>
                  {r}
                </option>
              ))}
            </select>

            {/* Environment Filter */}
            <select
              value={envFilter}
              onChange={(e) => {
                setEnvFilter(e.target.value);
                setPage(1);
              }}
              className="field-input py-1.5 text-xs w-36"
            >
              <option value="ALL">All Environments</option>
              {ENVIRONMENTS.map((env) => (
                <option key={env} value={env}>
                  {env}
                </option>
              ))}
            </select>
          </div>

          <span className="text-xs text-[var(--color-text-faint)] font-mono">
            {filteredServices.length} {filteredServices.length === 1 ? "service" : "services"}
          </span>
        </div>

        {/* Table Card */}
        <div className="card overflow-hidden">
          {loading ? (
            <TableSkeleton rows={5} cols={6} />
          ) : filteredServices.length === 0 ? (
            <EmptyState
              icon={Boxes}
              title="No services registered"
              description={
                search || regionFilter !== "ALL" || envFilter !== "ALL"
                  ? "Try adjusting your filters or search terms to find matching services."
                  : "No services have been registered yet. Add your first service node to build your topology graph."
              }
              actionLabel={canWrite ? "Add Service" : null}
              onAction={openCreate}
              actionIcon={Plus}
            />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead className="bg-[var(--color-surface-2)] text-[var(--color-text-dim)] border-b border-[var(--color-border)] uppercase tracking-wider font-semibold text-[10px]">
                  <tr>
                    <th className="px-5 py-3 font-semibold">Service Name</th>
                    <th className="px-5 py-3 font-semibold">Region</th>
                    <th className="px-5 py-3 font-semibold">Environment</th>
                    <th className="px-5 py-3 font-semibold">Mesh Zone</th>
                    <th className="px-5 py-3 font-semibold">Description</th>
                    <th className="px-5 py-3 font-semibold text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--color-border)]">
                  {paginatedServices.map((svc) => (
                    <tr
                      key={svc.id}
                      className="hover:bg-[var(--color-surface-2)]/60 transition-colors"
                    >
                      <td className="px-5 py-3 font-mono text-xs font-semibold text-[var(--color-text)]">
                        {svc.name}
                      </td>
                      <td className="px-5 py-3">
                        <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-blue-500/10 text-blue-600 dark:text-blue-400 border border-blue-500/20 font-semibold">
                          {svc.region}
                        </span>
                      </td>
                      <td className="px-5 py-3">
                        <Badge
                          variant={svc.environment === "production" ? "brand" : "neutral"}
                          size="sm"
                        >
                          {svc.environment}
                        </Badge>
                      </td>
                      <td className="px-5 py-3 font-mono text-[11px] text-[var(--color-text-dim)]">
                        {svc.meshZone || "—"}
                      </td>
                      <td className="px-5 py-3 text-xs text-[var(--color-text-dim)] max-w-sm truncate">
                        {svc.description || "—"}
                      </td>
                      <td className="px-5 py-3 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          {canWrite && (
                            <button
                              onClick={() => openEdit(svc)}
                              className="p-1 rounded-lg text-[var(--color-text-faint)] hover:text-[var(--color-text)] hover:bg-[var(--color-surface-2)] transition-colors"
                              title="Edit service"
                            >
                              <Pencil size={14} />
                            </button>
                          )}
                          {canWrite && (
                            <button
                              onClick={() => setDeleteConfirm(svc)}
                              className="p-1 rounded-lg text-[var(--color-text-faint)] hover:text-[var(--color-bad)] hover:bg-[var(--color-bad-light)] transition-colors"
                              title="Delete service"
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
                totalItems={filteredServices.length}
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

      {/* Add / Edit Service Modal */}
      <Modal
        isOpen={showModal}
        onClose={closeModal}
        title={editId ? "Edit Service Node" : "Register New Service"}
        subtitle="Specify the service name, regional deployment location, and environment tier."
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          {formError && (
            <div className="text-xs text-[var(--color-bad)] bg-[var(--color-bad-light)] border border-[var(--color-bad)]/30 rounded-lg p-2.5 flex items-center gap-2">
              <AlertTriangle size={14} className="shrink-0" />
              <span>{formError}</span>
            </div>
          )}

          <div>
            <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
              Service Name * <span className="text-[var(--color-text-faint)]">(e.g. orders-api)</span>
            </label>
            <input
              value={form.name}
              onChange={(e) => setField("name")(e.target.value)}
              placeholder="orders-api"
              className="field-input text-xs font-mono"
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
                Region *
              </label>
              <select
                value={form.region}
                onChange={(e) => setField("region")(e.target.value)}
                className="field-input text-xs"
              >
                {REGIONS.map((r) => (
                  <option key={r} value={r}>
                    {r}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
                Environment *
              </label>
              <select
                value={form.environment}
                onChange={(e) => setField("environment")(e.target.value)}
                className="field-input text-xs"
              >
                {ENVIRONMENTS.map((env) => (
                  <option key={env} value={env}>
                    {env}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
              Mesh Zone <span className="text-[var(--color-text-faint)]">(Optional, e.g. eu-west-1a)</span>
            </label>
            <input
              value={form.meshZone}
              onChange={(e) => setField("meshZone")(e.target.value)}
              placeholder="eu-west-1a"
              className="field-input text-xs font-mono"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
              Description <span className="text-[var(--color-text-faint)]">(Optional)</span>
            </label>
            <textarea
              rows={2}
              value={form.description}
              onChange={(e) => setField("description")(e.target.value)}
              placeholder="Handles checkout flows and payment authorizations."
              className="field-input text-xs resize-none"
            />
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
              <Button variant="ghost" size="md" onClick={closeModal}>
                Cancel
              </Button>
              <Button variant="primary" size="md" type="submit" loading={submitting}>
                {editId ? "Update Service" : "Save Service"}
              </Button>
            </div>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={!!deleteConfirm}
        onClose={() => setDeleteConfirm(null)}
        title="Confirm Service Deletion"
        subtitle={`Are you sure you want to remove '${deleteConfirm?.name}'? All connected data flow edges will be removed.`}
      >
        <div className="flex items-center justify-end gap-2 pt-4">
          <Button variant="ghost" size="md" onClick={() => setDeleteConfirm(null)}>
            Cancel
          </Button>
          <Button variant="danger" size="md" onClick={confirmDelete}>
            Delete Service
          </Button>
        </div>
      </Modal>
    </div>
  );
}
