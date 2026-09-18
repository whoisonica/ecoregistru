import type { ReactNode } from "react";

/**
 * Un document: butonul, iar sub el ce anume descarci — numele întreg din act și ce conține.
 *
 * <p>Așezarea aprobată pe 18.09.2026, pe tabul „Totalul anului": butonul nu poartă numele întreg
 * din act, fiindcă nu încape pe un rând — numele întreg rămâne în explicație, în `aria-label` și
 * pe documentul tipărit. A ieșit din `AnnualTotals` când a cerut-o și tabul „Ambalaje": două
 * rânduri de documente desenate la fel, în două fișiere, ar fi început să se depărteze.
 */
export function DocAction({ hint, action }: { hint: string; action: ReactNode }) {
  return (
    <div>
      {action}
      <p className="mt-1.5 text-xs leading-snug text-content-muted">{hint}</p>
    </div>
  );
}
