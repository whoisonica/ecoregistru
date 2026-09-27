// Ce spune coada despre un rând care tot nu pleacă: `npm run test:outbox` (Node rulează TypeScript direct).
import assert from "node:assert/strict";
import { test } from "node:test";

import { retryNote, STUCK_AFTER, stuckOnServer } from "./outboxRules.ts";

const apiError = (status: number, serverMessage: string | null = null) =>
  Object.assign(new Error(`HTTP ${status}`), { status, serverMessage });

test("o eroare a serverului se vede pe rând, ca omul să știe de ce nu pleacă", () => {
  assert.equal(retryNote(apiError(500)), "HTTP 500");
  assert.equal(retryNote(apiError(503, "Serverul e în mentenanță.")), "Serverul e în mentenanță.");
  assert.equal(retryNote(apiError(429)), "HTTP 429");
});

test("fără semnal nu se scrie nimic pe rând: e starea obișnuită a rampei", () => {
  assert.equal(retryNote(new TypeError("Network request failed")), null);
  assert.equal(retryNote(new Error("orice")), null);
});

test("un rând pe care serverul îl tot respinge cu 500 se raportează o dată", () => {
  assert.equal(stuckOnServer(apiError(500), STUCK_AFTER - 1), false);
  assert.equal(stuckOnServer(apiError(500), STUCK_AFTER), true);
  assert.equal(stuckOnServer(apiError(500), STUCK_AFTER + 1), false);
  // 429 și lipsa semnalului nu sunt defecte de raportat
  assert.equal(stuckOnServer(apiError(429), STUCK_AFTER), false);
  assert.equal(stuckOnServer(new TypeError("Network request failed"), STUCK_AFTER), false);
});
