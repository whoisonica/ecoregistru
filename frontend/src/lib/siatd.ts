import type { SiatdModule } from "./types";

/** Ordinea din Setări și din tab: de la termenul cel mai scurt la cel mai lung. */
export const SIATD_MODULES: SiatdModule[] = ["MUNICIPAL", "PACKAGING", "TYRE", "WEEE", "BATTERY"];

export interface SiatdDraftRow {
  on: boolean;
  /** yyyy-MM-dd, sau "" cât timp nu e completată. */
  from: string;
}

export type SiatdPayload =
  | { ok: true; enrolledFrom: Partial<Record<SiatdModule, string>> }
  | { ok: false; missing: SiatdModule[] };

/**
 * Cererea din Setări → SIATD: doar modulele bifate, fiecare cu data înrolării. Un modul bifat fără dată oprește salvarea —
 * termenul curge de la data aceea (art. 18 alin. (10)), deci fără ea termenele ar ieși greșite.
 */
export function siatdPayload(draft: Partial<Record<SiatdModule, SiatdDraftRow>>): SiatdPayload {
  const enrolledFrom: Partial<Record<SiatdModule, string>> = {};
  const missing: SiatdModule[] = [];
  for (const module of SIATD_MODULES) {
    const row = draft[module];
    if (!row?.on) continue;
    if (!row.from) missing.push(module);
    else enrolledFrom[module] = row.from;
  }
  return missing.length ? { ok: false, missing } : { ok: true, enrolledFrom };
}
