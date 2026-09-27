// Proba 51: soldul preluat și inventarul depozitului (F3, D3.5) — tabul „Inventar” de pe „Cântar”.
//
// Ce apără: pe un depozit nou, adminul preia din dialog 500 kg cu data implicită, azi (sursa „Fișele de magazie”), și
// confirmă nota → „Confirmată”, stocul e 500; deschide tot azi un inventar (decizie, comisie cu președinte) — soldul
// preluat e în scriptic, nu plus (recenzia D3.5, C1) —, salvează
// declarația, trece faptic 480 kg cântărit cu explicația „uscare” și lipsa neimputabilă, încheie PV-ul și îl aprobă →
// „Aprobat”, stocul e 480, lista 14-3-12 vine ca PDF; operatorul vede tabul fără „Inventar nou” și primește 403 la
// POST; 1440 fără lățire, dialogul și la 375.
//
// ⚠️ Lasă în urmă depozitul „Proba 51 <număr>” (dezactivat) cu nota, inventarul și liniile lor de stoc; pe o bază fără
// sortimente își face „Carton Proba 51”.
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
const DEPOT = `Proba 51 ${Date.now() % 100000}`;
const iso = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
const today = iso(new Date());
const api = (page, method, url, body) =>
  page.evaluate(
    async ({ method, url, body }) => {
      const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json", Authorization: "Bearer " + localStorage.getItem("eco_token") },
        body: body ? JSON.stringify(body) : undefined,
      });
      const type = res.headers.get("content-type") ?? "";
      const text = type.includes("json") ? await res.text() : "";
      return { status: res.status, type, json: text ? JSON.parse(text) : null };
    },
    { method, url, body }
  );
const top = (page) => page.locator('div[role="dialog"]').last();
const stockKg = async (page, depotId) =>
  ((await api(page, "GET", `/api/v1/stock?workPointId=${depotId}&date=${today}`)).json?.rows ?? []).reduce(
    (s, r) => s + r.stockKg,
    0
  );
const openTab = async (page) => {
  await page.goto(BASE + "/cantar", { waitUntil: "networkidle" });
  await page.waitForTimeout(600);
  await page.click('[role="tab"]:has-text("Inventar")');
  await page.waitForTimeout(500);
  await page.selectOption("#inv-depot", { label: DEPOT });
  await page.waitForTimeout(900);
};

const page = await newPage(browser, { width: 1440, height: 900 });
await login(page, "admin");
const depot = (await api(page, "POST", "/api/v1/work-points", { name: DEPOT, address: "Str. Probei 51" })).json;
let article = ((await api(page, "GET", "/api/v1/waste-articles")).json ?? []).find((a) => a.active);
if (!article) {
  const codes = (await api(page, "GET", "/api/v1/waste-codes?q=15%2001%2001")).json ?? [];
  const cardboard = (Array.isArray(codes) ? codes : codes.content ?? []).find((c) => c.code.startsWith("15 01 01"));
  article = (await api(page, "POST", "/api/v1/waste-articles",
    { name: "Carton Proba 51", wasteCodeId: cardboard.id, metal: false, forbiddenFromIndividuals: false })).json;
}

// 1. Nota de preluare, din dialog.
await openTab(page);
check("fără notă: „Nepreluat”", /Nepreluat/.test(await page.textContent('section[aria-label="Sold preluat"]')));
await page.click('button:has-text("Preia soldurile")');
await page.waitForTimeout(600);
await page.fill("#so-keeper", "Ion Gestionar");
await page.fill("#so-accountant", "Ana Contabil");
await top(page).locator("select").first().selectOption(article.id);
await top(page).locator('input[placeholder="Kg"]').first().fill("500");
await shot(page, "51-nota-preluare");
await top(page).locator('button:has-text("Salvează")').click();
await page.waitForTimeout(900);
await page.click('section[aria-label="Sold preluat"] button:has-text("Confirmă")');
await page.waitForTimeout(400);
await top(page).locator('button:has-text("Confirmă")').click();
await page.waitForTimeout(1000);
check("nota e „Confirmată”", /Confirmată/.test(await page.textContent('section[aria-label="Sold preluat"]')));
check("stocul e 500 kg", (await stockKg(page, depot.id)) === 500, String(await stockKg(page, depot.id)));

