import assert from "node:assert/strict";
import { test } from "node:test";
import { registersFor, screenOfMovement, screensFor } from "@/lib/movementScreens";

test("fiecare tip de firmă își vede ecranele", () => {
  assert.deepEqual(screensFor("GENERATOR"), ["GENERATED"]);
  assert.deepEqual(screensFor("COLLECTOR"), ["IN", "OUT"]);
  assert.deepEqual(screensFor("BOTH"), ["GENERATED", "IN", "OUT"]);
  assert.deepEqual(registersFor("COLLECTOR"), ["ART_48"]);
  assert.deepEqual(registersFor("BOTH"), ["ANEXA_1", "ART_48"]);
});

test("o mișcare se deschide pe ecranul ei, iar o ieșire fără cod stă pe „Ieșiri”", () => {
  assert.equal(screenOfMovement("ANEXA_1", "RECOVERED", "BOTH"), "GENERATED");
  assert.equal(screenOfMovement("ART_48", "COLLECTED", "BOTH"), "IN");
  assert.equal(screenOfMovement("ART_48", "UNCLASSIFIED_OUT", "BOTH"), "OUT");
  // Ecranul nu e vizibil tipului de firmă: cade pe primul pe care îl vede.
  assert.equal(screenOfMovement("ART_48", "COLLECTED", "GENERATOR"), "GENERATED");
  assert.equal(screenOfMovement("ANEXA_1", "GENERATED", "COLLECTOR"), "IN");
});
