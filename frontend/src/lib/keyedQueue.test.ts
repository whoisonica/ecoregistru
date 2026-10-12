import assert from "node:assert/strict";
import { test } from "node:test";
import { createKeyedQueue } from "@/lib/keyedQueue";

const later = (ms: number) => new Promise<void>((resolve) => setTimeout(resolve, ms));

test("aceeași celulă: ștergerea pusă după salvare rulează după ea, nu înaintea ei", async () => {
  const enqueue = createKeyedQueue();
  const server: (number | null)[] = [];
  // Salvarea cifrei anului răspunde greu; „Pe luni” e apăsat cât ea e încă în zbor.
  const save = enqueue("DIESEL-0", async () => {
    await later(20);
    server.push(1200);
  });
  const clear = enqueue("DIESEL-0", () => {
    server.push(null);
  });
  await Promise.all([save, clear]);
  assert.deepEqual(server, [1200, null]);
});

test("celule diferite nu se așteaptă", async () => {
  const enqueue = createKeyedQueue();
  const order: string[] = [];
  const slow = enqueue("DIESEL-1", async () => {
    await later(20);
    order.push("motorină");
  });
  const fast = enqueue("ELECTRICITY-1", () => {
    order.push("electricitate");
  });
  await Promise.all([slow, fast]);
  assert.deepEqual(order, ["electricitate", "motorină"]);
});

test("o sarcină căzută nu oprește coada celulei", async () => {
  const enqueue = createKeyedQueue();
  const failed = enqueue("COAL-0", () => {
    throw new Error("rețea");
  });
  await assert.rejects(failed);
  let ran = false;
  await enqueue("COAL-0", () => {
    ran = true;
  });
  assert.equal(ran, true);
});
