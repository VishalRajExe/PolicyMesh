import { useState, useEffect } from "react";
import { Search, Bell, LogOut, Upload, Plus } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import GlobalSearchModal from "./GlobalSearchModal";

export default function Topbar({ title, subtitle, alertCount = 0 }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [searchModalOpen, setSearchModalOpen] = useState(false);
  const initials = (user?.email || "CO").split("@")[0].slice(0, 2).toUpperCase();

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

  return (
    <>
      <header className="flex items-center justify-between gap-4 px-6 lg:px-8 h-20 shrink-0">
        <div className="min-w-0">
          <h1 className="text-2xl font-semibold text-white truncate">{title}</h1>
          {subtitle && <p className="text-sm text-[var(--color-text-dim)] mt-0.5">{subtitle}</p>}
        </div>

        <div className="flex items-center gap-3 shrink-0">
          <button
            type="button"
            onClick={() => setSearchModalOpen(true)}
            className="hidden md:flex items-center gap-2 bg-[var(--color-surface)] border border-[var(--color-border)] rounded-xl px-3.5 py-2.5 w-64 hover:border-[var(--color-brand)]/50 transition-colors text-left"
          >
            <Search size={16} className="text-[var(--color-text-faint)]" />
            <span className="text-sm text-[var(--color-text-faint)] flex-1 min-w-0">
              Search anything...
            </span>
            <kbd className="text-[10px] text-[var(--color-text-faint)] border border-[var(--color-border)] rounded px-1.5 py-0.5">
              Ctrl + K
            </kbd>
          </button>

          <button
            onClick={() => navigate("/alerts")}
            className="relative w-10 h-10 rounded-xl bg-[var(--color-surface)] border border-[var(--color-border)] flex items-center justify-center hover:bg-[var(--color-surface-2)] transition-colors focus-ring"
            title="Alerts"
          >
            <Bell size={17} className="text-[var(--color-text-dim)]" />
            {alertCount > 0 && (
              <span className="absolute -top-1.5 -right-1.5 min-w-[18px] h-[18px] px-1 rounded-full bg-[var(--color-bad)] text-white text-[10px] font-semibold flex items-center justify-center">
                {alertCount}
              </span>
            )}
          </button>

          <div className="flex items-center gap-2 rounded-xl pl-1 pr-2 py-1 bg-[var(--color-surface)] border border-[var(--color-border)]">
            <div className="w-8 h-8 rounded-full bg-gradient-to-br from-[#7c6cf9] to-[#5b3df0] flex items-center justify-center text-xs font-semibold text-white">
              {initials}
            </div>
            <div className="hidden sm:block text-left min-w-0">
              <p className="text-xs font-medium text-white leading-tight truncate max-w-[100px]">
                {user?.email?.split("@")[0] || "User"}
              </p>
              <p className="text-[10px] text-[var(--color-text-faint)] leading-tight">{user?.role || "VIEWER"}</p>
            </div>
            <button
              onClick={handleLogout}
              title="Sign out"
              className="ml-1 w-7 h-7 rounded-lg flex items-center justify-center text-[var(--color-text-faint)] hover:text-[var(--color-bad)] hover:bg-[var(--color-bad)]/10 transition-colors"
            >
              <LogOut size={13} />
            </button>
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

export function TopbarActions({ onImport, onCreate, createLabel = "New Policy" }) {
  return (
    <div className="flex items-center gap-3">
      {onImport && (
        <button
          onClick={onImport}
          className="flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-medium border border-[var(--color-border)] text-[var(--color-text-dim)] hover:text-white hover:bg-[var(--color-surface-2)] transition-colors focus-ring"
        >
          <Upload size={15} />
          Import Policy
        </button>
      )}
      {onCreate && (
        <button
          onClick={onCreate}
          className="flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-medium bg-[var(--color-brand)] text-white hover:bg-[var(--color-brand-dim)] transition-colors focus-ring"
        >
          <Plus size={15} />
          {createLabel}
        </button>
      )}
    </div>
  );
}
