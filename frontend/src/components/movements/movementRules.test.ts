import assert from "node:assert/strict";
import { test } from "node:test";
import {
  D_CODES,
  keepIfActive,
  destinationsFor,
  destinationsOpen,
  R_CODES,
  isAfterToday,
  isExit,
  leavesAnexa3Gap,
  operationsFor,
  suggestedDestinations,
  suggestedPackagingMaterial,
  transportForm,
  quantityProblem,
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

/** 29.09.2026: avertismentul „Data e în viitor.” — nu blochează (probele scriu dinadins în 2033). */
test("data de după azi se semnalează; azi și trecutul, nu", () => {
  assert.equal(isAfterToday("2026-09-30", "2026-09-29"), true);
  assert.equal(isAfterToday("2062-09-29", "2026-09-29"), true);
  assert.equal(isAfterToday("2026-09-29", "2026-09-29"), false);
  assert.equal(isAfterToday("2025-12-31", "2026-09-29"), false);
  assert.equal(isAfterToday("", "2026-09-29"), false);
});

/** 29.09.2026: o predare cu formular Anexa 3 emis, scoasă din registru, lasă un număr gol. */
test("golul din Registrul Anexa 3: fără partener, fără ieșire sau în alt an", () => {
  const issued = { anexa3Number: 17, date: "2026-05-10" };
  const same = { date: "2026-06-01", partnerId: "p1", operation: "RECOVERED" as const };
  assert.equal(leavesAnexa3Gap(issued, same), false);
  assert.equal(leavesAnexa3Gap(issued, { ...same, operation: "DISPOSED" }), false);
  assert.equal(leavesAnexa3Gap(issued, { ...same, partnerId: null }), true);
  assert.equal(leavesAnexa3Gap(issued, { ...same, operation: "GENERATED" }), true);
  assert.equal(leavesAnexa3Gap(issued, { ...same, date: "2027-01-02" }), true);
  // Fără formular emis nu e niciun număr de pierdut; o mișcare nouă nici atât.
  assert.equal(leavesAnexa3Gap({ anexa3Number: null, date: "2026-05-10" }, { ...same, partnerId: null }), false);
  assert.equal(leavesAnexa3Gap(null, { ...same, partnerId: null }), false);
});

test("o copie nu păstrează un partener dezactivat între timp", () => {
  const partners = [
    { id: "a", active: true },
    { id: "b", active: false },
  ];
  assert.equal(keepIfActive("a", partners), "a");
  assert.equal(keepIfActive("b", partners), "");
  assert.equal(keepIfActive("sters", partners), "");
  assert.equal(keepIfActive("", partners), "");
});

test("formularul de transport al predării: Anexa 3, Anexa 2 sau niciunul", () => {
  const generator = { collectorForms: false };
  const colector = { collectorForms: true };
  assert.equal(transportForm({ hazardous: false, medical: false, ...generator }), "ANEXA_3");
  assert.equal(transportForm({ hazardous: false, medical: false, ...colector }), "ANEXA_3");
  // Periculos la generator: formularul îl aduce colectorul, generatorul tipărește doar avizul.
  assert.equal(transportForm({ hazardous: true, medical: false, ...generator }), null);
  assert.equal(transportForm({ hazardous: true, medical: false, ...colector }), "ANEXA_2");
  // Cap. 18: formularul e al transportatorului (art. 24), la oricine.
  assert.equal(transportForm({ hazardous: true, medical: true, ...colector }), null);
  assert.equal(transportForm({ hazardous: true, medical: true, ...generator }), null);
});

test("cantitatea: kilograme întregi, tone cu cel mult trei zecimale", () => {
  assert.equal(quantityProblem("", "KG"), "required");
  assert.equal(quantityProblem("0", "KG"), "required");
  assert.equal(quantityProblem("-3", "KG"), "required");
  assert.equal(quantityProblem("120", "KG"), null);
  assert.equal(quantityProblem("120.000", "KG"), null);
  assert.equal(quantityProblem("120.5", "KG"), "wholeKg");
  assert.equal(quantityProblem("0.001", "KG"), "wholeKg");
  assert.equal(quantityProblem("0.250", "TONS"), null);
  assert.equal(quantityProblem("1.5", "TONS"), null);
  // Sub un kilogram în tone nu se poate scrie mai fin decât în kilograme.
  assert.equal(quantityProblem("0.0005", "TONS"), "tooPrecise");
});
