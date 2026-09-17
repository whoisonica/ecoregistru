// Proba 24: „Primii pași” pe Acasă, pentru un cont nou (16.09.2026).
//
// Ce se poate strica: (1) lista apare peste un cont care are totul — firma demo nu trebuie s-o vadă;
// (2) un pas se bifează din altceva decât din datele lui — firma nouă n-are punct de lucru, apoi are;
// (3) „Ascunde” nu ține la reîncărcare; (4) la 375px lista iese din ecran.
//
// ⚠️ Lasă în urmă câte o firmă „Proba 24 Primii Pași <număr>” cu un punct de lucru la fiecare rulare: o firmă
// refolosită ar avea deja punctul de lucru de data trecută, iar proba ar citi pasul gata.
import { launch, newPage, login, shot, switchCompany, validCui, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
const RUN = Date.now().toString().slice(-8);
const NAME = `Proba 24 Primii Pași ${RUN}`;

async function steps() {
  await page.goto(BASE + "/", { waitUntil: "networkidle" });
  await page.waitForTimeout(1200);
  return page.$$eval('[data-testid="first-steps"] li[data-step]', (lis) =>
    lis.map((li) => ({ id: li.dataset.step, done: li.dataset.done === "true", text: li.textContent }))
  );
}

// (1) Firma demo are date, puncte de lucru, parteneri și mișcări: nicio listă.
await login(page, "admin");
await page.goto(BASE + "/", { waitUntil: "networkidle" });
await page.waitForSelector('[data-testid="next-action"]', { timeout: 15000 });
await page.waitForTimeout(1200);
check("firma demo, care are tot, nu vede „Primii pași”", !(await page.$('[data-testid="first-steps"]')));

// O firmă nouă, fără punct de lucru, fără CAEN și fără persoana desemnată.
await page.goto(BASE + "/login", { waitUntil: "networkidle" });
await page.evaluate(() => localStorage.clear());
await login(page, "platform");
const created = await page.evaluate(async ([name, cui]) => {
  const headers = { Authorization: "Bearer " + localStorage.getItem("eco_token"), "Content-Type": "application/json" };
  const res = await fetch("/api/v1/companies", {
    method: "POST", headers,
    body: JSON.stringify({ name, cui, type: "GENERATOR", afmObligation: false, address: "Cluj-Napoca" }),
  });
  return res.ok ? (await res.json()).id : "HTTP " + res.status;
}, [NAME, validCui()]);
check("firma nouă există", /^[0-9a-f-]{36}$/.test(String(created)), String(created));
await page.reload({ waitUntil: "networkidle" });
check("comutat pe firma nouă", Boolean(await switchCompany(page, new RegExp(RUN))));

let s = await steps();
const byId = Object.fromEntries(s.map((x) => [x.id, x]));
check("patru pași, în ordine", s.map((x) => x.id).join(",") === "company,workPoint,partner,movement", s.map((x) => x.id).join(","));
check("datele firmei: spune ce lipsește, nu și adresa completată",
  byId.company && !byId.company.done && /codul CAEN/.test(byId.company.text) && /persoana desemnată/.test(byId.company.text) && !/adresa/.test(byId.company.text));
check("consultantul/platforma e trimis la Clienți, nu rugat să ne scrie", byId.company && /Deschide Clienți/.test(byId.company.text));
check("punct de lucru: de făcut", byId.workPoint && !byId.workPoint.done);
check("partener și mișcare: de făcut", byId.partner && !byId.partner.done && byId.movement && !byId.movement.done);
check("platforma vede la mișcare și importul din Excel", byId.movement && /Importă din Excel/.test(byId.movement.text));
const progress = await page.textContent('[data-testid="first-steps"]');
check("progresul: 0 din 4", /0 din 4 gata/.test(progress ?? ""));
await shot(page, "24-primii-pasi-1440");

// (2) Un punct de lucru bifează pasul lui și numai pe el.
const wp = await page.evaluate(async (id) => {
  const headers = { Authorization: "Bearer " + localStorage.getItem("eco_token"), "Content-Type": "application/json", "X-Tenant-Id": id };
  const list = await (await fetch("/api/v1/work-points", { headers })).json();
  if (list.length > 0) return "exista";
  const res = await fetch("/api/v1/work-points", { method: "POST", headers, body: JSON.stringify({ name: "Sediu Proba 24", address: "Cluj" }) });
  return res.ok ? "creat" : "HTTP " + res.status;
}, created);
check("punctul de lucru pus prin API", wp === "creat" || wp === "exista", wp);
s = await steps();
check("după punctul de lucru: pasul lui e gata, restul nu",
  s.find((x) => x.id === "workPoint")?.done === true && s.filter((x) => x.done).length === 1, s.map((x) => `${x.id}:${x.done}`).join(" "));

// (4) La 375px nu iese din ecran.
await page.setViewportSize({ width: 375, height: 812 });
await page.waitForTimeout(500);
const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
check("375px: fără derulare laterală", overflow <= 0, `scrollWidth - clientWidth = ${overflow}`);
await shot(page, "24-primii-pasi-375");
await page.setViewportSize({ width: 1440, height: 900 });

// (3) „Ascunde” ține după reîncărcare.
await page.click('[data-testid="first-steps"] button:has-text("Ascunde")');
await page.waitForTimeout(300);
check("„Ascunde” o scoate pe loc", !(await page.$('[data-testid="first-steps"]')));
await page.reload({ waitUntil: "networkidle" });
await page.waitForTimeout(1200);
check("și rămâne ascunsă după reîncărcare", !(await page.$('[data-testid="first-steps"]')));
await page.evaluate((id) => localStorage.removeItem(`wh.firstSteps.hidden.${id}`), created);

await browser.close();
console.log(fails === 0 ? "\n✓ proba 24 trece" : `\n✗ proba 24: ${fails} verificări căzute`);
process.exit(fails === 0 ? 0 : 1);
