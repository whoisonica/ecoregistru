import { cn } from "@/lib/utils";

export interface FormStep {
  name: string;
  /** Ce s-a completat deja pe pas, scris sub nume când pasul e trecut. */
  summary?: string;
  /** Pasul are o rubrică greșită: pătrățelul roșu, ca LED-ul unei stări. */
  invalid?: boolean;
}

/**
 * Cuprinsul unui formular pe pași, în stânga unui dialog — cererea de cont („Poster”) adusă în
 * aplicație, dar în „Cântar”: cifra în mono, pasul curent cu linia grafit la stânga, ca intrarea
 * activă din panou, fără verdele landingului.
 *
 * <p>Pașii se apasă: formularul împarte rubricile, nu le încuie. Cine editează un partener sare
 * direct la autorizație; cine adaugă unul trece prin „Continuă”, care își verifică pasul.
 */
export function FormStepRail({
  steps,
  current,
  onSelect,
  label,
  className,
}: {
  steps: FormStep[];
  /** Indexul pasului curent, de la 0. */
  current: number;
  onSelect: (index: number) => void;
  label: string;
  className?: string;
}) {
  return (
    <nav aria-label={label} className={className}>
      <ol className="space-y-1">
        {steps.map((step, index) => {
          const now = index === current;
          return (
            <li key={step.name}>
              <button
                type="button"
                onClick={() => onSelect(index)}
                aria-current={now ? "step" : undefined}
                className={cn(
                  "flex w-full gap-3 rounded-md border-l-[3px] py-2 pl-3 pr-2 text-left transition-colors",
                  now
                    ? "border-content bg-surface-muted"
                    : "border-transparent hover:bg-surface-muted"
                )}
              >
                <span
                  className={cn(
                    "w-6 shrink-0 font-mono text-lg leading-6",
                    now ? "text-brand-700" : "text-content-subtle"
                  )}
                >
                  {String(index + 1).padStart(2, "0")}
                </span>
                <span className="min-w-0">
                  <span
                    className={cn(
                      "flex items-center gap-1.5 text-sm font-semibold leading-6",
                      now ? "text-content" : "text-content-strong"
                    )}
                  >
                    {step.name}
                    {step.invalid && (
                      <span aria-hidden className="inline-block h-2 w-2 shrink-0 rounded-sm bg-state-bad" />
                    )}
                  </span>
                  {step.summary && (
                    <span className="block truncate text-xs text-content-muted">{step.summary}</span>
                  )}
                </span>
              </button>
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
