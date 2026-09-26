// Corectura de pe telefon: `npm run test:edit` (Node rulează TypeScript direct, fără Jest).
import assert from "node:assert/strict";
import { test } from "node:test";

import type { WasteMovement } from "@web/types";

import { canEditOnPhone, editBody } from "./movementEdit.ts";

const mv = {
  id: "m1",
  workPointId: "wp1",
  date: "2026-09-14",
  wasteCodeId: "c1",
  quantity: 1250,
  weighedAtUnloading: false,
  volumeM3: 3.5,
  unit: "KG",
  operation: "RECOVERED",
  register: "ANEXA_1",
  physicalState: "SOLID",
  storageType: "RP",
  treatmentMethod: "TM",
  transportMeans: "AN",
  wasteDestination: "Vr",
  operationCode: "R3",
  partnerId: "p1",
  internalGeneratorId: "sec1",
  documentReference: "DRS 000417",
  notes: "scrisă pe web",
  loadDate: null,
  unloadDate: null,
  partnerWorkPointId: null,
  transportPartnerId: null,
  driverName: null,
  driverIdentification: null,
  driverCnp: null,
  vehicleRegistration: "CJ 12 ABC",
  transportDestinations: [],
  anexa3Unit: null,
  anexa2Number: "A2-7",
  anexa2ApprovalNumber: null,
  anexa2Packaging: null,
  anexa2BelowOneTon: null,
  packagingOnMarket: true,
  packagingMaterial: "PLASTIC_OTHER",
  packagingCategory: "PRIMARY",
  packagingReusable: true,
  packagingHazardousContent: false,
  packagingOrigin: "POPULATION",
  weighingOperationId: null,
} as unknown as WasteMovement;

test("rubricile de pe ecran înlocuiesc, restul rămân de pe server", () => {
  const body = editBody(mv, { quantity: 900, documentReference: "DRS 000420" });
  assert.equal(body.quantity, 900);
  assert.equal(body.documentReference, "DRS 000420");
  // Ce nu e pe telefon nu se golește.
  assert.equal(body.notes, "scrisă pe web");
  assert.equal(body.volumeM3, 3.5);
  assert.equal(body.treatmentMethod, "TM");
  assert.equal(body.internalGeneratorId, "sec1");
  assert.equal(body.anexa2Number, "A2-7");
  assert.equal(body.packagingReusable, true);
  assert.equal(body.packagingOrigin, "POPULATION");
});

test("un null pus de ecran golește rubrica", () => {
  assert.equal(editBody(mv, { vehicleRegistration: null }).vehicleRegistration, null);
});

test("cererea nu poartă câmpuri de răspuns", () => {
  const body = editBody(mv, {}) as Record<string, unknown>;
  for (const k of ["id", "weighingOperationId", "partnerName", "attachments", "clientGeneratedId"]) {
    assert.ok(!(k in body), k);
  }
});

test("se corectează pe telefon numai predările proprii de pe Anexa 1, fără cântar, de cine scrie", () => {
  assert.equal(canEditOnPhone(mv, true), true);
  assert.equal(canEditOnPhone(mv, false), false);
  assert.equal(canEditOnPhone({ ...mv, weighingOperationId: "op1" }, true), false);
  assert.equal(canEditOnPhone({ ...mv, register: "ART_48" }, true), false);
  assert.equal(canEditOnPhone({ ...mv, operation: "GENERATED" } as WasteMovement, true), false);
});
