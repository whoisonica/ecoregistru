import assert from "node:assert/strict";
import { test } from "node:test";
import { energyAction, energyYearStatus, monthComplete, previousMonth, withEnergyAction } from "@/lib/energy";
import type { EnergyCell, EnergySheet, EnergyYearSummary } from "@/lib/types";

const sheet = (over: Partial<EnergySheet>): EnergySheet =>
  ({ year: 2026, carriers: [], cells: [], ...over }) as EnergySheet;
const cell = (carrier: EnergyCell["carrier"], month: number, quantity: number | null, tep: number | null = null): EnergyCell => ({
  carrier,
  month,
  quantity,
  tep,
});

test("în ianuarie, luna trecută e decembrie din anul trecut", () => {
  assert.deepEqual(previousMonth(new Date(2027, 0, 10)), { year: 2026, month: 12 });
  assert.deepEqual(previousMonth(new Date(2026, 9, 4)), { year: 2026, month: 9 });
});

test("cărbunele cu cantitate și fără tep nu face luna completă", () => {
  const s = sheet({ carriers: ["ELECTRICITY", "COAL"], cells: [cell("ELECTRICITY", 3, 5), cell("COAL", 3, 2)] });
  assert.equal(monthComplete(s, 3), false);
  const ok = sheet({ carriers: ["ELECTRICITY", "COAL"], cells: [cell("ELECTRICITY", 3, 5), cell("COAL", 3, 2, 1.5)] });
  assert.equal(monthComplete(ok, 3), true);
  assert.equal(monthComplete(ok, 4), false);
});

test("fără rubrici, acțiunea cere rubricile", () => {
  const a = energyAction(sheet({}), new Date(2026, 9, 4));
  assert.equal(a?.title, "Spuneți ce energie folosește firma");
  assert.equal(a?.hint, "O dată: bifați ce apare pe facturi.");
  assert.equal(a?.to, "/termene/energie?an=2026");
  assert.equal(a?.cta, "Fișa de energie");
  assert.equal(a?.tone, "warning");
});

test("noiembrie lipsă, în decembrie: acțiunea numește luna cu literă mică", () => {
  const s = sheet({ carriers: ["ELECTRICITY"], cells: [cell("ELECTRICITY", 10, 5)] });
  const a = energyAction(s, new Date(2026, 11, 3));
  assert.equal(a?.title, "Energia pe noiembrie nu e trecută");
  assert.equal(a?.hint, "De pe factura lunii, pentru declarația din 30 iunie.");
});

test("luna trecută completă: nicio acțiune; fără fișă: nicio acțiune", () => {
  const s = sheet({ carriers: ["ELECTRICITY"], cells: [cell("ELECTRICITY", 11, 5)] });
  assert.equal(energyAction(s, new Date(2026, 11, 3)), null);
  assert.equal(energyAction(undefined, new Date(2026, 11, 3)), null);
});

test("starea anului: depusă, depusă cu recipisă, incompletă, nedepusă", () => {
  const y = (over: Partial<EnergyYearSummary>): EnergyYearSummary =>
    ({ year: 2026, monthsComplete: 12, overThreshold: false, filedOn: null, receiptOn: null, hasReceipt: false, ...over });
  assert.deepEqual(energyYearStatus(y({ filedOn: "2027-06-12" })), { tone: "success", label: "Depusă pe 12.06.2027" });
  // Termenul bifat câștigă peste ziua recipisei.
  assert.deepEqual(energyYearStatus(y({ filedOn: "2027-06-12", receiptOn: "2027-06-10", hasReceipt: true })), {
    tone: "success",
    label: "Depusă pe 12.06.2027",
  });
  // Fără termen bifat (30.06.2026 e doar calculat), recipisa e dovada — chiar și cu luni lipsă.
  assert.deepEqual(energyYearStatus(y({ receiptOn: "2026-06-20", hasReceipt: true, monthsComplete: 7 })), {
    tone: "success",
    label: "Depusă (recipisă din 20.06.2026)",
  });
  assert.deepEqual(energyYearStatus(y({ monthsComplete: 7 })), { tone: "warning", label: "Incompletă: 7 din 12 luni" });
  assert.deepEqual(energyYearStatus(y({})), { tone: "muted", label: "Nedepusă" });
});

test("în ianuarie, pe fișa anului trecut, lipsește decembrie", () => {
  const s = sheet({ year: 2026, carriers: ["ELECTRICITY"], cells: [cell("ELECTRICITY", 11, 5)] });
  const a = energyAction(s, new Date(2027, 0, 12));
  assert.equal(a?.title, "Energia pe decembrie nu e trecută");
  assert.equal(a?.to, "/termene/energie?an=2026");
  const full = sheet({ year: 2026, carriers: ["ELECTRICITY"], cells: [cell("ELECTRICITY", 12, 5)] });
  assert.equal(energyAction(full, new Date(2027, 0, 12)), null);
});

test("Acasă: memento-ul de energie vine după tot restul, și după pașii de început", () => {
  const e = "energie";
  // Ceva de făcut: energia la coadă.
  assert.deepEqual(withEnergyAction(["termen"], ["punct de lucru"], e), ["termen", "energie"]);
  // Cont nou: pașii de început rămân primii, energia după ei.
  assert.deepEqual(withEnergyAction([], ["punct de lucru"], e), ["punct de lucru", "energie"]);
  // Nimic altceva: energia singură.
  assert.deepEqual(withEnergyAction([], [], e), ["energie"]);
  // Fără memento: listele neschimbate; nimic deloc = null („Ești la zi” îl pune apelantul).
  assert.deepEqual(withEnergyAction(["termen"], [], null), ["termen"]);
  assert.deepEqual(withEnergyAction([], ["punct de lucru"], null), ["punct de lucru"]);
  assert.equal(withEnergyAction([], [], null), null);
});
