// Testele parserului avizului: `npm run test:aviz` (Node rulează TypeScript direct, fără Jest).
//
// **Toate avizele de aici sunt inventate**, scrise de mână: firme, CUI-uri, numere, mașini. Nicio
// bucată dintr-un aviz de client (todo-mobil §10). Rândurile imită ce dă recunoașterea de text:
// coloanele tabelului pe rânduri separate, diacritice pierdute, spații în plus.
import assert from "node:assert/strict";
import { test } from "node:test";

import { cuiDigits, isValidCui, parseAviz, parseRoNumber, type AvizContext } from "./parse.ts";

const ctx: AvizContext = {
  ownCui: "RO30219879", // „Tâmplăria Nucet SRL”, firma care predă
  wasteCodes: ["15 01 01", "15 01 02", "20 01 40", "13 02 08", "17 04 05"],
  partners: [
    { id: "p-remat", cui: "RO44556610" }, // „Remat Nordic SRL”
    { id: "p-hartie", cui: "17900120" }, // „Hârtia Verde SA”, scris fără RO
    { id: "p-fara-cui", cui: null },
  ],
};

test("1. avizul tipic: CUI cu RO, cod cu spații, cantitate în kg, serie și număr", () => {
  const r = parseAviz(
    [
      "AVIZ DE ÎNSOȚIRE A MĂRFII",
      "Seria TNC Nr. 000412",
      "Data: 12.09.2026",
      "Furnizor: Tâmplăria Nucet SRL CIF: RO30219879",
      "Cumpărător: Remat Nordic SRL CIF: RO44556610",
      "Deșeuri de ambalaje din hârtie și carton 15 01 01",
      "1.250 kg",
      "Mijloc de transport: CJ 12 RMN",
    ],
    ctx,
  );
  assert.equal(r.partnerId?.value, "p-remat");
  assert.equal(r.unknownCui, undefined);
  assert.equal(r.wasteCode?.value, "15 01 01");
  assert.deepEqual(r.quantity?.value, { amount: 1250, unit: "KG" });
  assert.equal(r.documentNumber?.value, "TNC 000412");
  assert.equal(r.date?.value, "2026-09-12");
  assert.equal(r.vehicle?.value, "CJ 12 RMN");
  // Fiecare câmp poartă rândul lui, ca omul să vadă de unde vine.
  assert.equal(r.quantity?.source, "1.250 kg");
});

test("2. CUI-ul firmei însăși nu e niciodată partener, chiar dacă e singurul valid", () => {
  const r = parseAviz(["Furnizor Tamplaria Nucet SRL", "C.U.I. RO30219879", "Nr. 5 din 01.09.2026"], ctx);
  assert.equal(r.partnerId, undefined);
  assert.equal(r.unknownCui, undefined);
});

test("3. partenerul salvat fără RO se găsește și când avizul scrie RO", () => {
  const r = parseAviz(["Beneficiar: HARTIA VERDE SA", "Cod fiscal RO 17900120"], ctx);
  assert.equal(r.partnerId?.value, "p-hartie");
});

test("4. un CUI valid necunoscut se propune pentru ANAF; unul cu cifra de control greșită, nu", () => {
  const valid = parseAviz(["Cumparator: Eco Deal SRL", "CUI: 28104567"], ctx);
  assert.equal(valid.unknownCui?.value, "28104567");
  const wrong = parseAviz(["Cumparator: Eco Deal SRL", "CUI: 28104568"], ctx);
  assert.equal(wrong.unknownCui, undefined);
});

test("5. un număr gol devine CUI numai pe un rând care spune cod fiscal", () => {
  // 28104567 e un CUI valid, dar aici e numărul unui contract.
  const r = parseAviz(["Contract 28104567", "Remat Nordic SRL"], ctx);
  assert.equal(r.unknownCui, undefined);
  assert.equal(r.partnerId, undefined);
});

test("6. codul se citește numai dintre codurile firmei, oricum ar fi scris", () => {
  assert.equal(parseAviz(["Fier vechi 200140"], ctx).wasteCode?.value, "20 01 40");
  assert.equal(parseAviz(["Ulei uzat 13.02.08*"], ctx).wasteCode?.value, "13 02 08");
  // Un cod perfect valid din nomenclator, dar nu al firmei: tace.
  assert.equal(parseAviz(["Sticlă 15 01 07"], ctx).wasteCode, undefined);
});

test("7. o dată nu e citită drept cod de deșeu", () => {
  // „15.01.2026” ar fi dat „15 01 20” fără separatorul repetat, iar „01.02.20|26” alt cod.
  const r = parseAviz(["Data 15.01.2026", "Nr aviz 77"], { ...ctx, wasteCodes: ["15 01 20", "01 20 26"] });
  assert.equal(r.wasteCode, undefined);
  assert.equal(r.date?.value, "2026-01-15");
});

