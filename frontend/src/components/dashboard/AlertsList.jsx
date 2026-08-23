import { ShieldAlert, AlertTriangle, Info } from "lucide-react";
import Badge from "../ui/Badge";

const SEVERITY_CONFIG = {
  High: {
    icon: ShieldAlert,
    iconBg: "bg-rose-50 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400 border border-rose-200/50 dark:border-rose-800/40",
    badgeVariant: "bad",
  },
  Medium: {
    icon: AlertTriangle,
    iconBg: "bg-amber-50 dark:bg-amber-950/40 text-amber-600 dark:text-amber-400 border border-amber-200/50 dark:border-amber-800/40",
    badgeVariant: "warn",
  },
  Low: {
    icon: Info,
    iconBg: "bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400 border border-blue-200/50 dark:border-blue-800/40",
    badgeVariant: "info",
  },
};

export default function AlertsList({ alerts = [] }) {
  if (alerts.length === 0) {
    return (
      <div className="py-6 text-center text-xs text-[var(--color-text-faint)]">
        No active alerts. All systems operating normally.
      </div>
    );
  }

  return (
    <div className="space-y-2.5 divide-y divide-[var(--color-border)]/40">
      {alerts.slice(0, 4).map((alert, idx) => {
        const config = SEVERITY_CONFIG[alert.severity] || SEVERITY_CONFIG.Low;
        const Icon = config.icon;

        return (
          <div key={idx} className="flex items-start gap-2.5 pt-2.5 first:pt-0">
            <div className={`w-7 h-7 rounded-lg flex items-center justify-center shrink-0 shadow-2xs ${config.iconBg}`}>
              <Icon size={14} />
            </div>

            <div className="min-w-0 flex-1">
              <p className="text-xs font-medium text-[var(--color-text)] leading-snug line-clamp-1">
                {alert.message || alert.title}
              </p>
              <div className="flex items-center gap-2 mt-1">
                <Badge variant={config.badgeVariant} size="sm">
                  {alert.severity}
                </Badge>
                <span className="text-[11px] text-[var(--color-text-faint)] truncate">{alert.source}</span>
              </div>
            </div>

            <span className="text-[10px] text-[var(--color-text-faint)] shrink-0 font-medium">
              {alert.time}
            </span>
          </div>
        );
      })}
    </div>
  );
}
