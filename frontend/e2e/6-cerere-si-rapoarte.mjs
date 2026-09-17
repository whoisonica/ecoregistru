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
import { launch, newPage, login, shot, validCui, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};

// ══════════════════════════════════════════════ FORMULARUL PUBLIC, FĂRĂ AUTENTIFICARE
// Din 16.09.2026 (direcția „Poster”) formularul e în patru pași — Compania · Punctul de lucru ·
// Contactul · Deșeurile — și aproape totul e obligatoriu (proprietarul: „hai să obligăm omul să își
// facă și punct de lucru și tot ce e sub «Detalii care ne scutesc de un telefon»”). Proba merge pas
// cu pas, cum merge omul, și încearcă la fiecare pas să treacă cu rubricile goale.
await page.goto(BASE + "/cerere-cont", { waitUntil: "networkidle" });
await page.waitForTimeout(400);

const brand = await page.textContent("header");
check("pagina spune al cui e", /WasteHouse/.test(brand ?? ""), (brand ?? "").trim().slice(0, 40));

const steps = await page.$$eval("ol li", (li) => li.length);
check("verdele arată cei patru pași ai formularului", steps >= 4, steps + " pași");

const CONTINUE = 'button:has-text("Continuă")';
const requiredIds = () =>
  page.$$eval("label", (labels) =>
    labels.filter((l) => l.textContent.includes("*")).map((l) => l.getAttribute("for"))
  );
const onStep = async (n) => (await page.textContent("form")).includes(`Pasul ${n} din 4`);
const invalidCount = async () => (await page.$$('[data-invalid="true"]')).length;

// ---------------------------------------------- PASUL 1: COMPANIA
const marked1 = await requiredIds();
check(
  "pasul 1 își marchează rubricile obligatorii",
  ["ar-cui", "ar-name", "ar-address", "ar-caen"].every((id) => marked1.includes(id)),
  marked1.join(", ")
);
await page.click(CONTINUE);
await page.waitForTimeout(500);
check("„Continuă” pe pasul gol marchează rubricile", (await invalidCount()) === 4, (await invalidCount()) + " marcate");
const errorTexts1 = await page.$$eval("p[data-field-error]", (p) => p.map((x) => x.textContent.trim()));
check("fiecare rubrică își spune motivul", errorTexts1.length === 4, errorTexts1.join(" | "));
const focusedId = await page.evaluate(() => document.activeElement?.id ?? "");
check("focusul sare la prima rubrică greșită", focusedId === "ar-cui", focusedId || "(niciunul)");
check("și nu trece mai departe", await onStep(1));

// CUI-ul e verificat aici, nu la aprobare.
await page.fill("#ar-name", "Proba Automata SRL");
await page.fill("#ar-cui", "nu-e-cui");
await page.fill("#ar-address", "Str. Sediului nr. 1, Cluj-Napoca");
await page.fill("#ar-caen", "1071");
await page.click(CONTINUE);
await page.waitForTimeout(400);
check("CUI-ul stricat se respinge cu forma cerută", /cifra de control/.test(await page.textContent("form")), "");

const cui = validCui();
await page.fill("#ar-cui", cui);
await page.click(CONTINUE);
await page.waitForTimeout(400);
check("pasul 2 se deschide", await onStep(2));
check("verdele scrie ce s-a completat la pasul 1", (await page.textContent("ol")).includes(cui), "");

// ---------------------------------------------- PASUL 2: PUNCTUL DE LUCRU + AUTORIZAȚIA
// Firma demo e „Generator” (implicit), deci transportul nu se cere; punctul de lucru și autorizația, da.
await page.click(CONTINUE);
await page.waitForTimeout(400);
check("punctul de lucru gol oprește pasul 2", (await invalidCount()) === 4 && (await onStep(2)), (await invalidCount()) + " marcate");
check("data lipsă are mesajul ei", /Scrie data/.test(await page.textContent("form")), "");
await page.fill("#ar-wp-name", "Hala de probă");
await page.fill("#ar-wp-address", "Str. Probelor nr. 6, Cluj-Napoca");
// Nu toți generatorii au autorizație (Anexa 1 la Ordinul 1798/2007): bifa ține loc de răspuns și
// scoate cele două rubrici din obligații — apoi se scoate, ca proba să trimită o autorizație reală.
await page.check("#ar-no-env-auth");
await page.click(CONTINUE);
await page.waitForTimeout(400);
check("„n-avem nevoie de autorizație” lasă generatorul să treacă", await onStep(3));
await page.click('button:has-text("Înapoi")');
await page.waitForTimeout(300);
await page.uncheck("#ar-no-env-auth");
await page.fill("#ar-auth-number", "AM-PROBA-6");
await page.fill("#ar-auth-expiry", "2028-06-30");
await page.click(CONTINUE);
await page.waitForTimeout(400);
check("pasul 3 se deschide", await onStep(3));

