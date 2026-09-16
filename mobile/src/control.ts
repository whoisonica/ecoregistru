import { daysLabel } from "@/lib/deadlines";
import { readiness } from "@/lib/readiness";
import { countOf } from "@/lib/count";
import { strings } from "@web/strings";
import type { Deadline, MonthlyEvidence, Partner } from "@web/types";

import { formatDate } from "./format";

export type CheckTone = "ok" | "warn" | "bad" | "unknown";

export interface Check {
  key: "deadlines" | "missingCode" | "weighing" | "partners";
  title: string;
  detail: string;
  tone: CheckTone;
}

/** O listă așa cum o dă TanStack Query: datele, sau semnul că n-au venit. */
export interface Source<T> {
  data: T | undefined;
  failed: boolean;
}

const d = strings.dashboard;

/**
 * G5 — rândurile ecranului „A venit controlul”, din **aceeași socoteală** ca Panoul web
 * (`frontend/src/lib/readiness.ts`, importată, nu rescrisă). Tonurile sunt cele ale „următoarei
 * acțiuni” de pe web: termen depășit și ieșire fără cod R/D = de rezolvat; termen apropiat,
 * autorizație pe terminate și cântar = de văzut.
 *
 * <p>Fiecare rând depinde numai de lista lui: evidența căzută face „?” pe cod și pe cântar, dar nu
 * ascunde un termen depășit care a venit. Un rând al cărui răspuns e încă pe drum întoarce `null`,
 * ca afișajul să nu numere nimic până nu vin toate.
 */
export function controlChecks(
  year: number,
  deadlines: Source<Deadline[]>,
  evidences: Source<MonthlyEvidence[]>,
  partners: Source<Partner[]>,
): (Check | null)[] {
  const r = readiness(deadlines.data, evidences.data, partners.data);
  const unknown = (key: Check["key"], title: string): Check => ({
    key,
    title,
    detail: strings.mobile.controlUnknownRow,
    tone: "unknown",
  });
  const row = <T>(source: Source<T>, key: Check["key"], title: string, build: () => Omit<Check, "key" | "title">) =>
    source.failed ? unknown(key, title) : source.data === undefined ? null : { key, title, ...build() };

  return [
    row(deadlines, "deadlines", strings.mobile.checkDeadlines, () => {
      if (r.overdue.length === 1) {
        const x = r.overdue[0];
        return { tone: "bad", detail: `${strings.enums.reportType[x.reportType]} — ${daysLabel(x) ?? ""}` };
      }
      if (r.overdue.length > 1) {
        return { tone: "bad", detail: d.nextOverdue.replace("{count}", countOf(r.overdue.length, "termen", "termene")) };
      }
      if (r.nearDeadline) {
        const x = r.nearDeadline;
        return { tone: "warn", detail: `${strings.enums.reportType[x.reportType]} — ${daysLabel(x) ?? ""}` };
      }
      return { tone: "ok", detail: strings.mobile.checkDeadlinesOk };
    }),
    row(evidences, "missingCode", strings.mobile.checkMissingCode(year), () =>
      r.blockers.missingCode > 0
        ? {
            tone: "bad",
            detail: d.blockerMissingCode.replace("{count}", countOf(r.blockers.missingCode, "linie", "linii")),
          }
        : { tone: "ok", detail: strings.mobile.checkMissingCodeOk },
    ),
    row(evidences, "weighing", strings.mobile.checkWeighing, () =>
      r.blockers.awaitingWeighing > 0
        ? {
            tone: "warn",
            detail: d.blockerAwaitingWeighing.replace("{count}", countOf(r.blockers.awaitingWeighing, "linie", "linii")),
          }
        : { tone: "ok", detail: strings.mobile.checkWeighingOk },
    ),
    row(partners, "partners", strings.mobile.checkPartners, () => {
      const expiring = r.expiringPartners;
      if (expiring.length === 1 && expiring[0].authorizationValidUntil) {
        return {
          tone: "warn",
          detail: strings.mobile.checkPartnerOne(expiring[0].name, formatDate(expiring[0].authorizationValidUntil)),
        };
      }
      if (expiring.length > 0) {
        return { tone: "warn", detail: d.nextExpiring.replace("{count}", countOf(expiring.length, "partener", "parteneri")) };
      }
      return { tone: "ok", detail: strings.mobile.checkPartnersOk };
    }),
  ];
}
