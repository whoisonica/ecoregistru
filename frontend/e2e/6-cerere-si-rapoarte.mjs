// Proba 6: cele două capete ale primului contact cu produsul — formularul public `/cerere-cont` și
// inboxul care îl citește — plus drumul de la un raport la mișcarea vinovată.
//
// De ce împreună: sunt feliile din 07.09.2026 care au transformat trei ecrane de raport în unelte
// și au dat formularului public tratamentul pe care îl are de mult formularul de mișcare (marcaje
// de obligatoriu, erori pe rubrici, derulare la prima greșită).
//
// ⚠️ Formularul public **scrie** o cerere în baza de dev, cu un CUI unic la fiecare rulare. Nu
// curăță după el: aprobarea ar crea o firmă, iar aplicația n-are ștergere de firmă. Rândurile
// rămân în inboxul de dev, ceea ce e chiar util — ecranul are pe ce se uita.
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};

// ══════════════════════════════════════════════ FORMULARUL PUBLIC, FĂRĂ AUTENTIFICARE
await page.goto(BASE + "/cerere-cont", { waitUntil: "networkidle" });
await page.waitForTimeout(400);

const brand = await page.textContent("header");
check("pagina spune al cui e", /EcoRegistru/.test(brand ?? ""), (brand ?? "").trim().slice(0, 40));

const steps = await page.$$eval("ol li", (li) => li.length);
check("scrie ce urmează după trimitere", steps >= 3, steps + " pași");

// Cele trei rubrici obligatorii se văd **înainte** de a apăsa Trimite, nu după.
const marked = await page.$$eval("label", (labels) =>
  labels.filter((l) => l.textContent.includes("*")).map((l) => l.getAttribute("for"))
);
check(
  "cele trei rubrici obligatorii sunt marcate",
  ["ar-name", "ar-cui", "ar-contact-email"].every((id) => marked.includes(id)),
  marked.join(", ")
);

// Cele 28 de bife R/D nu mai stau deschise în fața cuiva care n-a auzit de R13.
const codesHiddenAtFirst = (await page.$$('input[type="checkbox"][class*="rounded"]')).length;
const hasUnknownEscape = (await page.textContent("form")).includes("Nu știu");
check("lista de coduri R/D e pliată, cu o ieșire onorabilă", hasUnknownEscape, "„Nu știu” există");
check("codurile nu sunt bifate din start", codesHiddenAtFirst < 28, codesHiddenAtFirst + " bife vizibile");

// Deschiderea listei o arată; „Nu știu” o ascunde **și** golește ce s-a bifat, ca să nu rămână
// în urmă răspunsuri pe care omul crede că le-a retras.
await page.click('input[name="ar-codes-mode"] >> nth=1');
await page.waitForTimeout(250);
const codesShown = (await page.$$('input[type="checkbox"]')).length;
check("alegerea „le aleg acum” deschide lista", codesShown > 20, codesShown + " bife");

// ---------------------------------------------- TRIMITEREA GOALĂ MARCHEAZĂ RUBRICILE
// Defectul nr. 4 din proba de pe 07.09 („bannerul marca nimic”), rămas pe pagina asta: apăsai
// butonul din capătul a șase secțiuni și părea că nu face nimic.
await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
await page.click('button[type="submit"]');
await page.waitForTimeout(600);

const invalidCount = (await page.$$('[data-invalid="true"]')).length;
check("trimiterea goală marchează rubricile", invalidCount === 3, invalidCount + " rubrici marcate");

const errorTexts = await page.$$eval("p.text-red-600", (p) => p.map((x) => x.textContent.trim()));
check("fiecare rubrică își spune motivul", errorTexts.length === 3, errorTexts.join(" | "));

// Derularea la prima greșită: butonul stă la capătul paginii, marcajul e în capul ei.
const focusedId = await page.evaluate(() => document.activeElement?.id ?? "");
check("focusul sare la prima rubrică greșită", focusedId === "ar-name", focusedId || "(niciunul)");

// ---------------------------------------------- CUI-UL E VERIFICAT AICI, NU LA APROBARE
await page.fill("#ar-name", "Proba Automata SRL");
await page.fill("#ar-cui", "nu-e-cui");
await page.fill("#ar-contact-email", "fara-arond");
await page.click('button[type="submit"]');
await page.waitForTimeout(500);
const formText = await page.textContent("form");
check("CUI-ul stricat se respinge cu forma cerută", /2–10 cifre/.test(formText), "");
check("emailul incomplet se respinge", /nu pare complet/.test(formText), "");

