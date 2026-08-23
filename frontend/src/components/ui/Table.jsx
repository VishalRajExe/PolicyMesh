export function Table({ children, className = "" }) {
  return (
    <div className={`overflow-x-auto ${className}`}>
      <table className="w-full text-left text-xs border-collapse">{children}</table>
    </div>
  );
}

export function TableHead({ children, className = "" }) {
  return (
    <thead
      className={`bg-[var(--color-surface-2)] text-[var(--color-text-dim)] border-b border-[var(--color-border)] uppercase tracking-wider font-semibold text-[10px] ${className}`}
    >
      {children}
    </thead>
  );
}

export function TableBody({ children, className = "" }) {
  return <tbody className={`divide-y divide-[var(--color-border)] ${className}`}>{children}</tbody>;
}

export function TableRow({ children, className = "", onClick, hover = true }) {
  return (
    <tr
      onClick={onClick}
      className={`transition-colors ${
        hover ? "hover:bg-[var(--color-surface-2)]/60" : ""
      } ${onClick ? "cursor-pointer" : ""} ${className}`}
    >
      {children}
    </tr>
  );
}

export function TableHeaderCell({ children, className = "", align = "left" }) {
  const alignClass = {
    left: "text-left",
    center: "text-center",
    right: "text-right",
  }[align] || "text-left";

  return <th className={`px-4 py-3 font-semibold ${alignClass} ${className}`}>{children}</th>;
}

export function TableCell({ children, className = "", align = "left" }) {
  const alignClass = {
    left: "text-left",
    center: "text-center",
    right: "text-right",
  }[align] || "text-left";

  return (
    <td className={`px-4 py-3 text-[var(--color-text)] whitespace-nowrap ${alignClass} ${className}`}>
      {children}
    </td>
  );
}

export default Table;
