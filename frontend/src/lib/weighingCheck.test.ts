import assert from "node:assert/strict";
import { test } from "node:test";
import { weighingCheck } from "@/lib/weighingCheck";

const row = (awaitingWeighing: boolean) => ({ awaitingWeighing });
const off = { isLoading: false, isError: false, data: undefined };

/** 29.09.2026: dosarul pleca înainte ca anii să vină, fără avertismentul „de cântărit”. */
test("cât un an încă se încarcă, verificarea nu e gata — nu se trece drept „nimic de cântărit”", () => {
  assert.deepEqual(weighingCheck([{ isLoading: true, isError: false }, off]), { state: "loading" });
});

test("un an care n-a putut fi citit nu e un an fără linii necântărite", () => {
  assert.deepEqual(
    weighingCheck([{ isLoading: false, isError: true }, { isLoading: true, isError: false }]),
    { state: "unknown" }
  );
});

test("cu anii veniți, liniile necântărite din toți; anii în afara perioadei nu contează", () => {
  const check = weighingCheck([
    { isLoading: false, isError: false, data: [row(true), row(false)] },
    { isLoading: false, isError: false, data: [row(true)] },
    off,
  ]);
  assert.equal(check.state, "ready");
  assert.equal(check.state === "ready" ? check.pending.length : -1, 2);
});
