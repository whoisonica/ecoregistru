// Unde e poza după o actualizare a aplicației: `npm test` (Node rulează TypeScript direct).
import assert from "node:assert/strict";
import { test } from "node:test";

import { rebasePhoto } from "./photoPath.ts";

const OLD = "file:///var/mobile/Containers/Data/Application/0A1B2C3D-1111-2222-3333-444455556666";
const NEW = "file:///var/mobile/Containers/Data/Application/9F8E7D6C-AAAA-BBBB-CCCC-DDDDEEEEFFFF";

test("iOS: o poză ținută sub containerul vechi se caută sub cel de acum", () => {
  assert.equal(rebasePhoto(`${OLD}/Documents/outbox/r1.jpg`, `${NEW}/Documents/`), `${NEW}/Documents/outbox/r1.jpg`);
  // și ciorna, a cărei poză stă în Caches
  assert.equal(
    rebasePhoto(`${OLD}/Library/Caches/ImageManipulator/a.jpg`, `${NEW}/Documents/`),
    `${NEW}/Library/Caches/ImageManipulator/a.jpg`,
  );
});

test("simulatorul are același container sub alt dosar", () => {
  const sim = (id: string) => `file:///Users/x/Library/Developer/CoreSimulator/Devices/D1/data/Containers/Data/Application/${id}`;
  assert.equal(rebasePhoto(`${sim("A")}/Documents/outbox/r.jpg`, `${sim("B")}/Documents/`), `${sim("B")}/Documents/outbox/r.jpg`);
});

test("fără container iOS (Android) sau fără poză, adresa rămâne cum e", () => {
  const android = "file:///data/user/0/ro.wastehouse.app/files/outbox/r.jpg";
  assert.equal(rebasePhoto(android, "file:///data/user/0/ro.wastehouse.app/files/"), android);
  assert.equal(rebasePhoto(null, `${NEW}/Documents/`), null);
  assert.equal(rebasePhoto(`${NEW}/Documents/outbox/r.jpg`, `${NEW}/Documents/`), `${NEW}/Documents/outbox/r.jpg`);
});
