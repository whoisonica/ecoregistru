import { useEffect, useRef, useState } from "react";
import { Check, ChevronsUpDown, Loader2, X } from "lucide-react";
import { cn } from "@/lib/utils";

export interface ComboboxItem {
  id: string;
  /** Primary label shown in the trigger and list. */
  label: string;
  /** Optional secondary line shown under the label in the list. */
  sublabel?: string;
}

interface ComboboxProps {
  value: ComboboxItem | null;
  onSelect: (item: ComboboxItem | null) => void;
  /** Called (debounced) as the user types, so the parent can fetch matches. */
  onQueryChange: (query: string) => void;
  items: ComboboxItem[];
  loading?: boolean;
  disabled?: boolean;
  placeholder?: string;
  searchPlaceholder?: string;
  emptyText?: string;
  id?: string;
}

/**
 * Searchable select: a trigger that opens a popover with a text input and a
 * results list. Data-agnostic — the parent supplies `items` and reacts to
 * `onQueryChange` (debounced here by ~250ms) to fetch them (e.g. waste codes).
 */
export function Combobox({
  value,
  onSelect,
  onQueryChange,
  items,
  loading = false,
  disabled = false,
  placeholder = "Selectează…",
  searchPlaceholder = "Caută…",
  emptyText = "Niciun rezultat.",
  id,
}: ComboboxProps) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Debounce the query pushed up to the parent so we don't fetch on every keystroke.
  useEffect(() => {
    const handle = window.setTimeout(() => onQueryChange(query), 250);
    return () => window.clearTimeout(handle);
  }, [query, onQueryChange]);

  // Close on outside click.
  useEffect(() => {
    if (!open) return;
    function onDown(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", onDown);
    return () => document.removeEventListener("mousedown", onDown);
  }, [open]);

  // Focus the search box when opening.
  useEffect(() => {
    if (open) inputRef.current?.focus();
  }, [open]);

  function choose(item: ComboboxItem) {
    onSelect(item);
    setOpen(false);
    setQuery("");
  }

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        id={id}
        disabled={disabled}
        onClick={() => setOpen((o) => !o)}
        className={cn(
          "flex h-10 w-full items-center justify-between rounded-md border border-gray-300 bg-white px-3 py-2 text-left text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand disabled:opacity-50",
          !value && "text-gray-400"
        )}
      >
        <span className="truncate">{value ? value.label : placeholder}</span>
        <span className="ml-2 flex shrink-0 items-center gap-1">
          {value && (
            <X
              className="h-4 w-4 text-gray-400 hover:text-gray-600"
              onClick={(e) => {
                e.stopPropagation();
                onSelect(null);
              }}
            />
          )}
          <ChevronsUpDown className="h-4 w-4 text-gray-400" />
        </span>
      </button>

      {open && (
        <div className="absolute z-50 mt-1 w-full rounded-md border border-gray-200 bg-white shadow-lg">
          <div className="border-b border-gray-100 p-2">
            <input
              ref={inputRef}
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder={searchPlaceholder}
              className="w-full rounded border border-gray-200 px-2 py-1.5 text-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-brand"
            />
          </div>
          <div className="max-h-56 overflow-y-auto py-1">
            {loading && (
              <div className="flex items-center gap-2 px-3 py-2 text-sm text-gray-400">
                <Loader2 className="h-4 w-4 animate-spin" />
                Se caută…
              </div>
            )}
            {!loading && items.length === 0 && (
              <div className="px-3 py-2 text-sm text-gray-400">{emptyText}</div>
            )}
            {!loading &&
              items.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => choose(item)}
                  className="flex w-full items-start justify-between gap-2 px-3 py-2 text-left text-sm hover:bg-gray-50"
                >
                  <span>
                    <span className="block text-gray-900">{item.label}</span>
                    {item.sublabel && (
                      <span className="block text-xs text-gray-400">{item.sublabel}</span>
                    )}
                  </span>
                  {value?.id === item.id && (
                    <Check className="mt-0.5 h-4 w-4 shrink-0 text-brand" />
                  )}
                </button>
              ))}
          </div>
        </div>
      )}
    </div>
  );
}
