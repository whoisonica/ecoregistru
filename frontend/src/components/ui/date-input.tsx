import { forwardRef, type InputHTMLAttributes } from "react";
import { cn } from "@/lib/utils";
import { fieldClasses } from "@/components/ui/input";

/**
 * Native <input type="date"> styled to match the other form controls.
 * Value/onChange use the browser's yyyy-MM-dd string, which is what the API expects.
 */
export const DateInput = forwardRef<
  HTMLInputElement,
  Omit<InputHTMLAttributes<HTMLInputElement>, "type">
>(({ className, ...props }, ref) => (
  <input
    ref={ref}
    type="date"
    // Aceleași clase ca `Input`: data e o rubrică, nu o excepție. Cifrele în mono, ca peste tot.
    className={cn("flex h-10 font-mono", fieldClasses, className)}
    {...props}
  />
));
DateInput.displayName = "DateInput";
