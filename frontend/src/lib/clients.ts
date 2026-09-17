import { isValidCui } from "@/lib/cui";
import { COMPANY_FIELDS, type CompanyField } from "@/lib/firstSteps";
import type { ClientOverview, Company } from "@/lib/types";

/**
 * F-B (todo-clienti-abonamente.md, 17.09.2026) — ce cere atenție la un client, citit din firmă și din rândul ei de
 * `/companies/overview`. Pur, ca să se probeze fără ecran: tabelul, filtrele și cifrele de sus citesc toate de aici,
 * deci „Cer atenție 5” și cele cinci rânduri de sub filtru nu pot ajunge să difere.
 */
export type ClientFilter = "ALL" | "ATTENTION" | "NO_SUBSCRIPTION" | "OVERDUE" | "FAILED" | "NO_USERS" | "CABINETS";

export type AttentionReason = "FAILED" | "OVERDUE" | "NO_USERS";

export interface ClientRow {
  company: Company;
  /** undefined = rândul n-a venit (lista de abonamente a căzut): nimic nu se deduce din lipsa lui. */
  overview: ClientOverview | undefined;
  failed: boolean;
  overdue: boolean;
  /** Zile peste scadență, doar când `overdue`. */
  overdueDays: number;
  noUsers: boolean;
  noSubscription: boolean;
  reasons: AttentionReason[];
  /** Rubricile de dosar goale; CUI-ul invalid e separat, fiindcă oprește facturarea. */
  gaps: CompanyField[];
  cuiInvalid: boolean;
}

function daysBetween(fromIso: string, toIso: string) {
  return Math.round((Date.parse(toIso) - Date.parse(fromIso)) / 86_400_000);
}

export function clientRow(company: Company, overview: ClientOverview | undefined, today: string): ClientRow {
  const invoice = overview?.lastInvoice ?? null;
  const failed = invoice?.status === "DRAFT" && Boolean(invoice.lastError);
  const pastDue = invoice?.status === "ISSUED" && invoice.dueDate != null && invoice.dueDate < today;
  const overdue =
    pastDue || overview?.subscriptionStatus === "PAST_DUE" || overview?.subscriptionStatus === "READ_ONLY";
  const noUsers = overview !== undefined && overview.userCount === 0;
  // O firmă de cabinet n-are abonament propriu dinadins: o plătește cabinetul.
  const noSubscription = overview !== undefined && !company.consultancyId && overview.subscriptionStatus == null;
  const reasons: AttentionReason[] = company.active
    ? [...(failed ? ["FAILED" as const] : []), ...(overdue ? ["OVERDUE" as const] : []), ...(noUsers ? ["NO_USERS" as const] : [])]
    : [];
  return {
    company,
    overview,
    failed,
    overdue,
    overdueDays: pastDue ? daysBetween(invoice!.dueDate!, today) : 0,
    noUsers,
    noSubscription,
    reasons,
    gaps: COMPANY_FIELDS.filter((f) => !(company[f] ?? "").trim()),
    cuiInvalid: !isValidCui(company.cui ?? ""),
  };
}

export const CLIENT_FILTERS: ClientFilter[] = [
  "ALL",
  "ATTENTION",
  "NO_SUBSCRIPTION",
  "OVERDUE",
  "FAILED",
  "NO_USERS",
  "CABINETS",
];

/** Filtrele de bani nu au sens la consultant: firmele lui le plătește cabinetul. */
export const CONSULTANT_FILTERS: ClientFilter[] = ["ALL", "ATTENTION", "NO_USERS"];

export const MATCHES: Record<ClientFilter, (r: ClientRow) => boolean> = {
  ALL: () => true,
  ATTENTION: (r) => r.reasons.length > 0,
  NO_SUBSCRIPTION: (r) => r.noSubscription,
  OVERDUE: (r) => r.overdue,
  FAILED: (r) => r.failed,
  NO_USERS: (r) => r.noUsers,
  CABINETS: (r) => Boolean(r.company.consultancyId),
};

/** Cele care cer atenție primele, apoi după nume. */
export function byAttention(a: ClientRow, b: ClientRow) {
  const attention = Number(b.reasons.length > 0) - Number(a.reasons.length > 0);
  return attention || a.company.name.localeCompare(b.company.name, "ro");
}
