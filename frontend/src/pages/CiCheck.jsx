import { useState, useEffect } from "react";
import {
  GitBranch,
  Play,
  ShieldCheck,
  ShieldAlert,
  Loader2,
  Hash,
  Zap,
  RotateCcw,
} from "lucide-react";
import Topbar from "../components/layout/Topbar";
import SearchableCombobox from "../components/ui/SearchableCombobox";
import { ciApi } from "../api";
import { useFormDraft } from "../hooks/useFormDraft";

const DEMO_BRANCHES = ["main", "develop", "feature/policy-v2", "fix/region-check"];

const CI_PRESETS = [
  {
    label: "Valid EU Flow (PR #42)",
    branch: "feature/eu-payments-v2",
    commitHash: "7f8a9b0",
    description: "Evaluates orders-api [EU] → payments-api [EU] (Compliant)",
  },
  {
    label: "Cross-Border Violation (PR #43)",
    branch: "feature/cross-border-analytics",
    commitHash: "e3d4c5b",
    description: "Evaluates orders-api [EU] → analytics-api [US] (Violation)",
  },
];

export default function CiCheck() {
  const { values: form, setValues: setForm, clearDraft, resetForm } = useFormDraft(
    "ci-check",
    { commitHash: "7f8a9b0", branch: "main" }
  );

  const [result, setResult] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);

  // Restore history from sessionStorage
  const [history, setHistory] = useState(() => {
    try {
      const saved = sessionStorage.getItem("policymesh:ci-check:history");
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  useEffect(() => {
    try {
      sessionStorage.setItem("policymesh:ci-check:history", JSON.stringify(history));
    } catch {}
  }, [history]);

  const HASH_PATTERN = /^[a-zA-Z0-9_.-]+$/;

  function applyPreset(p) {
    setForm({ branch: p.branch, commitHash: p.commitHash });
    setFormError(null);
  }

  async function handleRun(e) {
    e.preventDefault();
    const { commitHash, branch } = form;
    if (!commitHash.trim()) {
      setFormError("Commit hash is required.");
      return;
    }
    if (!HASH_PATTERN.test(commitHash.trim())) {
      setFormError("Commit hash must be alphanumeric (dots or hyphens allowed).");
      return;
    }
    if (!branch.trim()) {
      setFormError("Branch is required.");
      return;
    }
    setFormError(null);
    setResult(null);
    setSubmitting(true);
    try {
      const resp = await ciApi.runCheck({ commitHash: commitHash.trim(), branch: branch.trim() });
      setResult(resp);
      setHistory((prev) => [resp, ...prev.slice(0, 9)]);
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSubmitting(false);
    }
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
                <GitBranch size={16} className="text-[var(--color-brand)]" />
                <h2 className="font-semibold text-white text-sm">Run Compliance Check</h2>
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

            {/* Quick Scenario Presets */}
            <div className="space-y-1.5">
              <label className="block text-[11px] text-[var(--color-text-faint)] font-medium uppercase tracking-wider flex items-center gap-1">
                <Zap size={12} className="text-[var(--color-brand)]" /> CI Example Scenarios
              </label>
              <div className="grid grid-cols-1 gap-1.5">
                {CI_PRESETS.map((p, i) => (
                  <button
                    key={i}
                    type="button"
                    onClick={() => applyPreset(p)}
                    className="w-full text-left px-3 py-2 rounded-lg bg-[var(--color-surface-2)] border border-[var(--color-border)] hover:border-[var(--color-brand)]/50 hover:bg-[var(--color-surface)] text-xs transition-colors"
                  >
                    <p className="font-semibold text-white">{p.label}</p>
                    <p className="text-[11px] text-[var(--color-text-faint)] mt-0.5">{p.description}</p>
                  </button>
                ))}
              </div>
            </div>

            <form onSubmit={handleRun} className="space-y-4 pt-1">
              <div>
                <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">Git Branch *</label>
                <SearchableCombobox
                  value={form.branch}
                  onChange={(val) => setForm((f) => ({ ...f, branch: val }))}
                  options={DEMO_BRANCHES}
                  placeholder="Select branch..."
                  searchPlaceholder="Search branches..."
                />
              </div>

              <div>
                <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">Commit SHA-1 Hash *</label>
                <div className="relative">
                  <Hash size={13} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--color-text-faint)]" />
                  <input
                    value={form.commitHash}
                    onChange={(e) => setForm((f) => ({ ...f, commitHash: e.target.value }))}
                    placeholder="abc1234"
                    className="field-input pl-8 text-xs font-mono"
                    required
                  />
                </div>
              </div>

              {formError && (
                <p className="text-xs text-[var(--color-bad)] bg-[var(--color-bad)]/10 rounded-lg px-3 py-2">
                  {formError}
                </p>
              )}

              <button type="submit" disabled={submitting} className="btn-primary w-full justify-center text-xs">
                {submitting ? <Loader2 size={15} className="animate-spin" /> : <Play size={15} />}
                {submitting ? "Analyzing Topology Graph…" : "Execute CI Check"}
              </button>
            </form>
          </div>

          {/* Past Scans */}
          {history.length > 0 && (
            <div className="card overflow-hidden">
              <div className="px-4 py-3 border-b border-[var(--color-border)] flex items-center justify-between">
                <p className="text-xs font-medium text-[var(--color-text-dim)]">Scans (this session)</p>
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
                    key={i}
                    className="w-full text-left px-4 py-3 hover:bg-[var(--color-surface-2)] transition-colors"
                    onClick={() => setResult(h)}
                  >
                    <div className="flex items-center gap-2">
                      {h.result === "PASS" ? (
                        <ShieldCheck size={13} className="text-[var(--color-good)] shrink-0" />
                      ) : (
                        <ShieldAlert size={13} className="text-[var(--color-bad)] shrink-0" />
                      )}
                      <span className="text-xs font-medium text-white truncate">{h.branch}</span>
                      <span className="text-xs text-[var(--color-text-faint)] font-mono shrink-0">
                        @{h.commitHash?.slice(0, 8)}
                      </span>
                    </div>
                    <p
                      className={`text-xs mt-0.5 ml-5 font-semibold ${
                        h.result === "PASS" ? "text-[var(--color-good)]" : "text-[var(--color-bad)]"
                      }`}
                    >
                      {h.result} · {h.violationCount} violation{h.violationCount !== 1 ? "s" : ""}
                    </p>
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
              <GitBranch size={32} className="mx-auto mb-3 text-[var(--color-text-faint)]" />
              <p className="text-[var(--color-text-faint)] text-sm">
                Enter a branch and commit hash or select a preset scenario to evaluate compliance.
              </p>
            </div>
          )}

          {submitting && (
            <div className="card px-5 py-16 text-center">
              <Loader2 size={28} className="animate-spin mx-auto mb-3 text-[var(--color-brand)]" />
              <p className="text-[var(--color-text-dim)] text-sm">Analyzing policy definitions and data flow graph…</p>
            </div>
          )}

          {result && !submitting && (
            <div className="space-y-4 animate-in fade-in zoom-in-95">
              <div
                className={`card p-5 border-2 ${
                  result.result === "PASS"
                    ? "border-[var(--color-good)]/40 bg-[var(--color-good)]/5"
                    : "border-[var(--color-bad)]/40 bg-[var(--color-bad)]/5"
                }`}
              >
                <div className="flex items-center gap-3 mb-3">
                  {result.result === "PASS" ? (
                    <ShieldCheck size={32} className="text-[var(--color-good)]" />
                  ) : (
                    <ShieldAlert size={32} className="text-[var(--color-bad)]" />
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
                      Branch <strong className="text-white">{result.branch}</strong> @{" "}
                      <span className="font-mono text-white">{result.commitHash}</span>
                    </p>
                  </div>
                </div>
                <p className="text-sm text-[var(--color-text-dim)]">{result.humanReadable}</p>
              </div>

              {result.violations && result.violations.length > 0 && (
                <div className="card overflow-hidden">
                  <div className="px-5 py-3 border-b border-[var(--color-border)]">
                    <p className="text-sm font-medium text-[var(--color-bad)] flex items-center gap-2">
                      <ShieldAlert size={14} />
                      {result.violationCount} Policy Violation{result.violationCount !== 1 ? "s" : ""}
                    </p>
                  </div>
                  <div className="divide-y divide-[var(--color-border)]">
                    {result.violations.map((v, i) => (
                      <div key={i} className="px-5 py-4 space-y-1 hover:bg-[var(--color-surface-2)] transition-colors">
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="font-semibold text-white text-sm font-mono">{v.sourceService}</span>
                          <span className="text-[var(--color-text-faint)] text-xs">[{v.sourceRegion}]</span>
                          <span className="text-[var(--color-text-faint)]">→</span>
                          <span className="font-semibold text-white text-sm font-mono">{v.destinationService}</span>
                          <span className="text-[var(--color-text-faint)] text-xs">[{v.destinationRegion}]</span>
                          <span className="text-xs font-semibold px-2 py-0.5 rounded bg-[var(--color-bad)]/15 text-[var(--color-bad)] border border-[var(--color-bad)]/30">
                            {v.dataClass}
                          </span>
                        </div>
                        <div className="text-xs text-[var(--color-text-faint)]">
                          Policy Rule: <span className="text-white font-mono">{v.policyCode}</span>
                        </div>
                        <p className="text-xs text-[var(--color-text-dim)]">{v.reason}</p>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {result.result === "PASS" && (
                <div className="card px-5 py-6 text-center">
                  <ShieldCheck size={24} className="mx-auto mb-2 text-[var(--color-good)]" />
                  <p className="text-sm text-[var(--color-text-dim)]">
                    All data flow edges are compliant with active policies. Safe to merge.
                  </p>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
