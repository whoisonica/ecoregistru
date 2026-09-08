import type { Deadline } from "@/lib/types";
import { strings } from "@/lib/strings";
import { countOf } from "@/lib/utils";

/**
 * Socotelile pe termene, într-un singur loc.
 *
 * <p>`daysUntil` era scrisă identic pe Panou şi pe Termene, cu acelaşi javadoc, iar felia
 * „următoarea acţiune" ar fi cerut a treia copie. `documentFor` la fel: legătura termen → document
 * decide **anul raportat**, adică o cifră tipărită pe un ecran, iar două implementări ale ei sunt
 * două ocazii să difere.
 */

/**
 * Câte zile mai sunt până la o dată, socotite pe zile calendaristice.
 *
 * <p>Se compară la miezul nopţii, nu la ora curentă: altfel un termen de mâine dimineaţă ar ieşi
 * „0 zile" după-amiaza, ceea ce e adevărat în ore şi fals în felul în care se citeşte un calendar.
 */
export function daysUntil(iso: string): number {
  const [y, m, d] = iso.split("-").map(Number);
  const target = new Date(y, m - 1, d);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return Math.round((target.getTime() - today.getTime()) / 86_400_000);
}

/** Zilele rămase, în cuvinte. Un termen finalizat nu le mai are: nu mai e nimic de aşteptat. */
export function daysLabel(d: Deadline): string | null {
  if (d.status === "DONE") return null;
  const days = daysUntil(d.dueDate);
  if (days < 0)
    return strings.deadlines.daysOverdue.replace("{count}", countOf(-days, "zi", "zile"));
  if (days === 0) return strings.deadlines.daysToday;
  if (days === 1) return strings.deadlines.daysTomorrow;
  return strings.deadlines.daysLeft.replace("{count}", countOf(days, "zi", "zile"));
}

/**
 * Ecranul de pe care se scoate documentul care stinge termenul.
 *
 * <p>Amândouă raportările anuale acoperă **anul precedent** celui în care se depun — 15 martie
 * pentru evidenţa anului trecut (OUG 92/2021 art. 48 alin. (1)), 25 februarie pentru ambalajele
 * anului trecut (Ordinul 794/2012 art. 6, „pentru anul anterior") — deci linkul duce la anul
 * raportat, nu la anul termenului. A duce la anul termenului ar deschide un dosar gol chiar în ziua
 * depunerii.
 *
 * <p>Contribuţiile AFM n-au link, şi asta nu e o scăpare: sunt bani declaraţi în aplicaţia AFM, iar
 * aplicaţia noastră nu tipăreşte niciun formular pentru ele (vezi `docs/legislatie.md` §5.B). Un
 * link către un document care nu există ar promite mai mult decât ţinem — chiar defectul reparat pe
 * 07.09 la badge-ul roşu, pe dos.
 */
export function documentFor(d: Deadline): { to: string; label: string } | null {
  const reported = Number(d.dueDate.slice(0, 4)) - 1;
  if (d.reportType === "SIM_ANNUAL") {
    return {
      to: `/evidente?an=${reported}`,
      label: strings.deadlines.documentEvidence.replace("{year}", String(reported)),
    };
  }
  if (d.reportType === "PACKAGING_ANNUAL") {
    return {
      to: `/ambalaje?an=${reported}`,
      label: strings.deadlines.documentPackaging.replace("{year}", String(reported)),
    };
  }
  return null;
}
