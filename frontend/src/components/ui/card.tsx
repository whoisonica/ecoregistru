import type { HTMLAttributes, ReactNode } from "react";
import { cn } from "@/lib/utils";

/**
 * Suprafața pe care stă orice bloc de conținut. Exista deja, dar scrisă de mână — combinația
 * `rounded-xl border border-gray-200 bg-white p-5` apărea în vreo douăzeci de locuri, cu p-4,
 * p-5 și p-6 amestecate. Aici e o singură decizie.
 *
 * <p>„Cântar”: un chenar de 1px pe alb, colț de 6px, nicio umbră. Cardul e o cutie, nu o hârtie
 * care plutește.
 */
export function Card({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn("rounded-lg border border-line-strong/70 bg-surface p-4 sm:p-5", className)}
      {...props}
    />
  );
}

/**
 * O secțiune din Setări: aceeași cutie ca `Card`, pusă direct pe `<section>`, fiindcă secțiunile cu
 * tabel au deja capul și bara lor și n-au nevoie de încă un înveliș.
 */
export const SETTINGS_CARD = "scroll-mt-20 rounded-lg border border-line-strong/70 bg-surface p-4 sm:p-5";

/**
 * Capul unui card: titlu la stânga, acțiune la dreapta. Aliniat pe `items-start`, nu pe `center`,
 * fiindcă titlul poate avea și o linie de explicație sub el, iar butonul trebuie să rămână sus.
 */
export function CardHeader({
  title,
  description,
  action,
  className,
}: {
  title: ReactNode;
  description?: ReactNode;
  action?: ReactNode;
  className?: string;
}) {
  return (
    <div className={cn("flex items-start justify-between gap-4", className)}>
      <div className="min-w-0">
        <h2 className="text-[0.9375rem] font-semibold text-content">{title}</h2>
        {description && <p className="mt-0.5 text-sm text-content-muted">{description}</p>}
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </div>
  );
}
