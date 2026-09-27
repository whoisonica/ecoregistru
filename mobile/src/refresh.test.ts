// Când iese omul din cont la o reîmprospătare căzută: `npm run test:refresh`.
import assert from "node:assert/strict";
import { test } from "node:test";

import { sessionIsDead } from "./refresh.ts";

/** Forma lui `ApiError` din `api.ts` (care nu se poate importa aici: trage `expo-file-system`). */
function apiError(status: number) {
  return Object.assign(new Error(`HTTP ${status}`), { status });
}

test("refuzul serverului (400 device.session.invalid) scoate din cont", () => {
  assert.equal(sessionIsDead(apiError(400)), true);
  assert.equal(sessionIsDead(apiError(401)), true);
  assert.equal(sessionIsDead(apiError(403)), true);
});

test("fără semnal omul rămâne în cont", () => {
  assert.equal(sessionIsDead(new TypeError("Network request failed")), false);
});

test("serverul căzut sau ocupat nu scoate din cont", () => {
  assert.equal(sessionIsDead(apiError(500)), false);
  assert.equal(sessionIsDead(apiError(503)), false);
  assert.equal(sessionIsDead(apiError(429)), false);
});

test("o eroare fără stare nu e un refuz", () => {
  assert.equal(sessionIsDead(new Error("orice")), false);
  assert.equal(sessionIsDead(null), false);
});