test("8. tonele, virgula zecimală și cantitatea lipită de unitate", () => {
  assert.deepEqual(parseAviz(["Cantitate: 2,35 tone"], ctx).quantity?.value, { amount: 2.35, unit: "TONS" });
  assert.deepEqual(parseAviz(["Cupru 17 04 05 870kg"], ctx).quantity?.value, { amount: 870, unit: "KG" });
  assert.deepEqual(parseAviz(["1.250,5 KG"], ctx).quantity?.value, { amount: 1250.5, unit: "KG" });
});

test("9. prețul pe kilogram nu e cantitatea", () => {
  const r = parseAviz(["Valoare 1.250 lei", "Pret unitar 0,80 lei/kg", "Cantitate 900 kg"], ctx);
  assert.deepEqual(r.quantity?.value, { amount: 900, unit: "KG" });
  assert.equal(r.quantity?.source, "Cantitate 900 kg");
});

test("10. avizul tipărit de WasteHouse: „Serie / număr aviz” cu valoarea pe rândul următor", () => {
  const r = parseAviz(
    [
      "AVIZ DE ÎNSOȚIRE A MĂRFII",
      "Serie / număr aviz",
      "TNC-0042",
      "Data emitere",
      "03.09.2026",
      "Denumire",
      "Remat Nordic SRL",
      "RO44556610",
      "Număr auto / remorcă",
      "B 123 XYZ",
    ],
    ctx,
  );
  assert.equal(r.documentNumber?.value, "TNC-0042");
  assert.equal(r.date?.value, "2026-09-03");
  assert.equal(r.partnerId?.value, "p-remat");
  assert.equal(r.vehicle?.value, "B 123 XYZ");
});

test("11. data expirării autorizației nu e data avizului, chiar când vine prima", () => {
  const unlabelled = parseAviz(["Autorizatie de mediu valabila pana la 31.12.2027", "08.09.2026"], ctx);
  assert.equal(unlabelled.date?.value, "2026-09-08");
  const labelled = parseAviz(["Data expirarii autorizatiei: 31.12.2027", "Data: 08.09.2026"], ctx);
  assert.equal(labelled.date?.value, "2026-09-08");
});

test("12. o dată imposibilă e ignorată; numărul de mașină cu liniuțe se normalizează", () => {
  const r = parseAviz(["Data 31.02.2026", "Auto: IS-07-ABC"], ctx);
  assert.equal(r.date, undefined);
  assert.equal(r.vehicle?.value, "IS 07 ABC");
});

test("13. un aviz din care nu se citește nimic întoarce un obiect gol, nu valori ghicite", () => {
  assert.deepEqual(parseAviz(["", "   ", "Semnatura si stampila"], ctx), {});
});

test("14. tabelul citit pe telefon: „kg” și cantitatea pe rânduri separate, după capul „Cantitate”", () => {
  // Forma în care Vision (iOS 26) a dat avizul de probă din `scratchpad`: fiecare celulă pe rândul ei,
  // iar numărul curent „1” vine înaintea capului de coloană „Cantitate”.
  const r = parseAviz(
    ["Nr. crt.", "1", "Denumirea produselor", "Deşeuri ambalaje plastic 15 01 02", "U.M.", "kg", "Cantitate", "1.250"],
    ctx,
  );
  assert.deepEqual(r.quantity?.value, { amount: 1250, unit: "KG" });
  assert.equal(r.quantity?.source, "Cantitate 1.250 kg");
  // Fără capul „Cantitate”, un număr singur pe rând nu e cantitate.
  assert.equal(parseAviz(["U.M.", "kg", "1", "1.250"], ctx).quantity, undefined);
  // Fără unitate, nici atât.
  assert.equal(parseAviz(["Cantitate", "1.250"], ctx).quantity, undefined);
});

test("15. „RO” citit „R0” (O ca zero) tot duce la partener", () => {
  assert.equal(parseAviz(["CIF: R044556610"], ctx).partnerId?.value, "p-remat");
  assert.equal(parseAviz(["Cumparator R0 44556610"], ctx).partnerId?.value, "p-remat");
});

test("16. blocurile de pe Android se despart în rânduri", () => {
  // ML Kit întoarce blocuri de text; un bloc ține împreună celulele unei coloane.
  const r = parseAviz(["Seria DRS Nr. 000417 Data: 14.09.2026\nCIF: RO44556610", "U.M.\nkg", "Cantitate\n1.250"], ctx);
  assert.equal(r.documentNumber?.value, "DRS 000417");
  assert.equal(r.partnerId?.value, "p-remat");
  assert.deepEqual(r.quantity?.value, { amount: 1250, unit: "KG" });
});

test("cifra de control a CUI-ului", () => {
  assert.equal(isValidCui("RO44556610"), true);
  assert.equal(isValidCui("44556611"), false);
  assert.equal(isValidCui("1"), false);
  assert.equal(cuiDigits(" ro 17900120"), "17900120");
});

test("numerele scrise românește", () => {
  assert.equal(parseRoNumber("1.250"), 1250);
  assert.equal(parseRoNumber("1,25"), 1.25);
  assert.equal(parseRoNumber("1.25"), 1.25);
  assert.equal(parseRoNumber("12.500.000"), 12500000);
  assert.equal(parseRoNumber("0"), null);
  assert.equal(parseRoNumber("abc"), null);
});
