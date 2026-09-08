import type { LabelHTMLAttributes } from "react";
import { cn } from "@/lib/utils";
import { strings } from "@/lib/strings";

interface LabelProps extends LabelHTMLAttributes<HTMLLabelElement> {
  /**
   * Marchează rubrica drept obligatorie, cu asterisc.
   *
   * <p>Asteriscul e `aria-hidden` și îl dublează un text citit doar de cititorul de ecran: pe
   * hârtie „*" e o convenție învățată, dar rostit ca „stea" nu spune nimic. Cine vede formularul
   * află din marcaj, cine îl ascultă află din cuvânt — aceeași informație, nu două.
   */
  required?: boolean;
}

export function Label({ className, required, children, ...props }: LabelProps) {
  return (
    <label
      className={cn("mb-1 block text-sm font-medium text-content-strong", className)}
      {...props}
    >
      {children}
      {required && (
        <>
          <span aria-hidden="true" className="ml-0.5 text-red-600">
            *
          </span>
          <span className="sr-only"> {strings.common.requiredMarker}</span>
        </>
      )}
    </label>
  );
}
