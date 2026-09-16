import assert from "node:assert/strict";
import { test } from "node:test";
import {
  D_CODES,
  R_CODES,
  isExit,
  operationsFor,
  suggestedDestinations,
  suggestedPackagingMaterial,
} from "@/components/movements/movementRules";

test("ecranul decide operațiunile oferite", () => {
  assert.deepEqual(operationsFor("ANEXA_1", undefined), ["GENERATED"]);
  assert.deepEqual(operationsFor("ART_48", "IN"), ["COLLECTED"]);
  assert.deepEqual(operationsFor("ART_48", "OUT"), ["RECOVERED", "DISPOSED"]);
  assert.deepEqual(operationsFor("ART_48", undefined), ["COLLECTED", "RECOVERED", "DISPOSED"]);
});

test("doar valorificarea și eliminarea scot cantitatea de pe amplasament", () => {
  assert.equal(isExit("RECOVERED"), true);
  assert.equal(isExit("DISPOSED"), true);
  assert.equal(isExit("GENERATED"), false);
  assert.equal(isExit("COLLECTED"), false);
  assert.ok(R_CODES.length > 0 && R_CODES.every((c) => c.startsWith("R")));
  assert.ok(D_CODES.length > 0 && D_CODES.every((c) => c.startsWith("D")));
});

test("materialul se propune numai unde codul îl decide singur", () => {
  assert.equal(suggestedPackagingMaterial("15 01 01 — ambalaje de hârtie"), "HARTIE_CARTON");
  assert.equal(suggestedPackagingMaterial("15 01 02 — ambalaje de materiale plastice"), "ALTE_PLASTICE");
  // Metalicele acoperă și aluminiul, și oțelul: se întreabă, nu se ghicește.
  assert.equal(suggestedPackagingMaterial("15 01 04 — ambalaje metalice"), null);
});

test("casetele „Destinat:” de pe Anexa 3 urmează destinatarul, iar eliminarea nu se prebifează", () => {
  assert.deepEqual(suggestedDestinations("COLLECTOR", "RECOVERED"), ["COLECTARE", "VALORIFICARE"]);
  assert.deepEqual(suggestedDestinations("RECOVERER", "RECOVERED"), ["VALORIFICARE"]);
  assert.deepEqual(suggestedDestinations("COLLECTOR", "DISPOSED"), []);
  assert.deepEqual(suggestedDestinations(undefined, "RECOVERED"), []);
});
