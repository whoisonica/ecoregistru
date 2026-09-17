// Proba 30: tabelul Clienți ca tablou de lucru (F-B din todo-clienti-abonamente.md, 17.09.2026).
//
// Ce se poate strica: (1) cifrele de sus lipsesc sau nu se potrivesc cu filtrele; (2) filtrele arată alte firme decât
// spun; (3) cele care cer atenție nu stau primele; (4) coloanele Abonament / Ultima factură / Utilizatori / Fișa spun
// altceva decât baza; (5) „⋯” pierde acțiunile vechi sau „Deschide” nu deschide firma; (6) tasta N nu deschide firma
// nouă; (7) după o invitație numărul de utilizatori rămâne vechi; (8) administratorul firmei citește lista; (9) la 375px
// pagina iese din ecran.
//
// Facturile se scriu cu `psql`, ca la proba 29 (`E2E_DB`, implicit `ecoregistru`). Lasă în urmă firmele
// „Proba 30 … <număr>”, două cu abonament și factură, și un utilizator invitat pe firma fără abonament.
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
const LATE = `Proba 30 Restant ${RUN}`;
const FAILED = `Proba 30 Căzută ${RUN}`;
const BARE = `Proba 30 Fără abonament ${RUN}`;

function sql(statement) {
  return execFileSync("psql", ["-h", "localhost", "-U", "eco", "-d", process.env.E2E_DB ?? "ecoregistru", "-tAc", statement], {
    env: { ...process.env, PGPASSWORD: process.env.E2E_DB_PASSWORD ?? "eco" },
    encoding: "utf8",
  }).trim().split("\n")[0];
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

// ---------------------------------------------------------------- datele
await login(page, "platform");
const ids = {};
for (const [key, name, full] of [["late", LATE, false], ["failed", FAILED, true], ["bare", BARE, true]]) {
  const created = await api("POST", "/api/v1/companies", {
    name, cui: validCui(), type: "GENERATOR", afmObligation: false,
    ...(full ? { address: "Str. Probei 30", caenCode: "1071", wasteManagerName: "Popescu Proba" } : {}),
  });
  check(`firma „${key}” creată`, created.status === 200, String(created.status));
  ids[key] = created.json?.id;
}
for (const key of ["late", "failed"]) {
  const sub = await api("PUT", `/api/v1/subscriptions/company/${ids[key]}`, {
    plan: "GENERATOR", startedAt: "2026-07-17", founder: false, billingEmail: `facturi+${RUN}${key}@proba.ro`,
    billingCounty: "Cluj", billingCity: "Cluj-Napoca", billingAddress: "Str. Probei nr. 30",
  });
  check(`abonamentul „${key}”`, sub.status === 200, String(sub.status));
  ids[key + "Sub"] = sub.json?.id;
}
const lines = `'[{"label":"Abonament Generator","quantity":1,"unitPrice":99,"amount":99}]'`;
const lateNumber = `8${RUN.slice(-4)}`;
sql(`insert into subscription_invoices (id, subscription_id, period_start, period_end, total, lines_json, status, due_date, fgo_serie, fgo_numar, issued_at, created_at)
  values (gen_random_uuid(), '${ids.lateSub}', '2026-08-17', '2026-09-16', 99, ${lines}, 'ISSUED', current_date - 6, 'WH', '${lateNumber}', now(), now()) returning id`);
const reason = "CUI-ul „RO0” nu e valid: FGO nu-l acceptă.";
sql(`insert into subscription_invoices (id, subscription_id, period_start, period_end, total, lines_json, status, last_error, created_at)
  values (gen_random_uuid(), '${ids.failedSub}', '2026-07-17', '2026-08-16', 389, ${lines}, 'DRAFT', '${reason}', now()) returning id`);

// ---------------------------------------------------------------- (1) cifrele
await page.goto(BASE + "/clienti", { waitUntil: "networkidle" });
await page.waitForSelector('[data-testid="client-figures"]', { timeout: 15000 });
await page.waitForTimeout(1000);
const figures = await page.textContent('[data-testid="client-figures"]');
check("patru cifre: activi, încasat, de încasat, cer atenție",
  /Clienți activi/i.test(figures) && /Încasat în/i.test(figures) && /De încasat/i.test(figures) && /Cer atenție/i.test(figures), figures);
check("nicio cifră „?” cu sursele venite", !figures.includes("?"), figures);
const pill = (label) => page.locator('label:has(input[name="client-filter"])').filter({ hasText: new RegExp(`^${label} `) });
const attentionPill = (await pill("Cer atenție").textContent()) ?? "";
const attentionFigure = await page.textContent('[data-figure="Cer atenție"] [data-figure-value]');
check("„Cer atenție” sus = numărul de pe filtru", /^\d+$/.test(attentionFigure ?? "") && attentionPill.trim().endsWith(` ${attentionFigure}`), `${attentionFigure} / ${attentionPill}`);
await shot(page, "30-clienti-1440");

// ---------------------------------------------------------------- (3) ordinea: cele cu probleme primele
const names = await page.$eval("main table", (t) => [...t.querySelectorAll("tbody tr td:first-child > span:first-child")].map((x) => x.textContent));
const firstCalm = await page.$eval("main table", (t) =>
  [...t.tBodies[0].rows].findIndex((tr) => !/Depășită|Emitere căzută/.test(tr.textContent) && !/^0$/.test(tr.children[3]?.textContent.trim() ?? ""))
);
const lastTroubled = await page.$eval("main table", (t) => {
  let last = -1;
  [...t.tBodies[0].rows].forEach((tr, i) => {
    if (/Depășită|Emitere căzută/.test(tr.textContent) || tr.children[3]?.textContent.trim() === "0") last = i;
  });
  return last;
});
check("cele care cer atenție stau înaintea celorlalte", firstCalm === -1 || lastTroubled < firstCalm || lastTroubled === -1,
  `ultimul cu probleme ${lastTroubled}, primul liniștit ${firstCalm}, ${names.length} rânduri`);

// ---------------------------------------------------------------- (2) filtrele și (4) coloanele
async function pick(label) {
  await pill(label).click();
  await page.waitForTimeout(300);
  return page.$eval("main table", (t) => [...t.tBodies[0].rows].map((tr) => tr.textContent));
}
// Caseta de căutare apare doar pe liste lungi; pe o bază nouă firmele se văd toate.
const search = page.locator('input[type="search"]').first();
const searchable = (await search.count()) > 0;
if (searchable) {
  await search.fill("Proba 30");
  await page.waitForTimeout(400);
}

let rows = await pick("Restanți");
check("Restanți: firma restantă, cu „Depășită 6 zile” și numărul facturii",
  rows.some((r) => r.includes(LATE) && r.includes("Depășită 6 zile") && r.includes(`WH ${lateNumber}`)), rows.join(" | ").slice(0, 300));
check("Restanți: fără celelalte", !rows.some((r) => r.includes(FAILED) || r.includes(BARE)));
rows = await pick("Emitere căzută");
check("Emitere căzută: firma căzută, cu motivul", rows.every((r) => r.includes("Emitere căzută")) && rows.some((r) => r.includes(FAILED) && r.includes("nu e valid")), rows.join(" | ").slice(0, 300));
rows = await pick("Fără abonament");
check("Fără abonament: firma fără abonament, nu cele cu abonament",
  rows.some((r) => r.includes(BARE) && r.includes("Fără abonament")) && !rows.some((r) => r.includes(LATE) || r.includes(FAILED)));
rows = await pick("Toți");
const lateRow = rows.find((r) => r.includes(LATE)) ?? "";
check("fișa goală: „3 lipsuri”; fișa plină: „Completă”", lateRow.includes("3 lipsuri") && (rows.find((r) => r.includes(BARE)) ?? "").includes("Completă"));
check("abonamentul: „Așteaptă prima plată” · Generator · 99 lei", /Generator · 99 lei/.test(lateRow), lateRow.slice(0, 200));

// ---------------------------------------------------------------- (5) rândul: Deschide și ⋯
const bareRow = page.locator("table").first().locator("tbody tr", { hasText: BARE }).first();
await bareRow.locator('button:has-text("Deschide")').click();
await page.waitForTimeout(500);
check("„Deschide” arată firma", /Editează firma/.test((await page.textContent('div[role="dialog"]')) ?? ""));
await page.keyboard.press("Escape");
await page.waitForTimeout(300);
await bareRow.locator('button[aria-haspopup], button[aria-label]').last().click();
await page.waitForTimeout(300);
const menu = await page.$$eval('[role="menuitem"]', (items) => items.map((i) => i.textContent.trim()));
check("⋯ are Abonament, Cabinet și Invită", ["Abonament", "Cabinet", "Invită"].every((w) => menu.some((m) => m.startsWith(w))), menu.join(", "));

// ---------------------------------------------------------------- (7) invitația schimbă numărul
await page.locator('[role="menuitem"]', { hasText: "Invită" }).click();
await page.waitForTimeout(400);
await page.fill("#i-email", `invitat+${RUN}@proba.ro`);
await page.click('div[role="dialog"] button[type="submit"]');
await page.waitForTimeout(1500);
const usersCell = (await bareRow.locator("td").nth(3).textContent())?.trim();
check("după invitație, utilizatorii firmei sunt 1", usersCell === "1", usersCell);

// ---------------------------------------------------------------- (6) tasta N
await page.mouse.click(5, 5);
await page.keyboard.press("n");
await page.waitForTimeout(500);
check("tasta N deschide firma nouă", /Adaugă firmă/.test((await page.textContent('div[role="dialog"]').catch(() => "")) ?? ""));
await page.keyboard.press("Escape");

// ---------------------------------------------------------------- tabelul încape la 1440
if (searchable) {
  await search.fill("");
  await page.waitForTimeout(400);
}
const box = await page.evaluate(() => {
  const t = document.querySelector("main table")?.parentElement;
  return t ? [t.scrollWidth, t.clientWidth] : [0, 0];
});
check("1440px: tabelul nu derulează lateral", box[0] <= box[1], box.join(" / "));

// ---------------------------------------------------------------- (9) 375px
await page.setViewportSize({ width: 375, height: 812 });
await page.waitForTimeout(500);
const overflow = await page.evaluate(() => [document.documentElement.scrollWidth, document.documentElement.clientWidth]);
check("375px: pagina nu derulează lateral", overflow[0] <= overflow[1], overflow.join(" / "));
await shot(page, "30-clienti-375");
await page.setViewportSize({ width: 1440, height: 900 });

// ---------------------------------------------------------------- (8) administratorul firmei
await page.goto(BASE + "/login", { waitUntil: "networkidle" });
await page.evaluate(() => localStorage.clear());
await login(page, "admin");
const forbidden = await api("GET", "/api/v1/companies/overview");
check("administratorul firmei nu citește lista clienților", forbidden.status === 403, String(forbidden.status));

await browser.close();
console.log(fails === 0 ? "\n30 clienți: OK" : `\n30 clienți: ${fails} căderi`);
process.exit(fails === 0 ? 0 : 1);
