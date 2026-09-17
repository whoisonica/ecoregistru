import { useEffect, useState, type FormEvent } from "react";
import { useSearchParams } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { Check, Copy, CreditCard, FileText, Receipt } from "lucide-react";
import {
  useBillingAccount,
  useCardPayment,
  useCheckBillingPayment,
  useChoosePaymentMethod,
  usePayInvoiceByCard,
  useUpdateBillingDetails,
} from "@/hooks/useSubscriptions";
import type { BillingAccount, PaymentMethod, SubscriptionStatus } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import {
  amountDue,
  amountForBank,
  formatIban,
  invoiceNumber,
  isOverdue,
  looksLikeEmail,
  transferReference,
  type AmountDue,
} from "@/lib/billing";
import { COUNTIES } from "@/lib/counties";
import { strings } from "@/lib/strings";
import { cn, formatDate, withCount } from "@/lib/utils";
import { PageHeader } from "@/components/ui/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardHeader } from "@/components/ui/card";
import { Dialog } from "@/components/ui/dialog";
import { FieldError, invalidProps } from "@/components/ui/field-error";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { useToast } from "@/components/ui/toast";

const t = strings.billing;
const s = strings.subscriptions;

type Invoice = BillingAccount["invoices"][number];

const STATUS_BADGE: Record<SubscriptionStatus, "muted" | "warning" | "success" | "danger"> = {
  PENDING: "warning",
  ACTIVE: "success",
  PAST_DUE: "danger",
  READ_ONLY: "danger",
  CANCELLED: "muted",
};

/** Cât așteptăm notificarea Netopia înainte să spunem „nu plăti a doua oară". */
const SLOW_AFTER_MS = 60_000;

function lei(n: number) {
  return `${n.toLocaleString("ro-RO", { minimumFractionDigits: n % 1 ? 2 : 0, maximumFractionDigits: 2 })} lei`;
}

/** Ziua de azi în ora locală a omului, nu în UTC: la 01:00 în România, UTC e încă ieri. */
function todayIso() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

function timeOf(iso: string) {
  return new Date(iso).toLocaleTimeString("ro-RO", { hour: "2-digit", minute: "2-digit" });
}

/**
 * `/abonament` — ce are de făcut cine plătește contul: administratorul unei firme directe sau consultantul, pentru
 * cabinet.
 *
 * <p>F-E (todo-clienti-abonamente.md): sus, pe afișaj, singurul lucru mare — cât e de plată și până când, sau „Totul e
 * plătit”. Lângă el, transferul cu fiecare rând de copiat, și „Am plătit — verifică acum”, care întreabă FGO fără să
 * aștepte rularea de dimineață. Dedesubt pachetul, datele de facturare (clientul le ține la zi, contract art. 7.5) și
 * facturile, pe telefon ca rânduri.
 *
 * <p>F3 — cardul apare doar când cheile Netopia sunt pe server. Netopia întoarce omul aici cu `?plata=<id>`.
 *
 * <p>Nu cere o firmă aleasă: abonamentul unui consultant e al cabinetului, nu al firmei din comutator.
 */
export function BillingPage() {
  const { data: account, isLoading, isError } = useBillingAccount();
  const today = todayIso();

  return (
    <div>
      <PageHeader title={t.title} description={t.subtitle} />

      <CardReturnNotice />

      {isError && <p className="mt-6 text-sm text-state-bad-text">{t.loadError}</p>}
      {isLoading && <p className="mt-6 text-sm text-content-muted">{strings.common.loading}</p>}
      {account === null && <p className="mt-6 text-sm text-content-muted">{t.none}</p>}

      {account && <Account account={account} today={today} />}
    </div>
  );
}

function Account({ account, today }: { account: BillingAccount; today: string }) {
  const due = amountDue(account.invoices, today);

  return (
    <>
      {account.endsOn && (
        <p className="mt-6 border-l-4 border-state-warn bg-surface-muted px-4 py-3 text-sm text-content-strong">
          {t.endsOn.replace("{date}", formatDate(account.endsOn))}
        </p>
      )}

      <div className={cn("mt-6 grid grid-cols-1 gap-4", due.state !== "PAID_UP" && "lg:grid-cols-2 lg:items-start")}>
        <DueDisplay account={account} due={due} />
        {due.state !== "PAID_UP" && <TransferCard account={account} due={due} />}
      </div>

      <div className="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-2">
        <PackageCard account={account} />
        <BillingDataCard account={account} />
      </div>

      <Invoices account={account} today={today} />
    </>
  );
}

