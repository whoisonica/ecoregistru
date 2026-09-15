import { cn } from "@/lib/utils";
import { strings } from "@/lib/strings";

/**
 * Pașii unui formular împărțit pe întrebări — stilul „Prietenos” (docs/stil-interfata.md).
 *
 * <p>Pașii **împart** un formular, nu îl rearanjează: ordinea rubricilor din formularul de mișcare
 * e cea stabilită cu specialista și rămâne așa. Pasul curent poartă `aria-current="step"` și, pentru
 * cititorul de ecran, „pasul N din M”.
 */
export function Stepper({
  steps,
  current,
  className,
}: {
  steps: string[];
  /** Indexul pasului curent, de la 0. */
  current: number;
  className?: string;
}) {
  return (
    <ol className={cn("flex gap-2", className)}>
      {steps.map((step, index) => {
        const done = index < current;
        const now = index === current;
        return (
          <li
            key={step}
            aria-current={now ? "step" : undefined}
            className={cn(
              "grid min-w-0 flex-1 gap-1.5 text-xs font-bold",
              done ? "text-brand-800" : now ? "text-content" : "text-content-subtle"
            )}
          >
            <span
              aria-hidden
              className={cn(
                "h-1.5 rounded-full",
                done ? "bg-brand" : now ? "bg-gradient-to-r from-brand from-50% to-line to-50%" : "bg-line"
              )}
            />
            <span className="truncate">
              {now && (
                <span className="sr-only">
                  {strings.common.stepOf
                    .replace("{n}", String(index + 1))
                    .replace("{total}", String(steps.length))}{" "}
                </span>
              )}
              {step}
            </span>
          </li>
        );
      })}
    </ol>
  );
}
