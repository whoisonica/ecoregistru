import assert from "node:assert/strict";
import { test } from "node:test";

import { ago } from "./ago.ts";

const now = Date.parse("2026-09-27T14:22:00Z");

test("fără moment de actualizare nu e nicio vârstă", () => {
  assert.equal(ago(0, now), null);
});

test("sub un minut e „acum”, apoi minute, ore, zile", () => {
  assert.deepEqual(ago(now - 20_000, now), { kind: "now" });
  assert.deepEqual(ago(now - 3 * 60_000, now), { kind: "minutes", n: 3 });
  assert.deepEqual(ago(now - 59 * 60_000, now), { kind: "minutes", n: 59 });
  assert.deepEqual(ago(now - 2 * 3_600_000, now), { kind: "hours", n: 2 });
  assert.deepEqual(ago(now - 30 * 3_600_000, now), { kind: "days", n: 1 });
});

test("un ceas dat înainte nu dă o vârstă negativă", () => {
  assert.deepEqual(ago(now + 60_000, now), { kind: "now" });
});
