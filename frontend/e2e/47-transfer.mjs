// Proba 47: transferul între depozitele firmei (D2.5) — tabul „Transferuri” din „Cântar”.
//
// Ce apără: „Transfer nou” cere depozitul de destinație în loc de partener și n-are preț, cod R/D sau plată;
// „Pleacă” îl trece „În tranzit”; „Recepționează” cere greutatea de la destinație, arată diferența și, fără
// cântar la destinație (toleranță necunoscută), nu închide o diferență fără NIR și decizia comisiei — serverul
// refuză și mesajul lui ajunge pe ecran; cu NIR se închide „Recepționat”, cu diferența pe rând; avizul se
// deschide; nimic nu se lățește la 1440 și 375.
//
// ⚠️ Lasă în urmă depozitul „Proba 47 <număr>” (dezactivat) și transferul anulat cu motivul „Proba 47”.
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
const DEPOT = `Proba 47 ${Date.now() % 100000}`;
const dialog = 'div[role="dialog"]';
const api = (page, method, url, body) =>
  page.evaluate(
    async ({ method, url, body }) => {
      const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json", Authorization: "Bearer " + localStorage.getItem("eco_token") },
        body: body ? JSON.stringify(body) : undefined,
      });
      const text = await res.text();
      return { status: res.status, json: text ? JSON.parse(text) : null };
    },
    { method, url, body }
  );
const rowText = (page) =>
  page.evaluate((name) => {
    const tr = [...document.querySelectorAll("tbody tr")].find((r) => r.textContent.includes(name));
    return tr?.textContent.replace(/\s+/g, " ") ?? "";
  }, DEPOT);

const page = await newPage(browser, { width: 1440, height: 900 });
await login(page, "admin");
// Depozitul de destinație, cu autorizația lui (fără ea transferul e refuzat — HG 1061/2008 art. 1 alin. (3)).
const target = (await api(page, "POST", "/api/v1/work-points", { name: DEPOT, address: "Str. Probei 47, Turda" })).json;
await api(page, "PUT", `/api/v1/work-points/${target.id}`, {
  name: DEPOT, address: "Str. Probei 47, Turda", environmentalAuthNumber: "AM 47/2026", environmentalAuthExpiry: null,
});

// Un sortiment de carton (15 01 01), dacă baza n-are niciunul activ: transferul se face pe sortimente.
const articles = (await api(page, "GET", "/api/v1/waste-articles")).json ?? [];
if (!articles.some((a) => a.active)) {
  const codes = (await api(page, "GET", "/api/v1/waste-codes")).json ?? [];
  const cardboard = codes.find((c) => c.code === "15 01 01") ?? codes.find((c) => !c.hazardous);
  const made = await api(page, "POST", "/api/v1/waste-articles",
    { name: "Carton Proba 47", wasteCodeId: cardboard.id, metal: false, forbiddenFromIndividuals: false });
  check("sortimentul de probă se creează", made.status === 200 || made.status === 201, `HTTP ${made.status}`);
}
await page.goto(BASE + "/cantar", { waitUntil: "networkidle" });
await page.waitForTimeout(700);
await page.click('[role="tab"]:has-text("Transferuri")');
await page.waitForTimeout(500);
const header = await page.$$eval("thead th", (ths) => ths.map((th) => th.textContent.trim()));
check("tabul are „Traseu” și „Diferență”", header.includes("Traseu") && header.includes("Diferență"), header.join(" | "));

// ---------------------------------------------------------------- TRANSFER NOU
await page.click('button:has-text("Transfer nou")');
await page.waitForTimeout(900);
const form = await page.evaluate((d) => {
  const root = document.querySelector(d);
  return {
    target: Boolean(root.querySelector("#wo-target")),
    partner: Boolean(root.querySelector("#wo-partner")),
    price: Boolean(root.querySelector('input[id^="wo-p-"]')),
    code: Boolean(root.querySelector('select[id^="wo-c-"]')),
    payment: root.textContent.includes("Plata"),
  };
}, dialog);
check("formularul cere destinația, nu partenerul", form.target && !form.partner, JSON.stringify(form));
check("fără preț, cod R/D sau plată", !form.price && !form.code && !form.payment, JSON.stringify(form));
await page.selectOption("#wo-target", target.id);
// Sortimentele vin cu o cerere separată: se așteaptă lista, nu se ia prima opțiune goală.
await page.waitForFunction((d) => document.querySelector(`${d} select[id^="wo-art-"]`)?.options.length > 1, dialog,
  { timeout: 8000 }).catch(() => {});
