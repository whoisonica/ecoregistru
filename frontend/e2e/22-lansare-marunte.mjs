// Proba 22: măruntele din `todo-lansare.md`, „Interfață” și „Produs”, închise pe 16.09.2026.
//
// (1) „Istoric” pe rândul de mișcare duce în jurnalul de audit filtrat pe acel rând, iar „Arată tot jurnalul”
// scoate filtrul; un operator nu vede intrarea (jurnalul e al administratorului). (2) Dosarul de control spune
// cât cântărește înainte de descărcare. (3) Parola nouă se poate arăta și are bara de putere, aliniată la regula
// serverului. (4) Rubricile de șofer au limita serverului, deci nu se mai poate scrie ce primește 422.
//
// ⚠️ Lasă în urmă o mișcare pe 2034-02, ca proba 15 pe 2033: fapta e chiar ce se probează.
import { launch, newPage, login, shot, clickAt, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
let fails = 0;

function check(name, ok, detail = "") {
  if (ok) console.log(`  OK   ${name}${detail ? " — " + detail : ""}`);
  else {
    console.log(`  FAIL ${name}${detail ? " — " + detail : ""}`);
    fails++;
  }
}

await login(page, "admin");

// --------------------------------------------------------------------- (1) ISTORIC
const created = await page.evaluate(async () => {
  const auth = { Authorization: "Bearer " + localStorage.getItem("eco_token") };
  const json = async (url, init = {}) => {
    const res = await fetch(url, { ...init, headers: { ...auth, ...(init.headers || {}) } });
    return res.ok ? res.json() : null;
  };
  const workPoints = await json("/api/v1/work-points");
  const codes = await json("/api/v1/waste-codes?q=" + encodeURIComponent("20 01 01"));
  const wasteCodeId = (codes || []).find((c) => c.code === "20 01 01")?.id;
  const workPointId = workPoints?.find((w) => w.active)?.id;
  if (!wasteCodeId || !workPointId) return null;
  const body = (quantity) => JSON.stringify({
    workPointId, date: "2034-02-10", wasteCodeId, quantity, unit: "KG",
    physicalState: "SOLID", operation: "RECOVERED", register: "ANEXA_1", operationCode: "R13", notes: "proba 22 — istoric",
  });
  const m = await json("/api/v1/movements", { method: "POST", headers: { "Content-Type": "application/json" }, body: body(3.5) });
  if (!m) return null;
  await json("/api/v1/movements/" + m.id, { method: "PUT", headers: { "Content-Type": "application/json" }, body: body(6.5) });
  return m.id;
});
check("mișcarea de probă s-a scris", Boolean(created));

await page.goto(BASE + "/generare?luna=2034-02", { waitUntil: "networkidle" });
await page.waitForTimeout(800);
const rowMenu = `tr:has-text("proba 22") button[aria-label="Mai multe acțiuni"]`;
const menuBtn = await page.$(rowMenu) ?? (await page.$$('td button[aria-label="Mai multe acțiuni"]'))[0];
check("rândul are meniul „⋯”", Boolean(menuBtn));
if (menuBtn) {
  await menuBtn.click();
  await page.waitForTimeout(300);
  const istoric = page.locator('[role="menuitem"]:has-text("Istoric"), button:has-text("Istoric")').first();
  check("meniul are „Istoric”", (await istoric.count()) > 0);
  if ((await istoric.count()) > 0) {
    await istoric.click();
    await page.waitForURL((u) => u.pathname === "/setari", { timeout: 10000 }).catch(() => {});
    await page.waitForTimeout(1200);
    const url = new URL(page.url());
    check("duce în Setări, cu rândul în adresă", url.pathname === "/setari" && url.searchParams.get("istoric") === created,
      url.pathname + url.search);
    check("jurnalul spune că arată un singur rând", Boolean(await page.$('[data-testid="audit-one-row"]')));
    const labels = await page.$$eval("#jurnal-audit tbody tr", (trs) => trs.map((tr) => tr.textContent));
    check("și arată faptele acelui rând (creare + modificare)", labels.length === 2 && labels.every((t) => t.includes("2034-02-10")),
      `${labels.length} rânduri`);
    check("filtrul de tip e ascuns cât e un singur rând", !(await page.$('#jurnal-audit select[aria-label="Tipul înregistrării"]')));
    await shot(page, "istoric_un_rand");
    await page.click('#jurnal-audit button:has-text("Arată tot jurnalul")');
    await page.waitForTimeout(900);
    check("„Arată tot jurnalul” scoate filtrul", !(await page.$('[data-testid="audit-one-row"]')) && !new URL(page.url()).searchParams.has("istoric"));
  }
}

// ------------------------------------------------------------------ (2) DOSARUL
await page.goto(BASE + "/dosar-control?an=2026", { waitUntil: "networkidle" });
await page.waitForTimeout(800);
const size = await page.$eval('[data-testid="audit-file-size"]', (p) => p.textContent).catch(() => null);
// Demo are două atașamente seedate fără mărime (dinainte de V57), pe iunie 2026.
check("dosarul spune ce cântărește înainte de descărcare", Boolean(size) && /atașamente/.test(size), size ?? "lipsă");

// ------------------------------------------------------------------- (4) ȘOFERII
await page.goto(BASE + "/setari", { waitUntil: "networkidle" });
await page.waitForTimeout(800);
await page.evaluate(() => {
  document.querySelector("#soferi")?.scrollIntoView();
  [...document.querySelectorAll("#soferi button")].find((b) => /Adaugă/.test(b.textContent))?.click();
});
await page.waitForTimeout(500);
const limits = await page.evaluate(() =>
  ["d-name", "d-identification", "d-vehicle", "d-attestation"].map((id) => document.getElementById(id)?.maxLength ?? null));
check("rubricile de șofer au limita serverului (255/100/50/100)", JSON.stringify(limits) === "[255,100,50,100]", JSON.stringify(limits));
await page.keyboard.press("Escape");

// ---------------------------------------------------------------- operatorul
const opPage = await newPage(browser, { width: 1440, height: 900 });
await login(opPage, "operator");
await opPage.goto(BASE + "/generare?luna=2034-02", { waitUntil: "networkidle" });
await opPage.waitForTimeout(800);
const opMenu = (await opPage.$$('td button[aria-label="Mai multe acțiuni"]'))[0];
if (opMenu) {
  await opMenu.click();
  await opPage.waitForTimeout(300);
  check("operatorul nu are „Istoric” (jurnalul e al administratorului)",
    (await opPage.locator('[role="menuitem"]:has-text("Istoric"), button:has-text("Istoric")').count()) === 0);
} else {
  check("operatorul vede rândul de probă", false);
}

// ------------------------------------------------------------------ (3) PAROLA
const anon = await newPage(browser, { width: 1440, height: 900 });
await anon.goto(BASE + "/reseteaza-parola?code=proba-22", { waitUntil: "networkidle" });
await anon.waitForTimeout(500);
const levelFor = async (value) => {
  await anon.fill("#rp-pass", value);
  await anon.waitForTimeout(100);
  return anon.$eval('[data-testid="password-strength"]', (d) => d.dataset.level).catch(() => null);
};
check("sub regulă: nivelul 0", (await levelFor("parola")) === "0");
check("regula serverului atinsă: nivelul 1", (await levelFor("Parola12")) === "1");
check("diacriticele contează ca litere mari, ca pe server", (await levelFor("ĂȘparola1")) === "1");
check("lungă și cu semn: nivelul 3", (await levelFor("Parola-lunga-2034")) === "3");
check("la început e ascunsă", (await anon.getAttribute("#rp-pass", "type")) === "password");
await anon.click('#rp-pass ~ button[aria-label="Arată parola"]');
check("„Arată parola” o arată", (await anon.getAttribute("#rp-pass", "type")) === "text");
await shot(anon, "parola_putere");
const loginPage = await newPage(browser, { width: 1440, height: 900 });
await loginPage.goto(BASE + "/login", { waitUntil: "networkidle" });
await loginPage.fill("#login-password", "ceva");
check("la login nu e bară de putere", !(await loginPage.$('[data-testid="password-strength"]')));
check("dar are „Arată parola”", Boolean(await loginPage.$('#login-password ~ button[aria-label="Arată parola"]')));

for (const p of [page, opPage, anon, loginPage]) {
  if (p.problems.length > 0) {
    console.log("  FAIL consola/rețeaua");
    for (const problem of [...new Set(p.problems)]) console.log(`         ${problem}`);
    fails += p.problems.length;
  }
}

await browser.close();
console.log(fails === 0 ? "\n✓ Proba 22 trece." : `\n✗ Proba 22: ${fails} probleme.`);
process.exit(fails === 0 ? 0 : 1);
