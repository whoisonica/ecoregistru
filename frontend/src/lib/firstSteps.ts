import type { Company } from "@/lib/types";

/**
 * „Primii pași” pe Acasă (16.09.2026): drumul unui cont nou până la primul document, în ordinea în
 * care îl cere codul, nu o listă de sfaturi. Fiecare pas se bifează singur, din datele care îl
 * dovedesc; niciunul nu se bifează de mână, ca lista să nu poată spune „gata” peste un cont gol.
 *
 * <ol>
 *   <li><b>Datele firmei</b> — adresa, CAEN-ul și persoana desemnată (OUG 92/2021 art. 23) se tipăresc
 *       pe fișa de evidență și pe declarația anuală; goale, rubrica iese goală pe documentul depus.</li>
 *   <li><b>Punctul de lucru</b> — o mișcare se înregistrează pe unul.</li>
 *   <li><b>Partenerul</b> — colectorul căruia i se predă, cu autorizația lui (art. 23 alin. (1)).</li>
 *   <li><b>Prima înregistrare</b> — evidența și anexele se construiesc din mișcări.</li>
 * </ol>
 */
export type FirstStepId = "company" | "workPoint" | "partner" | "movement";

/** Rubricile firmei fără de care documentele ies cu goluri. */
export type CompanyField = "address" | "caenCode" | "wasteManagerName";

export type FirstStep = { id: FirstStepId; done: boolean; missing: CompanyField[] };

export const COMPANY_FIELDS: CompanyField[] = ["address", "caenCode", "wasteManagerName"];

/**
 * `null` cât timp vreo sursă n-a venit (sau a căzut): o listă socotită din liste goale ar spune
 * „de făcut” unui cont care are totul — aceeași regulă ca banda „Următoarea acțiune”.
 */
export function firstSteps(input: {
  company: Company | undefined;
  workPoints: readonly unknown[] | undefined;
  partners: readonly unknown[] | undefined;
  evidences: readonly unknown[] | undefined;
  movementCount: number | undefined;
}): FirstStep[] | null {
  const { company, workPoints, partners, evidences, movementCount } = input;
  if (!company || !workPoints || !partners || !evidences || movementCount === undefined) {
    return null;
  }
  const missing = COMPANY_FIELDS.filter((f) => !(company[f] ?? "").trim());
  return [
    { id: "company", done: missing.length === 0, missing },
    { id: "workPoint", done: workPoints.length > 0, missing: [] },
    { id: "partner", done: partners.length > 0, missing: [] },
    { id: "movement", done: movementCount > 0 || evidences.length > 0, missing: [] },
  ];
}

/**
 * Lista e a unui cont **nou**: se arată cât timp nu e pornit lucrul (punct de lucru, partener, mișcare).
 * Datele firmei singure n-o țin deschisă: un cont care lucrează de luni de zile și n-are CAEN-ul
 * completat nu e la „primii pași” — golul lui se vede în Setări, la datele firmei.
 */
export function showFirstSteps(steps: FirstStep[] | null): steps is FirstStep[] {
  return steps !== null && steps.some((s) => s.id !== "company" && !s.done);
}
