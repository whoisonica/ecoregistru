import type { HTMLAttributes, ReactNode } from "react";
import { cn } from "@/lib/utils";

/**
 * Suprafața pe care stă orice bloc de conținut. Exista deja, dar scrisă de mână — combinația
 * `rounded-xl border border-gray-200 bg-white p-5` apărea în vreo douăzeci de locuri, cu p-4,
 * p-5 și p-6 amestecate. Aici e o singură decizie.
 */
export function Card({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn("rounded-xl border border-line bg-surface p-5 shadow-card", className)}
      {...props}
    />
  );
}

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
        <h2 className="font-semibold text-content">{title}</h2>
        {description && <p className="mt-0.5 text-sm text-content-muted">{description}</p>}
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </div>
  );
}
