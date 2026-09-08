import { useMemo, useState, type ReactNode } from "react";
import { Select } from "@/components/ui/select";
import { strings } from "@/lib/strings";

const t = strings.common;

export type ActiveFilterValue = "active" | "inactive" | "all";

/**
 * Filtrul activ / inactiv de deasupra unui tabel cu rânduri care se dezactivează.
 *
 * <p>Dezactivarea nu șterge nimic — dinadins, fiindcă rândul e citat de mișcări vechi — dar asta
 * înseamnă că lista crește la nesfârșit. După un an de folosire, „Parteneri" e un cimitir prin
 * care se caută, cu jumătate din rânduri nefolosibile și nimic care să le dea la o parte.
 *
 * <p>Pornește pe **Active**, adică pe ce se poate alege azi. Nu ascunde nimic pe furiș: filtrul e
 * chiar lângă căutare, cu numărul celor scoase, iar cine caută un rând vechi îl găsește dintr-un
 * clic. Și **nu apare deloc** cât timp n-a fost dezactivat nimic — pe un cont nou ar fi un
 * comutator între „tot" și „tot".
 */
export function useActiveFilter<T extends { active: boolean }>(rows: T[]): {
  rows: T[];
  control: ReactNode;
} {
  const [value, setValue] = useState<ActiveFilterValue>("active");
  const inactiveCount = useMemo(() => rows.filter((r) => !r.active).length, [rows]);

  const filtered = useMemo(() => {
    if (value === "all" || inactiveCount === 0) return rows;
    return rows.filter((r) => (value === "active" ? r.active : !r.active));
  }, [rows, value, inactiveCount]);

  const control =
    inactiveCount === 0 ? null : (
      <Select
        aria-label={t.stateFilter}
        value={value}
        onChange={(ev) => setValue(ev.target.value as ActiveFilterValue)}
        className="h-10 w-auto"
      >
        <option value="active">{t.stateActive}</option>
        <option value="inactive">{t.stateInactive.replace("{n}", String(inactiveCount))}</option>
        <option value="all">{t.stateAll}</option>
      </Select>
    );

  return { rows: filtered, control };
}
