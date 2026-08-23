import { useState, useEffect, useCallback } from "react";
import {
  GitBranch,
  Play,
  ShieldCheck,
  ShieldAlert,
  Loader2,
  RotateCcw,
  RefreshCw,
  CheckCircle2,
  AlertTriangle,
  ArrowRight,
  ExternalLink,
  Code,
  Check,
} from "lucide-react";
import { Link } from "react-router-dom";
import Topbar from "../components/layout/Topbar";
import Button from "../components/ui/Button";
import Badge from "../components/ui/Badge";
import EmptyState from "../components/ui/EmptyState";
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

const HASH_PATTERN = /^(HEAD(~[0-9]+)?|HEAD\^?|[0-9a-fA-F]{3,40})$/;
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
      // Fallback
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
        title="CI/CD Compliance Guard"
        subtitle="Shift-left security scanner evaluating pull requests and commits against policy ASTs before merge."
      />

      <div className="px-6 lg:px-8 py-6 grid grid-cols-1 xl:grid-cols-5 gap-6 pb-12">
        {/* Left Column: Form Card */}
        <div className="xl:col-span-2 space-y-4">
          <div className="card p-5">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h3 className="font-bold text-sm text-[var(--color-text)]">Pipeline Scanner Trigger</h3>
                <p className="text-xs text-[var(--color-text-dim)] mt-0.5">
                  Test proposed topology changes against all policies.
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

            <form onSubmit={handleRun} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-[var(--color-text-dim)] mb-1">
                  Target Git Branch *
                </label>
                <SearchableCombobox
                  value={form.branch}
                  onChange={(val) => {
                    setForm((f) => ({ ...f, branch: val }));
                    setFormError(null);
                  }}
                  options={branches.map((b) => ({ id: b, name: b }))}
                  getOptionLabel={(b) => b.name}
                  getOptionValue={(b) => b.name}
                  placeholder="Select or type branch..."
                  searchPlaceholder="Search branch or type new..."
                  loading={branchesLoading}
                  onRetry={loadBranches}
                />
              </div>

              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="block text-xs font-medium text-[var(--color-text-dim)]">
                    Commit SHA-1 Hash *
                  </label>
                  <div className="flex items-center gap-1.5">
                    <button
                      type="button"
                      onClick={() => handleSetHash("HEAD")}
                      className="text-[10px] font-mono px-1.5 py-0.2 rounded bg-[var(--color-surface-2)] text-[var(--color-text-dim)] hover:text-[var(--color-text)] border border-[var(--color-border)]"
                    >
                      HEAD
                    </button>
                    <button
                      type="button"
                      onClick={() => handleSetHash("HEAD~1")}
                      className="text-[10px] font-mono px-1.5 py-0.2 rounded bg-[var(--color-surface-2)] text-[var(--color-text-dim)] hover:text-[var(--color-text)] border border-[var(--color-border)]"
                    >
                      HEAD~1
                    </button>
                    <button
                      type="button"
                      onClick={() => handleSetHash("7f8a9b0")}
                      className="text-[10px] font-mono px-1.5 py-0.2 rounded bg-[var(--color-surface-2)] text-[var(--color-text-dim)] hover:text-[var(--color-text)] border border-[var(--color-border)]"
                    >
                      7f8a9b0
                    </button>
                  </div>
                </div>

                <input
                  value={form.commitHash}
                  onChange={(e) => handleSetHash(e.target.value)}
                  placeholder="e.g. 7f8a9b0 or 40-char SHA"
                  className={`field-input text-xs font-mono ${
                    isHashInvalid ? "border-[var(--color-bad)] ring-1 ring-[var(--color-bad)]" : ""
                  }`}
                  required
                />

                {isHashInvalid && (
                  <p className="text-[11px] text-[var(--color-bad)] mt-1">
                    Please enter a valid 7–40 char hex SHA (0-9, a-f) or "HEAD".
                  </p>
                )}
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
                disabled={isHashInvalid || isBranchInvalid}
                icon={Play}
              >
                {submitting ? "Scanning Policy ASTs…" : "Execute CI Check"}
              </Button>
            </form>
          </div>
        </div>

        {/* Right Column: CI Result / Report */}
        <div className="xl:col-span-3 space-y-4">
          {result ? (
            <div className="card p-5 space-y-4 animate-in fade-in zoom-in-95">
              {/* Scan Status Header */}
              <div
                className={`p-4 rounded-xl border flex items-center justify-between ${
                  result.passed
                    ? "bg-emerald-50 dark:bg-emerald-950/40 border-emerald-200 dark:border-emerald-800/40"
                    : "bg-rose-50 dark:bg-rose-950/40 border-rose-200 dark:border-rose-800/40"
                }`}
              >
                <div className="flex items-center gap-3">
                  {result.passed ? (
                    <div className="w-10 h-10 rounded-xl bg-emerald-100 dark:bg-emerald-900/60 text-emerald-600 dark:text-emerald-300 flex items-center justify-center">
                      <ShieldCheck size={22} />
                    </div>
                  ) : (
                    <div className="w-10 h-10 rounded-xl bg-rose-100 dark:bg-rose-900/60 text-rose-600 dark:text-rose-300 flex items-center justify-center">
                      <ShieldAlert size={22} />
                    </div>
                  )}
                  <div>
                    <h3 className="font-bold text-sm text-[var(--color-text)]">
                      {result.passed ? "CI Compliance Gate: PASSED" : "CI Compliance Gate: BLOCKED"}
                    </h3>
                    <p className="text-xs text-[var(--color-text-dim)] font-mono mt-0.5">
                      Branch: <strong>{result.branch}</strong> • Commit:{" "}
                      <strong>{result.commitHash?.slice(0, 7)}</strong>
                    </p>
                  </div>
                </div>
                <Badge variant={result.passed ? "good" : "bad"} dot size="md">
                  {result.passed ? "MERGE ALLOWED" : "MERGE BLOCKED"}
                </Badge>
              </div>

              {/* Violations List */}
              {result.violations && result.violations.length > 0 ? (
                <div className="space-y-2">
                  <h4 className="font-semibold text-xs text-[var(--color-text)]">
                    Detected Policy Violations ({result.violations.length})
                  </h4>
                  <div className="divide-y divide-[var(--color-border)]/50 border border-[var(--color-border)] rounded-xl overflow-hidden bg-[var(--color-surface-2)]/30">
                    {result.violations.map((v, idx) => (
                      <div key={idx} className="p-3 text-xs flex items-start gap-2.5">
                        <AlertTriangle size={14} className="text-[var(--color-bad)] shrink-0 mt-0.5" />
                        <div className="min-w-0 flex-1">
                          <p className="font-semibold text-[var(--color-text)]">
                            {v.sourceService} → {v.destinationService}
                          </p>
                          <p className="text-[11px] text-[var(--color-bad)] mt-0.5 font-mono">
                            {v.reason || `Blocked by policy ${v.policyCode || ""}`}
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : (
                <div className="p-4 rounded-xl bg-[var(--color-surface-2)]/60 text-center text-xs text-[var(--color-text-dim)]">
                  <CheckCircle2 size={16} className="text-[var(--color-good)] mx-auto mb-1" />
                  All proposed service flows comply with active zero-trust residency rules.
                </div>
              )}
            </div>
          ) : (
            <div className="card p-8 text-center">
              <GitBranch size={24} className="text-[var(--color-text-faint)] mx-auto mb-2" />
              <h4 className="font-semibold text-sm text-[var(--color-text)]">No scan executed yet</h4>
              <p className="text-xs text-[var(--color-text-dim)] max-w-sm mx-auto mt-1">
                Select a target branch and commit SHA to execute pre-merge compliance validation.
              </p>
            </div>
          )}

          {/* Historical Scans */}
          {history.length > 0 && (
            <div className="card p-5">
              <h4 className="font-bold text-xs text-[var(--color-text)] mb-3">Recent Pipeline Scans</h4>
              <div className="divide-y divide-[var(--color-border)]/50">
                {history.map((h, idx) => (
                  <div
                    key={h.id || idx}
                    className="py-2.5 flex items-center justify-between gap-3 text-xs font-mono"
                  >
                    <div className="flex items-center gap-2 min-w-0">
                      <span className="font-semibold text-[var(--color-text)] truncate">{h.branch}</span>
                      <span className="text-[var(--color-text-faint)]">@</span>
                      <span className="text-[var(--color-text-dim)]">{h.commitHash?.slice(0, 7)}</span>
                    </div>
                    <Badge variant={h.passed ? "good" : "bad"} size="sm">
                      {h.passed ? "PASS" : "FAIL"}
                    </Badge>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
