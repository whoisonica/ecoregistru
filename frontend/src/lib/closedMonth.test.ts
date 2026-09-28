import { test } from "node:test";
import assert from "node:assert/strict";
import { closedMonth } from "./dates";

test("o zi din luna trecută e într-o lună încheiată", () => {
  assert.deepEqual(closedMonth("2026-08-31", "2026-09-01"), { year: 2026, month: 8 });
});

test("decembrie e încheiat în ianuarie", () => {
  assert.deepEqual(closedMonth("2025-12-15", "2026-01-02"), { year: 2025, month: 12 });
});

test("luna curentă și una viitoare nu întreabă", () => {
  assert.equal(closedMonth("2026-09-01", "2026-09-28"), null);
  assert.equal(closedMonth("2026-10-01", "2026-09-28"), null);
  assert.equal(closedMonth(null, "2026-09-28"), null);
});
