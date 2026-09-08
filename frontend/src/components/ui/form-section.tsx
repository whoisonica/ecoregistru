import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

/**
 * O secțiune dintr-un formular lung.
 *
 * <p>Formularul de mișcare are treizeci de rubrici într-o singură coloană care se derulează, iar
 * până acum singurele repere erau cele trei casete cu chenar care apar condiționat. Restul —
 * cantitatea, depozitarea, transportul, destinatarul — curgeau la rând, fără nimic care să spună
 * unde se termină o întrebare și începe alta.
 *
 * <p>Secțiunile **nu se pliază**, dinadins: jumătate din ele conțin rubrici obligatorii, iar un
 * câmp obligatoriu ascuns sub un titlu închis e un formular care se refuză fără să spună de ce.
 * Titlul e reper, nu ușă.
 */
export function FormSection({
  title,
  description,
  children,
  className,
}: {
  title: string;
  description?: ReactNode;
  children: ReactNode;
  className?: string;
}) {
  return (
    <section className={cn("space-y-3", className)}>
      <div className="border-b border-line pb-1.5">
        <h3 className="text-xs font-semibold uppercase tracking-wide text-content-muted">
          {title}
        </h3>
        {description && <p className="mt-1 text-xs text-content-subtle">{description}</p>}
      </div>
      {children}
    </section>
  );
}