// 2. Inventarul, pas cu pas.
await page.click('button:has-text("Inventar nou")');
await page.waitForTimeout(600);
await page.fill("#inv-dn", "51");
await page.fill("#inv-keeper", "Ion Gestionar");
await top(page).locator('input[placeholder="Nume"]').first().fill("Ana Președinte");
await top(page).locator('button:has-text("Deschide inventarul")').click();
await page.waitForTimeout(1200);
await top(page).locator('button:has-text("Salvează")').click(); // declarația, toate „Nu”
await page.waitForTimeout(900);
await top(page).locator('[role="tab"]:has-text("Numărare")').click();
await page.waitForTimeout(500);
await top(page).locator('input[inputmode="decimal"]').first().fill("480");
await top(page).locator('button:has-text("Detalii")').first().click();
await top(page).locator('label:has-text("Cântărit")').click();
await page.fill("#inv-expl", "uscare");
await top(page).locator('label:has-text("Neimputabilă")').click();
await shot(page, "51-numarare");
await top(page).locator('button:has-text("Salvează")').last().click();
await page.waitForTimeout(1000);
await top(page).locator('button:has-text("Încheie PV")').click();
await page.waitForTimeout(1000);
check("statusul e „PV încheiat”", /PV încheiat/.test(await top(page).textContent()));
await top(page).locator('button:has-text("Aprobă")').click();
await page.waitForTimeout(400);
await top(page).locator('button:has-text("Aprobă")').click();
await page.waitForTimeout(1200);
check("statusul e „Aprobat”", /Aprobat/.test(await page.locator('div[role="dialog"]').first().textContent()));
check("stocul e 480 kg după aprobare", (await stockKg(page, depot.id)) === 480, String(await stockKg(page, depot.id)));
const inv = ((await api(page, "GET", `/api/v1/inventories?workPointId=${depot.id}`)).json ?? [])[0];
const pdf = await api(page, "GET", `/api/v1/inventories/${inv.id}/pdf/lista`);
check("lista 14-3-12 vine ca PDF", pdf.status === 200 && pdf.type.includes("pdf"), `${pdf.status} ${pdf.type}`);
const width = await page.evaluate(() => document.body.scrollWidth - window.innerWidth);
check("nu se lățește la 1440px", width <= 0, `${width}px`);
await shot(page, "51-inventar-aprobat");

const phone = await newPage(browser, { width: 375, height: 812 });
await login(phone, "admin");
await openTab(phone);
await phone.click("tbody tr");
await phone.waitForTimeout(900);
check("inventarul redeschis își arată decizia (nr. 51)", (await phone.inputValue("#inv-dn")) === "51",
  await phone.inputValue("#inv-dn"));
await shot(phone, "51-inventar-375");

// 3. Operatorul vede, nu scrie.
const operator = await newPage(browser, { width: 1440, height: 900 });
await login(operator, "operator");
await openTab(operator);
check("operatorul nu are „Inventar nou”", !(await operator.$('button:has-text("Inventar nou")')));
const quiet = operator.problems.length; // 403-ul de mai jos e cerut dinadins
const refused = await api(operator, "POST", "/api/v1/inventories", {
  workPointId: depot.id, kind: "ANNUAL", startsOn: today, endsOn: today, keeperName: "X",
  commission: [{ name: "P", role: null, president: true }],
});
check("operatorul primește 403 la deschidere", refused.status === 403, String(refused.status));
await operator.waitForTimeout(300);
operator.problems.splice(quiet);

await api(page, "DELETE", `/api/v1/work-points/${depot.id}`);
for (const p of [page, phone, operator]) {
  const problems = [...new Set(p.problems)];
  if (problems.length > 0) {
    console.log("  FAIL consola/rețeaua");
    for (const problem of problems) console.log(`         ${problem}`);
    fails += problems.length;
  }
}
await browser.close();
console.log(fails === 0 ? "\n✓ Proba 51 trece." : `\n✗ Proba 51: ${fails} probleme.`);
process.exit(fails === 0 ? 0 : 1);
