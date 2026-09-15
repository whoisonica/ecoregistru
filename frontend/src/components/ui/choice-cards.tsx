import type { ReactNode } from "react";
import { Check } from "lucide-react";
import { cn } from "@/lib/utils";

export interface ChoiceOption<T extends string> {
  value: T;
  label: ReactNode;
  /** O propoziție care spune ce urmează din alegere. E motivul pentru care există cardul. */
  description?: ReactNode;
  /** O pictogramă lucide, deja dimensionată (`h-5 w-5`). */
  icon?: ReactNode;
  disabled?: boolean;
}

/**
 * O alegere între puține variante, ca rând de carduri — stilul „Prietenos” (docs/stil-interfata.md).
 *
 * <p>Sub șapte opțiuni, toate stau la vedere, fiecare cu propoziția ei: omul care nu știe ce e
 * „ADMIN_ONLY” sau „CT” nu mai deschide o listă ca să afle. Dedesubt e un grup de radio **nativ**,
 * ascuns vizual: tastatura (săgețile), cititorul de ecran și `name`-ul formularului funcționează ca
 * la orice radio, fără să fie scrise a doua oară.
 */
export function ChoiceCards<T extends string>({
  name,
  value,
  onChange,
  options,
  columns = 1,
  disabled = false,
  className,
  "aria-labelledby": labelledBy,
}: {
  name: string;
  value: T | null;
  onChange: (value: T) => void;
  options: ChoiceOption<T>[];
  /** Coloanele de la `sm` în sus; pe telefon cardurile stau mereu unul sub altul. */
  columns?: 1 | 2 | 3;
  disabled?: boolean;
  className?: string;
  "aria-labelledby"?: string;
}) {
  return (
    <div
      role="radiogroup"
      aria-labelledby={labelledBy}
      className={cn(
        "grid grid-cols-1 gap-2.5",
        columns === 2 && "sm:grid-cols-2",
        columns === 3 && "sm:grid-cols-3",
        className
      )}
    >
      {options.map((option) => {
        const checked = option.value === value;
        const off = disabled || option.disabled;
        return (
          <label
            key={option.value}
            className={cn(
              "relative flex gap-3 rounded-lg border bg-surface p-3.5 transition-colors",
              // Focusul stă pe radio-ul ascuns; cardul îl arată în locul lui.
              "has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-brand/40",
              checked ? "border-brand bg-brand-50 ring-1 ring-brand" : "border-line-strong hover:border-content-subtle",
              off ? "cursor-not-allowed opacity-55" : "cursor-pointer"
            )}
          >
            <input
              type="radio"
              name={name}
              value={option.value}
              checked={checked}
              disabled={off}
              onChange={() => onChange(option.value)}
              className="sr-only"
            />
            {option.icon && (
              <span
                aria-hidden
                className={cn(
                  "flex h-10 w-10 shrink-0 items-center justify-center rounded-md",
                  checked ? "bg-brand-100 text-brand-700" : "bg-surface-sunken text-content-strong"
                )}
              >
                {option.icon}
              </span>
            )}
            <span className="min-w-0 flex-1 pr-6">
              <span className="block text-sm font-semibold text-content">{option.label}</span>
              {option.description && (
                <span className="mt-0.5 block text-xs text-content-muted">{option.description}</span>
              )}
            </span>
            {checked && (
              <span
                aria-hidden
                className="absolute right-3 top-3 flex h-5 w-5 items-center justify-center rounded bg-brand text-brand-fg"
              >
                <Check className="h-3 w-3" strokeWidth={3.5} />
              </span>
            )}
          </label>
        );
      })}
    </div>
  );
}
