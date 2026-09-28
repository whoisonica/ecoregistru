import { createEventInCalendarAsync } from "expo-calendar/legacy";
import type { Deadline } from "@/lib/types";

import { deadlineEvent } from "./deadlineRules";

/**
 * F9: formularul de eveniment al sistemului, precompletat cu termenul — omul îl vede și apasă el „Adaugă”.
 * Fără acces la calendar (iOS 17+ și Android nu-l cer pentru formular), deci fără permisiune nouă la
 * magazin. `expo-calendar/legacy`, fiindcă API-ul nou (`addEventWithForm`) cere acces la calendar.
 *
 * @return `true` dacă evenimentul a fost salvat
 */
export async function addDeadlineToCalendar(d: Pick<Deadline, "dueDate">, label: string): Promise<boolean> {
  const result = await createEventInCalendarAsync(deadlineEvent(d, label));
  return result.action === "saved" || result.action === "done";
}
