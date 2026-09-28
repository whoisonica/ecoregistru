// Regulile celor trei pași ai formularului de predare (F4, valul B): `npm run test:steps`.
import assert from "node:assert/strict";
import { test } from "node:test";

import { stepBlocked, stepNote, stepOf } from "./handoverSteps.ts";

test("fiecare rubrică stă pe pasul ei: ce și cât · cui și cum · pe fișă și transport", () => {
  assert.equal(stepOf("workPoint"), 1);
  assert.equal(stepOf("date"), 1);
  assert.equal(stepOf("wasteCode"), 1);
  assert.equal(stepOf("quantity"), 1);
  assert.equal(stepOf("fate"), 2);
  assert.equal(stepOf("operationCode"), 2);
  assert.equal(stepOf("partner"), 2);
  assert.equal(stepOf("physicalState"), 3);
  assert.equal(stepOf("packagingCategory"), 3);
  assert.equal(stepOf("driverCnp"), 3);
  assert.equal(stepOf("documentReference"), 3);
  assert.equal(stepOf("vehicle"), 3);
});

test("„Continuă” se oprește la o eroare sau la o rubrică din poză neconfirmată de pe pasul curent, nu de pe altul", () => {
  assert.equal(stepBlocked(1, { date: "Scrie data" }, {}), true);
  assert.equal(stepBlocked(1, {}, { date: "Data 27.09.2026" }), true);
  assert.equal(stepBlocked(1, { physicalState: "Alege starea" }, { partner: "CUI 99900010" }), false);
  assert.equal(stepBlocked(2, { physicalState: "Alege starea" }, { partner: "CUI 99900010" }), true);
  assert.equal(stepBlocked(3, { physicalState: "Alege starea" }, {}), true);
  assert.equal(stepBlocked(3, {}, {}), false);
  // o eroare fără text nu e eroare
  assert.equal(stepBlocked(1, { date: undefined }, {}), false);
});

test("nota de sub buton: întâi rubricile de confirmat de pe pasul ăsta, apoi ce urmează", () => {
  assert.deepEqual(stepNote(1, { pending: { date: "x", vehicle: "y" }, singleWorkPoint: "Str. Tipografilor 4", online: true, hasPhoto: true }), { kind: "pending", n: 1 });
  assert.deepEqual(stepNote(1, { pending: {}, singleWorkPoint: "Str. Tipografilor 4", online: true, hasPhoto: true }), { kind: "singleWorkPoint", name: "Str. Tipografilor 4" });
  assert.deepEqual(stepNote(1, { pending: {}, singleWorkPoint: null, online: true, hasPhoto: true }), { kind: "none" });
  assert.deepEqual(stepNote(2, { pending: {}, singleWorkPoint: null, online: true, hasPhoto: true }), { kind: "nextSheet" });
  assert.deepEqual(stepNote(3, { pending: { vehicle: "y" }, singleWorkPoint: null, online: true, hasPhoto: true }), { kind: "pending", n: 1 });
  assert.deepEqual(stepNote(3, { pending: {}, singleWorkPoint: null, online: false, hasPhoto: true }), { kind: "offline" });
  assert.deepEqual(stepNote(3, { pending: {}, singleWorkPoint: null, online: true, hasPhoto: true }), { kind: "onlinePhoto" });
  assert.deepEqual(stepNote(3, { pending: {}, singleWorkPoint: null, online: true, hasPhoto: false }), { kind: "onlineNoPhoto" });
});
