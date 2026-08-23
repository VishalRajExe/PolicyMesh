import { PieChart, Pie, Cell, ResponsiveContainer } from "recharts";

export default function DonutStat({ data = [], total = 0, totalLabel = "Total", size = 150 }) {
  return (
    <div className="flex items-center justify-between gap-4">
      {/* Donut Chart */}
      <div style={{ width: size, height: size }} className="relative shrink-0">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={data}
              dataKey="value"
              nameKey="name"
              innerRadius="68%"
              outerRadius="95%"
              paddingAngle={3}
              stroke="none"
              startAngle={90}
              endAngle={-270}
            >
              {data.map((entry, idx) => (
                <Cell key={`cell-${idx}`} fill={entry.color} />
              ))}
            </Pie>
          </PieChart>
        </ResponsiveContainer>
        <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none text-center">
          <span className="text-xl font-bold text-[var(--color-text)] tracking-tight">{total}</span>
          <span className="text-[10px] text-[var(--color-text-faint)] font-medium leading-none">{totalLabel}</span>
        </div>
      </div>

      {/* Breakdown Legend */}
      <div className="space-y-2 flex-1 min-w-0">
        {data.map((d) => (
          <div key={d.name} className="flex items-center justify-between gap-2 text-xs">
            <div className="flex items-center gap-1.5 min-w-0">
              <span className="w-2 h-2 rounded-full shrink-0" style={{ backgroundColor: d.color }} />
              <span className="text-[var(--color-text-dim)] truncate text-[11px]">{d.name}</span>
            </div>
            <div className="flex items-center gap-1 shrink-0 font-mono text-[11px]">
              <span className="text-[var(--color-text)] font-semibold">{d.value}</span>
              <span className="text-[var(--color-text-faint)] text-[10px]">({d.pct || 0}%)</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
