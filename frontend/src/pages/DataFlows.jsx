import { useEffect, useState } from "react";
import { Plus, Trash2, ShieldCheck, ShieldAlert, GitBranch, Loader2, RefreshCw, X } from "lucide-react";
import Topbar from "../components/layout/Topbar";
import { servicesApi, edgesApi, graphApi } from "../api";
import { useAuth } from "../context/AuthContext";

const DATA_CLASSES = ["PII", "PCI", "PHI", "NON_SENSITIVE", "UNKNOWN"];

export default function DataFlows() {
  const { user } = useAuth();
  const [services, setServices] = useState([]);
  const [edges, setEdges] = useState([]);
  const [violations, setViolations] = useState([]);
  const [validationResult, setValidationResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [validating, setValidating] = useState(false);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ sourceServiceId: "", destinationServiceId: "", dataClasses: [] });
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);

  const canWrite = user?.role === "ADMIN" || user?.role === "ENGINEER";
  const svcMap = Object.fromEntries(services.map((s) => [s.id, s]));

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const [svcs, edg] = await Promise.all([servicesApi.list(), edgesApi.list()]);
      setServices(svcs);
      setEdges(edg);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function runValidation() {
    setValidating(true);
    try {
      const result = await graphApi.validate();
      setValidationResult(result);
      setViolations(result.violations || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setValidating(false);
    }
  }

  useEffect(() => {
    load().then(() => runValidation());
  }, []);

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
    if (form.sourceServiceId === form.destinationServiceId) {
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
      setForm({ sourceServiceId: "", destinationServiceId: "", dataClasses: [] });
      await load();
      await runValidation();
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(id) {
    try {
      await edgesApi.remove(id);
      await load();
      await runValidation();
    } catch (err) {
      setError(err.message);
    }
  }

  function edgeStatus(edge) {
    return violations.some((v) => v.edgeId === edge.id) ? "FLAGGED" : "ALLOWED";
  }

  return (
    <div>
      <Topbar
        title="Data Flows"
        subtitle="Service-to-service data flow edges and their compliance status."
      />

      <div className="px-6 lg:px-8 mt-4 flex flex-wrap items-center gap-3 justify-between">
        <div className="flex items-center gap-3">
          {validationResult && (
            <span className={`flex items-center gap-1.5 text-sm font-medium px-3 py-1.5 rounded-xl border ${
              validationResult.result === "PASS"
                ? "bg-[var(--color-good)]/10 border-[var(--color-good)]/30 text-[var(--color-good)]"
                : "bg-[var(--color-bad)]/10 border-[var(--color-bad)]/30 text-[var(--color-bad)]"
            }`}>
              {validationResult.result === "PASS"
                ? <ShieldCheck size={14} />
                : <ShieldAlert size={14} />}
              {validationResult.result === "PASS"
                ? `All ${validationResult.checkedEdges} edges compliant`
                : `${validationResult.violationCount} violation${validationResult.violationCount !== 1 ? "s" : ""}`}
            </span>
          )}
          <button
            onClick={() => { load(); runValidation(); }}
            disabled={validating}
            className="flex items-center gap-1.5 text-xs text-[var(--color-text-dim)] hover:text-white border border-[var(--color-border)] rounded-xl px-3 py-1.5 transition-colors disabled:opacity-50"
          >
            <RefreshCw size={13} className={validating ? "animate-spin" : ""} />
            Validate Graph
          </button>
        </div>
        {canWrite && (
          <button
            onClick={() => { setShowForm(true); setFormError(null); }}
            className="flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-medium bg-[var(--color-brand)] text-white hover:bg-[var(--color-brand-dim)] transition-colors"
          >
            <Plus size={15} />
            Add Data Flow
          </button>
        )}
      </div>

      {/* Violations panel */}
      {violations.length > 0 && (
        <div className="mx-6 lg:mx-8 mt-4 rounded-xl border border-[var(--color-bad)]/40 bg-[var(--color-bad)]/8 p-4">
          <p className="text-sm font-medium text-[var(--color-bad)] mb-3 flex items-center gap-2">
            <ShieldAlert size={14} />
            Compliance Violations Detected
          </p>
          <div className="space-y-2">
            {violations.map((v, i) => (
              <div key={i} className="text-xs bg-black/20 rounded-lg px-3 py-2 space-y-0.5">
                <p className="text-white font-medium">
                  {v.sourceService} [{v.sourceRegion}] → {v.destinationService} [{v.destinationRegion}]
                </p>
                <p className="text-[var(--color-text-dim)]">
                  <span className="text-[var(--color-bad)]">{v.dataClass}</span> · Policy <span className="text-white">{v.policyCode}</span>
                </p>
                <p className="text-[var(--color-text-faint)]">{v.reason}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Add flow modal */}
      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 px-4">
          <form onSubmit={handleCreate} className="card w-full max-w-md p-6 space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="font-semibold text-white text-base">Add Data Flow Edge</h2>
              <button type="button" onClick={() => setShowForm(false)} className="text-[var(--color-text-faint)] hover:text-white">
                <X size={18} />
              </button>
            </div>

            <div>
              <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">Source Service *</label>
              <select
                value={form.sourceServiceId}
                onChange={(e) => setForm((f) => ({ ...f, sourceServiceId: e.target.value }))}
                className="field-input"
                required
              >
                <option value="">Select source service…</option>
                {services.map((s) => <option key={s.id} value={s.id}>{s.name} [{s.region}]</option>)}
              </select>
            </div>

            <div>
              <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">Destination Service *</label>
              <select
                value={form.destinationServiceId}
                onChange={(e) => setForm((f) => ({ ...f, destinationServiceId: e.target.value }))}
                className="field-input"
                required
              >
                <option value="">Select destination service…</option>
                {services.map((s) => <option key={s.id} value={s.id}>{s.name} [{s.region}]</option>)}
              </select>
            </div>

            <div>
              <label className="block text-xs text-[var(--color-text-dim)] mb-2">Data Classes *</label>
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
              <p className="text-xs text-[var(--color-bad)] bg-[var(--color-bad)]/10 rounded-lg px-3 py-2">{formError}</p>
            )}

            <div className="flex justify-end gap-3 pt-1">
              <button type="button" onClick={() => setShowForm(false)} className="btn-ghost">Cancel</button>
              <button type="submit" disabled={submitting} className="btn-primary">
                {submitting && <Loader2 size={14} className="animate-spin" />}
                Add Flow
              </button>
            </div>
          </form>
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
                <th className="px-5 py-3 font-medium">Source</th>
                <th className="px-5 py-3 font-medium">Destination</th>
                <th className="px-5 py-3 font-medium">Data Classes</th>
                <th className="px-5 py-3 font-medium">Status</th>
                {canWrite && <th className="px-5 py-3" />}
              </tr>
            </thead>
            <tbody>
              {loading && (
                <tr>
                  <td colSpan={5} className="px-5 py-10 text-center text-[var(--color-text-faint)]">
                    <Loader2 size={18} className="animate-spin inline mr-2" />Loading data flows…
                  </td>
                </tr>
              )}
              {!loading && edges.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-5 py-12 text-center">
                    <GitBranch size={32} className="mx-auto mb-3 text-[var(--color-text-faint)]" />
                    <p className="text-[var(--color-text-faint)]">No data flow edges defined yet.</p>
                    {canWrite && (
                      <button onClick={() => setShowForm(true)} className="mt-3 text-sm text-[var(--color-brand)] hover:underline">
                        Add your first data flow
                      </button>
                    )}
                  </td>
                </tr>
              )}
              {edges.map((edge) => {
                const src = svcMap[edge.sourceServiceId];
                const dst = svcMap[edge.destinationServiceId];
                const status = edgeStatus(edge);
                const edgeViolations = violations.filter((v) => v.edgeId === edge.id);
                return (
                  <tr key={edge.id} className="border-b border-[var(--color-border)] last:border-0 hover:bg-[var(--color-surface-2)] transition-colors">
                    <td className="px-5 py-3">
                      <p className="font-medium text-white">{src?.name ?? `#${edge.sourceServiceId}`}</p>
                      {src && <p className="text-xs text-[var(--color-text-faint)]">{src.region}</p>}
                    </td>
                    <td className="px-5 py-3">
                      <p className="font-medium text-white">{dst?.name ?? `#${edge.destinationServiceId}`}</p>
                      {dst && <p className="text-xs text-[var(--color-text-faint)]">{dst.region}</p>}
                    </td>
                    <td className="px-5 py-3">
                      <div className="flex flex-wrap gap-1">
                        {[...edge.dataClasses].map((dc) => (
                          <span key={dc} className="text-xs px-2 py-0.5 rounded bg-[var(--color-surface-2)] border border-[var(--color-border)] text-[var(--color-text-dim)]">
                            {dc}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td className="px-5 py-3">
                      {status === "ALLOWED" ? (
                        <span className="flex items-center gap-1 text-xs font-medium text-[var(--color-good)]">
                          <ShieldCheck size={12} /> Compliant
                        </span>
                      ) : (
                        <div>
                          <span className="flex items-center gap-1 text-xs font-medium text-[var(--color-bad)]">
                            <ShieldAlert size={12} /> Violation
                          </span>
                          {edgeViolations.map((v, i) => (
                            <p key={i} className="text-xs text-[var(--color-text-faint)] mt-0.5">{v.reason}</p>
                          ))}
                        </div>
                      )}
                    </td>
                    {canWrite && (
                      <td className="px-5 py-3 text-right">
                        <button
                          onClick={() => handleDelete(edge.id)}
                          className="text-[var(--color-text-faint)] hover:text-[var(--color-bad)] transition-colors"
                          title="Delete"
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
        </div>
      </div>
    </div>
  );
}
