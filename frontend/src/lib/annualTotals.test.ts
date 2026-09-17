import { test } from "node:test";
import assert from "node:assert/strict";
import { byCode } from "./annualTotals";
import type { MonthlyEvidence } from "./types";

/**
 * Totalul anului pe cod — tabul „Totalul anului" de pe Generare (18.09.2026).
 *
 * <p>Testul există pentru **stoc**: e singura cifră care nu se adună. Pe baza demo toate stocurile
 * sunt zero, deci proba de ecran (10) compară numai zerouri și n-ar vedea greșeala; aici se vede.
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

test("stocul e al ultimei luni cu date, nu suma lunilor", () => {
  // Aceeași marfă, văzută în trei luni: 100 rămase în ianuarie, 60 în februarie, 80 în martie.
  // Suma ar da 240 — de trei ori aceleași kilograme. Adevărul e ultima lună: 80.
  const [rand] = byCode([
    linie({ month: 1, totalGenerated: 100, closingStock: 100 }),
    linie({ month: 2, totalRecovered: 40, closingStock: 60 }),
    linie({ month: 3, totalGenerated: 20, closingStock: 80 }),
  ]);
  assert.equal(rand.stock, 80);
  assert.equal(rand.generated, 120);
});

test("stocul ultimei luni se adună între punctele de lucru", () => {
  // Fiecare punct își poartă stocul lui, iar ultima lună a unuia nu e neapărat ultima a celuilalt.
  const [rand] = byCode([
    linie({ month: 3, closingStock: 30 }),
    linie({ month: 1, workPointId: "wp-2", workPointName: "Punct 2", closingStock: 999 }),
    linie({ month: 7, workPointId: "wp-2", workPointName: "Punct 2", closingStock: 12 }),
  ]);
  assert.equal(rand.stock, 42);
});

test("stocul negativ trece mai departe, nu se rotunjește la zero", () => {
  // Ieșiri mai mari decât intrările într-o fereastră: cifra e roșie pe ecran, dar rămâne cifra.
  const [rand] = byCode([linie({ month: 5, closingStock: -15 })]);
  assert.equal(rand.stock, -15);
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
