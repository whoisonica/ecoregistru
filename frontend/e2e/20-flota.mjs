// Proba 20: flota (D2.1) — secțiunea „Flota” din Setări și vehiculul recunoscut la cântar.
//
// Scrie un vehicul și îl șterge la final (dezactivare, apoi ștergere definitivă, tot din ecran). Ce apără:
// numărul se salvează normalizat; licența apare doar peste 3,5 t; un act expirat și unul care expiră în
// 30 de zile au stări diferite; la cântar, numărul tastat altfel e recunoscut din flotă, cu tara standard
// și avertismentul pentru actele expirate; vizualizatorul vede flota fără butoane; nimic nu se lățește.
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
const iso = (days) => {
  const d = new Date();
  d.setDate(d.getDate() + days);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
};
const PLATE = "CJ20FLT";
const section = "section#flota";

const page = await newPage(browser, { width: 1440, height: 900 });
await login(page, "admin");
await page.goto(BASE + "/setari#flota", { waitUntil: "networkidle" });
await page.waitForTimeout(900);
check("Setările au secțiunea „Flota”", (await page.$(section)) !== null);

// ---------------------------------------------------------------- ADAUGĂ
await page.click(`${section} button:has-text("Adaugă vehicul")`);
await page.waitForTimeout(500);
check("licența nu se cere sub 3,5 t", (await page.$("#v-license")) === null);
await page.fill("#v-registration", "cj 20-flt");
await page.fill("#v-kind", "Camion probă");
await page.fill("#v-tare", "9000");
await page.fill("#v-itp", iso(-3));
await page.click('label[for="v-heavy"]');
await page.waitForTimeout(300);
check("peste 3,5 t apare licența", (await page.$("#v-license")) !== null);
await page.fill("#v-license", "LIC 20");
await page.fill("#v-license-expiry", iso(10));
await shot(page, "20-flota-formular");
await page.click('div[role="dialog"] button[type="submit"]');
await page.waitForTimeout(1200);

const row = await page.evaluate(({ section, plate }) => {
  const tr = [...document.querySelectorAll(`${section} tbody tr`)].find((r) => r.textContent.includes(plate));
  const table = document.querySelector(`${section} table`);
  return {
    found: Boolean(tr),
    text: tr?.textContent.replace(/\s+/g, " ") ?? "",
    overflow: table ? table.scrollWidth - table.clientWidth : 0,
  };
}, { section, plate: PLATE });
check("numărul se salvează cu majuscule și fără spații", row.found, row.text.slice(0, 120));
check("ITP-ul trecut apare ca expirat", /Expirat · ITP/.test(row.text), row.text);
check("licența care expiră în 10 zile apare ca „Expiră”", /Expiră · Licență/.test(row.text), row.text);
check("tabelul flotei încape în 1440px", row.overflow <= 0, `${row.overflow}px peste`);
await shot(page, "20-flota");

// ---------------------------------------------------------------- LA CÂNTAR
await page.goto(BASE + "/cantar", { waitUntil: "networkidle" });
await page.waitForTimeout(800);
await page.click('button:has-text("Intrare nouă")');
await page.waitForTimeout(900);
await page.fill("#wo-vehicle", "cj 20 flt");
await page.waitForTimeout(400);
const transport = await page.evaluate(() => {
  const input = document.getElementById("wo-vehicle");
  return input.closest("div").textContent.replace(/\s+/g, " ");
});
check("numărul tastat altfel e recunoscut din flotă, cu tara standard", /Din flotă · tara standard 9\.000 kg/.test(transport), transport);
check("actele expirate se văd la cântar", /ITP-ul sau licența au expirat/.test(transport), transport);
const datalist = await page.$$eval("#wo-fleet option", (o) => o.map((x) => x.value));
check("lista de sugestii are vehiculul", datalist.includes(PLATE), datalist.join(", "));
await page.fill("#wo-vehicle", "B 99 OCZ");
await page.waitForTimeout(300);
const occasional = await page.evaluate(() => document.getElementById("wo-vehicle").closest("div").textContent);
check("o mașină ocazională rămâne text, fără „Din flotă”", !/Din flotă/.test(occasional), occasional);
await shot(page, "20-flota-cantar");
await page.keyboard.press("Escape");
await page.waitForTimeout(500);

// ---------------------------------------------------------------- VIZUALIZATORUL ȘI TELEFONUL
const viewer = await newPage(browser, { width: 375, height: 800 });
await login(viewer, "viewer");
await viewer.goto(BASE + "/setari#flota", { waitUntil: "networkidle" });
await viewer.waitForTimeout(900);
const seen = await viewer.evaluate(({ section, plate }) => ({
  row: document.querySelector(section)?.textContent.includes(plate) ?? false,
  add: [...document.querySelectorAll(`${section} button`)].some((b) => /Adaugă vehicul|Editează/.test(b.textContent)),
  overflow: document.body.scrollWidth - window.innerWidth,
}), { section, plate: PLATE });
check("vizualizatorul vede flota", seen.row);
check("vizualizatorul n-are butoane de scriere", !seen.add);
check("Setările nu se lățesc la 375px", seen.overflow <= 0, `${seen.overflow}px`);
await shot(viewer, "20-flota-telefon");

// ---------------------------------------------------------------- CURĂȚENIE
await page.goto(BASE + "/setari#flota", { waitUntil: "networkidle" });
await page.waitForTimeout(800);
const rowSel = `${section} tbody tr:has-text("${PLATE}")`;
await page.click(`${rowSel} button:has-text("Dezactivează")`);
await page.click('div[role="dialog"] button:has-text("Dezactivează")');
await page.waitForTimeout(900);
// Inactivele se văd abia după filtrul „toate” (vezi proba 17).
const filter = await page.$(`${section} select:has(option[value="inactive"])`);
if (filter) await filter.selectOption("all");
await page.waitForTimeout(400);
await page.click(`${rowSel} button:has-text("Șterge definitiv")`);
await page.click('div[role="dialog"] button:has-text("Șterge definitiv")');
await page.waitForTimeout(900);
check("vehiculul de probă a fost șters", !(await page.$(rowSel)));

for (const p of [page, viewer]) {
  if (p.problems.length > 0) {
    console.log("  FAIL consola/rețeaua");
    for (const problem of [...new Set(p.problems)]) console.log(`         ${problem}`);
    fails += p.problems.length;
  }
}

await browser.close();
console.log(fails === 0 ? "\n✓ Proba 20 trece." : `\n✗ Proba 20: ${fails} probleme.`);
process.exit(fails === 0 ? 0 : 1);
