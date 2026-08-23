import { FileText, GitBranch, User, CheckCircle2 } from "lucide-react";

const EVENT_ICONS = {
  policy: {
    icon: FileText,
    iconBg: "bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400 border border-emerald-200/50 dark:border-emerald-800/40",
  },
  flow: {
    icon: GitBranch,
    iconBg: "bg-indigo-50 dark:bg-indigo-950/40 text-indigo-600 dark:text-indigo-400 border border-indigo-200/50 dark:border-indigo-800/40",
  },
  user: {
    icon: User,
    iconBg: "bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400 border border-blue-200/50 dark:border-blue-800/40",
  },
  approval: {
    icon: CheckCircle2,
    iconBg: "bg-amber-50 dark:bg-amber-950/40 text-amber-600 dark:text-amber-400 border border-amber-200/50 dark:border-amber-800/40",
  },
};

export default function ActivityList({ activities = [] }) {
  if (activities.length === 0) {
    return (
      <div className="py-6 text-center text-xs text-[var(--color-text-faint)]">
        No recent activity logged.
      </div>
    );
  }

  return (
    <div className="space-y-2.5 divide-y divide-[var(--color-border)]/40">
      {activities.slice(0, 4).map((item, idx) => {
        const config = EVENT_ICONS[item.type] || EVENT_ICONS.policy;
        const Icon = config.icon;

        return (
          <div key={idx} className="flex items-start gap-2.5 pt-2.5 first:pt-0">
            <div className={`w-7 h-7 rounded-lg flex items-center justify-center shrink-0 shadow-2xs ${config.iconBg}`}>
              <Icon size={14} />
            </div>

            <div className="min-w-0 flex-1">
              <p className="text-xs font-medium text-[var(--color-text)] leading-snug line-clamp-1">
                {item.message}
              </p>
              <p className="text-[11px] text-[var(--color-text-faint)] mt-0.5 truncate">
                By {item.actor}
              </p>
            </div>

            <span className="text-[10px] text-[var(--color-text-faint)] shrink-0 font-medium">
              {item.time}
            </span>
          </div>
        );
      })}
    </div>
  );
}
