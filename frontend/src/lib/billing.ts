import type { BillingAccount } from "@/lib/types";

type Invoice = BillingAccount["invoices"][number];

/** Ce arată afișajul de sus pe `/abonament`: ce e de plată, până când și de câte zile e restant. */
export interface AmountDue {
  state: "PAID_UP" | "DUE" | "OVERDUE";
  /** Facturile emise și neplătite, cea mai veche scadență întâi. */
  unpaid: Invoice[];
  total: number;
  /** Cea mai apropiată scadență; null când nu e nimic de plată. */
  dueDate: string | null;
  /** Zilele trecute de la cea mai veche scadență; 0 cât nu e restantă. */
  overdueDays: number;
}

const DAY_MS = 86_400_000;

function days(fromIso: string, toIso: string): number {
  return Math.round((Date.parse(`${toIso}T00:00:00Z`) - Date.parse(`${fromIso}T00:00:00Z`)) / DAY_MS);
}

/**
 * F-E — suma de plată, adunată din toate facturile emise și neplătite. Scadența de azi nu e încă restanță, ca pe
 * tabelul Clienți (`lib/clients.ts`). Sumele se adună în bani, nu în lei cu virgulă, ca 0,1 + 0,2 să nu scrie 0,30000000000000004.
 */
export function amountDue(invoices: Invoice[], today: string): AmountDue {
  const unpaid = invoices
    .filter((i) => i.status === "ISSUED")
    .sort((a, b) => (a.dueDate ?? "9999").localeCompare(b.dueDate ?? "9999"));
  if (unpaid.length === 0) return { state: "PAID_UP", unpaid, total: 0, dueDate: null, overdueDays: 0 };
  const total = unpaid.reduce((sum, i) => sum + Math.round(i.total * 100), 0) / 100;
  const dueDate = unpaid[0].dueDate;
  const overdueDays = dueDate ? Math.max(0, days(dueDate, today)) : 0;
  return { state: overdueDays > 0 ? "OVERDUE" : "DUE", unpaid, total, dueDate, overdueDays };
}

/** O factură emisă, trecută de scadență: pe rândul ei scrie „Restantă”, nu „De plată”. */
export function isOverdue(invoice: Invoice, today: string): boolean {
  return invoice.status === "ISSUED" && invoice.dueDate != null && invoice.dueDate < today;
}

/** Numărul facturii așa cum se scrie la detaliile plății: „WH 14”. */
export function invoiceNumber(invoice: Pick<Invoice, "fgoSerie" | "fgoNumar">): string {
  return [invoice.fgoSerie, invoice.fgoNumar].filter(Boolean).join(" ");
}

/** Ce se trece la detaliile transferului: numerele tuturor facturilor plătite deodată. */
export function transferReference(unpaid: Invoice[]): string {
  return unpaid.map(invoiceNumber).join(", ");
}

/** IBAN-ul în grupuri de câte patru, cum e tipărit pe factură; la copiere pleacă fără spații. */
export function formatIban(iban: string): string {
  return iban.replace(/\s+/g, "").replace(/(.{4})(?=.)/g, "$1 ");
}

/** Suma pentru banca din România: virgulă zecimală, fără separator de mii („1234,50”). */
export function amountForBank(total: number): string {
  return total.toFixed(2).replace(".", ",");
}

/** Emailul „arată a email” — serverul are ultimul cuvânt (`@Email`). */
export function looksLikeEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim());
}
