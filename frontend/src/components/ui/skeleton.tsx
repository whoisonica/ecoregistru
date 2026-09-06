import type { HTMLAttributes } from "react";
import { cn } from "@/lib/utils";
import { TBody, TD, TR } from "@/components/ui/table";

/** O bară gri care pulsează, de mărimea conținutului care va veni. */
export function Skeleton({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      aria-hidden
      className={cn("animate-pulse rounded bg-surface-sunken", className)}
      {...props}
    />
  );
}

/**
 * Rânduri-fantomă pentru un tabel care se încarcă.
 *
 * <p>Până acum fiecare pagină scria `{isLoading && <p>Se încarcă…</p>}` deasupra locului unde avea
 * să apară tabelul, deci pagina sărea când veneau datele. Scheletul ține forma: aceleași coloane,
 * aceeași înălțime de rând, deci nimic nu se mișcă la sosire.
 *
 * <p>Lățimile alternează ca să nu arate ca o grilă tipărită — un tabel real n-are toate celulele
 * la fel de pline.
 */
export function TableSkeleton({ columns, rows = 5 }: { columns: number; rows?: number }) {
  const widths = ["w-24", "w-32", "w-20", "w-28", "w-16", "w-36", "w-24", "w-20", "w-28"];
  return (
    <TBody>
      {Array.from({ length: rows }, (_, r) => (
        <TR key={r} className="hover:bg-transparent">
          {Array.from({ length: columns }, (_, c) => (
            <TD key={c}>
              <Skeleton className={cn("h-4", widths[(r + c) % widths.length])} />
            </TD>
          ))}
        </TR>
      ))}
    </TBody>
  );
}

/** Varianta pentru listele de pe panou: un rând de text cu o etichetă la dreapta. */
export function ListSkeleton({ rows = 4 }: { rows?: number }) {
  return (
    <ul className="mt-3 divide-y divide-line">
      {Array.from({ length: rows }, (_, i) => (
        <li key={i} className="flex items-center justify-between py-2.5">
          <div className="space-y-1.5">
            <Skeleton className="h-3.5 w-40" />
            <Skeleton className="h-3 w-20" />
          </div>
          <Skeleton className="h-5 w-16 rounded-full" />
        </li>
      ))}
    </ul>
  );
}
