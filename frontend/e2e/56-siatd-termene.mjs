// Proba 56: termenele SIATD pe recepții (F6a, 28.09.2026) — Setări → SIATD, Cântar → SIATD, banda și dialogul.
//
// Ce apără: adminul bifează Ambalaje și Municipale cu data înrolării în Setări → SIATD; recepțiile pe coduri din modulele
// bifate apar în „De confirmat” (azi) sau „Ratate” (acum 10 zile, 3 zile la municipale), cu termenul lor; o recepție pe un
// cod fără modul nu apare; banda de pe Operațiuni spune aceleași cifre ca serverul și duce la tab; numărul deschide
// dialogul, care are rândul SIATD; „Confirmă” cu codul „P56-1” mută recepția în „Confirmate” cu codul; două bifate →
// „Confirmă selectate (2)” le confirmă fără cod; operatorul nu vede nici bife, nici butoane; 1440×900 fără derulare
// (pe `main#continut`) în fiecare subtab; 375px fără lățire.
//
// ⚠️ Lasă în urmă cinci intrări finalizate „Proba 56” pe primul depozit activ (patru confirmate în SIATD) și sortimentele
// „Carton/Hârtie/Fier P56 <număr>” (dezactivate). Modulele SIATD ale firmei se debifează la final.
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
const iso = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
const today = iso(new Date());
const tenDaysAgo = iso(new Date(Date.now() - 10 * 86_400_000));
const tag = Date.now() % 100000;
const api = (page, method, url, body) =>
  page.evaluate(
    async ({ method, url, body }) => {
      const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json", Authorization: "Bearer " + localStorage.getItem("eco_token") },
        body: body ? JSON.stringify(body) : undefined,
      });
      const text = (res.headers.get("content-type") ?? "").includes("json") ? await res.text() : "";
      return { status: res.status, json: text ? JSON.parse(text) : null };
    },
    { method, url, body }
  );
const scroll = (page) =>
  page.evaluate(() => {
    // Derulează `main#continut` (Layout.tsx), nu documentul: `documentElement` ar da mereu 0.
    const main = document.querySelector("#continut");
    return main.scrollHeight - main.clientHeight;
  });
const rowOf = (page, number) =>
  page.locator('[data-testid="siatd-row"]').filter({ has: page.locator(`button:text-is("${number}")`) });

const page = await newPage(browser, { width: 1440, height: 900 });
await login(page, "admin");

// --- Setări → SIATD, din interfață ---
await page.goto(BASE + "/setari/siatd", { waitUntil: "networkidle" });
await page.waitForTimeout(500);
for (const [module, from] of [["PACKAGING", "2024-01-01"], ["MUNICIPAL", "2024-01-01"]]) {
  const sw = page.locator(`#siatd-${module}`);
  if (!(await sw.isChecked())) await page.locator(`label[for="siatd-${module}"]`).click();
  await page.fill(`#siatd-${module}-from`, from);
}
await page.click('button:has-text("Salvează")');
await page.waitForTimeout(700);
const company = (await api(page, "GET", "/api/v1/companies/current")).json;
check("modulele se salvează cu data", company.siatdEnrolledFrom?.PACKAGING === "2024-01-01"
  && company.siatdEnrolledFrom?.MUNICIPAL === "2024-01-01", JSON.stringify(company.siatdEnrolledFrom));

