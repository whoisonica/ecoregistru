// Capturi pentru privit, nu probe: fiecare ecran al direcției „Cântar” la 1440px și la 375px.
// Se rulează ca `E2E_PASSWORD=… node e2e/shots-cantar.mjs`; capturile intră în `shots/cantar-*`.
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
const AN = new Date().getFullYear();
const SCREENS = [
  ["/", "acasa"],
  [`/generare?luna=${AN}`, "generare"],
  [`/intrari?luna=${AN}`, "intrari"],
  [`/iesiri?luna=${AN}`, "iesiri"],
  ["/evidente", "evidente"],
  ["/ambalaje", "ambalaje"],
  ["/termene", "termene"],
  ["/parteneri", "parteneri"],
  ["/setari", "setari"],
];

for (const [width, tag] of [[1440, "1440"], [375, "375"]]) {
  const page = await newPage(browser, { width, height: width === 375 ? 740 : 900 });
  await login(page, "admin");
  for (const [route, name] of SCREENS) {
    page.problems.length = 0;
    await page.goto(BASE + route, { waitUntil: "networkidle" });
    await page.waitForTimeout(900);
    await page.screenshot({ path: `e2e/shots/cantar-${name}-${tag}.png`, fullPage: false });
    if (page.problems.length) console.log(`${route} @${tag}:`, page.problems.join(" | "));
  }
  if (width === 1440) {
    // Panoul strâns, paleta deschisă, formularul de intrare.
    await page.goto(BASE + `/intrari?luna=${AN}`, { waitUntil: "networkidle" });
    await page.waitForTimeout(600);
    await page.keyboard.press("[");
    await page.waitForTimeout(400);
    await page.screenshot({ path: "e2e/shots/cantar-panou-strans-1440.png" });
    await page.keyboard.press("[");
    await page.keyboard.press("Control+k");
    await page.waitForTimeout(400);
    await page.screenshot({ path: "e2e/shots/cantar-paleta-1440.png" });
    await page.keyboard.press("Escape");
    await page.keyboard.press("i");
    await page.waitForTimeout(800);
    await page.screenshot({ path: "e2e/shots/cantar-formular-intrare-1440.png" });
  } else {
    await page.goto(BASE + "/", { waitUntil: "networkidle" });
    await page.click('button[aria-label][aria-controls="navigatie-principala"]');
    await page.waitForTimeout(500);
    await page.screenshot({ path: "e2e/shots/cantar-sertar-375.png" });
  }
  await page.close();
}
await browser.close();
console.log("gata");
