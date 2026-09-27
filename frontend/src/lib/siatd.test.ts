import { test } from "node:test";
import assert from "node:assert/strict";
import { siatdPayload } from "./siatd";

/**
 * Setări → SIATD (F6a): un modul bifat fără „Înrolat din” nu pleacă la server — termenul curge de la data aceea, deci
 * o bifă fără dată ar da termene greșite. Ce nu e bifat nu se trimite, iar serverul îl debifează.
 */
test("trimite doar modulele bifate, cu data lor", () => {
  const result = siatdPayload({
    PACKAGING: { on: true, from: "2024-01-01" },
    MUNICIPAL: { on: false, from: "2025-06-05" },
  });
  assert.deepEqual(result, { ok: true, enrolledFrom: { PACKAGING: "2024-01-01" } });
});

test("un modul bifat fără dată e o eroare pe modulul acela", () => {
  const result = siatdPayload({ PACKAGING: { on: true, from: "" }, WEEE: { on: true, from: "2027-01-01" } });
  assert.deepEqual(result, { ok: false, missing: ["PACKAGING"] });
});

test("nimic bifat → cerere goală, adică toate debifate", () => {
  assert.deepEqual(siatdPayload({}), { ok: true, enrolledFrom: {} });
});
