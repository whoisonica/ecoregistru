// Proba 5: ecran îngust — sertarul de navigație, formularul stivuit, fără derulare orizontală
// a paginii (a tabelului e în regulă, a paginii nu).
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};

// iPhone SE: cel mai îngust ecran pe care merită să funcționeze.
const page = await newPage(browser, { width: 375, height: 667 });

// Formularul public se probează **înainte** de autentificare, fiindcă aşa îl vede prospectul — şi
// fiindcă e singura pagină pe care o deschide cineva de pe telefon fără să aibă cont. A lipsit de
// aici până pe 07.09.2026, adică exact cât timp a fost singura pagină neatinsă de modernizare: s-a
// rescris cap-coadă fără ca nimeni să se uite la ea la 375px.
await page.goto(BASE + "/cerere-cont", { waitUntil: "networkidle" });
await page.waitForTimeout(600);
{
  const overflow = await page.evaluate(
    () => document.documentElement.scrollWidth - document.documentElement.clientWidth
  );
  check("cerere-cont: pagina nu se derulează lateral", overflow <= 1, String(overflow));
  // Banda de trei paşi e `sm:grid-cols-3`: pe telefon trebuie să se stivuiască, nu să stea în trei
  // coloane de o sută de pixeli.
  const stepsStacked = await page.evaluate(() => {
    const items = [...document.querySelectorAll("ol li")].slice(0, 3);
    if (items.length < 3) return false;
    const tops = items.map((i) => Math.round(i.getBoundingClientRect().top));
    return new Set(tops).size === 3;
  });
  check("cerere-cont: cei trei paşi se stivuiesc", stepsStacked);
  await shot(page, "telefon_cerere-cont");
}

await login(page, "admin");

for (const [route, name] of [["/", "panou"], ["/miscari", "miscari"], ["/evidente", "evidente"], ["/setari", "setari"]]) {
  await page.goto(BASE + route, { waitUntil: "networkidle" });
  await page.waitForTimeout(800);

  // Pagina nu trebuie să se miște pe orizontală. Tabelele au voie — ele au învelișul lor.
  const overflow = await page.evaluate(() => ({
    doc: document.documentElement.scrollWidth - document.documentElement.clientWidth,
    main: (() => {
      const m = document.getElementById("continut");
      return m ? m.scrollWidth - m.clientWidth : 0;
    })(),
  }));
  check(`${name}: pagina nu se derulează lateral`, overflow.doc <= 1 && overflow.main <= 1, JSON.stringify(overflow));
  await shot(page, `telefon_${name}`);
}

// ---------------------------------------------------------------- SERTARUL
await page.goto(BASE + "/miscari", { waitUntil: "networkidle" });
await page.waitForTimeout(700);
const hidden = await page.evaluate(() => {
  const aside = document.getElementById("navigatie-principala");
  const r = aside.getBoundingClientRect();
  return { stanga: Math.round(r.left), latime: Math.round(r.width) };
});
check("sertarul e ascuns la pornire", hidden.stanga + hidden.latime <= 1, JSON.stringify(hidden));

const burger = await page.$('header button[aria-controls="navigatie-principala"]');
check("bara de sus are butonul de meniu", !!burger);
await burger.click();
await page.waitForTimeout(500);
const shown = await page.evaluate(() => {
  const r = document.getElementById("navigatie-principala").getBoundingClientRect();
  return { stanga: Math.round(r.left), vizibil: r.left >= -1 };
});
check("butonul deschide sertarul", shown.vizibil, JSON.stringify(shown));
await shot(page, "telefon_sertar");

// Navigarea îl închide la loc.
await page.click('#navigatie-principala a[href="/evidente"]');
await page.waitForTimeout(600);
const afterNav = await page.evaluate(() => {
  const r = document.getElementById("navigatie-principala").getBoundingClientRect();
  return { inchis: r.left + r.width <= 1, url: location.pathname };
});
check("navigarea închide sertarul", afterNav.inchis && afterNav.url === "/evidente", JSON.stringify(afterNav));

// Escape îl închide.
await page.click('header button[aria-controls="navigatie-principala"]');
await page.waitForTimeout(400);
await page.keyboard.press("Escape");
await page.waitForTimeout(400);
const afterEsc = await page.evaluate(() => {
  const r = document.getElementById("navigatie-principala").getBoundingClientRect();
  return r.left + r.width <= 1;
});
check("Escape închide sertarul", afterEsc);

// ---------------------------------------------------------------- FORMULARUL
await page.goto(BASE + "/miscari", { waitUntil: "networkidle" });
await page.waitForTimeout(700);
await page.click('button:has-text("Adaugă mișcare")');
await page.waitForTimeout(700);
const form = await page.evaluate(() => {
  const d = document.querySelector('div[role="dialog"][aria-modal="true"]');
  const r = d.getBoundingClientRect();
  // Grilele de formular trebuie să fie pe o coloană sub `sm`.
  const grids = [...d.querySelectorAll(".grid")].map(
    (g) => getComputedStyle(g).gridTemplateColumns.split(" ").length
  );
  return {
    latime: Math.round(r.width),
    incape: r.left >= -1 && r.right <= window.innerWidth + 1,
    lipitDeJos: Math.round(window.innerHeight - r.bottom) <= 1,
    griliPeOColoana: grids.every((n) => n === 1),
    grili: grids.join(","),
  };
});
check("dialogul încape pe lățime", form.incape, form.latime + "px");
check("dialogul urcă de la marginea de jos", form.lipitDeJos, "");
check("grilele de formular sunt stivuite", form.griliPeOColoana, "coloane: " + form.grili);
await shot(page, "telefon_formular");

await browser.close();
console.log("");
console.log(fails === 0 ? "REZULTAT: ecranul îngust trece" : `REZULTAT: ${fails} eșecuri`);
process.exit(fails === 0 ? 0 : 1);
