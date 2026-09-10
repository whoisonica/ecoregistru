// Proba 12: cele două documente publice — `/termeni` și `/confidentialitate` — și drumurile care
// duc la ele.
//
// De ce merită o probă proprie, când sunt „doar text": paginile astea sunt **singurele** din
// aplicație care trebuie să se deschidă fără sesiune, fiindcă un consultant cere politica înainte
// de a avea cont. Iar textul lor trece printr-un marcaj de rând scris în casă (`**îngroșat**`,
// `` `cod` ``, `[text](adresă)`): dacă randarea lui se strică, nu cade nimic — apar semnele în
// pagină, și le vede clientul, nu compilatorul. De aici verificarea că nu rămâne niciun `**`.
import { launch, newPage, login, shot, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
let fails = 0;
const check = (n, ok, d = "") => {
  console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`);
  if (!ok) fails++;
};

// ══════════════════════════════════════════════ TERMENII, FĂRĂ AUTENTIFICARE
await page.goto(BASE + "/termeni", { waitUntil: "networkidle" });
await page.waitForTimeout(300);

const h1 = (await page.textContent("h1")) ?? "";
check("se deschide fără sesiune", /Termeni și condiții/.test(h1), h1.trim());
check("nu ne-a aruncat în login", !page.url().includes("/login"), page.url());

const dateLine = (await page.textContent("h1 + p")) ?? "";
check("spune de când e în vigoare", /În vigoare de la:\s*\S/.test(dateLine), dateLine.trim());

// Cele şaisprezece capitole, şi cuprinsul care le numără la fel. Un capitol adăugat în
// `legal.ts` şi uitat din cuprins n-ar avea cum să se vadă altfel.
const chapters = await page.$$eval("article section", (s) => s.length);
const tocEntries = await page.$$eval("nav ol li a", (a) => a.length);
check("are cele 16 capitole", chapters === 16, chapters + " capitole");
check("cuprinsul le are pe toate", tocEntries === chapters, tocEntries + " intrări");

// Marcajul de rând s-a consumat: în pagină nu rămâne niciun asterisc dublu şi niciun `[text](url)`.
const termsText = (await page.textContent("article")) ?? "";
check("marcajul îngroșat s-a randat", !termsText.includes("**"), "zero „**”");
check("nu rămân link-uri needesfăcute", !/\]\(https?:/.test(termsText));

// Trimiterile la termeni se fac pe capitol, deci ancorele sunt un contract cu cine dă linkul:
// dacă `legal.ts` schimbă un `id`, linkul trimis pe mail moare tăcut.
await page.goto(BASE + "/termeni#raspundere", { waitUntil: "networkidle" });
await page.waitForTimeout(400);
const anchored = await page.evaluate(() => {
  const el = document.getElementById("raspundere");
  if (!el) return { lipsă: true };
  const top = el.getBoundingClientRect().top;
  return { top, înEcran: top >= 0 && top < window.innerHeight };
});
check("o ancoră din adresă derulează la capitol", anchored.înEcran === true, JSON.stringify(anchored));

// ══════════════════════════════════════════════ POLITICA
await page.goto(BASE + "/confidentialitate", { waitUntil: "networkidle" });
await page.waitForTimeout(300);

const privacyH1 = (await page.textContent("h1")) ?? "";
check("politica se deschide", /Politica de confidențialitate/.test(privacyH1), privacyH1.trim());

const tables = await page.$$eval("article table", (t) => t.length);
check("cele două tabele sunt randate", tables === 2, tables + " tabele");

const privacyText = (await page.textContent("article")) ?? "";
check("marcajul s-a randat și aici", !privacyText.includes("**"), "zero „**”");
check(
  "spune deschis că atașamentele stau în SUA",
  /Cloudinary, în Statele Unite/.test(privacyText)
);
check(
  "spune că nu există cookie-uri de urmărire",
  /Nu folosim cookie-uri de urmărire/.test(privacyText)
);
// Propoziția despre societăţile-mamă era scrisă de două ori în sursă, cu două numărători care se
// contraziceau. A rămas una singură.
const parentCompanySentences = (privacyText.match(/societate-mamă în Statele Unite/g) ?? []).length;
check("propoziția despre societățile-mamă apare o dată", parentCompanySentences === 1, parentCompanySentences + " ocurențe");

// ---------------------------------------------- ECRAN ÎNGUST: tabelele derulează, pagina nu
await page.setViewportSize({ width: 375, height: 780 });
await page.waitForTimeout(400);
const overflow = await page.evaluate(() => ({
  pagină: document.documentElement.scrollWidth - document.documentElement.clientWidth,
  tabelDerulează: [...document.querySelectorAll("article table")].every(
    (t) => t.parentElement.scrollWidth > t.parentElement.clientWidth
  ),
}));
check("pagina nu depășește pe telefon", overflow.pagină <= 1, overflow.pagină + "px");
check("tabelele derulează în containerul lor", overflow.tabelDerulează === true);
await shot(page, "12-politica-telefon");
await page.setViewportSize({ width: 1440, height: 900 });

// ══════════════════════════════════════════════ DRUMURILE CĂTRE ELE
// Subsolul de pe login. Un link care duce în altă parte decât scrie e mai rău decât unul lipsă.
await page.goto(BASE + "/login", { waitUntil: "networkidle" });
await page.waitForTimeout(300);
const footerLinks = await page.$$eval("footer a", (a) =>
  a.map((x) => x.getAttribute("href") ?? x.getAttribute("to") ?? "")
);
check(
  "loginul are în subsol amândouă documentele",
  footerLinks.includes("/termeni") && footerLinks.includes("/confidentialitate"),
  footerLinks.join(", ")
);

await page.click('footer a[href="/confidentialitate"]');
await page.waitForURL((u) => u.pathname === "/confidentialitate", { timeout: 5000 });
check("linkul din subsol chiar deschide politica", page.url().endsWith("/confidentialitate"));

// Cererea de cont: rândul de sub buton, cu cele două trimiteri.
await page.goto(BASE + "/cerere-cont", { waitUntil: "networkidle" });
await page.waitForTimeout(300);
const noticeText = (await page.textContent("form")) ?? "";
check(
  "cererea de cont spune ce accepți trimițând-o",
  /confirmi că ai citit/.test(noticeText) && /politica de confidențialitate/.test(noticeText)
);
const noticeLinks = await page.$$eval('form a[href="/termeni"], form a[href="/confidentialitate"]', (a) => a.length);
check("și le leagă pe amândouă", noticeLinks === 2, noticeLinks + " linkuri");

// ══════════════════════════════════════════════ DIN APLICAȚIE, CU SESIUNE
await login(page);
await page.waitForTimeout(400);
const sidebarLinks = await page.$$eval('aside footer a', (a) => a.map((x) => x.getAttribute("href")));
check(
  "bara laterală are subsolul juridic",
  sidebarLinks.includes("/termeni") && sidebarLinks.includes("/confidentialitate"),
  sidebarLinks.join(", ")
);

// Documentele se citesc și cu sesiune deschisă, fără să iasă din aplicație într-o pagină goală.
await page.click('aside footer a[href="/termeni"]');
await page.waitForURL((u) => u.pathname === "/termeni", { timeout: 5000 });
const stillTerms = (await page.textContent("h1")) ?? "";
check("se deschid și dinăuntru", /Termeni și condiții/.test(stillTerms), stillTerms.trim());
await shot(page, "12-termeni");

console.log("");
if (page.problems.length) {
  console.log("  Cereri roșii pe drum:");
  for (const p of [...new Set(page.problems)]) console.log("    " + p);
}
console.log(fails === 0 ? "✓ paginile legale trec" : `✗ ${fails} verificări au căzut`);
await browser.close();
process.exit(fails === 0 ? 0 : 1);
