import type {
  HTMLAttributes,
  TableHTMLAttributes,
  TdHTMLAttributes,
  ThHTMLAttributes,
} from "react";
import { ArrowDown, ArrowUp, ChevronsUpDown } from "lucide-react";
import { cn } from "@/lib/utils";
import { strings } from "@/lib/strings";
import type { SortState } from "@/hooks/useTableView";

/**
 * Styled table primitives. `Table` wraps the element so wide tables scroll on their own.
 *
 * <p>`stickyHeader` ține capul de tabel pe loc cât timp se derulează rândurile. Pe Mișcări sunt
 * nouă coloane, iar la al treizecilea rând nu mai știi care cifră e „valorificat" și care „stoc"
 * fără să derulezi înapoi.
 */
export function Table({
  className,
  stickyHeader = false,
  ...props
}: TableHTMLAttributes<HTMLTableElement> & { stickyHeader?: boolean }) {
  return (
    <div
      className={cn(
        // „Cântar”: tabelul stă direct pe pagină, ca un tabel tipărit — fără cutie, fără umbră.
        // Capul are o linie de 2px grafit, rândurile o linie subțire.
        "overflow-x-auto bg-surface",
        // Înălțimea maximă e ce face antetul lipicios să însemne ceva: fără ea, containerul
        // crește cât tabelul și nu se derulează nimic pe dinăuntru.
        stickyHeader && "max-h-[70vh] overflow-y-auto"
      )}
    >
      {/* 14px, nu `text-sm`: la 15px, Mișcări ieșea din 1440px și coloana de atașamente intra sub
          coloana de acțiuni fixată — prinsă de proba 8 (15.09.2026). Tabelele sunt singurul loc unde
          densitatea bate mărimea. */}
      <table className={cn("w-full text-[0.875rem] leading-5", className)} {...props} />
    </div>
  );
}

export function THead({
  className,
  sticky = false,
  ...props
}: HTMLAttributes<HTMLTableSectionElement> & { sticky?: boolean }) {
  return (
    <thead
      className={cn(
        // Capul de tabel în mono, mic, cu majuscule — eticheta de pe bon. Linia de 2px de sub el
        // e ce desparte capul de rânduri; nu mai există fundal gri.
        "border-b-2 border-content bg-surface text-left font-mono text-[0.6875rem] font-medium uppercase tracking-[0.07em] text-content-muted",
        // `bg-surface` pe `thead` nu acoperă rândurile care trec pe dedesubt în unele browsere, de
        // asta culoarea se pune și pe celule, prin `[&>tr>th]`. Linia de 2px se mută tot pe celule:
        // un chenar pe `thead` lipicios rămâne în urmă la derulare.
        //
        // `z-20` bate `z-10` al coloanei fixate din dreapta: colțul din dreapta-sus e amândouă
        // deodată, iar acolo antetul trebuie să fie deasupra.
        sticky && "sticky top-0 z-20 border-b-0 [&>tr>th]:border-b-2 [&>tr>th]:border-content [&>tr>th]:bg-surface",
        className
      )}
      {...props}
    />
  );
}

export function TBody({ className, ...props }: HTMLAttributes<HTMLTableSectionElement>) {
  return <tbody className={cn("divide-y divide-line", className)} {...props} />;
}

export function TR({ className, ...props }: HTMLAttributes<HTMLTableRowElement>) {
  return (
    <tr
      className={cn(
        // `group` ca celula fixată din dreapta să poată prelua evidențierea rândului: are fundal
        // opac, altfel ar rămâne albă în timp ce restul rândului se colorează pe sub ea.
        "group hover:bg-surface-muted/60",
        // Loc pentru antetul lipicios, când ceva derulează rândul în raza vizibilă. Probat pe
        // 07.09.2026: fără marja asta, un rând adus la vedere ajunge **sub** antet, iar butoanele
        // lui nu se mai pot apăsa. Se întâmplă și fără cod de-al nostru — browserul derulează
        // singur când focusul ajunge pe un rând care nu se vede, adică la navigarea cu Tab.
        "scroll-mt-12",
        className
      )}
      {...props}
    />
  );
}

/**
 * Ce ține o celulă lipită de marginea din dreapta cât timp tabelul se derulează pe orizontală.
 *
 * <p>Măsurat pe 07.09.2026, la 1280px: Mișcări depășește lățimea disponibilă cu 202px, Ambalaje cu
 * 213, Parteneri cu 122. Derularea în sine e în regulă — asta face învelișul — dar coloana de
 * acțiuni ieșea din ecran, iar din ce rămânea vizibil nici nu se vedea că **există** o coloană de
 * acțiuni. Fixată, e mereu la îndemână, iar restul trece pe sub ea.
 *
 * <p>Dunga din stânga e un `before`, nu un `border-l`: un chenar ar intra în lățimea celulei și ar
 * muta conținutul cu un pixel față de rândurile nefixate.
 */
