import { useEffect, useState } from "react";
import {
  GitBranch,
  Play,
  ShieldCheck,
  ShieldAlert,
  AlertTriangle,
  FileCode,
  FileText,
  RotateCcw,
  CheckCircle2,
  XCircle,
  HelpCircle,
  ExternalLink,
  ChevronRight,
  ArrowRight,
  ArrowDown,
  Layers,
  User,
  Clock,
  GitCommit,
  Sparkles,
  Info,
  Check,
  Loader2,
  GitPullRequest,
  CheckSquare,
  Server,
  Box,
  SkipForward,
} from "lucide-react";
import Topbar from "../components/layout/Topbar";
import SearchableCombobox from "../components/ui/SearchableCombobox";
import Badge from "../components/ui/Badge";
import Button from "../components/ui/Button";
import Modal from "../components/ui/Modal";
import Pagination from "../components/ui/Pagination";
import { ciApi } from "../api/ci";
import { useFormDraft } from "../hooks/useFormDraft";

const HASH_PATTERN = /^(HEAD(~[0-9]+)?|HEAD\^?|[0-9a-fA-F]{3,40})$/;
const BRANCH_PATTERN = /^[a-zA-Z0-9][a-zA-Z0-9/_.-]*$/;

const SCAN_STEPS = [
  { id: 1, label: "Validating branch reference" },
  { id: 2, label: "Verifying commit existence & reachability" },
  { id: 3, label: "Retrieving commit metadata & diffs" },
  { id: 4, label: "Fetching live GitHub Actions workflow runs" },
  { id: 5, label: "Evaluating zero-trust policy ASTs" },
];

