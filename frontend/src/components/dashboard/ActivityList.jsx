import { FileText, GitBranch, User, CheckCircle2 } from "lucide-react";

const EVENT_ICONS = {
  policy: {
    icon: FileText,
    iconBoxClass: "icon-box-green",
  },
  flow: {
    icon: GitBranch,
    iconBoxClass: "icon-box-purple",
  },
  user: {
    icon: User,
    iconBoxClass: "icon-box-blue",
  },
  approval: {
    icon: CheckCircle2,
    iconBoxClass: "icon-box-amber",
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
            <div className={`w-7 h-7 rounded-lg flex items-center justify-center shrink-0 shadow-2xs ${config.iconBoxClass}`}>
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
