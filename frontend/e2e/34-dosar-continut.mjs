// Proba 34: „Ce intră în arhivă” pe Dosarul de control spune ce se generează și ce nu, pentru firma și anul
// alese, cu motivul (proprietarul, 17.09.2026: „să fie informații reale”).
//
// Firma demo are în 2026 mișcări, ambalaje și parteneri, dar profilul nu răspunde la rolul de piață și nici la
// rolul în lanțul ambalajelor: fișa și centralizata intră, Anexa 1 Ambalaje nu intră (profil necompletat),
// Anexa 3 Ambalaje lipsește (anul are ambalaje, rolul nu e ales). Pe un an fără mișcări, fișa „intră fără date”.
// Nu lasă nimic în urmă.
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
let fails = 0;

function check(name, ok, detail = "") {
  if (ok) console.log(`  OK   ${name}${detail ? " — " + detail : ""}`);
  else {
    console.log(`  FAIL ${name}${detail ? " — " + detail : ""}`);
    fails++;
  }
}

async function rows(page) {
  await page.waitForSelector('[data-testid="audit-file-contents"] li', { timeout: 10000 });
  return page.$$eval('[data-testid="audit-file-contents"] li', (lis) =>
    lis.map((li) => ({ state: li.children[0].textContent.trim(), text: li.children[1].textContent })));
}

const find = (list, title) => list.find((r) => r.text.includes(title));

// Numărul de parteneri nu e o constantă a bazei: probele 8 și 32 adaugă fiecare câte unul, iar în CI
// toate probele rulează una după alta pe aceeași bază — proba căuta „6 parteneri” și găsea 7, de trei
// rulări la rând. Se citește lista pe care o vede și ecranul „Parteneri”, iar expirările se socotesc
// din datele ei: o a doua socoteală peste date brute, nu aceeaşi cifră întoarsă de unde o ia ecranul.
async function partnerCounts(page) {
  return page.evaluate(async () => {
    const res = await fetch("/api/v1/partners", {
      headers: { Authorization: "Bearer " + localStorage.getItem("eco_token") },
    });
    if (!res.ok) return null;
    const list = await res.json();
    const today = new Date();
    const midnight = Date.UTC(today.getFullYear(), today.getMonth(), today.getDate());
    let expired = 0;
    let soon = 0;
    for (const p of list) {
      // Aceleaşi reguli ca `AuditFileService.status`: inactivul şi cel fără dată nu intră la socoteală.
      if (!p.active || !p.authorizationValidUntil) continue;
      const days = Math.round((Date.parse(p.authorizationValidUntil + "T00:00:00Z") - midnight) / 86400000);
      if (days < 0) expired++;
      else if (days <= 60) soon++;
    }
    return { total: list.length, expired, soon };
  });
}

/** „1 partener” · „6 parteneri” · „20 de parteneri”: regula pe care o scrie ecranul (`lib/count.ts`). */
function parteneri(n) {
  if (n === 1) return "1 partener";
  const lastTwo = Math.abs(n) % 100;
  return `${n}${lastTwo === 0 || lastTwo >= 20 ? " de " : " "}parteneri`;
}

for (const [width, height] of [[1440, 900], [375, 800]]) {
  const page = await newPage(browser, { width, height });
  await login(page, "admin");
  const counts = await partnerCounts(page);
  await page.goto(BASE + "/dosar-control?an=2026", { waitUntil: "networkidle" });
  const list = await rows(page);
  const sheet = find(list, "Evidența gestiunii deșeurilor generate");
  const central = find(list, "Evidența gestiunii deșeurilor centralizată");
  const a1 = find(list, "Anexa 1 Ambalaje");
  const a3 = find(list, "Anexa 3 Ambalaje");
  const partners = find(list, "Autorizațiile partenerilor");
  const att = find(list, "Atașamente");

  if (width === 1440) {
    check("șase rânduri, fără rezumatul neoficial", list.length === 6 && !list.some((r) => /tabel de lucru/.test(r.text)),
      String(list.length));
    check("fișa intră, cu mișcările anului și termenul de 15 martie 2027",
      sheet?.state === "Intră" && /\d+ (de )?mișcăr/.test(sheet.text) && /15 martie 2027/.test(sheet.text), sheet?.text);
    check("centralizata intră", central?.state === "Intră", central?.text);
    check("Anexa 1 Ambalaje nu intră și spune că profilul nu răspunde",
      a1?.state === "Nu intră" && /nu spune dacă e producător, importator sau comerciant/.test(a1.text), a1?.text);
    check("Anexa 3 Ambalaje lipsește și spune de ce", a3?.state === "Lipsește" && /colector, comerciant, reciclator/.test(a3.text),
      a3?.text);
    check("proba are de unde citi câți parteneri sunt", counts !== null && counts.total > 0, JSON.stringify(counts));
    if (counts !== null && counts.total > 0) {
      // Cifra se cere lipită de început de număr: „6 parteneri” e o bucată din „16 parteneri”, iar
      // fără garda asta un ecran care numără greşit ar trece.
      const spune = (n, coada) =>
        new RegExp(`(?<!\\d)${parteneri(n)}${coada}`).test(partners?.text ?? "");
      check(`autorizațiile: ${parteneri(counts.total)}, ${counts.expired} expirată/e, ${counts.soon} pe terminate`,
        partners?.state === "Intră" && spune(counts.total, "")
        // Rândul roşu şi cel galben apar numai când au pe cine număra — la zero, ecranul nu scrie nimic.
        && spune(counts.expired, " cu autorizația expirată") === (counts.expired > 0)
        && spune(counts.soon, " cu autorizația care expiră în următoarele 60 de zile") === (counts.soon > 0),
        partners?.text);
    }
    check("atașamentele spun câte sunt", att?.state === "Intră" && /atașamente/.test(att.text), att?.text);
    await shot(page, "dosar_continut_1440");

    // Un an fără mișcări: fișele ies fără date, și ecranul o spune.
    await page.goto(BASE + "/dosar-control?an=2021", { waitUntil: "networkidle" });
    const empty = await rows(page);
    const emptySheet = find(empty, "Evidența gestiunii deșeurilor generate");
    check("2021 fără mișcări: fișa „Intră fără date”", emptySheet?.state === "Intră fără date"
      && /Nicio mișcare înregistrată în 2021/.test(emptySheet.text), emptySheet?.text);

    // Trei ani: câte un rând pe an.
    await page.goto(BASE + "/dosar-control?an=2026&ani=3", { waitUntil: "networkidle" });
    const three = find(await rows(page), "Evidența gestiunii deșeurilor generate");
    check("pe trei ani, fișa numește 2024, 2025 și 2026", /2024:/.test(three?.text) && /2025:/.test(three?.text)
      && /2026:/.test(three?.text), three?.text);
  } else {
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
    check("375px fără derulare laterală", overflow <= 0, `${overflow}px`);
    await page.evaluate(() => document.querySelector('[data-testid="audit-file-contents"]').scrollIntoView());
    await shot(page, "dosar_continut_375");
  }
  await page.close();
}

await browser.close();
if (fails) {
  console.log(`\n${fails} verificări au căzut.`);
  process.exit(1);
}
console.log("\nToate verificările trec.");
