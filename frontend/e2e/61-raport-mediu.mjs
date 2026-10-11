// Proba 61: raportul anual de mediu (RAM, V86 — Andreea, 09.10.2026) — ecranul, autorizația, termenele, Word-ul.
//
// Ce se poate strica: (0) meniul n-are „Raport de mediu” imediat după „Energie” sau taburile lipsesc din panou;
// (1) „Da” la reclamații nu deschide rubrica textului, sau răspunsurile nu se păstrează la salvare; (2) „Da” la control
// nu arată lista, un control adăugat nu se salvează sau nu revine după reîncărcare; (3) o monitorizare nu se salvează;
// (4) autorizația nu se salvează, ziua vizei nu se socotește din data emiterii (20.12 → 21 octombrie) sau „Altă zi”
// nu ține ziua scrisă; (5) pe Termene nu apar „Raportul anual de mediu” (15 ianuarie) și „Cererea de viză” cu
// legăturile lor; (6) „Descarcă Word” nu dă un .docx; (7) fișa derulează la 1440×900 sau lateral la 375px.
// Date-robustă: AN vine din ceas; raportul se completează pe AN-1. La început golește anul (API); la sfârșit pune
// autorizația demo la loc. ⚠️ Lasă în urmă: pe firma demo, anul AN-1 cu răspunsurile, un control și o monitorizare.
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
const AN = new Date().getFullYear();
const Y = AN - 1;
const KNOWN_LOGIN_401 = /^\[HTTP 401\] POST \S*\/api\/v1\/auth\/login(\?|$)|^\[consolă error\] Failed to load resource: the server responded with a status of 401/;
const isWrite = (r) => r.request().method() === "PUT" && r.url().includes("/api/v1/environmental-report");

/** O cerere API cu sesiunea paginii (tokenul și firma din tab). */
const api = (method, path, body) =>
  page.evaluate(
    async ({ method, path, body }) => {
      const tenant = sessionStorage.getItem("eco_tenant") ?? localStorage.getItem("eco_tenant");
      const r = await fetch(path, {
        method,
        headers: {
          Authorization: "Bearer " + localStorage.getItem("eco_token"),
          "Content-Type": "application/json",
          ...(tenant ? { "X-Tenant-Id": tenant } : {}),
        },
        body: body ? JSON.stringify(body) : undefined,
      });
      return r.ok ? r.json() : { status: r.status };
    },
    { method, path, body }
  );
async function save(label = "Salvează") {
  const saved = page.waitForResponse(isWrite, { timeout: 8000 }).catch(() => null);
  await page.getByRole("button", { name: label, exact: true }).click();
  const r = await saved;
  await page.waitForTimeout(400);
  return r?.ok() ?? false;
}

await login(page, "admin");
await page.goto(BASE + "/", { waitUntil: "networkidle" });
const original = await api("GET", `/api/v1/environmental-report?year=${Y}`);
await api("PUT", "/api/v1/environmental-report", { year: Y, inspections: [], monitorings: [] });

// (0) Meniul.
const meniu = await page.evaluate(() =>
  [...document.querySelectorAll('#navigatie-principala nav a[data-nav="row"]')].map((a) => a.getAttribute("href"))
);
check("meniul are „Raport de mediu” imediat după „Energie”", meniu.indexOf("/raport-mediu") === meniu.indexOf("/energie") + 1 && meniu.includes("/energie"),
  meniu.join(" "));
await page.click('#navigatie-principala nav a[data-nav="row"][href="/raport-mediu"]');
await page.waitForURL((u) => u.pathname === "/raport-mediu", { timeout: 5000 }).catch(() => {});
await page.goto(BASE + `/raport-mediu?an=${Y}`, { waitUntil: "networkidle" });
await page.waitForTimeout(600);
check("pagina se deschide cu titlul anului", (await page.locator("h1").innerText()).includes(`Raportul anual de mediu · ${Y}`));
const tabs = await page.getByRole("tab").allInnerTexts();
check("taburile: Anul, Controale, Monitorizări, Autorizația", tabs.join("|") === "Anul|Controale|Monitorizări|Autorizația", tabs.join("|"));

// (1) Anul: Nu la incidente, Da la reclamații (cu text), Nu la prejudiciu.
const yesNo = (labelledBy) => page.locator(`[aria-labelledby="${labelledBy}"]`);
await yesNo("ram-incidente").getByText("Nu", { exact: true }).click();
await yesNo("ram-reclamatii").getByText("Da", { exact: true }).click();
check("„Da” la reclamații deschide rubrica textului", await page.locator("#ram-reclamatii-text").isVisible());
await page.fill("#ram-reclamatii-text", "O sesizare a unui vecin, rezolvată.");
await yesNo("ram-prejudiciu").getByText("Nu", { exact: true }).click();
check("salvarea anului trece", await save());
// Precompletarea e a unui an niciodată salvat: AN-2, pe care proba nu-l scrie.
await page.goto(BASE + `/raport-mediu?an=${Y - 1}`, { waitUntil: "networkidle" });
await page.waitForTimeout(500);
check("„Alte cerințe” vine precompletat pe un an nesalvat", (await page.inputValue("#ram-alte-cerinte")).startsWith("Evidența gestiunii deșeurilor"));
await page.goto(BASE + `/raport-mediu?an=${Y}`, { waitUntil: "networkidle" });
await page.waitForTimeout(500);

