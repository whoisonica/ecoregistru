// Proba 43 (20.09.2026): cele două fundături de pe ecranele generatorului.
//
// 1. O eroare de reţea lăsa ecranul **mort**: `retry: 1` pe TanStack Query, apoi o propoziţie roşie
//    şi nimic de apăsat. Omul nu avea de unde şti că F5 ajută — pagina nu spunea că mai e ceva de
//    încercat. Acum e un buton, şi proba cere ca el să chiar reîncarce, nu doar să existe.
// 2. Fişa de partener are patru paşi, iar Escape (sau clic pe fundal) ştergea tot, fără să întrebe.
//
// Eroarea se face cu `page.route(... abort)`, nu aşteptând una adevărată: aşa proba e repetabilă şi
// nu depinde de starea serverului.
import { launch, newPage, login, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
let fails = 0;
const check = (n, ok, d = "") => { console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`); if (!ok) fails++; };
const dialog = 'div[role="dialog"][aria-modal="true"]';

await login(page, "admin");

// ─── 1. Ecranul căzut are o ieşire ─────────────────────────────────────────────────────────────
await page.route("**/api/v1/partners**", (route) => route.abort());
await page.goto(BASE + "/parteneri", { waitUntil: "networkidle" });
await page.waitForTimeout(1200);

const errorBox = page.locator('main [role="alert"]:has-text("Nu am putut încărca")');
check("ecranul spune că n-a putut încărca", (await errorBox.count()) > 0);

const retry = page.locator('main button:visible:has-text("Încearcă din nou")');
check("şi are un buton de reîncercare, nu doar o propoziţie", (await retry.count()) > 0);

// Controlul: cât timp cererea tot cade, tabelul nu apare. Fără asta, „tabelul e acolo” de mai jos
// ar fi putut însemna că pagina nici n-a ascultat de `route`.
const tableWhileBroken = await page.locator("main table tbody tr").count();
check("cât timp cererea cade, nu se vede niciun rând", tableWhileBroken === 0, `rânduri: ${tableWhileBroken}`);

// ─── 2. …şi butonul chiar reîncarcă ────────────────────────────────────────────────────────────
await page.unroute("**/api/v1/partners**");
await retry.first().click();
await page.waitForTimeout(1500);

check("după „Încearcă din nou” ecranul se reface fără F5",
  (await page.locator('main [role="alert"]:has-text("Nu am putut încărca")').count()) === 0);
const rows = await page.locator("main table tbody tr").count();
check("şi lista de parteneri e acolo", rows > 0, `rânduri: ${rows}`);

// ─── 3. Fişa de partener nu se pierde la Escape ────────────────────────────────────────────────
await page.click('main button:visible:has-text("Adaugă partener")');
await page.waitForSelector(dialog, { timeout: 10000 });
await page.fill(`${dialog} input#p-name`, "Probă Fundătură SRL");
await page.keyboard.press("Escape");
await page.waitForTimeout(600);

// `useConfirm` deschide al doilea dialog peste primul; îl recunoaştem după titlul lui.
const confirmBox = page.locator(dialog).filter({ hasText: "Închizi fără să salvezi?" });
check("Escape pe un formular început întreabă întâi", (await confirmBox.count()) > 0);

// Şi răspunsul „nu” chiar păstrează ce s-a scris.
const keep = confirmBox.locator('button:visible:has-text("Anulează")').first();
if ((await keep.count()) > 0) {
  await keep.click();
  await page.waitForTimeout(400);
  const stillThere = await page.inputValue(`${dialog} input#p-name`).catch(() => "");
  check("iar „nu” păstrează ce era în formular", stillThere.includes("Fundătură"), stillThere);
} else {
  check("iar „nu” păstrează ce era în formular", false, "n-am găsit butonul de renunţare la închidere");
}

// Curăţenie: al doilea Escape, confirmat, ca proba să nu lase un dialog deschis.
await page.keyboard.press("Escape");
await page.waitForTimeout(400);
const discard = page.locator('button:visible:has-text("Închide, fără să salvez")').first();
if ((await discard.count()) > 0) await discard.click();
await browser.close();
process.exit(fails === 0 ? 0 : 1);
