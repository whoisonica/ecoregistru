// Ce spune coada despre un rând care tot nu pleacă: `npm run test:outbox` (Node rulează TypeScript direct).
import assert from "node:assert/strict";
import { test } from "node:test";

import { retryNote, STUCK_AFTER, stuckOnServer, ticketStatus } from "./outboxRules.ts";

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

// Bonul de după „Salvează” (`app/gata.tsx`): ce spune despre rândul din coadă.
const pending = { state: "PENDING" as const, movementId: null, attempts: 0, error: null };

test("bonul: până la prima citire a cozii, rândul e pe drum sau în telefon, după semnal", () => {
  assert.equal(ticketStatus({ loaded: false, online: true }), "sending");
  assert.equal(ticketStatus({ loaded: false, online: false }), "queued");
});

test("bonul: rândul plecat din coadă e în registru", () => {
  assert.equal(ticketStatus({ loaded: true, online: true, row: undefined }), "sent");
});

test("bonul: cu semnal, prima încercare e „Se trimite”; fără semnal, „în telefon”", () => {
  assert.equal(ticketStatus({ loaded: true, online: true, row: pending }), "sending");
  assert.equal(ticketStatus({ loaded: true, online: false, row: pending }), "queued");
});

test("bonul: cu semnal, dar cu serverul care nu răspunde, rândul trece în „De trimis” după prima cădere", () => {
  // 27.09.2026: pe rețea bună, cu serverul oprit, bonul rămânea „Se trimite…” la nesfârșit.
  assert.equal(ticketStatus({ loaded: true, online: true, row: { ...pending, attempts: 1 } }), "queued");
  assert.equal(ticketStatus({ loaded: true, online: true, row: { ...pending, attempts: 2, error: "HTTP 503" } }), "queued");
});

test("bonul: predarea e pe server și a căzut poza; refuzul are starea lui", () => {
  assert.equal(ticketStatus({ loaded: true, online: true, row: { ...pending, movementId: "m", attempts: 1 } }), "photoFailed");
  assert.equal(ticketStatus({ loaded: true, online: false, row: { ...pending, state: "REJECTED", error: "x" } }), "rejected");
  assert.equal(ticketStatus({ loaded: true, online: true, row: { ...pending, state: "REJECTED", movementId: "m" } }), "photoFailed");
});
