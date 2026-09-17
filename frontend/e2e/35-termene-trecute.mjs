// Proba 35: taburile de pe Termene — De făcut · Bifate · Trecute (17.09.2026).
//
// Ce se poate strica: (1) pagina arată iar toate listele una sub alta; (2) tabul nu stă în adresă;
// (3) „Trecute” aduce alt an, azi sau viitorul; (4) cele nebifate de acolo ies roșu „Depășit”, deși
// sunt istorie — seederul pune termene AFM trecute nebifate, ascunse din „De făcut” din 16.09.2026;
// (5) la 375px tabul iese din ecran. Nu lasă nimic în urmă.
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};

await login(page, "admin");
await page.goto(BASE + "/termene", { waitUntil: "networkidle" });
await page.waitForTimeout(1000);

const taburi = await page.$$eval('[role="tab"]', (b) => b.map((x) => x.textContent.replace(/\d+$/, "").trim()));
check("trei taburi, în ordine", JSON.stringify(taburi) === JSON.stringify(["De făcut", "Bifate", "Trecute"]), taburi.join(" · "));
check("implicit doar „De făcut”",
  (await page.$('[data-testid="deadlines-todo"]')) !== null &&
  (await page.$('[data-testid="deadlines-done"]')) === null &&
  (await page.$('[data-testid="deadlines-past"]')) === null);

await page.click('[role="tab"]:text-is("Bifate")');
await page.waitForTimeout(800);
check("„Bifate” stă în adresă", page.url().includes("tab=bifate"), page.url());
check("„Bifate” are selectorul de an și nimic altceva",
  (await page.$('[data-testid="deadlines-done"] #dl-year')) !== null &&
  (await page.$('[data-testid="deadlines-todo"]')) === null);

await page.click('[role="tab"]:has-text("Trecute")');
await page.waitForTimeout(1000);
check("„Trecute” stă în adresă", page.url().includes("tab=trecute"), page.url());
await page.reload({ waitUntil: "networkidle" });
await page.waitForTimeout(1000);
check("și rămâne după reîncărcare", (await page.$('[data-testid="deadlines-past"]')) !== null);

const randuri = await page.$$eval('[data-testid="deadlines-past"] tbody tr', (trs) =>
  trs.map((tr) => [...tr.querySelectorAll("td")].map((td) => td.textContent.trim())).filter((c) => c.length > 2)
);
const azi = new Date();
azi.setHours(0, 0, 0, 0);
const zi = (s) => {
  const m = /(\d{2})\.(\d{2})\.(\d{4})/.exec(s);
  return m ? new Date(+m[3], +m[2] - 1, +m[1]) : null;
};
check("„Trecute” are rânduri (AFM-urile trecute din seeder)", randuri.length > 0, `${randuri.length} rânduri`);
check("doar anul în curs, înainte de azi",
  randuri.every((c) => { const d = zi(c[1]); return d && d.getFullYear() === azi.getFullYear() && d < azi; }),
  randuri.find((c) => { const d = zi(c[1]); return !d || d.getFullYear() !== azi.getFullYear() || d >= azi; })?.[1]);
check("nebifatele spun „Nebifat”, nu „Depășit”, fără zile",
  randuri.some((c) => c[2] === "Nebifat") && randuri.every((c) => c[2] !== "Depășit" && !/depășit de/.test(c[1])),
  randuri.map((c) => c[2]).join(","));
await shot(page, "35-termene-trecute");

const tel = await newPage(browser, { width: 375, height: 800 });
await login(tel, "admin");
await tel.goto(BASE + "/termene?tab=trecute", { waitUntil: "networkidle" });
await tel.waitForTimeout(1000);
const lat = await tel.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
check("375px fără derulare laterală", lat <= 0, `${lat}px`);
check("375px: carduri pe „Trecute”", (await tel.$$('[data-testid="deadlines-past"] [data-testid="deadlines-cards"] li')).length === randuri.length);
await shot(tel, "35-termene-trecute-telefon");

await browser.close();
console.log(fails ? `\n${fails} căderi` : "\nOK");
process.exit(fails ? 1 : 0);