const article = await page.$eval(`${dialog} select[id^="wo-art-"]`, (s) => [...s.options].find((o) => o.value)?.value ?? "");
check("catalogul are sortimente de ales", Boolean(article));
await page.selectOption(`${dialog} select[id^="wo-art-"]`, article);
await page.fill(`${dialog} input[id^="wo-n-"]`, "1000");
await shot(page, "47-transfer-formular");

await page.click(`${dialog} button:has-text("Pleacă")`);
await page.waitForTimeout(500);
// Confirmarea se deschide peste formular: butonul ei e ultimul „Pleacă” din pagină.
await page.locator('button:text-is("Pleacă")').last().click();
await page.waitForTimeout(1500);
await shot(page, "47-dupa-pleaca");
let text = await rowText(page);
if (!text) console.log("  debug:", page.problems.join(" | "), await page.evaluate(() => [...document.querySelectorAll('[role=status],[role=alert]')].map((e) => e.textContent).join(" | ")));
check("după plecare, rândul e „În tranzit”", /În tranzit/.test(text), text);

// ---------------------------------------------------------------- RECEPȚIA
await page.click(`tbody tr:has-text("${DEPOT}") button:has-text("Deschide")`);
await page.waitForTimeout(800);
const [aviz] = await Promise.all([
  page.context().waitForEvent("page", { timeout: 8000 }).catch(() => null),
  page.click(`${dialog} button:has-text("Aviz")`),
]);
check("avizul se deschide", aviz !== null);
if (aviz) await aviz.close();
await page.click(`${dialog} button:has-text("Recepționează")`);
await page.waitForTimeout(800);
const receiveDialog = `${dialog}:has-text("Recepția transferului")`;
await page.fill(`${receiveDialog} input[id^="rt-n-"]`, "900");
await page.waitForTimeout(300);
const diff = await page.$eval(receiveDialog, (d) => d.textContent.replace(/\s+/g, " "));
check("diferența se vede înainte de recepție", /-100 kg/.test(diff), diff.slice(0, 300));
check("apar rubricile NIR și decizia comisiei", Boolean(await page.$("#rt-nir")) && Boolean(await page.$("#rt-reason")));
// Negativă: fără NIR serverul refuză (toleranța e necunoscută — destinația n-are cântar).
await page.click(`${receiveDialog} button:has-text("Recepționează")`);
await page.waitForTimeout(1000);
const refused = await page.evaluate(() => document.body.textContent.includes("Scrie numărul NIR-ului"));
check("fără NIR, diferența nu se închide", refused);
page.problems = page.problems.filter((p) => !p.includes("/receive") && !p.includes("400 (Bad Request)"));
await page.fill("#rt-nir", "NIR 47");
await page.fill("#rt-reason", "Proba 47: marfă udă, constatată de comisie");
await shot(page, "47-transfer-receptie");
await page.click(`${receiveDialog} button:has-text("Recepționează")`);
await page.waitForTimeout(1500);
text = await rowText(page);
check("după recepție, rândul e „Recepționat”", /Recepționat/.test(text), text);
check("rândul arată plecat / primit și diferența", /1\.000 \/ 900/.test(text) && /-100 kg/.test(text), text);
const width = await page.evaluate(() => document.body.scrollWidth - window.innerWidth);
check("nu se lățește la 1440px", width <= 0, `${width}px`);

// ---------------------------------------------------------------- TELEFON
const phone = await newPage(browser, { width: 375, height: 800 });
await login(phone, "admin");
await phone.goto(BASE + "/cantar", { waitUntil: "networkidle" });
await phone.waitForTimeout(700);
await phone.click('[role="tab"]:has-text("Transferuri")');
await phone.waitForTimeout(600);
const overflow = await phone.evaluate(() => document.body.scrollWidth - window.innerWidth);
check("tabul nu lățește pagina la 375px", overflow <= 0, `${overflow}px`);
await shot(phone, "47-transfer-telefon");

// ---------------------------------------------------------------- CURĂȚENIE
const list = (await api(page, "GET", "/api/v1/weighing-operations?type=TRANSFER")).json ?? [];
for (const op of list.filter((o) => o.transfer?.targetWorkPointId === target.id)) {
  await api(page, "POST", `/api/v1/weighing-operations/${op.id}/cancel`, { reason: "Proba 47" });
}
await api(page, "DELETE", `/api/v1/work-points/${target.id}`);

for (const p of [page, phone]) {
  if (p.problems.length > 0) {
    console.log("  FAIL consola/rețeaua");
    for (const problem of [...new Set(p.problems)]) console.log(`         ${problem}`);
    fails += p.problems.length;
  }
}
await browser.close();
console.log(fails === 0 ? "\n✓ Proba 47 trece." : `\n✗ Proba 47: ${fails} probleme.`);
process.exit(fails === 0 ? 0 : 1);
