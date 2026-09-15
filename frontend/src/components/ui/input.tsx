import { forwardRef, type InputHTMLAttributes } from "react";
import { cn } from "@/lib/utils";

/**
 * Clasele comune ale rubricilor (input, select, textarea), în direcția „Cântar”: colț de 5px,
 * chenar de 1px, iar la focus marginea în verdele mărcii plus un inel subțire. Stă într-un singur
 * loc, ca cele trei să nu se despartă.
 */
export const fieldClasses =
  "w-full rounded-md border border-line-strong bg-surface px-3 py-2 text-sm text-content placeholder:text-content-subtle transition-colors hover:border-content-subtle focus-visible:border-brand focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/25 focus-visible:ring-offset-0 disabled:bg-surface-muted disabled:opacity-60";

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(
  ({ className, ...props }, ref) => (
    <input ref={ref} className={cn("flex h-10", fieldClasses, className)} {...props} />
  )
);
Input.displayName = "Input";
