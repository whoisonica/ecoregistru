// Proba 2: coloana de acțiuni rămâne vizibilă și după ce tabelul se derulează la capăt.
import { launch, newPage, login, shot, BASE, SHOTS } from "./lib.mjs";
import path from "node:path";

const browser = await launch();
const page = await newPage(browser, { width: 1280, height: 800 });
await login(page, "admin");

// Mişcări deschide implicit luna curentă. Proba mergea pe bazele de lucru fiindcă acolo luna curentă
// avea mereu ceva — rânduri lăsate de alte probe; pe o bază curată, din prima zi a unei luni fără
// mişcări, tabelul arăta doar starea goală şi proba cădea cu o excepţie. Se alege luna celei mai noi
// mişcări, citită din API. Lista vine deja cu cea mai nouă întâi; un `sort=` în adresă e ignorat
// (verificat 14.09: `asc`, `desc` şi nimic dau acelaşi prim rând), deci nu se trimite.
const lunaCuDate = await page.evaluate(async () => {
  const res = await fetch("/api/v1/movements?size=1", {
    headers: { Authorization: "Bearer " + localStorage.getItem("eco_token") },
  });
  const cea = (await res.json()).content?.[0];
  return cea ? cea.date.slice(0, 7) : null;
});

let bad = 0;
for (const [route, name] of [[`/miscari${lunaCuDate ? "?luna=" + lunaCuDate : ""}`, "miscari"], ["/parteneri", "parteneri"]]) {
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

  if (check.eroare) {
    bad++;
    console.log(`  FAIL ${route} — ${check.eroare}`);
    continue;
  }

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
