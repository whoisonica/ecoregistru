// Cererea agățată: `npm run test:timeout` (Node rulează TypeScript direct, fără Jest).
import assert from "node:assert/strict";
import { test } from "node:test";

import { fetchWithTimeout } from "./timeout.ts";
import { sessionIsDead } from "./refresh.ts";

/** Un server care primește cererea și nu mai răspunde niciodată — dar se oprește când e anulat. */
const hanging: typeof fetch = (_input, init) =>
  new Promise((_resolve, reject) => {
    init?.signal?.addEventListener("abort", () => reject(new DOMException("Aborted", "AbortError")));
  });

test("o cerere fără răspuns cade după timp, ca una fără semnal", async () => {
  const started = Date.now();
  await assert.rejects(fetchWithTimeout("https://x", {}, 50, hanging), (error: unknown) => {
    // Aceeași formă ca `fetch` fără rețea: coada o reîncearcă, reîmprospătarea nu scoate din cont.
    assert.ok(error instanceof TypeError);
    assert.match((error as Error).message, /network request failed/i);
    assert.equal(sessionIsDead(error), false);
    return true;
  });
  assert.ok(Date.now() - started < 1000);
});

test("un răspuns venit la timp trece neatins", async () => {
  const ok: typeof fetch = async () => new Response("{}", { status: 200 });
  const res = await fetchWithTimeout("https://x", {}, 50, ok);
  assert.equal(res.status, 200);
});

test("o eroare a rețelei trece mai departe așa cum e", async () => {
  const offline: typeof fetch = async () => {
    throw new TypeError("Network request failed");
  };
  await assert.rejects(fetchWithTimeout("https://x", {}, 50, offline), /Network request failed/);
});
