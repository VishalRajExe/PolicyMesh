import { PieChart, Pie, Cell, ResponsiveContainer } from "recharts";

export default function DonutStat({ data, total, totalLabel, size = 200 }) {
  return (
    <div className="flex items-center gap-6 flex-wrap">
      <div style={{ width: size, height: size }} className="relative shrink-0">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={data}
              dataKey="value"
              nameKey="name"
              innerRadius="68%"
              outerRadius="100%"
              paddingAngle={2}
              stroke="none"
            >
              {data.map((entry) => (
                <Cell key={entry.name} fill={entry.color} />
              ))}
            </Pie>
          </PieChart>
        </ResponsiveContainer>
        <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
          <span className="text-2xl font-bold text-white">{total}</span>
          <span className="text-xs text-[var(--color-text-faint)]">{totalLabel}</span>
        </div>
      </div>

      <div className="space-y-2.5 min-w-0">
        {data.map((d) => (
          <div key={d.name} className="flex items-center gap-2 text-sm">
            <span className="w-2.5 h-2.5 rounded-full shrink-0" style={{ backgroundColor: d.color }} />
            <span className="text-[var(--color-text-dim)] flex-1">{d.name}</span>
            <span className="text-white font-medium">{d.value}</span>
            <span className="text-[var(--color-text-faint)] text-xs">({d.pct}%)</span>
          </div>
        ))}
      </div>
    </div>
  );
}
