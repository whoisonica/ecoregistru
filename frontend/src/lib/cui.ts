/**
 * CUI-ul unei firme românești, verificat ca în backend (`util/Cui.java`): 2–10 cifre, cu sau fără „RO”,
 * ultima fiind cifra de control. O cifră greșită trecea de formular și cădea abia la factura din FGO
 * (17.09.2026).
 */
const KEY = "753217532";

/** CUI-ul fără „RO”, fără spații, cu majuscule: forma în care două scrieri ale aceleiași firme se potrivesc. */
export function normalizeCui(raw: string): string {
  return raw.replace(/\s/g, "").toUpperCase().replace(/^RO/, "");
}

export function isValidCui(raw: string): boolean {
  const cui = normalizeCui(raw);
  if (!/^\d{2,10}$/.test(cui)) return false;
  const body = cui.slice(0, -1);
  const offset = KEY.length - body.length;
  let sum = 0;
  for (let i = 0; i < body.length; i++) sum += Number(body[i]) * Number(KEY[offset + i]);
  const control = (sum * 10) % 11;
  return (control === 10 ? 0 : control) === Number(cui[cui.length - 1]);
}

/**
 * Partenerul care are deja CUI-ul tastat, în afară de cel editat (29.09.2026). Serverul nu oprește un al
 * doilea partener cu același CUI, iar duplicatul ajunge pe documentele tipărite; formularul doar spune,
 * din lista deja încărcată. `RO 14399840` și `14399840` sunt aceeași firmă.
 */
export function partnerWithSameCui<P extends { id: string; cui: string | null }>(
  cui: string,
  partners: P[],
  exceptId: string | null
): P | undefined {
  const wanted = normalizeCui(cui);
  if (!wanted) return undefined;
  return partners.find((p) => p.id !== exceptId && p.cui != null && normalizeCui(p.cui) === wanted);
}
