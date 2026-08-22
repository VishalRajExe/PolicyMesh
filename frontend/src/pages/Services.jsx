import { useEffect, useState } from "react";
import {
  Plus,
  Trash2,
  Pencil,
  Loader2,
  Server,
  X,
  Search,
} from "lucide-react";
import Topbar from "../components/layout/Topbar";
import Pagination from "../components/ui/Pagination";
import { servicesApi } from "../api";
import { useAuth } from "../context/AuthContext";
import { useFormDraft } from "../hooks/useFormDraft";
import { useQueryState } from "../hooks/useQueryState";

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

  // URL query state
  const [search, setSearch] = useQueryState("search", "");
  const [regionFilter, setRegionFilter] = useQueryState("region", "ALL");
  const [envFilter, setEnvFilter] = useQueryState("env", "ALL");
  const [page, setPage] = useQueryState("page", 1);
  const [pageSize, setPageSize] = useQueryState("size", 10);

  // Form draft state
  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState(null);
  const { values: form, setValues: setForm, clearDraft, resetForm } = useFormDraft(
    "service-editor",
    EMPTY_FORM
  );
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);
  const [deleteConfirm, setDeleteConfirm] = useState(null);

  const canWrite = user?.role === "ADMIN" || user?.role === "ENGINEER";

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const data = await servicesApi.list();
      setServices(data);
    } catch (err) {
      setError(err.message);
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
    setShowForm(true);
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
    setShowForm(true);
  }

  function closeForm() {
    setShowForm(false);
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
      closeForm();
      await load();
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(id) {
    try {
      await servicesApi.remove(id);
      setDeleteConfirm(null);
      await load();
    } catch (err) {
      setError(err.message);
    }
  }

  const setField = (k) => (v) => setForm((f) => ({ ...f, [k]: v }));

  // Filtered list
  const filteredServices = services.filter((s) => {
    const q = search.trim().toLowerCase();
    const matchSearch =
      !q ||
      s.name.toLowerCase().includes(q) ||
      (s.id && s.id.toLowerCase().includes(q)) ||
      (s.description && s.description.toLowerCase().includes(q));
    const matchRegion = regionFilter === "ALL" || s.region === regionFilter;
    const matchEnv = envFilter === "ALL" || s.environment === envFilter;
    return matchSearch && matchRegion && matchEnv;
  });

  const paginatedServices = filteredServices.slice((page - 1) * pageSize, page * pageSize);

  return (
    <div>
      <Topbar
        title="Services"
        subtitle="Register and manage the microservices and topology nodes in your data mesh."
      />

      <div className="px-6 lg:px-8 mt-4 space-y-4 pb-12">
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
                placeholder="Search services by name or description..."
                className="field-input pl-9 text-xs"
              />
            </div>

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

            <select
              value={envFilter}
              onChange={(e) => {
                setEnvFilter(e.target.value);
                setPage(1);
              }}
              className="field-input py-1.5 text-xs w-36"
            >
              <option value="ALL">All Environments</option>
              {ENVIRONMENTS.map((e) => (
                <option key={e} value={e}>
                  {e}
                </option>
              ))}
            </select>
          </div>

          {canWrite && (
            <button
              onClick={openCreate}
              className="btn-primary flex items-center gap-1.5 text-xs"
            >
              <Plus size={15} />
              Register Service
            </button>
          )}
        </div>

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
                <th className="px-5 py-3 font-medium">Service Node</th>
                <th className="px-5 py-3 font-medium">Region</th>
                <th className="px-5 py-3 font-medium">Environment</th>
                <th className="px-5 py-3 font-medium">Mesh Zone</th>
                <th className="px-5 py-3 font-medium">Description</th>
                {canWrite && <th className="px-5 py-3 text-right">Actions</th>}
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr>
                  <td colSpan={6} className="px-5 py-10 text-center text-[var(--color-text-faint)]">
                    <Loader2 size={18} className="animate-spin inline mr-2" /> Loading services…
                  </td>
                </tr>
              )}
              {!loading && filteredServices.length === 0 && (
                <tr>
                  <td colSpan={6} className="px-5 py-12 text-center">
                    <Server size={32} className="mx-auto mb-3 text-[var(--color-text-faint)]" />
                    <p className="text-[var(--color-text-faint)] text-sm">No services match the filter criteria.</p>
                  </td>
                </tr>
              )}
              {!loading &&
                paginatedServices.map((svc) => (
                  <tr
                    key={svc.id}
                    className="border-b border-[var(--color-border)] last:border-0 hover:bg-[var(--color-surface-2)] transition-colors"
                  >
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-2">
                        <div className="w-2 h-2 rounded-full bg-[var(--color-good)] shrink-0" />
                        <span className="font-semibold text-white text-xs font-mono">{svc.name}</span>
                      </div>
                    </td>
                    <td className="px-5 py-3">
                      <span className="text-xs font-semibold px-2 py-0.5 rounded-lg bg-blue-500/15 text-blue-400 border border-blue-500/30">
                        {svc.region}
                      </span>
                    </td>
                    <td className="px-5 py-3 text-xs text-[var(--color-text-dim)]">{svc.environment}</td>
                    <td className="px-5 py-3 text-xs text-[var(--color-text-dim)]">
                      {svc.meshZone || <span className="text-[var(--color-text-faint)]">—</span>}
                    </td>
                    <td className="px-5 py-3 text-xs text-[var(--color-text-dim)] max-w-xs truncate">
                      {svc.description || <span className="text-[var(--color-text-faint)]">—</span>}
                    </td>
                    {canWrite && (
                      <td className="px-5 py-3 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => openEdit(svc)}
                            className="text-[var(--color-text-faint)] hover:text-white p-1 transition-colors"
                            title="Edit Service"
                          >
                            <Pencil size={14} />
                          </button>
                          <button
                            onClick={() => setDeleteConfirm(svc)}
                            className="text-[var(--color-text-faint)] hover:text-[var(--color-bad)] p-1 transition-colors"
                            title="Delete Service"
                          >
                            <Trash2 size={14} />
                          </button>
                        </div>
                      </td>
                    )}
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
      </div>

      {/* Form Modal */}
      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm px-4">
          <form
            onSubmit={handleSubmit}
            className="card w-full max-w-lg p-6 space-y-4 animate-in fade-in zoom-in-95"
          >
            <div className="flex items-center justify-between mb-1">
              <h2 className="font-semibold text-white text-base">
                {editId ? "Edit Service Node" : "Register New Service Node"}
              </h2>
              <button type="button" onClick={closeForm} className="text-[var(--color-text-faint)] hover:text-white">
                <X size={18} />
              </button>
            </div>

            <Field label="Service Identifier *" placeholder="orders-api">
              <input
                value={form.name}
                onChange={(e) => setField("name")(e.target.value)}
                placeholder="orders-api"
                className="field-input text-xs"
                required
              />
            </Field>

            <div className="grid grid-cols-2 gap-4">
              <Field label="Region *">
                <select
                  value={form.region}
                  onChange={(e) => setField("region")(e.target.value)}
                  className="field-input text-xs"
                  required
                >
                  <option value="">Select region…</option>
                  {REGIONS.map((r) => (
                    <option key={r} value={r}>
                      {r}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="Environment *">
                <select
                  value={form.environment}
                  onChange={(e) => setField("environment")(e.target.value)}
                  className="field-input text-xs"
                  required
                >
                  {ENVIRONMENTS.map((e) => (
                    <option key={e} value={e}>
                      {e}
                    </option>
                  ))}
                </select>
              </Field>
            </div>

            <Field label="Mesh Zone (optional)" placeholder="zone-a">
              <input
                value={form.meshZone}
                onChange={(e) => setField("meshZone")(e.target.value)}
                placeholder="zone-a"
                className="field-input text-xs"
              />
            </Field>

            <Field label="Description (optional)">
              <textarea
                value={form.description}
                onChange={(e) => setField("description")(e.target.value)}
                rows={2}
                placeholder="Brief description of this service…"
                className="field-input text-xs resize-none"
              />
            </Field>

            {formError && (
              <p className="text-xs text-[var(--color-bad)] bg-[var(--color-bad)]/10 rounded-lg px-3 py-2">
                {formError}
              </p>
            )}

            <div className="flex justify-end gap-3 pt-1">
              <button type="button" onClick={closeForm} className="btn-ghost text-xs">
                Cancel
              </button>
              <button type="submit" disabled={submitting} className="btn-primary text-xs">
                {submitting && <Loader2 size={14} className="animate-spin" />}
                {editId ? "Update Service" : "Register Service"}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Delete Confirmation */}
      {deleteConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm px-4">
          <div className="card w-full max-w-sm p-6 space-y-4 border-[var(--color-bad)]/30 animate-in fade-in zoom-in-95">
            <h2 className="font-semibold text-white">Delete Service Node?</h2>
            <p className="text-sm text-[var(--color-text-dim)]">
              This will permanently remove <strong className="text-white">{deleteConfirm.name}</strong> and any data flow
              edges connected to it.
            </p>
            <div className="flex justify-end gap-3 pt-2">
              <button onClick={() => setDeleteConfirm(null)} className="btn-ghost text-xs">
                Cancel
              </button>
              <button
                onClick={() => handleDelete(deleteConfirm.id)}
                className="btn-primary bg-[var(--color-bad)] hover:bg-[var(--color-bad)]/80 text-xs"
              >
                Delete Service
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function Field({ label, children }) {
  return (
    <div>
      <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">{label}</label>
      {children}
    </div>
  );
}
