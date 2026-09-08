import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

/**
 * Antetul unei pagini: titlu, o linie de explicație, și acțiunile la dreapta.
 *
 * <p>Se repeta în șapte pagini, scris de mână de fiecare dată — `flex items-center
 * justify-between`, un `h1` de `text-2xl font-bold`, un `p` de `text-sm text-gray-500`, cu marja
 * de sus când `mt-1`, când lipsă. Toate șapte aveau și același defect: pe un ecran îngust titlul
 * și butoanele se împart aceeași linie, iar Evidențe, cu cinci butoane, o rupea de tot.
 *
 * <p>Aici acțiunile trec sub titlu până la `sm` și se pot înfășura pe mai multe rânduri.
 */
export function PageHeader({
  title,
  description,
  actions,
  className,
}: {
  title: ReactNode;
  description?: ReactNode;
  /** Butoanele paginii. Se înfășoară singure: la Evidențe sunt cinci. */
  actions?: ReactNode;
  className?: string;
}) {
  return (
    <div
      className={cn(
        "flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between",
        className
      )}
    >
      <div className="min-w-0">
        <h1 className="text-2xl font-bold text-content">{title}</h1>
        {description && <p className="mt-1 text-sm text-content-muted">{description}</p>}
      </div>
      {actions && (
        <div className="flex flex-wrap items-center gap-2 sm:shrink-0">{actions}</div>
      )}
    </div>
  );
}
