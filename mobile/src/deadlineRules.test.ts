// Bifarea termenului de pe telefon (F5, valul B): `npm run test:deadlines`.
import assert from "node:assert/strict";
import { test } from "node:test";

import { canCompleteOnPhone } from "./deadlineRules.ts";

const d = { id: "d1", status: "UPCOMING", computed: false } as const;

test("se bifează un termen salvat, nebifat, de cine scrie — și unul depășit", () => {
  assert.equal(canCompleteOnPhone(d, true), true);
  assert.equal(canCompleteOnPhone({ ...d, status: "OVERDUE" }, true), true);
});

test("nu: vizualizarea, un termen deja bifat, un rând socotit de pe „Trecute”", () => {
  assert.equal(canCompleteOnPhone(d, false), false, "viewer");
  assert.equal(canCompleteOnPhone({ ...d, status: "DONE" }, true), false, "bifat");
  assert.equal(canCompleteOnPhone({ ...d, computed: true }, true), false, "socotit");
  assert.equal(canCompleteOnPhone({ ...d, id: null as never }, true), false, "fără id");
});
