import { forwardRef, type InputHTMLAttributes } from "react";
import { cn } from "@/lib/utils";

/**
 * Clasele comune ale rubricilor (input, select, textarea), în stilul „Prietenos”: colțuri rotunde,
 * chenar de 2px și, la focus, marginea în culoarea mărcii plus un halou pal — mai ușor de urmărit
 * într-un formular lung decât inelul subțire de dinainte. Stă într-un singur loc, ca cele trei să
 * nu se despartă.
 */
export const fieldClasses =
  "w-full rounded-xl border-2 border-line bg-surface px-3 py-2 text-sm font-medium placeholder:font-normal placeholder:text-content-subtle transition-colors hover:border-line-strong focus-visible:border-brand focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand/15 focus-visible:ring-offset-0 disabled:opacity-50";

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(
  ({ className, ...props }, ref) => (
    <input ref={ref} className={cn("flex h-10", fieldClasses, className)} {...props} />
  )
);
Input.displayName = "Input";
