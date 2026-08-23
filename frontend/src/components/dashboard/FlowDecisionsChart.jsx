import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import { useTheme } from "../../context/ThemeContext";

export default function FlowDecisionsChart({ data = [] }) {
  const { isDark } = useTheme();

  const gridColor = isDark ? "#1f293d" : "#f1f5f9";
  const textColor = isDark ? "#9ca3af" : "#64748b";
  const tooltipBg = isDark ? "#12161f" : "#ffffff";
  const tooltipBorder = isDark ? "#232939" : "#e2e8f0";

  return (
    <div className="space-y-2">
      {/* Legend header */}
      <div className="flex items-center gap-4 text-xs">
        <div className="flex items-center gap-1.5">
          <span className="w-2.5 h-0.5 bg-[#10b981] rounded-full" />
          <span className="text-[var(--color-text-dim)] text-[11px] font-medium">Allowed</span>
        </div>
        <div className="flex items-center gap-1.5">
          <span className="w-2.5 h-0.5 bg-[#ef4444] rounded-full" />
          <span className="text-[var(--color-text-dim)] text-[11px] font-medium">Blocked</span>
        </div>
      </div>

      <div className="h-44 -ml-4">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke={gridColor} vertical={false} />
            <XAxis
              dataKey="day"
              stroke={textColor}
              tick={{ fill: textColor, fontSize: 10 }}
              axisLine={{ stroke: gridColor }}
              tickLine={false}
            />
            <YAxis
              stroke={textColor}
              tick={{ fill: textColor, fontSize: 10 }}
              axisLine={false}
              tickLine={false}
              width={30}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: tooltipBg,
                borderColor: tooltipBorder,
                borderRadius: 10,
                fontSize: 11,
                boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1)",
                color: isDark ? "#f3f4f6" : "#0f172a",
              }}
              labelStyle={{ fontWeight: "bold", marginBottom: 2 }}
            />
            <Line
              type="monotone"
              dataKey="allowed"
              name="Allowed"
              stroke="#10b981"
              strokeWidth={2}
              dot={{ r: 2.5, fill: "#10b981", strokeWidth: 0 }}
              activeDot={{ r: 4.5 }}
            />
            <Line
              type="monotone"
              dataKey="blocked"
              name="Blocked"
              stroke="#ef4444"
              strokeWidth={2}
              dot={{ r: 2.5, fill: "#ef4444", strokeWidth: 0 }}
              activeDot={{ r: 4.5 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
