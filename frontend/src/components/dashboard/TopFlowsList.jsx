export default function TopFlowsList({ flows = [] }) {
  const max = Math.max(...flows.map((f) => f.count || 0), 1);

  if (flows.length === 0) {
    return (
      <div className="py-6 text-center text-xs text-[var(--color-text-faint)]">
        No active data flows recorded.
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {flows.slice(0, 5).map((flow) => (
        <div key={`${flow.source}-${flow.destination}`} className="space-y-1.5">
          <div className="flex items-center justify-between text-xs">
            <div className="flex items-center gap-1.5 min-w-0 font-mono">
              <span className="text-[var(--color-text)] font-semibold truncate text-[11px]">{flow.source}</span>
              <span className="text-[var(--color-text-faint)] text-[10px]">→</span>
              <span className="text-[var(--color-text)] font-semibold truncate text-[11px]">{flow.destination}</span>
            </div>
            <span className="text-xs font-semibold text-[var(--color-text-dim)] shrink-0 ml-2 font-mono">
              {flow.count}
            </span>
          </div>

          <div className="h-1.5 rounded-full bg-[var(--color-surface-2)] overflow-hidden">
            <div
              className="h-full rounded-full bg-gradient-to-r from-indigo-500 to-purple-500 transition-all duration-300"
              style={{ width: `${Math.min(100, (flow.count / max) * 100)}%` }}
            />
          </div>
        </div>
      ))}
    </div>
  );
}
