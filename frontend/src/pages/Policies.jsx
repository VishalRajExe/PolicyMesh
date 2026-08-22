import { useEffect, useState } from "react";
import { Plus, Trash2, Loader2, ToggleLeft, ToggleRight } from "lucide-react";
import Topbar from "../components/layout/Topbar";
import { policiesApi } from "../api";
import { useAuth } from "../context/AuthContext";

const JURISDICTIONS = ["EU", "US", "IN", "CN", "UK", "GLOBAL", "AP"];
const DATA_CLASSES = ["PII", "PCI", "PHI", "NON_SENSITIVE", "UNKNOWN"];
const REGIONS = ["EU", "US", "IN", "CN", "UK", "GLOBAL", "AP", "ME"];

const EMPTY_FORM = { policyCode: "", name: "", jurisdiction: "", dataClass: "", allowedRegions: "", deniedRegions: "" };

const STATUS_STYLE = {
  ACTIVE: "bg-[#22c55e]/15 text-[#4ade80]",
  DRAFT: "bg-[#3b82f6]/15 text-[#60a5fa]",
  UNDER_REVIEW: "bg-[#f59e0b]/15 text-[#fbbf24]",
  INACTIVE: "bg-[#5b6478]/15 text-[#8b93a7]",
};

export default function Policies() {
  const { user } = useAuth();
  const [policies, setPolicies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
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

  useEffect(() => { load(); }, []);

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
      setForm(EMPTY_FORM);
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

  const set = (k) => (v) => setForm((f) => ({ ...f, [k]: v }));

  return (
    <div>
      <Topbar title="Policies" subtitle="Manage the data-residency rules that govern every transfer." />

      <div className="px-6 lg:px-8 mt-4 flex items-center justify-between">
        <p className="text-sm text-[var(--color-text-faint)]">
          {policies.length} polic{policies.length !== 1 ? "ies" : "y"}
        </p>
        {canWrite && (
          <button
            onClick={() => setShowForm((s) => !s)}
            className="flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-medium bg-[var(--color-brand)] text-white hover:bg-[var(--color-brand-dim)] transition-colors"
          >
            <Plus size={15} />
            New Policy
          </button>
        )}
      </div>

      {showForm && (
        <form onSubmit={handleCreate} className="card p-5 mx-6 lg:mx-8 mt-4 grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">Policy Code * <span className="text-[var(--color-text-faint)]">(e.g. EU-PII-001)</span></label>
            <input
              value={form.policyCode}
              onChange={(e) => set("policyCode")(e.target.value.toUpperCase())}
              placeholder="EU-PII-001"
              className="field-input"
              required
            />
          </div>
          <div>
            <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">Name *</label>
            <input value={form.name} onChange={(e) => set("name")(e.target.value)} placeholder="EU PII Protection" className="field-input" required />
          </div>
          <div>
            <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">Jurisdiction *</label>
            <select value={form.jurisdiction} onChange={(e) => set("jurisdiction")(e.target.value)} className="field-input" required>
              <option value="">Select…</option>
              {JURISDICTIONS.map((j) => <option key={j} value={j}>{j}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">Data Class *</label>
            <select value={form.dataClass} onChange={(e) => set("dataClass")(e.target.value)} className="field-input" required>
              <option value="">Select…</option>
              {DATA_CLASSES.map((d) => <option key={d} value={d}>{d}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">Allowed Regions <span className="text-[var(--color-text-faint)]">(comma-separated)</span></label>
            <input value={form.allowedRegions} onChange={(e) => set("allowedRegions")(e.target.value)} placeholder="EU, UK" className="field-input" />
          </div>
          <div>
            <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">Denied Regions <span className="text-[var(--color-text-faint)]">(comma-separated)</span></label>
            <input value={form.deniedRegions} onChange={(e) => set("deniedRegions")(e.target.value)} placeholder="US, CN" className="field-input" />
          </div>

          <div className="md:col-span-3 flex justify-end gap-3">
            <button type="button" onClick={() => setShowForm(false)} className="btn-ghost">Cancel</button>
            <button type="submit" disabled={submitting} className="btn-primary">
              {submitting && <Loader2 size={14} className="animate-spin" />}
              Save Policy
            </button>
          </div>
        </form>
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
                <th className="px-5 py-3 font-medium">Code</th>
                <th className="px-5 py-3 font-medium">Name</th>
                <th className="px-5 py-3 font-medium">Jurisdiction</th>
                <th className="px-5 py-3 font-medium">Data Class</th>
                <th className="px-5 py-3 font-medium">Allowed</th>
                <th className="px-5 py-3 font-medium">Denied</th>
                <th className="px-5 py-3 font-medium">Status</th>
                <th className="px-5 py-3" />
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr>
                  <td colSpan={8} className="px-5 py-8 text-center text-[var(--color-text-faint)]">
                    <Loader2 size={16} className="animate-spin inline mr-2" />Loading policies…
                  </td>
                </tr>
              )}
              {!loading && policies.length === 0 && (
                <tr>
                  <td colSpan={8} className="px-5 py-10 text-center text-[var(--color-text-faint)]">
                    No policies yet.{" "}
                    {canWrite && (
                      <button onClick={() => setShowForm(true)} className="text-[var(--color-brand)] hover:underline">
                        Create your first policy
                      </button>
                    )}
                  </td>
                </tr>
              )}
              {policies.map((p) => (
                <tr key={p.id} className="border-b border-[var(--color-border)] last:border-0 hover:bg-[var(--color-surface-2)] transition-colors">
                  <td className="px-5 py-3 text-white font-medium font-mono text-xs">{p.policyCode}</td>
                  <td className="px-5 py-3 text-[var(--color-text-dim)]">{p.name}</td>
                  <td className="px-5 py-3 text-[var(--color-text-dim)]">{p.jurisdiction}</td>
                  <td className="px-5 py-3 text-[var(--color-text-dim)]">{p.dataClass}</td>
                  <td className="px-5 py-3 text-[var(--color-text-dim)]">{[...p.allowedRegions].join(", ") || "—"}</td>
                  <td className="px-5 py-3 text-[var(--color-text-dim)]">{[...p.deniedRegions].join(", ") || "—"}</td>
                  <td className="px-5 py-3">
                    <span className={`text-xs font-medium px-2 py-0.5 rounded-lg ${STATUS_STYLE[p.status] || STATUS_STYLE.INACTIVE}`}>
                      {p.status}
                    </span>
                  </td>
                  <td className="px-5 py-3 text-right">
                    <div className="flex items-center justify-end gap-3">
                      {canWrite && (
                        <button
                          onClick={() => toggleStatus(p)}
                          disabled={togglingId === p.id}
                          title={p.status === "ACTIVE" ? "Deactivate" : "Activate"}
                          className={`transition-colors disabled:opacity-50 ${
                            p.status === "ACTIVE"
                              ? "text-[var(--color-good)] hover:text-[var(--color-text-faint)]"
                              : "text-[var(--color-text-faint)] hover:text-[var(--color-good)]"
                          }`}
                        >
                          {togglingId === p.id
                            ? <Loader2 size={15} className="animate-spin" />
                            : p.status === "ACTIVE"
                              ? <ToggleRight size={18} />
                              : <ToggleLeft size={18} />}
                        </button>
                      )}
                      {canWrite && (
                        <button
                          onClick={() => handleDelete(p.id)}
                          className="text-[var(--color-text-faint)] hover:text-[var(--color-bad)] transition-colors"
                          title="Delete"
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
        </div>
      </div>
    </div>
  );
}

function splitCsv(value) {
  return value.split(",").map((v) => v.trim()).filter(Boolean);
}
