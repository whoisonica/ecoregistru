import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { CreditCard, Receipt } from "lucide-react";
import {
  useBillingAccount,
  useCardPayment,
  useChoosePaymentMethod,
  usePayInvoiceByCard,
} from "@/hooks/useSubscriptions";
import type { InvoiceStatus, PaymentMethod, SubscriptionStatus } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { formatDate } from "@/lib/utils";
import { PageHeader } from "@/components/ui/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { useToast } from "@/components/ui/toast";

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

/** Cât așteptăm notificarea Netopia înainte să spunem „nu plăti a doua oară". */
const SLOW_AFTER_MS = 60_000;

function lei(n: number) {
  return `${n.toLocaleString("ro-RO", { maximumFractionDigits: 2 })} lei`;
}

/**
 * Plata abonamentelor, F2 — abonamentul și facturile, pentru cine le plătește: administratorul unei
 * firme directe sau consultantul, pentru cabinet. Pachetul și datele de facturare le pune platforma.
 *
 * <p>F3 — cardul sau transferul, și „Plătește cu cardul" pe fiecare factură emisă. Netopia întoarce omul
 * aici cu `?plata=<id>`; redirecționarea vine de obicei înaintea notificării, deci plata se citește din nou
 * până se hotărăște.
 *
 * <p>Nu cere o firmă aleasă: abonamentul unui consultant e al cabinetului, nu al firmei din comutator.
 */
export function BillingPage() {
  const { data: account, isLoading, isError } = useBillingAccount();
  const chooseMut = useChoosePaymentMethod();
  const payMut = usePayInvoiceByCard();
  const { notify } = useToast();

  async function choose(method: PaymentMethod) {
    try {
      await chooseMut.mutateAsync(method);
      notify(t.methodSaved, "success");
    } catch (err) {
      notify(apiErrorMessage(err, t.methodError), "error");
    }
  }

  async function payByCard(invoiceId: string) {
    try {
      const url = await payMut.mutateAsync(invoiceId);
      window.location.assign(url);
    } catch (err) {
      notify(apiErrorMessage(err, t.payByCardError), "error");
    }
  }

  const method = account?.paymentMethod ?? "TRANSFER";

  return (
    <div>
      <PageHeader title={t.title} description={t.subtitle} />

      <CardReturnNotice />

      {isError && <p className="mt-6 text-sm text-red-600">{t.loadError}</p>}
      {isLoading && <p className="mt-6 text-sm text-content-muted">{strings.common.loading}</p>}
      {account === null && <p className="mt-6 text-sm text-content-muted">{t.none}</p>}

      {account && (
        <>
          {(account.endsOn || account.readOnlyOn) && (
            <div className="mt-6 space-y-1 rounded-md border border-amber-300 bg-amber-50 px-4 py-3 text-sm text-amber-900">
              {account.endsOn && <p>{t.endsOn.replace("{date}", formatDate(account.endsOn))}</p>}
              {account.readOnlyOn && <p>{t.readOnlyOn.replace("{date}", formatDate(account.readOnlyOn))}</p>}
            </div>
          )}

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

              <fieldset className="mt-4 space-y-2">
                <legend className="text-sm font-medium text-content-strong">{t.method}</legend>
                <MethodOption
                  checked={method === "CARD"}
                  disabled={chooseMut.isPending || !account.cardPaymentAvailable}
                  onChange={() => choose("CARD")}
                  label={t.methodCard}
                  hint={t.methodCardHint}
                />
                <MethodOption
                  checked={method === "TRANSFER"}
                  disabled={chooseMut.isPending}
                  onChange={() => choose("TRANSFER")}
                  label={t.methodTransfer}
                  hint={t.methodTransferHint}
                />
                {account.cardPanMasked && (
                  <p className="flex items-center gap-2 text-xs text-content-muted">
                    <CreditCard className="h-3.5 w-3.5" aria-hidden />
                    {t.savedCard
                      .replace("{pan}", account.cardPanMasked)
                      .replace("{expiry}", account.cardExpiry ?? "—")}
                  </p>
                )}
              </fieldset>
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
                      {invoice.status === "ISSUED" && invoice.lastCardError && (
                        <span className="mt-1 block text-xs text-red-600">
                          {t.cardRefused.replace("{reason}", invoice.lastCardError)}
                        </span>
                      )}
                    </TD>
                    <TD className="text-right tabular-nums">{lei(invoice.total)}</TD>
                    <TD>
                      <Badge variant={INVOICE_BADGE[invoice.status]}>{s.invoiceStatus[invoice.status]}</Badge>
                    </TD>
                    <TD>
                      {invoice.status === "PAID" && invoice.paidAt
                        ? `${t.paidOn.replace("{date}", formatDate(invoice.paidAt))}${
                            invoice.paidBy === "CARD" ? ` ${t.paidByCard}` : ""
                          }`
                        : formatDate(invoice.dueDate)}
                    </TD>
                    <TD className="text-right">
                      <span className="inline-flex flex-wrap items-center justify-end gap-3">
                        {invoice.status === "ISSUED" && account.cardPaymentAvailable && (
                          <Button size="sm" onClick={() => payByCard(invoice.id)} disabled={payMut.isPending}>
                            {t.payByCard}
                          </Button>
                        )}
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

/** Întoarcerea de la Netopia: plătită, refuzată, sau încă fără notificare. */
function CardReturnNotice() {
  const [params, setParams] = useSearchParams();
  const paymentId = params.get("plata");
  const { data: payment } = useCardPayment(paymentId);
  const qc = useQueryClient();
  const [slow, setSlow] = useState(false);

  useEffect(() => {
    if (!paymentId) return;
    setSlow(false);
    const timer = window.setTimeout(() => setSlow(true), SLOW_AFTER_MS);
    return () => window.clearTimeout(timer);
  }, [paymentId]);

  const settled = payment?.status === "PAID" || payment?.status === "FAILED";
  useEffect(() => {
    if (settled) qc.invalidateQueries({ queryKey: ["billing"] });
  }, [settled, qc]);

  if (!paymentId || !payment) return null;

  const tone =
    payment.status === "PAID"
      ? "border-green-300 bg-green-50 text-green-900"
      : payment.status === "FAILED"
        ? "border-red-300 bg-red-50 text-red-900"
        : "border-line bg-surface text-content";
  const text =
    payment.status === "PAID"
      ? t.returnPaid
      : payment.status === "FAILED"
        ? t.returnFailed.replace("{reason}", payment.error ?? "—")
        : slow
          ? t.returnSlow
          : t.returnPending;

  return (
    <div role="status" className={`mt-6 flex flex-wrap items-center gap-3 rounded-md border px-4 py-3 text-sm ${tone}`}>
      <span className="min-w-0 flex-1">{text}</span>
      {settled && (
        <button
          type="button"
          className="text-xs underline"
          onClick={() => {
            params.delete("plata");
            setParams(params, { replace: true });
          }}
        >
          {strings.common.close}
        </button>
      )}
    </div>
  );
}

function MethodOption({
  checked,
  disabled,
  onChange,
  label,
  hint,
}: {
  checked: boolean;
  disabled: boolean;
  onChange: () => void;
  label: string;
  hint: string;
}) {
  return (
    <label className="flex items-start gap-2 text-sm text-content-strong">
      <input
        type="radio"
        name="payment-method"
        className="mt-0.5 h-4 w-4 border-line-strong text-brand focus:ring-brand"
        checked={checked}
        disabled={disabled}
        onChange={onChange}
      />
      <span>
        {label}
        <span className="block text-xs text-content-muted">{hint}</span>
      </span>
    </label>
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
