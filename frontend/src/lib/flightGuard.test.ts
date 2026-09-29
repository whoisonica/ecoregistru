import assert from "node:assert/strict";
import { test } from "node:test";
import { flightGuard } from "@/lib/flightGuard";

/** 29.09.2026: dublu-clic pe „Salvează” = două mișcări; pe prima Anexa 3 = 409 la a doua cerere. */
test("al doilea clic, cât primul e în zbor, nu mai pleacă", async () => {
  const guard = flightGuard();
  let calls = 0;
  const releases: (() => void)[] = [];
  const slow = () =>
    new Promise<void>((resolve) => {
      calls++;
      releases.push(resolve);
    });
  const first = guard.run("m1", slow);
  const second = guard.run("m1", slow);
  assert.equal(guard.isBusy("m1"), true);
  releases.forEach((release) => release());
  assert.equal(await second, false);
  assert.equal(await first, true);
  assert.equal(calls, 1);
  assert.equal(guard.isBusy("m1"), false);
});

test("după terminare, sau după o eroare, se poate din nou; alte chei nu se blochează între ele", async () => {
  const guard = flightGuard();
  await assert.rejects(guard.run("m1", async () => { throw new Error("rețea"); }));
  assert.equal(guard.isBusy("m1"), false);
  let ran = 0;
  await guard.run("m1", async () => { ran++; });
  const other = guard.run("m2", async () => { ran++; });
  assert.equal(await guard.run("m3", async () => { ran++; }), true);
  await other;
  assert.equal(ran, 3);
});
