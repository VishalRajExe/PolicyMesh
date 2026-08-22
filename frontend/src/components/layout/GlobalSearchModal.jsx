import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import {
  Search,
  X,
  FileText,
  Boxes,
  GitBranch,
  Activity,
  Link2,
  Sparkles,
  CheckSquare,
  Users,
  Settings,
  ClipboardList,
  ArrowRight,
} from "lucide-react";
import { policiesApi, servicesApi } from "../../api";

const STATIC_ROUTES = [
  { label: "Dashboard", to: "/", icon: Activity, category: "Navigation" },
  { label: "Policies", to: "/policies", icon: FileText, category: "Navigation" },
  { label: "Services", to: "/services", icon: Boxes, category: "Navigation" },
  { label: "Data Flows Graph", to: "/data-flows", icon: GitBranch, category: "Navigation" },
  { label: "Runtime Monitor", to: "/runtime-monitor", icon: Activity, category: "Navigation" },
  { label: "Cryptographic Lineage", to: "/lineage", icon: Link2, category: "Navigation" },
  { label: "AI Classification", to: "/ai-classification", icon: Sparkles, category: "Navigation" },
  { label: "CI Compliance Check", to: "/ci-check", icon: CheckSquare, category: "Navigation" },
  { label: "Compliance Reports", to: "/reports", icon: ClipboardList, category: "Navigation" },
  { label: "Users & Roles", to: "/users-roles", icon: Users, category: "Navigation" },
  { label: "Settings & System Diagnostics", to: "/settings", icon: Settings, category: "Navigation" },
];

