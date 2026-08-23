import { useState, useEffect, useRef } from "react";
import {
  Search,
  Bell,
  LogOut,
  Upload,
  Plus,
  Sun,
  Moon,
  Monitor,
  Menu,
  ChevronDown,
  User,
  Settings as SettingsIcon,
  Shield,
} from "lucide-react";
import { useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { useTheme } from "../../context/ThemeContext";
import GlobalSearchModal from "./GlobalSearchModal";
import Button from "../ui/Button";

export default function Topbar({
  title,
  subtitle,
  alertCount = 3,
  actions,
  onMobileMenuToggle,
}) {
  const { user, logout } = useAuth();
  const { theme, setTheme, isDark } = useTheme();
  const navigate = useNavigate();
  const location = useLocation();

  const [searchModalOpen, setSearchModalOpen] = useState(false);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const [themeMenuOpen, setThemeMenuOpen] = useState(false);

  const userMenuRef = useRef(null);
  const themeMenuRef = useRef(null);

  const initials = (user?.email || "CO").split("@")[0].slice(0, 2).toUpperCase();
  const roleName = user?.role ? toTitleCase(user.role) : "Compliance Officer";

  // Close menus on outside click
  useEffect(() => {
    function handleClickOutside(e) {
      if (userMenuRef.current && !userMenuRef.current.contains(e.target)) {
        setUserMenuOpen(false);
      }
      if (themeMenuRef.current && !themeMenuRef.current.contains(e.target)) {
        setThemeMenuOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Global Ctrl+K / Cmd+K listener
  useEffect(() => {
    function handleKeyDown(e) {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        setSearchModalOpen(true);
      }
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, []);

  function handleLogout() {
    logout();
    navigate("/login");
  }

  // Default page title on Dashboard is "Welcome back, {Role} 👋"
  const isDashboard = location.pathname === "/";
  const displayTitle = title || (isDashboard ? `Welcome back, ${roleName} 👋` : "PolicyMesh Control Center");
  const displaySubtitle = subtitle || (isDashboard ? "Here's what's happening with your data governance today." : null);

  return (
    <>
      <header className="flex items-center justify-between gap-4 px-6 lg:px-8 py-4.5 bg-[var(--color-surface)] border-b border-[var(--color-border)] shrink-0 select-none">
        {/* Left: Mobile Menu Trigger + Title / Subtitle */}
        <div className="flex items-center gap-3 min-w-0">
          {onMobileMenuToggle && (
            <button
              type="button"
              onClick={onMobileMenuToggle}
              className="lg:hidden p-2 rounded-xl border border-[var(--color-border)] text-[var(--color-text-dim)] hover:text-[var(--color-text)] hover:bg-[var(--color-surface-2)] focus-ring"
            >
              <Menu size={18} />
            </button>
          )}

          <div className="min-w-0">
            <h1 className="text-lg lg:text-xl font-bold tracking-tight text-[var(--color-text)] truncate">
              {displayTitle}
            </h1>
            {displaySubtitle && (
              <p className="text-xs text-[var(--color-text-dim)] mt-0.5 truncate hidden sm:block">
                {displaySubtitle}
              </p>
            )}
          </div>
        </div>

        {/* Right: Search, Actions, Notifications, Theme, Profile */}
        <div className="flex items-center gap-2.5 shrink-0">
          {/* Global Search Bar */}
          <button
            type="button"
            onClick={() => setSearchModalOpen(true)}
            className="hidden md:flex items-center gap-2.5 bg-[var(--color-surface-2)]/80 hover:bg-[var(--color-surface-2)] border border-[var(--color-border)] rounded-xl px-3 py-1.5 w-60 transition-all text-left group focus-ring"
          >
            <Search size={14} className="text-[var(--color-text-faint)] group-hover:text-[var(--color-text-dim)]" />
            <span className="text-xs text-[var(--color-text-faint)] flex-1 min-w-0 truncate">
              Search anything...
            </span>
            <kbd className="text-[10px] text-[var(--color-text-faint)] border border-[var(--color-border)] rounded px-1.5 py-0.2 bg-[var(--color-surface)] shadow-2xs font-mono">
              Ctrl + K
            </kbd>
          </button>

          {/* Page Actions Slot (if supplied) */}
          {actions && <div className="hidden sm:flex items-center gap-2">{actions}</div>}

          {/* Theme Switcher */}
          <div className="relative" ref={themeMenuRef}>
            <button
              type="button"
              onClick={() => setThemeMenuOpen((v) => !v)}
              className="w-9 h-9 rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] hover:bg-[var(--color-surface-2)] text-[var(--color-text-dim)] hover:text-[var(--color-text)] flex items-center justify-center transition-colors focus-ring"
              title={`Theme: ${theme}`}
            >
              {theme === "dark" ? <Moon size={15} /> : theme === "light" ? <Sun size={15} /> : <Monitor size={15} />}
            </button>

            {themeMenuOpen && (
              <div className="absolute right-0 mt-1.5 w-36 bg-[var(--color-surface)] border border-[var(--color-border)] rounded-xl shadow-xl p-1 z-50 animate-in fade-in duration-100">
                <button
                  onClick={() => {
                    setTheme("light");
                    setThemeMenuOpen(false);
                  }}
                  className={`w-full flex items-center gap-2 px-2.5 py-1.5 text-xs rounded-lg transition-colors ${
                    theme === "light"
                      ? "bg-[var(--color-brand-light)] text-[var(--color-brand-text)] font-semibold"
                      : "text-[var(--color-text-dim)] hover:bg-[var(--color-surface-2)] hover:text-[var(--color-text)]"
                  }`}
                >
                  <Sun size={14} /> Light
                </button>
                <button
                  onClick={() => {
                    setTheme("dark");
                    setThemeMenuOpen(false);
                  }}
                  className={`w-full flex items-center gap-2 px-2.5 py-1.5 text-xs rounded-lg transition-colors ${
                    theme === "dark"
                      ? "bg-[var(--color-brand-light)] text-[var(--color-brand-text)] font-semibold"
                      : "text-[var(--color-text-dim)] hover:bg-[var(--color-surface-2)] hover:text-[var(--color-text)]"
                  }`}
                >
                  <Moon size={14} /> Dark
                </button>
                <button
                  onClick={() => {
                    setTheme("system");
                    setThemeMenuOpen(false);
                  }}
                  className={`w-full flex items-center gap-2 px-2.5 py-1.5 text-xs rounded-lg transition-colors ${
                    theme === "system"
                      ? "bg-[var(--color-brand-light)] text-[var(--color-brand-text)] font-semibold"
                      : "text-[var(--color-text-dim)] hover:bg-[var(--color-surface-2)] hover:text-[var(--color-text)]"
                  }`}
                >
                  <Monitor size={14} /> System
                </button>
              </div>
            )}
          </div>

          {/* Notifications Bell */}
          <button
            type="button"
            onClick={() => navigate("/alerts")}
            className="relative w-9 h-9 rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] hover:bg-[var(--color-surface-2)] text-[var(--color-text-dim)] hover:text-[var(--color-text)] flex items-center justify-center transition-colors focus-ring"
            title="Recent Alerts"
          >
            <Bell size={15} />
            {alertCount > 0 && (
              <span className="absolute -top-1 -right-1 min-w-[17px] h-[17px] px-1 rounded-full bg-[var(--color-bad)] text-white text-[9px] font-bold flex items-center justify-center shadow-xs">
                {alertCount}
              </span>
            )}
          </button>

          {/* User Profile Pill & Dropdown */}
          <div className="relative" ref={userMenuRef}>
            <button
              type="button"
              onClick={() => setUserMenuOpen((v) => !v)}
              className="flex items-center gap-2 pl-1 pr-2 py-1 rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] hover:bg-[var(--color-surface-2)] transition-colors focus-ring"
            >
              <div className="w-7 h-7 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-[11px] font-bold text-white shadow-2xs">
                {initials}
              </div>
              <ChevronDown
                size={13}
                className={`text-[var(--color-text-faint)] transition-transform duration-150 ${
                  userMenuOpen ? "rotate-180" : ""
                }`}
              />
            </button>

            {userMenuOpen && (
              <div className="absolute right-0 mt-1.5 w-52 bg-[var(--color-surface)] border border-[var(--color-border)] rounded-xl shadow-xl p-1.5 z-50 animate-in fade-in duration-100 divide-y divide-[var(--color-border)]/50">
                <div className="px-3 py-2">
                  <p className="text-xs font-bold text-[var(--color-text)] truncate">
                    {user?.email?.split("@")[0] || "User"}
                  </p>
                  <p className="text-[11px] text-[var(--color-text-faint)] truncate">{user?.email || ""}</p>
                  <span className="inline-block mt-1 text-[10px] font-semibold px-1.5 py-0.5 rounded bg-[var(--color-brand-light)] text-[var(--color-brand-text)] border border-[var(--color-brand)]/20">
                    {roleName}
                  </span>
                </div>

                <div className="py-1">
                  <button
                    onClick={() => {
                      navigate("/settings");
                      setUserMenuOpen(false);
                    }}
                    className="w-full flex items-center gap-2 px-2.5 py-1.5 text-xs text-[var(--color-text-dim)] hover:text-[var(--color-text)] hover:bg-[var(--color-surface-2)] rounded-lg transition-colors"
                  >
                    <SettingsIcon size={14} /> Settings & Profile
                  </button>
                  <button
                    onClick={() => {
                      navigate("/system");
                      setUserMenuOpen(false);
                    }}
                    className="w-full flex items-center gap-2 px-2.5 py-1.5 text-xs text-[var(--color-text-dim)] hover:text-[var(--color-text)] hover:bg-[var(--color-surface-2)] rounded-lg transition-colors"
                  >
                    <Shield size={14} /> System Health
                  </button>
                </div>

                <div className="pt-1">
                  <button
                    onClick={() => {
                      setUserMenuOpen(false);
                      handleLogout();
                    }}
                    className="w-full flex items-center gap-2 px-2.5 py-1.5 text-xs text-[var(--color-bad)] hover:bg-[var(--color-bad-light)] rounded-lg transition-colors"
                  >
                    <LogOut size={14} /> Sign out
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </header>

      <GlobalSearchModal
        isOpen={searchModalOpen}
        onClose={() => setSearchModalOpen(false)}
      />
    </>
  );
}

export function TopbarActions({ onImport, onCreate, createLabel = "New Policy", importLabel = "Import Policy" }) {
  return (
    <div className="flex items-center gap-2">
      {onImport && (
        <Button variant="secondary" size="md" onClick={onImport} icon={Upload}>
          {importLabel}
        </Button>
      )}
      {onCreate && (
        <Button variant="primary" size="md" onClick={onCreate} icon={Plus}>
          {createLabel}
        </Button>
      )}
    </div>
  );
}

function toTitleCase(value) {
  return value
    .toLowerCase()
    .split("_")
    .map((w) => w[0]?.toUpperCase() + w.slice(1))
    .join(" ");
}