// ---------------------------------------------- TRIMITEREA CARE MERGE
const cui = "RO" + String(Date.now()).slice(-8);
const email = "proba" + String(Date.now()).slice(-6) + "@example.ro";
await page.fill("#ar-cui", cui);
await page.fill("#ar-contact-email", email);
// Exact cele opt rubrici pe care tabelul inboxului le ascundea. Se completează aici ca dialogul de
// mai jos să aibă ce citi: o cerere goală n-ar dovedi nimic, fiindcă secțiunile fără niciun răspuns
// se pliază dinadins la un singur rând.
await page.fill("#ar-wp-name", "Hala de probă");
await page.fill("#ar-wp-address", "Str. Probelor nr. 6, Cluj-Napoca");
await page.fill("#ar-contact-phone", "0740000006");
await page.fill("#ar-auth-number", "AM-PROBA-6");
await page.fill("#ar-notes", "Rând scris de proba automată 6-cerere-si-rapoarte.");
await page.click('button[type="submit"]');
await page.waitForTimeout(1200);

const thanks = await page.textContent("body");
check("cererea se trimite", /Cererea a fost trimisă/.test(thanks), "");
check("pagina de mulțumire numește emailul", thanks.includes(email), "");
check("și dă un termen", /1–2 zile lucrătoare/.test(thanks), "");
await shot(page, "6-cerere-multumire");

// ══════════════════════════════════════════════ INBOXUL CARE O CITEȘTE
await login(page, "platform");
await page.goto(BASE + "/clienti", { waitUntil: "networkidle" });
await page.waitForTimeout(900);

await page.evaluate(() => {
  const h = [...document.querySelectorAll("h2")].find((x) => x.textContent.includes("Cereri de cont"));
  h?.scrollIntoView({ block: "center" });
});
await page.waitForTimeout(300);

const seeButtons = await page.$$('button:has-text("Vezi cererea")');
check("fiecare cerere se poate citi întreagă", seeButtons.length > 0, seeButtons.length + " rânduri");

// Coloana de acţiuni nu se mai rupe pe două rânduri. Un rând de tabel cu butoane frânte a fost
// defectul găsit uitându-mă la captură, cu toate verificările de DOM verzi.
const actionsWrap = await page.$$eval("tbody tr td:last-child > div", (cells) =>
  cells.some((c) => c.getBoundingClientRect().height > 44)
);
check("butoanele de acţiune stau pe un rând", !actionsWrap);

await seeButtons[0].click();
await page.waitForTimeout(500);
const dialog = await page.textContent('div[role="dialog"]');
// Cele opt rubrici pe care tabelul le ascundea. `notes` e cea care contează cel mai mult: e
// rubrica de text liber în care omul scrie ce nu încape în restul formularului.
const sectionsInDialog = await page.$$eval('div[role="dialog"] section h3', (h) =>
  h.map((x) => x.textContent.trim())
);
check(
  "dialogul are toate secțiunile formularului",
  sectionsInDialog.length === 7,
  sectionsInDialog.join(" · ")
);
check("se citește și textul liber", dialog.includes("proba automată"), "");
check("se citește adresa punctului de lucru", dialog.includes("Str. Probelor nr. 6"), "");
check("se citește telefonul", dialog.includes("0740000006"), "");
check("se citește autorizația de mediu", dialog.includes("AM-PROBA-6"), "");
// O rubrică necompletată se **arată** goală, nu se sare: cine creează firma trebuie să vadă că
// lipsește, nu să deducă asta din absența ei printre cele completate.
const emptyMarks = await page.$$eval('div[role="dialog"] dd span.text-content-subtle', (s) => s.length);
check("rubricile necompletate se văd ca goale", emptyMarks > 0, emptyMarks + " liniuțe");
await shot(page, "6-cerere-dialog");

await page.keyboard.press("Escape");
await page.waitForTimeout(300);
check("dialogul se închide", (await page.$('div[role="dialog"]')) === null);

// ══════════════════════════════════════════════ DE LA RAPORT LA MIȘCAREA VINOVATĂ
await login(page, "admin");

// Panoul promitea drumul cu linkul „Repară”. Ducea la vederea lunară, unde rândul e un agregat
// pe (punct de lucru, cod, lună) și nu se poate deschide nicio mișcare.
await page.goto(BASE + "/", { waitUntil: "networkidle" });
await page.waitForTimeout(900);
const panel = await page.textContent("body");
// Cele două blocaje au acum rânduri în seed-ul demo, deci se probează pe date, nu pe gol.
// „1 linie fără cod R/D la ieșire", de la felia de numeral din 09.09: complementul zicea „ieșiri"
// și pe un rând singur. Regexul prinde amândouă formele de plural, plus singularul.
check("panoul numără ieșirile fără cod R/D",
  /lini(e|i) fără cod R\/D la ieșire/.test(panel), "");
