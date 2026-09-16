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

  /** Cele două feluri de „nu e gata", numărate pe linii de evidență. */
  const rows = evidences ?? [];
  const blockers = {
    missingCode: rows.filter((r) => r.totalUnclassifiedOut > 0).length,
    awaitingWeighing: rows.filter((r) => r.awaitingWeighing).length,
  };

  return { openDeadlines, overdue, nextDeadline, nearDeadline, expiringPartners, blockers };
}
