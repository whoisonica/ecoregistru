// Proba 16: importul din Excel (P2.15), pe ecran.
//
// Backendul are regulile probate în `ExcelImportIT`. Aici se probează ce nu vede niciun test de
// backend: că „Importă” rămâne blocat până când **același** fișier a trecut o verificare fără erori,
// că alegerea altui fișier îl blochează la loc, că un fișier stricat își spune mesajul pe ecran, și
// că șablonul chiar se descarcă. Proba nu salvează nimic: verifică doar șablonul gol.
//
// Din 16.09.2026 importul e numai al platformei (îl facem noi, la implementare): administratorul firmei
// nu-l mai vede nici în Setări, nici în paletă, iar adresa scrisă de mână îl duce acasă.
import { launch, newPage, login, shot, switchCompany, BASE } from "./lib.mjs";

const browser = await launch();
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
const XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
const importDisabled = (page) =>
  page.$eval('button:has-text("Importă")', (b) => b.disabled).catch(() => null);

// ══════════════════════════════════════════════ ADMINISTRATORUL FIRMEI
const admin = await newPage(browser);
await login(admin, "admin");
await admin.goto(BASE + "/setari", { waitUntil: "networkidle" });
await admin.waitForTimeout(500);
check("administratorul nu are importul în Setări", !(await admin.$('a[href="/import"]')));
await admin.goto(BASE + "/import", { waitUntil: "networkidle" });
await admin.waitForTimeout(500);
check("adresa /import îl duce acasă", new URL(admin.url()).pathname === "/", admin.url());
check("și nu vede ecranul de import", !(await admin.isVisible("text=Descarcă șablonul")));
await admin.close();

// ══════════════════════════════════════════════ PLATFORMA
const page = await newPage(browser);
await login(page, "platform");
check("platforma comută pe firma demo", Boolean(await switchCompany(page, /Demo/)));
await page.goto(BASE + "/import", { waitUntil: "networkidle" });
await page.waitForTimeout(500);

const h1 = (await page.textContent("h1")) ?? "";
check("ecranul se deschide", /Import din Excel/.test(h1), h1.trim());
// Importul nu stă în meniu: drumul e butonul din Setări, plus paleta Ctrl+K.
await page.goto(BASE + "/setari", { waitUntil: "networkidle" });
await page.waitForTimeout(500);
check("Setări duce la el", Boolean(await page.$('a[href="/import"]')));
await page.goto(BASE + "/import", { waitUntil: "networkidle" });
await page.waitForTimeout(500);
check("„Importă” e blocat fără fișier", (await importDisabled(page)) === true);

const [download] = await Promise.all([
  page.waitForEvent("download", { timeout: 10000 }),
  page.click('button:has-text("Descarcă șablonul")'),
]);
check("șablonul se descarcă", download.suggestedFilename() === "sablon-import-wastehouse.xlsx",
  download.suggestedFilename());

// Un fișier care nu e Excel: mesajul backendului ajunge pe ecran, „Importă” rămâne blocat.
await page.setInputFiles('input[type="file"]', {
  name: "gresit.xlsx", mimeType: XLSX, buffer: Buffer.from("nu e un Excel"),
});
await page.click('button:has-text("Verifică")');
await page.waitForTimeout(1200);
const body = await page.evaluate(() => document.body.innerText);
check("fișierul stricat își spune problema", /nu se poate citi ca Excel/.test(body));
check("și „Importă” rămâne blocat", (await importDisabled(page)) === true);

// Șablonul gol, luat cu sesiunea paginii: o verificare fără erori deblochează „Importă”.
const template = await page.evaluate(async () => {
  const res = await fetch("/api/v1/import/sablon", {
    headers: {
      Authorization: "Bearer " + localStorage.getItem("eco_token"),
      "X-Tenant-Id": localStorage.getItem("eco_tenant") ?? "",
    },
  });
  const bytes = new Uint8Array(await res.arrayBuffer());
  let s = "";
  bytes.forEach((b) => (s += String.fromCharCode(b)));
  return btoa(s);
});
await page.setInputFiles('input[type="file"]', {
  name: "sablon.xlsx", mimeType: XLSX, buffer: Buffer.from(template, "base64"),
});
check("alegerea unui fișier nou îl blochează la loc", (await importDisabled(page)) === true);
await page.click('button:has-text("Verifică")');
await page.waitForTimeout(1200);
check("verificarea fără erori o spune", await page.isVisible("text=Nicio eroare"));
check("și deblochează „Importă”", (await importDisabled(page)) === false);
check("istoricul importurilor e pe ecran", await page.isVisible('[data-testid="import-history"]'));
check("numele fișierului e pe ecran",
  ((await page.textContent('[data-testid="import-file-name"]')) ?? "").includes("sablon.xlsx"));
await shot(page, "import_verificat");

// Singurul 400 așteptat e al fișierului stricat: îl scrie și routerul, și consola browserului.
const unexpected = page.problems.filter((p) =>
  !/HTTP 400\] POST .*\/api\/v1\/import\/verificare/.test(p) &&
  !/\[consolă error\] Failed to load resource: the server responded with a status of 400/.test(p));
const expected400 = page.problems.filter((p) => /status of 400|HTTP 400/.test(p)).length;
check("singurul 400 e al fișierului stricat", expected400 <= 2, expected400 + " intrări");
check("fără erori neașteptate", unexpected.length === 0, unexpected.join(" | "));
await page.close();

// ══════════════════════════════════════════════ CITITORUL
const viewer = await newPage(browser);
await login(viewer, "viewer");
check("cititorul nu vede importul în bară", !(await viewer.$('nav a[href="/import"]')));
await viewer.close();

await browser.close();
console.log(fails === 0 ? "✓ proba 16 trece." : `✗ proba 16: ${fails} verificări căzute`);
process.exit(fails === 0 ? 0 : 1);