export default function CiCheck() {
  const {
    values: form,
    setValues: setForm,
    resetForm: resetDraft,
  } = useFormDraft("ci_check", {
    branch: "main",
    commitHash: "HEAD",
  });

  const [branches, setBranches] = useState(["main", "develop", "staging", "demo/policymesh-ci-failure"]);
  const [branchesLoading, setBranchesLoading] = useState(false);

  const [submitting, setSubmitting] = useState(false);
  const [scanStep, setScanStep] = useState(0);
  const [result, setResult] = useState(null);
  const [invalidError, setInvalidError] = useState(null);
  const [formError, setFormError] = useState(null);

  // History & Details Modal
  const [history, setHistory] = useState([]);
  const [historyPage, setHistoryPage] = useState(1);
  const [historyTotal, setHistoryTotal] = useState(0);
  const [selectedScan, setSelectedScan] = useState(null);

  async function loadBranches() {
    setBranchesLoading(true);
    try {
      const data = await ciApi.listBranches();
      if (Array.isArray(data) && data.length > 0) {
        setBranches(data);
      }
    } catch {
      // Keep defaults
    } finally {
      setBranchesLoading(false);
    }
  }

  async function loadHistory(page = 1) {
    try {
      const data = await ciApi.listScans(page - 1, 6);
      if (data && data.content) {
        setHistory(data.content);
        setHistoryTotal(data.totalElements || data.content.length);
      } else if (Array.isArray(data)) {
        setHistory(data);
        setHistoryTotal(data.length);
      }
    } catch {
      // Ignored
    }
  }

  useEffect(() => {
    loadBranches();
    loadHistory(1);
  }, []);

  function resetForm() {
    setForm({ branch: "main", commitHash: "HEAD" });
    setFormError(null);
    setInvalidError(null);
    setResult(null);
    resetDraft();
  }

  const isHashInvalid = form.commitHash && !HASH_PATTERN.test(form.commitHash);
  const isBranchInvalid = form.branch && !BRANCH_PATTERN.test(form.branch);

  async function handleRun(e) {
    e.preventDefault();
    const branch = form.branch?.trim();
    const hash = form.commitHash?.trim();

    if (!branch) {
      setFormError("Target Git branch is required.");
      return;
    }
    if (!BRANCH_PATTERN.test(branch)) {
      setFormError("Invalid Branch Name. Must start with an alphanumeric character.");
      return;
    }
    if (!hash) {
      setFormError("Commit SHA-1 Hash is required. Enter a hash or click 'HEAD'.");
      return;
    }
    if (!HASH_PATTERN.test(hash)) {
      setFormError("Invalid Commit SHA-1 Hash. Must be 3-40 hex characters or 'HEAD'.");
      return;
    }

    setFormError(null);
    setInvalidError(null);
    setResult(null);
    setSubmitting(true);
    setScanStep(1);

    const stepTimer1 = setTimeout(() => setScanStep(2), 250);
    const stepTimer2 = setTimeout(() => setScanStep(3), 500);
    const stepTimer3 = setTimeout(() => setScanStep(4), 750);
    const stepTimer4 = setTimeout(() => setScanStep(5), 1000);

    try {
      const resp = await ciApi.runCheck({ commitHash: hash, branch });
      setResult(resp);
      loadHistory(1);
    } catch (err) {
      const detail = err.response?.data?.detail || err.message || "Commit verification failed.";
      const errorCode = err.response?.data?.errorCode || "INVALID_COMMIT";
      setInvalidError({
        errorCode,
        message: detail,
        branch,
        commitHash: hash,
      });
    } finally {
      clearTimeout(stepTimer1);
      clearTimeout(stepTimer2);
      clearTimeout(stepTimer3);
      clearTimeout(stepTimer4);
      setSubmitting(false);
      setScanStep(0);
    }
  }

  function handleSetHash(val) {
    setForm((f) => ({ ...f, commitHash: val }));
    setFormError(null);
    setInvalidError(null);
  }

  return (
    <div>
      <Topbar
        title="CI/CD Compliance Guard"
        subtitle="Shift-left security scanner evaluating pull requests and commits against policy ASTs before merge."
      />

      <div className="px-6 lg:px-8 py-6 grid grid-cols-1 xl:grid-cols-5 gap-6 pb-12">
        {/* Left Column: Form Trigger & Recent Scans */}
        <div className="xl:col-span-2 space-y-4">
          <div className="card p-5">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h3 className="font-bold text-sm text-[var(--color-text)]">Pipeline Scanner Trigger</h3>
                <p className="text-xs text-[var(--color-text-dim)] mt-0.5">
                  Test proposed topology changes against active policies.
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
                    setInvalidError(null);
                  }}
                  options={branches.map((b) => ({ id: b, name: b }))}
                  getOptionLabel={(b) => b.name}
                  getOptionValue={(b) => b.name}
                  placeholder="Select or type branch..."
                  searchPlaceholder="Search branch..."
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
                      className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-[var(--color-surface-2)] text-[var(--color-text-dim)] hover:text-[var(--color-text)] border border-[var(--color-border)]"
                    >
                      HEAD
                    </button>
                    <button
                      type="button"
                      onClick={() => handleSetHash("HEAD~1")}
                      className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-[var(--color-surface-2)] text-[var(--color-text-dim)] hover:text-[var(--color-text)] border border-[var(--color-border)]"
                    >
                      HEAD~1
                    </button>
                    <button
                      type="button"
                      onClick={() => handleSetHash("c82a4c0")}
                      className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-[var(--color-surface-2)] text-[var(--color-text-dim)] hover:text-[var(--color-text)] border border-[var(--color-border)]"
                    >
                      c82a4c0
                    </button>
                    <button
                      type="button"
                      onClick={() => handleSetHash("f73a470")}
                      className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-[var(--color-surface-2)] text-[var(--color-text-dim)] hover:text-[var(--color-text)] border border-[var(--color-border)]"
                    >
                      f73a470
                    </button>
                  </div>
                </div>

                <input
                  value={form.commitHash}
                  onChange={(e) => handleSetHash(e.target.value)}
                  placeholder="e.g. HEAD, c82a4c0, f73a470"
                  className={`field-input text-xs font-mono ${
                    isHashInvalid ? "border-[var(--color-bad)] ring-1 ring-[var(--color-bad)]" : ""
                  }`}
                  required
                />

                {isHashInvalid && (
                  <p className="text-[11px] text-[var(--color-bad)] mt-1">
                    Please enter a valid hexadecimal SHA (3–40 chars) or "HEAD".
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
                disabled={isHashInvalid || isBranchInvalid || submitting}
                icon={Play}
              >
                {submitting ? "Analyzing Commit..." : "Execute CI Check"}
              </Button>
            </form>
          </div>

          {/* Historical Scans */}
          <div className="card p-5">
            <div className="flex items-center justify-between mb-3">
              <h4 className="font-bold text-xs text-[var(--color-text)]">Recent Pipeline Scans</h4>
              <span className="text-[10px] text-[var(--color-text-faint)] font-mono">
                {historyTotal} total
              </span>
            </div>

            {history.length === 0 ? (
              <p className="text-xs text-[var(--color-text-faint)] py-4 text-center">
                No recent scans recorded.
              </p>
            ) : (
              <div className="divide-y divide-[var(--color-border)]/50">
                {history.map((h, idx) => {
                  const hPassed =
                    h.finalDecision?.allowed === true ||
                    (h.finalDecision?.allowed === undefined &&
                      (h.passed === true || h.result === "PASS" || h.status === "PASSED" || h.status === "PASS") &&
                      (h.violationCount === 0 && (!h.violations || h.violations.length === 0)));

                  return (
                    <button
                      key={h.id || idx}
                      type="button"
                      onClick={() => setSelectedScan(h)}
                      className="w-full text-left py-2.5 flex items-center justify-between gap-3 text-xs hover:bg-[var(--color-surface-2)]/60 px-2 rounded-lg transition-colors"
                    >
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2">
                          <span className="font-semibold text-[var(--color-text)] truncate">{h.branch}</span>
                          <span className="text-[var(--color-text-faint)] font-mono">@</span>
                          <span className="text-[var(--color-text-dim)] font-mono">{h.commitShortSha || h.commitHash?.slice(0, 7)}</span>
                        </div>
                        {h.commitMessage && (
                          <p className="text-[11px] text-[var(--color-text-faint)] truncate mt-0.5">
                            {h.commitMessage}
                          </p>
                        )}
                      </div>
                      <Badge variant={hPassed ? "good" : "bad"} size="sm">
                        {hPassed ? "PASS" : "BLOCKED"}
                      </Badge>
                    </button>
                  );
                })}
              </div>
            )}

            {historyTotal > 6 && (
              <div className="pt-3 border-t border-[var(--color-border)] mt-3">
                <Pagination
                  page={historyPage}
                  total={historyTotal}
                  pageSize={6}
                  onPageChange={(p) => {
                    setHistoryPage(p);
                    loadHistory(p);
                  }}
                />
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Output / Active Result Display */}
        <div className="xl:col-span-3 space-y-4">
          {/* 1. In-Progress Multi-Step Loading State */}
          {submitting && (
            <div className="card p-6 space-y-4 animate-in fade-in">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl flex items-center justify-center icon-box-purple animate-pulse">
                  <Loader2 size={20} className="animate-spin" />
                </div>
                <div>
                  <h4 className="font-bold text-sm text-[var(--color-text)]">
                    Evaluating Commit Compliance
                  </h4>
                  <p className="text-xs text-[var(--color-text-dim)] mt-0.5">
                    Inspecting Git references, diffs, and evaluating policy ASTs...
                  </p>
                </div>
              </div>

              <div className="space-y-2.5 pt-2 border-t border-[var(--color-border)]/50">
                {SCAN_STEPS.map((step) => {
                  const isDone = scanStep > step.id;
                  const isCurrent = scanStep === step.id;
                  return (
                    <div key={step.id} className="flex items-center gap-3 text-xs">
                      {isDone ? (
                        <CheckCircle2 size={16} className="text-[var(--color-good)] shrink-0" />
                      ) : isCurrent ? (
                        <Loader2 size={16} className="text-[var(--color-brand)] animate-spin shrink-0" />
                      ) : (
                        <div className="w-4 h-4 rounded-full border border-[var(--color-border)] shrink-0" />
                      )}
                      <span
                        className={
                          isDone
                            ? "text-[var(--color-text)] font-medium"
                            : isCurrent
                            ? "text-[var(--color-brand)] font-bold"
                            : "text-[var(--color-text-faint)]"
                        }
                      >
                        {step.label}
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* 2. INVALID COMMIT / ERROR STATE */}
          {!submitting && invalidError && (
            <div className="card p-6 border-l-4 border-l-[var(--color-warn)] bg-[var(--color-surface)] space-y-4 animate-in fade-in zoom-in-95">
              <div className="flex items-start gap-3.5">
                <div className="w-10 h-10 rounded-xl flex items-center justify-center shrink-0 icon-box-amber shadow-xs">
                  <AlertTriangle size={22} />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <h3 className="font-bold text-sm text-[var(--color-text)]">
                      Invalid Commit Reference
                    </h3>
                    <Badge variant="warn" size="sm">
                      {invalidError.errorCode || "NOT_FOUND"}
                    </Badge>
                  </div>
                  <p className="text-xs text-[var(--color-text-dim)] mt-1 leading-relaxed">
                    {invalidError.message}
                  </p>
                </div>
              </div>

              <div className="p-3.5 rounded-xl bg-[var(--color-surface-2)]/60 border border-[var(--color-border)]/60 text-xs space-y-2">
                <p className="font-semibold text-[var(--color-text)]">Verification Checklist:</p>
                <ul className="space-y-1 text-[var(--color-text-dim)] list-disc list-inside">
                  <li>Confirm that commit SHA <code className="font-mono bg-[var(--color-surface-3)] px-1 py-0.5 rounded">{invalidError.commitHash}</code> exists in the repository.</li>
                  <li>Verify that branch <code className="font-mono bg-[var(--color-surface-3)] px-1 py-0.5 rounded">{invalidError.branch}</code> has been pushed to the remote.</li>
                  <li>Ensure the commit is reachable from the target branch.</li>
                </ul>
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <Button variant="secondary" size="sm" onClick={resetForm}>
                  Try Again
                </Button>
              </div>
            </div>
          )}

          {/* 3. VERIFIED RESULT STATE (PASSED or BLOCKED) */}
          {!submitting && !invalidError && result && (
            <ScanResultView result={result} />
          )}

          {/* 4. DEFAULT EMPTY STATE */}
          {!submitting && !invalidError && !result && (
            <div className="card p-10 text-center space-y-3">
              <div className="w-12 h-12 rounded-2xl bg-[var(--color-surface-2)] flex items-center justify-center mx-auto text-[var(--color-text-faint)] border border-[var(--color-border)]">
                <GitCommit size={24} />
              </div>
              <div>
                <h4 className="font-semibold text-sm text-[var(--color-text)]">Awaiting Compliance Check</h4>
                <p className="text-xs text-[var(--color-text-dim)] max-w-md mx-auto mt-1">
                  Enter a target Git branch and commit reference on the left to verify data residency & sovereignty rules before merging.
                </p>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Historical Scan Details Modal */}
      {selectedScan && (
        <Modal
          isOpen={!!selectedScan}
          onClose={() => setSelectedScan(null)}
          title={`Scan Details: ${selectedScan.branch} @ ${selectedScan.commitShortSha || selectedScan.commitHash?.slice(0, 7)}`}
          maxWidth="max-w-3xl"
        >
          <div className="space-y-4">
            <ScanResultView result={selectedScan} isModal />
          </div>
        </Modal>
      )}
    </div>
  );
}

/**
 * Reusable component to render the 3 separate evaluation gates:
 * 1. Zero-Trust Policy Compliance Gate
 * 2. GitHub Actions CI Checks Gate
 * 3. Final Aggregate Merge Gate
 */
function ScanResultView({ result, isModal = false }) {
  const isPolicyPassed =
    result.passed === true ||
    result.result === "PASS" ||
    result.status === "PASSED" ||
    result.status === "PASS" ||
    (result.violationCount === 0 && (!result.violations || result.violations.length === 0));

  const isCodeOnly = result.impactType === "CODE_ONLY";
  const githubChecks = result.githubChecks || { overallStatus: "UNAVAILABLE", checks: [] };
  const finalDecision = result.finalDecision || {
    allowed: isPolicyPassed && githubChecks.overallStatus === "SUCCESS",
    decision: isPolicyPassed && githubChecks.overallStatus === "SUCCESS" ? "MERGE ALLOWED" : "MERGE BLOCKED",
    summaryReason: isPolicyPassed
      ? "All zero-trust residency policies satisfied."
      : "Policy violation detected.",
  };

  const isMergeAllowed = finalDecision.allowed === true;

  const githubBadgeVariant =
    githubChecks.overallStatus === "SUCCESS"
      ? "good"
      : githubChecks.overallStatus === "FAILURE"
      ? "bad"
      : githubChecks.overallStatus === "SKIPPED"
      ? "warn"
      : githubChecks.overallStatus === "PENDING"
      ? "brand"
      : "neutral";

  const githubBadgeLabel =
    githubChecks.overallStatus === "SUCCESS"
      ? "ALL CHECKS PASSED"
      : githubChecks.overallStatus === "FAILURE"
      ? "CHECKS FAILED"
      : githubChecks.overallStatus === "SKIPPED"
      ? "CHECKS SKIPPED"
      : githubChecks.overallStatus === "PENDING"
      ? "IN PROGRESS"
      : githubChecks.overallStatus === "LOCAL_ANALYSIS"
      ? "LOCAL ANALYSIS"
      : "STATUS UNAVAILABLE";

  return (
    <div className="space-y-4 animate-in fade-in">
      {/* 1. AGGREGATE FINAL MERGE DECISION BANNER */}
      <div
        className={`p-4 rounded-xl border flex flex-wrap items-center justify-between gap-3 shadow-xs ${
          isMergeAllowed
            ? "bg-[var(--color-good-light)] border-[var(--color-good)]/30"
            : "bg-[var(--color-bad-light)] border-[var(--color-bad)]/30"
        }`}
      >
        <div className="flex items-center gap-3">
          <div
            className={`w-11 h-11 rounded-xl flex items-center justify-center shadow-xs ${
              isMergeAllowed ? "icon-box-green" : "icon-box-red"
            }`}
          >
            {isMergeAllowed ? <ShieldCheck size={24} /> : <ShieldAlert size={24} />}
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="font-bold text-sm text-[var(--color-text)]">
                Final Merge Decision: {finalDecision.decision}
              </h3>
              <Badge variant={isMergeAllowed ? "good" : "bad"} dot size="sm">
                {finalDecision.decision}
              </Badge>
            </div>
            <p className="text-xs text-[var(--color-text-dim)] font-mono mt-0.5">
              Branch: <strong className="text-[var(--color-text)]">{result.branch}</strong> • Commit:{" "}
              <strong className="text-[var(--color-text)]">{result.commitShortSha || result.commitHash?.slice(0, 7)}</strong>
            </p>
          </div>
        </div>
        <div className="text-right text-xs text-[var(--color-text-dim)] max-w-sm">
          <p className="font-medium text-[var(--color-text)] leading-snug">{finalDecision.summaryReason}</p>
        </div>
      </div>

      {/* 2. THREE INDEPENDENT GATES STATUS GRID */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        {/* Gate 1: Policy Compliance */}
        <div className={`card p-3.5 border-l-4 ${
          isPolicyPassed ? "border-l-[var(--color-good)]" : "border-l-[var(--color-bad)]"
        } space-y-1.5`}>
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-[var(--color-text-dim)] flex items-center gap-1.5">
              <ShieldCheck size={14} className={isPolicyPassed ? "text-[var(--color-good)]" : "text-[var(--color-bad)]"} />
              1. Policy Compliance Gate
            </span>
            <Badge variant={isPolicyPassed ? "good" : "bad"} size="sm">
              {isPolicyPassed ? "PASSED" : "BLOCKED"}
            </Badge>
          </div>
          <p className="text-[11px] text-[var(--color-text-dim)]">
            {isPolicyPassed
              ? "Zero data residency or sovereignty violations."
              : `${result.violationCount || result.violations?.length || 1} residency violation(s) detected.`}
          </p>
        </div>

        {/* Gate 2: GitHub Actions CI Checks */}
        <div className={`card p-3.5 border-l-4 ${
          githubChecks.overallStatus === "FAILURE"
            ? "border-l-[var(--color-bad)]"
            : githubChecks.overallStatus === "SKIPPED"
            ? "border-l-[var(--color-warn)]"
            : githubChecks.overallStatus === "SUCCESS"
            ? "border-l-[var(--color-good)]"
            : "border-l-[var(--color-brand)]"
        } space-y-1.5`}>
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-[var(--color-text-dim)] flex items-center gap-1.5">
              <Server size={14} className={
                githubChecks.overallStatus === "FAILURE"
                  ? "text-[var(--color-bad)]"
                  : githubChecks.overallStatus === "SUCCESS"
                  ? "text-[var(--color-good)]"
                  : "text-[var(--color-warn)]"
              } />
              2. GitHub Actions CI Gate
            </span>
            <Badge variant={githubBadgeVariant} size="sm">
              {githubBadgeLabel}
            </Badge>
          </div>
          <p className="text-[11px] text-[var(--color-text-dim)]">
            {githubChecks.failureReason
              ? githubChecks.failureReason
              : githubChecks.overallStatus === "SUCCESS"
              ? "All automated test suites & build workflows verified on GitHub."
              : "GitHub verification status unavailable for this commit."}
          </p>
        </div>
      </div>

      {/* 3. Commit Metadata & Impact Card */}
      <div className="card p-4 space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[var(--color-border)]/50 pb-2.5">
          <div className="flex items-center gap-2">
            <GitCommit size={15} className="text-[var(--color-brand)]" />
            <span className="text-xs font-bold text-[var(--color-text)] font-mono">
              {result.commitShortSha || result.commitHash?.slice(0, 7)}
            </span>
            {result.impactType && (
              <Badge variant={isCodeOnly ? "neutral" : isPolicyPassed ? "good" : "warn"} size="sm">
                {result.impactType}
              </Badge>
            )}
          </div>
          {result.author && (
            <div className="flex items-center gap-1.5 text-xs text-[var(--color-text-dim)]">
              <User size={13} className="text-[var(--color-text-faint)]" />
              <span>{result.author}</span>
            </div>
          )}
          {result.timestamp && (
            <div className="flex items-center gap-1.5 text-xs text-[var(--color-text-faint)]">
              <Clock size={13} />
              <span>{new Date(result.timestamp).toLocaleString()}</span>
            </div>
          )}
        </div>

        {result.commitMessage && (
          <p className="text-xs text-[var(--color-text)] font-medium bg-[var(--color-surface-2)]/40 p-2.5 rounded-lg border border-[var(--color-border)]/40">
            "{result.commitMessage}"
          </p>
        )}

        {/* Impact Summary Banner */}
        {result.impactSummary && (
          <div className={`p-2.5 rounded-lg text-xs flex items-center gap-2 ${
            isCodeOnly
              ? "bg-[var(--color-surface-2)] border border-[var(--color-border)] text-[var(--color-text-dim)]"
              : isPolicyPassed
              ? "bg-[var(--color-good-light)] border border-[var(--color-good)]/20 text-[var(--color-good-text)]"
              : "bg-[var(--color-bad-light)] border border-[var(--color-bad)]/20 text-[var(--color-bad-text)]"
          }`}>
            <Info size={14} className="shrink-0" />
            <span>{result.impactSummary}</span>
          </div>
        )}

        {/* Changed Files Table */}
        {result.changedFiles && result.changedFiles.length > 0 && (
          <div className="space-y-1.5 pt-1">
            <div className="flex items-center justify-between text-xs">
              <span className="font-semibold text-[var(--color-text-dim)]">
                Changed Files in Commit ({result.changedFiles.length})
              </span>
              <span className="text-[11px] text-[var(--color-text-faint)] font-mono">
                {result.flowsChecked || (isCodeOnly ? 0 : 1)} flow(s) evaluated
              </span>
            </div>
            <div className="divide-y divide-[var(--color-border)]/40 border border-[var(--color-border)] rounded-lg overflow-hidden text-xs">
              {result.changedFiles.map((f, i) => (
                <div key={i} className="p-2 flex items-center justify-between gap-2 bg-[var(--color-surface)]">
                  <div className="flex items-center gap-2 min-w-0">
                    <FileCode size={13} className="text-[var(--color-text-faint)] shrink-0" />
                    <span className="font-mono text-[11px] text-[var(--color-text)] truncate">{f.path}</span>
                  </div>
                  <Badge variant={f.category === "SERVICE" || f.category === "DATAFLOW" || f.category === "POLICY" ? "brand" : "neutral"} size="sm">
                    {f.category || "CODE"}
                  </Badge>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* 4. Live GitHub Actions Checks Breakdown */}
      {githubChecks.checks && githubChecks.checks.length > 0 && (
        <div className="card p-4 space-y-2.5">
          <div className="flex items-center justify-between">
            <h4 className="font-bold text-xs text-[var(--color-text)] flex items-center gap-1.5">
              <Server size={14} />
              GitHub Actions Workflow Runs ({githubChecks.checks.length})
            </h4>
            <div className="flex items-center gap-2 text-[10px] font-mono">
              <span className="text-[var(--color-good)]">{githubChecks.passedChecks || 0} passed</span>
              <span>•</span>
              <span className="text-[var(--color-bad)]">{githubChecks.failedChecks || 0} failed</span>
              {githubChecks.skippedChecks > 0 && (
                <>
                  <span>•</span>
                  <span className="text-[var(--color-warn)]">{githubChecks.skippedChecks} skipped</span>
                </>
              )}
            </div>
          </div>

          <div className="divide-y divide-[var(--color-border)]/40 border border-[var(--color-border)] rounded-lg overflow-hidden text-xs">
            {githubChecks.checks.map((c, idx) => {
              const conclusion = (c.conclusion || c.status || "").toLowerCase();
              const isSuccess = conclusion === "success" || conclusion === "neutral";
              const isFailed = conclusion === "failure" || conclusion === "timed_out" || conclusion === "action_required" || conclusion === "cancelled";
              const isSkipped = conclusion === "skipped";

              const badgeVar = isSuccess ? "good" : isFailed ? "bad" : isSkipped ? "warn" : "brand";

              return (
                <div key={idx} className="p-2.5 flex items-center justify-between gap-3 bg-[var(--color-surface)]">
                  <div className="flex items-center gap-2 min-w-0">
                    {isSuccess ? (
                      <CheckCircle2 size={14} className="text-[var(--color-good)] shrink-0" />
                    ) : isFailed ? (
                      <XCircle size={14} className="text-[var(--color-bad)] shrink-0" />
                    ) : isSkipped ? (
                      <SkipForward size={14} className="text-[var(--color-warn)] shrink-0" />
                    ) : (
                      <Loader2 size={14} className="text-[var(--color-brand)] animate-spin shrink-0" />
                    )}
                    <span className="font-semibold text-[var(--color-text)] truncate">{c.name}</span>
                    {c.details && c.details !== conclusion && (
                      <span className="text-[11px] text-[var(--color-text-faint)] truncate font-mono">
                        ({c.details})
                      </span>
                    )}
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    <Badge variant={badgeVar} size="sm">
                      {isSkipped ? "SKIPPED" : isFailed ? "FAILED" : isSuccess ? "PASSED" : (c.conclusion || c.status)}
                    </Badge>
                    {c.url && (
                      <a
                        href={c.url}
                        target="_blank"
                        rel="noreferrer"
                        className="text-[var(--color-text-faint)] hover:text-[var(--color-brand)] transition-colors p-1"
                        title="View job on GitHub"
                      >
                        <ExternalLink size={12} />
                      </a>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* 5. Policy Violations Breakdown (If BLOCKED) */}
      {!isPolicyPassed && result.violations && result.violations.length > 0 && (
        <div className="space-y-3">
          <h4 className="font-bold text-xs text-[var(--color-bad)] flex items-center gap-1.5">
            <AlertTriangle size={15} />
            Detected Policy Violations ({result.violations.length})
          </h4>

          {result.violations.map((v, idx) => (
            <div key={idx} className="card p-4 space-y-3 border-l-4 border-l-[var(--color-bad)]">
              {/* Header */}
              <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[var(--color-border)]/50 pb-2">
                <div className="flex items-center gap-2">
                  <span className="font-mono text-xs font-bold text-[var(--color-text)]">
                    {v.sourceService} [{v.sourceRegion}] → {v.destinationService} [{v.destinationRegion}]
                  </span>
                </div>
                <div className="flex items-center gap-1.5">
                  <Badge variant="warn" size="sm">
                    {v.dataClass}
                  </Badge>
                  <Badge variant="bad" size="sm">
                    {v.policyCode}
                  </Badge>
                </div>
              </div>

              {/* Policy & Reason */}
              <div className="text-xs space-y-1">
                <p className="font-semibold text-[var(--color-text)]">
                  Policy: <span className="font-normal text-[var(--color-text-dim)]">{v.policyName || v.policyCode}</span>
                </p>
                <div className="p-2.5 rounded-lg bg-[var(--color-bad-light)] border border-[var(--color-bad)]/20 text-[var(--color-bad-text)] text-xs">
                  <p className="font-semibold mb-0.5">Why is this blocked?</p>
                  <p className="text-[11px] leading-relaxed">{v.reason}</p>
                </div>
              </div>

              {/* Before -> After Comparison */}
              {v.beforeAfter && (
                <div className="text-xs space-y-1.5 pt-1">
                  <p className="font-semibold text-[var(--color-text-dim)]">What Changed?</p>
                  <div className="grid grid-cols-2 gap-2 text-[11px] font-mono">
                    <div className="p-2 rounded-lg bg-[var(--color-surface-2)]/60 border border-[var(--color-border)]">
                      <span className="text-[var(--color-text-faint)] block mb-1">Previous Flow</span>
                      <p className="text-[var(--color-good)] font-semibold flex items-center gap-1">
                        <Check size={12} /> {v.beforeAfter.previous?.source} → {v.beforeAfter.previous?.destination}
                      </p>
                    </div>
                    <div className="p-2 rounded-lg bg-[var(--color-bad-light)] border border-[var(--color-bad)]/30">
                      <span className="text-[var(--color-bad-text)] block mb-1">Proposed Change</span>
                      <p className="text-[var(--color-bad)] font-semibold flex items-center gap-1">
                        <XCircle size={12} /> {v.beforeAfter.proposed?.source} → {v.beforeAfter.proposed?.destination}
                      </p>
                    </div>
                  </div>
                </div>
              )}

              {/* Visual Decision Pipeline */}
              {v.visualFlow && v.visualFlow.length > 0 && (
                <div className="text-xs space-y-1.5 pt-1">
                  <p className="font-semibold text-[var(--color-text-dim)]">Decision Pipeline:</p>
                  <div className="flex flex-wrap items-center gap-1.5 p-2.5 rounded-lg bg-[var(--color-surface-2)]/50 border border-[var(--color-border)]/50 text-[10px] font-mono">
                    {v.visualFlow.map((step, sIdx) => (
                      <span key={sIdx} className="flex items-center gap-1">
                        <span className={`px-1.5 py-0.5 rounded ${
                          sIdx === v.visualFlow.length - 1
                            ? "bg-[var(--color-bad)] text-white font-bold"
                            : "bg-[var(--color-surface)] text-[var(--color-text)] border border-[var(--color-border)]"
                        }`}>
                          {step}
                        </span>
                        {sIdx < v.visualFlow.length - 1 && (
                          <ChevronRight size={10} className="text-[var(--color-text-faint)]" />
                        )}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {/* How to Fix */}
              {v.howToFix && (
                <div className="p-2.5 rounded-lg bg-[var(--color-brand-light)] border border-[var(--color-brand)]/20 text-[var(--color-brand-text)] text-xs space-y-1">
                  <p className="font-semibold flex items-center gap-1.5">
                    <Sparkles size={13} className="text-[var(--color-brand)]" />
                    How to fix this issue:
                  </p>
                  <p className="text-[11px] leading-relaxed opacity-90 whitespace-pre-line">{v.howToFix}</p>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