/** Afișajul de sus, ca pe cântar: cifra în verde, eticheta în galben cât e de plată, în roșu când e restantă. */
function DueDisplay({ account, due }: { account: BillingAccount; due: AmountDue }) {
  const checkMut = useCheckBillingPayment();
  const payMut = usePayInvoiceByCard();
  const { notify } = useToast();
  const [checkedAt, setCheckedAt] = useState<string | null>(null);

  const oldest = due.unpaid[0];
  const lastChecked = due.unpaid
    .map((i) => i.paymentCheckedAt)
    .filter((v): v is string => Boolean(v))
    .sort()
    .at(-1);

  async function check() {
    const ids = due.unpaid.map((i) => i.id);
    try {
      await checkMut.mutateAsync(ids);
      setCheckedAt(new Date().toISOString());
    } catch (err) {
      notify(apiErrorMessage(err, t.checkError), "error");
    }
  }

  // Verificarea s-a întors, iar lista s-a citit din nou: dacă nu mai e nimic de plată, a ajuns.
  useEffect(() => {
    if (checkedAt && due.state === "PAID_UP") {
      notify(t.checkedPaid, "success");
      setCheckedAt(null);
    }
  }, [checkedAt, due.state, notify]);

  async function payByCard(invoiceId: string) {
    try {
      window.location.assign(await payMut.mutateAsync(invoiceId));
    } catch (err) {
      notify(apiErrorMessage(err, t.payByCardError), "error");
    }
  }

  if (due.state === "PAID_UP") {
    const first = account.invoices.length === 0;
    const next = (first ? t.firstInvoiceOn : t.nextInvoiceOn)
      .replace("{date}", formatDate(account.nextInvoice.from))
      .replace("{total}", lei(account.nextInvoice.total));
    return (
      <section className="rounded-lg bg-lcd p-4 font-mono sm:p-5" data-testid="billing-display" data-state="PAID_UP">
        <div className="text-[0.6875rem] uppercase tracking-[0.08em] text-lcd-unit">{t.title}</div>
        <div className="mt-1 text-2xl font-medium text-lcd-digit sm:text-3xl">{t.paidUp}</div>
        <p className="mt-2 font-sans text-sm text-panel-text">{next}</p>
        <NextInvoiceLines account={account} />
      </section>
    );
  }

  const overdue = due.state === "OVERDUE";
  const numbers = transferReference(due.unpaid);
  const stillUnpaidAt = checkedAt && !checkMut.isPending ? lastChecked : null;

  return (
    <section className="rounded-lg bg-lcd p-4 sm:p-5" data-testid="billing-display" data-state={due.state}>
      <div
        className={cn(
          "font-mono text-[0.6875rem] uppercase tracking-[0.08em]",
          overdue ? "text-lcd-bad" : "text-lcd-warn"
        )}
      >
        {overdue
          ? withCount(t.overdueBy, due.overdueDays, t.dayOne, t.dayMany)
          : t.dueBy.replace("{date}", formatDate(due.dueDate))}
      </div>
      <div className="mt-1 flex items-baseline gap-2 font-mono">
        <span className="text-4xl font-medium text-lcd-digit" data-testid="billing-amount">
          {due.total.toLocaleString("ro-RO", { minimumFractionDigits: due.total % 1 ? 2 : 0, maximumFractionDigits: 2 })}
        </span>
        <span className="text-lcd-unit">{t.lei}</span>
      </div>
      <p className="mt-1 text-sm text-panel-text">
        {(due.unpaid.length > 1 ? t.dueInvoicesMany : t.dueInvoices).replace("{numbers}", numbers)}
        {overdue && due.dueDate && <> · {s.invoiceDue.replace("{date}", formatDate(due.dueDate))}</>}
      </p>
      {account.readOnlyOn && (
        <p className="mt-2 text-sm text-lcd-warn">{t.readOnlyOn.replace("{date}", formatDate(account.readOnlyOn))}</p>
      )}

      <div className="mt-4 flex flex-wrap gap-2">
        {account.cardPaymentAvailable && oldest && (
          <Button onClick={() => payByCard(oldest.id)} loading={payMut.isPending} className="grow sm:grow-0">
            <CreditCard className="mr-2 h-4 w-4" aria-hidden />
            {t.payByCard}
          </Button>
        )}
        <Button
          variant={account.cardPaymentAvailable ? "outline" : "default"}
          onClick={check}
          loading={checkMut.isPending}
          className="grow sm:grow-0"
        >
          {t.checkPayment}
        </Button>
        {oldest?.fgoLink && (
          <a
            href={oldest.fgoLink}
            target="_blank"
            rel="noreferrer"
            className="inline-flex h-10 items-center gap-2 rounded-md border border-panel-key px-4 text-sm font-semibold text-panel-text hover:bg-panel-hover"
          >
            <FileText className="h-4 w-4" aria-hidden />
            {s.invoicePdf}
          </a>
        )}
      </div>
      <p className="mt-2 text-xs text-panel-mid" role="status" data-testid="billing-check-status">
        {stillUnpaidAt ? t.checkedNotYet.replace("{time}", timeOf(stillUnpaidAt)) : t.checkPaymentHint}
      </p>
    </section>
  );
}

