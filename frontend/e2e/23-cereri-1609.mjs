// Proba 23: cererile proprietarului din 16.09.2026, seara.
//
// (1) Pe „Generare” nu mai e selectul „Operațiune” cu o singură opțiune; lista de coduri se citește după
// denumire; scopul tratării (V/E) se vede din ce pleacă; destinația are patru opțiuni și e obligatorie;
// un destinatar fără autorizație de mediu e refuzat. (2) Partenerul colector cere autorizația.
// (3) La firme, „Economia circulară” a ieșit din configurare, iar CUI-ul are butonul ANAF.
// (4) Ambalaje: generatorul vede Anexa 3 cu ieșirile.
//
// ⚠️ Lasă în urmă un partener „Proba 23 Fără Autorizație”: fapta e chiar ce se probează.
import { launch, newPage, login, shot, switchCompany, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};

await login(page, "admin");
const AN = new Date().getFullYear();

// Un destinatar fără număr de autorizație. Generatorul e singurul tip pe care serverul îl primește așa.
const noAuth = await page.evaluate(async () => {
  const headers = { Authorization: "Bearer " + localStorage.getItem("eco_token"), "Content-Type": "application/json" };
  const list = await (await fetch("/api/v1/partners", { headers })).json();
  const found = list.find((p) => p.name === "Proba 23 Fără Autorizație");
  if (found) return found.name;
  const res = await fetch("/api/v1/partners", {
    method: "POST", headers,
    body: JSON.stringify({ name: "Proba 23 Fără Autorizație", type: "GENERATOR", client: true, supplier: false, carrier: false }),
  });
  return res.ok ? (await res.json()).name : null;
});
check("partenerul fără autorizație există", Boolean(noAuth));

// ------------------------------------------------------------ (1) FORMULARUL DE PE GENERARE
await page.goto(BASE + `/generare?luna=${AN}`, { waitUntil: "networkidle" });
await page.waitForTimeout(700);
await page.click('button:has-text("Deșeuri proprii")');
await page.waitForTimeout(600);
check("fără selectul „Operațiune”", !(await page.$("#mv-op")));

await page.click("#mv-code");
await page.waitForTimeout(300);
await page.keyboard.type("hartie");
await page.waitForTimeout(900);
const options = await page.$$eval('[role="listbox"] [role="option"]', (os) => os.map((o) => o.textContent.trim()));
check("lista începe cu denumirea, codul lângă ea", options.length > 0 && /^[A-ZĂÂÎȘȚ]/.test(options[0]) && /\d\d \d\d \d\d/.test(options[0]),
  options.slice(0, 2).join(" | "));
const paper = page.locator('[role="listbox"] [role="option"]', { hasText: "15 01 01" }).first();
if ((await paper.count()) > 0) await paper.click();
else await page.locator('[role="listbox"] [role="option"]').first().click();
await page.waitForTimeout(300);

const destinations = await page.$$eval('#mv-destination input[name="mv-destination"]', (os) => os.map((o) => o.value).filter(Boolean));
check("destinația are patru opțiuni: DO, I, Vr, A", destinations.join(",") === "DO,I,Vr,A", destinations.join(","));

await page.fill("#mv-qty", "12");
await page.locator('label:has(input[name="mv-fate"])').nth(1).click(); // spre eliminare
await page.waitForTimeout(200);
const purposeE = await page.textContent('[data-testid="mv-purpose"]').catch(() => null);
check("eliminarea arată scopul E", (purposeE ?? "").startsWith("E"), purposeE ?? "—");
await page.locator('label:has(input[name="mv-fate"])').nth(0).click(); // spre valorificare
await page.waitForTimeout(200);
const purposeV = await page.textContent('[data-testid="mv-purpose"]').catch(() => null);
check("valorificarea arată scopul V", (purposeV ?? "").startsWith("V"), purposeV ?? "—");

const partnerValue = await page.$eval("#mv-partner", (s, name) =>
  [...s.options].find((o) => o.textContent.startsWith(name))?.value ?? "", noAuth);
