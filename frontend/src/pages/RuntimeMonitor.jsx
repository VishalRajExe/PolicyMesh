import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import {
  Play,
  ShieldCheck,
  ShieldAlert,
  Clock,
  Loader2,
  ChevronDown,
  ChevronUp,
  RotateCcw,
  Sparkles,
  Zap,
} from "lucide-react";
import Topbar from "../components/layout/Topbar";
import Pagination from "../components/ui/Pagination";
import { enforcementApi, servicesApi, auditApi } from "../api";
import { useFormDraft } from "../hooks/useFormDraft";

const REGIONS = ["EU", "US", "IN", "CN", "GLOBAL", "AP", "ME"];
const DATA_CLASSES = ["PII", "PCI", "PHI", "NON_SENSITIVE", "UNKNOWN"];
const DEFAULT_FORM = {
  sourceService: "",
  destinationService: "",
  sourceRegion: "",
  destinationRegion: "",
  dataClass: "PII",
};

const QUICK_SCENARIOS = [
  {
    label: "EU → EU (PII) • Expected ALLOW",
    sourceService: "orders-api",
    destinationService: "payments-api",
    sourceRegion: "EU",
    destinationRegion: "EU",
    dataClass: "PII",
    tone: "good",
  },
  {
    label: "EU → US (PII) • Expected DENY",
    sourceService: "orders-api",
    destinationService: "analytics-api",
    sourceRegion: "EU",
    destinationRegion: "US",
    dataClass: "PII",
    tone: "bad",
  },
  {
    label: "EU → US (PCI) • Expected DENY",
    sourceService: "payments-api",
    destinationService: "analytics-api",
    sourceRegion: "EU",
    destinationRegion: "US",
    dataClass: "PCI",
    tone: "bad",
  },
];

