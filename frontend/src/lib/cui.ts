/**
 * CUI-ul unei firme românești, verificat ca în backend (`util/Cui.java`): 2–10 cifre, cu sau fără „RO”,
 * ultima fiind cifra de control. O cifră greșită trecea de formular și cădea abia la factura din FGO
 * (17.09.2026).
 */
const KEY = "753217532";

export function isValidCui(raw: string): boolean {
  const cui = raw.replace(/\s/g, "").toUpperCase().replace(/^RO/, "");
  if (!/^\d{2,10}$/.test(cui)) return false;
  const body = cui.slice(0, -1);
  const offset = KEY.length - body.length;
  let sum = 0;
  for (let i = 0; i < body.length; i++) sum += Number(body[i]) * Number(KEY[offset + i]);
  const control = (sum * 10) % 11;
  return (control === 10 ? 0 : control) === Number(cui[cui.length - 1]);
}
