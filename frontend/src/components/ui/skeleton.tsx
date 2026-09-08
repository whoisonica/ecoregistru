import type { HTMLAttributes } from "react";
import { cn } from "@/lib/utils";

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
