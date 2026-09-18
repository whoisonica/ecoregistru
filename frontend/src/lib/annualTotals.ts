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
}

/**
 * Liniile lunare, strânse pe cod de deșeu.
 *
 * <p>⚠️ **Fără stoc, dinadins** (proprietarul, 18.09.2026: „scoate «în stoc», oamenii nu au stoc la
 * generatori"). E aceeași decizie ca la `V58` (16.09.2026): un generator n-are cântar și nu ține
 * stoc — deșeul stă în pubelă până vine colectorul, iar cantitatea se află abia la predare, de pe
 * tichetul lui. De aceea serverul refuză o generare fără ieșire, generarea e cea dedusă din predare
 * (`V24`), iar `stoc = stoc_anterior + generat − valorificat − eliminat` iese **zero prin
 * construcție** pe Anexa 1. O coloană de zerouri nu e o informație. Coloana „rămasă în stoc" rămâne
 * unde o cere actul: pe fișa tipărită (HG 856/2002, anexa 1, cap. 1) și pe centralizator.
 */
export function byCode(rows: MonthlyEvidence[]): CodeTotals[] {
  const acc = new Map<string, CodeTotals>();

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
    };
    entry.generated += r.totalGenerated;
    entry.recovered += r.totalRecovered;
    entry.disposed += r.totalDisposed;
    entry.unclassified += r.totalUnclassifiedOut;
    if (r.awaitingWeighing) entry.awaiting += 1;
    acc.set(r.wasteCode, entry);
  }

  return [...acc.values()].sort((a, b) => a.wasteCode.localeCompare(b.wasteCode, "ro"));
}
