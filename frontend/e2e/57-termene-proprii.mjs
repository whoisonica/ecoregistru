// Proba 57: termenele proprii (V81, Andreea 29.09.2026) — măsurători de zgomot, analize de apă, emisii.
//
// Ce se poate strica: (1) „Adaugă termen” lipsește sau salvează un termen fără nume; (2) termenul nu apare în
// „De făcut” cu numele lui și cu repetarea; (3) tasta N nu deschide formularul pe Termene; (4) modificarea nu
// ajunge pe rând; (5) bifat, un termen anual nu-și aduce apariția de peste un an; (6) ștergerea întreabă și ia
// rândul; (7) Acasă nu-l arată pe coloana lunii; (8) vizualizatorul poate adăuga; (9) la 375px pagina se lățește.
// ⚠️ Lasă în urmă, pe firma demo, termenul bifat „Zgomot P57 <număr>” și unul „Apă P57 <număr>” deschis.
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
const RUN = Date.now().toString().slice(-6);
const iso = (days) => {
  const d = new Date();
  d.setDate(d.getDate() + days);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
};
const ro = (s) => s.split("-").reverse().join(".");
const row = (text) => page.locator('[data-testid="deadlines-todo"] tbody tr', { hasText: text });
const dialog = () => page.locator('[role="dialog"]');

await login(page, "admin");
await page.goto(BASE + "/termene", { waitUntil: "networkidle" });
await page.waitForTimeout(800);

// (1) Formularul, gol, nu salvează.
await page.click('[data-testid="custom-deadline-add"]');
await dialog().waitFor();
check("„Adaugă termen” deschide „Termen propriu”", (await dialog().innerText()).includes("Termen propriu"));
await page.click('[data-testid="custom-deadline-save"]');
await page.waitForTimeout(300);
check("fără nume nu se salvează", (await dialog().innerText()).includes("Scrie ce ai de făcut."));
await shot(page, "57-termen-propriu-formular");

// (2) Sugestia pune numele; anual, cu detalii.
await dialog().locator("button", { hasText: "Măsurători de zgomot" }).click();
check("sugestia completează numele", (await page.inputValue("#cd-title")) === "Măsurători de zgomot");
await page.fill("#cd-title", `Zgomot P57 ${RUN}`);
await page.fill("#cd-date", iso(12));
await dialog().locator("label", { hasText: "Anual" }).click();
await page.fill("#cd-details", "Laboratorul Eco Test");
await page.click('[data-testid="custom-deadline-save"]');
await dialog().waitFor({ state: "detached" });
await page.waitForTimeout(800);
const r = await row(`Zgomot P57 ${RUN}`).innerText().catch(() => "");
check("apare în „De făcut” cu numele, data și repetarea",
  r.includes(ro(iso(12))) && r.includes("Se repetă anual · Laboratorul Eco Test"), r.replace(/\s+/g, " "));
await shot(page, "57-termen-propriu-lista");

// (3) Tasta N.
await page.keyboard.press("n");
await page.waitForTimeout(300);
check("N deschide formularul", (await dialog().count()) === 1);
await page.keyboard.press("Escape");
await dialog().waitFor({ state: "detached" }).catch(() => {});

// (4) Modificarea, din meniul rândului.
await row(`Zgomot P57 ${RUN}`).getByRole("button", { name: "Mai multe acțiuni" }).click();
await page.getByRole("menuitem", { name: "Editează" }).click();
await dialog().waitFor();
await page.fill("#cd-title", `Zgomot hală P57 ${RUN}`);
await page.click('[data-testid="custom-deadline-save"]');
await dialog().waitFor({ state: "detached" });
await page.waitForTimeout(800);
check("modificarea ajunge pe rând", (await row(`Zgomot hală P57 ${RUN}`).count()) === 1);

