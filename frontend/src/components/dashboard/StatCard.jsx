import { ArrowUp, ArrowDown } from "lucide-react";

const ICON_BOX_CLASSES = {
  purple: "icon-box-purple",
  blue: "icon-box-blue",
  green: "icon-box-green",
  red: "icon-box-red",
  amber: "icon-box-amber",
};

export default function StatCard({
  icon: Icon,
  color = "purple",
  label,
  value,
  delta,
  trend = "up",
  subtitle = "from last week",
}) {
  const iconClass = ICON_BOX_CLASSES[color] || ICON_BOX_CLASSES.purple;
  const isDown = trend === "down";
  const isNeutral = trend === "neutral";

  return (
    <div className="card p-4.5 flex flex-col justify-between min-w-0 transition-all hover:border-[var(--color-border-strong)]">
      <div className="flex items-start justify-between gap-3 mb-2">
        <div>
          <p className="text-xs font-medium text-[var(--color-text-dim)] truncate">{label}</p>
          <p className="text-2xl font-bold tracking-tight text-[var(--color-text)] mt-1">
            {value}
          </p>
        </div>
        <div className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 shadow-2xs ${iconClass}`}>
          <Icon size={18} />
        </div>
      </div>

      {delta && (
        <div className="flex items-center gap-1 text-[11px] font-medium pt-1">
          {isDown ? (
            <span className="flex items-center gap-0.5 text-[var(--color-bad)]">
              <ArrowDown size={12} /> {delta}
            </span>
          ) : isNeutral ? (
            <span className="text-[var(--color-text-dim)]">{delta}</span>
          ) : (
            <span className="flex items-center gap-0.5 text-[var(--color-good)]">
              <ArrowUp size={12} /> {delta}
            </span>
          )}
          {subtitle && (
            <span className="text-[var(--color-text-faint)] font-normal truncate">{subtitle}</span>
          )}
        </div>
      )}
    </div>
  );
}
