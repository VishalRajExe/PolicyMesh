import { Clock } from "lucide-react";

export default function ComingSoon({ title, subtitle, eta }) {
  return (
    <div className="flex flex-col items-center justify-center h-full min-h-[60vh] px-6 text-center">
      <div className="w-16 h-16 rounded-2xl bg-[var(--color-surface-2)] border border-[var(--color-border)] flex items-center justify-center mb-5">
        <Clock size={28} className="text-[var(--color-text-faint)]" />
      </div>
      <h1 className="text-xl font-semibold text-white mb-2">{title}</h1>
      <p className="text-sm text-[var(--color-text-dim)] max-w-sm">{subtitle}</p>
      {eta && (
        <p className="mt-3 text-xs text-[var(--color-text-faint)]">Expected: {eta}</p>
      )}
      <div className="mt-6 text-xs text-[var(--color-text-faint)] bg-[var(--color-surface)] border border-[var(--color-border)] rounded-xl px-4 py-2">
        This feature requires additional backend API endpoints not yet available in the current release.
      </div>
    </div>
  );
}
