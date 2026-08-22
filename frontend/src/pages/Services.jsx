import { useEffect, useState } from "react";
import { Plus, Trash2, Pencil, Loader2, Server, X, Check } from "lucide-react";
import Topbar from "../components/layout/Topbar";
import { servicesApi } from "../api";
import { useAuth } from "../context/AuthContext";

const REGIONS = ["EU", "US", "IN", "CN", "GLOBAL", "AP", "ME"];
const ENVIRONMENTS = ["production", "staging", "development", "qa"];

const EMPTY_FORM = { name: "", region: "", environment: "production", meshZone: "", description: "" };

export default function Services() {
  const { user } = useAuth();
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
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

  useEffect(() => { load(); }, []);

  function openCreate() {
    setEditId(null);
    setForm(EMPTY_FORM);
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
    setForm(EMPTY_FORM);
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

  const set = (k) => (v) => setForm((f) => ({ ...f, [k]: v }));

  return (
    <div>
      <Topbar
        title="Services"
        subtitle="Register and manage the microservices in your data mesh."
      />

      <div className="px-6 lg:px-8 mt-4 flex items-center justify-between">
        <p className="text-sm text-[var(--color-text-faint)]">
          {services.length} service{services.length !== 1 ? "s" : ""} registered
        </p>
        {canWrite && (
          <button
            onClick={openCreate}
            className="flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-medium bg-[var(--color-brand)] text-white hover:bg-[var(--color-brand-dim)] transition-colors"
          >
            <Plus size={15} />
            Register Service
          </button>
        )}
      </div>

      {/* Form modal */}
      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 px-4">
          <form
            onSubmit={handleSubmit}
            className="card w-full max-w-lg p-6 space-y-4"
          >
            <div className="flex items-center justify-between mb-1">
              <h2 className="font-semibold text-white text-base">
                {editId ? "Edit Service" : "Register New Service"}
              </h2>
              <button type="button" onClick={closeForm} className="text-[var(--color-text-faint)] hover:text-white">
                <X size={18} />
              </button>
            </div>

            <Field label="Service Name *" placeholder="orders-api">
              <input
                value={form.name}
                onChange={(e) => set("name")(e.target.value)}
                placeholder="orders-api"
                className="field-input"
                required
              />
            </Field>

            <div className="grid grid-cols-2 gap-4">
              <Field label="Region *">
                <select value={form.region} onChange={(e) => set("region")(e.target.value)} className="field-input" required>
                  <option value="">Select region…</option>
                  {REGIONS.map((r) => <option key={r} value={r}>{r}</option>)}
                </select>
              </Field>
              <Field label="Environment *">
                <select value={form.environment} onChange={(e) => set("environment")(e.target.value)} className="field-input" required>
                  {ENVIRONMENTS.map((e) => <option key={e} value={e}>{e}</option>)}
                </select>
              </Field>
            </div>

            <Field label="Mesh Zone (optional)" placeholder="zone-a">
              <input
                value={form.meshZone}
                onChange={(e) => set("meshZone")(e.target.value)}
                placeholder="zone-a"
                className="field-input"
              />
            </Field>

            <Field label="Description (optional)">
              <textarea
                value={form.description}
                onChange={(e) => set("description")(e.target.value)}
                rows={2}
                placeholder="Brief description of this service…"
                className="field-input resize-none"
              />
            </Field>

            {formError && (
              <p className="text-xs text-[var(--color-bad)] bg-[var(--color-bad)]/10 rounded-lg px-3 py-2">{formError}</p>
            )}

            <div className="flex justify-end gap-3 pt-1">
              <button type="button" onClick={closeForm} className="btn-ghost">Cancel</button>
              <button type="submit" disabled={submitting} className="btn-primary">
                {submitting && <Loader2 size={14} className="animate-spin" />}
                {editId ? "Update" : "Register"}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Delete confirmation */}
      {deleteConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 px-4">
          <div className="card w-full max-w-sm p-6 space-y-4">
            <h2 className="font-semibold text-white">Delete Service?</h2>
            <p className="text-sm text-[var(--color-text-dim)]">
              This will permanently remove <strong className="text-white">{deleteConfirm.name}</strong> and any data flow edges connected to it.
            </p>
            <div className="flex justify-end gap-3">
              <button onClick={() => setDeleteConfirm(null)} className="btn-ghost">Cancel</button>
              <button
                onClick={() => handleDelete(deleteConfirm.id)}
                className="flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-medium bg-[var(--color-bad)] text-white hover:opacity-90 transition-opacity"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="px-6 lg:px-8 mt-4 pb-8">
        {error && (
          <div className="rounded-xl bg-[var(--color-bad)]/10 border border-[var(--color-bad)]/30 px-4 py-3 text-sm text-[var(--color-bad)] mb-4 flex items-center justify-between">
            {error}
            <button onClick={load} className="underline ml-4 text-xs">Retry</button>
          </div>
        )}

        <div className="card overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-[var(--color-text-faint)] border-b border-[var(--color-border)]">
                <th className="px-5 py-3 font-medium">Service</th>
                <th className="px-5 py-3 font-medium">Region</th>
                <th className="px-5 py-3 font-medium">Environment</th>
                <th className="px-5 py-3 font-medium">Mesh Zone</th>
                <th className="px-5 py-3 font-medium">Description</th>
                {canWrite && <th className="px-5 py-3" />}
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr>
                  <td colSpan={6} className="px-5 py-10 text-center text-[var(--color-text-faint)]">
                    <Loader2 size={18} className="animate-spin inline mr-2" />
                    Loading services…
                  </td>
                </tr>
              )}
              {!loading && services.length === 0 && (
                <tr>
                  <td colSpan={6} className="px-5 py-12 text-center">
                    <Server size={32} className="mx-auto mb-3 text-[var(--color-text-faint)]" />
                    <p className="text-[var(--color-text-faint)]">No services registered yet.</p>
                    {canWrite && (
                      <button onClick={openCreate} className="mt-3 text-sm text-[var(--color-brand)] hover:underline">
                        Register your first service
                      </button>
                    )}
                  </td>
                </tr>
              )}
              {services.map((svc) => (
                <tr
                  key={svc.id}
                  className="border-b border-[var(--color-border)] last:border-0 hover:bg-[var(--color-surface-2)] transition-colors"
                >
                  <td className="px-5 py-3">
                    <div className="flex items-center gap-2">
                      <div className="w-2 h-2 rounded-full bg-[var(--color-good)] shrink-0" />
                      <span className="font-medium text-white">{svc.name}</span>
                    </div>
                  </td>
                  <td className="px-5 py-3">
                    <span className="text-xs font-medium px-2 py-1 rounded-lg bg-[var(--color-info)]/15 text-[#60a5fa]">
                      {svc.region}
                    </span>
                  </td>
                  <td className="px-5 py-3 text-[var(--color-text-dim)]">{svc.environment}</td>
                  <td className="px-5 py-3 text-[var(--color-text-dim)]">{svc.meshZone || <span className="text-[var(--color-text-faint)]">—</span>}</td>
                  <td className="px-5 py-3 text-[var(--color-text-dim)] max-w-xs truncate">{svc.description || <span className="text-[var(--color-text-faint)]">—</span>}</td>
                  {canWrite && (
                    <td className="px-5 py-3 text-right space-x-2">
                      <button
                        onClick={() => openEdit(svc)}
                        className="text-[var(--color-text-faint)] hover:text-[var(--color-info)] transition-colors"
                        title="Edit"
                      >
                        <Pencil size={14} />
                      </button>
                      <button
                        onClick={() => setDeleteConfirm(svc)}
                        className="text-[var(--color-text-faint)] hover:text-[var(--color-bad)] transition-colors"
                        title="Delete"
                      >
                        <Trash2 size={14} />
                      </button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
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
