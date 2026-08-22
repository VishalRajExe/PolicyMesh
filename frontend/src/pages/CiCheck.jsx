import { useState } from "react";
import { GitBranch, Play, ShieldCheck, ShieldAlert, Loader2, Hash } from "lucide-react";
import Topbar from "../components/layout/Topbar";
import { ciApi } from "../api";

const DEMO_BRANCHES = ["main", "develop", "feature/policy-v2", "fix/region-check"];

export default function CiCheck() {
  const [form, setForm] = useState({ commitHash: "", branch: "main" });
  const [result, setResult] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);
  const [history, setHistory] = useState([]);

  const HASH_PATTERN = /^[a-zA-Z0-9_.-]+$/;

  async function handleRun(e) {
    e.preventDefault();
    const { commitHash, branch } = form;
    if (!commitHash.trim()) { setFormError("Commit hash is required."); return; }
    if (!HASH_PATTERN.test(commitHash.trim())) { setFormError("Commit hash must be alphanumeric (dots or hyphens allowed)."); return; }
    if (!branch.trim()) { setFormError("Branch is required."); return; }
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
        subtitle="Run policy compliance checks against your service graph to catch violations before merge."
      />

      <div className="px-6 lg:px-8 mt-6 grid grid-cols-1 xl:grid-cols-5 gap-6 pb-8">
        {/* Form */}
        <div className="xl:col-span-2 space-y-4">
          <div className="card p-5 space-y-4">
            <div className="flex items-center gap-2">
              <GitBranch size={16} className="text-[var(--color-brand)]" />
              <h2 className="font-semibold text-white text-sm">Run Compliance Check</h2>
            </div>

            <form onSubmit={handleRun} className="space-y-4">
              <div>
                <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">Branch *</label>
                <div className="flex gap-2">
                  <input
                    value={form.branch}
                    onChange={(e) => setForm((f) => ({ ...f, branch: e.target.value }))}
                    placeholder="main"
                    list="branch-list"
                    className="field-input"
                    required
                  />
                  <datalist id="branch-list">
                    {DEMO_BRANCHES.map((b) => <option key={b} value={b} />)}
                  </datalist>
                </div>
              </div>

              <div>
                <label className="block text-xs text-[var(--color-text-dim)] mb-1.5">
                  Commit Hash *
                </label>
                <div className="relative">
                  <Hash size={13} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--color-text-faint)]" />
                  <input
                    value={form.commitHash}
                    onChange={(e) => setForm((f) => ({ ...f, commitHash: e.target.value }))}
                    placeholder="abc1234"
                    className="field-input pl-8"
                    required
                  />
                </div>
              </div>

              {formError && (
                <p className="text-xs text-[var(--color-bad)] bg-[var(--color-bad)]/10 rounded-lg px-3 py-2">{formError}</p>
              )}

              <button type="submit" disabled={submitting} className="btn-primary w-full justify-center">
                {submitting ? <Loader2 size={15} className="animate-spin" /> : <Play size={15} />}
                {submitting ? "Analyzing…" : "Run Check"}
              </button>
            </form>
          </div>

          {/* Past scans */}
          {history.length > 0 && (
            <div className="card overflow-hidden">
              <div className="px-4 py-3 border-b border-[var(--color-border)]">
                <p className="text-xs font-medium text-[var(--color-text-dim)]">Past Scans (this session)</p>
              </div>
              <div className="divide-y divide-[var(--color-border)]">
                {history.map((h, i) => (
                  <button
                    key={i}
                    className="w-full text-left px-4 py-3 hover:bg-[var(--color-surface-2)] transition-colors"
                    onClick={() => setResult(h)}
                  >
                    <div className="flex items-center gap-2">
                      {h.result === "PASS"
                        ? <ShieldCheck size={13} className="text-[var(--color-good)] shrink-0" />
                        : <ShieldAlert size={13} className="text-[var(--color-bad)] shrink-0" />}
                      <span className="text-xs font-medium text-white truncate">{h.branch}</span>
                      <span className="text-xs text-[var(--color-text-faint)] font-mono shrink-0">@{h.commitHash?.slice(0, 8)}</span>
                    </div>
                    <p className={`text-xs mt-0.5 ml-5 ${h.result === "PASS" ? "text-[var(--color-good)]" : "text-[var(--color-bad)]"}`}>
                      {h.result} · {h.violationCount} violation{h.violationCount !== 1 ? "s" : ""}
                    </p>
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Results panel */}
        <div className="xl:col-span-3">
          {!result && !submitting && (
            <div className="card px-5 py-16 text-center">
              <GitBranch size={32} className="mx-auto mb-3 text-[var(--color-text-faint)]" />
              <p className="text-[var(--color-text-faint)] text-sm">
                Enter a branch and commit hash, then run a compliance check to see results.
              </p>
            </div>
          )}

          {submitting && (
            <div className="card px-5 py-16 text-center">
              <Loader2 size={28} className="animate-spin mx-auto mb-3 text-[var(--color-brand)]" />
              <p className="text-[var(--color-text-dim)] text-sm">Analyzing policies and service graph…</p>
            </div>
          )}

          {result && !submitting && (
            <div className="space-y-4">
              {/* Result header */}
              <div className={`card p-5 border-2 ${
                result.result === "PASS"
                  ? "border-[var(--color-good)]/40 bg-[var(--color-good)]/5"
                  : "border-[var(--color-bad)]/40 bg-[var(--color-bad)]/5"
              }`}>
                <div className="flex items-center gap-3 mb-3">
                  {result.result === "PASS"
                    ? <ShieldCheck size={32} className="text-[var(--color-good)]" />
                    : <ShieldAlert size={32} className="text-[var(--color-bad)]" />}
                  <div>
                    <p className={`text-2xl font-bold ${result.result === "PASS" ? "text-[var(--color-good)]" : "text-[var(--color-bad)]"}`}>
                      {result.result === "PASS" ? "Compliance Passed" : "Compliance Failed"}
                    </p>
                    <p className="text-xs text-[var(--color-text-faint)]">
                      Branch <strong className="text-white">{result.branch}</strong> @ <span className="font-mono">{result.commitHash}</span>
                    </p>
                  </div>
                </div>
                <p className="text-sm text-[var(--color-text-dim)]">{result.humanReadable}</p>
              </div>

              {/* Violations */}
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
                      <div key={i} className="px-5 py-4 space-y-1">
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="font-medium text-white text-sm">{v.sourceService}</span>
                          <span className="text-[var(--color-text-faint)] text-xs">[{v.sourceRegion}]</span>
                          <span className="text-[var(--color-text-faint)]">→</span>
                          <span className="font-medium text-white text-sm">{v.destinationService}</span>
                          <span className="text-[var(--color-text-faint)] text-xs">[{v.destinationRegion}]</span>
                          <span className="text-xs px-2 py-0.5 rounded bg-[var(--color-bad)]/15 text-[var(--color-bad)]">{v.dataClass}</span>
                        </div>
                        <div className="text-xs text-[var(--color-text-faint)]">
                          Policy: <span className="text-white">{v.policyCode}</span>
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
