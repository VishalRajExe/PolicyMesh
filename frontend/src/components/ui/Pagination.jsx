import { ChevronLeft, ChevronRight } from "lucide-react";

export default function Pagination({
  currentPage,
  page,
  totalItems,
  total,
  pageSize = 10,
  onPageChange,
  onPageSizeChange,
  pageSizeOptions = [10, 20, 50, 100],
}) {
  const activePage = page ?? currentPage ?? 1;
  const count = total ?? totalItems ?? 0;
  const totalPages = Math.max(1, Math.ceil(count / pageSize));
  const safePage = Math.min(Math.max(1, activePage), totalPages);

  const startItem = count === 0 ? 0 : (safePage - 1) * pageSize + 1;
  const endItem = Math.min(safePage * pageSize, count);

  function getPageNumbers() {
    const pages = [];
    if (totalPages <= 5) {
      for (let i = 1; i <= totalPages; i++) pages.push(i);
    } else {
      if (safePage <= 3) {
        pages.push(1, 2, 3, 4, "...", totalPages);
      } else if (safePage >= totalPages - 2) {
        pages.push(1, "...", totalPages - 3, totalPages - 2, totalPages - 1, totalPages);
      } else {
        pages.push(1, "...", safePage - 1, safePage, safePage + 1, "...", totalPages);
      }
    }
    return pages;
  }

  if (totalItems <= 0) return null;

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-3 border-t border-[var(--color-border)] text-xs text-[var(--color-text-dim)]">
      <div className="flex items-center gap-3">
        <span>
          Showing <strong className="text-[var(--color-text)] font-semibold">{startItem}</strong>–<strong className="text-[var(--color-text)] font-semibold">{endItem}</strong> of{" "}
          <strong className="text-[var(--color-text)] font-semibold">{totalItems}</strong>
        </span>

        {onPageSizeChange && (
          <div className="flex items-center gap-1.5 ml-2">
            <span className="text-[var(--color-text-faint)]">Rows:</span>
            <select
              value={pageSize}
              onChange={(e) => onPageSizeChange(Number(e.target.value))}
              className="bg-[var(--color-surface)] border border-[var(--color-border)] rounded-lg px-2 py-1 text-[var(--color-text)] text-xs outline-none focus:border-[var(--color-brand)]"
            >
              {pageSizeOptions.map((opt) => (
                <option key={opt} value={opt}>
                  {opt}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      <div className="flex items-center gap-1">
        <button
          onClick={() => onPageChange(safePage - 1)}
          disabled={safePage <= 1}
          className="p-1.5 rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] text-[var(--color-text-dim)] hover:text-[var(--color-text)] hover:border-[var(--color-border-strong)] disabled:opacity-40 disabled:pointer-events-none transition-colors"
          title="Previous Page"
        >
          <ChevronLeft size={14} />
        </button>

        {getPageNumbers().map((page, index) => {
          if (page === "...") {
            return (
              <span key={`ellipsis-${index}`} className="px-2 py-1 text-[var(--color-text-faint)]">
                ...
              </span>
            );
          }
          const isSelected = page === safePage;
          return (
            <button
              key={page}
              onClick={() => onPageChange(page)}
              className={`min-w-[28px] h-7 px-2 rounded-lg text-xs font-medium transition-colors ${
                isSelected
                  ? "bg-[var(--color-brand)] text-white font-semibold shadow-sm"
                  : "bg-[var(--color-surface)] border border-[var(--color-border)] text-[var(--color-text-dim)] hover:text-[var(--color-text)] hover:border-[var(--color-border-strong)]"
              }`}
            >
              {page}
            </button>
          );
        })}

        <button
          onClick={() => onPageChange(safePage + 1)}
          disabled={safePage >= totalPages}
          className="p-1.5 rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] text-[var(--color-text-dim)] hover:text-[var(--color-text)] hover:border-[var(--color-border-strong)] disabled:opacity-40 disabled:pointer-events-none transition-colors"
          title="Next Page"
        >
          <ChevronRight size={14} />
        </button>
      </div>
    </div>
  );
}
