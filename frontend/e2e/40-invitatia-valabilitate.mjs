// Proba 40 (QA de lansare, 19.09.2026, BUG-042): dialogul „Invită utilizator” spune cât ține linkul.
//
// Linkul de invitație ține 7 zile (`AuthenticationService.INVITE_TTL_DAYS`), iar mailul spune „valabil 7 zile”.
// Dialogul spunea „valabil 30 de minute” — timpul linkului de resetare a parolei —, deci adminul care invită
// pe cineva seara crede că trebuie să-l sune pe loc. Nu scrie nimic în bază: dialogul se închide cu „Anulează”.
import { launch, newPage, login, BASE } from "./lib.mjs";

const browser = await launch();
const page = await newPage(browser, { width: 1440, height: 900 });
let fails = 0;
const check = (n, ok, d = "") => { console.log(`  ${ok ? "OK  " : "FAIL"} ${n}${d ? " — " + d : ""}`); if (!ok) fails++; };

await login(page, "admin");
await page.goto(BASE + "/setari/utilizatori", { waitUntil: "networkidle" });
await page.click('main button:has-text("Invită")');
const text = (await page.textContent('div[role="dialog"][aria-modal="true"]')).replace(/\s+/g, " ");
check("dialogul spune 7 zile, cât ține linkul", /7 zile/.test(text), text.slice(0, 160));
check("și nu 30 de minute", !/30 de minute/.test(text));
await page.click('div[role="dialog"] button:has-text("Anulează")');

await browser.close();
console.log(fails === 0 ? "\n✓ proba 40 trece" : `\n✗ proba 40: ${fails} verificări căzute`);
process.exit(fails === 0 ? 0 : 1);
