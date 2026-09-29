import assert from "node:assert/strict";
import { test } from "node:test";
import { MOVEMENT_DEPENDENT_KEYS } from "@/lib/movementQueries";

/** 29.09.2026: după o salvare, Ambalajele și dosarul rămâneau pe cifrele vechi până la 30 de secunde. */
test("o scriere de mișcare recitește și Ambalajele, și dosarul de control", () => {
  const roots = MOVEMENT_DEPENDENT_KEYS.map((k) => k[0]);
  for (const root of ["movements", "evidences", "packaging", "audit-file-contents", "audit-file-size"]) {
    assert.ok(roots.includes(root), `lipsește ${root}`);
  }
});
