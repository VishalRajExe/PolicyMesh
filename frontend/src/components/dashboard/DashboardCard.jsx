import { ArrowRight } from "lucide-react";
import { Link } from "react-router-dom";

export function DashboardCard({ title, action, children, className = "" }) {
  return (
    <div className={`card p-5 flex flex-col ${className}`}>
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-[15px] font-semibold text-white">{title}</h3>
        {action}
      </div>
      {children}
    </div>
  );
}

export function CardFooterLink({ to, children }) {
  return (
    <Link
      to={to}
      className="mt-4 inline-flex items-center gap-1.5 text-sm text-[var(--color-brand)] hover:text-[#8b7dfa] font-medium transition-colors focus-ring w-fit"
    >
      {children}
      <ArrowRight size={14} />
    </Link>
  );
}
