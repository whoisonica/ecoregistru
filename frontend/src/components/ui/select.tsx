import { forwardRef, type SelectHTMLAttributes } from "react";
import { ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils";
import { fieldClasses } from "@/components/ui/input";

/**
 * Styled native <select>. Kept native for accessibility and zero-dependency
 * behaviour; used for unit / operation / physical state / R-D code / partner.
 *
 * <p>Într-un formular nou, sub șapte opțiuni se folosesc `ChoiceCards` sau `PillGroup`
 * (docs/stil-interfata.md): lista derulantă ascunde variantele exact omului care nu le știe.
 */
export const Select = forwardRef<HTMLSelectElement, SelectHTMLAttributes<HTMLSelectElement>>(
  ({ className, children, ...props }, ref) => (
    <div className="relative">
      <select
        ref={ref}
        className={cn("flex h-10 appearance-none pr-9", fieldClasses, className)}
        {...props}
      >
        {children}
      </select>
      <ChevronDown className="pointer-events-none absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 text-content-subtle" />
    </div>
  )
);
Select.displayName = "Select";