// ---------------------------------------------- PASUL 3: CONTACTUL
await page.click(CONTINUE);
await page.waitForTimeout(400);
const focused3 = await page.evaluate(() => document.activeElement?.id ?? "");
check("contactul gol oprește pasul 3, cu focus pe nume", focused3 === "ar-contact-name", focused3 || "(niciunul)");
await page.fill("#ar-contact-name", "Proba Automată");
await page.fill("#ar-contact-phone", "0740000006");
await page.fill("#ar-contact-role", "administrator");
await page.fill("#ar-contact-email", "fara-arond");
await page.click(CONTINUE);
await page.waitForTimeout(400);
check("emailul incomplet se respinge", /nu pare complet/.test(await page.textContent("form")), "");
const email = "proba" + String(Date.now()).slice(-6) + "@example.ro";
await page.fill("#ar-contact-email", email);
await page.click(CONTINUE);
await page.waitForTimeout(400);
check("pasul 4 se deschide", await onStep(4));

// ---------------------------------------------- PASUL 4: DEȘEURILE, TIPUL DE GENERATOR, CODURILE
// Cele 28 de bife R/D nu stau deschise în fața cuiva care n-a auzit de R13.
const codesHiddenAtFirst = (await page.$$('input[type="checkbox"][class*="rounded"]')).length;
const hasUnknownEscape = (await page.textContent("form")).includes("Nu știu");
check("lista de coduri R/D e pliată, cu o ieșire onorabilă", hasUnknownEscape, "„Nu știu” există");
check("codurile nu sunt bifate din start", codesHiddenAtFirst < 28, codesHiddenAtFirst + " bife vizibile");
await page.click('input[name="ar-codes-mode"] >> nth=1');
await page.waitForTimeout(250);
const codesShown = (await page.$$('input[type="checkbox"]')).length;
check("alegerea „le aleg acum” deschide lista", codesShown > 20, codesShown + " bife");

// Tipul de generator e obligatoriu la cine generează: trimiterea fără nicio bifă se oprește aici.
await page.fill("#ar-notes", "Rând scris de proba automată 6-cerere-si-rapoarte.");
await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
await page.click('button[type="submit"]');
await page.waitForTimeout(600);
check("fără tipul de generator nu se trimite", /Bifează cel puțin una/.test(await page.textContent("form")), "");
await page.click('label:has-text("Producător") input[type="checkbox"]');
await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
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
// Numai rândurile cu date: rândul gol al unui tabel („Niciun cabinet încă”, P2.13) are o singură
// celulă, cu mesajul și iconița, și nu e o coloană de acțiuni.
const actionsWrap = await page.$$eval("tbody tr td:last-child > div", (cells) =>
  cells
    .filter((c) => c.parentElement.parentElement.children.length > 1)
    .some((c) => c.getBoundingClientRect().height > 44)
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

// Din 15.09.2026 banda „Următoarea acțiune” duce și ea cu „Vezi liniile”; aici se numără doar
// drumurile din caseta de blocaje, nu și verdictul de deasupra.
const fixHref = await page.$$eval("a", (a) =>
  a.filter((x) => x.textContent.trim() === "Vezi liniile" && !x.closest('[data-testid="next-action"]')).map((x) => x.getAttribute("href"))
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
  // De la P3.1 lista vine paginată: rândurile stau în `content`, nu în rădăcină. Scris aşa,
  // `list.length` era `undefined` şi toată bucata de mai jos se sărea în tăcere — o probă care
  // trece fiindcă nu probează nimic.
  const list = (await res.json()).content;
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
