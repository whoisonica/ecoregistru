import { Receipt } from "lucide-react";
import { useBillingAccount } from "@/hooks/useSubscriptions";
import type { InvoiceStatus, SubscriptionStatus } from "@/lib/types";
import { strings } from "@/lib/strings";
import { formatDate } from "@/lib/utils";
import { PageHeader } from "@/components/ui/page-header";
import { Badge } from "@/components/ui/badge";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { TableFallbackRow } from "@/components/ui/table-fallback";

const t = strings.billing;
const s = strings.subscriptions;

const STATUS_BADGE: Record<SubscriptionStatus, "muted" | "warning" | "success" | "danger"> = {
  PENDING: "warning",
  ACTIVE: "success",
  PAST_DUE: "danger",
  READ_ONLY: "danger",
  CANCELLED: "muted",
};

const INVOICE_BADGE: Record<InvoiceStatus, "muted" | "warning" | "success"> = {
  DRAFT: "muted",
  ISSUED: "warning",
  PAID: "success",
};

function lei(n: number) {
  return `${n.toLocaleString("ro-RO", { maximumFractionDigits: 2 })} lei`;
}

/**
 * Plata abonamentelor, F2 — abonamentul și facturile, pentru cine le plătește: administratorul unei
 * firme directe sau consultantul, pentru cabinet. Numai citire: pachetul și datele de facturare le
 * pune platforma, din Clienți.
 *
 * <p>Nu cere o firmă aleasă: abonamentul unui consultant e al cabinetului, nu al firmei din comutator.
 */
export function BillingPage() {
  const { data: account, isLoading, isError } = useBillingAccount();

  return (
    <div>
      <PageHeader title={t.title} description={t.subtitle} />

      {isError && <p className="mt-6 text-sm text-red-600">{t.loadError}</p>}
      {isLoading && <p className="mt-6 text-sm text-content-muted">{strings.common.loading}</p>}
      {account === null && <p className="mt-6 text-sm text-content-muted">{t.none}</p>}

      {account && (
        <>
          <div className="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-2">
            <section className="rounded-md border border-line bg-surface p-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h2 className="text-lg font-semibold text-content">{account.clientName}</h2>
                <Badge variant={STATUS_BADGE[account.status]}>{s.status[account.status]}</Badge>
              </div>
              <dl className="mt-3 space-y-1 text-sm">
                <Row label={t.plan}>
                  {s.plans[account.plan]}
                  {account.founder && <span className="text-content-muted"> · {t.founder}</span>}
                </Row>
                <Row label={t.startedAt}>{formatDate(account.startedAt)}</Row>
              </dl>
              <p className="mt-3 text-xs text-content-muted">{t.payment}</p>
            </section>

            <section className="rounded-md border border-line bg-surface p-4">
              <h2 className="text-sm font-medium text-content-strong">
                {t.nextInvoice
                  .replace("{from}", formatDate(account.nextInvoice.from))
                  .replace("{to}", formatDate(account.nextInvoice.to))}
              </h2>
              <ul className="mt-2 space-y-1 text-sm">
                {account.nextInvoice.lines.map((line, i) => (
                  <li key={i} className="flex justify-between gap-3">
                    <span>
                      {line.label}
                      {line.quantity > 1 && ` × ${line.quantity}`}
                    </span>
                    <span className="tabular-nums">{lei(line.amount)}</span>
                  </li>
                ))}
                <li className="flex justify-between gap-3 border-t border-line pt-1 font-medium text-content-strong">
                  <span>{s.total}</span>
                  <span className="tabular-nums">{lei(account.nextInvoice.total)}</span>
                </li>
              </ul>
              <p className="mt-3 text-xs text-content-muted">{t.nextInvoiceHint}</p>
            </section>
          </div>

          <section className="mt-4 rounded-md border border-line bg-surface p-4">
            <h2 className="text-sm font-medium text-content-strong">{t.billingData}</h2>
            <dl className="mt-2 space-y-1 text-sm">
              <Row label={t.billingEmail}>{account.billingEmail ?? "—"}</Row>
              <Row label={t.billingAddress}>
                {[account.billingAddress, account.billingCity, account.billingCounty].filter(Boolean).join(", ") ||
                  "—"}
              </Row>
            </dl>
            <p className="mt-3 text-xs text-content-muted">{t.billingDataHint}</p>
          </section>

          <section className="mt-8">
            <h2 className="mb-3 text-lg font-semibold text-content">{t.invoices}</h2>
            <Table>
              <THead>
                <TR>
                  <TH>{t.number}</TH>
                  <TH>{t.period}</TH>
                  <TH className="text-right">{s.total}</TH>
                  <TH>{strings.common.status}</TH>
                  <TH>{t.due}</TH>
                  <TH className="text-right">{t.document}</TH>
                </TR>
              </THead>
              <TBody>
                {account.invoices.length === 0 && (
                  <TableFallbackRow columns={6} loading={false} icon={Receipt} title={t.noInvoices} />
                )}
                {account.invoices.map((invoice) => (
                  <TR key={invoice.id}>
                    <TD className="font-medium text-content">
                      {invoice.fgoSerie} {invoice.fgoNumar}
                    </TD>
                    <TD>
                      {formatDate(invoice.periodStart)} – {formatDate(invoice.periodEnd)}
                    </TD>
                    <TD className="text-right tabular-nums">{lei(invoice.total)}</TD>
                    <TD>
                      <Badge variant={INVOICE_BADGE[invoice.status]}>{s.invoiceStatus[invoice.status]}</Badge>
                    </TD>
                    <TD>
                      {invoice.status === "PAID" && invoice.paidAt
                        ? t.paidOn.replace("{date}", formatDate(invoice.paidAt))
                        : formatDate(invoice.dueDate)}
                    </TD>
                    <TD className="text-right">
                      <span className="inline-flex gap-3">
                        {invoice.status === "ISSUED" && invoice.fgoLinkPlata && (
                          <a href={invoice.fgoLinkPlata} target="_blank" rel="noreferrer" className="text-brand underline">
                            {t.pay}
                          </a>
                        )}
                        {invoice.fgoLink && (
                          <a href={invoice.fgoLink} target="_blank" rel="noreferrer" className="text-brand underline">
                            {s.invoicePdf}
                          </a>
                        )}
                      </span>
                    </TD>
                  </TR>
                ))}
              </TBody>
            </Table>
          </section>
        </>
      )}
    </div>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-wrap gap-x-3">
      <dt className="w-32 shrink-0 text-content-muted">{label}</dt>
      <dd className="min-w-0 text-content">{children}</dd>
    </div>
  );
}