function NextInvoiceLines({ account }: { account: BillingAccount }) {
  return (
    <details className="mt-3 font-sans text-sm text-panel-text">
      <summary className="cursor-pointer text-panel-mid hover:text-panel-text">{t.howCalculated}</summary>
      <ul className="mt-2 space-y-1">
        {account.nextInvoice.lines.map((line, i) => (
          <li key={i} className="flex justify-between gap-3">
            <span>
              {line.label}
              {line.quantity > 1 && ` × ${line.quantity}`}
            </span>
            <span className="font-mono">{lei(line.amount)}</span>
          </li>
        ))}
        <li className="flex justify-between gap-3 border-t border-panel-line pt-1 font-semibold">
          <span>{s.total}</span>
          <span className="font-mono">{lei(account.nextInvoice.total)}</span>
        </li>
      </ul>
      <p className="mt-2 text-xs text-panel-mid">{t.nextInvoiceHint}</p>
    </details>
  );
}

/** Fiecare rând al transferului, de copiat în aplicația băncii. IBAN-ul pleacă fără spații. */
function TransferCard({ account, due }: { account: BillingAccount; due: AmountDue }) {
  const rows: { label: string; shown: string; copied: string; mono?: boolean }[] = [
    { label: t.payee, shown: account.payee.name, copied: account.payee.name },
    { label: t.payeeCui, shown: account.payee.cui, copied: account.payee.cui, mono: true },
    { label: t.iban, shown: formatIban(account.payee.iban), copied: account.payee.iban.replace(/\s+/g, ""), mono: true },
    { label: t.bank, shown: account.payee.bank, copied: account.payee.bank },
    { label: t.amount, shown: lei(due.total), copied: amountForBank(due.total), mono: true },
    { label: t.reference, shown: transferReference(due.unpaid), copied: transferReference(due.unpaid), mono: true },
  ];
  return (
    <Card data-testid="billing-transfer">
      <CardHeader title={t.transferTitle} description={t.transferHint} />
      <dl className="mt-3 divide-y divide-line">
        {rows.map((row) => (
          <div key={row.label} className="flex items-center gap-3 py-2">
            <dt className="w-28 shrink-0 text-xs text-content-muted">{row.label}</dt>
            <dd className={cn("min-w-0 flex-1 break-words text-sm text-content", row.mono && "font-mono")}>
              {row.shown}
            </dd>
            <CopyButton value={row.copied} what={row.label} />
          </div>
        ))}
      </dl>
    </Card>
  );
}

