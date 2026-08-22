import { NavLink } from "react-router-dom";
import {
  LayoutGrid,
  FileText,
  Boxes,
  GitBranch,
  Activity,
  Link2,
  Sparkles,
  ClipboardList,
  Bell,
  Users,
  Settings,
  ChevronLeft,
  Shield,
  CheckSquare,
} from "lucide-react";
import { useAuth } from "../../context/AuthContext";

const NAV_ITEMS = [
  { to: "/", label: "Dashboard", icon: LayoutGrid, end: true },
  { to: "/policies", label: "Policies", icon: FileText },
  { to: "/services", label: "Services", icon: Boxes },
  { to: "/data-flows", label: "Data Flows", icon: GitBranch },
  { to: "/runtime-monitor", label: "Runtime Monitor", icon: Activity },
  { to: "/lineage", label: "Lineage", icon: Link2 },
  { to: "/ai-classification", label: "AI Classification", icon: Sparkles },
  { to: "/ci-check", label: "CI Check", icon: CheckSquare },
  { to: "/alerts", label: "Alerts", icon: Bell },
  { to: "/reports", label: "Reports", icon: ClipboardList },
  { to: "/users-roles", label: "Users & Roles", icon: Users },
  { to: "/settings", label: "Settings", icon: Settings },
];

export default function Sidebar({ collapsed, onToggle }) {
  const { user } = useAuth();

  const initials = (user?.email || "CO")
    .split("@")[0]
    .slice(0, 2)
    .toUpperCase();

  return (
    <aside
      className={`hidden lg:flex flex-col shrink-0 border-r border-[var(--color-border)] bg-[var(--color-bg)] transition-all duration-200 ${
        collapsed ? "w-20" : "w-64"
      }`}
    >
      {/* Brand */}
      <div className="flex items-center gap-3 px-5 h-16 border-b border-[var(--color-border)]">
        <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-[#7c6cf9] to-[#5b3df0] flex items-center justify-center shrink-0">
          <Shield size={18} className="text-white" strokeWidth={2.5} />
        </div>
        {!collapsed && (
          <div className="leading-tight">
            <p className="font-semibold text-[15px] text-white">PolicyMesh</p>
            <p className="text-[11px] text-[var(--color-text-faint)]">Govern. Enforce. Trust.</p>
          </div>
        )}
      </div>

      {/* Nav */}
      <nav className="flex-1 overflow-y-auto py-4 px-3 space-y-1">
        {NAV_ITEMS.map(({ to, label, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm transition-colors focus-ring ${
                isActive
                  ? "bg-[var(--color-brand)] text-white font-medium"
                  : "text-[var(--color-text-dim)] hover:bg-[var(--color-surface-2)] hover:text-white"
              }`
            }
            title={collapsed ? label : undefined}
          >
            <Icon size={18} className="shrink-0" />
            {!collapsed && <span className="truncate">{label}</span>}
          </NavLink>
        ))}
      </nav>

      {/* User + collapse */}
      <div className="border-t border-[var(--color-border)] p-3 space-y-2">
        <div className="flex items-center gap-3 rounded-xl px-2 py-2">
          <div className="w-9 h-9 rounded-full bg-[var(--color-surface-2)] border border-[var(--color-border)] flex items-center justify-center text-xs font-semibold text-white shrink-0">
            {initials}
          </div>
          {!collapsed && (
            <div className="leading-tight min-w-0">
              <p className="text-sm font-medium text-white truncate">
                {user?.role ? toTitleCase(user.role) : "Compliance Officer"}
              </p>
              <p className="text-[11px] text-[var(--color-text-faint)] truncate">View all permissions</p>
            </div>
          )}
        </div>
        <button
          onClick={onToggle}
          className="w-full flex items-center gap-2 rounded-xl px-3 py-2 text-sm text-[var(--color-text-dim)] hover:bg-[var(--color-surface-2)] hover:text-white transition-colors focus-ring"
        >
          <ChevronLeft size={16} className={`transition-transform ${collapsed ? "rotate-180" : ""}`} />
          {!collapsed && <span>Collapse</span>}
        </button>
      </div>
    </aside>
  );
}

function toTitleCase(value) {
  return value
    .toLowerCase()
    .split("_")
    .map((w) => w[0]?.toUpperCase() + w.slice(1))
    .join(" ");
}
