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

// ── ce se scrie după o reîmprospătare care a durat ──────────────────────────

import { afterRefresh, keepPendingLogout } from "./refresh.ts";

const auth = (refreshToken: string | null, token = "acces") =>
  ({ token, refreshToken, role: "CONSULTANT", tenantId: "t0", tenantName: "Prima", consultancyName: null, email: "a@b.ro", deviceSessionId: "d" }) as const;

test("firma aleasă în timpul reîmprospătării rămâne cea aleasă", () => {
  const before = { auth: auth("r1"), tenantId: "t1", tenantName: "Unu" };
  const now = { ...before, tenantId: "t2", tenantName: "Doi" };
  const next = afterRefresh(before, now, auth("r2", "nou"));
  assert.equal(next?.tenantId, "t2");
  assert.equal(next?.auth.token, "nou");
  assert.equal(next?.auth.refreshToken, "r2");
});

test("omul ieșit din cont în timpul reîmprospătării rămâne afară", () => {
  const before = { auth: auth("r1"), tenantId: "t1", tenantName: "Unu" };
  assert.equal(afterRefresh(before, null, auth("r2")), null);
});

test("alt cont intrat între timp nu e înlocuit cu sesiunea celui vechi", () => {
  const before = { auth: auth("r1"), tenantId: "t1", tenantName: "Unu" };
  const other = { auth: auth("altul"), tenantId: "t9", tenantName: "Nouă" };
  assert.equal(afterRefresh(before, other, auth("r2")), null);
});

test("ieșirea fără semnal sau cu serverul căzut se reîncearcă; un refuz nu", () => {
  assert.equal(keepPendingLogout(new TypeError("Network request failed")), true);
  assert.equal(keepPendingLogout(apiError(503)), true);
  assert.equal(keepPendingLogout(apiError(429)), true);
  assert.equal(keepPendingLogout(apiError(400)), false);
  assert.equal(keepPendingLogout(null), false);
});
