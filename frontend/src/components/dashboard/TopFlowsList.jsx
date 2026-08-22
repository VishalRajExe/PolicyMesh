export default function TopFlowsList({ flows }) {
  const max = Math.max(...flows.map((f) => f.count), 1);

  return (
    <div className="space-y-4">
      {flows.map((flow) => (
        <div key={`${flow.source}-${flow.destination}`}>
          <div className="flex items-center justify-between text-sm mb-1.5">
            <span className="text-[var(--color-text-dim)] truncate">
              <span className="text-white font-medium">{flow.source}</span>
              <span className="mx-1.5 text-[var(--color-text-faint)]">→</span>
              <span className="text-white font-medium">{flow.destination}</span>
            </span>
            <span className="text-[var(--color-text-dim)] shrink-0 ml-3">{flow.count}</span>
          </div>
          <div className="h-1.5 rounded-full bg-[var(--color-surface-2)] overflow-hidden">
            <div
              className="h-full rounded-full bg-gradient-to-r from-[#6d5ef8] to-[#8b7dfa]"
              style={{ width: `${(flow.count / max) * 100}%` }}
            />
          </div>
        </div>
      ))}
    </div>
  );
}
