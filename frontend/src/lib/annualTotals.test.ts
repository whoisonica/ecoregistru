import { test } from "node:test";
import assert from "node:assert/strict";
import { byCode } from "./annualTotals";
import type { MonthlyEvidence } from "./types";

/**
 * Totalul anului pe cod — tabul „Totalul anului" de pe Generare (18.09.2026).
 *
 * <p>Socoteala e scoasă din componentă ca să se poată proba pe date alese, nu pe baza demo: acolo
 * toate cifrele care contează sunt mici sau zero, deci o probă de ecran ar compara prea puțin.
 */
function linie(over: Partial<MonthlyEvidence>): MonthlyEvidence {
  return {
    id: Math.random().toString(36).slice(2),
    workPointId: "wp-1",
    workPointName: "Punct 1",
    year: 2026,
    month: 1,
    wasteCodeId: "wc-1",
    wasteCode: "20 01 01",
    wasteCodeName: "hârtie și carton",
    hazardous: false,
    totalGenerated: 0,
    totalRecovered: 0,
    totalDisposed: 0,
    totalHandedOver: 0,
    totalUnclassifiedOut: 0,
    incomplete: false,
    awaitingWeighing: false,
    closingStock: 0,
    generatedAt: "2026-01-31T23:59:59Z",
    ...over,
  };
}

test("cantitățile se adună pe cod, peste luni și puncte de lucru", () => {
  const [rand] = byCode([
    linie({ month: 1, totalGenerated: 100, totalRecovered: 60 }),
    linie({ month: 2, totalGenerated: 40, totalDisposed: 25 }),
    linie({ month: 2, workPointId: "wp-2", totalGenerated: 10 }),
  ]);
  assert.equal(rand.generated, 150);
  assert.equal(rand.recovered, 60);
  assert.equal(rand.disposed, 25);
});

test("stocul liniilor nu ajunge în totaluri — la generator nu există stoc", () => {
  // Decizia proprietarului (18.09.2026), aceeași ca la `V58`: un generator n-are cântar și nu ține
  // stoc, iar pe Anexa 1 cifra iese zero prin construcție. Liniile vechi pot purta un `closingStock`
  // nenul (cumulativ, cu anii dinainte în el); totalurile ecranului nu-l ating.
  const [rand] = byCode([
    linie({ month: 1, totalGenerated: 100, totalRecovered: 60, closingStock: 40 }),
    linie({ month: 2, totalGenerated: 20, closingStock: 60 }),
    linie({ month: 3, workPointId: "wp-2", workPointName: "Punct 2", closingStock: -15 }),
  ]);
  assert.equal(rand.generated, 120);
  assert.equal(rand.recovered, 60);
  assert.deepEqual(Object.keys(rand).filter((k) => /stoc|stock/i.test(k)), []);
});

test("liniile care așteaptă cântarul se numără, kilogramele fără cod R/D se adună", () => {
  const [rand] = byCode([
    linie({ month: 1, awaitingWeighing: true }),
    linie({ month: 2, awaitingWeighing: true, totalUnclassifiedOut: 200 }),
    linie({ month: 3, totalUnclassifiedOut: 50 }),
  ]);
  assert.equal(rand.awaiting, 2);
  assert.equal(rand.unclassified, 250);
});

test("codurile ies în ordine, câte unul", () => {
  const randuri = byCode([
    linie({ wasteCode: "20 03 01", wasteCodeName: "amestecate" }),
    linie({ wasteCode: "13 02 08", wasteCodeName: "uleiuri", hazardous: true }),
    linie({ wasteCode: "20 03 01", wasteCodeName: "amestecate", month: 2 }),
  ]);
  assert.deepEqual(
    randuri.map((r) => r.wasteCode),
    ["13 02 08", "20 03 01"]
  );
  assert.equal(randuri[0].hazardous, true);
});

test("fără linii, nu există rânduri", () => {
  assert.deepEqual(byCode([]), []);
});
