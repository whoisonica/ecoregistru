// Proba 17: tabul „Persoane fizice” de lângă Parteneri (D1.7b).
//
// Backendul are regulile probate în `NaturalPersonRegistryIT` (CNP mascat, unic, izolare, ștergere).
// Aici se probează ce nu vede niciun test de backend: că tabul se deschide din URL și din clic, că un
// CNP cu cifra de control greșită își marchează rubrica fără să salveze, că lista arată doar ultimele
// 4 cifre și caută după ele, că formularul de editare primește CNP-ul întreg, și că vizualizatorul
// vede lista fără buton de adăugare. Persoana creată se dezactivează și se șterge la final.
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
// CNP valid (cifra de control 7), același ca în testele de backend; numele e unic pe rulare.
const CNP = "1900101123457";
const NAME = `Proba PF ${Date.now().toString().slice(-6)}`;

// ══════════════════════════════════════════════ ADMIN
const page = await newPage(browser);
await login(page, "admin");
await page.goto(BASE + "/parteneri", { waitUntil: "networkidle" });
await page.waitForTimeout(400);

const tab = page.locator('[role="tab"]:has-text("Persoane fizice")');
check("tabul apare pe Parteneri (firma demo are art. 48)", (await tab.count()) === 1);
await tab.click();
await page.waitForTimeout(600);
check("clicul scrie tabul în URL", page.url().includes("tab=persoane-fizice"), page.url());
check("„Adaugă partener” dispare pe tab", (await page.locator('button:has-text("Adaugă partener")').count()) === 0);

// Proba se poate relua: dacă rulează pe o bază unde CNP-ul există deja, curățăm întâi.
await page.click('button:has-text("Adaugă persoană")');
await page.fill("#np-name", NAME);
await page.fill("#np-cnp", "1900101123458");
await page.click('button[form="natural-person-form"]');
await page.waitForTimeout(400);
check("CNP-ul greșit își marchează rubrica", (await page.getAttribute("#np-cnp", "aria-invalid")) === "true");
check("și dialogul rămâne deschis", (await page.locator("#natural-person-form").count()) === 1);

await page.fill("#np-cnp", CNP);
await page.fill("#np-id", "CJ 123456");
await page.fill("#np-address", "Cluj-Napoca, str. Horea 1");
await page.click('button[form="natural-person-form"]');
await page.waitForTimeout(1000);
let body = await page.evaluate(() => document.body.innerText);
if (/Există deja o persoană cu CNP-ul ăsta/.test(body)) {
  console.log("  (CNP-ul exista de la o rulare anterioară: proba nu poate crea, verifică doar lista)");
  await page.keyboard.press("Escape");
}
// Caseta de căutare apare abia de la 10 rânduri (`SEARCH_FROM`), deci o bază proaspătă primește
// umplutură: persoane doar cu nume, prin API, cu sesiunea paginii. Se șterg la final.
const api = (method, url, body) =>
  page.evaluate(
    async ([m, u, b]) => {
      const res = await fetch(u, {
        method: m,
        headers: { Authorization: "Bearer " + localStorage.getItem("eco_token"), "Content-Type": "application/json" },
        body: b ? JSON.stringify(b) : undefined,
      });
      return res.ok && res.status !== 204 ? res.json() : res.status;
    },
    [method, url, body]
  );
const fillers = [];
for (let i = 0; i < 10; i++) {
  fillers.push((await api("POST", "/api/v1/natural-persons", { name: `${NAME} umplutură ${i}` })).id);
}
await page.reload({ waitUntil: "networkidle" });
await page.waitForTimeout(600);
await page.fill('input[placeholder^="Caută după nume sau ultimele cifre"]', "3457");
await page.waitForTimeout(400);
body = await page.evaluate(() => document.body.innerText);
check("căutarea după ultimele 4 cifre găsește persoana", /•••••••••3457/.test(body));
check("CNP-ul întreg nu e nicăieri în listă", !body.includes(CNP));
check("fișa completă e marcată pentru metal", /Complet pentru metal/.test(body));
await shot(page, "17-persoane-fizice-lista");

const row = page.locator("tr", { hasText: "•••••••••3457" }).first();
await row.locator('button:has-text("Editează")').click();
await page.waitForFunction(() => document.querySelector("#np-cnp")?.value?.length === 13, null, { timeout: 5000 })
  .catch(() => {});
check("formularul primește CNP-ul întreg", (await page.inputValue("#np-cnp")) === CNP);
await page.keyboard.press("Escape");
await page.waitForTimeout(300);

// Curățenia: dezactivare, apoi ștergere definitivă (persoana n-are operațiuni).
const personName = (await row.locator("td").first().innerText()).trim();
await row.locator('button:has-text("Dezactivează")').click();
await page.click('[role="dialog"] button:has-text("Dezactivează")');
await page.waitForTimeout(800);
// Filtrul de stare arată implicit doar activele (`useActiveFilter`); trecem pe „toate”.
await page.selectOption('select:has(option[value="inactive"])', "all");
await page.waitForTimeout(300);
const del = page.locator("tr", { hasText: personName }).first().locator('button:has-text("Șterge definitiv")');
check("fișa fără operațiuni oferă ștergerea definitivă", (await del.count()) === 1);
if (await del.count()) {
  await del.click();
  await page.click('[role="dialog"] button:has-text("Șterge definitiv")');
  await page.waitForTimeout(800);
  body = await page.evaluate(() => document.body.innerText);
  check("și fișa dispare", !body.includes("•••••••••3457"));
}
for (const id of fillers) {
  await api("DELETE", `/api/v1/natural-persons/${id}`);
  await api("DELETE", `/api/v1/natural-persons/${id}/definitiv`);
}
await page.close();

// ══════════════════════════════════════════════ VIEWER
const vpage = await newPage(browser);
await login(vpage, "viewer");
await vpage.goto(BASE + "/parteneri?tab=persoane-fizice", { waitUntil: "networkidle" });
await vpage.waitForTimeout(600);
const vbody = await vpage.evaluate(() => document.body.innerText);
check("vizualizatorul deschide tabul din URL", /Oamenii care vând depozitului/.test(vbody));
check("fără „Adaugă persoană”", (await vpage.locator('button:has-text("Adaugă persoană")').count()) === 0);
await vpage.close();

await browser.close();
console.log(fails ? `\n✗ ${fails} verificări au căzut` : "\n✓ tabul de persoane fizice");
process.exit(fails ? 1 : 0);