export default function RuntimeMonitor() {
  const [searchParams] = useSearchParams();
  const { values: form, setValues: setForm, clearDraft, resetForm } = useFormDraft(
    "runtime-monitor",
    DEFAULT_FORM
  );

  const [result, setResult] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);
  const [history, setHistory] = useState([]);
  const [services, setServices] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(true);
  const [expanded, setExpanded] = useState(null);

  // Pagination for decisions history
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  useEffect(() => {
    servicesApi.list().then(setServices).catch(() => {});
    loadHistory();
  }, []);

  // Override draft with URL query params if supplied
  useEffect(() => {
    const src = searchParams.get("source");
    const dst = searchParams.get("target") || searchParams.get("destination");
    const dc = searchParams.get("dataClass");
    if (src || dst || dc) {
      setForm((prev) => ({
        ...prev,
        sourceService: src || prev.sourceService,
        destinationService: dst || prev.destinationService,
        dataClass: dc || prev.dataClass,
      }));
    }
  }, [searchParams, setForm]);

  async function loadHistory() {
    setHistoryLoading(true);
    try {
      const items = await auditApi.recent(100);
      setHistory(items);
    } catch {
      // history is optional
    } finally {
      setHistoryLoading(false);
    }
  }

  const setField = (field) => (val) => {
    setForm((f) => {
      const next = { ...f, [field]: val };
      // Auto-populate region if known service selected
      if (field === "sourceService") {
        const found = services.find((s) => s.id === val || s.name === val);
        if (found && found.region && !next.sourceRegion) {
          next.sourceRegion = found.region;
        }
      }
      if (field === "destinationService") {
        const found = services.find((s) => s.id === val || s.name === val);
        if (found && found.region && !next.destinationRegion) {
          next.destinationRegion = found.region;
        }
      }
      return next;
    });
  };

  function applyScenario(sc) {
    setForm({
      sourceService: sc.sourceService,
      destinationService: sc.destinationService,
      sourceRegion: sc.sourceRegion,
      destinationRegion: sc.destinationRegion,
      dataClass: sc.dataClass,
    });
    setFormError(null);
  }

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

  const paginatedHistory = history.slice((page - 1) * pageSize, page * pageSize);

  return (
    <div>
      <Topbar
        title="Runtime Monitor"
        subtitle="Execute live zero-trust policy enforcement evaluations against compiled AST rules."
      />

      <div className="px-6 lg:px-8 mt-6 grid grid-cols-1 xl:grid-cols-5 gap-6 pb-12">
        {/* Left Column: Form & Result */}
        <div className="xl:col-span-2 space-y-4">
          <div className="card p-5">
            {/* Header with Reset */}
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="font-semibold text-white">Enforcement Check</h2>
                <p className="text-xs text-[var(--color-text-faint)]">Evaluate transfer authorization in real time.</p>
              </div>
              <button
                type="button"
                onClick={resetForm}
                className="text-xs text-[var(--color-text-faint)] hover:text-white flex items-center gap-1 transition-colors"
                title="Reset Form"
              >
                <RotateCcw size={12} /> Reset
              </button>
            </div>

            {/* Quick Demo Scenarios */}
            <div className="mb-4 space-y-1.5">
              <label className="block text-[11px] text-[var(--color-text-faint)] font-medium uppercase tracking-wider flex items-center gap-1">
                <Zap size={12} className="text-[var(--color-brand)]" /> Quick Presets
              </label>
              <div className="grid grid-cols-1 gap-1.5">
                {QUICK_SCENARIOS.map((sc, i) => (
                  <button
                    key={i}
                    type="button"
                    onClick={() => applyScenario(sc)}
                    className="w-full text-left px-3 py-1.5 rounded-lg bg-[var(--color-surface-2)] border border-[var(--color-border)] hover:border-[var(--color-brand)]/50 hover:bg-[var(--color-surface)] text-xs text-[var(--color-text-dim)] hover:text-white transition-colors flex items-center justify-between"
                  >
                    <span>{sc.label}</span>
                    <span
                      className={`text-[10px] px-1.5 py-0.5 rounded font-mono ${
                        sc.tone === "good"
                          ? "bg-[var(--color-good)]/15 text-[var(--color-good)]"
                          : "bg-[var(--color-bad)]/15 text-[var(--color-bad)]"
                      }`}
                    >
                      {sc.dataClass}
                    </span>
                  </button>
                ))}
              </div>
            </div>

            {/* Main Form */}
            <form onSubmit={handleCheck} className="space-y-4">
              <FormField label="Source Service *">
                <ServiceInput
                  value={form.sourceService}
                  onChange={setField("sourceService")}
                  services={services}
                  placeholder="orders-api"
                />
              </FormField>

              <FormField label="Destination Service *">
                <ServiceInput
                  value={form.destinationService}
                  onChange={setField("destinationService")}
                  services={services}
                  placeholder="payments-api"
                />
              </FormField>

              <div className="grid grid-cols-2 gap-3">
                <FormField label="Source Region">
                  <select
                    value={form.sourceRegion}
                    onChange={(e) => setField("sourceRegion")(e.target.value)}
                    className="field-input text-xs"
                  >
                    <option value="">Auto-detect</option>
                    {REGIONS.map((r) => (
                      <option key={r} value={r}>
                        {r}
                      </option>
                    ))}
                  </select>
                </FormField>
                <FormField label="Destination Region">
                  <select
                    value={form.destinationRegion}
                    onChange={(e) => setField("destinationRegion")(e.target.value)}
                    className="field-input text-xs"
                  >
                    <option value="">Auto-detect</option>
                    {REGIONS.map((r) => (
                      <option key={r} value={r}>
                        {r}
                      </option>
                    ))}
                  </select>
                </FormField>
              </div>

              <FormField label="Data Sensitivity Class *">
                <div className="flex flex-wrap gap-2">
                  {DATA_CLASSES.map((dc) => (
                    <button
                      key={dc}
                      type="button"
                      onClick={() => setField("dataClass")(dc)}
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
                <p className="text-xs text-[var(--color-bad)] bg-[var(--color-bad)]/10 rounded-lg px-3 py-2">
                  {formError}
                </p>
              )}

              <button type="submit" disabled={submitting} className="btn-primary w-full justify-center">
                {submitting ? <Loader2 size={15} className="animate-spin" /> : <Play size={15} />}
                {submitting ? "Evaluating AST Policies…" : "Run Enforcement Check"}
              </button>
            </form>
          </div>

          {/* Result Card */}
          {result && (
            <div
              className={`card p-5 border-2 animate-in fade-in zoom-in-95 ${
                result.decision === "ALLOW"
                  ? "border-[var(--color-good)]/40 bg-[var(--color-good)]/5"
                  : "border-[var(--color-bad)]/40 bg-[var(--color-bad)]/5"
              }`}
            >
              <div className="flex items-center gap-3 mb-3">
                {result.decision === "ALLOW" ? (
                  <ShieldCheck size={28} className="text-[var(--color-good)]" />
                ) : (
                  <ShieldAlert size={28} className="text-[var(--color-bad)]" />
                )}
                <div>
                  <p
                    className={`text-xl font-bold ${
                      result.decision === "ALLOW" ? "text-[var(--color-good)]" : "text-[var(--color-bad)]"
                    }`}
                  >
                    {result.decision}
                  </p>
                  <p className="text-xs text-[var(--color-text-faint)]">
                    {result._form.sourceService} [{result._form.sourceRegion || "?"}] → {result._form.destinationService} [
                    {result._form.destinationRegion || "?"}]
                  </p>
                </div>
              </div>
              <div className="space-y-2 text-sm">
                <DetailRow label="Reason" value={result.reason} />
                {result.policyId && <DetailRow label="Policy ID" value={result.policyId} />}
                {result.decisionId && <DetailRow label="Decision ID" value={`#${result.decisionId}`} />}
                {result.lineageId && <DetailRow label="Lineage Block" value={`#${result.lineageId}`} />}
                {result.lineageHash && (
                  <div className="pt-1">
                    <p className="text-[var(--color-text-faint)] text-xs mb-0.5">SHA-256 Ledger Hash</p>
                    <p className="font-mono text-[11px] text-[var(--color-text-dim)] break-all bg-[var(--color-surface)] p-2 rounded-lg border border-[var(--color-border)]">
                      {result.lineageHash}
                    </p>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Right Column: History table with Pagination */}
        <div className="xl:col-span-3">
          <div className="card overflow-hidden">
            <div className="px-5 py-4 border-b border-[var(--color-border)] flex items-center justify-between">
              <div>
                <h2 className="font-semibold text-white text-sm">Recent Decisions Audit Ledger</h2>
                <p className="text-xs text-[var(--color-text-faint)]">Live write-ahead log of runtime checks.</p>
              </div>
              <button
                onClick={loadHistory}
                className="text-[var(--color-text-faint)] hover:text-white transition-colors"
                title="Refresh history"
              >
                <Clock size={15} />
              </button>
            </div>

            {historyLoading && (
              <div className="px-5 py-12 text-center text-[var(--color-text-faint)]">
                <Loader2 size={16} className="animate-spin inline mr-2" /> Loading recent decisions...
              </div>
            )}

            {!historyLoading && history.length === 0 && (
              <div className="px-5 py-12 text-center text-[var(--color-text-faint)] text-sm">
                No runtime decisions recorded yet. Run a check on the left.
              </div>
            )}

            {!historyLoading && history.length > 0 && (
              <>
                <div className="divide-y divide-[var(--color-border)]">
                  {paginatedHistory.map((d) => (
                    <div key={d.id} className="px-5 py-3 hover:bg-[var(--color-surface-2)] transition-colors">
                      <div
                        className="flex items-center justify-between cursor-pointer"
                        onClick={() => setExpanded(expanded === d.id ? null : d.id)}
                      >
                        <div className="flex items-center gap-3 min-w-0">
                          <DecisionBadge decision={d.decision} />
                          <div className="min-w-0">
                            <p className="text-sm font-medium text-white truncate">
                              {d.sourceService} → {d.destinationService}
                            </p>
                            <p className="text-xs text-[var(--color-text-faint)]">
                              {d.dataClass} · {d.sourceRegion ?? "?"} → {d.destinationRegion ?? "?"}
                            </p>
                          </div>
                        </div>
                        <div className="flex items-center gap-3 shrink-0">
                          <span className="text-xs text-[var(--color-text-faint)] hidden sm:block font-mono">
                            {new Date(d.createdAt).toLocaleTimeString()}
                          </span>
                          {expanded === d.id ? (
                            <ChevronUp size={14} className="text-[var(--color-text-faint)]" />
                          ) : (
                            <ChevronDown size={14} className="text-[var(--color-text-faint)]" />
                          )}
                        </div>
                      </div>
                      {expanded === d.id && (
                        <div className="mt-3 bg-[var(--color-surface-2)] rounded-lg p-3 space-y-1.5 text-xs border border-[var(--color-border)]">
                          <DetailRow label="Policy" value={d.policyId || "—"} />
                          <DetailRow label="Reason" value={d.reason} />
                          <DetailRow label="Timestamp" value={new Date(d.createdAt).toLocaleString()} />
                        </div>
                      )}
                    </div>
                  ))}
                </div>

                <Pagination
                  currentPage={page}
                  totalItems={history.length}
                  pageSize={pageSize}
                  onPageChange={setPage}
                  onPageSizeChange={(sz) => {
                    setPageSize(sz);
                    setPage(1);
                  }}
                />
              </>
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
  const serviceList = services || [];
  const quickSuggestions = serviceList.slice(0, 4);

  return (
    <div className="space-y-1.5">
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
          {serviceList.map((s) => (
            <option key={s.id} value={s.id}>
              {s.name} ({s.region})
            </option>
          ))}
        </datalist>
      </div>

      {quickSuggestions.length > 0 && (
        <div className="flex flex-wrap gap-1.5 pt-0.5">
          {quickSuggestions.map((s) => (
            <button
              key={s.id}
              type="button"
              onClick={() => onChange(s.id)}
              className={`text-[11px] px-2 py-0.5 rounded-md border transition-colors ${
                value === s.id
                  ? "bg-[var(--color-brand)]/20 border-[var(--color-brand)] text-white"
                  : "bg-[var(--color-surface)] border-[var(--color-border)] text-[var(--color-text-faint)] hover:text-white hover:border-[var(--color-text-faint)]"
              }`}
            >
              {s.id}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function DecisionBadge({ decision }) {
  return (
    <span
      className={`text-xs font-bold px-2.5 py-1 rounded shrink-0 ${
        decision === "ALLOW"
          ? "bg-[var(--color-good)]/15 text-[var(--color-good)] border border-[var(--color-good)]/30"
          : "bg-[var(--color-bad)]/15 text-[var(--color-bad)] border border-[var(--color-bad)]/30"
      }`}
    >
      {decision}
    </span>
  );
}

function DetailRow({ label, value }) {
  return (
    <div className="flex gap-2 text-xs">
      <span className="text-[var(--color-text-faint)] shrink-0 w-24">{label}:</span>
      <span className="text-[var(--color-text-dim)]">{value}</span>
    </div>
  );
}
