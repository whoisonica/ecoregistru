import type { Deadline, MonthlyEvidence, Partner } from "@/lib/types";
import { daysUntil } from "@/lib/deadlines";

/**
 * Cât de aproape trebuie să fie un termen ca să merite să fie **acțiunea următoare**.
 *
 * <p>Treizeci de zile, fiindcă atât ia strâns un dosar: regenerarea evidenței, verificarea liniilor
 * roșii, scoaterea documentului. Mai devreme de-atât, banda ar numi luni întregi un lucru pe care
 * nimeni nu-l face azi — iar o bandă care spune mereu același lucru devine tapet în trei zile, exact
 * ce s-a reparat pe 07.09 la bannerul galben permanent de pe Evidențe.
 */
export const NEAR_DEADLINE_DAYS = 30;

/**
 * Ce e în neregulă cu firma acum, socotit din listele pe care le au și Panoul web, și ecranul „A venit
 * controlul” de pe telefon (todo-mobil G5).
 *
 * <p>A stat în `useDashboardData` până pe 16.09.2026. Telefonul avea nevoie de exact aceleași
 * socoteli, iar două copii ar fi ajuns să difere — un termen „depășit” pe web și „în regulă” pe
 * telefon, în fața inspectorului. De aceea fișierul n-are React și nici alt `import` decât tipuri,
 * `deadlines.ts` și, prin el, textele.
 *
 * <p>O listă `undefined` (neîncărcată) se socotește goală; cine arată rezultatul trebuie să știe
 * singur că n-a venit — aici nu se poate deosebi „nimic” de „nu știu”.
 */
export function readiness(
  deadlines: Deadline[] | undefined,
  evidences: MonthlyEvidence[] | undefined,
  partners: Partner[] | undefined
) {
  const openDeadlines = [...(deadlines ?? [])]
    .filter((d) => d.status !== "DONE")
    .sort((a, b) => a.dueDate.localeCompare(b.dueDate));
  const overdue = openDeadlines.filter((d) => d.status === "OVERDUE");
  const nextDeadline = openDeadlines.find((d) => d.status !== "OVERDUE");
  const nearDeadline =
    nextDeadline && daysUntil(nextDeadline.dueDate) <= NEAR_DEADLINE_DAYS ? nextDeadline : undefined;

  const expiringPartners = (partners ?? []).filter((p) => p.active && p.expiringSoon);

  const blockers = blockersOf(evidences);

  return { openDeadlines, overdue, nextDeadline, nearDeadline, expiringPartners, blockers };
}

/** Cele două feluri de „nu e gata", numărate pe linii de evidență. */
export function blockersOf(evidences: MonthlyEvidence[] | undefined) {
  const rows = evidences ?? [];
  return {
    missingCode: rows.filter((r) => r.totalUnclassifiedOut > 0).length,
    awaitingWeighing: rows.filter((r) => r.awaitingWeighing).length,
  };
}

/**
 * Anul care încă se depune, dacă suntem în fereastra lui: între 1 ianuarie și 15 martie inclusiv, anul
 * trecut (evidența anului trecut se depune până pe 15 martie — OUG 92/2021 art. 48 alin. (1), vezi
 * `documentFor`). În afara ferestrei, `null`.
 *
 * <p>29.09.2026: Acasă număra liniile fără cod R/D și pe cele de cântărit numai pe anul curent. Pe
 * 10 ianuarie, o predare din decembrie fără cod R/D — chiar ce oprește depunerea de pe 15 martie — nu
 * apărea nicăieri, iar banda scria „Ești la zi”.
 */
export function filingYear(today = new Date()): number | null {
  const month = today.getMonth();
  const inWindow = month < 2 || (month === 2 && today.getDate() <= 15);
  return inWindow ? today.getFullYear() - 1 : null;
}

/**
 * Blocajele Acasă, pe anul curent **și**, în fereastra de depunere, pe anul care se depune: liniile se
 * adună, iar „Repară” duce întâi la anul care se depune, dacă acolo e ceva de reparat — acela are termen.
 * Cu `filed = null` (în afara ferestrei), doar anul curent, ca înainte.
 */
export function yearBlockers(
  current: { year: number; rows: MonthlyEvidence[] | undefined },
  filed: { year: number; rows: MonthlyEvidence[] | undefined } | null
) {
  const now = blockersOf(current.rows);
  const past = blockersOf(filed?.rows);
  return {
    missingCode: now.missingCode + past.missingCode,
    awaitingWeighing: now.awaitingWeighing + past.awaitingWeighing,
    missingCodeYear: filed && past.missingCode > 0 ? filed.year : current.year,
  };
}

/**
 * Un indicator din panou („3 de cântărit”, „2 fără cod R/D”) pe aceeași regulă ca blocajele de pe Acasă
 * (29.09.2026): anul curent și, în fereastra din `filingYear`, anul care se depune — adunate. `filed =
 * null` în afara ferestrei. `null` în rezultat = unul dintre ani n-a putut fi citit, deci „?”, nu o cifră
 * mai mică decât adevărul (decizia 68).
 */
export function filingYearCount(
  current: { isError: boolean; count: number | undefined },
  filed: { isError: boolean; count: number | undefined } | null
): number | null {
  if (current.isError || filed?.isError) return null;
  return (current.count ?? 0) + (filed?.count ?? 0);
}
