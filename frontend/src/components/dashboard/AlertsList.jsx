import { ShieldAlert, AlertTriangle, Info } from "lucide-react";

const SEVERITY_STYLES = {
  High: { icon: ShieldAlert, iconBg: "bg-[#ef4444]/15 text-[#f87171]", badge: "bg-[#ef4444]/15 text-[#f87171]" },
  Medium: { icon: AlertTriangle, iconBg: "bg-[#f59e0b]/15 text-[#fbbf24]", badge: "bg-[#f59e0b]/15 text-[#fbbf24]" },
  Low: { icon: Info, iconBg: "bg-[#3b82f6]/15 text-[#60a5fa]", badge: "bg-[#3b82f6]/15 text-[#60a5fa]" },
};

export default function AlertsList({ alerts }) {
  return (
    <div className="space-y-1">
      {alerts.map((alert, idx) => {
        const style = SEVERITY_STYLES[alert.severity] || SEVERITY_STYLES.Low;
        const Icon = style.icon;
        return (
          <div key={idx} className="flex items-start gap-3 py-2.5">
            <div className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 ${style.iconBg}`}>
              <Icon size={15} />
            </div>
            <div className="min-w-0 flex-1">
              <p className="text-sm text-white leading-snug">{alert.message}</p>
              <div className="flex items-center gap-2 mt-1">
                <span className={`text-[10px] font-semibold px-1.5 py-0.5 rounded ${style.badge}`}>
                  {alert.severity}
                </span>
                <span className="text-xs text-[var(--color-text-faint)]">{alert.source}</span>
              </div>
            </div>
            <span className="text-xs text-[var(--color-text-faint)] shrink-0">{alert.time}</span>
          </div>
        );
      })}
    </div>
  );
}