// (5) Bifat, vine apariția de peste un an.
await row(`Zgomot hală P57 ${RUN}`).getByRole("button", { name: "Marchează finalizat" }).click();
await dialog().waitFor();
await dialog().getByRole("button", { name: "Marchează finalizat" }).click();
await dialog().waitFor({ state: "detached" });
await page.waitForTimeout(1000);
const next = new Date();
next.setDate(next.getDate() + 12);
next.setFullYear(next.getFullYear() + 1);
const nextIso = `${next.getFullYear()}-${String(next.getMonth() + 1).padStart(2, "0")}-${String(next.getDate()).padStart(2, "0")}`;
const r2 = await row(`Zgomot hală P57 ${RUN}`).innerText().catch(() => "");
check("bifat, apare cel de peste un an", r2.includes(ro(nextIso)), r2.replace(/\s+/g, " "));

// (6) Ștergerea întreabă și ia rândul.
await row(`Zgomot hală P57 ${RUN}`).getByRole("button", { name: "Mai multe acțiuni" }).click();
await page.getByRole("menuitem", { name: "Șterge" }).click();
await dialog().waitFor();
check("întreabă cu numele termenului", (await dialog().innerText()).includes(`Zgomot hală P57 ${RUN}`));
await dialog().getByRole("button", { name: "Șterge" }).click();
await page.waitForTimeout(1000);
check("rândul pleacă din „De făcut”", (await row(`Zgomot hală P57 ${RUN}`).count()) === 0);

// Termenele din lege n-au meniul.
const legalMenus = await page.locator('[data-testid="deadlines-todo"] tbody tr', { hasText: "15 martie" })
  .getByRole("button", { name: "Mai multe acțiuni" }).count();
check("termenele din lege nu se modifică și nu se șterg", legalMenus === 0);

// (7) Acasă îl arată pe coloana lunii.
await page.click('[data-testid="custom-deadline-add"]');
await dialog().waitFor();
await page.fill("#cd-title", `Apă P57 ${RUN}`);
await page.fill("#cd-date", iso(20));
await page.click('[data-testid="custom-deadline-save"]');
await dialog().waitFor({ state: "detached" });
const scroll = await page.evaluate(() => {
  const m = document.querySelector("main#continut");
  return m ? m.scrollHeight - m.clientHeight : -1;
});
check("1440×900: Termene fără derulare", scroll <= 0, `${scroll}px`);
await page.goto(BASE + "/", { waitUntil: "networkidle" });
await page.waitForTimeout(1200);
check("Acasă îl arată cu numele lui", (await page.locator("main").innerText()).includes(`Apă P57 ${RUN}`));
await shot(page, "57-termen-propriu-acasa");

// (8) Vizualizatorul nu adaugă.
const viewer = await newPage(browser, { width: 1440, height: 900 });
await login(viewer, "viewer");
await viewer.goto(BASE + "/termene", { waitUntil: "networkidle" });
await viewer.waitForTimeout(800);
check("vizualizatorul n-are „Adaugă termen”", (await viewer.$('[data-testid="custom-deadline-add"]')) === null);

// (9) Telefonul.
const tel = await newPage(browser, { width: 375, height: 800 });
await login(tel, "admin");
await tel.goto(BASE + "/termene", { waitUntil: "networkidle" });
await tel.waitForTimeout(1000);
const lat = await tel.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
check("375px fără derulare laterală", lat <= 0, `${lat}px`);
check("375px: cardul are numele", (await tel.locator('[data-testid="deadlines-cards"] li', { hasText: `Apă P57 ${RUN}` }).count()) === 1);
await shot(tel, "57-termen-propriu-telefon");
await tel.click('[data-testid="custom-deadline-add"]');
await tel.locator('[role="dialog"]').waitFor();
await tel.waitForTimeout(600);
const latDlg = await tel.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
check("375px: formularul nu lățește pagina", latDlg <= 0, `${latDlg}px`);
await shot(tel, "57-termen-propriu-formular-telefon");

await browser.close();
console.log(fails ? `\n${fails} FAIL` : "\nToate OK");
process.exit(fails ? 1 : 0);
