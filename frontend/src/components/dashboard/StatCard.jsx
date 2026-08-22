import { ArrowUp, ArrowDown } from "lucide-react";

const ICON_BG = {
  violet: "bg-[#6d5ef8]/15 text-[#8b7dfa]",
  blue: "bg-[#3b82f6]/15 text-[#60a5fa]",
  green: "bg-[#22c55e]/15 text-[#4ade80]",
  red: "bg-[#ef4444]/15 text-[#f87171]",
  amber: "bg-[#f59e0b]/15 text-[#fbbf24]",
};

export default function StatCard({ icon: Icon, color = "violet", label, value, delta, trend = "up" }) {
  const isUp = trend === "up";
  const deltaGood = (isUp && trend === "up") || trend === "down-good";

  return (
    <div className="card p-5 flex flex-col gap-3 min-w-0">
      <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${ICON_BG[color]}`}>
        <Icon size={18} />
      </div>
      <div>
        <p className="text-sm text-[var(--color-text-dim)]">{label}</p>
        <p className="text-2xl font-semibold text-white mt-0.5">{value}</p>
      </div>
      {delta && (
        <p
          className={`flex items-center gap-1 text-xs font-medium ${
            trend === "down" ? "text-[var(--color-bad)]" : "text-[var(--color-good)]"
          }`}
        >
          {trend === "down" ? <ArrowDown size={13} /> : <ArrowUp size={13} />}
          {delta}
          <span className="text-[var(--color-text-faint)] font-normal">from last week</span>
        </p>
      )}
    </div>
  );
}
