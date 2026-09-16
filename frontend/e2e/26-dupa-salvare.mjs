// Proba 26: ce urmează după o mișcare nouă (17.09.2026, regula 12 a formularelor).
//
// (1) O predare nouă, salvată prin ecran, nu mai închide doar dialogul cu un mesaj de patru secunde: apare
// „Predarea e în evidență”, cu bonul citit din răspunsul serverului și cu documentele pe care rândul le poate
// tipări (aceleași reguli ca meniul „⋯”: Anexa 3 și avizul pe o predare nepericuloasă către un partener).
// (2) „Încă una la fel” pornește formularul cu alegerile ei — codul, partenerul — dar fără cantitate și fără
// document; „La fel ca data trecută” nu se mai oferă acolo. (3) Editarea păstrează mesajul scurt, fără ecran.
// (4) Formularul deschis prin `?nou=1` într-un tab proaspăt pornește cu punctul de lucru ales.
//
// Șterge la final mișcarea pe care a creat-o.
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
await login(page, "admin");
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};
const api = (path, opts = {}) =>
  page.evaluate(
    async ([path, opts]) => {
      const r = await fetch(path, {
        ...opts,
        headers: { Authorization: "Bearer " + localStorage.getItem("eco_token"), "Content-Type": "application/json" },
      });
      return r.status === 204 ? null : r.json();
    },
    [path, opts]
  );

const DOC = "PROBA26-" + Date.now().toString().slice(-6);
const AN = new Date().getFullYear();
const partners = await api("/api/v1/partners");
const authorized = partners.find((p) => p.active && p.authorizationNumber && p.type !== "GENERATOR");
check("există un destinatar autorizat", Boolean(authorized), authorized?.name ?? "niciunul");

// ------------------------------------------------------------ (1) PREDAREA NOUĂ
await page.goto(BASE + `/generare?luna=${AN}`, { waitUntil: "networkidle" });
await page.waitForTimeout(700);
await page.click('button:has-text("Deșeuri proprii")');
await page.waitForTimeout(600);
await page.click("#mv-code");
await page.keyboard.type("15 01 01");
await page.waitForTimeout(900);
await page.locator('[role="listbox"] [role="option"]', { hasText: "15 01 01" }).first().click();
await page.waitForTimeout(300);
await page.fill("#mv-qty", "33");
await page.locator('label:has(input[name="mv-fate"][value="RECOVERED"])').click();
await page.waitForTimeout(200);
await page.selectOption("#mv-code-rd", { index: 1 });
await page.locator('label:has(input[name="mv-destination"][value="Vr"])').click();
await page.selectOption("#mv-partner", authorized.id);
await page.fill("#mv-doc", DOC);
await page.click('button[type="submit"][form="movement-form"]');
await page.waitForSelector('[data-testid="movement-saved"]', { timeout: 8000 }).catch(() => {});
await page.waitForTimeout(600); // animația de deschidere, ca s-o prindă captura întreagă

const saved = await page.evaluate(() => {
  const d = document.querySelector('[data-testid="movement-saved"]')?.closest('[role="dialog"]');
  return d
    ? {
        titlu: d.querySelector("h2")?.textContent.trim(),
        text: d.textContent.replace(/\s+/g, " "),
        formular: Boolean(document.querySelector("#movement-form")),
      }
    : null;
});
check("după salvare apare ecranul „Ce urmează”", Boolean(saved), saved?.titlu ?? "—");
check("titlul spune că predarea e în evidență", saved?.titlu === "Predarea e în evidență", saved?.titlu);
check("formularul s-a închis", saved?.formular === false);
check("bonul arată codul, cantitatea și destinatarul",
  /15 01 01/.test(saved?.text ?? "") && /33 kg/.test(saved?.text ?? "") && (saved?.text ?? "").includes(authorized.name));
