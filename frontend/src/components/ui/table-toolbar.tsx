import { useRef, type ComponentProps, type ReactNode } from "react";
import { Search, X } from "lucide-react";
import { cn } from "@/lib/utils";
import { strings } from "@/lib/strings";
import { Pagination } from "@/components/ui/table";
import { Menu, MenuItem } from "@/components/ui/menu";
import type { TableViewControls } from "@/hooks/useTableView";

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
          // Cârligul după care scurtătura `/` găsește caseta ecranului curent.
          data-table-search
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
 *
 * <p>Comportamentul stă de-acum în `ui/menu.tsx`, fiindcă antetul de pe Evidențe are nevoie de
 * același meniu cu o etichetă în loc de „⋯". Cele două nume rămân aici: un „meniu de rând" e ce
 * caută cineva care citește un tabel.
 */
export function RowActions({ children }: { children: ReactNode }) {
  return <Menu>{children}</Menu>;
}

/** Un rând din meniul de mai sus. `tone="danger"` pentru fapta care nu se ia înapoi. */
export function RowAction(props: ComponentProps<typeof MenuItem>) {
  return <MenuItem {...props} />;
}

/**
 * Pragul de la care o casetă de căutare ajută mai mult decât încurcă.
 *
 * <p>Sub el, caseta e zgomot: pe un tabel cu patru puncte de lucru se vede totul dintr-o privire,
 * iar un câmp de căutare deasupra doar ocupă locul. Peste el, derularea începe să coste.
 *
 * <p>Pragul e o singură decizie, aici, nu un „arată sau nu" hotărât separat la fiecare tabel —
 * exact felul în care aplicația ajunsese să aibă unsprezece tabele și o singură bară de căutare.
 */
const SEARCH_FROM = 10;

/**
 * Bara de deasupra unui tabel: căutarea, plus ce mai vrea pagina să pună lângă ea.
 *
 * <p>Rămâne pe ecran cât timp se caută ceva, chiar dacă rezultatele au scăzut sub prag: altfel
 * caseta ar dispărea sub degetul care tocmai a tastat în ea.
 */
export function TableToolbar({
  view,
  placeholder,
  children,
  className,
}: {
  view: TableViewControls;
  placeholder?: string;
  /** Filtre proprii ale paginii, așezate lângă căutare. */
  children?: ReactNode;
  className?: string;
}) {
  const showSearch = view.totalCount >= SEARCH_FROM || view.query !== "";
  if (!showSearch && !children) return null;
  return (
    <div className={cn("mb-3 flex flex-wrap items-center gap-2", className)}>
      {showSearch && (
        <TableSearch
          value={view.query}
          onChange={view.search}
          placeholder={placeholder}
          matchCount={view.matchCount}
          className="min-w-0 flex-1"
        />
      )}
      {children}
    </div>
  );
}

/** Paginarea de sub tabel. Se ascunde singură pe o pagină unică. */
export function TablePagination({ view }: { view: TableViewControls }) {
  return (
    <Pagination
      page={view.page}
      pageCount={view.pageCount}
      onPage={view.setPage}
      matchCount={view.matchCount}
      pageSize={view.pageSize}
    />
  );
}
