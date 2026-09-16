/**
 * M1b — ce se poate citi de pe un aviz de însoțire a mărfii, din rândurile de text pe care le dă
 * recunoașterea de pe telefon (ML Kit pe Android, Vision pe iOS).
 *
 * <p>Cod pur, fără niciun import, ca să se testeze cu `node --test` (vezi `parse.test.ts`).
 *
 * <p>**Nimic de aici nu intră în registru singur.** Fiecare câmp e o propunere, cu rândul din care
 * vine, pe care omul o confirmă pe ecran. De asta regulile aleg să tacă mai degrabă decât să
 * ghicească:
 * - codul de deșeu se potrivește **numai** cu codurile firmei — un șir de șase cifre de pe aviz
 *   nu devine cod doar fiindcă are forma lui;
 * - partenerul se caută după CUI în lista firmei; un CUI necunoscut se propune numai dacă cifra lui
 *   de control e corectă, fiindcă atunci merită o căutare la ANAF;
 * - CUI-ul firmei însăși (emitentul avizului) nu e niciodată partener.
 */

export type AvizUnit = "KG" | "TONS";

/** O valoare citită și rândul din care vine — ecranul îl arată lângă câmp. */
export interface Read<T> {
  value: T;
  source: string;
}

export interface AvizReading {
  /** Id-ul partenerului din lista firmei, când CUI-ul citit e al lui. */
  partnerId?: Read<string>;
  /** Un CUI valid care nu e în lista firmei: de căutat la ANAF și, la cerere, de adăugat. */
  unknownCui?: Read<string>;
  /** Codul, exact cum îl scrie nomenclatorul („15 01 01”). */
  wasteCode?: Read<string>;
  quantity?: Read<{ amount: number; unit: AvizUnit }>;
  documentNumber?: Read<string>;
  /** yyyy-MM-dd */
  date?: Read<string>;
  vehicle?: Read<string>;
}

export interface AvizContext {
  /** CUI-ul firmei care predă — emitentul avizului, deci niciodată partenerul. */
  ownCui: string | null | undefined;
  /** Codurile de deșeu ale firmei („15 01 01”). Numai acestea pot fi citite. */
  wasteCodes: string[];
  partners: { id: string; cui: string | null }[];
}

export function parseAviz(lines: string[], ctx: AvizContext): AvizReading {
  // Vision (iOS) dă rânduri; ML Kit (Android) dă blocuri, cu rândurile despărțite de „\n”.
  const clean = lines
    .flatMap((l) => l.split(/\r?\n/))
    .map((l) => l.replace(/\s+/g, " ").trim())
    .filter(Boolean);
  const reading: AvizReading = {};

  const cuis = findCuis(clean, ctx);
  if (cuis.partner) reading.partnerId = cuis.partner;
  else if (cuis.unknown) reading.unknownCui = cuis.unknown;

  const code = findWasteCode(clean, ctx.wasteCodes);
  if (code) reading.wasteCode = code;
  const quantity = findQuantity(clean);
  if (quantity) reading.quantity = quantity;
  const number = findDocumentNumber(clean);
  if (number) reading.documentNumber = number;
  const date = findDate(clean);
  if (date) reading.date = date;
  const vehicle = findVehicle(clean);
  if (vehicle) reading.vehicle = vehicle;
  return reading;
}

// ── CUI ──────────────────────────────────────────────────────────────────────

/** Doar cifrele, fără „RO” — așa se compară un CUI scris în orice fel. */
export function cuiDigits(cui: string | null | undefined): string {
  return (cui ?? "").replace(/^\s*RO/i, "").replace(/\D/g, "");
}

/**
 * Cifra de control a codului de identificare fiscală: cheia 753217532 pe cifrele fără ultima,
 * aliniate la dreapta; suma ori 10, modulo 11, iar 10 devine 0.
 */
export function isValidCui(cui: string): boolean {
  const digits = cuiDigits(cui);
  if (digits.length < 2 || digits.length > 10) return false;
  const key = "753217532";
  const body = digits.slice(0, -1).padStart(9, "0");
  let sum = 0;
  for (let i = 0; i < 9; i++) sum += Number(body[i]) * Number(key[i]);
  const control = ((sum * 10) % 11) % 10;
  return control === Number(digits[digits.length - 1]);
}

