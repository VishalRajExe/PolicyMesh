export default function ChartLegend({ items }) {
  return (
    <div className="flex items-center gap-5">
      {items.map((item) => (
        <div key={item.label} className="flex items-center gap-2 text-sm text-[var(--color-text-dim)]">
          <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: item.color }} />
          {item.label}
        </div>
      ))}
    </div>
  );
}
