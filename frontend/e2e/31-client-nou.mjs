// Proba 31: „Client nou” în pași (F-C din todo-clienti-abonamente.md, 17.09.2026).
//
// Ce se poate strica: (1) „Creează contul” pe o cerere încă face firma pe loc, fără pași; (2) răspunsurile cererii nu
// ajung în pași; (3) pasul 1 trece fără CUI valid; (4) abonamentul pleacă fără adresa pe care FGO o cere; (5) prima
// factură nu se arată sau nu e cea din grilă; (6) la final lipsește abonamentul sau invitația, sau cererea rămâne nouă;
// (7) o invitație refuzată lasă totuși firma în urmă; (8) tasta N nu duce la pagina nouă; (9) la 375px pagina iese din
// ecran; (10) administratorul unei firme poate crea clienți.
//
// Lasă în urmă firma „Proba 31 <număr>” cu abonament și un administrator invitat.
import { launch, newPage, login, shot, validCui, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
const RUN = Date.now().toString().slice(-8);
const NAME = `Proba 31 ${RUN}`;
const CUI = validCui();
const CONTACT = `admin+${RUN}@proba31.ro`;

async function api(p, method, path, body) {
  return p.evaluate(
    async ([method, path, body]) => {
      const token = localStorage.getItem("eco_token");
      const res = await fetch(path, {
        method,
        headers: { ...(token ? { Authorization: "Bearer " + token } : {}), "Content-Type": "application/json" },
        body: body ? JSON.stringify(body) : undefined,
      });
      const text = await res.text();
      let json = null;
      try {
        json = text ? JSON.parse(text) : null;
      } catch {
        json = null;
      }
      return { status: res.status, json };
    },
    [method, path, body]
  );
}
const text = async (selector = "main") => (await page.textContent(selector).catch(() => "")) ?? "";
const button = (label) => page.locator(`main button:has-text("${label}")`).last();
const card = (name, value) => page.locator(`label:has(input[name="${name}"][value="${value}"])`);

// ---------------------------------------------------------------- (1) cererea trece în pași, nu în firmă
await login(page, "platform");
const submitted = await api(page, "POST", "/api/v1/account-requests", {
  companyName: NAME, cui: CUI, companyType: "GENERATOR", companyAddress: "Str. Probei nr. 31",
  workPointName: "Sediu", workPointAddress: "Str. Probei nr. 31", contactName: "Ana Proba", contactEmail: CONTACT,
  marketRoles: [], operationCodes: [],
});
check("cererea publică e primită", submitted.status === 202, String(submitted.status));

await page.goto(BASE + "/clienti?tab=cereri", { waitUntil: "networkidle" });
await page.waitForTimeout(800);
const row = page.locator("tr", { hasText: NAME });
await row.locator('button:has-text("Creează contul")').click();
await page.waitForURL(/\/clienti\/nou\?cerere=/, { timeout: 5000 }).catch(() => {});
check("„Creează contul” duce la pașii clientului nou", /\/clienti\/nou\?cerere=/.test(page.url()), page.url());
const companiesBefore = await api(page, "GET", "/api/v1/companies");
check("nicio firmă creată doar din apăsare", !(companiesBefore.json ?? []).some((c) => c.name === NAME));

// ---------------------------------------------------------------- (2) răspunsurile sunt puse
await page.waitForSelector("#nc-cui", { timeout: 10000 });
check("CUI-ul din cerere", (await page.inputValue("#nc-cui")) === CUI, await page.inputValue("#nc-cui"));
check("numele din cerere", (await page.inputValue("#nc-name")) === NAME);
check("cuprinsul are cei patru pași", (await page.locator('nav ol li').count()) === 4);
check("spune că vine din cerere", (await text()).includes(`Din cererea de cont a firmei ${NAME}`));
await shot(page, "31_pas1");

// ---------------------------------------------------------------- (3) CUI greșit oprește pasul 1
await page.fill("#nc-cui", "RO12345678");
await button("Continuă: ce face").click();
await page.waitForTimeout(300);
check("CUI greșit: rămâne pe Firma, cu mesaj", (await page.locator("#nc-cui-error").count()) === 1 && (await page.locator("#nc-name").count()) === 1);
await page.fill("#nc-cui", CUI);
await page.selectOption("#nc-county", "Bihor");
await page.fill("#nc-city", "Sântandrei");
await button("Continuă: ce face").click();
await page.waitForTimeout(300);
check("pasul 2: tipul din cerere e ales", await page.locator('input[name="nc-type"][value="GENERATOR"]').isChecked());
await button("Continuă: abonamentul").click();
await page.waitForTimeout(1200);

// ---------------------------------------------------------------- (4)(5) abonamentul
const step3 = await text();
check("pachetele au prețul din grilă", step3.includes("99") && step3.includes("149"), "");
check("prima factură: 99 + 290 = 389 lei", /Total\s*389 lei/.test(step3), step3.match(/Total[^N]*/)?.[0]);
check("adresa firmei vine de la pasul 1", step3.includes("Str. Probei nr. 31, Sântandrei, Bihor"));
check("email pentru facturi din cerere", (await page.inputValue("#nc-billing-email")) === CONTACT);
check("toate verificările bifate", (await page.locator('[data-testid="new-client-checks"] li[data-ok="true"]').count()) === 3);
await shot(page, "31_pas3");

// Adresa ștearsă de la pasul 1 → abonamentul nu mai trece.
await page.locator("nav ol li button").first().click();
await page.selectOption("#nc-county", "");
await page.locator("nav ol li button").nth(2).click();
await page.waitForTimeout(300);
await button("Continuă: administratorul").click();
await page.waitForTimeout(300);
check("fără județ, pasul 3 nu trece", (await text()).includes("Completează ce e bifat cu roșu"));
await card("nc-billing-address", "other").click();
await page.selectOption("#nc-billing-county", "Bihor");
await page.fill("#nc-billing-city", "Oradea");
await page.fill("#nc-billing-address-line", "Str. Facturii nr. 1");
await button("Continuă: administratorul").click();
await page.waitForTimeout(300);

// ---------------------------------------------------------------- (6) administratorul și finalul
check("pasul 4: emailul din cerere", (await page.inputValue("#nc-admin-email")) === CONTACT);
await button("Creează clientul").click();
await page.waitForSelector('[data-testid="new-client-done"]', { timeout: 10000 }).catch(() => {});
const done = await text();
check("rezumat: firma e gata", done.includes(`${NAME} e gata`));
check("rezumat: abonament și prima factură", done.includes("Abonament Generator, prima factură 389 lei"));
// Fără SMTP (CI), contul se creează dar mailul nu pleacă: rezumatul spune atunci „n-a putut fi trimis”.
// Oricare din cele două, dar cu emailul în el — „Nimeni invitat încă.” tot cade.
check(
  "rezumat: invitația",
  done.includes(`Invitație trimisă la ${CONTACT}`) || done.includes(`Contul pentru ${CONTACT} e creat`),
);
await shot(page, "31_gata");

const companies = await api(page, "GET", "/api/v1/companies");
const created = (companies.json ?? []).find((c) => c.name === NAME);
check("firma există", !!created);
if (created) {
  const sub = await api(page, "GET", `/api/v1/subscriptions/company/${created.id}`);
  check("abonamentul are adresa aleasă", sub.json?.billingCounty === "Bihor" && sub.json?.billingCity === "Oradea", JSON.stringify(sub.json?.billingCity));
  const overview = await api(page, "GET", "/api/v1/companies/overview");
  const mine = (overview.json ?? []).find((o) => o.companyId === created.id);
  check("un utilizator invitat", mine?.userCount === 1, String(mine?.userCount));
}
const requests = await api(page, "GET", "/api/v1/account-requests");
check("cererea e aprobată", (requests.json ?? []).find((r) => r.companyName === NAME)?.status === "APPROVED");

// ---------------------------------------------------------------- (7) invitația refuzată nu lasă firma
const OTHER = `Proba 31 Refuz ${RUN}`;
await page.goto(BASE + "/clienti/nou", { waitUntil: "networkidle" });
await page.fill("#nc-cui", validCui());
await page.fill("#nc-name", OTHER);
await button("Continuă: ce face").click();
await button("Continuă: abonamentul").click();
await button("Salvează fără abonament").click();
await page.waitForTimeout(300);
await page.fill("#nc-admin-email", "admin@demo.ro");
await button("Creează clientul").click();
await page.waitForTimeout(1500);
check("eroarea se vede pe pagină", (await page.locator('main [role="alert"]').count()) === 1, await text('main [role="alert"]'));
const after = await api(page, "GET", "/api/v1/companies");
check("firma nu a rămas în urmă", !(after.json ?? []).some((c) => c.name === OTHER));
page.problems = page.problems.filter((p) => !/422/.test(p));

// ---------------------------------------------------------------- (8) tasta N
await page.goto(BASE + "/clienti", { waitUntil: "networkidle" });
await page.mouse.click(5, 5);
await page.keyboard.press("n");
await page.waitForTimeout(500);
check("tasta N duce la /clienti/nou", new URL(page.url()).pathname === "/clienti/nou", page.url());

// ---------------------------------------------------------------- (9) 375px
const phone = await newPage(browser, { width: 375, height: 800 });
await login(phone, "platform");
await phone.goto(BASE + "/clienti/nou", { waitUntil: "networkidle" });
await phone.waitForSelector("#nc-cui");
const widths = await phone.evaluate(() => [document.documentElement.scrollWidth, document.documentElement.clientWidth]);
check("375px fără derulare laterală", widths[0] <= widths[1], widths.join("/"));
await shot(phone, "31_telefon");

// ---------------------------------------------------------------- (10) administratorul firmei
const admin = await newPage(browser, { width: 1440, height: 900 });
await login(admin, "admin");
const denied = await api(admin, "POST", "/api/v1/companies/onboard", {
  company: { name: "Nu", cui: validCui(), type: "GENERATOR", afmObligation: false },
  accountRequestId: null, subscription: null, admin: null,
});
check("administratorul firmei primește 403", denied.status === 403, String(denied.status));
admin.problems = admin.problems.filter((p) => !/403/.test(p));

for (const p of [page, phone, admin]) {
  for (const problem of p.problems) {
    console.log(`  FAIL ${problem}`);
    fails++;
  }
}
await browser.close();
console.log(fails === 0 ? "\n✓ proba 31 trece" : `\n✗ proba 31: ${fails} căderi`);
process.exit(fails === 0 ? 0 : 1);
