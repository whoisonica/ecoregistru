import { test } from "node:test";
import assert from "node:assert/strict";
import { periodOf, periodValid } from "./depotReportPeriod";

/** D4.7 — scurtăturile de pe Cântar → Rapoarte și limita de un an, aceeași ca serverul (366 de zile, capetele incluse). */
const today = new Date(2026, 0, 15);

test("luna trecută din ianuarie e decembrie anului trecut", () => {
  assert.deepEqual(periodOf("LAST_MONTH", today), { from: "2025-12-01", to: "2025-12-31" });
});

test("luna asta, trimestrul și anul sunt întregi", () => {
  assert.deepEqual(periodOf("THIS_MONTH", today), { from: "2026-01-01", to: "2026-01-31" });
  assert.deepEqual(periodOf("QUARTER", new Date(2026, 7, 20)), { from: "2026-07-01", to: "2026-09-30" });
  assert.deepEqual(periodOf("YEAR", today), { from: "2026-01-01", to: "2026-12-31" });
});

test("un an bisect întreg încape, o zi în plus nu", () => {
  assert.equal(periodValid("2024-01-01", "2024-12-31"), true);
  assert.equal(periodValid("2024-01-01", "2025-01-01"), false);
  assert.equal(periodValid("2026-09-30", "2026-09-01"), false);
  assert.equal(periodValid("", "2026-09-01"), false);
});
