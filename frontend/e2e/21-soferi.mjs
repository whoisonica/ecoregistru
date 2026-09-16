// Proba 21: șoferii extinși (D2.2) — depozitul implicit și atestatul în Setări, șoferul ales din listă la cântar.
//
// Scrie un șofer și îl șterge la final (dezactivare, apoi ștergere definitivă, tot din ecran). Ce apără:
// depozitul și atestatul se salvează și se văd în tabel; un atestat expirat are starea lui; la cântar numele
// tastat cu alte majuscule e recunoscut din listă, mașina lui obișnuită intră în rubrica goală, atestatul
// expirat se vede; un nume necunoscut rămâne șofer ocazional; nimic nu se lățește la 375px.
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
const NAME = "Șofer Probă Douăunu";
const PLATE = "CJ21SOF";
const section = "section#soferi";

const page = await newPage(browser, { width: 1440, height: 900 });
await login(page, "admin");
await page.goto(BASE + "/setari#soferi", { waitUntil: "networkidle" });
await page.waitForTimeout(900);
check("Setările au secțiunea șoferilor", (await page.$(section)) !== null);

// ---------------------------------------------------------------- ADAUGĂ
await page.click(`${section} button:has-text("Adaugă șofer")`);
await page.waitForTimeout(500);
await page.fill("#d-name", NAME);
await page.fill("#d-vehicle", PLATE);
const depot = await page.$$eval("#d-home option", (o) => o.filter((x) => x.value).map((x) => ({ v: x.value, t: x.textContent })));
check("depozitul implicit se alege din depozitele firmei", depot.length > 0, `${depot.length} opțiuni`);
if (depot.length) await page.selectOption("#d-home", depot[0].v);
await page.fill("#d-attestation", "ADR 21");
await page.fill("#d-attestation-expiry", iso(-2));
await shot(page, "21-soferi-formular");
await page.click('div[role="dialog"] button[type="submit"]');
await page.waitForTimeout(1200);

const row = await page.evaluate(({ section, name }) => {
  const tr = [...document.querySelectorAll(`${section} tbody tr`)].find((r) => r.textContent.includes(name));
  const table = document.querySelector(`${section} table`);
  return {
    text: tr?.textContent.replace(/\s+/g, " ") ?? "",
    overflow: table ? table.scrollWidth - table.clientWidth : 0,
  };
}, { section, name: NAME });
check("rândul are depozitul implicit", depot.length > 0 && row.text.includes(depot[0].t), row.text);
check("atestatul expirat apare ca „Expirat”", /Expirat · /.test(row.text), row.text);
check("tabelul șoferilor încape în 1440px", row.overflow <= 0, `${row.overflow}px peste`);
await shot(page, "21-soferi");

// ---------------------------------------------------------------- LA CÂNTAR
await page.goto(BASE + "/cantar", { waitUntil: "networkidle" });
await page.waitForTimeout(800);
await page.click('button:has-text("Intrare nouă")');
await page.waitForTimeout(900);
const suggestions = await page.$$eval("#wo-drivers option", (o) => o.map((x) => x.value));
check("lista de sugestii are șoferul", suggestions.includes(NAME), `${suggestions.length} sugestii`);
await page.fill("#wo-driver", NAME.toLocaleLowerCase("ro"));
await page.waitForTimeout(400);
const picked = await page.evaluate(() => ({
  driver: document.getElementById("wo-driver").closest("div").textContent.replace(/\s+/g, " "),
  vehicle: document.getElementById("wo-vehicle").value,
}));
check("numele tastat cu alte majuscule e recunoscut din listă", /Din lista de șoferi/.test(picked.driver), picked.driver);
check("mașina lui obișnuită intră în rubrica goală", picked.vehicle === PLATE, picked.vehicle);
check("atestatul expirat se vede la cântar", /Atestatul șoferului a expirat/.test(picked.driver), picked.driver);
await shot(page, "21-soferi-cantar");

await page.fill("#wo-vehicle", "B 99 OCZ");
await page.fill("#wo-driver", "Gheorghe Ocazional");
await page.waitForTimeout(300);
const occasional = await page.evaluate(() => ({
  driver: document.getElementById("wo-driver").closest("div").textContent,
  vehicle: document.getElementById("wo-vehicle").value,
}));
check("un nume necunoscut rămâne șofer ocazional", /Șofer ocazional/.test(occasional.driver), occasional.driver);
check("mașina scrisă nu se rescrie", occasional.vehicle === "B 99 OCZ", occasional.vehicle);
await page.keyboard.press("Escape");
await page.waitForTimeout(500);

// ---------------------------------------------------------------- TELEFONUL
const phone = await newPage(browser, { width: 375, height: 800 });
await login(phone, "admin");
await phone.goto(BASE + "/setari#soferi", { waitUntil: "networkidle" });
await phone.waitForTimeout(900);
const overflow = await phone.evaluate(() => document.body.scrollWidth - window.innerWidth);
check("Setările nu se lățesc la 375px", overflow <= 0, `${overflow}px`);
await shot(phone, "21-soferi-telefon");

// ---------------------------------------------------------------- CURĂȚENIE
await page.goto(BASE + "/setari#soferi", { waitUntil: "networkidle" });
await page.waitForTimeout(800);
const rowSel = `${section} tbody tr:has-text("${NAME}")`;
await page.click(`${rowSel} button:has-text("Dezactivează")`);
await page.click('div[role="dialog"] button:has-text("Dezactivează")');
await page.waitForTimeout(900);
const filter = await page.$(`${section} select:has(option[value="inactive"])`);
if (filter) await filter.selectOption("all");
await page.waitForTimeout(400);
await page.click(`${rowSel} button:has-text("Șterge definitiv")`);
await page.click('div[role="dialog"] button:has-text("Șterge definitiv")');
await page.waitForTimeout(900);
check("șoferul de probă a fost șters", !(await page.$(rowSel)));

for (const p of [page, phone]) {
  if (p.problems.length > 0) {
    console.log("  FAIL consola/rețeaua");
    for (const problem of [...new Set(p.problems)]) console.log(`         ${problem}`);
    fails += p.problems.length;
  }
}

await browser.close();
console.log(fails === 0 ? "\n✓ Proba 21 trece." : `\n✗ Proba 21: ${fails} probleme.`);
process.exit(fails === 0 ? 0 : 1);