const CUI_LABEL = /\b(C\.?\s?U\.?\s?I|C\.?\s?I\.?\s?F|cod\s+fiscal|cod\s+de\s+identificare)\b/i;

function findCuis(lines: string[], ctx: AvizContext) {
  const own = cuiDigits(ctx.ownCui);
  const byDigits = new Map(ctx.partners.filter((p) => cuiDigits(p.cui)).map((p) => [cuiDigits(p.cui), p.id] as const));
  let partner: Read<string> | undefined;
  let unknown: Read<string> | undefined;

  for (const line of lines) {
    const candidates: string[] = [];
    // „RO12345678” se ia oriunde — și „R0…”, fiindcă recunoașterea citește des O-ul ca zero; un număr
    // gol, numai pe un rând care spune că e cod fiscal — altfel orice număr de aviz ar fi fost un CUI.
    for (const m of line.matchAll(/\bR[O0]\s?(\d{2,10})\b/gi)) candidates.push(m[1]);
    if (CUI_LABEL.test(line)) {
      for (const m of line.matchAll(/(?<![\d/])(\d{2,10})(?![\d/])/g)) candidates.push(m[1]);
    }
    for (const digits of candidates) {
      if (digits === own) continue;
      const id = byDigits.get(digits);
      if (id && !partner) partner = { value: id, source: line };
      else if (!id && !unknown && isValidCui(digits)) unknown = { value: digits, source: line };
    }
  }
  return { partner, unknown };
}

// ── codul de deșeu ───────────────────────────────────────────────────────────

/**
 * „15 01 01”, „150101”, „15.01.01”, cu sau fără „*”. Separatorul e același de două ori (`\2`), ca
 * „12.09.2026” să nu fie citit „09 20 26”.
 */
const CODE = /(?<![\d.])(\d{2})( ?|\.)(\d{2})\2(\d{2})\*?(?![\d.])/g;

function findWasteCode(lines: string[], wasteCodes: string[]): Read<string> | undefined {
  const known = new Map(wasteCodes.map((c) => [c.replace(/\D/g, ""), c] as const));
  for (const line of lines) {
    for (const m of line.matchAll(CODE)) {
      const code = known.get(m[1] + m[3] + m[4]);
      if (code) return { value: code, source: line };
    }
  }
  return undefined;
}

// ── cantitatea ───────────────────────────────────────────────────────────────

/**
 * Un număr scris românește: virgula e zecimala, punctul urmat de exact trei cifre e despărțitorul de
 * mii. „1.250” = 1250, „1.250,5” = 1250,5, „1,25” = 1,25, „1.25” = 1,25.
 */
export function parseRoNumber(text: string): number | null {
  const t = text.replace(/\s/g, "");
  if (!/^\d[\d.,]*$/.test(t)) return null;
  let normalized: string;
  if (t.includes(",")) normalized = t.replace(/\./g, "").replace(",", ".");
  else if (/^\d{1,3}(\.\d{3})+$/.test(t)) normalized = t.replace(/\./g, "");
  else normalized = t;
  const n = Number(normalized);
  return Number.isFinite(n) && n > 0 ? n : null;
}

const NUMBER = String.raw`(\d{1,3}(?:[. ]\d{3})+(?:,\d+)?|\d+(?:[.,]\d+)?)`;
const WITH_UNIT = new RegExp(String.raw`(?<![\d.,])` + NUMBER + String.raw`\s*(kg|kilograme?|tone|tona|to|t)\b`, "i");

function findQuantity(lines: string[]): Read<{ amount: number; unit: AvizUnit }> | undefined {
  for (const line of lines) {
    // Codul de deșeu iese întâi din rând: în „17 04 05 870kg” spațiul ar fi lipit „05” de cantitate,
    // ca despărțitor de mii, și ar fi ieșit 5.870 kg.
    const m = line.replace(CODE, " ; ").match(WITH_UNIT);
    if (!m) continue;
    const amount = parseRoNumber(m[1]);
    if (amount == null) continue;
    return { value: { amount, unit: unitOf(m[2]) }, source: line };
  }
  return fromTable(lines);
}

function unitOf(text: string): AvizUnit {
  return /^k/i.test(text) ? "KG" : "TONS";
}

const UNIT_CELL = /^(kg|kilograme?|tone|tona|to|t)$/i;

