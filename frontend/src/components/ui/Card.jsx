export function Card({
  children,
  className = "",
  hover = false,
  padding = "p-5",
  ...props
}) {
  return (
    <div
      className={`card ${hover ? "card-hover" : ""} ${padding} ${className}`}
      {...props}
    >
      {children}
    </div>
  );
}

export function CardHeader({
  title,
  subtitle,
  icon: Icon,
  action,
  className = "",
}) {
  return (
    <div className={`flex items-center justify-between gap-3 mb-4 ${className}`}>
      <div className="flex items-center gap-2.5 min-w-0">
        {Icon && (
          <div className="w-8 h-8 rounded-lg bg-[var(--color-surface-2)] border border-[var(--color-border)] flex items-center justify-center text-[var(--color-brand)] shrink-0">
            <Icon size={16} />
          </div>
        )}
        <div className="min-w-0">
          <h3 className="font-semibold text-sm text-[var(--color-text)] truncate">{title}</h3>
          {subtitle && <p className="text-xs text-[var(--color-text-dim)] truncate mt-0.5">{subtitle}</p>}
        </div>
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </div>
  );
}

export default Card;
