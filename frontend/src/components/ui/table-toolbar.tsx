import { useEffect, useRef, useState, type ReactNode } from "react";
import { MoreHorizontal, Search, X } from "lucide-react";
import { cn } from "@/lib/utils";
import { strings } from "@/lib/strings";

/**
 * Caseta de căutare de deasupra unui tabel.
 *
 * <p>Mișcări avea filtru pe lună și pe punct de lucru, dar nimic pentru „unde e predarea aia către
 * Hamburger" — singura cale era să derulezi. Căutarea lucrează pe rândurile deja aduse, deci nu
 * mai cere nimic serverului și răspunde la fiecare tastă.
 */
export function TableSearch({
  value,
  onChange,
  placeholder,
  className,
  /** Câte se potrivesc acum, arătat lângă casetă când se caută ceva. */
  matchCount,
}: {
  value: string;
  onChange: (next: string) => void;
  placeholder?: string;
  className?: string;
  matchCount?: number;
}) {
  const inputRef = useRef<HTMLInputElement>(null);

  return (
    <div className={cn("flex items-center gap-2", className)}>
      <div className="relative min-w-0 flex-1 sm:max-w-xs">
        <Search
          className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-content-subtle"
          aria-hidden
        />
        <input
          ref={inputRef}
          type="search"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onKeyDown={(e) => {
            // Escape golește căutarea fără să muți mâna pe maus — reflexul obișnuit.
            if (e.key === "Escape" && value) {
              e.preventDefault();
              onChange("");
            }
          }}
          placeholder={placeholder ?? strings.common.searchPlaceholder}
          aria-label={strings.common.search}
          className="h-10 w-full rounded-md border border-line-strong bg-surface pl-9 pr-9 text-sm placeholder:text-content-subtle focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand [&::-webkit-search-cancel-button]:hidden"
        />
        {value && (
          <button
            type="button"
            onClick={() => {
              onChange("");
              inputRef.current?.focus();
            }}
            aria-label={strings.common.clearSearch}
            className="absolute right-2 top-1/2 -translate-y-1/2 rounded p-1 text-content-subtle transition-colors hover:text-content-muted"
          >
            <X className="h-4 w-4" />
          </button>
        )}
      </div>
      {value && matchCount != null && (
        <span aria-live="polite" className="shrink-0 text-sm text-content-muted">
          {matchCount}
        </span>
      )}
    </div>
  );
}

/**
 * Meniul de acțiuni al unui rând.
 *
 * <p>Pe Mișcări fiecare rând purta până la patru butoane cu text — Cântar, Anexa 3, Editează,
 * Șterge — adică vreo 380px de comenzi repetate pe fiecare rând, într-un tabel care are deja nouă
 * coloane. Acțiunea principală rămâne afară; restul intră aici.
 */
export function RowActions({ children, label }: { children: ReactNode; label?: string }) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    function onDown(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    }
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") setOpen(false);
    }
    document.addEventListener("mousedown", onDown);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDown);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  return (
    <div ref={ref} className="relative inline-block text-left">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        aria-label={label ?? strings.common.moreActions}
        aria-haspopup="menu"
        aria-expanded={open}
        className="rounded-md p-1.5 text-content-muted transition-colors hover:bg-surface-sunken"
      >
        <MoreHorizontal className="h-4 w-4" />
      </button>
      {open && (
        <div
          role="menu"
          // Deschis spre stânga: butonul stă în ultima coloană, lipit de marginea din dreapta.
          className="absolute right-0 z-30 mt-1 w-52 animate-slide-up overflow-hidden rounded-md border border-line bg-surface py-1 shadow-popover"
          onClick={() => setOpen(false)}
        >
          {children}
        </div>
      )}
    </div>
  );
}

/** Un rând din meniul de mai sus. `tone="danger"` pentru fapta care nu se ia înapoi. */
export function RowAction({
  icon: Icon,
  children,
  onClick,
  disabled = false,
  tone = "default",
}: {
  icon?: React.ComponentType<{ className?: string }>;
  children: ReactNode;
  onClick: () => void;
  disabled?: boolean;
  tone?: "default" | "danger";
}) {
  return (
    <button
      type="button"
      role="menuitem"
      onClick={onClick}
      disabled={disabled}
      className={cn(
        "flex w-full items-center gap-2.5 px-3 py-2 text-left text-sm transition-colors disabled:opacity-40",
        tone === "danger"
          ? "text-red-600 hover:bg-red-50"
          : "text-content hover:bg-surface-muted"
      )}
    >
      {Icon && <Icon className="h-4 w-4 shrink-0" />}
      {children}
    </button>
  );
}