/**
 * Tabelul avizului, citit celulă cu celulă: recunoașterea dă „kg” și „1.250” pe rânduri separate.
 * Cantitatea e primul număr singur pe rând **după** capul de coloană „Cantitate” — înaintea lui stă
 * numărul curent al rândului („1”), care are aceeași formă. Fără cap sau fără unitate: tace.
 */
function fromTable(lines: string[]): Read<{ amount: number; unit: AvizUnit }> | undefined {
  const head = lines.findIndex((l) => /^cantitat(e|ea)\b/i.test(l));
  const unit = lines.find((l) => UNIT_CELL.test(l));
  if (head < 0 || !unit) return undefined;
  for (const line of lines.slice(head + 1)) {
    const amount = /^[\d.,]+$/.test(line) ? parseRoNumber(line) : null;
    if (amount != null) return { value: { amount, unit: unitOf(unit) }, source: `${lines[head]} ${line} ${unit}` };
  }
  return undefined;
}

// ── numărul avizului ─────────────────────────────────────────────────────────

function findDocumentNumber(lines: string[]): Read<string> | undefined {
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    if (!/aviz|seria|serie/i.test(line)) continue;
    const series = line.match(/\bseri[ae]\s*:?\s*([A-Z]{1,6})\b(?!\s*\/)/i)?.[1];
    const number = line.match(/\b(?:nr|num[aă]r)\b\.?\s*:?\s*([A-Z]{0,6}[- ]?\d{1,10})\b/i)?.[1];
    if (number) {
      const value =
        series && !number.toUpperCase().startsWith(series.toUpperCase()) ? `${series.toUpperCase()} ${number}` : number;
      return { value: value.toUpperCase(), source: line };
    }
    // „Serie / număr aviz” cu valoarea pe rândul următor, cum îl tipărește chiar WasteHouse.
    if (/(seri[ae]|nr|num[aă]r)/i.test(line) && /aviz/i.test(line) && !/\d/.test(line)) {
      const next = lines[i + 1];
      if (next && /^[A-Z]{0,6}[- ]?\d{1,10}$/i.test(next))
        return { value: next.toUpperCase(), source: `${line} ${next}` };
    }
  }
  return undefined;
}

// ── data ─────────────────────────────────────────────────────────────────────

const DATE = /(?<!\d)(\d{1,2})[./-](\d{1,2})[./-](\d{4})(?!\d)/;

function toIso(m: RegExpMatchArray): string | null {
  const [day, month, year] = [Number(m[1]), Number(m[2]), Number(m[3])];
  if (year < 2000 || year > 2100 || month < 1 || month > 12) return null;
  const d = new Date(Date.UTC(year, month - 1, day));
  if (d.getUTCDate() !== day) return null;
  return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
}

/** Întâi data de pe un rând care o numește („Data emiterii”), apoi prima dată de pe aviz. */
function findDate(lines: string[]): Read<string> | undefined {
  const labelled = lines.filter((l) => /\bdata\b/i.test(l));
  for (const line of [...labelled, ...lines]) {
    // Autorizația partenerului și buletinul delegatului au și ele date; niciuna nu e a avizului.
    if (/expir|valabil|na[sș]ter/i.test(line)) continue;
    const m = line.match(DATE);
    const iso = m && toIso(m);
    if (iso) return { value: iso, source: line };
  }
  return undefined;
}

// ── numărul de înmatriculare ─────────────────────────────────────────────────

const COUNTIES =
  "AB|AR|AG|BC|BH|BN|BT|BV|BR|BZ|CS|CL|CJ|CT|CV|DB|DJ|GL|GR|GJ|HR|HD|IL|IS|IF|MM|MH|MS|NT|OT|PH|SM|SJ|SB|SV|TR|TM|TL|VS|VL|VN";
/** București are două sau trei cifre, celelalte județe două; apoi trei litere. */
const PLATE = new RegExp(String.raw`\b(?:(B)[ -]?(\d{2,3})|(${COUNTIES})[ -]?(\d{2}))[ -]?([A-Z]{3})\b`);

function findVehicle(lines: string[]): Read<string> | undefined {
  for (const line of lines) {
    const m = line.toUpperCase().match(PLATE);
    if (m) return { value: `${m[1] ?? m[3]} ${m[2] ?? m[4]} ${m[5]}`, source: line };
  }
  return undefined;
}
