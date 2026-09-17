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

for (const [width, height] of [[1440, 900], [375, 800]]) {
  const page = await newPage(browser, { width, height });
  await login(page, "admin");
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
    check("autorizațiile: 6 parteneri, 1 expirată", partners?.state === "Intră" && /6 parteneri/.test(partners.text)
      && /1 partener cu autorizația expirată/.test(partners.text)
      && /1 partener cu autorizația care expiră în următoarele 60 de zile/.test(partners.text), partners?.text);
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
