// Capturi pentru pasul 2 al temelor: Acasă, Generare, Termene, Setări, login, la 1280 și 375.
// E2E_BASE=… E2E_PASSWORD=… node e2e/shots-teme.mjs <dir> [luminos|padure|intunecat]
import { launch, newPage, login, BASE } from "./lib.mjs";

const OUT = process.argv[2];
const THEME = process.argv[3]; // dacă e dat, se pune în localStorage înainte de încărcare
const AN = new Date().getFullYear();
const SCREENS = [
  ["/", "acasa"],
  [`/generare?luna=${AN}`, "generare"],
  ["/termene", "termene"],
  ["/setari", "setari"],
];
const browser = await launch();
for (const width of [1280, 375]) {
  const page = await newPage(browser, { width, height: width === 375 ? 740 : 800 });
  if (THEME) await page.addInitScript((t) => { try { localStorage.setItem("eco360.tema", t); } catch {} }, THEME);
  await page.goto(BASE + "/login", { waitUntil: "networkidle" });
  await page.waitForTimeout(500);
  await page.screenshot({ path: `${OUT}/login-${width}.png` });
  await login(page, "admin");
  for (const [route, name] of SCREENS) {
    page.problems.length = 0;
    await page.goto(BASE + route, { waitUntil: "networkidle" });
    await page.waitForTimeout(800);
    await page.screenshot({ path: `${OUT}/${name}-${width}.png` });
    if (page.problems.length) console.log(`${route} @${width}:`, page.problems.join(" | "));
  }
  if (width === 375) {
    await page.goto(BASE + "/", { waitUntil: "networkidle" });
    await page.getByRole("button", { name: /meniu|navigare/i }).first().click().catch(() => {});
    await page.waitForTimeout(500);
    await page.screenshot({ path: `${OUT}/sertar-375.png` });
  } else {
    // Un dialog deschis și tooltipul: fondul de sub dialog, umbra.
    await page.goto(BASE + `/generare?luna=${AN}&nou=1`, { waitUntil: "networkidle" });
    await page.waitForTimeout(800);
    await page.screenshot({ path: `${OUT}/dialog-1280.png` });
  }
  await page.close();
}
await browser.close();
