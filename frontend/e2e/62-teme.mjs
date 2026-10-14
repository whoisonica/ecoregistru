// Proba 62 (15.10.2026, temele de culori, pasul 6 din `docs/specs/2026-10-14-teme-culori-plan.md`): tema aleasă din
// Setări → „Aspect” se pune pe `<html data-theme>`, rămâne după reîncărcare și după deconectare (login-ul e tot în ea),
// iar Luminos înseamnă fără atribut. La 1280 și 375. Tema stă în `localStorage`, deci fiecare lățime are contextul ei.
import { launch, newPage, login, BASE } from "./lib.mjs";

const browser = await launch();
let fails = 0;
const check = (n, ok, d = "") => { console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`); if (!ok) fails++; };
const theme = (page) => page.evaluate(() => document.documentElement.getAttribute("data-theme"));
const background = (page) => page.evaluate(() => getComputedStyle(document.body).backgroundColor);

async function choose(page, label) {
  await page.goto(BASE + "/setari", { waitUntil: "networkidle" });
  const card = page.locator('[role="radiogroup"][aria-labelledby="grup-aspect"] label', { hasText: label });
  await card.scrollIntoViewIfNeeded();
  await card.click();
  await page.waitForTimeout(200);
}

async function logout(page) {
  const openNav = page.locator('button[aria-label="Deschide meniul de navigație"]:visible');
  if ((await openNav.count()) > 0) {
    // Pe telefon meniul contului stă în sertar, care e în pagină dar în afara ecranului până se deschide.
    await openNav.first().click();
    await page.waitForTimeout(400);
  }
  await page.locator('button[aria-label="Meniul contului"]:visible').first().click();
  await page.click('[role="menuitem"]:has-text("Deconectare")');
  await page.waitForURL((u) => u.pathname.startsWith("/login"), { timeout: 15000 });
  await page.waitForLoadState("networkidle");
}

for (const width of [1280, 375]) {
  console.log(`\n  — ${width}px`);
  const page = await newPage(browser, { width, height: width === 375 ? 740 : 800 });
  await login(page, "admin");
  check("fără alegere, Luminos (fără atribut)", (await theme(page)) === null, String(await theme(page)));
  const light = await background(page);

  await choose(page, "Pădure");
  check("Pădure din Setări → data-theme=padure", (await theme(page)) === "padure", String(await theme(page)));
  check("cardul Pădure e bifat", await page.locator('input[name="tema"][value="padure"]').isChecked());
  await page.reload({ waitUntil: "networkidle" });
  check("după reîncărcare rămâne Pădure", (await theme(page)) === "padure", String(await theme(page)));

  await choose(page, "Întunecat");
  check("Întunecat din Setări → data-theme=intunecat", (await theme(page)) === "intunecat", String(await theme(page)));
  const dark = await background(page);
  check("fondul paginii se schimbă în Întunecat", dark !== light, `${light} → ${dark}`);
  await page.reload({ waitUntil: "networkidle" });
  check("după reîncărcare rămâne Întunecat", (await theme(page)) === "intunecat", String(await theme(page)));

  await logout(page);
  check("login-ul e tot în Întunecat", (await theme(page)) === "intunecat", String(await theme(page)));
  check("fondul login-ului e cel întunecat", (await background(page)) === dark, await background(page));
  await page.reload({ waitUntil: "networkidle" });
  check("login-ul reîncărcat rămâne în Întunecat", (await theme(page)) === "intunecat", String(await theme(page)));

  await login(page, "admin");
  await choose(page, "Luminos");
  check("înapoi pe Luminos → fără atribut", (await theme(page)) === null, String(await theme(page)));
  await page.reload({ waitUntil: "networkidle" });
  check("după reîncărcare rămâne Luminos", (await theme(page)) === null, String(await theme(page)));
  check("fondul e iar cel deschis", (await background(page)) === light, await background(page));

  const x = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  if (width === 375) check("Setări la 375px fără derulare laterală", x <= 0, `${x}px`);
  check("fără erori de consolă sau HTTP", page.problems.length === 0, page.problems.join(" | "));
  await page.context().close();
}

await browser.close();
console.log(fails === 0 ? "\n✓ proba 62 trece" : `\n✗ proba 62: ${fails} verificări căzute`);
process.exit(fails === 0 ? 0 : 1);
