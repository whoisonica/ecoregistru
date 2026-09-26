// Proba 48: registrul formularelor primite (D2.6) — tabul „Formulare primite” din „Cântar”.
//
// Ce apără: „Trece formular” (butonul principal, tasta N) trece o Anexa 3 primită cu numărul 1 în registrul
// depozitului; „Corectează” deschide rândul completat și îl trece ca rând nou, cu motivul — rândul greșit rămâne, cu
// „Corectat de nr. 2”, iar corectura cu „Corectează nr. 1”; un rând corectat nu mai are butonul; registrul se
// descarcă PDF; 1440 și 375 fără lățire.
//
// ⚠️ Registrul nu se șterge (append-only): lasă în urmă rândurile „P48-<număr>” pe primul depozit.
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
const NUMBER = `P48-${Date.now() % 100000}`;
const dialog = 'div[role="dialog"]';
const rowOf = (page, text) =>
  page.evaluate((text) => {
    const tr = [...document.querySelectorAll("tbody tr")].find((r) => r.textContent.includes(text));
    return tr?.textContent.replace(/\s+/g, " ") ?? "";
  }, text);

const page = await newPage(browser, { width: 1440, height: 900 });
await login(page, "admin");
await page.goto(BASE + "/cantar", { waitUntil: "networkidle" });
await page.waitForTimeout(700);
await page.click('[role="tab"]:has-text("Formulare primite")');
await page.waitForTimeout(700);
check("butonul principal e „Trece formular”", Boolean(await page.$('button:has-text("Trece formular")')));

await page.keyboard.press("n");
await page.waitForTimeout(600);
check("tasta N deschide formularul", Boolean(await page.$(`${dialog} #rf-number`)));
await page.fill("#rf-series", "CJ");
await page.fill("#rf-number", NUMBER);
await page.fill("#rf-sender", "Magazin Probă SRL");
await page.fill("#rf-cui", "RO48");
await page.fill("#rf-waste", "15 01 01 carton");
await page.fill("#rf-qty", "1200");
await shot(page, "48-formular");
await page.click(`${dialog} button[type="submit"]`);
await page.waitForTimeout(1200);
let first = await rowOf(page, NUMBER);
check("rândul apare în registru", first.includes("Magazin Probă SRL"), first);

// Corectura: un rând nou, rândul greșit rămâne.
await page.click(`tbody tr:has-text("${NUMBER}") button:has-text("Corectează")`);
await page.waitForTimeout(600);
const prefilled = await page.$eval("#rf-number", (i) => i.value);
check("corectura pornește din rândul greșit", prefilled === NUMBER, prefilled);
await page.fill("#rf-qty", "1210");
await page.fill("#rf-reason", "Proba 48: cantitatea citită greșit");
await page.click(`${dialog} button[type="submit"]`);
await page.waitForTimeout(1200);
const rows = await page.$$eval("tbody tr", (trs) => trs.map((tr) => tr.textContent.replace(/\s+/g, " ")));
const mine = rows.filter((r) => r.includes(NUMBER));
check("registrul are acum două rânduri pentru formular", mine.length === 2, mine.join(" || "));
check("rândul greșit spune cine îl corectează", mine.some((r) => /Corectat de nr\. \d+/.test(r) && r.includes("1.200")), mine.join(" || "));
check("corectura spune ce corectează și de ce", mine.some((r) => /Corectează nr\. \d+: Proba 48/.test(r) && r.includes("1.210")), mine.join(" || "));
const correctedHasButton = await page.evaluate((n) => {
  const tr = [...document.querySelectorAll("tbody tr")].find((r) => r.textContent.includes(n) && r.textContent.includes("Corectat de"));
  return [...(tr?.querySelectorAll("button") ?? [])].some((b) => b.textContent.includes("Corectează"));
}, NUMBER);
check("rândul corectat nu mai are „Corectează”", !correctedHasButton);

const [pdf] = await Promise.all([
  page.waitForEvent("download", { timeout: 8000 }).catch(() => null),
  page.click('button:has-text("Registrul (PDF)")'),
]);
check("registrul se descarcă", pdf !== null && pdf.suggestedFilename().endsWith(".pdf"), pdf?.suggestedFilename());
const width = await page.evaluate(() => document.body.scrollWidth - window.innerWidth);
check("nu se lățește la 1440px", width <= 0, `${width}px`);
await shot(page, "48-registru");

const phone = await newPage(browser, { width: 375, height: 800 });
await login(phone, "admin");
await phone.goto(BASE + "/cantar", { waitUntil: "networkidle" });
await phone.waitForTimeout(600);
await phone.click('[role="tab"]:has-text("Formulare primite")');
await phone.waitForTimeout(700);
const overflow = await phone.evaluate(() => document.body.scrollWidth - window.innerWidth);
check("tabul nu lățește pagina la 375px", overflow <= 0, `${overflow}px`);
await shot(phone, "48-registru-telefon");

for (const p of [page, phone]) {
  if (p.problems.length > 0) {
    console.log("  FAIL consola/rețeaua");
    for (const problem of [...new Set(p.problems)]) console.log(`         ${problem}`);
    fails += p.problems.length;
  }
}
await browser.close();
console.log(fails === 0 ? "\n✓ Proba 48 trece." : `\n✗ Proba 48: ${fails} probleme.`);
process.exit(fails === 0 ? 0 : 1);
