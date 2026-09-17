// Proba 29: ecranul „Facturare” al platformei (F-A din todo-clienti-abonamente.md, 17.09.2026).
//
// Ce se poate strica: (1) tasta B sau intrarea din panou lipsesc, ori apar la administratorul firmei; (2) ultima
// rulare nu arată firma și motivul căzut; (3) filtrele numără sau arată alte facturi; (4) „Verifică plata” pleacă la
// FGO fără chei; (5) „Oprește” lasă factura căzută sau abonamentul în urmă; (6) dialogul de abonament mai are butonul
// de rulare; (7) la 375px pagina iese din ecran.
//
// Facturile nu se pot emite local (FGO n-are chei în dev), deci proba le scrie direct în baza locală cu `psql`:
// `E2E_DB` (implicit `ecoregistru`, ca în CI), utilizatorul `eco`. Lasă în urmă firma „Proba 29 A <număr>” cu două
// facturi; firma B își pierde abonamentul prin „Oprește”, chiar pasul probat.
import { execFileSync } from "node:child_process";
import { launch, newPage, login, shot, validCui, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
const RUN = Date.now().toString().slice(-8);
const A = `Proba 29 A ${RUN}`;
const B = `Proba 29 B ${RUN}`;

function sql(statement) {
  return execFileSync("psql", ["-h", "localhost", "-U", "eco", "-d", process.env.E2E_DB ?? "ecoregistru", "-tAc", statement], {
    env: { ...process.env, PGPASSWORD: process.env.E2E_DB_PASSWORD ?? "eco" },
    encoding: "utf8",
  }).trim().split("\n")[0]; // `insert … returning id` scrie și „INSERT 0 1” pe al doilea rând
}

async function api(method, path, body) {
  return page.evaluate(
    async ([method, path, body]) => {
      const res = await fetch(path, {
        method,
        headers: { Authorization: "Bearer " + localStorage.getItem("eco_token"), "Content-Type": "application/json" },
        body: body ? JSON.stringify(body) : undefined,
      });
      const text = await res.text();
      return { status: res.status, json: text ? JSON.parse(text) : null };
    },
    [method, path, body]
  );
}

// ---------------------------------------------------------------- datele: două firme, trei facturi, o rulare
await login(page, "platform");
const ids = {};
for (const [key, name, withAddress] of [["a", A, true], ["b", B, false]]) {
  const created = await api("POST", "/api/v1/companies", { name, cui: validCui(), type: "GENERATOR", afmObligation: false });
  check(`firma ${key.toUpperCase()} creată`, created.status === 200, String(created.status));
  ids[key] = created.json?.id;
  const sub = await api("PUT", `/api/v1/subscriptions/company/${ids[key]}`, {
    plan: "GENERATOR", startedAt: "2026-07-17", founder: false, billingEmail: `facturi+${RUN}${key}@proba.ro`,
    ...(withAddress ? { billingCounty: "Cluj", billingCity: "Cluj-Napoca", billingAddress: "Str. Probei nr. 29" } : {}),
  });
  check(`abonamentul lui ${key.toUpperCase()}`, sub.status === 200, String(sub.status));
  ids[key + "Sub"] = sub.json?.id;
}
const now = "now()";
const lines = `'[{"label":"Abonament Generator","quantity":1,"unitPrice":99,"amount":99}]'`;
const paidId = sql(`insert into subscription_invoices (id, subscription_id, period_start, period_end, total, lines_json, status, due_date, fgo_serie, fgo_numar, amount_paid, issued_at, paid_at, paid_by, created_at, payment_checked_at)
  values (gen_random_uuid(), '${ids.aSub}', '2026-07-17', '2026-08-16', 389, ${lines}, 'PAID', '2026-07-27', 'WH', '9${RUN.slice(-3)}1', 389, ${now}, ${now}, 'TRANSFER', ${now} - interval '2 day', ${now}) returning id`);
const overdueId = sql(`insert into subscription_invoices (id, subscription_id, period_start, period_end, total, lines_json, status, due_date, fgo_serie, fgo_numar, issued_at, created_at)
  values (gen_random_uuid(), '${ids.aSub}', '2026-08-17', '2026-09-16', 99, ${lines}, 'ISSUED', '2026-08-27', 'WH', '9${RUN.slice(-3)}2', ${now}, ${now} - interval '1 day') returning id`);
const reason = "CUI-ul „RO0” nu e valid: FGO nu-l acceptă. Corectează CUI-ul în fișa firmei.";
const failedId = sql(`insert into subscription_invoices (id, subscription_id, period_start, period_end, total, lines_json, status, last_error, created_at)
  values (gen_random_uuid(), '${ids.bSub}', '2026-07-17', '2026-08-16', 389, ${lines}, 'DRAFT', '${reason}', ${now}) returning id`);
const result = {
  configured: true, reserved: 0, issued: 0, failed: 1, paid: 1,
  failures: [{ invoiceId: failedId, owner: { kind: "company", id: ids.b }, client: B, reason }],
  notStarted: [], issuedInvoices: [],
  paidInvoices: [{ invoiceId: paidId, client: A, number: `WH 9${RUN.slice(-3)}1`, total: 389 }],
};
sql(`insert into billing_runs (id, started_at, finished_at, kind, result_json) values (gen_random_uuid(), now() + interval '1 hour', now() + interval '1 hour', 'MANUAL', '${JSON.stringify(result)}')`);
check("facturile și rularea scrise în bază", [paidId, overdueId, failedId].every((id) => /^[0-9a-f-]{36}$/.test(id)));

// ---------------------------------------------------------------- (1) tasta B
await page.goto(BASE + "/clienti", { waitUntil: "networkidle" });
await page.waitForTimeout(800);
await page.keyboard.press("b");
await page.waitForURL(/\/facturare$/, { timeout: 5000 }).catch(() => {});
check("tasta B deschide Facturare", page.url().endsWith("/facturare"), page.url());
await page.waitForSelector("#last-run", { timeout: 15000 });
await page.waitForTimeout(800);

// ---------------------------------------------------------------- (2) ultima rulare
const panel = await page.textContent('section[aria-labelledby="last-run"]');
check("rularea numește firma căzută și motivul", panel.includes(B) && panel.includes("nu e valid") && /Nu s-a emis/.test(panel));
check("emisele și plătitele stau strânse sub „Detalii”", !panel.includes(A) && /Detalii \(1\)/.test(panel), panel.slice(0, 200));
await page.locator('section[aria-labelledby="last-run"] button:has-text("Detalii")').click();
await page.waitForTimeout(300);
const panelOpen = await page.textContent('section[aria-labelledby="last-run"]');
check("„Detalii” arată factura plătită", panelOpen.includes(A) && /Plătită/.test(panelOpen));
check("cifrele rulării, acordate", /1 factură plătită/.test(panel) && /1 factură căzută/.test(panel), panel.slice(0, 200));
await shot(page, "29-facturare-1440");

// ---------------------------------------------------------------- (3) filtrele
const rowsText = async () => page.$$eval("table tbody tr", (trs) => trs.map((tr) => tr.textContent));
async function pick(label) {
  // `has-text` nu ține cont de litere mari: „Plătite” s-ar potrivi și pe „Emise, neplătite”.
  await page.locator('label:has(input[name="invoice-filter"])').filter({ hasText: new RegExp(`^${label} `) }).click();
  await page.waitForTimeout(300);
  return rowsText();
}
// F-B2 — ecranul pornește pe „De rezolvat”: căzutele și restantele, nu plătitele.
let rows = await rowsText();
const actionPill = page.locator('label:has(input[name="invoice-filter"])').filter({ hasText: /^De rezolvat / });
check("implicit: „De rezolvat” apăsat", (await actionPill.locator("input").isChecked()));
check("De rezolvat: B căzută și A restantă, fără plătita lui A",
  rows.some((r) => r.includes(B) && r.includes("Căzută")) && rows.some((r) => r.includes(A) && r.includes("Restantă")) &&
  !rows.some((r) => r.includes(A) && r.includes("Plătită")), rows.length + " rânduri");
rows = await pick("Căzute");
check("Căzute: firma B, numai căzute", rows.some((r) => r.includes(B)) && rows.every((r) => r.includes("Căzută")), rows.length + " rânduri");
check("Căzute: fără A", !rows.some((r) => r.includes(A)));
rows = await pick("Restante");
check("Restante: factura lui A, „Restantă”", rows.some((r) => r.includes(A) && r.includes("Restantă")) && rows.every((r) => r.includes("Restantă")));
rows = await pick("Plătite");
check("Plătite: factura lui A plătită prin transfer", rows.some((r) => r.includes(A) && /plătită/.test(r) && /transfer/.test(r)));
rows = await pick("Toate");
check("Toate: A de două ori și B", rows.filter((r) => r.includes(A)).length === 2 && rows.some((r) => r.includes(B)));

// ---------------------------------------------------------------- F-B2: căutarea, luna, paginarea pe server
const search = page.locator('input[type="search"]').first();
await search.fill(`WH 9${RUN.slice(-3)}1`);
await page.waitForTimeout(900);
rows = await rowsText();
check("căutarea după număr: doar factura plătită a lui A", rows.length === 1 && rows[0].includes(`WH 9${RUN.slice(-3)}1`), rows.length + " rânduri");
await search.fill(A);
await page.waitForTimeout(900);
await page.selectOption('select[aria-label="Luna perioadei"]', "2026-08");
await page.waitForTimeout(700);
rows = await rowsText();
check("luna august: doar restanța lui A (perioada 17.08)", rows.length === 1 && rows[0].includes("Restantă"), rows.join(" | ").slice(0, 200));
check("luna stă în adresă", /luna=2026-08/.test(page.url()), page.url());
await page.selectOption('select[aria-label="Luna perioadei"]', "");
// 55 de plătite în plus pe A: pagina are 50, a doua restul.
sql(`insert into subscription_invoices (id, subscription_id, period_start, period_end, total, lines_json, status, due_date, fgo_serie, fgo_numar, amount_paid, issued_at, paid_at, paid_by, created_at)
  select gen_random_uuid(), '${ids.aSub}', date '2020-01-01' + (n || ' month')::interval, date '2020-01-28' + (n || ' month')::interval, 99, ${lines}, 'PAID',
         date '2020-01-10' + (n || ' month')::interval, 'WH', '7${RUN.slice(-3)}' || n, 99, now(), now() - interval '100 day', 'TRANSFER', now() - interval '3 day'
  from generate_series(0, 54) n`);
await pick("Plătite");
await page.waitForTimeout(700);
rows = await rowsText();
const range = await page.textContent("body");
check("pagina 1: 50 de rânduri din 56", rows.length === 50 && /1[–-]50 din 56/.test(range), rows.length + " rânduri");
await page.locator('button:has-text("Înainte")').last().click();
await page.waitForTimeout(700);
rows = await rowsText();
check("pagina 2: 6 rânduri", rows.length === 6, rows.length + " rânduri");
await search.fill("");
await page.waitForTimeout(900);

// ---------------------------------------------------------------- (4) Verifică plata fără chei FGO
await pick("Restante");
const aRow = page.locator("table tbody tr", { hasText: A }).first();
check("rândul restant spune că plata e verificată sau nu", /verificat|neverificat/.test((await aRow.textContent()) ?? ""));
await aRow.locator('button:has-text("Verifică plata")').click();
await page.waitForTimeout(1200);
check("fără cheile FGO, spune asta și nu marchează nimic",
  /lipsesc cheile FGO/.test(await page.textContent("body")) && sql(`select status from subscription_invoices where id='${overdueId}'`) === "ISSUED");

// ---------------------------------------------------------------- (6) „Corectează” duce la abonamentul firmei (F-D)
await pick("Căzute");
await page.locator("table tbody tr", { hasText: B }).first().locator('button:has-text("Corectează")').click();
await page.waitForSelector('[data-testid="subscription-panel"]', { timeout: 8000 });
check("„Corectează” deschide abonamentul lui B, pe pagina firmei",
  new URL(page.url()).pathname === `/clienti/${ids.b}` && /tab=abonament/.test(page.url()), page.url());
const panelText = await page.textContent('[data-testid="subscription-panel"]');
check("tabul abonamentului n-are butonul de rulare", !/Emite facturile scadente acum/.test(panelText) && /Facturare/.test(panelText));
await page.goBack({ waitUntil: "networkidle" });
await page.waitForTimeout(600);

// ---------------------------------------------------------------- (5) Oprește
await page.locator("table tbody tr", { hasText: B }).first().locator('button:has-text("Oprește")').click();
await page.waitForSelector('[role="dialog"]:has-text("Oprești abonamentul")', { timeout: 5000 });
await page.locator('[role="dialog"] button:has-text("Oprește")').click();
await page.waitForTimeout(1500);
rows = await rowsText();
check("după „Oprește”, B nu mai e între căzute", !rows.some((r) => r.includes(B)));
const bSub = await api("GET", `/api/v1/subscriptions/company/${ids.b}`);
check("abonamentul lui B e șters (fără alte facturi)", bSub.status === 204, String(bSub.status));
const panelAfter = page.locator('section[aria-labelledby="last-run"] li', { hasText: B });
await page.locator('section[aria-labelledby="last-run"]').waitFor();
check("rândul din rulare rămâne, fără butoane", (await panelAfter.count()) === 1 && (await panelAfter.locator("button").count()) === 0);

// ---------------------------------------------------------------- (7) 375px
await page.setViewportSize({ width: 375, height: 812 });
await pick("Toate");
await page.waitForTimeout(500);
const overflow = await page.evaluate(() => [document.documentElement.scrollWidth, document.documentElement.clientWidth]);
check("375px: pagina nu derulează lateral", overflow[0] <= overflow[1], overflow.join(" / "));
await shot(page, "29-facturare-375");
await page.setViewportSize({ width: 1440, height: 900 });

// ---------------------------------------------------------------- (1) administratorul firmei
await page.goto(BASE + "/login", { waitUntil: "networkidle" });
await page.evaluate(() => localStorage.clear());
await login(page, "admin");
await page.goto(BASE + "/", { waitUntil: "networkidle" });
await page.waitForTimeout(1000);
check("administratorul firmei n-are Facturare în panou", !(await page.$('nav a[href="/facturare"], aside a[href="/facturare"]')));
const forbidden = await api("GET", "/api/v1/subscriptions/invoices");
check("…și nici facturile tuturor", forbidden.status === 403, String(forbidden.status));

await browser.close();
console.log(fails === 0 ? "\n29 facturare: OK" : `\n29 facturare: ${fails} căderi`);
process.exit(fails === 0 ? 0 : 1);
