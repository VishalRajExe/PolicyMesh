import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";

export default function FlowDecisionsChart({ data }) {
  return (
    <div className="h-64 -ml-2">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#1d2432" vertical={false} />
          <XAxis
            dataKey="day"
            stroke="#5b6478"
            tick={{ fill: "#8b93a7", fontSize: 12 }}
            axisLine={{ stroke: "#232939" }}
            tickLine={false}
          />
          <YAxis
            stroke="#5b6478"
            tick={{ fill: "#8b93a7", fontSize: 12 }}
            axisLine={false}
            tickLine={false}
          />
          <Tooltip
            contentStyle={{
              backgroundColor: "#171c27",
              border: "1px solid #232939",
              borderRadius: 12,
              fontSize: 12,
            }}
            labelStyle={{ color: "#e7e9ee" }}
          />
          <Line
            type="monotone"
            dataKey="allowed"
            name="Allowed"
            stroke="#22c55e"
            strokeWidth={2.5}
            dot={{ r: 3, fill: "#22c55e", strokeWidth: 0 }}
            activeDot={{ r: 5 }}
          />
          <Line
            type="monotone"
            dataKey="blocked"
            name="Blocked"
            stroke="#ef4444"
            strokeWidth={2.5}
            dot={{ r: 3, fill: "#ef4444", strokeWidth: 0 }}
            activeDot={{ r: 5 }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