if (partnerValue) await page.selectOption("#mv-partner", partnerValue);
await page.waitForTimeout(200);
// Și cu destinatar ales, scopul rămâne V: se tipărește după unde pleacă deșeul.
const purposeHanded = await page.textContent('[data-testid="mv-purpose"]').catch(() => null);
check("la predare scopul rămâne V", (purposeHanded ?? "").startsWith("V"), purposeHanded ?? "—");
await page.click('button[type="submit"][form="movement-form"]');
await page.waitForTimeout(700);
const errors = await page.$$eval('div[role="dialog"] p[data-field-error]', (p) => p.map((x) => x.id + ": " + x.textContent.trim()));
check("destinația goală e marcată", errors.some((e) => e.startsWith("mv-destination-err")), errors.join(" | "));
check("destinatarul fără autorizație e refuzat", errors.some((e) => e.startsWith("mv-partner-err") && e.includes("autorizației")), errors.join(" | "));
await shot(page, "23_formular_generare");
await page.keyboard.press("Escape");
await page.waitForTimeout(300);
const discard = page.locator('button:has-text("Renunță"), button:has-text("Închide fără")').first();
if ((await discard.count()) > 0) await discard.click().catch(() => {});

// ------------------------------------------------------------ (2) PARTENERUL COLECTOR
await page.goto(BASE + "/parteneri?nou=1", { waitUntil: "networkidle" });
await page.waitForTimeout(800);
await page.fill("#p-name", "Proba 23 Colector");
// Trei pași (17.09.2026): Continuă, Continuă, Salvează — colectorul e tipul implicit.
for (let i = 0; i < 3; i++) {
  await page.click('div[role="dialog"] button[type="submit"]');
  await page.waitForTimeout(400);
}
const authErr = await page.$("#p-auth-number-err");
check("colectorul fără autorizație nu se salvează", Boolean(authErr));
check("butonul ANAF e lângă CUI", (await page.locator('div[role="dialog"] button:has-text("Completează din ANAF")').count()) === 1);

// ------------------------------------------------------------ (3) FIRME, CA PLATFORMĂ
const ctx2 = await newPage(browser, { width: 1440, height: 900 });
await login(ctx2, "platform");
await ctx2.goto(BASE + "/clienti", { waitUntil: "networkidle" });
await ctx2.waitForTimeout(800);
const add = ctx2.locator('button:has-text("Client nou")').first();
if ((await add.count()) > 0) {
  await add.click();
  await ctx2.waitForTimeout(600);
  // F-C: clientul nou e o pagină în pași, nu un dialog.
  const dialog = await ctx2.textContent("main");
  check("fără „Economia circulară” la firmă nouă", !dialog.includes("Economia circulară"));
  check("CUI-ul firmei are butonul ANAF", (await ctx2.locator('main button:has-text("Completează din ANAF")').count()) === 1);
  await shot(ctx2, "23_firma_noua");
} else {
  check("ecranul de firme are „Client nou”", false);
}

// ------------------------------------------------------------ (4) AMBALAJE LA GENERATOR
await ctx2.goto(BASE + "/", { waitUntil: "networkidle" });
await ctx2.waitForTimeout(600);
const switched = await switchCompany(ctx2, /Proba Automata/i);
check("comutat pe generatorul de probă", Boolean(switched), switched ?? "");
await ctx2.goto(BASE + `/generare?tab=ambalaje&luna=${AN}`, { waitUntil: "networkidle" });
await ctx2.waitForTimeout(1200);
const a3 = await ctx2.$("#anexa-3");
const a3Text = a3 ? await a3.textContent() : "";
check("generatorul vede Anexa 3", Boolean(a3));
check("… cu ieșirile, fără preluări", a3Text.includes("predate") && !a3Text.includes("Cantitatea preluată"), a3Text.slice(0, 90));
await shot(ctx2, "23_ambalaje_generator");

await browser.close();
console.log(fails === 0 ? "\n23 — OK" : `\n23 — ${fails} FAIL`);
process.exit(fails === 0 ? 0 : 1);
