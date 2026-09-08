import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

interface EmptyStateProps {
  icon?: LucideIcon;
  title: string;
  /** De ce e gol — nu „nu există date", ci ce anume lipsește și de unde vine. */
  description?: ReactNode;
  /** Butonul care umple golul. Lipsește la un cont care oricum n-ar avea voie să-l apese. */
  action?: ReactNode;
  className?: string;
}

/**
 * Un ecran gol care spune ceva.
 *
 * <p>Aplicația avea două feluri de gol: caseta punctată cu explicație (Termene, Evidențe) și un
 * `<td colSpan>` gri fără nicio ieșire (Mișcări, Setări). Al doilea lasă omul să se întrebe dacă
 * s-a stricat ceva. Aici e unul singur, și cere mereu un titlu — „Niciun rezultat" nu e un titlu,
 * e o constatare.
 */
export function EmptyState({
  icon: Icon,
  title,
  description,
  action,
  className,
}: EmptyStateProps) {
  return (
    <div
      className={cn(
        "flex flex-col items-center rounded-xl border border-dashed border-line-strong bg-surface px-6 py-10 text-center",
        className
      )}
    >
      {Icon && (
        <div className="mb-3 flex h-11 w-11 items-center justify-center rounded-full bg-surface-muted">
          <Icon className="h-5 w-5 text-content-subtle" aria-hidden />
        </div>
      )}
      <p className="font-medium text-content">{title}</p>
      {description && (
        <p className="mt-1 max-w-md text-sm text-content-muted">{description}</p>
      )}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}
