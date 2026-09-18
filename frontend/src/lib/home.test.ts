import assert from "node:assert/strict";
import { test } from "node:test";
import { deadlinesByMonth, deadlinesIcs, emptyMonths, monthlyKg, topCodes } from "@/lib/home";
import type { Deadline, MonthlyEvidence } from "@/lib/types";

const line = (month: number, code: string, kg: number, hazardous = false) =>
  ({ month, wasteCode: code, wasteCodeName: "Nume " + code, hazardous, totalGenerated: kg }) as MonthlyEvidence;

test("kilogramele se adună pe lună, peste coduri și puncte de lucru", () => {
  const kg = monthlyKg([line(1, "15 01 01", 100), line(1, "15 01 02", 20), line(3, "15 01 01", 5)]);
  assert.equal(kg.length, 12);
  assert.equal(kg[0], 120);
  assert.equal(kg[1], 0);
  assert.equal(kg[2], 5);
});

test("luna goală se numără doar între prima lună cu date și luna curentă", () => {
  // ian 0, feb 10, mar 0, apr 5, mai 0 (curentă) — martie e golul; ianuarie e dinainte de început.
  assert.deepEqual(emptyMonths([0, 10, 0, 5, 0, 0, 0, 0, 0, 0, 0, 0], 5), [3]);
  // Luna curentă nu e un gol, e în lucru.
  assert.deepEqual(emptyMonths([10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0], 2), []);
  // Un an fără nimic nu are goluri: n-are început.
  assert.deepEqual(emptyMonths(Array(12).fill(0), 9), []);
  // Un an încheiat: decembrie se numără și el.
  assert.deepEqual(emptyMonths([5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0], 13).length, 11);
});

test("codurile anului: cele mari întâi, zero nu intră, limita se ține", () => {
  const top = topCodes([line(1, "A", 5), line(2, "B", 50), line(3, "A", 10), line(4, "C", 0)], 5);
  assert.deepEqual(top.map((c) => [c.code, c.kg]), [["B", 50], ["A", 15]]);
  assert.equal(topCodes([line(1, "A", 1), line(1, "B", 2), line(1, "C", 3)], 2).length, 2);
});

const TODAY = new Date(2026, 8, 18);

const dl = (id: string, dueDate: string, status: Deadline["status"] = "UPCOMING") =>
  ({ id, dueDate, status, reportType: "AFM_QUARTERLY", completedAt: null, completionNote: null }) as Deadline;

test("termenele pe douăsprezece luni: depășitele în prima, cele de peste un an afară", () => {
  const cols = deadlinesByMonth(
    [dl("a", "2026-10-25"), dl("b", "2026-08-25", "OVERDUE"), dl("c", "2027-03-15"), dl("d", "2027-09-25")],
    TODAY
  );
  assert.deepEqual(cols[0].map((d) => d.id), ["b"]);
  assert.deepEqual(cols[1].map((d) => d.id), ["a"]);
  assert.deepEqual(cols[6].map((d) => d.id), ["c"]);
  assert.equal(cols.flat().length, 3);
});

test("fișierul .ics: o zi întreagă, scăpat, cu memento și fără termene calculate", () => {
  const ics = deadlinesIcs(
    [dl("a", "2026-12-31"), { ...dl("x", "2026-10-25"), id: null as unknown as string }],
    () => "AFM; trimestrial, 25",
    new Date(Date.UTC(2026, 8, 18, 10, 0, 0))
  );
  assert.match(ics, /^BEGIN:VCALENDAR\r\n/);
  assert.match(ics, /DTSTART;VALUE=DATE:20261231\r\nDTEND;VALUE=DATE:20270101/);
  assert.match(ics, /SUMMARY:AFM\\; trimestrial\\, 25/);
  assert.match(ics, /TRIGGER:-P3D/);
  assert.equal(ics.match(/BEGIN:VEVENT/g)?.length, 1);
  assert.match(ics, /DTSTAMP:20260918T100000Z/);
});
