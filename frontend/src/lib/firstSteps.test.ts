import assert from "node:assert/strict";
import { test } from "node:test";
import { firstSteps, showFirstSteps } from "@/lib/firstSteps";
import type { Company } from "@/lib/types";

const empty: Company = { id: "c", name: "Nou SRL", cui: "RO1", type: "GENERATOR", active: true };
const full: Company = { ...empty, address: "Cluj", caenCode: "1071", wasteManagerName: "Popescu Andrei" };

test("nimic nu se socotește până nu vin toate sursele", () => {
  const base = { company: full, workPoints: [], partners: [], evidences: [], movementCount: 0 };
  assert.equal(firstSteps({ ...base, company: undefined }), null);
  assert.equal(firstSteps({ ...base, workPoints: undefined }), null);
  assert.equal(firstSteps({ ...base, partners: undefined }), null);
  assert.equal(firstSteps({ ...base, evidences: undefined }), null);
  assert.equal(firstSteps({ ...base, movementCount: undefined }), null);
});

test("un cont gol are toți pașii de făcut, iar la firmă spune ce rubrici lipsesc", () => {
  const steps = firstSteps({ company: empty, workPoints: [], partners: [], evidences: [], movementCount: 0 })!;
  assert.deepEqual(steps.map((s) => s.id), ["company", "workPoint", "partner", "movement"]);
  assert.ok(steps.every((s) => !s.done));
  assert.deepEqual(steps[0].missing, ["address", "caenCode", "wasteManagerName"]);
});

test("o rubrică cu spații nu e completată", () => {
  const steps = firstSteps({
    company: { ...full, caenCode: "  " }, workPoints: [], partners: [], evidences: [], movementCount: 0,
  })!;
  assert.equal(steps[0].done, false);
  assert.deepEqual(steps[0].missing, ["caenCode"]);
});

test("fiecare pas se bifează din datele lui", () => {
  const steps = firstSteps({ company: full, workPoints: [{}], partners: [{}], evidences: [], movementCount: 1 })!;
  assert.ok(steps.every((s) => s.done));
  // O evidență pe an fără mișcări luna asta (import, sau luni trecute) tot înseamnă că s-a început.
  const earlier = firstSteps({ company: full, workPoints: [{}], partners: [{}], evidences: [{}], movementCount: 0 })!;
  assert.equal(earlier[3].done, true);
});

test("lista e a contului nou: un cont care lucrează nu o vede doar pentru datele firmei", () => {
  const working = firstSteps({ company: empty, workPoints: [{}], partners: [{}], evidences: [{}], movementCount: 3 });
  assert.equal(showFirstSteps(working), false);
  const fresh = firstSteps({ company: full, workPoints: [{}], partners: [], evidences: [], movementCount: 0 });
  assert.equal(showFirstSteps(fresh), true);
  assert.equal(showFirstSteps(null), false);
});
