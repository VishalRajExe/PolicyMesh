import { ArrowUp, ArrowDown } from "lucide-react";

const COLOR_STYLES = {
  purple: {
    bg: "bg-purple-50 dark:bg-purple-950/40 text-purple-600 dark:text-purple-400 border border-purple-200/50 dark:border-purple-800/40",
  },
  blue: {
    bg: "bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400 border border-blue-200/50 dark:border-blue-800/40",
  },
  green: {
    bg: "bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400 border border-emerald-200/50 dark:border-emerald-800/40",
  },
  red: {
    bg: "bg-rose-50 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400 border border-rose-200/50 dark:border-rose-800/40",
  },
  amber: {
    bg: "bg-amber-50 dark:bg-amber-950/40 text-amber-600 dark:text-amber-400 border border-amber-200/50 dark:border-amber-800/40",
  },
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
  const style = COLOR_STYLES[color] || COLOR_STYLES.purple;
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
        <div className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 shadow-2xs ${style.bg}`}>
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
