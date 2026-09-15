import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

/**
 * Un da/nu cu o propoziție — stilul „Prietenos” (docs/stil-interfata.md). Înlocuiește bifa lungă
 * („Se cântărește la descărcare” + trei rânduri de explicație) cu un comutator și un rând.
 *
 * <p>Dedesubt e un checkbox nativ cu `role="switch"`, deci Space și cititorul de ecran merg singure.
 */
export function Switch({
  id,
  checked,
  onChange,
  label,
  description,
  disabled = false,
  className,
}: {
  id: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
  label: ReactNode;
  description?: ReactNode;
  disabled?: boolean;
  className?: string;
}) {
  return (
    <label
      htmlFor={id}
      className={cn(
        "flex items-start gap-3 rounded-lg border p-3.5 transition-colors",
        "has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-brand/40",
        checked ? "border-brand bg-brand-50" : "border-line-strong bg-surface hover:border-content-subtle",
        disabled ? "cursor-not-allowed opacity-55" : "cursor-pointer",
        className
      )}
    >
      <input
        id={id}
        type="checkbox"
        role="switch"
        checked={checked}
        disabled={disabled}
        onChange={(e) => onChange(e.target.checked)}
        className="sr-only"
      />
      <span
        aria-hidden
        className={cn(
          "relative mt-0.5 h-6 w-11 shrink-0 rounded-full transition-colors",
          checked ? "bg-brand" : "bg-line-strong"
        )}
      >
        <span
          className={cn(
            "absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition-[left]",
            checked ? "left-[1.375rem]" : "left-0.5"
          )}
        />
      </span>
      <span className="min-w-0">
        <span className="block text-sm font-semibold text-content">{label}</span>
        {description && <span className="mt-0.5 block text-xs text-content-muted">{description}</span>}
      </span>
    </label>
  );
}
