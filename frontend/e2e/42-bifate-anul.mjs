// Proba 42 (QA de lansare, 19.09.2026, BUG-044): termenul tocmai bifat se vede pe „Bifate”.
//
// „De făcut” ține următorul termen al fiecărui fel, care stă aproape mereu în anul viitor (15 martie pentru datele
// de acum). „Bifate” pornea pe anul curent și filtra după anul scadenței, deci termenul abia bifat nu era nicăieri,
// iar „Redeschide” nu se găsea fără să schimbi anul. Proba bifează primul termen din anul viitor, îl caută pe „Bifate” fără să
// atingă anul, apoi îl redeschide (prin API, dacă ecranul nu-l arată), ca baza să rămână cum a fost.
import { launch, newPage, login, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
let fails = 0;
const check = (n, ok, d = "") => { console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`); if (!ok) fails++; };
const dialog = 'div[role="dialog"][aria-modal="true"]';

await login(page, "admin");
await page.goto(BASE + "/termene", { waitUntil: "networkidle" });
await page.waitForTimeout(800);
// Un termen din anul viitor: cazul obișnuit (15 martie). Unul lunar, din anul curent, s-ar vedea oricum.
const nextYear = new Date().getFullYear() + 1;
const first = page.locator("main tr:has(button:visible:has-text('Marchează finalizat'))", { hasText: "." + nextYear }).first();
if ((await first.count()) === 0) { console.log("  ——   niciun termen de făcut în " + nextYear + ": proba n-are pe ce rula."); await browser.close(); process.exit(1); }
const label = ((await first.locator("td").first().textContent()) ?? "").trim();
await first.locator("button:visible:has-text('Marchează finalizat')").click();
await page.locator(`${dialog} button:has-text("Marchează finalizat")`).click();
await page.waitForTimeout(800);

await page.click('main button:has-text("Bifate")');
await page.waitForTimeout(800);
const shown = ((await page.textContent('[data-testid="deadlines-done"]')) ?? "").includes(label.slice(0, 20));
check("termenul abia bifat e pe „Bifate”, fără să schimbi anul", shown, label.slice(0, 60));

// Curățenia: redeschis, oricum ar fi ieșit verificarea.
const ids = await page.evaluate(async () => {
  const h = { Authorization: "Bearer " + localStorage.getItem("eco_token") };
  const y = new Date().getFullYear();
  const all = [...await (await fetch(`/api/v1/deadlines?year=${y}`, { headers: h })).json(), ...await (await fetch(`/api/v1/deadlines?year=${y + 1}`, { headers: h })).json()];
  const done = all.filter((d) => d.status === "DONE" && d.completedAt && Date.now() - Date.parse(d.completedAt) < 120000);
  for (const d of done) await fetch(`/api/v1/deadlines/${d.id}/reopen`, { method: "POST", headers: h });
  return done.length;
});
console.log(`  ··   redeschise la curățenie: ${ids}`);

await browser.close();
console.log(fails === 0 ? "\n✓ proba 42 trece" : `\n✗ proba 42: ${fails} verificări căzute`);
process.exit(fails === 0 ? 0 : 1);
