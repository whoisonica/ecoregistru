import assert from "node:assert/strict";
import { test } from "node:test";
import { signOutRequest } from "@/lib/signOut";

/** 29.09.2026: cererea pleca pe o rută fără `/api/v1` și fără token (interceptorul rula după golire). */
test("deconectarea merge pe ruta serverului și poartă tokenul citit înainte de golire", () => {
  assert.deepEqual(signOutRequest("abc"), {
    url: "/api/v1/auth/sign-out",
    headers: { Authorization: "Bearer abc" },
  });
});

test("fără token nu pleacă nicio cerere de deconectare", () => {
  assert.equal(signOutRequest(null), null);
  assert.equal(signOutRequest(""), null);
});