function CopyButton({ value, what }: { value: string; what: string }) {
  const [done, setDone] = useState(false);
  const { notify } = useToast();

  useEffect(() => {
    if (!done) return;
    const timer = window.setTimeout(() => setDone(false), 1500);
    return () => window.clearTimeout(timer);
  }, [done]);

  async function copy() {
    try {
      await navigator.clipboard.writeText(value);
      setDone(true);
    } catch {
      // Fără acces la clipboard (http, permisiune refuzată): valoarea rămâne pe ecran, de selectat de mână.
      notify(value, "info");
    }
  }

  const label = t.copy.replace("{what}", what.toLowerCase());
  return (
    <Button variant="ghost" size="icon-sm" onClick={copy} aria-label={done ? t.copied : label} data-copy={value}>
      {done ? <Check className="h-4 w-4 text-state-ok-text" aria-hidden /> : <Copy className="h-4 w-4" aria-hidden />}
    </Button>
  );
}

function PackageCard({ account }: { account: BillingAccount }) {
  const chooseMut = useChoosePaymentMethod();
  const { notify } = useToast();
  const method = account.paymentMethod ?? "TRANSFER";

  async function choose(next: PaymentMethod) {
    try {
      await chooseMut.mutateAsync(next);
      notify(t.methodSaved, "success");
    } catch (err) {
      notify(apiErrorMessage(err, t.methodError), "error");
    }
  }

  return (
    <Card>
      <CardHeader
        title={t.packageTitle}
        action={<Badge variant={STATUS_BADGE[account.status]}>{s.status[account.status]}</Badge>}
      />
      <p className="mt-2 text-base font-semibold text-content">
        {s.plans[account.plan]}
        {account.founder && <span className="text-sm font-normal text-content-muted"> · {t.founder}</span>}
      </p>
      <dl className="mt-2 space-y-1 text-sm">
        <Row label={t.startedAt}>{formatDate(account.startedAt)}</Row>
        <Row label={t.method}>
          {account.cardPaymentAvailable && method === "CARD" ? t.methodCard : t.methodTransfer}
        </Row>
      </dl>

      {account.cardPaymentAvailable ? (
        <fieldset className="mt-4 space-y-2">
          <legend className="sr-only">{t.method}</legend>
          <MethodOption
            checked={method === "CARD"}
            disabled={chooseMut.isPending}
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
              {t.savedCard.replace("{pan}", account.cardPanMasked).replace("{expiry}", account.cardExpiry ?? "—")}
            </p>
          )}
        </fieldset>
      ) : (
        <p className="mt-3 text-xs text-content-muted">{t.methodTransferOnly}</p>
      )}
      <p className="mt-3 text-xs text-content-muted">{t.otherPlan}</p>
    </Card>
  );
}

function BillingDataCard({ account }: { account: BillingAccount }) {
  const [editing, setEditing] = useState(false);
  const address = [account.billingAddress, account.billingCity, account.billingCounty].filter(Boolean).join(", ");
  const incomplete = !account.billingAddress || !account.billingCity || !account.billingCounty;

  return (
    <Card data-testid="billing-data">
      <CardHeader
        title={t.billingData}
        description={t.billingDataHint}
        action={
          <Button variant="outline" size="sm" onClick={() => setEditing(true)}>
            {t.editBilling}
          </Button>
        }
      />
      <dl className="mt-3 space-y-1 text-sm">
        <Row label={t.billingName}>
          {account.clientName} · <span className="font-mono">{account.clientCui}</span>
        </Row>
        <Row label={t.billingEmail}>{account.billingEmail ?? "—"}</Row>
        <Row label={t.billingAddress}>{address || "—"}</Row>
      </dl>
      {incomplete && <p className="mt-3 text-xs font-medium text-state-bad-text">{t.billingMissing}</p>}
      {editing && <BillingDetailsDialog account={account} onClose={() => setEditing(false)} />}
    </Card>
  );
}

type DetailsForm = { billingEmail: string; billingCounty: string; billingCity: string; billingAddress: string };

