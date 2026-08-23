import { FolderOpen } from "lucide-react";
import Button from "./Button";

export function EmptyState({
  icon: Icon = FolderOpen,
  title = "No data available",
  description = "Get started by adding your first item or adjusting your filters.",
  actionLabel,
  onAction,
  actionIcon,
  className = "",
}) {
  return (
    <div className={`flex flex-col items-center justify-center py-10 px-4 text-center ${className}`}>
      <div className="w-12 h-12 rounded-2xl bg-[var(--color-surface-2)] border border-[var(--color-border)] flex items-center justify-center text-[var(--color-text-faint)] mb-3">
        <Icon size={22} />
      </div>
      <h4 className="text-sm font-semibold text-[var(--color-text)] mb-1">{title}</h4>
      {description && (
        <p className="text-xs text-[var(--color-text-dim)] max-w-sm mb-4 leading-relaxed">
          {description}
        </p>
      )}
      {actionLabel && onAction && (
        <Button variant="primary" size="sm" onClick={onAction} icon={actionIcon}>
          {actionLabel}
        </Button>
      )}
    </div>
  );
}

export default EmptyState;
