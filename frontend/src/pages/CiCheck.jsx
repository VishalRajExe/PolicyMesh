import { useState, useEffect, useCallback } from "react";
import {
  GitBranch,
  Play,
  ShieldCheck,
  ShieldAlert,
  Loader2,
  Hash,
  RotateCcw,
  RefreshCw,
  Info,
  CheckCircle2,
  AlertTriangle,
  ArrowRight,
  ExternalLink,
  HelpCircle,
} from "lucide-react";
import { Link } from "react-router-dom";
import Topbar from "../components/layout/Topbar";
import SearchableCombobox from "../components/ui/SearchableCombobox";
import { ciApi } from "../api";
import { useFormDraft } from "../hooks/useFormDraft";

const FALLBACK_BRANCHES = [
  "main",
  "develop",
  "staging",
  "feature/cross-border-analytics",
  "feature/eu-payments-v2",
  "fix/gdpr-residency-gate",
];

const HASH_PATTERN = /^(HEAD(~[0-9]+)?|HEAD\^?|[0-9a-fA-F]{7,40})$/;
const BRANCH_PATTERN = /^[a-zA-Z0-9][a-zA-Z0-9/_.-]*$/;

export default function CiCheck() {
  const { values: form, setValues: setForm, resetForm } = useFormDraft(
    "ci-check",
    { commitHash: "7f8a9b0", branch: "main" }
  );

  const [branches, setBranches] = useState(FALLBACK_BRANCHES);
  const [branchesLoading, setBranchesLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);

  // Restore scan history from sessionStorage
  const [history, setHistory] = useState(() => {
    try {
      const saved = sessionStorage.getItem("policymesh:ci-check:history");
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  const loadBranches = useCallback(async () => {
    setBranchesLoading(true);
    try {
      const data = await ciApi.listBranches();
      if (Array.isArray(data) && data.length > 0) {
        setBranches(data);
      }
    } catch {
      // Fallback to initial defaults
    } finally {
      setBranchesLoading(false);
    }
  }, []);

  useEffect(() => {
    loadBranches();
  }, [loadBranches]);

  useEffect(() => {
    try {
      sessionStorage.setItem("policymesh:ci-check:history", JSON.stringify(history));
    } catch {}
  }, [history]);

  // Validation helpers
  const isHashEmpty = !form.commitHash || !form.commitHash.trim();
  const isHashInvalid = !isHashEmpty && !HASH_PATTERN.test(form.commitHash.trim());
  const isBranchEmpty = !form.branch || !form.branch.trim();
  const isBranchInvalid = !isBranchEmpty && !BRANCH_PATTERN.test(form.branch.trim());

  async function handleRun(e) {
    e.preventDefault();
    const hash = form.commitHash.trim();
    const branch = form.branch.trim();

    if (!branch) {
      setFormError("Git Branch is required. Select an existing branch or type a custom branch name.");
      return;
    }

    if (!BRANCH_PATTERN.test(branch)) {
      setFormError("Invalid Branch Name. Must start with an alphanumeric character and contain only letters, numbers, hyphens, slashes, or underscores.");
      return;
    }

    if (!hash) {
      setFormError("Commit SHA-1 Hash is required. Enter a 7-40 character hexadecimal hash or click 'HEAD'.");
      return;
    }

    if (!HASH_PATTERN.test(hash)) {
      setFormError("Invalid Commit SHA-1 Hash. Must be a 7 to 40 character hexadecimal hash (0-9, a-f, A-F) or 'HEAD'.");
      return;
    }

    setFormError(null);
    setResult(null);
    setSubmitting(true);
    try {
      const resp = await ciApi.runCheck({ commitHash: hash, branch });
      setResult(resp);
      setHistory((prev) => [resp, ...prev.filter((h) => h.id !== resp.id).slice(0, 9)]);
    } catch (err) {
      setFormError(err.message || "Failed to execute CI compliance scan.");
    } finally {
      setSubmitting(false);
    }
  }

  function handleSetHash(val) {
    setForm((f) => ({ ...f, commitHash: val }));
    setFormError(null);
  }

  return (
    <div>
      <Topbar
        title="CI Compliance Check"
        subtitle="Pre-merge gate scanning data flow topologies against compiled policy ASTs."
      />

      <div className="px-6 lg:px-8 mt-6 grid grid-cols-1 xl:grid-cols-5 gap-6 pb-12">
        {/* Form Column */}
        <div className="xl:col-span-2 space-y-4">
          <div className="card p-5 space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <GitBranch size={17} className="text-[var(--color-brand)]" />
                <h2 className="font-semibold text-white text-sm">Run Compliance Check</h2>
              </div>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={loadBranches}
                  disabled={branchesLoading}
                  className="text-xs text-[var(--color-text-faint)] hover:text-white flex items-center gap-1 transition-colors"
                  title="Refresh Git branches"
                >
                  <RefreshCw size={11} className={branchesLoading ? "animate-spin" : ""} />
                </button>
                <button
                  type="button"
                  onClick={resetForm}
                  className="text-xs text-[var(--color-text-faint)] hover:text-white flex items-center gap-1 transition-colors"
                  title="Reset Form"
                >
                  <RotateCcw size={11} /> Reset
                </button>
              </div>
            </div>

            <form onSubmit={handleRun} className="space-y-4 pt-1">
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="block text-xs text-[var(--color-text-dim)]">
                    Git Branch * <span className="text-[var(--color-text-faint)]">(select or type custom)</span>
                  </label>
                  <span className="text-[10px] text-[var(--color-text-faint)] font-mono">
                    {branches.length} branches discovered
                  </span>
                </div>
                <SearchableCombobox
                  value={form.branch}
                  onChange={(val) => {
                    setForm((f) => ({ ...f, branch: val }));
                    setFormError(null);
                  }}
                  options={branches}
                  placeholder="Select branch or enter custom..."
                  searchPlaceholder="Search branch or type custom name..."
                  allowCustom={true}
                  renderOption={(b, { isSelected, isHighlighted }) => (
                    <div className="px-3 py-2 rounded-lg flex items-center justify-between gap-2 text-xs">
                      <div className="flex items-center gap-2 min-w-0">
                        <GitBranch size={13} className="text-[var(--color-text-faint)] shrink-0" />
                        <span className="font-mono text-white truncate">{b}</span>
                      </div>
                      {isSelected && <CheckCircle2 size={13} className="text-[var(--color-brand)] shrink-0" />}
                    </div>
                  )}
                />
                {isBranchEmpty && (
                  <p className="text-[11px] text-amber-400 mt-1 flex items-center gap-1">
                    <AlertTriangle size={11} /> Please select or enter a Git branch name.
                  </p>
                )}
                {isBranchInvalid && (
                  <p className="text-[11px] text-[var(--color-bad)] mt-1 flex items-center gap-1">
                    <AlertTriangle size={11} /> Branch name must start with an alphanumeric character and contain no spaces.
                  </p>
                )}
              </div>

              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="block text-xs text-[var(--color-text-dim)]">Commit SHA-1 Hash *</label>
                  <div className="flex items-center gap-1.5">
                    <button
                      type="button"
                      onClick={() => handleSetHash("HEAD")}
                      className="text-[10px] px-1.5 py-0.5 rounded bg-[var(--color-surface-2)] border border-[var(--color-border)] hover:border-[var(--color-brand)] text-[var(--color-text-dim)] hover:text-white font-mono transition-colors"
                    >
                      HEAD
                    </button>
                    <button
                      type="button"
                      onClick={() => handleSetHash("7f8a9b0")}
                      className="text-[10px] px-1.5 py-0.5 rounded bg-[var(--color-surface-2)] border border-[var(--color-border)] hover:border-[var(--color-brand)] text-[var(--color-text-dim)] hover:text-white font-mono transition-colors"
                    >
                      Demo SHA
                    </button>
                  </div>
                </div>

                <div className="relative">
                  <Hash size={13} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--color-text-faint)]" />
                  <input
                    value={form.commitHash}
                    onChange={(e) => {
                      setForm((f) => ({ ...f, commitHash: e.target.value }));
                      setFormError(null);
                    }}
                    placeholder="e.g. 7f8a9b0 or HEAD"
                    className={`field-input pl-8 text-xs font-mono ${
                      isHashInvalid ? "border-[var(--color-bad)] ring-1 ring-[var(--color-bad)]" : ""
                    }`}
                    required
                  />
                </div>

                {isHashInvalid && (
                  <p className="text-[11px] text-[var(--color-bad)] mt-1 flex items-center gap-1">
                    <AlertTriangle size={11} /> Invalid SHA-1 format. Enter a 7–40 character hex hash (0-9, a-f) or "HEAD".
                  </p>
                )}
              </div>

              {formError && (
                <div className="text-xs text-[var(--color-bad)] bg-[var(--color-bad)]/10 border border-[var(--color-bad)]/30 rounded-lg p-2.5 flex items-start gap-2">
                  <AlertTriangle size={14} className="shrink-0 mt-0.5" />
                  <div>
                    <p className="font-semibold">Input Guidance</p>
                    <p className="mt-0.5">{formError}</p>
                  </div>
                </div>
              )}

              <button
                type="submit"
                disabled={submitting || isHashInvalid || isBranchEmpty || isHashEmpty || isBranchInvalid}
                className="btn-primary w-full justify-center text-xs"
              >
                {submitting ? <Loader2 size={15} className="animate-spin" /> : <Play size={15} />}
                {submitting ? "Analyzing Topology Graph…" : "Execute CI Check"}
              </button>
            </form>

            {/* Input Guidance Footer */}
            <div className="bg-[var(--color-surface-2)]/60 rounded-xl p-3 text-xs text-[var(--color-text-faint)] space-y-1.5 border border-[var(--color-border)]/40">
              <p className="font-semibold text-white flex items-center gap-1.5">
                <HelpCircle size={13} className="text-[var(--color-brand)]" /> How CI Compliance Gate Works
              </p>
              <p className="text-[11px] leading-relaxed">
                PolicyMesh simulates pull-request validation by scanning all registered data flow topologies against active
                policy ASTs. If any cross-border edge violates jurisdiction rules, the gate returns <strong>FAIL</strong> with
                exit code <code>1</code>.
              </p>
            </div>
          </div>

          {/* Past Scans in this session */}
          {history.length > 0 && (
            <div className="card overflow-hidden">
              <div className="px-4 py-3 border-b border-[var(--color-border)] flex items-center justify-between">
                <p className="text-xs font-semibold text-white">Scans (this session)</p>
                <button
                  onClick={() => {
                    setHistory([]);
                    sessionStorage.removeItem("policymesh:ci-check:history");
                  }}
                  className="text-[11px] text-[var(--color-text-faint)] hover:text-white"
                >
                  Clear
                </button>
              </div>
              <div className="divide-y divide-[var(--color-border)]">
                {history.map((h, i) => (
                  <button
                    key={h.id || i}
                    className="w-full text-left px-4 py-3 hover:bg-[var(--color-surface-2)] transition-colors"
                    onClick={() => setResult(h)}
                  >
                    <div className="flex items-center justify-between gap-2">
                      <div className="flex items-center gap-2 min-w-0">
                        {h.result === "PASS" ? (
                          <ShieldCheck size={14} className="text-[var(--color-good)] shrink-0" />
                        ) : (
                          <ShieldAlert size={14} className="text-[var(--color-bad)] shrink-0" />
                        )}
                        <span className="text-xs font-medium text-white truncate font-mono">{h.branch}</span>
                        <span className="text-xs text-[var(--color-text-faint)] font-mono shrink-0">
                          @{h.commitHash?.slice(0, 7)}
                        </span>
                      </div>
                      <span
                        className={`text-[10px] font-bold px-1.5 py-0.5 rounded font-mono ${
                          h.result === "PASS"
                            ? "bg-[var(--color-good)]/15 text-[var(--color-good)]"
                            : "bg-[var(--color-bad)]/15 text-[var(--color-bad)]"
                        }`}
                      >
                        {h.result}
                      </span>
                    </div>
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Results Column */}
        <div className="xl:col-span-3">
          {!result && !submitting && (
            <div className="card px-5 py-16 text-center">
              <GitBranch size={36} className="mx-auto mb-3 text-[var(--color-text-faint)] opacity-60" />
              <h3 className="text-base font-semibold text-white mb-1">Pre-Merge Compliance Gate</h3>
              <p className="text-[var(--color-text-faint)] text-xs max-w-sm mx-auto">
                Select a Git branch and enter a commit SHA to evaluate topology graph compliance against all compiled AST rules.
              </p>
            </div>
          )}

          {submitting && (
            <div className="card px-5 py-16 text-center">
              <Loader2 size={32} className="animate-spin mx-auto mb-3 text-[var(--color-brand)]" />
              <h3 className="text-sm font-semibold text-white mb-1">Evaluating Topological Flow Graph…</h3>
              <p className="text-[var(--color-text-faint)] text-xs">
                Compiling AST rules and validating cross-border service boundaries.
              </p>
            </div>
          )}

          {result && !submitting && (
            <div className="space-y-4 animate-in fade-in zoom-in-95">
              {/* Main Banner */}
              <div
                className={`card p-5 border-2 ${
                  result.result === "PASS"
                    ? "border-[var(--color-good)]/40 bg-[var(--color-good)]/5"
                    : "border-[var(--color-bad)]/40 bg-[var(--color-bad)]/5"
                }`}
              >
                <div className="flex items-center gap-3.5 mb-3">
                  {result.result === "PASS" ? (
                    <ShieldCheck size={36} className="text-[var(--color-good)]" />
                  ) : (
                    <ShieldAlert size={36} className="text-[var(--color-bad)]" />
                  )}
                  <div>
                    <p
                      className={`text-2xl font-bold ${
                        result.result === "PASS" ? "text-[var(--color-good)]" : "text-[var(--color-bad)]"
                      }`}
                    >
                      {result.result === "PASS" ? "Compliance Passed" : "Compliance Failed"}
                    </p>
                    <p className="text-xs text-[var(--color-text-faint)]">
                      Branch <strong className="text-white font-mono">{result.branch}</strong> @{" "}
                      <span className="font-mono text-white">{result.commitHash}</span>
                    </p>
                  </div>
                </div>
                <p className="text-xs text-[var(--color-text-dim)] bg-[var(--color-surface)] p-2.5 rounded-lg border border-[var(--color-border)] font-mono">
                  {result.humanReadable}
                </p>
              </div>

              {/* Violations List */}
              {result.violations && result.violations.length > 0 && (
                <div className="card overflow-hidden">
                  <div className="px-5 py-3 border-b border-[var(--color-border)] flex items-center justify-between">
                    <p className="text-xs font-semibold text-[var(--color-bad)] flex items-center gap-2">
                      <ShieldAlert size={15} />
                      {result.violationCount} Policy Violation{result.violationCount !== 1 ? "s" : ""} Detected
                    </p>
                    <span className="text-[11px] text-[var(--color-text-faint)]">PR Blocked from Merge</span>
                  </div>
                  <div className="divide-y divide-[var(--color-border)]">
                    {result.violations.map((v, i) => (
                      <div key={i} className="px-5 py-4 space-y-2 hover:bg-[var(--color-surface-2)] transition-colors">
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="font-semibold text-white text-xs font-mono">{v.sourceService}</span>
                          <span className="text-[10px] px-1.5 py-0.5 rounded bg-blue-500/10 text-blue-400 font-mono">
                            {v.sourceRegion}
                          </span>
                          <ArrowRight size={13} className="text-[var(--color-text-faint)]" />
                          <span className="font-semibold text-white text-xs font-mono">{v.destinationService}</span>
                          <span className="text-[10px] px-1.5 py-0.5 rounded bg-blue-500/10 text-blue-400 font-mono">
                            {v.destinationRegion}
                          </span>
                          <span className="text-[10px] font-semibold px-2 py-0.5 rounded bg-[var(--color-bad)]/15 text-[var(--color-bad)] border border-[var(--color-bad)]/30 font-mono">
                            {v.dataClass}
                          </span>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-2 text-xs pt-1">
                          <div className="bg-[var(--color-surface-2)]/60 p-2.5 rounded-lg border border-[var(--color-border)]">
                            <span className="text-[var(--color-text-faint)] text-[11px] block">Triggered Policy Rule:</span>
                            <span className="font-semibold text-white font-mono text-xs">{v.policyCode || "N/A"}</span>
                          </div>
                          <div className="bg-[var(--color-surface-2)]/60 p-2.5 rounded-lg border border-[var(--color-border)]">
                            <span className="text-[var(--color-text-faint)] text-[11px] block">Jurisdiction Reason:</span>
                            <span className="text-[var(--color-bad)] text-xs font-medium">{v.reason}</span>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Remediation Guidance */}
              {result.violations && result.violations.length > 0 && (
                <div className="card p-5 space-y-3 bg-[var(--color-surface)] border-amber-500/30">
                  <div className="flex items-center gap-2 text-amber-400">
                    <Info size={16} />
                    <h4 className="text-xs font-semibold uppercase tracking-wider">How to Fix & Remediate This Violation</h4>
                  </div>
                  <p className="text-xs text-[var(--color-text-dim)]">
                    This check failed because a data pipeline routes sensitive data across disallowed jurisdictional boundaries.
                    To resolve this and pass the CI gate, choose one of the following remediation paths:
                  </p>
                  <div className="space-y-2 pt-1">
                    <div className="flex items-start gap-2.5 text-xs text-[var(--color-text-dim)]">
                      <span className="w-5 h-5 rounded-full bg-amber-500/20 text-amber-300 font-bold flex items-center justify-center shrink-0 text-[11px]">
                        1
                      </span>
                      <div>
                        <strong className="text-white">Reroute or Remove Disallowed Flow:</strong> Go to the{" "}
                        <Link to="/data-flows" className="text-[var(--color-brand)] underline hover:text-white">
                          Data Flows Canvas
                        </Link>{" "}
                        and delete or reconfigure the cross-border edge.
                      </div>
                    </div>
                    <div className="flex items-start gap-2.5 text-xs text-[var(--color-text-dim)]">
                      <span className="w-5 h-5 rounded-full bg-amber-500/20 text-amber-300 font-bold flex items-center justify-center shrink-0 text-[11px]">
                        2
                      </span>
                      <div>
                        <strong className="text-white">Mask or Tokenize Sensitive Data:</strong> Downgrade data class sensitivity
                        if payload is anonymized before leaving the source jurisdiction.
                      </div>
                    </div>
                    <div className="flex items-start gap-2.5 text-xs text-[var(--color-text-dim)]">
                      <span className="w-5 h-5 rounded-full bg-amber-500/20 text-amber-300 font-bold flex items-center justify-center shrink-0 text-[11px]">
                        3
                      </span>
                      <div>
                        <strong className="text-white">Update Governance Policy:</strong> If this transfer is legally authorized
                        under Standard Contractual Clauses (SCCs), update the allowed regions in{" "}
                        <Link to="/policies" className="text-[var(--color-brand)] underline hover:text-white">
                          Policies Manager
                        </Link>
                        .
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* Passed Guidance */}
              {result.result === "PASS" && (
                <div className="card p-5 space-y-3 bg-[var(--color-good)]/5 border-[var(--color-good)]/30">
                  <div className="flex items-center gap-2 text-[var(--color-good)]">
                    <CheckCircle2 size={16} />
                    <h4 className="text-xs font-semibold uppercase tracking-wider">What Went Right & Verification Summary</h4>
                  </div>
                  <ul className="text-xs text-[var(--color-text-dim)] space-y-1.5 list-disc list-inside">
                    <li>All cross-service data pipelines on branch <strong className="text-white font-mono">{result.branch}</strong> strictly comply with active jurisdictional rules.</li>
                    <li>Zero unauthorized cross-border data residency leaks detected across all compiled policy ASTs.</li>
                    <li>Pull Request is <strong>Safe to Merge</strong> into production.</li>
                  </ul>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
