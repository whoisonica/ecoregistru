// Proba 33: pagina firmei, `/clienti/:id` (F-D din todo-clienti-abonamente.md, 17.09.2026).
//
// Ce se poate strica: (1) „Deschide” nu duce la pagină sau capul ei nu spune starea; (2) taburile lipsesc sau nu stau
// în adresă; (3) fișa nu arată ce lipsește pentru dosar, ori salvarea nu stinge lipsurile sau calcă ce s-a scris;
// (4) utilizatorii și istoricul sunt ai firmei din comutator, nu ai celei din adresă — sau pagina mută comutatorul;
// (5) abonamentul nu se vede, „Verifică plata” și „Oprește” lipsesc de pe facturi, salvarea nu merge; (6) „Invită
// utilizator” și tasta N nu deschid invitația; (7) „Intră în cont” nu comută firma; (8) o firmă străină arată o pagină
// goală; (9) administratorul unei firme citește utilizatorii altei firme punând antetul; (10) la 375px pagina iese din
// ecran.
//
// Facturile se scriu cu `psql` (`E2E_DB`), ca la probele 29 și 30. Lasă în urmă firma „Proba 33 <număr>”, cu un
// abonament, o factură emisă și doi utilizatori invitați.
import { execFileSync } from "node:child_process";
import { launch, newPage, login, shot, switchCompany, validCui, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
const RUN = Date.now().toString().slice(-8);
const NAME = `Proba 33 ${RUN}`;
const INVITED = `proba33+${RUN}@proba.ro`;

function sql(statement) {
  return execFileSync("psql", ["-h", "localhost", "-U", "eco", "-d", process.env.E2E_DB ?? "ecoregistru", "-tAc", statement], {
    env: { ...process.env, PGPASSWORD: process.env.E2E_DB_PASSWORD ?? "eco" },
    encoding: "utf8",
  }).trim().split("\n")[0];
}

async function api(method, path, body, headers = {}) {
  return page.evaluate(
    async ([method, path, body, headers]) => {
      const res = await fetch(path, {
        method,
        headers: { Authorization: "Bearer " + localStorage.getItem("eco_token"), "Content-Type": "application/json", ...headers },
        body: body ? JSON.stringify(body) : undefined,
      });
      const text = await res.text();
      return { status: res.status, json: text ? JSON.parse(text) : null };
    },
    [method, path, body, headers]
  );
}

const storedTenant = () => page.evaluate(() => sessionStorage.getItem("eco_tenant") ?? localStorage.getItem("eco_tenant"));

// ---------------------------------------------------------------- datele
await login(page, "platform");
const created = await api("POST", "/api/v1/companies", { name: NAME, cui: validCui(), type: "GENERATOR", afmObligation: false });
check("firma creată", created.status === 200, String(created.status));
const id = created.json?.id;
const sub = await api("PUT", `/api/v1/subscriptions/company/${id}`, {
  plan: "GENERATOR", startedAt: "2026-08-17", founder: false, billingEmail: `facturi+${RUN}@proba.ro`,
  billingCounty: "Cluj", billingCity: "Cluj-Napoca", billingAddress: "Str. Probei nr. 33",
});
check("abonamentul", sub.status === 200, String(sub.status));
const lines = `'[{"label":"Abonament Generator","quantity":1,"unitPrice":99,"amount":99}]'`;
const number = `3${RUN.slice(-4)}`;
sql(`insert into subscription_invoices (id, subscription_id, period_start, period_end, total, lines_json, status, due_date, fgo_serie, fgo_numar, issued_at, created_at)
  values (gen_random_uuid(), '${sub.json?.id}', '2026-08-17', '2026-09-16', 99, ${lines}, 'ISSUED', current_date + 5, 'WH', '${number}', now(), now()) returning id`);
const invited = await api("POST", `/api/v1/companies/${id}/users`, { email: INVITED, role: "ADMIN", firstName: null, lastName: null });
check("un utilizator invitat", invited.status === 200, String(invited.status));
// Comutatorul pe firma demo: fără antetul pus de pagină, utilizatorii și istoricul ar fi ai ei.
await page.goto(BASE + "/", { waitUntil: "networkidle" });
const demo = await switchCompany(page, /Demo/i);
const tenantBefore = await storedTenant();
check("comutatorul stă pe firma demo", Boolean(demo && tenantBefore && tenantBefore !== id), `${demo} ${tenantBefore}`);

// ---------------------------------------------------------------- (1) „Deschide” și capul paginii
await page.goto(BASE + "/clienti", { waitUntil: "networkidle" });
const search = page.locator('input[type="search"]').first();
if ((await search.count()) > 0) {
  await search.fill(RUN);
  await page.waitForTimeout(400);
}
await page.locator("main table").first().locator("tbody tr", { hasText: NAME }).first().locator('button:has-text("Deschide")').click();
await page.waitForSelector('[data-testid="company-status"]', { timeout: 10000 });
check("„Deschide” duce la /clienti/:id", new URL(page.url()).pathname === `/clienti/${id}`, page.url());
check("titlul e numele firmei", (await page.textContent("h1"))?.trim() === NAME);
await page.waitForTimeout(800);
const status = (await page.textContent('[data-testid="company-status"]')) ?? "";
check("starea: abonamentul și 1 utilizator", /Așteaptă prima plată · Generator · 99 lei/.test(status) && /1 utilizator/.test(status), status);

// ---------------------------------------------------------------- (2) taburile
const tabNames = await page.$$eval('[role="tab"]', (ts) => ts.map((x) => x.textContent.replace(/\d+$/, "").trim()));
check("taburile: Profil · Utilizatori · Abonament și facturi · Istoric",
  JSON.stringify(tabNames) === JSON.stringify(["Profil", "Utilizatori", "Abonament și facturi", "Istoric"]), tabNames.join(" · "));
const tab = (label) => page.locator('[role="tab"]', { hasText: label });

// ---------------------------------------------------------------- (3) Profil: ce lipsește și salvarea
const gaps = (await page.textContent('[data-testid="profile-gaps"]')) ?? "";
check("„Ce lipsește pentru dosar”: adresa, codul CAEN, persoana desemnată",
  ["adresa", "codul CAEN", "persoana desemnată"].every((g) => gaps.includes(g)), gaps);
const navItems = await page.$$eval('nav[aria-label="Secțiunile fișei firmei"] a', (as) => as.length);
check("cuprinsul fișei are 7 secțiuni", navItems === 7, String(navItems));
check("fișa are numele și CUI-ul firmei", (await page.inputValue("#c-name")) === NAME && (await page.inputValue("#c-cui")) === created.json?.cui);
await shot(page, "33-firma-profil-1440");
await page.fill("#c-address", "Str. Probei nr. 33, Cluj-Napoca");
await page.fill("#c-caen", "1071");
await page.fill("#c-wm-name", "Popescu Proba");
await page.locator('#company-form button[type="submit"]').click();
await page.waitForTimeout(1500);
const saved = await api("GET", "/api/v1/companies");
const after = saved.json?.find((c) => c.id === id);
check("salvarea scrie rubricile", after?.address === "Str. Probei nr. 33, Cluj-Napoca" && after?.caenCode === "1071" && after?.wasteManagerName === "Popescu Proba");
check("după salvare lipsurile dispar", (await page.$('[data-testid="profile-gaps"]')) === null);
check("rubricile păstrează ce s-a scris după reîncărcarea listei", (await page.inputValue("#c-caen")) === "1071");
check("CUI greșit oprit în pagină", await (async () => {
  await page.fill("#c-cui", "RO12345678");
  await page.locator('#company-form button[type="submit"]').click();
  await page.waitForTimeout(400);
  const txt = (await page.textContent("#firma-identificare")) ?? "";
  await page.fill("#c-cui", created.json?.cui);
  return /CUI/.test(txt) && (await api("GET", "/api/v1/companies")).json?.find((c) => c.id === id)?.cui === created.json?.cui;
})());

// ---------------------------------------------------------------- (4) utilizatorii firmei, fără comutator
await tab("Utilizatori").click();
await page.waitForSelector("#utilizatori table tbody tr", { timeout: 8000 });
await page.waitForTimeout(800);
check("tabul Utilizatori stă în adresă", /tab=utilizatori/.test(page.url()), page.url());
let usersText = (await page.textContent("#utilizatori table")) ?? "";
check("lista e a firmei: invitatul ei, nu conturile firmei demo", usersText.includes(INVITED) && !usersText.includes("admin@demo.ro"), usersText.slice(0, 200));
await page.locator('#utilizatori button:has-text("Invită utilizator")').first().click();
await page.fill("#cu-email", `doi+${INVITED}`);
await page.click('div[role="dialog"] button[type="submit"]');
await page.waitForTimeout(1500);
usersText = (await page.textContent("#utilizatori table")) ?? "";
check("invitația din tab ajunge la firma din adresă", usersText.includes(`doi+${INVITED}`));
const onFirm = await api("GET", "/api/v1/users", undefined, { "X-Tenant-Id": id });
check("serverul are ambii invitați pe firmă", (onFirm.json ?? []).filter((u) => u.email.includes(INVITED)).length === 2, String(onFirm.json?.length));
check("comutatorul n-a fost mutat", (await storedTenant()) === tenantBefore, `${await storedTenant()} / ${tenantBefore}`);

// ---------------------------------------------------------------- (4) istoricul firmei
await tab("Istoric").click();
await page.waitForSelector("#jurnal-audit table tbody tr", { timeout: 8000 });
await page.waitForTimeout(1000);
const historyText = (await page.textContent("#jurnal-audit table")) ?? "";
check("istoricul are fișa salvată a firmei din adresă", /Datele firmei/.test(historyText) && historyText.includes(NAME), historyText.slice(0, 300));
check("istoricul nu are mișcările firmei demo", !/Mișcare/.test(historyText));

// ---------------------------------------------------------------- (5) abonamentul și facturile
await tab("Abonament și facturi").click();
await page.waitForSelector('[data-testid="subscription-panel"]', { timeout: 8000 });
await page.waitForTimeout(600);
const panel = page.locator('[data-testid="subscription-panel"]');
const invoiceRow = panel.locator("li", { hasText: `WH ${number}` }).first();
check("factura emisă, cu „neverificat încă”", /Emisă/.test((await invoiceRow.textContent()) ?? "") && /neverificat încă/.test((await invoiceRow.textContent()) ?? ""));
await invoiceRow.locator('button:has-text("Verifică plata")').click();
await page.waitForTimeout(1200);
check("„Verifică plata” fără cheile FGO spune asta", /lipsesc cheile FGO/.test(await page.textContent("body")));
check("prima factură și lunar, calculate", /Prima factură/.test(await panel.textContent()) && /Apoi lunar/.test(await panel.textContent()));
check("oprirea abonamentului stă separat", (await panel.locator("h2", { hasText: "Oprirea abonamentului" }).count()) === 1);
await page.fill("#sub-city", "Florești");
await page.locator('button[form="subscription-form"]').click();
await page.waitForTimeout(1200);
const subAfter = await api("GET", `/api/v1/subscriptions/company/${id}`);
check("salvarea abonamentului de pe pagină", subAfter.json?.billingCity === "Florești", subAfter.json?.billingCity);
await shot(page, "33-firma-abonament-1440");

// ---------------------------------------------------------------- (6) invitația din cap și tasta N
await page.locator('main button:has-text("Invită utilizator")').first().click();
await page.waitForTimeout(400);
check("„Invită utilizator” din cap deschide invitația firmei",
  ((await page.textContent('div[role="dialog"]')) ?? "").includes(`Invită utilizator în firma ${NAME}`));
await page.keyboard.press("Escape");
await page.waitForTimeout(300);
await page.mouse.click(5, 5);
await page.keyboard.press("n");
await page.waitForTimeout(400);
check("tasta N deschide invitația și rămâne pe pagina firmei (nu „Adaugă deșeuri” al firmei din comutator)",
  (await page.locator('div[role="dialog"]', { hasText: "Invită utilizator" }).count()) === 1 && new URL(page.url()).pathname === `/clienti/${id}`,
  page.url());
await page.keyboard.press("Escape");
await page.waitForTimeout(300);

// ---------------------------------------------------------------- (10) 375px
await page.setViewportSize({ width: 375, height: 812 });
for (const [label, testid] of [["Abonament și facturi", "subscription-panel"], ["Profil", null]]) {
  await tab(label).click();
  await page.waitForTimeout(600);
  if (testid) await page.waitForSelector(`[data-testid="${testid}"]`);
  const overflow = await page.evaluate(() => [document.documentElement.scrollWidth, document.documentElement.clientWidth]);
  check(`375px, ${label}: fără derulare laterală`, overflow[0] <= overflow[1], overflow.join(" / "));
}
await shot(page, "33-firma-375");
await page.setViewportSize({ width: 1440, height: 900 });

// ---------------------------------------------------------------- (8) firma străină
await page.goto(BASE + "/clienti/00000000-0000-0000-0000-000000000000", { waitUntil: "networkidle" });
await page.waitForTimeout(600);
check("o firmă care nu e în listă spune asta", /Firma nu e în lista ta/.test(await page.textContent("main")));

// ---------------------------------------------------------------- (7) „Intră în cont”
await page.goto(BASE + `/clienti/${id}`, { waitUntil: "networkidle" });
await page.locator('main button:has-text("Intră în cont")').click();
await page.waitForURL((u) => u.pathname === "/", { timeout: 5000 }).catch(() => {});
await page.waitForTimeout(800);
check("„Intră în cont” duce acasă, pe firma din pagină", new URL(page.url()).pathname === "/" && (await storedTenant()) === id, `${page.url()} ${await storedTenant()}`);
check("panoul arată firma aleasă", ((await page.textContent('[data-testid="company-label"]')) ?? "").includes(NAME));

// ---------------------------------------------------------------- (9) administratorul altei firme
await page.goto(BASE + "/login", { waitUntil: "networkidle" });
await page.evaluate(() => { localStorage.clear(); sessionStorage.clear(); });
await login(page, "admin");
const foreign = await api("GET", "/api/v1/users", undefined, { "X-Tenant-Id": id });
check("administratorul firmei demo, cu antetul altei firme, își vede doar colegii",
  foreign.status === 200 && !(foreign.json ?? []).some((u) => u.email.includes(INVITED)) && (foreign.json ?? []).some((u) => u.email === "admin@demo.ro"),
  `${foreign.status} ${(foreign.json ?? []).map((u) => u.email).join(",")}`);
await page.goto(BASE + `/clienti/${id}`, { waitUntil: "networkidle" });
await page.waitForTimeout(500);
check("administratorul firmei nu vede pagina", !(await page.$('[data-testid="company-status"]')));

const problems = page.problems.filter((p) => !/HTTP (503|403)/.test(p) && !/Failed to load resource/.test(p));
check("fără erori în consolă", problems.length === 0, problems.slice(0, 3).join(" | "));

await browser.close();
console.log(fails === 0 ? "\n  Proba 33: toate verificările trec." : `\n  Proba 33: ${fails} căderi.`);
process.exit(fails === 0 ? 0 : 1);
