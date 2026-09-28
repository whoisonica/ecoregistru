import assert from "node:assert/strict";
import { test } from "node:test";
import {
  D_CODES,
  destinationsFor,
  destinationsOpen,
  R_CODES,
  isExit,
  operationsFor,
  suggestedDestinations,
  suggestedPackagingMaterial,
} from "@/components/movements/movementRules";

test("ecranul decide operațiunile oferite", () => {
  assert.deepEqual(operationsFor("ANEXA_1", undefined), ["GENERATED"]);
  assert.deepEqual(operationsFor("ART_48", "IN"), ["COLLECTED"]);
  assert.deepEqual(operationsFor("ART_48", "OUT"), ["RECOVERED", "DISPOSED"]);
  assert.deepEqual(operationsFor("ART_48", undefined), ["COLLECTED", "RECOVERED", "DISPOSED"]);
});

test("doar valorificarea și eliminarea scot cantitatea de pe amplasament", () => {
  assert.equal(isExit("RECOVERED"), true);
  assert.equal(isExit("DISPOSED"), true);
  assert.equal(isExit("GENERATED"), false);
  assert.equal(isExit("COLLECTED"), false);
  assert.ok(R_CODES.length > 0 && R_CODES.every((c) => c.startsWith("R")));
  assert.ok(D_CODES.length > 0 && D_CODES.every((c) => c.startsWith("D")));
});

test("materialul se propune numai unde codul îl decide singur", () => {
  assert.equal(suggestedPackagingMaterial("15 01 01 — ambalaje de hârtie"), "HARTIE_CARTON");
  assert.equal(suggestedPackagingMaterial("15 01 02 — ambalaje de materiale plastice"), "ALTE_PLASTICE");
  // Metalicele acoperă și aluminiul, și oțelul: se întreabă, nu se ghicește.
  assert.equal(suggestedPackagingMaterial("15 01 04 — ambalaje metalice"), null);
});

test("casetele „Destinat:” de pe Anexa 3 urmează destinatarul, iar eliminarea nu se prebifează", () => {
  assert.deepEqual(suggestedDestinations("COLLECTOR", "RECOVERED"), ["COLECTARE", "VALORIFICARE"]);
  assert.deepEqual(suggestedDestinations("RECOVERER", "RECOVERED"), ["VALORIFICARE"]);
  assert.deepEqual(suggestedDestinations("COLLECTOR", "DISPOSED"), []);
  assert.deepEqual(suggestedDestinations(undefined, "RECOVERED"), []);
});

/**
 * Nota 5 pe tabere (20.09.2026). Proba ține două lucruri deodată: că regula chiar desparte, și că
 * **nu pierde niciun cod** — reducerea listei la patru valori a fost tocmai greșeala de reparat.
 */
test("destinațiile se oferă pe tabăra operațiunii, fără să dispară vreun cod al notei 5", () => {
  assert.deepEqual(destinationsFor("RECOVERED"), ["Vr", "P", "Ve", "A"]);
  assert.deepEqual(destinationsFor("DISPOSED"), ["DO", "HP", "HC", "I", "A"]);

  // Fără operațiune aleasă (o intrare, o generare fără soartă) se oferă toate opt.
  assert.equal(destinationsFor("").length, 8);

  // Niciun cod al notei 5 nu rămâne pe dinafară: reuniunea celor două tabere le acoperă pe toate.
  const reunite = new Set([...destinationsFor("RECOVERED"), ...destinationsFor("DISPOSED")]);
  assert.deepEqual([...reunite].sort(), ["A", "DO", "HC", "HP", "I", "P", "Ve", "Vr"]);

  // „Altele" e singura din amândouă: e rubrica pentru ce nu intră nicăieri.
  const inAmandoua = destinationsFor("RECOVERED").filter((d) => destinationsFor("DISPOSED").includes(d));
  assert.deepEqual(inAmandoua, ["A"]);
});

/**
 * Ordinea de pe „Generare” (Andreea, 29.09.2026): întâi valorificare/eliminare, apoi codul, apoi
 * unde ajunge deșeul. Până la alegere destinația nu se oferă — dar un rând care o are deja o arată,
 * ca redeschiderea să nu ascundă ce s-a salvat (și rândul vechi `UNCLASSIFIED_OUT` la fel).
 */
test("pe Generare destinația se oferă după valorificare/eliminare, în rest ca înainte", () => {
  // Generare, rând nou: nimic ales, nimic oferit; după alegere, oferit.
  assert.equal(destinationsOpen("ANEXA_1", true, "", ""), false);
  assert.equal(destinationsOpen("ANEXA_1", true, "RECOVERED", ""), true);
  assert.equal(destinationsOpen("ANEXA_1", true, "DISPOSED", ""), true);
  // Rândul deschis cu o destinație salvată (și cel vechi, fără soartă) o arată.
  assert.equal(destinationsOpen("ANEXA_1", true, "", "DO"), true);
  // Fără alegere de soartă pe ecran nu e după ce aștepta.
  assert.equal(destinationsOpen("ANEXA_1", false, "", ""), true);
  // Celelalte ecrane rămân neschimbate: destinația se oferă mereu.
  assert.equal(destinationsOpen("ART_48", true, "", ""), true);
  assert.equal(destinationsOpen("ART_48", false, "", ""), true);
});
