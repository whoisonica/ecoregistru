import { closedMonth } from "@/lib/dates";
import { strings } from "@/lib/strings";

/** D2 (28.09.2026) — „august 2026” pentru o zi dintr-o lună încheiată, altfel `null`. */
export function closedMonthLabel(iso: string | null | undefined): string | null {
  const m = closedMonth(iso);
  return m ? `${strings.months[m.month - 1].toLowerCase()} ${m.year}` : null;
}
