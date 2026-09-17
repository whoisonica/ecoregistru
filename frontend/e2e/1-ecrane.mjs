// Proba 1: fiecare ecran se deschide, randează ceva și nu scoate erori.
import { launch, newPage, login, shot, report, BASE } from "./lib.mjs";

const SCREENS = [
  ["/", "Acasă", "h1"],
  ["/miscari", "Generare", "table"],
  ["/intrari", "Intrări", "table"],
  ["/iesiri", "Ieșiri", "table"],
  // Adresa veche (14–15.09.2026) rămâne un redirect spre Intrări: linkuri și mailuri vechi.
  ["/intrari-iesiri", "Intrări (adresa veche)", "table"],
  ["/evidente", "Evidențe", "table"],
  ["/parteneri", "Parteneri", "table"],
  ["/termene", "Termene", "table"],
  ["/ambalaje", "Ambalaje", "table"],
  ["/dosar-control", "Dosar de control", "h1"],
  ["/setari", "Setări", 'a[href="/setari/datele-firmei"]'],
  ["/setari/puncte-de-lucru", "Setări · Puncte de lucru", "table"],
];

// QA-TRACE punctul 18: agenţia se numeşte ANMAP din 11.07.2026 (Legea 26/2026 art. IV). Un şir de
// ecran care alunecă înapoi la „ANPM" nu cade la niciun test de backend, iar lista de mai sus citeşte
// deja fiecare ecran — deci verificarea stă aici, pe tot textul, nu pe un şir anume.
//
// ⚠️ `innerText`, nu `textContent`. `textContent` lipeşte nodurile fără niciun separator, deci eticheta
// „Termene ANPM" din bara laterală ajungea „Termene ANPMDosar de control" — iar `\b` nu vede graniţă
// între M şi D. Prima variantă a verificării a trecut verde exact aşa, cu „ANPM" pe ecran; a prins-o
// proba negativă (14.09.2026). `innerText` pune rând nou între blocuri, adică textul aşa cum se citeşte.
async function anpm(page) {
  const text = await page.evaluate(() => document.body.innerText).catch(() => null);
  if (text === null) page.problems.push("textul ecranului nu s-a putut citi — verificarea „ANPM” n-a rulat");
  else if (/\bANPM\b/.test(text)) page.problems.push("ecranul scrie „ANPM” — numele e ANMAP din 11.07.2026");
}

const browser = await launch();
let failures = 0;

console.log("=== ADMIN, desktop 1440x900 ===");
{
  const page = await newPage(browser);
  await login(page, "admin");
  for (const [path, name, must] of SCREENS) {
    page.problems.length = 0;
    await page.goto(BASE + path, { waitUntil: "networkidle" });
    // Lasă timp React Query să așeze datele și scheletele să dispară.
    await page.waitForTimeout(700);
    const has = await page.$(must);
    if (!has) page.problems.push(`lipsește <${must}> pe ecran`);
    const title = await page.textContent("h1").catch(() => null);
    if (!title || !title.trim()) page.problems.push("titlul paginii e gol");
    await anpm(page);
    await shot(page, "admin" + path.replace(/\//g, "_"));
    failures += report(`${name} (${path}) — titlu: ${JSON.stringify((title || "").trim())}`, page.problems);
  }
  await page.close();
}

console.log("");
console.log("=== PLATFORM_ADMIN: ecranul Clienți ===");
{
  const page = await newPage(browser);
  await login(page, "platform");
  page.problems.length = 0;
  await page.goto(BASE + "/clienti", { waitUntil: "networkidle" });
  await page.waitForTimeout(700);
  if (!(await page.$("table"))) page.problems.push("lipsește tabelul de firme");
  await shot(page, "platform_clienti");
  failures += report("Clienți (/clienti)", page.problems);
  await page.close();
}

console.log("");
console.log("=== Pagini publice ===");
{
  const page = await newPage(browser);
  for (const [path, name] of [["/login", "Autentificare"], ["/cerere-cont", "Cerere de cont"], ["/parola-uitata", "Parolă uitată"]]) {
    page.problems.length = 0;
    await page.goto(BASE + path, { waitUntil: "networkidle" });
    await page.waitForTimeout(400);
    if (!(await page.$("h1"))) page.problems.push("lipsește h1");
    await anpm(page);
    await shot(page, "public" + path.replace(/\//g, "_"));
    failures += report(`${name} (${path})`, page.problems);
  }
  await page.close();
}

await browser.close();
console.log("");
console.log(failures === 0 ? "REZULTAT: toate ecranele curate" : `REZULTAT: ${failures} probleme`);
process.exit(failures === 0 ? 0 : 1);
