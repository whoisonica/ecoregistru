import { strings } from "@/lib/strings";
import type { SubscriptionPlan } from "@/lib/types";

/** Treapta după numărul de angajați. Se păstrează doar treapta, nu numărul. */
export type SizeTier = 1 | 2 | 3 | 4 | 5;

/** Intervalele din grilă, în ordinea treptelor. */
export const SIZE_TIERS: readonly { tier: SizeTier; range: string }[] = [
  { tier: 1, range: "0–2" },
  { tier: 2, range: "3–9" },
  { tier: 3, range: "10–19" },
  { tier: 4, range: "20–39" },
  { tier: 5, range: "40+" },
];

/** „Generator, treapta 2”; fără treaptă sau la preț personalizat doar numele pachetului. */
export function planLabel(plan: SubscriptionPlan, sizeTier: SizeTier | null, customPrice: boolean): string {
  const name = strings.subscriptions.plans[plan];
  if (sizeTier == null || customPrice) return name;
  return `${name}, ${strings.subscriptions.tierLabel.replace("{n}", String(sizeTier))}`;
}

/** „Generator · treapta 2 · 50 lei”; personalizat: „Generator · 42 lei · preț personalizat”; vechi: „Generator · 99 lei”. */
export function subscriptionSummary(
  plan: SubscriptionPlan,
  sizeTier: SizeTier | null,
  customPrice: boolean,
  price: string | null
): string {
  const name = strings.subscriptions.plans[plan];
  const parts: string[] = [name];
  if (customPrice) {
    if (price) parts.push(price);
    parts.push(strings.subscriptions.customPrice.toLowerCase());
  } else {
    if (sizeTier != null) parts.push(strings.subscriptions.tierLabel.replace("{n}", String(sizeTier)));
    if (price) parts.push(price);
  }
  return parts.join(" · ");
}

/** Prețul scris de mână, ca la server: peste zero, cel mult 8 cifre și 2 zecimale; altfel null. */
export function parseMonthlyPrice(text: string): number | null {
  const v = text.trim();
  if (!/^\d{1,8}(\.\d{1,2})?$/.test(v)) return null;
  const n = Number(v);
  return n > 0 ? n : null;
}
