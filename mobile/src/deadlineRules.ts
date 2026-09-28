import type { Deadline } from "@/lib/types";

/**
 * „Bifează” pe telefon (F5, valul B; decizia D9 din 28.09.2026: da, cu numărul de înregistrare) — aceeași
 * regulă ca butonul „Marchează finalizat” de pe web (`DeadlinesPage`): cine scrie, pe un termen salvat
 * (un rând „socotit” de pe „Trecute” n-are id) și încă nebifat. Serverul cere oricum `CAN_WRITE`.
 */
export function canCompleteOnPhone(d: Pick<Deadline, "id" | "status" | "computed">, writer: boolean): boolean {
  return writer && !!d.id && !d.computed && d.status !== "DONE";
}
