// Proba 36: `/abonament` la client (F-E din todo-clienti-abonamente.md, 17.09.2026).
//
// Ce se poate strica: (1) afișajul de sus nu adună facturile neplătite sau nu spune că e restantă; (2) transferul
// arată alt IBAN, suma ori detaliile greșite, sau copiază IBAN-ul cu spații; (3) „Am plătit — verifică acum” nu spune
// când a fost verificată plata, sau o eroare FGO trece în tăcere; (4) datele de facturare nu se pot schimba, trec goale
// ori nu intră în jurnalul firmei; (5) facturile nu spun care e restantă; (6) plătit tot, pagina tot cere bani; (7) la
// 375px pagina iese din ecran; (8) operatorul schimbă datele de facturare.
//
// Platforma vede abonamentul firmei alese în comutator, deci proba nu are nevoie de un administrator nou. Facturile se
// scriu cu `psql` (`E2E_DB`), ca la probele 29, 30 și 33: FGO n-are chei în dev. Lasă în urmă firma „Proba 36 <număr>”
// cu abonamentul și două facturi plătite.
import { execFileSync } from "node:child_process";
import { launch, newPage, login, shot, switchCompany, validCui, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
await page.context().grantPermissions(["clipboard-read", "clipboard-write"], { origin: BASE });
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
const RUN = Date.now().toString().slice(-8);
const NAME = `Proba 36 ${RUN}`;

function sql(statement) {
  return execFileSync("psql", ["-h", "localhost", "-U", "eco", "-d", process.env.E2E_DB ?? "ecoregistru", "-tAc", statement], {
    env: { ...process.env, PGPASSWORD: process.env.E2E_DB_PASSWORD ?? "eco" },
    encoding: "utf8",
  }).trim().split("\n")[0];
}

async function api(method, path, body) {
  return page.evaluate(
    async ([method, path, body]) => {
      const tenant = sessionStorage.getItem("eco_tenant") ?? localStorage.getItem("eco_tenant");
      const res = await fetch(path, {
        method,
        headers: {
          Authorization: "Bearer " + localStorage.getItem("eco_token"),
          "Content-Type": "application/json",
          ...(tenant ? { "X-Tenant-Id": tenant } : {}),
        },
        body: body ? JSON.stringify(body) : undefined,
      });
      const text = await res.text();
      return { status: res.status, json: text ? JSON.parse(text) : null };
    },
    [method, path, body]
  );
}

const display = () => page.locator('[data-testid="billing-display"]');
const text = async (selector) => ((await page.textContent(selector)) ?? "").replace(/\s+/g, " ");

// ---------------------------------------------------------------- datele
await login(page, "platform");
const created = await api("POST", "/api/v1/companies", { name: NAME, cui: validCui(), type: "GENERATOR", afmObligation: false });
check("firma creată", created.status === 200, String(created.status));
const id = created.json?.id;
const sub = await api("PUT", `/api/v1/subscriptions/company/${id}`, {
  plan: "GENERATOR", startedAt: "2026-08-17", founder: false, billingEmail: `facturi+${RUN}@proba.ro`,
  billingCounty: "Cluj", billingCity: "Cluj-Napoca", billingAddress: "Str. Probei nr. 36",
});
check("abonamentul", sub.status === 200, String(sub.status));
const lines = `'[{"label":"Abonament Generator","quantity":1,"unitPrice":99,"amount":99}]'`;
const older = `5${RUN.slice(-4)}`;
const newer = `6${RUN.slice(-4)}`;
// Cea restantă de 6 zile și cea care vine peste 4, amândouă citite de FGO chiar acum.
sql(`insert into subscription_invoices (id, subscription_id, period_start, period_end, total, lines_json, status, due_date, fgo_serie, fgo_numar, issued_at, created_at, payment_checked_at)
  values (gen_random_uuid(), '${sub.json?.id}', '2026-08-17', '2026-09-16', 389, ${lines}, 'ISSUED', current_date - 6, 'WH', '${older}', now(), now(), now()),
         (gen_random_uuid(), '${sub.json?.id}', '2026-09-17', '2026-10-16', 99.5, ${lines}, 'ISSUED', current_date + 4, 'WH', '${newer}', now(), now(), now()) returning id`);

await page.goto(BASE + "/", { waitUntil: "networkidle" });
await switchCompany(page, new RegExp(NAME));
await page.goto(BASE + "/abonament", { waitUntil: "networkidle" });
await page.waitForSelector('[data-testid="billing-display"]');

// ---------------------------------------------------------------- (1) afișajul
check("afișajul spune restanta", (await display().getAttribute("data-state")) === "OVERDUE", await display().getAttribute("data-state"));
const shown = await text('[data-testid="billing-display"]');
check("„Restantă de 6 zile”", /Restantă de 6 zile/i.test(shown), shown.slice(0, 120));
check("suma adună amândouă facturile: 488,50", (await text('[data-testid="billing-amount"]')).trim() === "488,50", await text('[data-testid="billing-amount"]'));
check("numește amândouă facturile, cea veche întâi", shown.includes(`WH ${older}, WH ${newer}`), shown);

// ---------------------------------------------------------------- (2) transferul
const copies = await page.$$eval('[data-testid="billing-transfer"] [data-copy]', (els) => els.map((e) => e.getAttribute("data-copy")));
check("beneficiarul, CUI-ul, IBAN-ul și banca din contract",
  copies[0] === "ONSIA S.R.L." && copies[1] === "51779887" && copies[2] === "RO32RZBR0000060027995375" && copies[3] === "Raiffeisen Bank România",
  copies.join(" | "));
check("suma pentru bancă și detaliile plății", copies[4] === "488,50" && copies[5] === `WH ${older}, WH ${newer}`, copies.slice(4).join(" | "));
check("IBAN-ul se vede în grupuri de patru", (await text('[data-testid="billing-transfer"]')).includes("RO32 RZBR 0000 0600 2799 5375"));
// Al treilea rând e IBAN-ul; după poziție, nu după valoare, ca un IBAN copiat greșit să cadă aici, nu să nu fie găsit.
await page.locator('[data-testid="billing-transfer"] [data-copy]').nth(2).click();
const clip = await page.evaluate(() => navigator.clipboard.readText()).catch((e) => "eroare: " + e.message);
check("„Copiază” pune IBAN-ul fără spații în clipboard", clip === "RO32RZBR0000060027995375", clip);
await shot(page, "36-abonament-restant");

// ---------------------------------------------------------------- (3) „Am plătit — verifică acum”
await page.locator('[data-testid="billing-display"] button:has-text("Am plătit")').click();
await page.waitForFunction(() => /Verificat la/.test(document.querySelector('[data-testid="billing-check-status"]')?.textContent ?? ""), null, { timeout: 5000 }).catch(() => {});
const status = await text('[data-testid="billing-check-status"]');
check("citită acum două minute: spune ora și că plata n-a ajuns", /Verificat la \d\d:\d\d: plata nu a ajuns încă/.test(status), status);
sql(`update subscription_invoices set payment_checked_at = now() - interval '1 hour' where fgo_numar in ('${older}', '${newer}')`);
await page.locator('[data-testid="billing-display"] button:has-text("Am plătit")').click();
const toast = await page.waitForSelector('[role="status"]:has-text("FGO"), [role="alert"]:has-text("FGO")', { timeout: 5000 }).catch(() => null);
check("fără cheile FGO, verificarea spune de ce n-a mers", Boolean(toast), toast ? await toast.textContent() : "niciun mesaj");
check("și nu marchează nimic plătit", (await display().getAttribute("data-state")) === "OVERDUE");

// ---------------------------------------------------------------- (5) facturile
const rows = await page.$$eval('[data-testid="billing-invoices"] tbody tr', (trs) => trs.map((tr) => tr.textContent.replace(/\s+/g, " ")));
check("două facturi în tabel", rows.length === 2, String(rows.length));
check("cea veche „Restantă”, cea nouă „De plată”",
  rows.some((r) => r.includes(`WH ${older}`) && r.includes("Restantă")) && rows.some((r) => r.includes(`WH ${newer}`) && r.includes("De plată")),
  rows.join(" || "));

// ---------------------------------------------------------------- (4) datele de facturare
await page.locator('[data-testid="billing-data"] button:has-text("Schimbă")').click();
await page.waitForSelector('[role="dialog"] #billing-city');
check("dialogul arată firma și CUI-ul, fără să le poată schimba",
  (await text('[role="dialog"]')).includes(NAME) && !(await page.$('[role="dialog"] input[name="cui"], [role="dialog"] #billing-cui')));
await page.fill('[role="dialog"] #billing-city', "");
await page.click('[role="dialog"] button[type="submit"]');
check("localitatea goală e oprită", /Completează rubrica/.test(await text('[role="dialog"]')));
const NEW_EMAIL = `contabil+${RUN}@proba.ro`;
await page.fill('[role="dialog"] #billing-email', NEW_EMAIL);
check("schimbarea emailului spune că anunțăm adresa veche", /Anunțăm și adresa veche/.test(await text('[role="dialog"]')));
await page.fill('[role="dialog"] #billing-city', "Oradea");
await page.selectOption('[role="dialog"] #billing-county', "Bihor");
await page.click('[role="dialog"] button[type="submit"]');
await page.waitForSelector('[role="dialog"]', { state: "detached", timeout: 5000 }).catch(() => {});
await page.waitForTimeout(400);
const card = await text('[data-testid="billing-data"]');
check("cardul arată emailul și adresa noi", card.includes(NEW_EMAIL) && card.includes("Oradea, Bihor"), card);
check("schimbarea e în jurnalul firmei",
  sql(`select count(*) from audit_log where company_id = '${id}' and entity_type = 'Subscription'`) === "1");

// ---------------------------------------------------------------- (7) telefonul
await page.setViewportSize({ width: 375, height: 800 });
await page.waitForTimeout(500);
const overflow = await page.evaluate(() => [document.documentElement.scrollWidth, document.documentElement.clientWidth]);
check("375px: fără derulare laterală", overflow[0] <= overflow[1], overflow.join(" / "));
check("375px: facturile ca rânduri, nu tabel",
  (await page.locator('[data-testid="billing-invoices"] ul li').count()) === 2 && !(await page.locator('[data-testid="billing-invoices"] table').isVisible()));
await shot(page, "36-abonament-375");
await page.setViewportSize({ width: 1440, height: 900 });

// ---------------------------------------------------------------- (6) totul plătit
sql(`update subscription_invoices set status = 'PAID', paid_at = now() where fgo_numar in ('${older}', '${newer}')`);
await page.reload({ waitUntil: "networkidle" });
await page.waitForSelector('[data-testid="billing-display"]');
check("plătit tot: „Totul e plătit”", (await display().getAttribute("data-state")) === "PAID_UP" && /Totul e plătit/.test(await text('[data-testid="billing-display"]')));
check("și nu mai cere transfer", !(await page.$('[data-testid="billing-transfer"]')));
check("spune următoarea factură", /Următoarea factură pe \d\d\.\d\d\.\d{4}/.test(await text('[data-testid="billing-display"]')));
await shot(page, "36-abonament-platit");

// ---------------------------------------------------------------- (8) operatorul
await page.evaluate(() => { localStorage.clear(); sessionStorage.clear(); });
await login(page, "operator");
const denied = await api("PUT", "/api/v1/billing/details", {
  billingEmail: "x@proba.ro", billingCounty: "Cluj", billingCity: "Cluj", billingAddress: "Str. 1",
});
check("operatorul nu schimbă datele de facturare", denied.status === 403, String(denied.status));

const problems = page.problems.filter((p) => !/HTTP (503|403)/.test(p) && !/Failed to load resource/.test(p));
check("fără erori în consolă", problems.length === 0, problems.slice(0, 3).join(" | "));

await browser.close();
console.log(fails === 0 ? "\n  Proba 36: toate verificările trec." : `\n  Proba 36: ${fails} căderi.`);
process.exit(fails === 0 ? 0 : 1);
