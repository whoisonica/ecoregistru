// Registrul lunii pe tabul „Generare” (F6, valul B): `npm run test:register`.
import assert from "node:assert/strict";
import { test } from "node:test";

import { byDay, composition, gapMonths, kgOf, missingCode, monthsBack } from "./register.ts";

const row = (date: string, code: string, quantity: number | null, unit = "KG") =>
  ({ date, wasteCode: code, wasteCodeName: code, hazardous: false, quantity, unit }) as never;

test("ultimele 12 luni, cea curentă întâi, peste schimbarea anului", () => {
  const m = monthsBack(new Date(2026, 1, 15), 12);
  assert.equal(m.length, 12);
  assert.deepEqual(m[0], { year: 2026, month: 2 });
  assert.deepEqual(m[1], { year: 2026, month: 1 });
  assert.deepEqual(m[2], { year: 2025, month: 12 });
  assert.deepEqual(m[11], { year: 2025, month: 3 });
});

test("luna goală e galbenă numai între prima lună cu ceva și cea curentă", () => {
  // index 0 = luna curentă (goală, dar în lucru), 5 = prima cu ceva; 7 e înainte de început.
  assert.deepEqual([...gapMonths([0, 3, 0, 2, 0, 1, 0, 0])], [2, 4]);
  assert.deepEqual([...gapMonths([0, 0, 0])], [], "nicio lună cu ceva: nimic galben");
  assert.deepEqual([...gapMonths([4, undefined, 2])], [], "o lună încă neîncărcată nu e goală");
});

test("kg: tonele se înmulțesc, rândul care așteaptă cântarul nu adaugă nimic", () => {
  assert.equal(kgOf({ quantity: 2.5, unit: "TONS" } as never), 2500);
  assert.equal(kgOf({ quantity: 380, unit: "KG" } as never), 380);
  assert.equal(kgOf({ quantity: null, unit: "KG" } as never), 0);
});

test("fără cod R/D = ieșirea neclasificată, ca pe server", () => {
  assert.equal(missingCode({ operation: "UNCLASSIFIED_OUT" } as never), true);
  assert.equal(missingCode({ operation: "RECOVERED" } as never), false);
});

test("zilele în ordinea de la server, cu totalul fiecăreia", () => {
  const days = byDay([row("2026-09-27", "15 01 01", 1240), row("2026-09-27", "15 01 02", 1, "TONS"), row("2026-09-22", "15 01 02", null)]);
  assert.deepEqual(days.map((d) => [d.date, d.kg, d.rows.length]), [["2026-09-27", 2240, 2], ["2026-09-22", 0, 1]]);
});

test("compoziția: pe coduri, cele mai mari întâi, fără codurile cu zero", () => {
  const c = composition([row("d", "20 03 01", 390), row("d", "15 01 01", 2000), row("d", "15 01 01", 40), row("d", "15 01 02", null)]);
  assert.deepEqual(c.map((s) => [s.code, s.kg]), [["15 01 01", 2040], ["20 03 01", 390]]);
});
