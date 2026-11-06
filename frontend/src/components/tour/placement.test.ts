import assert from "node:assert/strict";
import { test } from "node:test";
import {
  BOX_GAP,
  CUTOUT_PADDING,
  TOUR_BOX_WIDTH,
  VIEWPORT_MARGIN,
  boxWidthFor,
  cutoutRect,
  placeBox,
} from "@/components/tour/placement";

const desktop = { width: 1440, height: 900 };
const phone = { width: 375, height: 812 };
const box = { width: TOUR_BOX_WIDTH, height: 180 };

test("sub țintă când încape, centrată pe ea", () => {
  const pos = placeBox({ top: 100, left: 500, width: 200, height: 40 }, box, desktop);
  assert.equal(pos.side, "below");
  assert.equal(pos.top, 100 + 40 + CUTOUT_PADDING + BOX_GAP);
  assert.equal(pos.left, 600 - TOUR_BOX_WIDTH / 2);
  assert.equal(pos.width, TOUR_BOX_WIDTH);
});

test("deasupra când dedesubt nu mai e loc", () => {
  const pos = placeBox({ top: 700, left: 500, width: 200, height: 40 }, box, desktop);
  assert.equal(pos.side, "above");
  assert.equal(pos.top, 700 - CUTOUT_PADDING - BOX_GAP - 180);
});

test("la dreapta unei ținte înalte (meniul din stânga), centrată pe verticală", () => {
  const pos = placeBox({ top: 0, left: 0, width: 262, height: 900 }, box, desktop);
  assert.equal(pos.side, "right");
  assert.equal(pos.left, 262 + CUTOUT_PADDING + BOX_GAP);
  assert.equal(pos.top, 450 - 90);
});

test("la stânga unei ținte înalte lipite de marginea din dreapta", () => {
  const pos = placeBox({ top: 0, left: 1200, width: 240, height: 900 }, box, desktop);
  assert.equal(pos.side, "left");
  assert.equal(pos.left, 1200 - CUTOUT_PADDING - BOX_GAP - TOUR_BOX_WIDTH);
});

test("o țintă cât fereastra: caseta stă jos, peste ea", () => {
  const pos = placeBox({ top: 0, left: 0, width: 1440, height: 900 }, box, desktop);
  assert.equal(pos.side, "overlap");
  assert.equal(pos.top, 900 - VIEWPORT_MARGIN - 180);
  assert.equal(pos.left, 720 - TOUR_BOX_WIDTH / 2);
});

test("nu iese la mai puțin de 16px de margini", () => {
  const nearRight = placeBox({ top: 100, left: 1380, width: 40, height: 30 }, box, desktop);
  assert.equal(nearRight.side, "below");
  assert.equal(nearRight.left, 1440 - VIEWPORT_MARGIN - TOUR_BOX_WIDTH);

  const nearLeft = placeBox({ top: 100, left: 0, width: 30, height: 30 }, box, desktop);
  assert.equal(nearLeft.left, VIEWPORT_MARGIN);

  // Mai înaltă decât fereastra: lipită sus, nu cu vârful afară.
  const tall = placeBox(null, { width: TOUR_BOX_WIDTH, height: 400 }, { width: 1440, height: 300 });
  assert.equal(tall.top, VIEWPORT_MARGIN);
});

test("fără țintă (sau cu ținta în afara ferestrei) caseta stă în mijloc", () => {
  const centered = placeBox(null, box, desktop);
  assert.deepEqual(centered, { top: 360, left: 544, width: TOUR_BOX_WIDTH, side: "center" });

  const offscreen = placeBox({ top: 1200, left: 500, width: 200, height: 40 }, box, desktop);
  assert.equal(offscreen.side, "center");
});

test("pe ecran îngust caseta e lipită jos, pe toată lățimea, cu sau fără țintă", () => {
  const narrowBox = { width: boxWidthFor(phone.width), height: 200 };
  const expected = { top: 812 - VIEWPORT_MARGIN - 200, left: VIEWPORT_MARGIN, width: 375 - 2 * VIEWPORT_MARGIN, side: "pinned" };
  assert.deepEqual(placeBox({ top: 100, left: 20, width: 300, height: 40 }, narrowBox, phone), expected);
  assert.deepEqual(placeBox(null, narrowBox, phone), expected);
  // 1023px e încă îngust (pragul `lg`), 1024px e lat.
  assert.equal(placeBox(null, box, { width: 1023, height: 800 }).side, "pinned");
  assert.equal(placeBox(null, box, { width: 1024, height: 800 }).side, "center");
});

test("lățimea casetei și decupajul", () => {
  assert.equal(boxWidthFor(1440), TOUR_BOX_WIDTH);
  assert.equal(boxWidthFor(375), 343);
  assert.equal(boxWidthFor(1023), 991);
  assert.deepEqual(cutoutRect({ top: 10, left: 20, width: 100, height: 30 }), {
    top: 10 - CUTOUT_PADDING,
    left: 20 - CUTOUT_PADDING,
    width: 100 + 2 * CUTOUT_PADDING,
    height: 30 + 2 * CUTOUT_PADDING,
  });
});
