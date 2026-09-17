import { test } from "node:test";
import assert from "node:assert/strict";
import { fgoCounty } from "@/lib/counties";

test("județul de la ANAF ajunge în forma FGO, fără diacritice", () => {
  assert.equal(fgoCounty("BIHOR"), "Bihor");
  assert.equal(fgoCounty("BISTRIŢA-NĂSĂUD"), "Bistrita-Nasaud");
  assert.equal(fgoCounty("Caraș-Severin"), "Caras-Severin");
  assert.equal(fgoCounty("SATU MARE"), "Satu Mare");
  assert.equal(fgoCounty("MUNICIPIUL BUCUREŞTI"), "Bucuresti");
});

test("ce nu e județ rămâne necompletat, nu ghicit", () => {
  assert.equal(fgoCounty(null), null);
  assert.equal(fgoCounty(""), null);
  assert.equal(fgoCounty("Transilvania"), null);
});
