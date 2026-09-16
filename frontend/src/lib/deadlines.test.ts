import assert from "node:assert/strict";
import { test } from "node:test";
import { daysLabel, daysUntil, documentFor, evidenceReadiness } from "@/lib/deadlines";
import type { MonthlyEvidence } from "@/lib/types";
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
  assert.equal(documentFor(deadline({ reportType: "PACKAGING_ANNEX3", dueDate: "2027-02-25" }))?.to, "/ambalaje?an=2026");
  assert.equal(documentFor(deadline({ reportType: "AFM_MONTHLY", dueDate: "2026-10-25" })), null);
});

const line = (over: Partial<MonthlyEvidence>) =>
  ({ wasteCode: "15 01 01", totalGenerated: 0, totalUnclassifiedOut: 0, awaitingWeighing: false, ...over }) as MonthlyEvidence;

test("evidența e gata de depus numai pe un an încheiat, cu linii și fără blocaje", () => {
  const jan2027 = new Date(2027, 0, 10);
  const clean = [line({ totalGenerated: 1200 }), line({ wasteCode: "20 01 01", totalGenerated: 300 })];
  const r = evidenceReadiness(clean, 2026, jan2027);
  assert.equal(r.codes, 2);
  assert.equal(r.generatedKg, 1500);
  assert.equal(r.ready, true);

  assert.equal(evidenceReadiness(clean, 2026, new Date(2026, 8, 16)).ready, false, "anul încă curge");
  assert.equal(evidenceReadiness([], 2026, jan2027).ready, false, "nimic de depus nu e „gata”");
  const blocked = evidenceReadiness([...clean, line({ totalUnclassifiedOut: 5 }), line({ awaitingWeighing: true })], 2026, jan2027);
  assert.equal(blocked.missingCode, 1);
  assert.equal(blocked.awaitingWeighing, 1);
  assert.equal(blocked.ready, false);
});