// --- recepțiile de probă, prin API ---
const depot = ((await api(page, "GET", "/api/v1/work-points")).json ?? []).find((w) => w.active);
const partner = ((await api(page, "GET", "/api/v1/partners")).json ?? []).find((p) => p.active);
const article = async (name, code) => {
  const c = ((await api(page, "GET", "/api/v1/waste-codes?q=" + encodeURIComponent(code))).json ?? []).find((x) => x.code === code);
  return (await api(page, "POST", "/api/v1/waste-articles",
    { name: `${name} P56 ${tag}`, wasteCodeId: c.id, metal: false, forbiddenFromIndividuals: false })).json;
};
const cardboard = await article("Carton", "15 01 01");
const paper = await article("Hârtie", "20 01 01");
const scrap = await article("Fier", "17 04 05");
const reception = async (date, art) => {
  const op = (await api(page, "POST", "/api/v1/weighing-operations",
    { type: "IN", workPointId: depot.id, date, partnerId: partner.id, notes: "Proba 56" })).json;
  await api(page, "PUT", `/api/v1/weighing-operations/${op.id}/lines`, {
    grossKg: null, tareKg: null,
    lines: [{ articleId: art.id, grossKg: null, tareKg: null, netKg: 250, finalKg: null, unitPrice: 0.4, operationCode: null, notes: null }],
  });
  // Acum zece zile poate fi luna trecută: confirmarea D2 (28.09) e dată aici, nu e subiectul probei.
  const done = await api(page, "POST", `/api/v1/weighing-operations/${op.id}/finalize`, { confirmPastPeriod: true });
  return { id: op.id, number: op.number, ok: done.status === 200 };
};
const a = await reception(today, cardboard);
const b = await reception(today, cardboard);
const c = await reception(today, cardboard);
const late = await reception(tenDaysAgo, paper);
const none = await reception(today, scrap);
check("recepțiile de probă sunt finalizate", [a, b, c, late, none].every((r) => r.ok));

// --- banda de pe Operațiuni, aceleași cifre ca serverul ---
const summary = (await api(page, "GET", "/api/v1/depot-siatd/summary")).json;
await page.goto(BASE + "/cantar", { waitUntil: "networkidle" });
await page.waitForTimeout(600);
const strip = (await page.locator('[data-testid="siatd-strip"]').textContent().catch(() => "")) ?? "";
check("banda spune câte sunt de confirmat", strip.includes(`${summary.pending} de confirmat`), strip);
check("banda spune câte sunt ratate", summary.missed > 0 && strip.includes(`${summary.missed} ratate`), strip);
const operationsScroll = await scroll(page);
console.log(`  info Operațiuni derulează ${operationsScroll}px (54 px înainte de F6a, rândul de paginare)`);
await shot(page, "56-operatiuni-banda");
await page.locator('[data-testid="siatd-strip"]').click();
await page.waitForTimeout(600);
check("banda duce la ?tab=siatd", page.url().includes("tab=siatd"), page.url());

// --- din „Balotare”: banda stă singură acolo, iar numărul din tab tot deschide dialogul (recenzia finală F6a) ---
await page.goto(BASE + "/cantar", { waitUntil: "networkidle" });
await page.waitForTimeout(500);
await page.click('[role="tab"]:has-text("Balotare")');
await page.waitForTimeout(500);
await page.locator('[data-testid="siatd-strip"]').click();
await page.waitForTimeout(600);
await page.click('[role="tab"]:has-text("Ratate")');
await page.waitForTimeout(500);
await rowOf(page, late.number).locator(`button:text-is("${late.number}")`).click();
await page.waitForTimeout(700);
check("de pe Balotare, numărul deschide dialogul", (await page.locator('[data-testid="siatd-line"]').count()) === 1);
await page.keyboard.press("Escape");
await page.waitForTimeout(400);
await page.goto(BASE + "/cantar?tab=siatd", { waitUntil: "networkidle" });
await page.waitForTimeout(500);

// --- tabul: De confirmat / Ratate ---
check("recepțiile de azi sunt în „De confirmat”",
  (await rowOf(page, a.number).count()) === 1 && (await rowOf(page, b.number).count()) === 1);
// Celula termenului singură: `textContent` pe rând lipește celulele („250,000luni”).
const dueCell = ((await rowOf(page, a.number).locator("td").nth(-2).textContent()) ?? "").trim();
check("termenul de 5 zile la ambalaje e scris cu ziua", /^(luni|marți|miercuri|joi|vineri) \d\d\.\d\d$/.test(dueCell), dueCell);
check("recepția pe un cod fără modul nu apare", (await rowOf(page, none.number).count()) === 0);
check("1440×900 fără derulare — De confirmat", (await scroll(page)) <= 0, `${await scroll(page)}px`);
await shot(page, "56-de-confirmat");

await page.click('[role="tab"]:has-text("Ratate")');
await page.waitForTimeout(500);
check("recepția municipală de acum 10 zile e ratată", (await rowOf(page, late.number).count()) === 1);
check("1440×900 fără derulare — Ratate", (await scroll(page)) <= 0, `${await scroll(page)}px`);

