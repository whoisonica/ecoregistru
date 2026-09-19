import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import {
  useCancelSubscription,
  useCheckInvoicePayment,
  useDeleteSubscription,
  useDiscardInvoice,
  useFounderCount,
  useSaveSubscription,
  useSubscription,
} from "@/hooks/useSubscriptions";
import type {
  InvoicePreview,
  InvoiceStatus,
  Subscription,
  SubscriptionInvoice,
  SubscriptionOwner,
  SubscriptionPlan,
  SubscriptionStatus,
} from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { formatDate, todayIso } from "@/lib/utils";
import { COUNTIES } from "@/lib/counties";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { DateInput } from "@/components/ui/date-input";
import { Dialog } from "@/components/ui/dialog";
import { Badge } from "@/components/ui/badge";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm-dialog";

const t = strings.subscriptions;
const COMPANY_PLANS: SubscriptionPlan[] = ["GENERATOR", "GENERATOR_PACKAGING", "FULL_SERVICE"];

/** Ca pe `/abonament`: „Activ” era galben, ca o așteptare (P7 din todo-clienti-abonamente.md). */
export const STATUS_BADGE: Record<SubscriptionStatus, "muted" | "warning" | "success" | "danger"> = {
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

export function lei(n: number) {
  return `${n.toLocaleString("ro-RO", { maximumFractionDigits: 2 })} lei`;
}

function periodLabel(invoice: InvoicePreview) {
  return `${formatDate(invoice.from)} – ${formatDate(invoice.to)}`;
}

/** „azi, 10:29” sau „16.09.2026 06:30”, pe ora de pe calculatorul omului. */
export function checkedWhen(instant: string) {
  const d = new Date(instant);
  const time = d.toLocaleTimeString("ro-RO", { hour: "2-digit", minute: "2-digit" });
  const day = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
  return day === todayIso() ? strings.invoicing.today.replace("{time}", time) : `${formatDate(day)} ${time}`;
}

/**
 * Plata abonamentelor, F1 — pachetul unui client și ce se va factura. F2 — datele de facturare cerute de FGO și
 * facturile emise.
 *
 * <p>F-D: starea și acțiunile stau aici, iar pagina firmei (tabul „Abonament și facturi”) și dialogul cabinetului le
 * așază fiecare în felul lui. Rubricile pornesc goale (`null`) și cad pe abonamentul încărcat, ca să nu fie nevoie de
 * un efect care să le umple după încărcare.
 */
function useSubscriptionEditor(owner: SubscriptionOwner) {
  const query = useSubscription(owner);
  const subscription = query.data;
  const saveMut = useSaveSubscription(owner);
  const deleteMut = useDeleteSubscription(owner);
  const cancelMut = useCancelSubscription(owner);
  const [confirm, confirmDialog] = useConfirm();
  const { notify } = useToast();

  const isConsultancy = owner.kind === "consultancy";
  const [plan, setPlan] = useState<SubscriptionPlan | null>(null);
  const [startedAt, setStartedAt] = useState<string | null>(null);
  const [founder, setFounder] = useState<boolean | null>(null);
  const [billingEmail, setBillingEmail] = useState<string | null>(null);
  const [billingCounty, setBillingCounty] = useState<string | null>(null);
  const [billingCity, setBillingCity] = useState<string | null>(null);
  const [billingAddress, setBillingAddress] = useState<string | null>(null);

  const values = {
    plan: plan ?? subscription?.plan ?? (isConsultancy ? "CONSULTANCY" : "GENERATOR"),
    startedAt: startedAt ?? subscription?.startedAt ?? todayIso(),
    founder: founder ?? subscription?.founder ?? false,
    billingEmail: billingEmail ?? subscription?.billingEmail ?? "",
    billingCounty: billingCounty ?? subscription?.billingCounty ?? "",
    billingCity: billingCity ?? subscription?.billingCity ?? "",
    billingAddress: billingAddress ?? subscription?.billingAddress ?? "",
  };
  const setters = { setPlan, setStartedAt, setFounder, setBillingEmail, setBillingCounty, setBillingCity, setBillingAddress };
  const busy = saveMut.isPending || deleteMut.isPending || cancelMut.isPending;

  async function save(e: FormEvent) {
    e.preventDefault();
    try {
      await saveMut.mutateAsync({
        plan: values.plan,
        startedAt: values.startedAt,
        founder: values.founder,
        billingEmail: values.billingEmail || null,
        billingCounty: values.billingCounty || null,
        billingCity: values.billingCity || null,
        billingAddress: values.billingAddress || null,
      });
      notify(t.saved, "success");
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  async function remove() {
    try {
      await deleteMut.mutateAsync();
      setPlan(null);
      setStartedAt(null);
      setFounder(null);
      setBillingEmail(null);
      setBillingCounty(null);
      setBillingCity(null);
      setBillingAddress(null);
      notify(t.removed, "success");
    } catch (err) {
      notify(apiErrorMessage(err, t.removeError), "error");
    }
  }

  /** F4, §9.3 — oprirea cu preaviz de o lună, sau anularea ei cât abonamentul n-a ajuns la capăt. */
  function cancel() {
    confirm({
      title: t.cancel,
      message: t.cancelConfirm,
      confirmLabel: t.cancel,
      tone: "danger",
      onConfirm: async () => {
        try {
          const saved = await cancelMut.mutateAsync("cancel");
          notify(t.cancelled.replace("{date}", formatDate(saved.endsOn)), "success");
        } catch (err) {
          notify(apiErrorMessage(err, t.cancelError), "error");
        }
      },
    });
  }

  async function resume() {
    try {
      await cancelMut.mutateAsync("resume");
      notify(t.resumed, "success");
    } catch (err) {
      notify(apiErrorMessage(err, t.cancelError), "error");
    }
  }

  return {
    owner,
    isConsultancy,
    subscription,
    refetch: query.refetch,
    isLoading: query.isLoading,
    isError: query.isError,
    values,
    setters,
    busy,
    saving: saveMut.isPending,
    save,
    remove,
    cancel,
    resume,
    confirmDialog,
    confirm,
  };
}

type Editor = ReturnType<typeof useSubscriptionEditor>;

/** Pachetul, data de start, fondatorul și datele de facturare — aceleași rubrici în dialog și pe pagină. */
function SubscriptionFields({ editor, formId }: { editor: Editor; formId: string }) {
  const { data: founderCount } = useFounderCount();
  const { isConsultancy, values, setters } = editor;
  return (
    <form id={formId} onSubmit={editor.save} className="space-y-4">
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <div>
          <Label htmlFor="sub-plan">{t.plan}</Label>
          {isConsultancy ? (
            <p id="sub-plan" className="mt-2 text-sm text-content-strong">{t.plans.CONSULTANCY}</p>
          ) : (
            <>
              <Select
                id="sub-plan"
                value={values.plan}
                onChange={(e) => setters.setPlan(e.target.value as SubscriptionPlan)}
              >
                {COMPANY_PLANS.map((p) => (
                  <option key={p} value={p}>
                    {t.plans[p]}
                  </option>
                ))}
              </Select>
              <p className="mt-1 text-xs text-content-muted">{t.planHint}</p>
            </>
          )}
        </div>
        <div>
          <Label htmlFor="sub-start">{t.startedAt}</Label>
          <DateInput id="sub-start" value={values.startedAt} onChange={(e) => setters.setStartedAt(e.target.value)} />
          <p className="mt-1 text-xs text-content-muted">{t.startedAtHint}</p>
        </div>
      </div>

      <label className="flex items-start gap-2 text-sm text-content-strong">
        <input
          type="checkbox"
          className="mt-0.5 h-4 w-4 rounded border-line-strong text-brand focus:ring-brand"
          checked={values.founder}
          onChange={(e) => setters.setFounder(e.target.checked)}
        />
        <span>
          {t.founder}
          {founderCount != null && (
            <span className="block text-xs text-content-muted">
              {t.founderCount.replace("{n}", String(founderCount))}
            </span>
          )}
        </span>
      </label>

      <fieldset className="space-y-3 rounded-md border border-line p-3">
        <legend className="px-1 text-sm font-medium text-content-strong">{t.billing}</legend>
        <p className="text-xs text-content-muted">{t.billingHint}</p>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div>
            <Label htmlFor="sub-email">{t.billingEmail}</Label>
            <Input
              id="sub-email"
              type="email"
              maxLength={100}
              value={values.billingEmail}
              onChange={(e) => setters.setBillingEmail(e.target.value)}
            />
            {!isConsultancy && <p className="mt-1 text-xs text-content-muted">{t.billingEmailHint}</p>}
          </div>
          <div>
            <Label htmlFor="sub-county">{t.billingCounty}</Label>
            <Select id="sub-county" value={values.billingCounty} onChange={(e) => setters.setBillingCounty(e.target.value)}>
              <option value="">{t.billingCountyPlaceholder}</option>
              {COUNTIES.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </Select>
          </div>
          <div>
            <Label htmlFor="sub-city">{t.billingCity}</Label>
            <Input
              id="sub-city"
              maxLength={100}
              value={values.billingCity}
              onChange={(e) => setters.setBillingCity(e.target.value)}
            />
          </div>
          <div>
            <Label htmlFor="sub-address">{t.billingAddress}</Label>
            <Input
              id="sub-address"
              maxLength={500}
              value={values.billingAddress}
              onChange={(e) => setters.setBillingAddress(e.target.value)}
            />
          </div>
        </div>
      </fieldset>
    </form>
  );
}

/** Starea abonamentului și metoda de plată, într-un rând. */
function StatusLine({ subscription }: { subscription: Subscription | null | undefined }) {
  if (!subscription) return <p className="text-sm text-content-muted">{t.none}</p>;
  return (
    <div className="flex flex-wrap items-center gap-2">
      <Badge variant={STATUS_BADGE[subscription.status]}>{t.status[subscription.status]}</Badge>
      {subscription.endsOn && (
        <Badge variant="muted">{t.endsOn.replace("{date}", formatDate(subscription.endsOn))}</Badge>
      )}
      <span className="text-xs text-content-muted">
        {t.method}:{" "}
        {subscription.paymentMethod
          ? strings.billing[subscription.paymentMethod === "CARD" ? "methodCard" : "methodTransfer"]
          : t.methodNone}
        {subscription.cardPanMasked &&
          ` · ${t.savedCard
            .replace("{pan}", subscription.cardPanMasked)
            .replace("{expiry}", subscription.cardExpiry ?? "—")}`}
      </span>
    </div>
  );
}

/**
 * Dialogul abonamentului — după F-D rămâne doar pentru cabinete, care n-au pagina lor. O firmă își are abonamentul pe
 * `/clienti/:id?tab=abonament` (`SubscriptionPanel`).
 */
export function SubscriptionDialog({ owner, onClose }: { owner: SubscriptionOwner; onClose: () => void }) {
  const editor = useSubscriptionEditor(owner);
  const { subscription, busy, isLoading, isError } = editor;

  return (
    <Dialog
      open
      onClose={onClose}
      title={t.title.replace("{name}", owner.name)}
      // Patru butoane în subsol („Șterge”, „Oprește”, „Închide”, „Salvează”) nu încap în 512px și ieșeau din chenar.
      size="xl"
      footer={
        <>
          {subscription && subscription.invoices.length === 0 && (
            <Button variant="outline" onClick={editor.remove} disabled={busy} className="mr-auto">
              {t.remove}
            </Button>
          )}
          {subscription && subscription.status !== "CANCELLED" && (
            <Button variant="outline" onClick={subscription.endsOn ? editor.resume : editor.cancel} disabled={busy}>
              {subscription.endsOn ? t.resume : t.cancel}
            </Button>
          )}
          <Button variant="outline" onClick={onClose} disabled={busy}>
            {strings.common.close}
          </Button>
          <Button type="submit" form="subscription-form" disabled={busy || isLoading || isError}>
            {editor.saving ? strings.common.saving : subscription ? strings.common.save : t.create}
          </Button>
        </>
      }
    >
      {editor.confirmDialog}
      {isError && <p className="text-sm text-state-bad-text">{t.loadError}</p>}
      {isLoading && <p className="text-sm text-content-muted">{strings.common.loading}</p>}
      {!isLoading && !isError && (
        <div className="space-y-4">
          <StatusLine subscription={subscription} />
          <p className="text-xs text-content-muted">{t.hint}</p>
          <SubscriptionFields editor={editor} formId="subscription-form" />
          {subscription && (
            <div className="space-y-3">
              <InvoicePreviewBlock
                title={t.firstInvoice.replace("{period}", periodLabel(subscription.firstInvoice))}
                invoice={subscription.firstInvoice}
              />
              <InvoicePreviewBlock
                title={t.monthlyInvoice.replace("{period}", periodLabel(subscription.monthlyInvoice))}
                invoice={subscription.monthlyInvoice}
              />
              <p className="text-xs text-content-muted">{t.previewHint}</p>
            </div>
          )}
          {subscription && (
            <div className="space-y-2">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <span className="text-sm font-medium text-content-strong">{t.invoices}</span>
                {/* F-A: rularea e a tuturor clienților, deci stă pe ecranul Facturare, nu aici. */}
                <Link to="/facturare" className="text-xs font-semibold text-brand-700 hover:underline">
                  {t.toInvoicing}
                </Link>
              </div>
              <InvoiceList editor={editor} />
            </div>
          )}
        </div>
      )}
    </Dialog>
  );
}

/**
 * F-D (todo-clienti-abonamente.md, macheta B) — abonamentul și facturile unei firme, pe tabul paginii ei. Aceleași
 * rubrici și aceleași acțiuni ca dialogul de dinainte, dar așezate: sus starea, în stânga pachetul și datele de
 * facturare, în dreapta facturile care urmează; dedesubt facturile emise, cu „Verifică plata” și „Oprește” pe rând;
 * jos oprirea abonamentului, departe de „Salvează”.
 */
export function SubscriptionPanel({ owner }: { owner: SubscriptionOwner }) {
  const editor = useSubscriptionEditor(owner);
  const { subscription, busy, isLoading, isError } = editor;

  if (isError) return <p className="mt-6 text-sm text-state-bad-text">{t.loadError}</p>;
  if (isLoading) return <p className="mt-6 text-sm text-content-muted">{strings.common.loading}</p>;

  return (
    <div className="mt-6 space-y-6" data-testid="subscription-panel">
      {editor.confirmDialog}
      <StatusLine subscription={subscription} />

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
        <Card className="p-4 sm:p-5">
          <h2 className="mb-1 text-base font-semibold text-content">{t.plan}</h2>
          <p className="mb-4 text-xs text-content-muted">{t.hint}</p>
          <SubscriptionFields editor={editor} formId="subscription-form" />
          <div className="mt-4 flex flex-wrap justify-end gap-2">
            {subscription && subscription.invoices.length === 0 && (
              <Button variant="outline" onClick={editor.remove} disabled={busy} className="mr-auto">
                {t.remove}
              </Button>
            )}
            <Button type="submit" form="subscription-form" disabled={busy}>
              {editor.saving ? strings.common.saving : subscription ? strings.common.save : t.create}
            </Button>
          </div>
        </Card>

        <div className="space-y-3">
          {subscription ? (
            <>
              <InvoicePreviewBlock
                title={t.firstInvoice.replace("{period}", periodLabel(subscription.firstInvoice))}
                invoice={subscription.firstInvoice}
              />
              <InvoicePreviewBlock
                title={t.monthlyInvoice.replace("{period}", periodLabel(subscription.monthlyInvoice))}
                invoice={subscription.monthlyInvoice}
              />
              <p className="text-xs text-content-muted">{t.previewHint}</p>
            </>
          ) : (
            <p className="text-sm text-content-muted">{t.panelNoSubscription}</p>
          )}
        </div>
      </div>

      {subscription && (
        <section aria-labelledby="sub-invoices">
          <div className="mb-2 flex flex-wrap items-baseline justify-between gap-2">
            <h2 id="sub-invoices" className="text-base font-semibold text-content">
              {t.invoices}
              {subscription.invoices.length > 0 && (
                <span className="ml-2 font-mono text-xs font-normal text-content-muted">
                  {t.invoicesSummary
                    .replace("{billed}", lei(subscription.invoices.reduce((s, i) => s + (i.status === "DRAFT" ? 0 : i.total), 0)))
                    .replace("{paid}", lei(subscription.invoices.reduce((s, i) => s + (i.status === "PAID" ? i.total : 0), 0)))}
                </span>
              )}
            </h2>
            <Link to="/facturare" className="text-xs font-semibold text-brand-700 hover:underline">
              {t.toInvoicing}
            </Link>
          </div>
          <InvoiceList editor={editor} />
        </section>
      )}

      {subscription && subscription.status !== "CANCELLED" && (
        <section className="flex flex-col gap-3 rounded-md border border-line p-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-sm font-semibold text-content">{t.stopSection}</h2>
            <p className="text-xs text-content-muted">{subscription.endsOn ? t.endsOn.replace("{date}", formatDate(subscription.endsOn)) : t.stopSectionHint}</p>
          </div>
          <Button variant="outline" onClick={subscription.endsOn ? editor.resume : editor.cancel} disabled={busy}>
            {subscription.endsOn ? t.resume : t.cancel}
          </Button>
        </section>
      )}
    </div>
  );
}

/** Facturile emise, cu verificarea plății pe fiecare și oprirea celei refuzate de FGO (F-A, acum și aici). */
function InvoiceList({ editor }: { editor: Editor }) {
  const { subscription } = editor;
  const checkMut = useCheckInvoicePayment();
  const discardMut = useDiscardInvoice();
  const { notify } = useToast();
  if (!subscription) return null;
  if (subscription.invoices.length === 0) return <p className="text-sm text-content-muted">{t.noInvoices}</p>;

  async function check(invoice: SubscriptionInvoice) {
    const number = `${invoice.fgoSerie} ${invoice.fgoNumar}`;
    try {
      await checkMut.mutateAsync(invoice.id);
      const fresh = (await editor.refetch()).data?.invoices.find((i) => i.id === invoice.id);
      if (fresh?.status === "PAID") notify(strings.invoicing.checkedPaid.replace("{number}", number), "success");
      else notify(strings.invoicing.checkedUnpaid.replace("{number}", number), "info");
    } catch (err) {
      notify(apiErrorMessage(err, strings.invoicing.checkError), "error");
    }
  }

  function stop(invoice: SubscriptionInvoice) {
    const client = editor.owner.name;
    editor.confirm({
      title: strings.invoicing.stopTitle.replace("{client}", client),
      message: strings.invoicing.stopMessage,
      confirmLabel: strings.invoicing.stop,
      tone: "danger",
      onConfirm: async () => {
        try {
          await discardMut.mutateAsync(invoice.id);
          notify(strings.invoicing.stopped.replace("{client}", client), "success");
        } catch (err) {
          notify(apiErrorMessage(err, strings.invoicing.stopError), "error");
        }
      },
    });
  }

  return (
    <ul className="divide-y divide-line rounded-md border border-line">
      {subscription.invoices.map((invoice) => (
        <InvoiceRow
          key={invoice.id}
          invoice={invoice}
          onCheck={() => check(invoice)}
          onStop={() => stop(invoice)}
          busy={checkMut.isPending || discardMut.isPending}
        />
      ))}
    </ul>
  );
}

function InvoiceRow({
  invoice,
  onCheck,
  onStop,
  busy,
}: {
  invoice: SubscriptionInvoice;
  onCheck: () => void;
  onStop: () => void;
  busy: boolean;
}) {
  const failed = invoice.status === "DRAFT" && Boolean(invoice.lastError);
  return (
    <li className="flex flex-col gap-2 p-3 text-sm sm:flex-row sm:items-start sm:justify-between" data-invoice={invoice.id}>
      <div className="min-w-0 space-y-1">
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
          {invoice.fgoNumar && (
            <span className="font-mono text-content">
              {invoice.fgoSerie} {invoice.fgoNumar}
            </span>
          )}
          <span className="text-content-strong">
            {formatDate(invoice.periodStart)} – {formatDate(invoice.periodEnd)}
          </span>
          <Badge variant={failed ? "danger" : INVOICE_BADGE[invoice.status]}>
            {failed ? strings.invoicing.stateFailed : t.invoiceStatus[invoice.status]}
          </Badge>
        </div>
        <div className="flex flex-wrap gap-x-3 gap-y-1 text-xs text-content-muted">
          {invoice.dueDate && invoice.status === "ISSUED" && (
            <span>{t.invoiceDue.replace("{date}", formatDate(invoice.dueDate))}</span>
          )}
          {invoice.status === "PAID" && invoice.paidAt && (
            <span>{strings.invoicing.paidOn.replace("{date}", formatDate(invoice.paidAt))}</span>
          )}
          {invoice.status === "PAID" && invoice.paidBy === "CARD" && <span>{t.paidByCard}</span>}
          {invoice.status === "PAID" && invoice.paidBy === "CARD" && !invoice.fgoCollectedAt && (
            <span className="text-state-warn-text">{t.notInFgo}</span>
          )}
          {invoice.status === "ISSUED" && (
            <span>
              {invoice.paymentCheckedAt
                ? strings.invoicing.checkedAt.replace("{when}", checkedWhen(invoice.paymentCheckedAt))
                : strings.invoicing.neverChecked}
            </span>
          )}
        </div>
        {failed && <p className="text-xs text-state-bad-text">{invoice.lastError}</p>}
        {invoice.status === "ISSUED" && invoice.lastCardError && (
          <p className="text-xs text-state-bad-text">
            {strings.billing.cardRefused.replace("{reason}", invoice.lastCardError)}
          </p>
        )}
      </div>
      <div className="flex shrink-0 flex-wrap items-center gap-2 sm:justify-end">
        <span className="font-mono tabular-nums text-content">{lei(invoice.total)}</span>
        {invoice.status === "ISSUED" && (
          <Button size="sm" variant="outline" onClick={onCheck} disabled={busy}>
            {strings.invoicing.checkPayment}
          </Button>
        )}
        {failed && (
          <Button size="sm" variant="outline" onClick={onStop} disabled={busy}>
            {strings.invoicing.stop}
          </Button>
        )}
        {invoice.fgoLink && (
          <a href={invoice.fgoLink} target="_blank" rel="noreferrer" className="text-xs text-brand underline">
            {t.invoicePdf}
          </a>
        )}
      </div>
    </li>
  );
}

function InvoicePreviewBlock({ title, invoice }: { title: string; invoice: InvoicePreview }) {
  return (
    <div className="rounded-md border border-line p-3">
      <span className="block text-sm font-medium text-content-strong">{title}</span>
      <ul className="mt-2 space-y-1 text-sm">
        {invoice.lines.map((line, i) => (
          <li key={i} className="flex justify-between gap-3">
            <span>
              {line.label}
              {line.quantity > 1 && ` × ${line.quantity}`}
            </span>
            <span className="tabular-nums">{lei(line.amount)}</span>
          </li>
        ))}
        <li className="flex justify-between gap-3 border-t border-line pt-1 font-medium text-content-strong">
          <span>{t.total}</span>
          <span className="tabular-nums">{lei(invoice.total)}</span>
        </li>
      </ul>
    </div>
  );
}
