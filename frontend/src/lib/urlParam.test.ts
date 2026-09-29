import assert from "node:assert/strict";
import { test } from "node:test";
import { withUrlParam } from "@/lib/urlParam";

/**
 * „Șterge filtrele” (29.09.2026): patru setteri în același clic. Fiecare trebuie să pornească de la adresa
 * lăsată de cel dinainte — așa se comportă `useUrlState` acum, citind `window.location.search`, pe care
 * `BrowserRouter` îl scrie sincron. Din instantaneul randării, ultimul câștiga și rămâneau trei filtre.
 */
test("mai multe schimbări în același clic se adună, nu se calcă", () => {
  let live = "luna=2026-03&punct=wp1&problema=fara-cod&ambalaje=1&tab=iesiri";
  for (const [key, next, fallback] of [
    ["luna", "2026-09", "2026-09"],
    ["punct", "", ""],
    ["problema", "", ""],
    ["ambalaje", "", ""],
  ] as const) {
    live = withUrlParam(live, key, next, fallback);
  }
  assert.equal(live, "tab=iesiri");
});

test("instantaneul vechi pierde toate schimbările în afară de ultima — defectul reparat", () => {
  const snapshot = "luna=2026-03&punct=wp1&problema=fara-cod";
  let written = snapshot;
  for (const key of ["luna", "punct", "problema"]) written = withUrlParam(snapshot, key, "", "");
  assert.equal(written, "luna=2026-03&punct=wp1");
});

test("valoarea implicită nu se scrie, cea nouă se pune sau se înlocuiește", () => {
  assert.equal(withUrlParam("?an=2025", "an", "2026", "2026"), "");
  assert.equal(withUrlParam("an=2025", "luna", "3", ""), "an=2025&luna=3");
  assert.equal(withUrlParam("an=2025", "an", "2024", ""), "an=2024");
});
