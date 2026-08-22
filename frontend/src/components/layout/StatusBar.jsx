const SERVICES = [
  { label: "API", status: "Healthy" },
  { label: "AI Service", status: "Healthy" },
  { label: "Database", status: "Healthy" },
  { label: "Kafka", status: "Healthy" },
];

export default function StatusBar({ services = SERVICES, allSystemsLabel = "All systems operational" }) {
  return (
    <footer className="flex items-center justify-between px-6 lg:px-8 h-14 shrink-0 border-t border-[var(--color-border)] text-xs">
      <div className="flex items-center gap-2 text-[var(--color-text-dim)]">
        <span className="w-2 h-2 rounded-full bg-[var(--color-good)]" />
        {allSystemsLabel}
      </div>
      <div className="hidden sm:flex items-center gap-2">
        {services.map((s) => (
          <div
            key={s.label}
            className="flex items-center gap-1.5 rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-1.5 text-[var(--color-text-dim)]"
          >
            <span>{s.label}</span>
            <span className="w-1.5 h-1.5 rounded-full bg-[var(--color-good)]" />
            <span className="text-[var(--color-good)]">{s.status}</span>
          </div>
        ))}
      </div>
      <span className="text-[var(--color-text-faint)]">PolicyMesh v1.0.0</span>
    </footer>
  );
}
