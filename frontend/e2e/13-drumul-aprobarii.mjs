// Proba 13: drumul formularului de aprobare, pe ecran (G-1).
//
// Peste 1 t/an, Anexa 2 singură nu e de ajuns: art. 7 din HG 1061/2008 cere şi formularul de
// aprobare din anexa 1, iar el trece prin patru mâini înainte să se întoarcă la client. Până acum
// ecranul îi tipărea rubrica „nr. formularului de aprobare" şi nu-i spunea nimic despre cum se
// obţine — singurul loc din aplicaţie unde clientul poate crede că a terminat când n-a terminat.
//
// Ce se probează aici e o **afirmaţie**, nu un randament: că lista apare doar peste prag, că are
// toţi cei nouă paşi, şi că pasul 9 spune **ISU**, nu client. Ultimul e chiar motivul feliei:
// prima variantă a documentaţiei noastre l-a scris ca obligaţie a clientului, şi greşeala ajunsese
// pe ecran. Dacă noi am confundat-o citind actul, clientul o confundă sigur.
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
let fails = 0;

function check(name, ok, detail = "") {
  if (ok) console.log(`  OK   ${name}${detail ? " — " + detail : ""}`);
  else {
    console.log(`  FAIL ${name}${detail ? " — " + detail : ""}`);
    fails++;
  }
}

await login(page, "admin");

// Mişcarea pe care se probează e aleasă după aceleaşi condiţii ca butonul Anexa 2 din
// `useAnexa2.canPrintAnexa2`: periculoasă, în afara capitolului 18 (fluxul medical are alt act),
// cu destinatar, şi ieşită prin valorificare sau eliminare. Se caută în date, nu se ţine un id
// scris în probă: seed-ul se poate schimba, condiţia nu.
const target = await page.evaluate(async () => {
  const res = await fetch("/api/v1/movements", {
    headers: { Authorization: "Bearer " + localStorage.getItem("eco_token") },
  });
  if (!res.ok) return null;
  const list = await res.json();
  const m = list.find(
    (x) =>
      x.hazardous &&
      !x.wasteCode.startsWith("18") &&
      x.partnerId != null &&
      (x.operation === "RECOVERED" || x.operation === "DISPOSED")
  );
  return m ? { id: m.id, date: m.date, code: m.wasteCode } : null;
});

if (!target) {
  console.log("  ——   nicio mişcare periculoasă cu destinatar în baza de dev: proba n-are pe ce rula.");
  await browser.close();
  process.exit(1);
}
console.log(`  ··   mişcarea de probă: ${target.code} din ${target.date}`);

await page.goto(BASE + `/miscari?luna=${target.date.slice(0, 7)}&miscare=${target.id}`, {
  waitUntil: "networkidle",
});
await page.waitForTimeout(1400);

const dialog = 'div[role="dialog"][aria-modal="true"]';
check("mişcarea se deschide în formular", (await page.$(dialog)) !== null);

// ---------------------------------------------- 1. SUB PRAG NU SE SPUNE NIMIC
// Uleiul din seed e 15 kg pe an, deci bifa se propune „sub 1 t/an" — iar sub prag art. 6 alin. (1)
// scoate tocmai aprobarea. O listă de nouă paşi arătată cuiva care n-are nevoie de ei e zgomot, şi
// e chiar felul de alertă falsă pe care V21 a fost scrisă să-l repare.
await page.selectOption("#mv-anexa2-threshold", "true");
await page.waitForTimeout(400);
check(
  "sub prag, drumul aprobării nu apare",
  (await page.$(`${dialog} [data-testid="anexa2-approval-road"]`)) === null
);

// ---------------------------------------------- 2. PESTE PRAG APAR TOŢI NOUĂ PAŞI
await page.selectOption("#mv-anexa2-threshold", "false");
await page.waitForTimeout(400);

const road = await page.evaluate(() => {
  const el = document.querySelector('div[role="dialog"][aria-modal="true"] [data-testid="anexa2-approval-road"]');
  if (!el) return null;
  return {
    text: el.textContent.replace(/\s+/g, " ").trim(),
    steps: [...el.querySelectorAll("ol > li")].map((li) => li.textContent.replace(/\s+/g, " ").trim()),
  };
});

check("peste prag, drumul aprobării apare", road !== null);
if (road) {
  check("are exact nouă paşi", road.steps.length === 9, `${road.steps.length} paşi`);

  // Cei doi paşi în care actul trimite hârtia la agenţia **destinatarului**, nu a expeditorului —
  // confuzia cea mai scumpă după pasul 9, fiindcă trimite clientul la ghişeul greşit.
  check(
    "pasul 4 trimite la agenţia instalaţiei destinatarului",
    /Destinatarul.*instalaţiei LUI|Destinatarul.*instalației LUI/.test(road.steps[3]),
    road.steps[3]
  );
  check(
    "pasul 5 spune cele 7 zile lucrătoare",
    /7 zile lucrătoare/.test(road.steps[4]),
    road.steps[4]
  );

  // Pasul 8 e obligaţia clientului: el duce formularul la ISU, pentru autorizarea rutei.
  check(
    "pasul 8 e al clientului, la ISU, pentru rută",
    /^Tu /.test(road.steps[7]) && /ISU/.test(road.steps[7]) && /rutei/.test(road.steps[7]),
    road.steps[7]
  );

  // ---------------------------------------------- 3. PASUL 9 — CAPCANA
  // Art. 14 alin. (1): notificarea de 48 de ore o face **inspectoratul**, după ce primeşte
  // formularele. Nu clientul. Verificarea e scrisă în amândouă sensurile dinadins: că pasul
  // numeşte ISU-ul, ŞI că nu începe cu „Tu" — un text care ar aluneca înapoi la varianta greşită
  // ar trece o verificare scrisă doar pe „conţine 48 de ore".
  check(
    "pasul 9 numeşte ISU-ul ca autor al notificării de 48 de ore",
    /^ISU-ul /.test(road.steps[8]) && /48 de ore/.test(road.steps[8]),
    road.steps[8]
  );
  check("şi pasul 9 NU e al clientului", !/^Tu /.test(road.steps[8]), road.steps[8]);
  check("cu temeiul lui, art. 14", /art\. 14/.test(road.steps[8]), road.steps[8]);

  // Rezumatul care spune clientului, într-o propoziţie, care paşi sunt ai lui.
  check("se spune pe faţă care trei paşi sunt ai clientului", /ai tăi sunt trei: 1, 2 şi 8|ai tăi sunt trei: 1, 2 și 8/.test(road.text));

  // Art. 5: o aprobare poate acoperi mai multe transporturi, şi atunci ţine 2 ani — rubrica pentru
  // care clientul nu reia tot drumul la fiecare cursă.
  check("valabilitatea de 2 ani e spusă (art. 5)", /2 ani/.test(road.text) && /art\. 5/.test(road.text));

  // Fiecare pas îşi poartă temeiul: o listă fără articole e o părere.
  check(
    "fiecare pas poartă un articol",
    road.steps.every((s) => /art\. \d/.test(s)),
    road.steps.filter((s) => !/art\. \d/.test(s)).join(" | ") || "toate"
  );
}

await shot(page, "13-drumul-aprobarii");
await page.keyboard.press("Escape");
await page.waitForTimeout(300);

console.log("");
if (fails === 0) console.log("✓ proba 13 trece.");
else console.log(`✗ proba 13: ${fails} verificări au căzut.`);
await browser.close();
process.exit(fails === 0 ? 0 : 1);
