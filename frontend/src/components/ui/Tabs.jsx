export function Tabs({ tabs = [], activeTab, onChange, className = "" }) {
  return (
    <div
      className={`flex items-center gap-1 p-1 bg-[var(--color-surface-2)] border border-[var(--color-border)] rounded-xl ${className}`}
    >
      {tabs.map((t) => {
        const id = typeof t === "string" ? t : t.id;
        const label = typeof t === "string" ? t : t.label;
        const Icon = typeof t === "object" ? t.icon : null;
        const count = typeof t === "object" ? t.count : null;
        const isActive = activeTab === id;

        return (
          <button
            key={id}
            type="button"
            onClick={() => onChange(id)}
            className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-medium transition-all select-none ${
              isActive
                ? "bg-[var(--color-surface)] text-[var(--color-text)] shadow-sm font-semibold"
                : "text-[var(--color-text-dim)] hover:text-[var(--color-text)]"
            }`}
          >
            {Icon && <Icon size={14} className="shrink-0" />}
            <span>{label}</span>
            {count !== null && count !== undefined && (
              <span
                className={`text-[10px] px-1.5 py-0.2 rounded-full ${
                  isActive
                    ? "bg-[var(--color-brand-light)] text-[var(--color-brand)]"
                    : "bg-[var(--color-surface-3)] text-[var(--color-text-dim)]"
                }`}
              >
                {count}
              </span>
            )}
          </button>
        );
      })}
    </div>
  );
}

export default Tabs;
