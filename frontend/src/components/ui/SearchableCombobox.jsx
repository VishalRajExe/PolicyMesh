import { useState, useEffect, useRef, useId } from "react";
import { Search, ChevronDown, X, Loader2, RefreshCw, Check } from "lucide-react";

export default function SearchableCombobox({
  value = "",
  onChange,
  options = [],
  placeholder = "Select an option...",
  searchPlaceholder = "Search...",
  loading = false,
  error = null,
  onRetry,
  getOptionLabel = (opt) => (typeof opt === "string" ? opt : opt.name || opt.label || String(opt)),
  getOptionValue = (opt) => (typeof opt === "string" ? opt : opt.name || opt.id || String(opt)),
  renderOption,
  disabled = false,
  className = "",
  id,
}) {
  const [isOpen, setIsOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [highlightedIndex, setHighlightedIndex] = useState(-1);
  const containerRef = useRef(null);
  const inputRef = useRef(null);
  const listboxRef = useRef(null);
  const generatedId = useId();
  const comboboxId = id || generatedId;

  // Find the selected option object if available
  const selectedOption = options.find((opt) => getOptionValue(opt) === value || getOptionLabel(opt) === value);

  // Filter options based on query
  const filteredOptions = options.filter((opt) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    const label = String(getOptionLabel(opt)).toLowerCase();
    const val = String(getOptionValue(opt)).toLowerCase();
    const region = opt.region ? String(opt.region).toLowerCase() : "";
    const env = opt.environment ? String(opt.environment).toLowerCase() : "";
    const desc = opt.description ? String(opt.description).toLowerCase() : "";
    return (
      label.includes(q) ||
      val.includes(q) ||
      region.includes(q) ||
      env.includes(q) ||
      desc.includes(q)
    );
  });

  // Handle click outside to close
  useEffect(() => {
    function handleClickOutside(e) {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setIsOpen(false);
        setSearchQuery("");
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Reset highlight index when filtered options change
  useEffect(() => {
    setHighlightedIndex(filteredOptions.length > 0 ? 0 : -1);
  }, [searchQuery, filteredOptions.length]);

  // Scroll active item into view
  useEffect(() => {
    if (isOpen && highlightedIndex >= 0 && listboxRef.current) {
      const item = listboxRef.current.children[highlightedIndex];
      if (item) {
        item.scrollIntoView({ block: "nearest" });
      }
    }
  }, [highlightedIndex, isOpen]);

  function handleSelect(opt) {
    const val = getOptionValue(opt);
    onChange(val, opt);
    setIsOpen(false);
    setSearchQuery("");
  }

  function handleClear(e) {
    e.stopPropagation();
    onChange("", null);
    setSearchQuery("");
    setIsOpen(false);
  }

  function handleKeyDown(e) {
    if (disabled) return;

    if (!isOpen) {
      if (e.key === "ArrowDown" || e.key === "ArrowUp" || e.key === "Enter" || e.key === " ") {
        e.preventDefault();
        setIsOpen(true);
      }
      return;
    }

    if (e.key === "ArrowDown") {
      e.preventDefault();
      setHighlightedIndex((prev) => (prev < filteredOptions.length - 1 ? prev + 1 : 0));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setHighlightedIndex((prev) => (prev > 0 ? prev - 1 : filteredOptions.length - 1));
    } else if (e.key === "Enter") {
      e.preventDefault();
      if (highlightedIndex >= 0 && highlightedIndex < filteredOptions.length) {
        handleSelect(filteredOptions[highlightedIndex]);
      }
    } else if (e.key === "Escape" || e.key === "Tab") {
      setIsOpen(false);
      setSearchQuery("");
    }
  }

  // Display text in the trigger
  const displayLabel = selectedOption
    ? selectedOption.region
      ? `${getOptionLabel(selectedOption)} (${selectedOption.region})`
      : getOptionLabel(selectedOption)
    : value || "";

  return (
    <div ref={containerRef} className={`relative w-full ${className}`}>
      {/* Combobox Trigger */}
      <div
        id={comboboxId}
        role="combobox"
        aria-expanded={isOpen}
        aria-haspopup="listbox"
        aria-controls={`${comboboxId}-listbox`}
        tabIndex={disabled ? -1 : 0}
        onClick={() => {
          if (!disabled) {
            setIsOpen((prev) => !prev);
            if (!isOpen) {
              setTimeout(() => inputRef.current?.focus(), 50);
            }
          }
        }}
        onKeyDown={handleKeyDown}
        className={`field-input flex items-center justify-between gap-2 cursor-pointer select-none text-xs transition-colors ${
          isOpen ? "border-[var(--color-brand)] ring-1 ring-[var(--color-brand)]" : ""
        } ${disabled ? "opacity-50 cursor-not-allowed pointer-events-none" : ""}`}
      >
        <span className={`truncate flex-1 text-left ${value ? "text-white font-medium" : "text-[var(--color-text-faint)]"}`}>
          {value ? displayLabel : placeholder}
        </span>

        <div className="flex items-center gap-1.5 shrink-0 text-[var(--color-text-faint)]">
          {value && !disabled && (
            <button
              type="button"
              onClick={handleClear}
              className="p-0.5 rounded hover:text-white hover:bg-[var(--color-surface-2)] transition-colors"
              title="Clear selection"
            >
              <X size={13} />
            </button>
          )}
          <ChevronDown
            size={14}
            className={`transition-transform duration-200 ${isOpen ? "rotate-180 text-[var(--color-brand)]" : ""}`}
          />
        </div>
      </div>

      {/* Floating Dropdown Listbox */}
      {isOpen && (
        <div
          id={`${comboboxId}-listbox`}
          role="listbox"
          className="absolute left-0 right-0 top-full mt-1.5 z-40 bg-[var(--color-surface)] border border-[var(--color-border)] rounded-xl shadow-2xl overflow-hidden animate-in fade-in duration-100 backdrop-blur-md"
        >
          {/* Live Search Input */}
          <div className="p-2 border-b border-[var(--color-border)] bg-[var(--color-surface-2)]/60 flex items-center gap-2">
            <Search size={14} className="text-[var(--color-text-faint)] shrink-0 ml-1" />
            <input
              ref={inputRef}
              type="text"
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setHighlightedIndex(0);
              }}
              onKeyDown={handleKeyDown}
              placeholder={searchPlaceholder}
              className="w-full bg-transparent outline-none text-xs text-white placeholder:text-[var(--color-text-faint)]"
              autoComplete="off"
            />
            {searchQuery && (
              <button
                type="button"
                onClick={() => setSearchQuery("")}
                className="text-[var(--color-text-faint)] hover:text-white p-0.5"
              >
                <X size={12} />
              </button>
            )}
          </div>

          {/* Options List */}
          <div ref={listboxRef} className="max-h-60 overflow-y-auto divide-y divide-[var(--color-border)]/30 p-1">
            {loading && (
              <div className="py-6 px-4 text-center text-xs text-[var(--color-text-faint)] flex items-center justify-center gap-2">
                <Loader2 size={14} className="animate-spin text-[var(--color-brand)]" />
                <span>Loading services...</span>
              </div>
            )}

            {!loading && error && (
              <div className="py-4 px-4 text-center text-xs text-[var(--color-bad)]">
                <p className="mb-2">Unable to load services</p>
                {onRetry && (
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      onRetry();
                    }}
                    className="inline-flex items-center gap-1 text-[11px] text-white bg-[var(--color-surface-2)] border border-[var(--color-border)] px-2.5 py-1 rounded-lg hover:border-white"
                  >
                    <RefreshCw size={11} /> Retry
                  </button>
                )}
              </div>
            )}

            {!loading && !error && options.length === 0 && (
              <div className="py-6 px-4 text-center text-xs text-[var(--color-text-faint)]">
                No services registered yet.
              </div>
            )}

            {!loading && !error && options.length > 0 && filteredOptions.length === 0 && (
              <div className="py-6 px-4 text-center text-xs text-[var(--color-text-faint)]">
                No services found matching "{searchQuery}"
              </div>
            )}

            {!loading &&
              !error &&
              filteredOptions.map((opt, idx) => {
                const optVal = getOptionValue(opt);
                const optLabel = getOptionLabel(opt);
                const isSelected = optVal === value || optLabel === value;
                const isHighlighted = idx === highlightedIndex;

                if (renderOption) {
                  return (
                    <div
                      key={optVal || idx}
                      role="option"
                      aria-selected={isSelected}
                      onClick={() => handleSelect(opt)}
                      onMouseEnter={() => setHighlightedIndex(idx)}
                      className={`cursor-pointer transition-colors ${
                        isHighlighted ? "bg-[var(--color-surface-2)]" : ""
                      }`}
                    >
                      {renderOption(opt, { isSelected, isHighlighted })}
                    </div>
                  );
                }

                return (
                  <div
                    key={optVal || idx}
                    role="option"
                    aria-selected={isSelected}
                    onClick={() => handleSelect(opt)}
                    onMouseEnter={() => setHighlightedIndex(idx)}
                    className={`px-3 py-2.5 rounded-lg flex items-center justify-between gap-3 cursor-pointer text-xs transition-colors ${
                      isSelected
                        ? "bg-[var(--color-brand)]/15 text-white"
                        : isHighlighted
                        ? "bg-[var(--color-surface-2)] text-white"
                        : "text-[var(--color-text-dim)] hover:text-white"
                    }`}
                  >
                    <div className="min-w-0 flex items-center gap-2">
                      <span className="font-semibold font-mono text-white truncate">{optLabel}</span>
                      {opt.region && (
                        <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-blue-500/15 text-blue-400 border border-blue-500/30">
                          {opt.region}
                        </span>
                      )}
                      {opt.environment && (
                        <span className="text-[10px] text-[var(--color-text-faint)] hidden sm:inline">
                          • {opt.environment}
                        </span>
                      )}
                    </div>

                    {isSelected && (
                      <Check size={14} className="text-[var(--color-brand)] shrink-0" />
                    )}
                  </div>
                );
              })}
          </div>
        </div>
      )}
    </div>
  );
}
