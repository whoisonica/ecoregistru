import assert from "node:assert/strict";
import { test } from "node:test";
import { daysLabel, daysUntil, documentFor } from "@/lib/deadlines";
import type { Deadline } from "@/lib/types";

function isoInDays(n: number): string {
  const d = new Date();
  d.setDate(d.getDate() + n);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

const deadline = (over: Partial<Deadline>) => ({ status: "OPEN", ...over }) as Deadline;

test("zilele se socotesc pe calendar, nu pe ore", () => {
  assert.equal(daysUntil(isoInDays(0)), 0);
  assert.equal(daysUntil(isoInDays(1)), 1);
  assert.equal(daysUntil(isoInDays(-3)), -3);
});

test("un termen finalizat nu mai are zile rămase", () => {
  assert.equal(daysLabel(deadline({ status: "DONE", dueDate: isoInDays(5) })), null);
  assert.notEqual(daysLabel(deadline({ dueDate: isoInDays(5) })), null);
});

test("documentul care stinge termenul e pe anul raportat, iar AFM n-are document", () => {
  assert.equal(documentFor(deadline({ reportType: "SIM_ANNUAL", dueDate: "2027-03-15" }))?.to, "/evidente?an=2026");
  assert.equal(documentFor(deadline({ reportType: "PACKAGING_ANNUAL", dueDate: "2027-02-25" }))?.to, "/ambalaje?an=2026");
  assert.equal(documentFor(deadline({ reportType: "AFM_MONTHLY", dueDate: "2026-10-25" })), null);
});
