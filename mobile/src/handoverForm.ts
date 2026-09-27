import type { WasteCode, WasteMovement } from "@web/types";

/**
 * Regulile formularului de predare scoase din ecran, ca să poată fi probate fără telefon.
 */

/**
 * Cantitatea tastată, ca număr — sau `null` dacă nu se poate citi fără ghicit.
 *
 * <p>Tastatura numerică a telefonului are o singură tastă de zecimale: virgula pe un telefon în română,
 * punctul pe unul în engleză. Deci un singur separator e mereu zecimala. Numai împreună cu virgula
 * punctele despart miile („1.250,5”, cum scrie avizul). Înainte punctul era șters întotdeauna, iar
 * „2.5” t pleca drept 25 t. Cel mult trei zecimale, cât primește serverul.
 */
export function parseQuantity(text: string): number | null {
  const s = text.trim();
  const match = /^(\d{1,3}(?:\.\d{3})+|\d+)(?:,(\d+))?$/.exec(s) ?? /^(\d+)(?:\.(\d+))?$/.exec(s);
  if (!match) return null;
  const [, whole, decimals = ""] = match;
  if (decimals.length > 3) return null;
  const value = Number(`${whole.replace(/\./g, "")}.${decimals || "0"}`);
  return value > 0 ? value : null;
}

/**
 * Cântărită la descărcare, cu greutatea deja venită de la colector: la corectură câmpul cantității
 * rămâne deschis și cantitatea pleacă înapoi. Altfel `PUT` trimitea `quantity: null` și greutatea se
 * ștergea (aceeași regulă ca `weightRecorded` din formularul web).
 */
export function weightRecorded(
  weighed: boolean,
  original: { weighedAtUnloading: boolean; quantity: number | null } | undefined
): boolean {
  return weighed && original?.weighedAtUnloading === true && original.quantity != null;
}

/**
 * „Ai pus TU ambalajul pe piață?” — pe o predare nouă pornește cu „nu”; pe una veche fără răspuns
 * rămâne `null` până o atinge cineva. Serverul socotește `null` drept „da” (Anexa 1 Ambalaje), deci
 * un `false` pus tăcut ar scoate cantitatea dintr-o declarație deja tipărită.
 */
export function initialPackagingOnMarket(original: { packagingOnMarket: boolean | null } | undefined): boolean | null {
  return original ? original.packagingOnMarket : false;
}

/**
 * Anul predării, între 2000 și zece ani de acum — aceeași margine ca `YearRangeGuard` pe server. Fără ea,
 * o dată tastată „14.09.0206” intra în coadă și ieșea „refuzată” abia la trimitere.
 */
export function yearInRange(isoDate: string, today: Date = new Date()): boolean {
  const year = Number(isoDate.slice(0, 4));
  return year >= 2000 && year <= today.getFullYear() + 10;
}

/**
 * Codul R/D e printre cele declarate de firmă (`validateAgainstProfile`). „La fel ca data trecută” poate
 * aduce un cod scos între timp din profil; un profil gol înseamnă necompletat, nu „nimic voie”.
 */
export function codeInProfile(code: string, profileCodes: readonly string[]): boolean {
  return !code || profileCodes.length === 0 || profileCodes.includes(code);
}

/**
 * O predare refuzată din coadă, redeschisă în formular („Corectează” pe rândul refuzat): cererea salvată
 * are aceleași nume de rubrici ca predarea, iar codul de deșeu își ia numele din profilul firmei. Înainte
 * singura ieșire era „Scoate”, care arunca tot ce scrisese omul, cu poza.
 */
export function queuedToMovement(
  payload: Record<string, unknown>,
  summary: { wasteCode: string },
  profileCodes: readonly WasteCode[],
): WasteMovement {
  const code = profileCodes.find((c) => c.id === payload.wasteCodeId);
  return {
    ...(payload as Partial<WasteMovement>),
    wasteCode: code?.code ?? summary.wasteCode,
    wasteCodeName: code?.name ?? "",
    hazardous: code?.hazardous ?? false,
  } as WasteMovement;
}
