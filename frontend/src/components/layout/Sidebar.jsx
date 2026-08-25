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
  ChevronRight,
  Shield,
  CheckSquare,
  Server,
  User,
  ExternalLink,
  GitPullRequest,
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
  { to: "/reports", label: "Reports", icon: ClipboardList },
  { to: "/alerts", label: "Alerts", icon: Bell },
  { to: "/ci-check", label: "CI Check", icon: CheckSquare },
  { to: "/github", label: "GitHub Scans", icon: GitPullRequest },
  { to: "/system", label: "System Status", icon: Server },
  { to: "/users-roles", label: "Users & Roles", icon: Users },
  { to: "/settings", label: "Settings", icon: Settings },
];

export default function Sidebar({ collapsed, onToggle, mobileOpen, onMobileClose }) {
  const { user } = useAuth();

  const initials = (user?.email || "CO")
    .split("@")[0]
    .slice(0, 2)
    .toUpperCase();

  const roleLabel = user?.role ? toTitleCase(user.role) : "Compliance Officer";

  const content = (
    <div className="flex flex-col h-full bg-[var(--color-surface)] border-r border-[var(--color-border)] select-none">
      {/* Brand Logo & Tagline */}
      <div className="flex items-center justify-between px-5 h-18 border-b border-[var(--color-border)] shrink-0">
        <div className="flex items-center gap-3 min-w-0">
          <div className="w-9 h-9 rounded-xl bg-[var(--color-brand-light)] border border-[var(--color-brand)]/20 flex items-center justify-center overflow-hidden shrink-0 shadow-sm p-1">
            <img
              src="/logo.png"
              alt="PolicyMesh Logo"
              className="w-full h-full object-contain"
              onError={(e) => {
                e.currentTarget.style.display = "none";
                if (e.currentTarget.nextElementSibling) {
                  e.currentTarget.nextElementSibling.style.display = "flex";
                }
              }}
            />
            <div style={{ display: "none" }} className="w-full h-full items-center justify-center text-[var(--color-brand)]">
              <Shield size={20} className="fill-[var(--color-brand)]/20 stroke-[var(--color-brand)]" />
            </div>
          </div>
          {!collapsed && (
            <div className="leading-tight min-w-0">
              <p className="font-bold text-sm tracking-tight text-[var(--color-text)]">PolicyMesh</p>
              <p className="text-[11px] text-[var(--color-text-faint)] font-medium">Govern. Enforce. Trust.</p>
            </div>
          )}
        </div>

        {/* Mobile Close Button (only visible on mobile drawer) */}
        {mobileOpen && (
          <button
            type="button"
            onClick={onMobileClose}
            className="lg:hidden p-1.5 rounded-lg text-[var(--color-text-faint)] hover:text-[var(--color-text)] hover:bg-[var(--color-surface-2)] transition-colors focus-ring"
            title="Close menu"
          >
            <ChevronLeft size={18} />
          </button>
        )}
      </div>

      {/* Navigation Items */}
      <nav className="flex-1 overflow-y-auto py-3 px-3 space-y-0.5">
        {NAV_ITEMS.map(({ to, label, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            onClick={() => onMobileClose && onMobileClose()}
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-xl px-3 py-2.5 text-xs font-medium transition-all duration-150 focus-ring ${
                isActive
                  ? "bg-[var(--color-brand-light)] text-[var(--color-brand-text)] font-semibold shadow-xs"
                  : "text-[var(--color-text-dim)] hover:bg-[var(--color-surface-2)] hover:text-[var(--color-text)]"
              }`
            }
            title={collapsed ? label : undefined}
          >
            <Icon
              size={17}
              className="shrink-0 transition-transform duration-150"
            />
            {!collapsed && <span className="truncate">{label}</span>}
          </NavLink>
        ))}
      </nav>

      {/* User Card & Collapse Button */}
      <div className="border-t border-[var(--color-border)] p-3 space-y-2 shrink-0 bg-[var(--color-surface-2)]/30">
        <NavLink
          to="/settings"
          onClick={() => onMobileClose && onMobileClose()}
          className="flex items-center justify-between gap-2.5 rounded-xl p-2 hover:bg-[var(--color-surface-2)] border border-transparent hover:border-[var(--color-border)] transition-all group"
        >
          <div className="flex items-center gap-2.5 min-w-0">
            <div className="w-8 h-8 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-xs font-semibold text-white shrink-0 shadow-xs">
              {initials}
            </div>
            {!collapsed && (
              <div className="leading-tight min-w-0">
                <p className="text-xs font-semibold text-[var(--color-text)] truncate">{roleLabel}</p>
                <p className="text-[10px] text-[var(--color-text-faint)] truncate group-hover:text-[var(--color-brand)]">
                  View all permissions
                </p>
              </div>
            )}
          </div>
          {!collapsed && (
            <ChevronRight size={14} className="text-[var(--color-text-faint)] group-hover:text-[var(--color-text)] shrink-0" />
          )}
        </NavLink>

        <button
          type="button"
          onClick={onToggle}
          className="w-full hidden lg:flex items-center justify-center gap-2 rounded-lg py-1.5 text-xs text-[var(--color-text-faint)] hover:text-[var(--color-text)] hover:bg-[var(--color-surface-2)] transition-colors focus-ring"
          title={collapsed ? "Expand sidebar" : "Collapse sidebar"}
        >
          <ChevronLeft
            size={15}
            className={`transition-transform duration-200 ${collapsed ? "rotate-180" : ""}`}
          />
          {!collapsed && <span className="text-[11px]">Collapse</span>}
        </button>
      </div>
    </div>
  );

  return (
    <>
      {/* Desktop Sidebar */}
      <aside
        className={`hidden lg:block shrink-0 transition-all duration-200 ${
          collapsed ? "w-18" : "w-60"
        }`}
      >
        {content}
      </aside>

      {/* Mobile Drawer */}
      {mobileOpen && (
        <div className="fixed inset-0 z-50 lg:hidden flex">
          <div
            className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs transition-opacity"
            onClick={onMobileClose}
          />
          <div className="relative w-64 max-w-[80vw] h-full shadow-2xl z-10 animate-in slide-in-from-left duration-200">
            {content}
          </div>
        </div>
      )}
    </>
  );
}

function toTitleCase(value) {
  return value
    .toLowerCase()
    .split("_")
    .map((w) => w[0]?.toUpperCase() + w.slice(1))
    .join(" ");
}
