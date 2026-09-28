// Ce spune coada despre un rând care tot nu pleacă: `npm run test:outbox` (Node rulează TypeScript direct).
import assert from "node:assert/strict";
import { test } from "node:test";

import { coalesce, PhotoMissingError, rejectsRow, retryNote, skipsToNext, STUCK_AFTER, stuckOnServer, ticketStatus } from "./outboxRules.ts";

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

test("o cădere 5xx trece la rândul următor; rețeaua, timpul expirat și 429 opresc coada", () => {
  assert.equal(skipsToNext(apiError(500)), true, "poza unei predări salvate cade pe server: următoarea pleacă");
  assert.equal(skipsToNext(apiError(503)), true);
  assert.equal(skipsToNext(apiError(429)), false, "prea multe cereri: așteaptă toată coada");
  assert.equal(skipsToNext(new TypeError("Network request failed")), false, "fără semnal");
  assert.equal(skipsToNext(Object.assign(new Error("timeout"), { name: "TimeoutError" })), false, "timp expirat");
});

// B1 (28.09.2026): o eroare a telefonului — poza ștearsă de pe disc, o citire căzută — n-are status și nu e de rețea.
test("o eroare a telefonului pe un rând nu ține pe loc coada: trece la următorul", () => {
  assert.equal(skipsToNext(new Error("File does not exist")), true, "poza nu mai e pe disc");
  assert.equal(skipsToNext(new RangeError("orice altceva al nostru")), true);
  // rămân opritoare: fără semnal, timp expirat, 429
  assert.equal(skipsToNext(new TypeError("Network request failed (no answer in 30 s)")), false);
  assert.equal(skipsToNext(Object.assign(new Error("aborted"), { name: "AbortError" })), false);
});

test("poza care nu mai e pe telefon e un refuz: nicio reîncercare n-o aduce înapoi", () => {
  assert.equal(rejectsRow(new PhotoMissingError()), true);
  assert.equal(skipsToNext(new PhotoMissingError()), true);
  assert.equal(rejectsRow(apiError(400)), true);
  assert.equal(rejectsRow(apiError(422)), true);
  assert.equal(rejectsRow(apiError(429)), false, "prea multe cereri se reîncearcă");
  assert.equal(rejectsRow(apiError(500)), false);
  assert.equal(rejectsRow(new TypeError("Network request failed")), false);
  assert.equal(rejectsRow(new Error("File does not exist")), false, "altă eroare a telefonului se reîncearcă, fără să oprească restul");
});

// B6 (28.09.2026): „Trimite acum” apăsat cât o trimitere e în aer primea trimiterea veche — rândurile scoase din
// pauză după ce ea își citise lista rămâneau pe loc până la următoarea ocazie.
test("o cerere venită cât coada e în aer mai face o trecere după ea, nu una paralelă", async () => {
  let running = 0;
  let maxParallel = 0;
  let runs = 0;
  let release!: () => void;
  const first = new Promise<void>((r) => (release = r));
  const drain = coalesce(async () => {
    runs++;
    running++;
    maxParallel = Math.max(maxParallel, running);
    if (runs === 1) await first;
    running--;
    return runs;
  });
  const a = drain();
  const b = drain();
  const c = drain();
  release();
  assert.equal(await a, 1, "prima trecere întoarce ce a trimis ea");
  assert.equal(await b, 2, "cererile din timpul ei primesc trecerea de după");
  assert.equal(await c, 2, "două cereri în aer se strâng într-o singură trecere în plus");
  assert.equal(runs, 2);
  assert.equal(maxParallel, 1, "niciodată două trimiteri deodată");
  assert.equal(await drain(), 3, "după liniște, o cerere nouă pornește o trecere nouă");
});