export default function GlobalSearchModal({ isOpen, onClose }) {
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const [policies, setPolicies] = useState([]);
  const [services, setServices] = useState([]);
  const inputRef = useRef(null);

  useEffect(() => {
    if (isOpen) {
      setTimeout(() => inputRef.current?.focus(), 50);
      policiesApi.list().then(setPolicies).catch(() => {});
      servicesApi.list().then(setServices).catch(() => {});
    } else {
      setQuery("");
    }
  }, [isOpen]);

  useEffect(() => {
    function handleKeyDown(e) {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        if (isOpen) onClose();
        else {
          // Open handled by parent or custom event
        }
      }
      if (e.key === "Escape" && isOpen) {
        onClose();
      }
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const q = query.trim().toLowerCase();

  const matchingRoutes = STATIC_ROUTES.filter((r) =>
    r.label.toLowerCase().includes(q)
  );

  const matchingPolicies = policies
    .filter(
      (p) =>
        p.policyCode?.toLowerCase().includes(q) ||
        p.name?.toLowerCase().includes(q) ||
        p.jurisdiction?.toLowerCase().includes(q) ||
        p.dataClass?.toLowerCase().includes(q)
    )
    .slice(0, 5);

  const matchingServices = services
    .filter(
      (s) =>
        s.id?.toLowerCase().includes(q) ||
        s.name?.toLowerCase().includes(q) ||
        s.region?.toLowerCase().includes(q)
    )
    .slice(0, 5);

  function handleSelect(path) {
    onClose();
    navigate(path);
  }

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center pt-20 bg-black/70 backdrop-blur-sm p-4 animate-in fade-in duration-150">
      <div className="card w-full max-w-xl shadow-2xl border-[var(--color-border)] overflow-hidden">
        {/* Search Input */}
        <div className="flex items-center gap-3 px-4 py-3.5 border-b border-[var(--color-border)] bg-[var(--color-surface)]">
          <Search size={18} className="text-[var(--color-brand)] shrink-0" />
          <input
            ref={inputRef}
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search policies, services, data flows, routes..."
            className="bg-transparent outline-none text-white placeholder:text-[var(--color-text-faint)] flex-1 text-sm"
          />
          {query && (
            <button
              onClick={() => setQuery("")}
              className="text-[var(--color-text-faint)] hover:text-white p-1"
            >
              <X size={14} />
            </button>
          )}
          <kbd className="text-[10px] text-[var(--color-text-faint)] border border-[var(--color-border)] rounded px-1.5 py-0.5">
            ESC
          </kbd>
        </div>

        {/* Search Results */}
        <div className="max-h-[380px] overflow-y-auto p-2 space-y-3 divide-y divide-[var(--color-border)]/40">
          {/* Matching Policies */}
          {matchingPolicies.length > 0 && (
            <div className="pt-2 first:pt-0 space-y-1">
              <p className="px-3 text-[11px] font-semibold text-[var(--color-text-faint)] uppercase tracking-wider">
                Policies
              </p>
              {matchingPolicies.map((p) => (
                <button
                  key={p.policyCode}
                  onClick={() => handleSelect(`/policies?search=${p.policyCode}`)}
                  className="w-full flex items-center justify-between px-3 py-2 rounded-xl text-left hover:bg-[var(--color-surface-2)] transition-colors group"
                >
                  <div className="flex items-center gap-2.5 min-w-0">
                    <FileText size={15} className="text-purple-400 shrink-0" />
                    <div className="min-w-0">
                      <p className="text-xs font-semibold text-white truncate">{p.policyCode} — {p.name}</p>
                      <p className="text-[11px] text-[var(--color-text-faint)]">
                        {p.jurisdiction} • {p.dataClass} • Status: {p.status}
                      </p>
                    </div>
                  </div>
                  <ArrowRight size={13} className="text-[var(--color-text-faint)] group-hover:text-white shrink-0" />
                </button>
              ))}
            </div>
          )}

          {/* Matching Services */}
          {matchingServices.length > 0 && (
            <div className="pt-2 space-y-1">
              <p className="px-3 text-[11px] font-semibold text-[var(--color-text-faint)] uppercase tracking-wider">
                Services & Nodes
              </p>
              {matchingServices.map((s) => (
                <button
                  key={s.id}
                  onClick={() => handleSelect(`/services?search=${s.id}`)}
                  className="w-full flex items-center justify-between px-3 py-2 rounded-xl text-left hover:bg-[var(--color-surface-2)] transition-colors group"
                >
                  <div className="flex items-center gap-2.5 min-w-0">
                    <Boxes size={15} className="text-blue-400 shrink-0" />
                    <div className="min-w-0">
                      <p className="text-xs font-semibold text-white truncate">{s.id}</p>
                      <p className="text-[11px] text-[var(--color-text-faint)]">{s.name} • Region: {s.region}</p>
                    </div>
                  </div>
                  <ArrowRight size={13} className="text-[var(--color-text-faint)] group-hover:text-white shrink-0" />
                </button>
              ))}
            </div>
          )}

          {/* Navigation Routes */}
          {matchingRoutes.length > 0 && (
            <div className="pt-2 space-y-1">
              <p className="px-3 text-[11px] font-semibold text-[var(--color-text-faint)] uppercase tracking-wider">
                Pages & Navigation
              </p>
              {matchingRoutes.slice(0, 6).map((r) => {
                const Icon = r.icon;
                return (
                  <button
                    key={r.to}
                    onClick={() => handleSelect(r.to)}
                    className="w-full flex items-center justify-between px-3 py-2 rounded-xl text-left hover:bg-[var(--color-surface-2)] transition-colors group"
                  >
                    <div className="flex items-center gap-2.5">
                      <Icon size={15} className="text-[var(--color-brand)] shrink-0" />
                      <span className="text-xs text-white font-medium">{r.label}</span>
                    </div>
                    <ArrowRight size={13} className="text-[var(--color-text-faint)] group-hover:text-white shrink-0" />
                  </button>
                );
              })}
            </div>
          )}

          {matchingRoutes.length === 0 && matchingPolicies.length === 0 && matchingServices.length === 0 && (
            <div className="py-8 text-center text-xs text-[var(--color-text-faint)]">
              No results found for "{query}".
            </div>
          )}
        </div>

        <div className="px-4 py-2 bg-[var(--color-surface-2)] border-t border-[var(--color-border)] text-[11px] text-[var(--color-text-faint)] flex items-center justify-between">
          <span>Navigate with click or Enter</span>
          <span>Press ESC to close</span>
        </div>
      </div>
    </div>
  );
}
