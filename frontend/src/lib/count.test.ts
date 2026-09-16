import assert from "node:assert/strict";
import { test } from "node:test";
import { countOf } from "@/lib/count";

test("numeralul românesc: 1, 2–19, apoi „de” de la 20, reluat la fiecare sută", () => {
  assert.equal(countOf(1, "linie", "linii"), "1 linie");
  assert.equal(countOf(2, "linie", "linii"), "2 linii");
  assert.equal(countOf(19, "linie", "linii"), "19 linii");
  assert.equal(countOf(20, "linie", "linii"), "20 de linii");
  assert.equal(countOf(100, "linie", "linii"), "100 de linii");
  assert.equal(countOf(101, "linie", "linii"), "101 linii");
  assert.equal(countOf(119, "linie", "linii"), "119 linii");
  assert.equal(countOf(120, "linie", "linii"), "120 de linii");
});
