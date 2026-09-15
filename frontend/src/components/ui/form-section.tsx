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
        {/* Stilul „Prietenos”: titlul secțiunii e un titlu citit, nu o etichetă mică cu majuscule. */}
        <h3 className="text-base font-extrabold text-content">{title}</h3>
        {description && <p className="mt-0.5 text-sm text-content-muted">{description}</p>}
      </div>
      {children}
    </section>
  );
}
