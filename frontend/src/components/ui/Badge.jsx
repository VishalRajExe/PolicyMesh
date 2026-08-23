export function Badge({
  children,
  variant = "neutral", // good, bad, warn, info, brand, neutral
  size = "md", // sm, md
  dot = false,
  icon: Icon,
  className = "",
}) {
  const variantMap = {
    good: "badge-good",
    success: "badge-good",
    allow: "badge-good",
    active: "badge-good",
    approved: "badge-good",
    pass: "badge-good",

    bad: "badge-bad",
    danger: "badge-bad",
    deny: "badge-bad",
    blocked: "badge-bad",
    rejected: "badge-bad",
    fail: "badge-bad",
    high: "badge-bad",

    warn: "badge-warn",
    warning: "badge-warn",
    medium: "badge-warn",
    draft: "badge-info",
    pending: "badge-warn",
    "under review": "badge-warn",
    under_review: "badge-warn",

    info: "badge-info",
    low: "badge-info",

    brand: "badge-brand",
    neutral: "badge-neutral",
  };

  const dotColorMap = {
    good: "bg-[var(--color-good)]",
    bad: "bg-[var(--color-bad)]",
    warn: "bg-[var(--color-warn)]",
    info: "bg-[var(--color-info)]",
    brand: "bg-[var(--color-brand)]",
    neutral: "bg-[var(--color-text-faint)]",
  };

  const key = String(variant).toLowerCase();
  const badgeClass = variantMap[key] || "badge-neutral";
  const dotColor = dotColorMap[key] || "bg-[var(--color-text-faint)]";

  const sizeClass = size === "sm" ? "text-[10px] px-1.5 py-0.5" : "text-[11px] px-2 py-0.5";

  return (
    <span className={`badge ${badgeClass} ${sizeClass} ${className}`}>
      {dot && <span className={`w-1.5 h-1.5 rounded-full ${dotColor} shrink-0`} />}
      {Icon && <Icon size={12} className="shrink-0" />}
      <span>{children}</span>
    </span>
  );
}

export default Badge;
