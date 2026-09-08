// Proba 2: coloana de acțiuni rămâne vizibilă și după ce tabelul se derulează la capăt.
import { launch, newPage, login, shot, BASE, SHOTS } from "./lib.mjs";
import path from "node:path";

const browser = await launch();
const page = await newPage(browser, { width: 1280, height: 800 });
await login(page, "admin");

let bad = 0;
for (const [route, name] of [["/miscari", "miscari"], ["/parteneri", "parteneri"]]) {
  await page.goto(BASE + route, { waitUntil: "networkidle" });
  await page.waitForTimeout(700);

  // Derulează tabelul până la capătul din dreapta.
  await page.evaluate(() => {
    const box = document.querySelector("table").parentElement;
    box.scrollLeft = box.scrollWidth;
  });
  await page.waitForTimeout(300);

  const check = await page.evaluate(() => {
    const box = document.querySelector("table").parentElement;
    const boxRect = box.getBoundingClientRect();
    // Prima celulă de acțiuni din corp.
    const cell = document.querySelector("tbody tr td[class*='sticky']");
    if (!cell) return { eroare: "nicio celulă fixată găsită" };
    const r = cell.getBoundingClientRect();
    return {
      scrollLeft: Math.round(box.scrollLeft),
      scrollMax: Math.round(box.scrollWidth - box.clientWidth),
      celulaDreapta: Math.round(r.right),
      containerDreapta: Math.round(boxRect.right),
      inauntru: r.right <= boxRect.right + 1 && r.left >= boxRect.left,
      latime: Math.round(r.width),
    };
  });

  // Și la scrollLeft = 0: celula trebuie să fie tot pe margine.
  await page.evaluate(() => {
    document.querySelector("table").parentElement.scrollLeft = 0;
  });
  await page.waitForTimeout(300);
  const atStart = await page.evaluate(() => {
    const box = document.querySelector("table").parentElement;
    const cell = document.querySelector("tbody tr td[class*='sticky']");
    const r = cell.getBoundingClientRect();
    const b = box.getBoundingClientRect();
    return { inauntru: r.right <= b.right + 1 && r.left >= b.left };
  });

  const ok = check.inauntru && atStart.inauntru;
  if (!ok) bad++;
  console.log(`${ok ? "  OK  " : "  FAIL"} ${route}`);
  console.log(`         derulat la ${check.scrollLeft}/${check.scrollMax}px; celula la ${check.celulaDreapta}, container la ${check.containerDreapta}`);
  console.log(`         vizibilă derulat: ${check.inauntru} · vizibilă la început: ${atStart.inauntru}`);

  await page.evaluate(() => {
    document.querySelector("table").parentElement.scrollLeft = 99999;
  });
  await page.waitForTimeout(200);
  await page.screenshot({ path: path.join(SHOTS, `fix_${name}_derulat.png`) });
}

await browser.close();
console.log(bad === 0 ? "REZULTAT: coloana de acțiuni rămâne la îndemână" : `REZULTAT: ${bad} tabele cu probleme`);
process.exit(bad === 0 ? 0 : 1);
