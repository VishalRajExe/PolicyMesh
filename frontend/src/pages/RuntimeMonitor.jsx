import { useEffect, useState, useCallback } from "react";
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
  Activity,
  AlertTriangle,
  Zap,
} from "lucide-react";
import Topbar from "../components/layout/Topbar";
import Pagination from "../components/ui/Pagination";
import Button from "../components/ui/Button";
import Badge from "../components/ui/Badge";
import SearchableCombobox from "../components/ui/SearchableCombobox";
import EmptyState from "../components/ui/EmptyState";
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

export default function RuntimeMonitor() {
  const [searchParams] = useSearchParams();
  const { values: form, setValues: setForm, resetForm } = useFormDraft(
    "runtime-monitor",
    DEFAULT_FORM
  );

  const [result, setResult] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);
  const [history, setHistory] = useState([]);
  const [services, setServices] = useState([]);
  const [servicesLoading, setServicesLoading] = useState(true);
  const [servicesError, setServicesError] = useState(null);
  const [historyLoading, setHistoryLoading] = useState(true);
  const [expanded, setExpanded] = useState(null);

  // Pagination for decisions history
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const loadServices = useCallback(async () => {
    setServicesLoading(true);
    setServicesError(null);
    try {
      const data = await servicesApi.list();
      setServices(Array.isArray(data) ? data : []);
    } catch (err) {
      setServicesError(err.message || "Failed to load services");
    } finally {
      setServicesLoading(false);
    }
  }, []);

  const loadHistory = useCallback(async () => {
    setHistoryLoading(true);
    try {
      const items = await auditApi.recent(100);
      setHistory(Array.isArray(items) ? items : []);
    } catch {
      // history is optional
    } finally {
      setHistoryLoading(false);
    }
  }, []);

  useEffect(() => {
    loadServices();
    loadHistory();
  }, [loadServices, loadHistory]);

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

  // Auto-sync regions when services list loads or updates
  useEffect(() => {
    if (services.length > 0) {
      setForm((prev) => {
        let changed = false;
        const next = { ...prev };
        if (prev.sourceService) {
          const srcObj = services.find((s) => s.name === prev.sourceService || String(s.id) === prev.sourceService);
          if (srcObj && srcObj.region && prev.sourceRegion !== srcObj.region) {
            next.sourceRegion = srcObj.region;
            changed = true;
          }
        }
        if (prev.destinationService) {
          const dstObj = services.find((s) => s.name === prev.destinationService || String(s.id) === prev.destinationService);
          if (dstObj && dstObj.region && prev.destinationRegion !== dstObj.region) {
            next.destinationRegion = dstObj.region;
            changed = true;
          }
        }
        return changed ? next : prev;
      });
    }
  }, [services, setForm]);

  const setSourceService = (val, opt) => {
    setForm((prev) => {
      const next = { ...prev, sourceService: val };
      const serviceObj = opt || services.find((s) => s.name === val || String(s.id) === val);
      if (serviceObj && serviceObj.region) {
        next.sourceRegion = serviceObj.region;
      }
      return next;
    });
  };

  const setDestinationService = (val, opt) => {
    setForm((prev) => {
      const next = { ...prev, destinationService: val };
      const serviceObj = opt || services.find((s) => s.name === val || String(s.id) === val);
      if (serviceObj && serviceObj.region) {
        next.destinationRegion = serviceObj.region;
      }
      return next;
    });
  };

  const setField = (field) => (val) => {
    setForm((f) => ({ ...f, [field]: val }));
  };

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
      setFormError(err.message || "Failed to evaluate policy decision.");
    } finally {
      setSubmitting(false);
    }
  }

  const paginatedHistory = history.slice((page - 1) * pageSize, page * pageSize);

  return (
    <div>
      <Topbar
        title="Runtime Policy Monitor"
        subtitle="Execute live zero-trust policy enforcement evaluations against compiled AST rules."
      />

      <div className="px-6 lg:px-8 py-6 grid grid-cols-1 xl:grid-cols-5 gap-6 pb-12">
        {/* Left Column: Form & Result Card */}
        <div className="xl:col-span-2 space-y-4">
          <div className="card p-5">
            {/* Header */}
            <div className="flex items-center justify-between mb-4">
              <div>
                <h3 className="font-bold text-sm text-[var(--color-text)]">Enforcement Simulator</h3>
                <p className="text-xs text-[var(--color-text-dim)] mt-0.5">
                  Simulate live payload egress across zero-trust policy gates.
                </p>
              </div>
              <button
                type="button"
                onClick={resetForm}
                className="text-xs text-[var(--color-text-faint)] hover:text-[var(--color-text)] flex items-center gap-1 transition-colors"
                title="Reset Form"
              >
                <RotateCcw size={12} /> Reset
              </button>
            </div>

            {/* Form */}
            <form onSubmit={handleCheck} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
                  Source Service *
                </label>
                <SearchableCombobox
                  value={form.sourceService}
                  onChange={setSourceService}
                  options={services}
                  getOptionLabel={(s) => s.name || s.id}
                  getOptionValue={(s) => s.name || s.id}
                  placeholder="Select source service..."
                  searchPlaceholder="Search services..."
                  loading={servicesLoading}
                  error={servicesError}
                  onRetry={loadServices}
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
                  Destination Service *
                </label>
                <SearchableCombobox
                  value={form.destinationService}
                  onChange={setDestinationService}
                  options={services}
                  getOptionLabel={(s) => s.name || s.id}
                  getOptionValue={(s) => s.name || s.id}
                  placeholder="Select destination service..."
                  searchPlaceholder="Search services..."
                  loading={servicesLoading}
                  error={servicesError}
                  onRetry={loadServices}
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
                    Source Region
                  </label>
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
                </div>

                <div>
                  <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
                    Destination Region
                  </label>
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
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1.5">
                  Data Sensitivity Class *
                </label>
                <div className="flex flex-wrap gap-2">
                  {DATA_CLASSES.map((dc) => {
                    const isSelected = form.dataClass === dc;
                    return (
                      <button
                        key={dc}
                        type="button"
                        onClick={() => setField("dataClass")(dc)}
                        className={`text-xs font-medium px-2.5 py-1 rounded-lg border transition-all ${
                          isSelected
                            ? "bg-[var(--color-brand-light)] text-[var(--color-brand-text)] border-[var(--color-brand)]/40 font-semibold"
                            : "bg-[var(--color-surface-2)] text-[var(--color-text-dim)] border-[var(--color-border)] hover:text-[var(--color-text)]"
                        }`}
                      >
                        {dc}
                      </button>
                    );
                  })}
                </div>
              </div>

              {formError && (
                <div className="text-xs text-[var(--color-bad)] bg-[var(--color-bad-light)] border border-[var(--color-bad)]/30 rounded-lg p-2.5 flex items-center gap-2">
                  <AlertTriangle size={14} className="shrink-0" />
                  <span>{formError}</span>
                </div>
              )}

              <Button
                type="submit"
                variant="primary"
                size="md"
                className="w-full justify-center"
                loading={submitting}
                icon={Play}
              >
                {submitting ? "Evaluating AST Policies…" : "Run Enforcement Check"}
              </Button>
            </form>
          </div>

          {/* Evaluation Result Card */}
          {result && (
            <div
              className={`card p-5 border-l-4 animate-in fade-in zoom-in-95 ${
                result.decision === "ALLOW"
                  ? "border-l-[var(--color-good)] bg-emerald-500/5"
                  : "border-l-[var(--color-bad)] bg-rose-500/5"
              }`}
            >
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                  {result.decision === "ALLOW" ? (
                    <div className="w-8 h-8 rounded-xl bg-emerald-50 dark:bg-emerald-950/50 text-emerald-600 dark:text-emerald-400 flex items-center justify-center">
                      <ShieldCheck size={18} />
                    </div>
                  ) : (
                    <div className="w-8 h-8 rounded-xl bg-rose-50 dark:bg-rose-950/50 text-rose-600 dark:text-rose-400 flex items-center justify-center">
                      <ShieldAlert size={18} />
                    </div>
                  )}
                  <div>
                    <h4 className="font-bold text-sm text-[var(--color-text)]">
                      Decision: {result.decision}
                    </h4>
                    <p className="text-[11px] text-[var(--color-text-faint)]">
                      Evaluated in {result.evaluationLatencyMs != null ? `${result.evaluationLatencyMs}ms` : "< 1ms"}
                    </p>
                  </div>
                </div>
                <Badge variant={result.decision === "ALLOW" ? "good" : "bad"} size="sm" dot>
                  {result.decision}
                </Badge>
              </div>

              <div className="space-y-1.5 text-xs text-[var(--color-text-dim)] font-mono text-[11px]">
                <p>
                  <strong className="text-[var(--color-text)]">Policy ID:</strong> {result.policyId || "DEFAULT_GATE"}
                </p>
                <p>
                  <strong className="text-[var(--color-text)]">Reason:</strong>{" "}
                  <span className={result.decision === "DENY" ? "text-[var(--color-bad)]" : "text-[var(--color-good)]"}>
                    {result.reason || (result.decision === "ALLOW" ? "Complies with jurisdictional constraints" : "Blocked")}
                  </span>
                </p>
                {result.auditLogId && (
                  <p className="text-[10px] text-[var(--color-text-faint)] pt-1 truncate">
                    Audit Log ID: {result.auditLogId}
                  </p>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Right Column: Historical Decision Audit Log */}
        <div className="xl:col-span-3 card overflow-hidden flex flex-col justify-between">
          <div>
            <div className="p-5 border-b border-[var(--color-border)] flex items-center justify-between">
              <div>
                <h3 className="font-bold text-sm text-[var(--color-text)]">Live Decision Stream</h3>
                <p className="text-xs text-[var(--color-text-dim)] mt-0.5">
                  Immutable audit records generated by the runtime policy engine.
                </p>
              </div>
              <span className="text-xs text-[var(--color-text-faint)] font-mono">
                {history.length} events
              </span>
            </div>

            {historyLoading ? (
              <div className="p-8 text-center text-xs text-[var(--color-text-faint)] flex items-center justify-center gap-2">
                <Loader2 size={16} className="animate-spin text-[var(--color-brand)]" />
                <span>Loading runtime telemetry...</span>
              </div>
            ) : history.length === 0 ? (
              <EmptyState
                icon={Activity}
                title="No decisions logged yet"
                description="Run an enforcement check to trigger your first runtime policy evaluation."
              />
            ) : (
              <div className="divide-y divide-[var(--color-border)]">
                {paginatedHistory.map((item, idx) => {
                  const isAllow = item.decision === "ALLOW";
                  const isExp = expanded === (item.id || idx);

                  return (
                    <div key={item.id || idx} className="p-4 hover:bg-[var(--color-surface-2)]/40 transition-colors">
                      <div
                        className="flex items-center justify-between gap-3 cursor-pointer select-none"
                        onClick={() => setExpanded(isExp ? null : (item.id || idx))}
                      >
                        <div className="flex items-center gap-2.5 min-w-0">
                          {isAllow ? (
                            <ShieldCheck size={16} className="text-[var(--color-good)] shrink-0" />
                          ) : (
                            <ShieldAlert size={16} className="text-[var(--color-bad)] shrink-0" />
                          )}
                          <div className="min-w-0">
                            <div className="flex items-center gap-2 text-xs font-mono">
                              <span className="font-semibold text-[var(--color-text)] truncate">{item.sourceService}</span>
                              <span className="text-[var(--color-text-faint)]">→</span>
                              <span className="font-semibold text-[var(--color-text)] truncate">{item.destinationService}</span>
                              <span className="text-[10px] font-semibold px-1.5 py-0.2 rounded bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20">
                                {item.dataClass || "PII"}
                              </span>
                            </div>
                          </div>
                        </div>

                        <div className="flex items-center gap-2.5 shrink-0">
                          <Badge variant={isAllow ? "good" : "bad"} size="sm">
                            {item.decision}
                          </Badge>
                          <span className="text-[11px] text-[var(--color-text-faint)] hidden sm:inline">
                            {item.createdAt ? new Date(item.createdAt).toLocaleTimeString() : "just now"}
                          </span>
                          {isExp ? <ChevronUp size={14} className="text-[var(--color-text-faint)]" /> : <ChevronDown size={14} className="text-[var(--color-text-faint)]" />}
                        </div>
                      </div>

                      {/* Expanded Details */}
                      {isExp && (
                        <div className="mt-3 pt-3 border-t border-[var(--color-border)]/50 text-[11px] font-mono space-y-1 text-[var(--color-text-dim)] bg-[var(--color-surface-2)]/40 p-3 rounded-lg">
                          <p>
                            <span className="text-[var(--color-text-faint)]">Policy Code:</span> {item.policyId || "N/A"}
                          </p>
                          <p>
                            <span className="text-[var(--color-text-faint)]">Reason:</span>{" "}
                            <span className={isAllow ? "text-[var(--color-good)]" : "text-[var(--color-bad)]"}>
                              {item.reason || "Evaluated by policy engine"}
                            </span>
                          </p>
                          {item.auditHash && (
                            <p className="truncate">
                              <span className="text-[var(--color-text-faint)]">Audit Hash:</span> {item.auditHash}
                            </p>
                          )}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
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
        </div>
      </div>
    </div>
  );
}
