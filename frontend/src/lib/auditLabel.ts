import { strings } from "./strings";
import { formatDate } from "./dates";

const e = strings.enums;

/**
 * Eticheta unui rând din „Istoric”, pe românește (29.09.2026).
 *
 * <p>Backendul scrie eticheta termenului ca `SIM_ANNUAL · 2027-03-15` și pe a cifrelor de ambalaje ca
 * `2026 · HARTIE_CARTON` — nume de enum, bune pentru căutare, nu pentru citit. Aici devin „Evidența gestiunii
 * deșeurilor generate (anual, 15 martie) · 15.03.2027” și „2026 · Plastic”. Orice altă etichetă trece
 * neatinsă.
 */
export function auditRowLabel(entityType: string, label: string | null | undefined): string {
  if (!label) return "";
  const [head, tail] = label.split(" · ");
  if (tail === undefined) return label;
  if (entityType === "ReportingDeadline") {
    const type = e.reportType[head as keyof typeof e.reportType] ?? head;
    return `${type} · ${formatDate(tail) || tail}`;
  }
  if (entityType === "PackagingMarketEntry") {
    return `${head} · ${e.packagingMaterial[tail as keyof typeof e.packagingMaterial] ?? tail}`;
  }
  return label;
}
