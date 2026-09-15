import type { ReactNode } from "react";
import { Check } from "lucide-react";
import { cn } from "@/lib/utils";

export interface PillOption<T extends string> {
  value: T;
  label: ReactNode;
  /** Codul legii, scris mic lângă cuvinte („Container transportabil · CT”). */
  code?: string;
  disabled?: boolean;
}

/**
 * Variante scurte, ca pastile pe un rând care se înfășoară — stilul „Prietenos”
 * (docs/stil-interfata.md). Pentru răspunsuri de un cuvânt sau două: stare fizică, mijloc de
 * transport, bifele „Destinat:”. Cu `multiple`, pastilele sunt checkbox-uri; altfel radio.
 *
 * <p>Codul oficial rămâne vizibil, mic: pe hârtie se tipărește codul, pe ecran omul citește cuvinte.
 */
export function PillGroup<T extends string>({
  name,
  options,
  selected,
  onToggle,
  multiple = false,
  disabled = false,
  className,
  "aria-labelledby": labelledBy,
}: {
  name: string;
  options: PillOption<T>[];
  selected: T[];
  onToggle: (value: T) => void;
  multiple?: boolean;
  disabled?: boolean;
  className?: string;
  "aria-labelledby"?: string;
}) {
  return (
    <div
      role={multiple ? "group" : "radiogroup"}
      aria-labelledby={labelledBy}
      className={cn("flex flex-wrap gap-2", className)}
    >
      {options.map((option) => {
        const checked = selected.includes(option.value);
        const off = disabled || option.disabled;
        return (
          <label
            key={option.value}
            className={cn(
              "inline-flex items-center gap-1.5 rounded-full border-2 px-3.5 py-1.5 text-sm font-bold transition-colors",
              "has-[:focus-visible]:ring-4 has-[:focus-visible]:ring-brand/20",
              checked
                ? "border-brand bg-brand-50 text-brand-800"
                : "border-line bg-surface text-content-strong hover:border-line-strong",
              off ? "cursor-not-allowed opacity-55" : "cursor-pointer"
            )}
          >
            <input
              type={multiple ? "checkbox" : "radio"}
              name={name}
              value={option.value}
              checked={checked}
              disabled={off}
              onChange={() => onToggle(option.value)}
              className="sr-only"
            />
            {checked && multiple && <Check className="h-3.5 w-3.5" strokeWidth={3} aria-hidden />}
            {option.label}
            {option.code && <span className="font-mono text-xs font-medium opacity-70">{option.code}</span>}
          </label>
        );
      })}
    </div>
  );
}
