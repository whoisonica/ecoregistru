import type { MonthlyEvidence } from "@/lib/types";

export interface CodeTotals {
  wasteCode: string;
  wasteCodeName: string;
  hazardous: boolean;
  generated: number;
  recovered: number;
  disposed: number;
  unclassified: number;
  /** Câte linii ale codului așteaptă cântarul destinatarului — linii, nu kilograme. */
  awaiting: number;
  stock: number;
}

/**
 * Liniile lunare, strânse pe cod de deșeu.
 *
 * <p>⚠️ Stocul **nu** se adună peste luni: `closingStock` e deja cumulativ pe (punct de lucru, cod)
 * la finalul lunii lui, și poartă și ce a rămas din anii dinainte. Deci pentru fiecare pereche
 * (punct, cod) se ia stocul **ultimei** luni cu date, iar cele ale punctelor se adună între ele.
 * Adunarea tuturor lunilor ar fi numărat același kilogram de douăsprezece ori.
 */
export function byCode(rows: MonthlyEvidence[]): CodeTotals[] {
  const acc = new Map<string, CodeTotals>();
  const lastMonth = new Map<string, { month: number; stock: number }>();

  for (const r of rows) {
    const entry = acc.get(r.wasteCode) ?? {
      wasteCode: r.wasteCode,
      wasteCodeName: r.wasteCodeName,
      hazardous: r.hazardous,
      generated: 0,
      recovered: 0,
      disposed: 0,
      unclassified: 0,
      awaiting: 0,
      stock: 0,
    };
    entry.generated += r.totalGenerated;
    entry.recovered += r.totalRecovered;
    entry.disposed += r.totalDisposed;
    entry.unclassified += r.totalUnclassifiedOut;
    if (r.awaitingWeighing) entry.awaiting += 1;
    acc.set(r.wasteCode, entry);

    const key = `${r.wasteCode}|${r.workPointId}`;
    const seen = lastMonth.get(key);
    if (!seen || r.month > seen.month) lastMonth.set(key, { month: r.month, stock: r.closingStock });
  }

  for (const [key, { stock }] of lastMonth) {
    const code = key.slice(0, key.indexOf("|"));
    const entry = acc.get(code);
    if (entry) entry.stock += stock;
  }

  return [...acc.values()].sort((a, b) => a.wasteCode.localeCompare(b.wasteCode, "ro"));
}