// --- dialogul operațiunii are rândul SIATD ---
await rowOf(page, late.number).locator(`button:text-is("${late.number}")`).click();
await page.waitForTimeout(700);
const line = (await page.locator('[data-testid="siatd-line"]').textContent().catch(() => "")) ?? "";
check("dialogul spune „ratată”", line.includes("ratată"), line);
await page.keyboard.press("Escape");
await page.waitForTimeout(400);

// --- confirmarea uneia, cu cod ---
await page.click('[role="tab"]:has-text("De confirmat")');
await page.waitForTimeout(500);
await rowOf(page, a.number).locator('button:has-text("Confirmă")').click();
await page.fill("#siatd-code", "P56-1");
await page.locator('[role="dialog"] button:has-text("Confirmă")').click();
await page.waitForTimeout(700);
check("confirmata iese din „De confirmat”", (await rowOf(page, a.number).count()) === 0);
await page.click('[role="tab"]:has-text("Confirmate")');
await page.waitForTimeout(500);
check("e în „Confirmate”, cu codul", ((await rowOf(page, a.number).textContent().catch(() => "")) ?? "").includes("P56-1"));
check("1440×900 fără derulare — Confirmate", (await scroll(page)) <= 0, `${await scroll(page)}px`);

// --- două deodată, fără cod ---
await page.click('[role="tab"]:has-text("De confirmat")');
await page.waitForTimeout(500);
await rowOf(page, b.number).locator('input[type="checkbox"]').check();
await rowOf(page, c.number).locator('input[type="checkbox"]').check();
await page.click('button:has-text("Confirmă selectate (2)")');
check("la mai multe nu se cere cod", (await page.locator("#siatd-code").count()) === 0);
await page.locator('[role="dialog"] button:has-text("Confirmă")').click();
await page.waitForTimeout(800);
check("amândouă ies din „De confirmat”",
  (await rowOf(page, b.number).count()) === 0 && (await rowOf(page, c.number).count()) === 0);

// --- operatorul: fără bife și fără butoane ---
const operator = await newPage(browser, { width: 1440, height: 900 });
await login(operator, "operator");
await operator.goto(BASE + "/cantar?tab=siatd", { waitUntil: "networkidle" });
await operator.waitForTimeout(700);
await operator.click('[role="tab"]:has-text("Ratate")');
await operator.waitForTimeout(500);
check("operatorul vede ratata", (await rowOf(operator, late.number).count()) === 1);
check("operatorul n-are bife și nici „Confirmă”",
  (await operator.locator('[data-testid="siatd-row"] input[type="checkbox"]').count()) === 0
  && (await operator.locator('[data-testid="siatd-row"] button:has-text("Confirmă")').count()) === 0);

// --- telefonul ---
const phone = await newPage(browser, { width: 375, height: 800 });
await login(phone, "admin");
for (const url of ["/cantar?tab=siatd", "/setari/siatd"]) {
  await phone.goto(BASE + url, { waitUntil: "networkidle" });
  await phone.waitForTimeout(600);
  const overflow = await phone.evaluate(() => document.body.scrollWidth - window.innerWidth);
  check(`nu lățește pagina la 375px — ${url}`, overflow <= 0, `${overflow}px`);
}
await shot(phone, "56-telefon");

// --- ce lasă în urmă: recepția ratată confirmată, modulele debifate, sortimentele dezactivate ---
await api(page, "POST", "/api/v1/depot-siatd/confirmations", { operationIds: [late.id] });
const off = await api(page, "PUT", "/api/v1/companies/current/siatd", { enrolledFrom: {} });
check("modulele se debifează la final", off.status === 200 && Object.keys(off.json.siatdEnrolledFrom ?? {}).length === 0);
for (const art of [cardboard, paper, scrap]) {
  await api(page, "PUT", `/api/v1/waste-articles/${art.id}`, { ...art, active: false });
}

await browser.close();
console.log(fails === 0 ? "\nProba 56: OK" : `\nProba 56: ${fails} căderi`);
process.exit(fails === 0 ? 0 : 1);