const STICKY_RIGHT =
  "sticky right-0 z-10 bg-surface group-hover:bg-surface-muted " +
  // Cât timp se lucrează în celulă, ea urcă peste celelalte. `position: sticky` face un context
  // de stivuire propriu, deci meniul de rând (oricât de mare i-ar fi z-index-ul) rămâne prins
  // înăuntrul lui — iar celulele fixate ale rândurilor **de dedesubt**, fiind mai târziu în DOM,
  // se desenau peste el. Probat pe 07.09.2026: „Șterge" din meniu nu se putea apăsa.
  "focus-within:z-30 " +
  "before:absolute before:inset-y-0 before:left-0 before:w-px before:bg-line";

export function TH({
  className,
  sticky,
  ...props
}: ThHTMLAttributes<HTMLTableCellElement> & {
  /** `right` ține coloana lipită de marginea din dreapta. Pentru coloana de acțiuni. */
  sticky?: "right";
}) {
  return (
    <th
      className={cn("relative px-2.5 py-2", sticky === "right" && STICKY_RIGHT, className)}
      {...props}
    />
  );
}

export function TD({
  className,
  sticky,
  ...props
}: TdHTMLAttributes<HTMLTableCellElement> & {
  /** `right` ține celula lipită de marginea din dreapta. Pentru coloana de acțiuni. */
  sticky?: "right";
}) {
  return (
    <td
      className={cn(
        // `px-3`, nu `px-4`: la 15px Mișcări ieșea din 1440px cu 63px, adică exact coloana de
        // atașamente sub cea de acțiuni fixată (proba 8). Textul celulei e cel principal, nu
        // estompat: pe un tabel fără fundal gri, griul pe alb era prea puțin.
        "relative px-2.5 py-2.5 text-content",
        sticky === "right" && STICKY_RIGHT,
        className
      )}
      {...props}
    />
  );
}

/**
 * Cap de coloană pe care se poate apăsa ca să sorteze.
 *
 * <p>`aria-sort` e partea care nu se vede: fără el, săgeata spune direcția doar celui care o vede,
 * iar un cititor de ecran anunță o coloană oarecare. Săgeata rămâne palidă până se sortează după
 * coloana asta, ca să se știe că se **poate** apăsa, fără să pară că e deja sortată.
 */
export function SortableTH({
  sortKey,
  sort,
  onSort,
  className,
  children,
  align = "left",
  ...props
}: ThHTMLAttributes<HTMLTableCellElement> & {
  sortKey: string;
  sort: SortState | null;
  onSort: (key: string) => void;
  /** Coloanele de cifre stau la dreapta; butonul n-are de unde ști asta singur. */
  align?: "left" | "right";
}) {
  const active = sort?.key === sortKey;
  const Icon = !active ? ChevronsUpDown : sort.direction === "asc" ? ArrowUp : ArrowDown;
  const label = active
    ? sort.direction === "asc"
      ? strings.common.sortedAsc
      : strings.common.sortedDesc
    : undefined;
  return (
    <th
      className={cn("px-0 py-0", className)}
      aria-sort={active ? (sort.direction === "asc" ? "ascending" : "descending") : "none"}
      {...props}
    >
      <button
        type="button"
        onClick={() => onSort(sortKey)}
        // Moștenește alinierea celulei: coloanele de cifre sunt la dreapta, iar butonul nu are de
        // unde ști asta singur.
        className={cn(
          "flex w-full items-center gap-1.5 px-2.5 py-2 uppercase tracking-[0.07em] transition-colors hover:text-content",
          align === "right" ? "justify-end text-right" : "text-left"
        )}
      >
        <span>{children}</span>
        <Icon
          className={cn("h-3 w-3 shrink-0", active ? "text-brand" : "text-content-subtle/50")}
          aria-hidden
        />
        {label && <span className="sr-only">{label}</span>}
      </button>
    </th>
  );
}

/**
 * „12–24 din 340", cu Înapoi/Înainte.
 *
 * <p>Nu se arată deloc pe o singură pagină: o bară de paginare sub cinci rânduri e zgomot.
 */
export function Pagination({
  page,
  pageCount,
  onPage,
  matchCount,
  pageSize,
}: {
  page: number;
  pageCount: number;
  onPage: (page: number) => void;
  matchCount: number;
  pageSize: number;
}) {
  if (pageCount <= 1) return null;
  const from = page * pageSize + 1;
  const to = Math.min(matchCount, (page + 1) * pageSize);
  return (
    <div className="mt-3 flex flex-wrap items-center justify-between gap-3 text-sm text-content-muted">
      <span className="font-mono text-xs">
        {strings.common.rangeOfTotal
          .replace("{from}", String(from))
          .replace("{to}", String(to))
          .replace("{total}", String(matchCount))}
      </span>
      <div className="flex items-center gap-2">
        <button
          type="button"
          onClick={() => onPage(page - 1)}
          disabled={page === 0}
          className="rounded-md border border-line-strong px-3 py-1.5 font-medium text-content transition-colors hover:bg-surface-muted disabled:opacity-40 disabled:hover:bg-transparent"
        >
          {strings.common.previous}
        </button>
        <button
          type="button"
          onClick={() => onPage(page + 1)}
          disabled={page >= pageCount - 1}
          className="rounded-md border border-line-strong px-3 py-1.5 font-medium text-content transition-colors hover:bg-surface-muted disabled:opacity-40 disabled:hover:bg-transparent"
        >
          {strings.common.next}
        </button>
      </div>
    </div>
  );
}