function BillingDetailsDialog({ account, onClose }: { account: BillingAccount; onClose: () => void }) {
  const updateMut = useUpdateBillingDetails();
  const { notify } = useToast();
  const [form, setForm] = useState<DetailsForm>({
    billingEmail: account.billingEmail ?? "",
    billingCounty: account.billingCounty ?? "",
    billingCity: account.billingCity ?? "",
    billingAddress: account.billingAddress ?? "",
  });
  const [errors, setErrors] = useState<Partial<Record<keyof DetailsForm, string>>>({});
  // Un județ vechi, scris altfel decât în nomenclatorul FGO, rămâne de ales cât nu e schimbat.
  const counties = account.billingCounty && !COUNTIES.includes(account.billingCounty)
    ? [account.billingCounty, ...COUNTIES]
    : COUNTIES;
  const emailChanged =
    Boolean(account.billingEmail) && form.billingEmail.trim().toLowerCase() !== account.billingEmail!.toLowerCase();

  function set<K extends keyof DetailsForm>(key: K, value: string) {
    setForm((f) => ({ ...f, [key]: value }));
    setErrors((e) => ({ ...e, [key]: undefined }));
  }

  async function submit(e: FormEvent) {
    e.preventDefault();
    const next: Partial<Record<keyof DetailsForm, string>> = {};
    (Object.keys(form) as (keyof DetailsForm)[]).forEach((k) => {
      if (!form[k].trim()) next[k] = t.required;
    });
    if (!next.billingEmail && !looksLikeEmail(form.billingEmail)) next.billingEmail = t.invalidEmail;
    setErrors(next);
    if (Object.keys(next).length > 0) return;
    try {
      await updateMut.mutateAsync({
        billingEmail: form.billingEmail.trim(),
        billingCounty: form.billingCounty,
        billingCity: form.billingCity.trim(),
        billingAddress: form.billingAddress.trim(),
      });
      notify(t.billingSaved, "success");
      onClose();
    } catch (err) {
      notify(apiErrorMessage(err, t.billingSaveError), "error");
    }
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title={t.editBillingTitle}
      description={t.editBillingDescription}
      size="md"
      busy={updateMut.isPending}
      footer={
        <>
          <Button variant="outline" onClick={onClose} disabled={updateMut.isPending}>
            {strings.common.cancel}
          </Button>
          <Button type="submit" form="billing-details" loading={updateMut.isPending}>
            {strings.common.save}
          </Button>
        </>
      }
    >
      <form id="billing-details" onSubmit={submit} noValidate className="space-y-4">
        <p className="text-sm text-content">
          {account.clientName} · <span className="font-mono">{account.clientCui}</span>
        </p>
        <div>
          <Label htmlFor="billing-email" required>
            {t.billingEmailLabel}
          </Label>
          <Input
            id="billing-email"
            type="email"
            autoComplete="email"
            value={form.billingEmail}
            onChange={(e) => set("billingEmail", e.target.value)}
            {...invalidProps("billing-email-error", errors.billingEmail)}
          />
          <FieldError id="billing-email-error" message={errors.billingEmail} />
          <p className="mt-1 text-xs text-content-muted">{emailChanged ? t.billingEmailChangedHint : t.billingEmailHint}</p>
        </div>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <Label htmlFor="billing-county" required>
              {t.billingCounty}
            </Label>
            <Select
              id="billing-county"
              value={form.billingCounty}
              onChange={(e) => set("billingCounty", e.target.value)}
              {...invalidProps("billing-county-error", errors.billingCounty)}
            >
              <option value="">{t.billingCountyPlaceholder}</option>
              {counties.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </Select>
            <FieldError id="billing-county-error" message={errors.billingCounty} />
          </div>
          <div>
            <Label htmlFor="billing-city" required>
              {t.billingCity}
            </Label>
            <Input
              id="billing-city"
              value={form.billingCity}
              onChange={(e) => set("billingCity", e.target.value)}
              {...invalidProps("billing-city-error", errors.billingCity)}
            />
            <FieldError id="billing-city-error" message={errors.billingCity} />
          </div>
        </div>
        <div>
          <Label htmlFor="billing-address" required>
            {t.billingStreet}
          </Label>
          <Input
            id="billing-address"
            autoComplete="street-address"
            value={form.billingAddress}
            onChange={(e) => set("billingAddress", e.target.value)}
            {...invalidProps("billing-address-error", errors.billingAddress)}
          />
          <FieldError id="billing-address-error" message={errors.billingAddress} />
        </div>
      </form>
    </Dialog>
  );
}

function InvoiceState({ invoice, today }: { invoice: Invoice; today: string }) {
  if (invoice.status === "PAID") return <Badge variant="success">{t.invoicePaid}</Badge>;
  if (isOverdue(invoice, today)) return <Badge variant="danger">{t.invoiceOverdue}</Badge>;
  return <Badge variant="warning">{t.invoiceUnpaid}</Badge>;
}

function paidOrDue(invoice: Invoice) {
  if (invoice.status === "PAID" && invoice.paidAt) {
    return `${t.paidOn.replace("{date}", formatDate(invoice.paidAt))}${invoice.paidBy === "CARD" ? ` ${t.paidByCard}` : ""}`;
  }
  return formatDate(invoice.dueDate);
}

function Invoices({ account, today }: { account: BillingAccount; today: string }) {
  const invoices = account.invoices;
  return (
    <section className="mt-8" data-testid="billing-invoices">
      <h2 className="mb-3 text-lg font-semibold text-content">{t.invoices}</h2>

      {/* Telefon: câte un rând pe factură, ca în machetă. */}
      <ul className="divide-y divide-line rounded-lg border border-line-strong/70 sm:hidden">
        {invoices.length === 0 && <li className="p-4 text-sm text-content-muted">{t.noInvoices}</li>}
        {invoices.map((invoice) => (
          <li key={invoice.id} className="flex items-center gap-3 px-4 py-3">
            <div className="min-w-0 flex-1">
              <div className="font-mono font-semibold text-content">
                {invoiceNumber(invoice)} · {lei(invoice.total)}
              </div>
              <div className="font-mono text-xs text-content-muted">
                {formatDate(invoice.periodStart)} – {formatDate(invoice.periodEnd)}
              </div>
              <div className="mt-1">
                <InvoiceState invoice={invoice} today={today} />
              </div>
            </div>
            {invoice.fgoLink && (
              <a href={invoice.fgoLink} target="_blank" rel="noreferrer" className="text-sm font-medium text-brand-700 underline">
                {s.invoicePdf}
              </a>
            )}
          </li>
        ))}
      </ul>

      <div className="hidden sm:block">
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
            {invoices.length === 0 && (
              <TableFallbackRow columns={6} loading={false} icon={Receipt} title={t.noInvoices} />
            )}
            {invoices.map((invoice) => (
              <TR key={invoice.id}>
                <TD className="font-mono font-medium text-content">{invoiceNumber(invoice)}</TD>
                <TD className="font-mono">
                  {formatDate(invoice.periodStart)} – {formatDate(invoice.periodEnd)}
                  {invoice.status === "ISSUED" && invoice.lastCardError && (
                    <span className="mt-1 block font-sans text-xs text-state-bad-text">
                      {t.cardRefused.replace("{reason}", invoice.lastCardError)}
                    </span>
                  )}
                </TD>
                <TD className="text-right font-mono">{lei(invoice.total)}</TD>
                <TD>
                  <InvoiceState invoice={invoice} today={today} />
                </TD>
                <TD>{paidOrDue(invoice)}</TD>
                <TD className="text-right">
                  <span className="inline-flex flex-wrap items-center justify-end gap-3">
                    {invoice.status === "ISSUED" && invoice.fgoLinkPlata && (
                      <a href={invoice.fgoLinkPlata} target="_blank" rel="noreferrer" className="text-brand-700 underline">
                        {t.pay}
                      </a>
                    )}
                    {invoice.fgoLink && (
                      <a href={invoice.fgoLink} target="_blank" rel="noreferrer" className="text-brand-700 underline">
                        {s.invoicePdf}
                      </a>
                    )}
                  </span>
                </TD>
              </TR>
            ))}
          </TBody>
        </Table>
      </div>
    </section>
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
      ? "border-state-ok"
      : payment.status === "FAILED"
        ? "border-state-bad"
        : "border-line-strong";
  const text =
    payment.status === "PAID"
      ? t.returnPaid
      : payment.status === "FAILED"
        ? t.returnFailed.replace("{reason}", payment.error ?? "—")
        : slow
          ? t.returnSlow
          : t.returnPending;

  return (
    <div
      role="status"
      className={`mt-6 flex flex-wrap items-center gap-3 border-l-4 bg-surface-muted px-4 py-3 text-sm text-content-strong ${tone}`}
    >
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
