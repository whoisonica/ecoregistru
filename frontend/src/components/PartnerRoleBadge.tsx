import { strings } from "@/lib/strings";
import { cn } from "@/lib/utils";

const r = strings.enums.partnerRole;

/** Just the two flags — so a movement row can use this without a full Partner. */
export interface PartnerRole {
  client: boolean;
  supplier: boolean;
}

/** The commercial role in one word, for a list read at a glance. */
export function partnerRoleLabel(p: PartnerRole): string {
  if (p.client && p.supplier) return r.both;
  if (p.client) return r.client;
  if (p.supplier) return r.supplier;
  return r.none;
}

/**
 * The colours the meeting asked for, and the reason they are these two: the role says which way
 * the invoice travels, so money coming in (a client, who buys our waste) is green and money going
 * out (a supplier, who invoices us for the service) is amber. A partner with no role yet is grey
 * and says so — V7 did not guess one, because nothing stored implies it.
 */
const ROLE_CLASSES = {
  client: "bg-emerald-100 text-emerald-800 ring-emerald-200",
  supplier: "bg-amber-100 text-amber-800 ring-amber-200",
  none: "bg-gray-100 text-gray-500 ring-gray-200",
} as const;

function Chip({ label, tone }: { label: string; tone: keyof typeof ROLE_CLASSES }) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ring-1 ring-inset",
        ROLE_CLASSES[tone]
      )}
    >
      {label}
    </span>
  );
}

/** One chip per role, so "both" reads as both rather than as a third thing. */
export function PartnerRoleBadge({ partner }: { partner: PartnerRole }) {
  if (!partner.client && !partner.supplier) {
    return <Chip label={r.none} tone="none" />;
  }
  return (
    <span className="inline-flex flex-wrap gap-1">
      {partner.client && <Chip label={r.client} tone="client" />}
      {partner.supplier && <Chip label={r.supplier} tone="supplier" />}
    </span>
  );
}
