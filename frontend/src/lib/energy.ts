import type { EnergyCarrier, EnergySheet, EnergyYearSummary } from "@/lib/types";
import { strings } from "@/lib/strings";
import { formatDate } from "@/lib/dates";

/** Rubricile Anexei 1, în ordinea ei; coeficientul tipărit pe Anexă (`null` = tep-ul îl scrie omul). */
export const ENERGY_CARRIERS: { id: EnergyCarrier; unit: string; coefficient: number | null }[] = [
  { id: "ELECTRICITY", unit: "MWh", coefficient: 0.086 },
  { id: "HEAT", unit: "Gcal", coefficient: 0.1 },
  { id: "NATURAL_GAS", unit: "MWh", coefficient: 0.086 },
  { id: "FUEL_OIL", unit: "t", coefficient: 0.95 },
  { id: "LIGHT_FUEL_OIL", unit: "t", coefficient: 0.97 },
  { id: "PETROL", unit: "t", coefficient: 1.05 },
  { id: "DIESEL", unit: "t", coefficient: 1.015 },
  { id: "COAL", unit: "t", coefficient: null },
  { id: "OTHER_FUEL", unit: "u.m.", coefficient: null },
  { id: "RENEWABLE_ELECTRICITY", unit: "MWh", coefficient: 0.086 },
  { id: "RENEWABLE_HEAT", unit: "Gcal", coefficient: 0.1 },
];

/** Ordinea formularului EfEnClima: cea a Anexei, dar „alți combustibili” după cele două regenerabile. */
export const EFENCLIMA_ORDER: EnergyCarrier[] = [
  ...ENERGY_CARRIERS.map((c) => c.id).filter((id) => id !== "OTHER_FUEL"),
  "OTHER_FUEL",
];

/** Luna dinaintea celei de azi (1–12); în ianuarie, decembrie din anul trecut. */
export function previousMonth(today: Date): { year: number; month: number } {
  const m = today.getMonth(); // 0–11
  return m === 0 ? { year: today.getFullYear() - 1, month: 12 } : { year: today.getFullYear(), month: m };
}

/** Aceeași regulă ca serverul: fiecare rubrică bifată are cantitate, iar cărbunele/alții și tep. */
export function monthComplete(sheet: EnergySheet, month: number): boolean {
  return sheet.carriers.every((carrier) => {
    const cell = sheet.cells.find((c) => c.carrier === carrier && c.month === month);
    const byHand = ENERGY_CARRIERS.find((c) => c.id === carrier)?.coefficient == null;
    return cell != null && cell.quantity != null && (!byHand || cell.tep != null);
  });
}

/** Structural egal cu `NextAction` din hooks: `lib/` nu importă din `hooks/` (îl citește și mobilul). */
export type EnergyAction = { tone: "warning"; title: string; hint: string; to: string; cta: string };

/**
 * Acțiunea de pe Acasă. `sheet` trebuie să fie fișa anului din `previousMonth(today)` (în ianuarie,
 * anul trecut). Fără fișă (cerere în curs sau căzută) nu spune nimic.
 */
export function energyAction(sheet: EnergySheet | undefined, today: Date): EnergyAction | null {
  if (!sheet) return null;
  const to = `/termene/energie?an=${sheet.year}`;
  const cta = strings.energy.title;
  if (sheet.carriers.length === 0) {
    return {
      tone: "warning",
      title: strings.energy.nextActionNoCarriersTitle,
      hint: strings.energy.nextActionNoCarriersHint,
      to,
      cta,
    };
  }
  const { month } = previousMonth(today);
  if (monthComplete(sheet, month)) return null;
  return {
    tone: "warning",
    title: strings.energy.nextActionMonthTitle.replace("{month}", strings.months[month - 1].toLowerCase()),
    hint: strings.energy.nextActionMonthHint,
    to,
    cta,
  };
}

/** Starea unui an în tabul „Energie” al dosarului: depusă, incompletă, nedepusă — în ordinea asta. */
export function energyYearStatus(s: EnergyYearSummary): { tone: "success" | "warning" | "muted"; label: string } {
  if (s.filedOn) {
    return { tone: "success", label: strings.energy.filedOn.replace("{date}", formatDate(s.filedOn)) };
  }
  if (s.monthsComplete < 12) {
    return { tone: "warning", label: strings.energy.incomplete.replace("{n}", String(s.monthsComplete)) };
  }
  return { tone: "muted", label: strings.energy.notFiled };
}
