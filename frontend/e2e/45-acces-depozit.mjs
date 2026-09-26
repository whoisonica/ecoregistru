// Proba 45: accesul pe depozit (D2.4) — Setări → Utilizatori, coloana „Depozite” și dialogul ei.
//
// Ce apără: la o firmă cu depozit și cu mai multe depozite, rândul operatorului arată „Toate” (implicit, decizia
// proprietarului din 16.09.2026); adminul îl restrânge la un depozit din dialog, iar rândul arată numele lui; „Doar
// cele alese” fără niciun depozit nu pleacă la server; operatorul restrâns nu mai vede operațiunea din celălalt
// depozit (404 pe ea, lipsă din listă), nici depozitul în selectoare; rândul adminului n-are buton (vede mereu tot);
// 375px fără lățire. La final operatorul primește înapoi „Toate depozitele”.
//
// ⚠️ Lasă în urmă depozitul „Proba 45 <număr>” (dezactivat) și o intrare anulată cu motivul „Proba 45”.
import { launch, newPage, login, shot, BASE, ACCOUNTS } from "./lib.mjs";

const browser = await launch();
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
const today = (() => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
})();
const DEPOT = `Proba 45 ${Date.now() % 100000}`;
const section = "section#utilizatori";
const operatorEmail = ACCOUNTS.operator.email;

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
const rowText = (page, email) =>
  page.evaluate(({ section, email }) => {
    const tr = [...document.querySelectorAll(`${section} tbody tr`)].find((r) => r.textContent.includes(email));
    return tr?.textContent.replace(/\s+/g, " ") ?? "";
  }, { section, email });

// ---------------------------------------------------------------- PREGĂTIRE (admin, prin API)
const page = await newPage(browser, { width: 1440, height: 900 });
await login(page, "admin");
const home = (await api(page, "GET", "/api/v1/work-points")).json.find((wp) => wp.active);
const other = (await api(page, "POST", "/api/v1/work-points", { name: DEPOT, address: null })).json;
const users = (await api(page, "GET", "/api/v1/users")).json;
const operatorRow = users.find((u) => u.email === operatorEmail);
check("operatorul pornește cu toate depozitele", operatorRow?.allWorkPoints === true, JSON.stringify(operatorRow?.workPointIds));
const op = (await api(page, "POST", "/api/v1/weighing-operations", { type: "IN", workPointId: other.id, date: today })).json;
check("intrarea din depozitul nou există", Boolean(op?.id));

// ---------------------------------------------------------------- COLOANA ȘI DIALOGUL
await page.goto(BASE + "/setari/utilizatori", { waitUntil: "networkidle" });
await page.waitForTimeout(800);
const header = await page.$$eval(`${section} thead th`, (ths) => ths.map((th) => th.textContent.trim()));
check("tabelul are coloana „Depozite”", header.includes("Depozite"), header.join(" | "));
let text = await rowText(page, operatorEmail);
check("rândul operatorului arată „Toate”", /Toate/.test(text), text);
const adminButton = await page.$(`${section} tbody tr:has-text("${ACCOUNTS.admin.email}") button[aria-label="Alege depozitele"]`);
check("rândul adminului n-are buton de depozite", adminButton === null);

await page.click(`${section} tbody tr:has-text("${operatorEmail}") button[aria-label="Alege depozitele"]`);
await page.waitForTimeout(500);
await page.click('div[role="dialog"] >> text="Doar cele alese"');
await page.waitForTimeout(200);
// Negativă: fără niciun depozit bifat nu pleacă nimic la server.
let sent = 0;
page.on("request", (r) => r.url().includes("/work-points") && r.method() === "PUT" && sent++);
for (const box of await page.$$('div[role="dialog"] fieldset input[type="checkbox"]')) {
  if (await box.isChecked()) await box.uncheck();
}
await page.click('div[role="dialog"] button:has-text("Salvează")');
await page.waitForTimeout(400);
const required = await page.$('div[role="dialog"] >> text="Alege cel puțin un depozit."');
check("„Doar cele alese” fără depozit cere unul", required !== null);
check("și nu trimite nimic", sent === 0, `${sent} cereri`);

await page.check(`div[role="dialog"] fieldset label:has-text("${home.name}") input`);
await shot(page, "45-depozite-dialog");
await page.click('div[role="dialog"] button:has-text("Salvează")');
await page.waitForTimeout(1000);
text = await rowText(page, operatorEmail);
check("rândul arată depozitul ales", text.includes(home.name) && !/Toate/.test(text.split("@")[1] ?? ""), text);
await shot(page, "45-utilizatori");
const width = await page.$eval(`${section} table`, (t) => t.parentElement.scrollWidth - t.parentElement.clientWidth);
check("tabelul nu se lățește la 1440px", width <= 0, `${width}px`);

// ---------------------------------------------------------------- OPERATORUL RESTRÂNS
const operator = await newPage(browser, { width: 1440, height: 900 });
await login(operator, "operator");
const seenOp = await api(operator, "GET", `/api/v1/weighing-operations/${op.id}`);
check("operatorul primește 404 pe intrarea din celălalt depozit", seenOp.status === 404, `HTTP ${seenOp.status}`);
const list = (await api(operator, "GET", "/api/v1/weighing-operations")).json ?? [];
check("și n-o vede în listă", !list.some((o) => o.id === op.id) && list.every((o) => o.workPointId === home.id),
  `${list.length} rânduri`);
const depots = (await api(operator, "GET", "/api/v1/work-points")).json ?? [];
check("selectoarele lui au doar depozitul ales", depots.length === 1 && depots[0].id === home.id,
  depots.map((d) => d.name).join(", "));
const moved = await api(operator, "POST", "/api/v1/weighing-operations", { type: "IN", workPointId: other.id, date: today });
check("nu poate porni o intrare în celălalt depozit", moved.status === 404, `HTTP ${moved.status}`);
// Cele trei 404 de mai sus sunt chiar proba.
operator.problems = operator.problems.filter((p) => !p.includes("404"));

// ---------------------------------------------------------------- TELEFONUL
const phone = await newPage(browser, { width: 375, height: 800 });
await login(phone, "admin");
await phone.goto(BASE + "/setari/utilizatori", { waitUntil: "networkidle" });
await phone.waitForTimeout(900);
const overflow = await phone.evaluate(() => document.body.scrollWidth - window.innerWidth);
check("Utilizatorii nu lățesc pagina la 375px", overflow <= 0, `${overflow}px`);
await shot(phone, "45-utilizatori-telefon");

// ---------------------------------------------------------------- CURĂȚENIE
const back = await api(page, "PUT", `/api/v1/users/${operatorRow.id}/work-points`, { allWorkPoints: true, workPointIds: [] });
check("operatorul primește înapoi toate depozitele", back.json?.allWorkPoints === true);
await api(page, "POST", `/api/v1/weighing-operations/${op.id}/cancel`, { reason: "Proba 45" });
await api(page, "DELETE", `/api/v1/work-points/${other.id}`);

for (const p of [page, operator, phone]) {
  if (p.problems.length > 0) {
    console.log("  FAIL consola/rețeaua");
    for (const problem of [...new Set(p.problems)]) console.log(`         ${problem}`);
    fails += p.problems.length;
  }
}

await browser.close();
console.log(fails === 0 ? "\n✓ Proba 45 trece." : `\n✗ Proba 45: ${fails} probleme.`);
process.exit(fails === 0 ? 0 : 1);