check("și le ține separate de cele care așteaptă cântarul", /lini(e|i) care așteaptă cântarul/.test(panel), "");

const fixHref = await page.$$eval("a", (a) =>
  a.filter((x) => x.textContent.trim() === "Vezi liniile").map((x) => x.getAttribute("href"))
);
check("panoul are un drum pentru fiecare blocaj", fixHref.length === 2, fixHref.join(" | "));
// Ducea la vederea lunară, unde rândul e un agregat și nu se poate deschide nicio mișcare.
check(
  "blocajul roșu duce la rândurile care se pot repara, filtrate",
  fixHref.includes("/evidente?vedere=handovers&problema=cod-rd"),
  fixHref.join(" | ")
);

// Filtrul „doar ce blochează depunerea” se vede și se poate scoate — altfel tabelul pare gol pe
// nedrept și omul caută rânduri care există.
await page.goto(BASE + "/evidente?vedere=handovers&problema=cod-rd", { waitUntil: "networkidle" });
await page.waitForTimeout(900);
const bodyText = await page.textContent("body");
check(
  "filtrul pus din altă parte se anunță",
  /Doar ieșirile fără cod R\/D/.test(bodyText),
  ""
);
// Registrul filtrat arată chiar rândul vinovat, cu badge roșu și cu acțiunea care duce la el.
const redRows = await page.$$eval("tbody tr", (rows) =>
  rows.map((r) => r.textContent.replace(/\s+/g, " ").trim())
);
check("filtrul lasă doar ieșirile fără cod", redRows.length >= 1, redRows.length + " rânduri");
check(
  "rândul poartă badge-ul roșu",
  redRows.every((r) => r.includes("Fără cod R/D")),
  redRows[0]?.slice(0, 80) ?? ""
);
const fixLink = await page.$$eval("a", (a) =>
  a.filter((x) => x.textContent.trim() === "Completează codul").map((x) => x.getAttribute("href"))
);
check("și acțiunea care deschide mișcarea", fixLink.length >= 1, fixLink.join(" | "));
check(
  "linkul poartă luna lui `date`, altfel rândul n-ar fi printre cele aduse",
  fixLink.every((h) => /\/miscari\?luna=\d{4}-\d{2}&miscare=/.test(h)),
  fixLink[0] ?? ""
);

const offButton = await page.$('button:has-text("Arată toate predările")');
check("și se poate scoate de aici", offButton !== null);
await offButton?.click();
await page.waitForTimeout(600);
check(
  "scoaterea filtrului îl șterge din adresă",
  !page.url().includes("problema="),
  page.url().replace(BASE, "")
);
await shot(page, "6-registru-filtrat");

// Linkul către o mișcare o deschide direct în formularul de editare, iar parametrul se consumă:
// lăsat în adresă, un refresh ar redeschide dialogul peste ce lucrezi.
const firstMovement = await page.evaluate(async () => {
  const res = await fetch("/api/v1/movements", {
    headers: { Authorization: "Bearer " + localStorage.getItem("eco_token") },
  });
  if (!res.ok) return null;
  const list = await res.json();
  return list.length ? { id: list[0].id, date: list[0].date } : null;
});
if (firstMovement) {
  await page.goto(
    BASE + `/miscari?luna=${firstMovement.date.slice(0, 7)}&miscare=${firstMovement.id}`,
    { waitUntil: "networkidle" }
  );
  await page.waitForTimeout(1200);
  check("adresa deschide mișcarea cerută", (await page.$('div[role="dialog"]')) !== null);
  check(
    "parametrul se consumă, ca refreshul să nu redeschidă dialogul",
    !page.url().includes("miscare="),
    page.url().replace(BASE, "")
  );
  await shot(page, "6-miscare-din-link");
} else {
  console.log("  ——   nicio mișcare în baza de dev, linkul nu s-a putut proba");
}

for (const p of [...new Set(page.problems)]) {
  console.log("  PROBLEMĂ " + p);
  fails++;
}

console.log(fails === 0 ? "\n✓ proba 6 trece" : `\n✗ proba 6: ${fails} probleme`);
await browser.close();
process.exit(fails === 0 ? 0 : 1);
