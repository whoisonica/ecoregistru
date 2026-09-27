// Regulile formularului de predare: `npm run test:form` (Node rulează TypeScript direct, fără Jest).
import assert from "node:assert/strict";
import { test } from "node:test";

import { initialPackagingOnMarket, parseQuantity, weightRecorded } from "./handoverForm.ts";

test("cantitatea cu virgulă e zecimală", () => {
  assert.equal(parseQuantity("2,5"), 2.5);
  assert.equal(parseQuantity("0,75"), 0.75);
  assert.equal(parseQuantity("1250"), 1250);
});

test("cantitatea cu punct e tot zecimală — tastatura telefonului în engleză are punct, nu virgulă", () => {
  assert.equal(parseQuantity("2.5"), 2.5);
  assert.equal(parseQuantity("0.75"), 0.75);
});

test("cu punct și virgulă, punctele despart miile (cum scrie avizul)", () => {
  assert.equal(parseQuantity("1.250,5"), 1250.5);
});

test("o cantitate de neînțeles nu devine un număr", () => {
  for (const bad of ["", " ", "abc", "2,5,1", "2.5.1", "1,250.5", "-3", "0", "2,5 kg"]) {
    assert.equal(parseQuantity(bad), null, bad);
  }
});

test("mai mult de trei zecimale nu trece — serverul primește cel mult trei", () => {
  assert.equal(parseQuantity("0,1234"), null);
  assert.equal(parseQuantity("0,125"), 0.125);
});

test("greutatea venită după cântărire se păstrează la corectură", () => {
  const cantarita = { weighedAtUnloading: true, quantity: 830 };
  assert.equal(weightRecorded(true, cantarita), true);
  // încă fără greutate: câmpul rămâne închis, ca la o predare nouă
  assert.equal(weightRecorded(true, { weighedAtUnloading: true, quantity: null }), false);
  // bifa pusă acum, pe o predare care avea cantitate scrisă de mână: se așteaptă cântarul
  assert.equal(weightRecorded(true, { weighedAtUnloading: false, quantity: 830 }), false);
  assert.equal(weightRecorded(false, cantarita), false);
  assert.equal(weightRecorded(true, undefined), false);
});

test("bifa de ambalaj a unei predări vechi rămâne necompletată, nu devine „nu”", () => {
  assert.equal(initialPackagingOnMarket({ packagingOnMarket: null }), null);
  assert.equal(initialPackagingOnMarket({ packagingOnMarket: false }), false);
  assert.equal(initialPackagingOnMarket({ packagingOnMarket: true }), true);
  // predarea nouă pornește cu „nu” (întrebarea e „ai pus TU ambalajul pe piață?”)
  assert.equal(initialPackagingOnMarket(undefined), false);
});

import { codeInProfile, yearInRange } from "./handoverForm.ts";

test("anul predării e între 2000 și zece ani de acum, ca pe server", () => {
  const today = new Date("2026-09-27T10:00:00Z");
  assert.equal(yearInRange("2026-09-14", today), true);
  assert.equal(yearInRange("2000-01-01", today), true);
  assert.equal(yearInRange("2036-12-31", today), true);
  assert.equal(yearInRange("1999-12-31", today), false);
  assert.equal(yearInRange("2037-01-01", today), false);
  assert.equal(yearInRange("0206-09-14", today), false);
});

test("codul R/D trebuie să fie în profilul firmei, când profilul e completat", () => {
  assert.equal(codeInProfile("R3", ["R3", "R4"]), true);
  // „La fel ca data trecută” a adus un cod scos între timp din profil
  assert.equal(codeInProfile("R5", ["R3", "R4"]), false);
  // profil gol = necompletat, nu „nimic voie” (`validateAgainstProfile`)
  assert.equal(codeInProfile("R5", []), true);
  assert.equal(codeInProfile("", ["R3"]), true);
});

import { queuedToMovement } from "./handoverForm.ts";

test("predarea refuzată din coadă se redeschide în formular cu tot ce avea", () => {
  const payload = {
    workPointId: "wp1",
    date: "2026-09-14",
    wasteCodeId: "c1",
    quantity: 2.5,
    unit: "TONS",
    operationCode: "R5",
    partnerId: "p1",
    vehicleRegistration: "CJ 12 ABC",
    packagingOnMarket: null,
    clientGeneratedId: "q1",
  };
  const mv = queuedToMovement(payload, { wasteCode: "15 01 01" }, [
    { id: "c1", code: "15 01 01", name: "ambalaje de hârtie", hazardous: false, metalSuggested: false },
  ]);
  assert.equal(mv.wasteCodeId, "c1");
  assert.equal(mv.wasteCode, "15 01 01");
  assert.equal(mv.wasteCodeName, "ambalaje de hârtie");
  assert.equal(mv.quantity, 2.5);
  assert.equal(mv.operationCode, "R5");
  assert.equal(mv.vehicleRegistration, "CJ 12 ABC");
  assert.equal(mv.packagingOnMarket, null);
});

test("codul care nu mai e în profil se redeschide cu ce scrie pe rând", () => {
  const mv = queuedToMovement({ wasteCodeId: "c9", date: "2026-09-14" }, { wasteCode: "20 01 01" }, []);
  assert.equal(mv.wasteCodeId, "c9");
  assert.equal(mv.wasteCode, "20 01 01");
  assert.equal(mv.wasteCodeName, "");
});
