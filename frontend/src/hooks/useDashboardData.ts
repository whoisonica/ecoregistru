import { useMemo } from "react";
import { useEvidences } from "@/hooks/useEvidences";
import { useMovementSummary } from "@/hooks/useMovements";
import { useUpcomingDeadlines } from "@/hooks/useDeadlines";
import { usePartners } from "@/hooks/usePartners";
import { useWorkPoints } from "@/hooks/useWorkPoints";
import { strings } from "@/lib/strings";
import { countOf } from "@/lib/utils";
import { daysLabel, documentFor } from "@/lib/deadlines";
import { readiness } from "@/lib/readiness";

const t = strings.dashboard;

/** Ce anume e de făcut, o singură dată, cu drumul către el. `null` = nu s-a putut încă decide. */
export type NextAction = {
  /**
   * `unknown` = una dintre surse n-a răspuns; nu se afirmă nici că e ceva, nici că nu e.
   * `start` = contul e gol; nu e „gata", e „de unde încep".
   */
  tone: "danger" | "warning" | "ok" | "start" | "unknown";
  title: string;
  hint: string;
  to: string;
  cta: string;
};

/**
 * Datele ecranului Acasă **și** ale panoului din stânga, dintr-un singur loc.
 *
 * <p>Au stat în `DashboardPage` până pe 15.09.2026, când afișajul lunii și indicatorii din panou
 * (direcția „Cântar”) au avut nevoie de aceleași cifre: kilogramele lunii, liniile fără cod R/D,
 * cele de cântărit, termenele depășite, autorizațiile pe terminate, și verdictul „următoarea
 * acțiune". Două socoteli ale aceluiași lucru ar fi ajuns să difere; aici e una, iar TanStack
 * Query dedublează cererile, deci panoul nu costă nimic în plus față de Acasă.
 *
 * <p>Nu inventează nicio cifră: fiecare ramură citește exact numărul pe care ecranul îl arată.
 */
