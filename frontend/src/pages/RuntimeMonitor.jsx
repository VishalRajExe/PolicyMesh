import { useEffect, useState } from "react";
import { Play, ShieldCheck, ShieldAlert, Clock, Loader2, ChevronDown, ChevronUp } from "lucide-react";
import Topbar from "../components/layout/Topbar";
import { enforcementApi, servicesApi, auditApi } from "../api";

const REGIONS = ["EU", "US", "IN", "CN", "GLOBAL", "AP", "ME"];
const DATA_CLASSES = ["PII", "PCI", "PHI", "NON_SENSITIVE", "UNKNOWN"];
const EMPTY = { sourceService: "", destinationService: "", sourceRegion: "", destinationRegion: "", dataClass: "PII" };

export default function RuntimeMonitor() {
  const [form, setForm] = useState(EMPTY);
  const [result, setResult] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);
  const [history, setHistory] = useState([]);
  const [services, setServices] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(true);
  const [expanded, setExpanded] = useState(null);

  useEffect(() => {
    servicesApi.list().then(setServices).catch(() => {});
    loadHistory();
  }, []);

  async function loadHistory() {
    setHistoryLoading(true);
    try {
      const items = await auditApi.recent(20);
      setHistory(items);
    } catch {
      // history is optional
    } finally {
      setHistoryLoading(false);
    }
  }

  const set = (k) => (v) => setForm((f) => ({ ...f, [k]: v }));

  async function handleCheck(e) {
    e.preventDefault();
    const { sourceService, destinationService, sourceRegion, destinationRegion, dataClass } = form;
    if (!sourceService.trim() || !destinationService.trim()) {
      setFormError("Source and destination service names are required.");
      return;
    }
    if (!dataClass) {
      setFormError("Select a data class.");
      return;
    }
    setFormError(null);
    setResult(null);
    setSubmitting(true);
    try {
      const resp = await enforcementApi.check({
        sourceService: sourceService.trim(),
        destinationService: destinationService.trim(),
        sourceRegion: sourceRegion || undefined,
        destinationRegion: destinationRegion || undefined,
        dataClass,
      });
      setResult({ ...resp, _ts: new Date().toISOString(), _form: { ...form } });
      await loadHistory();
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div>
      <Topbar
        title="Runtime Monitor"
        subtitle="Test live enforcement decisions against your active policies."
      />

      <div className="px-6 lg:px-8 mt-6 grid grid-cols-1 xl:grid-cols-5 gap-6 pb-8">
        {/* Check form */}
        <div className="xl:col-span-2">
          <div className="card p-5">
            <h2 className="font-semibold text-white mb-4">Enforcement Check</h2>
            <form onSubmit={handleCheck} className="space-y-4">
              <FormField label="Source Service *">
                <ServiceInput
                  value={form.sourceService}
                  onChange={set("sourceService")}
                  services={services}
                  placeholder="orders-api"
                />
              </FormField>

              <FormField label="Destination Service *">
                <ServiceInput
                  value={form.destinationService}
                  onChange={set("destinationService")}
                  services={services}
                  placeholder="analytics-api"
                />
              </FormField>

              <div className="grid grid-cols-2 gap-3">
                <FormField label="Source Region">
                  <select value={form.sourceRegion} onChange={(e) => set("sourceRegion")(e.target.value)} className="field-input">
                    <option value="">Auto-detect</option>
                    {REGIONS.map((r) => <option key={r} value={r}>{r}</option>)}
                  </select>
                </FormField>
                <FormField label="Destination Region">
                  <select value={form.destinationRegion} onChange={(e) => set("destinationRegion")(e.target.value)} className="field-input">
                    <option value="">Auto-detect</option>
                    {REGIONS.map((r) => <option key={r} value={r}>{r}</option>)}
                  </select>
                </FormField>
              </div>

              <FormField label="Data Class *">
                <div className="flex flex-wrap gap-2">
                  {DATA_CLASSES.map((dc) => (
                    <button
                      key={dc}
                      type="button"
                      onClick={() => set("dataClass")(dc)}
                      className={`text-xs font-medium px-3 py-1.5 rounded-lg border transition-colors ${
                        form.dataClass === dc
                          ? "bg-[var(--color-brand)] border-[var(--color-brand)] text-white"
                          : "border-[var(--color-border)] text-[var(--color-text-dim)] hover:border-[var(--color-brand)] hover:text-white"
                      }`}
                    >
                      {dc}
                    </button>
                  ))}
                </div>
              </FormField>

              {formError && (
                <p className="text-xs text-[var(--color-bad)] bg-[var(--color-bad)]/10 rounded-lg px-3 py-2">{formError}</p>
              )}

              <button type="submit" disabled={submitting} className="btn-primary w-full justify-center">
                {submitting ? <Loader2 size={15} className="animate-spin" /> : <Play size={15} />}
                {submitting ? "Checking…" : "Run Enforcement Check"}
              </button>
            </form>
          </div>

          {/* Result card */}
          {result && (
            <div className={`card mt-4 p-5 border-2 ${
              result.decision === "ALLOW"
                ? "border-[var(--color-good)]/40 bg-[var(--color-good)]/5"
                : "border-[var(--color-bad)]/40 bg-[var(--color-bad)]/5"
            }`}>
              <div className="flex items-center gap-3 mb-3">
                {result.decision === "ALLOW"
                  ? <ShieldCheck size={28} className="text-[var(--color-good)]" />
                  : <ShieldAlert size={28} className="text-[var(--color-bad)]" />}
                <div>
                  <p className={`text-xl font-bold ${result.decision === "ALLOW" ? "text-[var(--color-good)]" : "text-[var(--color-bad)]"}`}>
                    {result.decision}
                  </p>
                  <p className="text-xs text-[var(--color-text-faint)]">
                    {result._form.sourceService} → {result._form.destinationService}
                  </p>
                </div>
              </div>
              <div className="space-y-2 text-sm">
                <DetailRow label="Reason" value={result.reason} />
                {result.policyId && <DetailRow label="Policy" value={result.policyId} />}
                {result.decisionId && <DetailRow label="Decision ID" value={`#${result.decisionId}`} />}
                {result.lineageId && <DetailRow label="Lineage ID" value={`#${result.lineageId}`} />}
                {result.lineageHash && (
                  <div>
                    <p className="text-[var(--color-text-faint)] text-xs mb-0.5">Lineage Hash</p>
                    <p className="font-mono text-xs text-[var(--color-text-dim)] break-all">{result.lineageHash}</p>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {/* History table */}
        <div className="xl:col-span-3">
          <div className="card overflow-hidden">
            <div className="px-5 py-4 border-b border-[var(--color-border)] flex items-center justify-between">
              <h2 className="font-semibold text-white text-sm">Recent Decisions</h2>
              <button onClick={loadHistory} className="text-[var(--color-text-faint)] hover:text-white transition-colors" title="Refresh">
                <Clock size={14} />
              </button>
            </div>
            {historyLoading && (
              <div className="px-5 py-8 text-center text-[var(--color-text-faint)]">
                <Loader2 size={16} className="animate-spin inline mr-2" />Loading…
              </div>
            )}
            {!historyLoading && history.length === 0 && (
              <div className="px-5 py-10 text-center text-[var(--color-text-faint)] text-sm">
                No decisions recorded yet. Run a check above.
              </div>
            )}
            {!historyLoading && history.length > 0 && (
              <div className="divide-y divide-[var(--color-border)]">
                {history.map((d) => (
                  <div key={d.id} className="px-5 py-3">
                    <div
                      className="flex items-center justify-between cursor-pointer"
                      onClick={() => setExpanded(expanded === d.id ? null : d.id)}
                    >
                      <div className="flex items-center gap-3 min-w-0">
                        <DecisionBadge decision={d.decision} />
                        <div className="min-w-0">
                          <p className="text-sm text-white truncate">
                            {d.sourceService} → {d.destinationService}
                          </p>
                          <p className="text-xs text-[var(--color-text-faint)]">
                            {d.dataClass} · {d.sourceRegion ?? "?"} → {d.destinationRegion ?? "?"}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center gap-3 shrink-0">
                        <span className="text-xs text-[var(--color-text-faint)] hidden sm:block">
                          {new Date(d.createdAt).toLocaleTimeString()}
                        </span>
                        {expanded === d.id ? <ChevronUp size={14} className="text-[var(--color-text-faint)]" /> : <ChevronDown size={14} className="text-[var(--color-text-faint)]" />}
                      </div>
                    </div>
                    {expanded === d.id && (
                      <div className="mt-2 bg-[var(--color-surface-2)] rounded-lg px-3 py-2 space-y-1 text-xs">
                        <DetailRow label="Policy" value={d.policyId || "—"} />
                        <DetailRow label="Reason" value={d.reason} />
                        <DetailRow label="Time" value={new Date(d.createdAt).toLocaleString()} />
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function FormField({ label, children }) {
  return (
    <div>
      <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">{label}</label>
      {children}
    </div>
  );
}

function ServiceInput({ value, onChange, services, placeholder }) {
  return (
    <div className="relative">
      <input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        list="svc-list"
        className="field-input"
        autoComplete="off"
      />
      <datalist id="svc-list">
        {services.map((s) => <option key={s.id} value={s.name} />)}
      </datalist>
    </div>
  );
}

function DecisionBadge({ decision }) {
  return (
    <span className={`text-xs font-bold px-2 py-0.5 rounded shrink-0 ${
      decision === "ALLOW"
        ? "bg-[var(--color-good)]/15 text-[var(--color-good)]"
        : "bg-[var(--color-bad)]/15 text-[var(--color-bad)]"
    }`}>
      {decision}
    </span>
  );
}

function DetailRow({ label, value }) {
  return (
    <div className="flex gap-2">
      <span className="text-[var(--color-text-faint)] shrink-0 w-20">{label}</span>
      <span className="text-[var(--color-text-dim)]">{value}</span>
    </div>
  );
}