// (2) Controale.
await page.getByRole("tab", { name: "Controale" }).click();
await yesNo("ram-control").getByText("Da", { exact: true }).click();
await page.getByRole("button", { name: "Adaugă un control" }).click();
await page.fill("#ram-c0-inst", "Garda de Mediu");
await page.fill("#ram-c0-nr", "118");
await page.fill("#ram-c0-data", `${Y}-06-03`);
await page.fill("#ram-c0-masuri", "Salubritate pe amplasament.");
check("salvarea controlului trece", await save());

// (3) Monitorizări.
await page.getByRole("tab", { name: "Monitorizări" }).click();
await page.getByRole("button", { name: "Adaugă o monitorizare" }).click();
await page.fill("#ram-m0-factor", "Zgomot");
await page.fill("#ram-m0-nr", "Z-0457");
check("salvarea monitorizării trece", await save());

await page.reload({ waitUntil: "networkidle" });
await page.waitForTimeout(600);
check("după reîncărcare, monitorizarea e acolo", (await page.inputValue("#ram-m0-nr").catch(() => "")) === "Z-0457");
await page.getByRole("tab", { name: "Controale" }).click();
check("după reîncărcare, controlul e acolo", (await page.inputValue("#ram-c0-nr").catch(() => "")) === "118");
await page.getByRole("tab", { name: "Anul", exact: true }).click();
check("după reîncărcare, textul reclamației e acolo", (await page.inputValue("#ram-reclamatii-text").catch(() => "")) === "O sesizare a unui vecin, rezolvată.");

// (4) Autorizația și ziua vizei.
await page.getByRole("tab", { name: "Autorizația" }).click();
await page.fill("#ram-aut-nr", "APM-CJ-123");
await page.fill("#ram-aut-emitent", "ANMAP – Direcția Județeană pentru Mediu Cluj");
await page.fill("#ram-aut-emisa", "2015-12-20");
await page.fill("#ram-aut-admin", "Ion Administrator");
if (await page.getByRole("button", { name: "Înapoi la ziua socotită" }).count()) {
  await page.getByRole("button", { name: "Înapoi la ziua socotită" }).click();
}
check("salvarea autorizației trece", await save());
check("ziua vizei se socotește: 21 octombrie", await page.getByText("Socotit: 21 octombrie").isVisible());
check("termenul raportului: 15 ianuarie", (await page.inputValue("#ram-termen-zi")) === "15" && (await page.inputValue("#ram-termen-luna")) === "1");
await shot(page, "61-raport-autorizatie");

// (5) Termene.
await page.goto(BASE + "/termene", { waitUntil: "networkidle" });
await page.waitForTimeout(800);
const ram = page.locator('[data-testid="deadlines-todo"] tbody tr', { hasText: "Raportul anual de mediu (RAM)" });
const viza = page.locator('[data-testid="deadlines-todo"] tbody tr', { hasText: "Cererea de viză anuală" });
check("Termene: rândul raportului, pe 15.01", (await ram.count()) === 1 && (await ram.first().innerText()).includes("15.01."),
  (await ram.count()) ? (await ram.first().innerText()).replace(/\s+/g, " ").slice(0, 120) : "lipsă");
check("Termene: rândul vizei, pe 21.10", (await viza.count()) === 1 && (await viza.first().innerText()).includes("21.10."),
  (await viza.count()) ? (await viza.first().innerText()).replace(/\s+/g, " ").slice(0, 120) : "lipsă");
const link = ram.first().getByRole("link", { name: "Raportul anual de mediu" });
check("rândul raportului duce la raport", (await link.count()) === 1 && ((await link.getAttribute("href")) ?? "").startsWith("/raport-mediu?an="));

// (6) Word.
await page.goto(BASE + `/raport-mediu?an=${Y}`, { waitUntil: "networkidle" });
await page.waitForTimeout(600);
const [dl] = await Promise.all([
  page.waitForEvent("download", { timeout: 15000 }).catch(() => null),
  page.getByRole("button", { name: "Descarcă Word" }).click(),
]);
check("„Descarcă Word” dă un .docx", dl !== null && dl.suggestedFilename().endsWith(".docx"), dl ? dl.suggestedFilename() : "nimic");
await shot(page, "61-raport-anul");

// (7) Derularea, pe taburile care se pot lungi.
for (const tab of ["", "controale", "monitorizari", "autorizatie"]) {
  await page.goto(BASE + `/raport-mediu?an=${Y}${tab ? "&tab=" + tab : ""}`, { waitUntil: "networkidle" });
  await page.waitForTimeout(500);
  const over = await page.evaluate(() => {
    const m = document.querySelector("main#continut");
    return m.scrollHeight - m.clientHeight;
  });
  check(`1440×900, tabul „${tab || "Anul"}”: main#continut fără derulare`, over <= 1, `${over}px`);
}
const tel = await newPage(browser, { width: 375, height: 800 });
await login(tel, "admin");
await tel.goto(BASE + `/raport-mediu?an=${Y}&tab=controale`, { waitUntil: "networkidle" });
await tel.waitForTimeout(800);
const lat = await tel.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
check("375px fără derulare laterală", lat <= 0, `${lat}px`);
await shot(tel, "61-raport-telefon");

// Autorizația demo, la loc.
if (original?.permit) {
  const { expiry, visaComputedMonth, visaComputedDay, ...permit } = original.permit;
  void expiry, visaComputedMonth, visaComputedDay;
  await api("PUT", `/api/v1/environmental-report/permit?year=${Y}`, permit);
}

for (const p of [page, tel]) {
  const real = p.problems.filter((x) => !KNOWN_LOGIN_401.test(x));
  check("fără erori de consolă sau HTTP", real.length === 0, [...new Set(real)].join(" | "));
}

await browser.close();
console.log(fails ? `\n${fails} FAIL` : "\nToate OK");
process.exit(fails ? 1 : 0);
