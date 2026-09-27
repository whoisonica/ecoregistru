// Ce vede omul când serverul refuză: `npm test`.
import assert from "node:assert/strict";
import { test } from "node:test";

import { loginErrorText } from "./errors.ts";

const texts = { generic: "Datele nu se potrivesc.", unreachable: "Nu am ajuns la server." };
const apiError = (status: number, serverMessage: string | null = null) =>
  Object.assign(new Error(`HTTP ${status}`), { status, serverMessage });

test("propoziția serverului ajunge la om: cont oprit, prea multe încercări, cont neactivat", () => {
  assert.equal(loginErrorText(apiError(429, "Prea multe încercări. Mai încearcă peste 15 minute."), texts), "Prea multe încercări. Mai încearcă peste 15 minute.");
  assert.equal(loginErrorText(apiError(403, "Contul a fost dezactivat."), texts), "Contul a fost dezactivat.");
});

test("fără propoziție, textul general; fără răspuns, „n-am ajuns la server”", () => {
  assert.equal(loginErrorText(apiError(401), texts), texts.generic);
  assert.equal(loginErrorText(new TypeError("Network request failed"), texts), texts.unreachable);
});
