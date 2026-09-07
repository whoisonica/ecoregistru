// Proba 1: fiecare ecran se deschide, randează ceva și nu scoate erori.
import { launch, newPage, login, shot, report, BASE } from "./lib.mjs";

const SCREENS = [
  ["/", "Panou", "h1"],
  ["/miscari", "Mișcări", "table"],
  ["/evidente", "Evidențe", "table"],
  ["/parteneri", "Parteneri", "table"],
  ["/termene", "Termene", "table"],
  ["/ambalaje", "Ambalaje", "table"],
  ["/dosar-control", "Dosar de control", "h1"],
  ["/setari", "Setări", "table"],
];

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
    await shot(page, "public" + path.replace(/\//g, "_"));
    failures += report(`${name} (${path})`, page.problems);
  }
  await page.close();
}

await browser.close();
console.log("");
console.log(failures === 0 ? "REZULTAT: toate ecranele curate" : `REZULTAT: ${failures} probleme`);
process.exit(failures === 0 ? 0 : 1);
