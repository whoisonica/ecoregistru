import { strings } from "@web/strings";

import { ago } from "./ago";

/** „actualizat acum 3 min”, din momentul ultimei încărcări reușite (`query.dataUpdatedAt`). */
export function agoText(updatedAt: number, now = Date.now()): string | null {
  const a = ago(updatedAt, now);
  if (!a) return null;
  const m = strings.mobile;
  switch (a.kind) {
    case "now":
      return m.updatedNow;
    case "minutes":
      return m.updatedMinutes(a.n);
    case "hours":
      return m.updatedHours(a.n);
    case "days":
      return m.updatedDays(a.n);
  }
}

/** „Sâmbătă, 27 septembrie” — ziua de pe capul ecranului. */
export function dayLine(date = new Date()) {
  return `${strings.weekdays[date.getDay()]}, ${date.getDate()} ${strings.months[date.getMonth()].toLowerCase()}`;
}

/** Salutul după ora telefonului. */
export function greeting(date = new Date()) {
  const h = date.getHours();
  const d = strings.dashboard;
  return h < 12 ? d.greetingMorning : h < 18 ? d.greetingDay : d.greetingEvening;
}
