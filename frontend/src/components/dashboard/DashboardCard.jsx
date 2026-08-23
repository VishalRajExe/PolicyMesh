import { ArrowRight } from "lucide-react";
import { Link } from "react-router-dom";

export function DashboardCard({ title, action, children, className = "", noPadding = false }) {
  return (
    <div className={`card flex flex-col justify-between ${noPadding ? "" : "p-5"} ${className}`}>
      <div>
        <div className="flex items-center justify-between gap-3 mb-4">
          <h3 className="text-sm font-bold tracking-tight text-[var(--color-text)]">{title}</h3>
          {action && <div className="shrink-0">{action}</div>}
        </div>
        {children}
      </div>
    </div>
  );
}

export function CardFooterLink({ to, children }) {
  return (
    <Link
      to={to}
      className="mt-4 pt-2 border-t border-[var(--color-border)]/50 inline-flex items-center gap-1.5 text-xs text-[var(--color-brand)] hover:text-[var(--color-brand-dim)] font-medium transition-colors focus-ring w-full group"
    >
      <span>{children}</span>
      <ArrowRight size={13} className="transition-transform group-hover:translate-x-0.5" />
    </Link>
  );
}

export default DashboardCard;