check("oferă Anexa 3 și avizul", /Tipărește Anexa 3/.test(saved?.text ?? "") && /avizul de însoțire/.test(saved?.text ?? ""));
check("nu oferă Anexa 2 pe un deșeu nepericulos", !/Anexa 2/.test(saved?.text ?? ""));
await shot(page, "26_dupa_salvare");

// ------------------------------------------------------------ (2) ÎNCĂ UNA LA FEL
await page.click('button:has-text("Încă una la fel")');
await page.waitForTimeout(800);
const again = await page.evaluate(() => ({
  formular: Boolean(document.querySelector("#movement-form")),
  titlu: document.querySelector('div[role="dialog"] h2')?.textContent.trim(),
  cod: document.querySelector("#mv-code")?.textContent?.trim(),
  cantitate: document.querySelector("#mv-qty")?.value,
  document: document.querySelector("#mv-doc")?.value,
  partener: document.querySelector("#mv-partner")?.value,
  laFel: [...document.querySelectorAll("button")].some((b) => /La fel ca data trecută/.test(b.textContent)),
}));
check("„Încă una la fel” deschide formularul", again.formular, again.titlu);
check("… cu codul ales", /15 01 01/.test(again.cod ?? ""), again.cod);
check("… cu destinatarul ales", again.partener === authorized.id);
check("… fără cantitate", again.cantitate === "", JSON.stringify(again.cantitate));
check("… fără document", again.document === "", JSON.stringify(again.document));
check("… și fără butonul „La fel ca data trecută”", !again.laFel);
await page.keyboard.press("Escape");
await page.waitForTimeout(300);
const discard = page.locator('button:has-text("Renunță"), button:has-text("Închide fără")').first();
if ((await discard.count()) > 0) await discard.click().catch(() => {});
await page.waitForTimeout(300);

// ------------------------------------------------------------ (3) EDITAREA RĂMÂNE CU MESAJUL SCURT
const created = (await api(`/api/v1/movements?search=${DOC}&size=5`)).content.find((m) => m.documentReference === DOC);
check("mișcarea s-a salvat o singură dată", Boolean(created));
if (created) {
  await page.goto(BASE + `/generare?luna=${AN}&miscare=${created.id}`, { waitUntil: "networkidle" });
  await page.waitForTimeout(1200);
  await page.click('button[type="submit"][form="movement-form"]');
  await page.waitForTimeout(1200);
  const afterEdit = await page.evaluate(() => ({
    ecran: Boolean(document.querySelector('[data-testid="movement-saved"]')),
    mesaj: /Mișcare actualizată/.test(document.body.textContent),
  }));
  check("la editare nu apare ecranul „Ce urmează”", !afterEdit.ecran);
  check("… ci mesajul scurt de dinainte", afterEdit.mesaj);
  await api(`/api/v1/movements/${created.id}`, { method: "DELETE" });
}

// ------------------------------------------------------------ (4) ?nou=1 ÎNTR-UN TAB PROASPĂT
// Butonul „+” de pe telefon și „Adaugă deșeuri” din panou deschid formularul prin adresă, înainte să
// sosească punctele de lucru. Până pe 17.09.2026 rubrica rămânea goală și salvarea cădea pe ea.
const fresh = await newPage(browser, { width: 375, height: 812 });
await login(fresh, "admin");
await fresh.goto(BASE + "/generare?nou=1");
await fresh.waitForSelector("#mv-wp", { timeout: 8000 }).catch(() => {});
await fresh.waitForTimeout(1200);
const wp = await fresh.$eval("#mv-wp", (s) => s.value).catch(() => null);
check("formularul deschis din adresă are punctul de lucru ales", Boolean(wp), JSON.stringify(wp));
await fresh.close();

console.log("");
if (fails === 0) console.log("✓ proba 26 trece.");
else console.log(`✗ proba 26: ${fails} verificări au căzut.`);
await browser.close();
process.exit(fails === 0 ? 0 : 1);
