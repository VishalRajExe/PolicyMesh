import { FileText, GitBranch, UserPlus, CheckCircle2 } from "lucide-react";

const ICONS = {
  policy: { icon: FileText, bg: "bg-[#22c55e]/15 text-[#4ade80]" },
  flow: { icon: GitBranch, bg: "bg-[#6d5ef8]/15 text-[#8b7dfa]" },
  user: { icon: UserPlus, bg: "bg-[#3b82f6]/15 text-[#60a5fa]" },
  approval: { icon: CheckCircle2, bg: "bg-[#f59e0b]/15 text-[#fbbf24]" },
};

export default function ActivityList({ activities }) {
  return (
    <div className="space-y-1">
      {activities.map((item, idx) => {
        const cfg = ICONS[item.type] || ICONS.policy;
        const Icon = cfg.icon;
        return (
          <div key={idx} className="flex items-start gap-3 py-2.5">
            <div className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 ${cfg.bg}`}>
              <Icon size={15} />
            </div>
            <div className="min-w-0 flex-1">
              <p className="text-sm text-white leading-snug">{item.message}</p>
              <p className="text-xs text-[var(--color-text-faint)] mt-0.5">By {item.actor}</p>
            </div>
            <span className="text-xs text-[var(--color-text-faint)] shrink-0">{item.time}</span>
          </div>
        );
      })}
    </div>
  );
}
