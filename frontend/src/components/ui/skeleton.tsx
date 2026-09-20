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
