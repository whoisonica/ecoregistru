import type { Deadline } from "@/lib/types";

/**
 * „Bifează” pe telefon (F5, valul B; decizia D9 din 28.09.2026: da, cu numărul de înregistrare) — aceeași
 * regulă ca butonul „Marchează finalizat” de pe web (`DeadlinesPage`): cine scrie, pe un termen salvat
 * (un rând „socotit” de pe „Trecute” n-are id) și încă nebifat. Serverul cere oricum `CAN_WRITE`.
 */
export function canCompleteOnPhone(d: Pick<Deadline, "id" | "status" | "computed">, writer: boolean): boolean {
  return writer && !!d.id && !d.computed && d.status !== "DONE";
}

/**
 * „În calendar” (F9, valul B): evenimentul pentru formularul de calendar al sistemului — o zi întreagă, în
 * ziua termenului, cu memento cu trei zile înainte (ca fișierul `.ics` de pe web, `deadlinesIcs`).
 * Data se ia în ora telefonului: „25 februarie” e 25 februarie oriunde stă omul.
 */
export function deadlineEvent(d: Pick<Deadline, "dueDate">, label: string) {
  const [y, m, day] = d.dueDate.split("-").map(Number);
  const start = new Date(y, m - 1, day);
  return {
    title: label,
    startDate: start,
    endDate: start,
    allDay: true,
    notes: "WasteHouse",
    // Evenimentul e de o zi întreagă (de la miezul nopții): memento la 9 dimineața, cu trei zile înainte.
    alarms: [{ relativeOffset: -3 * 24 * 60 + 9 * 60 }],
  };
}
