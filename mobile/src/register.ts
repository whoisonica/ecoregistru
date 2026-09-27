import type { WasteMovement } from "@/lib/types";

/**
 * Socotelile tabului „Generare” ca registru al lunii (F6, valul B), fără React, ca să se probeze cu
 * `npm run test:register`. Ecranul e `components/MovementList.tsx`.
 */

export type Month = { year: number; month: number };

/** Ultimele `n` luni, cea curentă întâi — șirul de luni de răsfoit. */
export function monthsBack(now: Date, n = 12): Month[] {
  return Array.from({ length: n }, (_, i) => {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
    return { year: d.getFullYear(), month: d.getMonth() + 1 };
  });
}

/**
 * Lunile goale din șir (index în `rows`, cea curentă la 0): zero mișcări, **după prima lună cu ceva**
 * și înainte de luna curentă — aceeași regulă ca `emptyMonths` de pe webul Acasă. O firmă care a început
 * în mai n-are în față șapte luni galbene care nu spun nimic despre ea; luna curentă e încă în lucru.
 * O lună încă neîncărcată (`undefined`) nu e goală.
 */
export function gapMonths(rows: (number | undefined)[]): Set<number> {
  const out = new Set<number>();
  const oldest = rows.findLastIndex((r) => (r ?? 0) > 0);
  for (let i = 1; i < oldest; i++) if (rows[i] === 0) out.add(i);
  return out;
}

/** Cantitatea unui rând în kg; un rând care așteaptă cântarul nu adaugă nimic (ca `/movements/totals`). */
export function kgOf(m: Pick<WasteMovement, "quantity" | "unit">): number {
  if (m.quantity == null) return 0;
  return m.unit === "TONS" ? m.quantity * 1000 : m.quantity;
}

/** A plecat fără cod R/D — aceeași condiție ca `missingOperationCode` din `MovementQueryService`. */
export function missingCode(m: Pick<WasteMovement, "operation">): boolean {
  return m.operation === "UNCLASSIFIED_OUT";
}

export interface Day<T> {
  date: string;
  kg: number;
  rows: T[];
}

/** Rândurile pe zile, în ordinea în care vin de la server (data descrescător), cu totalul zilei. */
export function byDay<T extends Pick<WasteMovement, "date" | "quantity" | "unit">>(rows: T[]): Day<T>[] {
  const days: Day<T>[] = [];
  for (const r of rows) {
    const last = days[days.length - 1];
    if (last && last.date === r.date) {
      last.rows.push(r);
      last.kg += kgOf(r);
    } else days.push({ date: r.date, kg: kgOf(r), rows: [r] });
  }
  return days;
}

export interface Share {
  code: string;
  name: string;
  hazardous: boolean;
  kg: number;
}

/** Compoziția lunii pe coduri, cele mai mari întâi; codurile fără kg nu intră (n-au lățime pe bară). */
export function composition(rows: Pick<WasteMovement, "wasteCode" | "wasteCodeName" | "hazardous" | "quantity" | "unit">[]): Share[] {
  const by = new Map<string, Share>();
  for (const r of rows) {
    const s = by.get(r.wasteCode) ?? { code: r.wasteCode, name: r.wasteCodeName, hazardous: r.hazardous, kg: 0 };
    s.kg += kgOf(r);
    by.set(r.wasteCode, s);
  }
  return [...by.values()].filter((s) => s.kg > 0).sort((a, b) => b.kg - a.kg || a.code.localeCompare(b.code));
}