export function useDashboardData(enabled = true) {
  const now = new Date();
  const year = now.getFullYear();
  const month = now.getMonth() + 1;

  /**
   * Cele două cifre ale lunii se **cer socotite**, nu se adună din rânduri (P3.1): serverul socotește
   * peste luna întreagă, în kilograme.
   */
  const { data: summary, isLoading: loadingMovements, isError: failedMovements } =
    useMovementSummary(year, month, enabled);
  const { data: deadlines, isLoading: loadingDeadlines, isError: failedDeadlines } =
    useUpcomingDeadlines(enabled);
  const { data: partners, isLoading: loadingPartners, isError: failedPartners } = usePartners(enabled);
  /**
   * Punctele de lucru, numai ca să se poată deosebi „nu e nimic de făcut" de „nu s-a început încă".
   * E o listă mică, deja în cache pe Setări și pe formularul de mișcare.
   */
  const { data: workPoints, isLoading: loadingWorkPoints, isError: failedWorkPoints } =
    useWorkPoints(enabled);
  /**
   * Evidența anului întreg, nu a lunii: ce blochează depunerea e o întrebare despre an, fiindcă
   * fișa și declarația acoperă anul.
   */
  const { data: evidences, isLoading: loadingEvidences, isError: failedEvidences } =
    useEvidences({ year }, enabled);

  /** Socoteala stă în `lib/readiness.ts`, fiindcă o face și telefonul (ecranul „A venit controlul”). */
  const { openDeadlines, overdue, nextDeadline, nearDeadline, expiringPartners, blockers } = useMemo(
    () => readiness(deadlines, evidences, partners),
    [deadlines, evidences, partners]
  );
  const overdueCount = overdue.length;
  const blockerCount = blockers.missingCode + blockers.awaitingWeighing;

  const generatedThisMonth = summary?.quantityKg ?? 0;
  /** Câte mișcări s-au înregistrat luna asta — cifra care spune dacă evidența se ține la zi. */
  const movementCount = summary?.movements ?? 0;

  /**
   * Un singur lucru de făcut, ales după cât costă dacă rămâne nefăcut. Ordinea: termen depășit,
   * ieșire fără cod R/D, termen apropiat, autorizație pe terminate, cântar (așteptare legitimă —
   * decizia 13 — deci ultimul). Contribuțiile AFM cad pe `/termene`, nu pe un document:
   * `documentFor` întoarce `null` tocmai ca să nu promitem unul.
   */
  const nextAction = useMemo<NextAction | null>(() => {
    // 0. Dacă vreuna dintre surse n-a răspuns, nu se alege nimic: fiecare ramură de mai jos
    //    citeşte o listă care ar fi **goală din alt motiv**. Se spune că nu se ştie.
    if (failedDeadlines || failedEvidences || failedPartners || failedWorkPoints) {
      return {
        tone: "unknown",
        title: t.nextUnknown,
        hint: t.nextUnknownHint,
        to: "/",
        cta: t.nextUnknownCta,
      };
    }
    if (overdue.length === 1) {
      const d = overdue[0];
      const doc = documentFor(d);
      return {
        tone: "danger",
        title: t.nextDeadline
          .replace("{label}", strings.enums.reportType[d.reportType])
          .replace("{days}", daysLabel(d) ?? ""),
        hint: t.nextOverdueHint,
        to: doc?.to ?? "/termene",
        cta: doc?.label ?? t.nextOverdueCta,
      };
    }
    if (overdue.length > 1) {
      return {
        tone: "danger",
        title: t.nextOverdue.replace("{count}", countOf(overdue.length, "termen", "termene")),
        hint: t.nextOverdueHint,
        to: "/termene",
        cta: t.nextOverdueCta,
      };
    }
    if (blockers.missingCode > 0) {
      return {
        tone: "danger",
        title: t.nextMissingCode.replace("{count}", countOf(blockers.missingCode, "linie", "linii")),
        hint: t.nextMissingCodeHint,
        to: "/evidente?vedere=handovers&problema=cod-rd",
        cta: t.blockerFix,
      };
    }
    if (nearDeadline) {
      const doc = documentFor(nearDeadline);
      return {
        tone: "warning",
        title: t.nextDeadline
          .replace("{label}", strings.enums.reportType[nearDeadline.reportType])
          .replace("{days}", daysLabel(nearDeadline) ?? ""),
        hint: t.nextDeadlineHint,
        to: doc?.to ?? "/termene",
        cta: doc?.label ?? t.nextDeadlineCta,
      };
    }
    if (expiringPartners.length > 0) {
      return {
        tone: "warning",
        title: t.nextExpiring.replace("{count}", countOf(expiringPartners.length, "partener", "parteneri")),
        hint: t.nextExpiringHint,
        to: "/parteneri",
        cta: t.nextExpiringCta,
      };
    }
    if (blockers.awaitingWeighing > 0) {
      return {
        tone: "warning",
        title: t.nextWeighing.replace("{count}", countOf(blockers.awaitingWeighing, "linie", "linii")),
        hint: t.nextWeighingHint,
        to: "/miscari",
        cta: t.nextWeighingCta,
      };
    }
    // „Nimic de făcut" are două înţelesuri: un cont pe care nu s-a scris încă nimic nu e la zi, e
    // neînceput. O mişcare se înregistrează **pe** un punct de lucru; evidenţa se calculează **din**
    // mişcări — amândouă sunt dependenţe din cod, nu preferinţe de flux.
    if ((workPoints ?? []).length === 0) {
      return {
        tone: "start",
        title: t.nextStartWorkPoint,
        hint: t.nextStartWorkPointHint,
        to: "/setari/puncte-de-lucru",
        cta: t.nextStartWorkPointCta,
      };
    }
    // Trei liste goale deodată, nu una: o firmă care lucrează are parteneri chiar şi într-o lună
    // fără mişcări.
    if (movementCount === 0 && (evidences ?? []).length === 0 && (partners ?? []).length === 0) {
      return {
        tone: "start",
        title: t.nextStartMovement,
        hint: t.nextStartMovementHint,
        to: "/miscari",
        cta: t.nextStartMovementCta,
      };
    }
    return { tone: "ok", title: t.nextNothing, hint: t.nextNothingHint, to: "/evidente", cta: t.viewAll };
  }, [
    overdue,
    nearDeadline,
    blockers,
    expiringPartners,
    failedDeadlines,
    failedEvidences,
    failedPartners,
    failedWorkPoints,
    workPoints,
    evidences,
    partners,
    movementCount,
  ]);

  /**
   * Banda tace până vin **toate** sursele din care alege. Fără garda asta, un `partners` întârziat
   * ar scrie „Ești la zi" o clipă, peste o autorizație care expiră.
   */
  const nextActionLoading =
    loadingDeadlines || loadingEvidences || loadingPartners || loadingWorkPoints || loadingMovements;

  return {
    year,
    month,
    monthLabel: strings.months[month - 1],
    summary,
    loadingMovements,
    failedMovements,
    deadlines,
    loadingDeadlines,
    failedDeadlines,
    partners,
    loadingPartners,
    failedPartners,
    workPoints,
    evidences,
    loadingEvidences,
    failedEvidences,
    openDeadlines,
    overdueCount,
    nextDeadline,
    expiringPartners,
    blockers,
    blockerCount,
    generatedThisMonth,
    movementCount,
    nextAction,
    nextActionLoading,
  };
}
