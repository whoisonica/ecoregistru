import assert from "node:assert/strict";
import { test } from "node:test";
import { filingYear, filingYearCount, yearBlockers } from "@/lib/readiness";
import type { MonthlyEvidence } from "@/lib/types";

const row = (extra: Partial<MonthlyEvidence>) =>
  ({ totalUnclassifiedOut: 0, awaitingWeighing: false, ...extra }) as MonthlyEvidence;

/** 29.09.2026: între 1 ianuarie și 15 martie, anul trecut încă se depune. */
test("fereastra de depunere: 1 ianuarie – 15 martie inclusiv, pe anul trecut", () => {
  assert.equal(filingYear(new Date(2027, 0, 1)), 2026);
  assert.equal(filingYear(new Date(2027, 1, 28)), 2026);
  assert.equal(filingYear(new Date(2027, 2, 15, 23, 0)), 2026);
  assert.equal(filingYear(new Date(2027, 2, 16)), null);
  assert.equal(filingYear(new Date(2026, 11, 31)), null);
});

test("pe 10 ianuarie, o linie din decembrie fără cod R/D sau de cântărit e un blocaj", () => {
  const today = new Date(2027, 0, 10);
  const filed = filingYear(today);
  assert.equal(filed, 2026);
  const december = [row({ month: 12, totalUnclassifiedOut: 40 }), row({ month: 12, awaitingWeighing: true })];
  const b = yearBlockers({ year: 2027, rows: [] }, filed != null ? { year: filed, rows: december } : null);
  assert.deepEqual(b, { missingCode: 1, awaitingWeighing: 1, missingCodeYear: 2026 });
});

test("amândoi anii se adună; „Repară” duce la anul curent când cel depus e curat", () => {
  const b = yearBlockers(
    { year: 2027, rows: [row({ totalUnclassifiedOut: 5 })] },
    { year: 2026, rows: [row({ awaitingWeighing: true })] }
  );
  assert.deepEqual(b, { missingCode: 1, awaitingWeighing: 1, missingCodeYear: 2027 });
});

test("în afara ferestrei, doar anul curent, ca înainte", () => {
  const b = yearBlockers({ year: 2026, rows: [row({ totalUnclassifiedOut: 5 }), row({})] }, null);
  assert.deepEqual(b, { missingCode: 1, awaitingWeighing: 0, missingCodeYear: 2026 });
});

/** 29.09.2026: panoul, pe aceeași regulă ca Acasă — în fereastră, anul curent plus cel care se depune. */
test("indicatorul din panou: pe 10 ianuarie adună decembrie; în afara ferestrei, doar anul curent", () => {
  const filed = filingYear(new Date(2027, 0, 10));
  const window = filed != null ? { isError: false, count: 2 } : null;
  assert.equal(filingYearCount({ isError: false, count: 0 }, window), 2);
  const outside = filingYear(new Date(2027, 3, 1));
  assert.equal(filingYearCount({ isError: false, count: 1 }, outside != null ? { isError: false, count: 2 } : null), 1);
});

test("indicatorul din panou arată „?” dacă oricare an n-a putut fi citit", () => {
  assert.equal(filingYearCount({ isError: false, count: 3 }, { isError: true, count: undefined }), null);
  assert.equal(filingYearCount({ isError: true, count: undefined }, null), null);
  assert.equal(filingYearCount({ isError: false, count: undefined }, null), 0);
});
