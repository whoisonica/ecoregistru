import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import {
  useCancelSubscription,
  useDeleteSubscription,
  useFounderCount,
  useSaveSubscription,
  useSubscription,
} from "@/hooks/useSubscriptions";
import type {
  InvoicePreview,
  InvoiceStatus,
  SubscriptionInvoice,
  SubscriptionOwner,
  SubscriptionPlan,
  SubscriptionStatus,
} from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { formatDate } from "@/lib/utils";
import { COUNTIES } from "@/lib/counties";
import { Button } from "@/components/ui/button";
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

function periodLabel(invoice: InvoicePreview) {
  return `${formatDate(invoice.from)} – ${formatDate(invoice.to)}`;
}

function today() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

/**
 * Plata abonamentelor, F1 — pachetul unui client și ce se va factura, pe ecranul Clienți al
 * platformei. Pentru o firmă directă sau pentru un cabinet; firmele unui cabinet n-au abonament
 * propriu, deci rândul lor nu deschide dialogul ăsta.
 *
 * <p>F2 — datele de facturare cerute de FGO și facturile emise.
 *
 * <p>Rubricile pornesc goale (`null`) și cad pe abonamentul încărcat, ca dialogul să nu aibă nevoie
 * de un efect care să le umple după încărcare.
 */
export function SubscriptionDialog({ owner, onClose }: { owner: SubscriptionOwner; onClose: () => void }) {
  const { data: subscription, isLoading, isError } = useSubscription(owner);
  const { data: founderCount } = useFounderCount();
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

  const currentPlan = plan ?? subscription?.plan ?? (isConsultancy ? "CONSULTANCY" : "GENERATOR");
  const currentStart = startedAt ?? subscription?.startedAt ?? today();
  const currentFounder = founder ?? subscription?.founder ?? false;
  const currentEmail = billingEmail ?? subscription?.billingEmail ?? "";
  const currentCounty = billingCounty ?? subscription?.billingCounty ?? "";
  const currentCity = billingCity ?? subscription?.billingCity ?? "";
  const currentAddress = billingAddress ?? subscription?.billingAddress ?? "";
  const busy = saveMut.isPending || deleteMut.isPending || cancelMut.isPending;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    try {
      await saveMut.mutateAsync({
        plan: currentPlan,
        startedAt: currentStart,
        founder: currentFounder,
        billingEmail: currentEmail || null,
        billingCounty: currentCounty || null,
        billingCity: currentCity || null,
        billingAddress: currentAddress || null,
      });
      notify(t.saved, "success");
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  async function handleDelete() {
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
  function handleCancel() {
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

  async function handleResume() {
    try {
      await cancelMut.mutateAsync("resume");
      notify(t.resumed, "success");
    } catch (err) {
      notify(apiErrorMessage(err, t.cancelError), "error");
    }
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title={t.title.replace("{name}", owner.name)}
      footer={
        <>
          {subscription && subscription.invoices.length === 0 && (
            <Button variant="outline" onClick={handleDelete} disabled={busy} className="mr-auto">
              {t.remove}
            </Button>
          )}
          {subscription && subscription.status !== "CANCELLED" && (
            <Button
              variant="outline"
              onClick={subscription.endsOn ? handleResume : handleCancel}
              disabled={busy}
            >
              {subscription.endsOn ? t.resume : t.cancel}
            </Button>
          )}
          <Button variant="outline" onClick={onClose} disabled={busy}>
            {strings.common.close}
          </Button>
          <Button type="submit" form="subscription-form" disabled={busy || isLoading || isError}>
            {saveMut.isPending ? strings.common.saving : subscription ? strings.common.save : t.create}
          </Button>
        </>
      }
    >
      {confirmDialog}
      {isError && <p className="text-sm text-red-600">{t.loadError}</p>}
      {isLoading && <p className="text-sm text-content-muted">{strings.common.loading}</p>}
      {!isLoading && !isError && (
        <form id="subscription-form" onSubmit={handleSubmit} className="space-y-4">
          <div className="flex flex-wrap items-center gap-2">
            {subscription ? (
              <>
                <Badge variant={STATUS_BADGE[subscription.status]}>{t.status[subscription.status]}</Badge>
                {subscription.endsOn && (
                  <Badge variant="muted">{t.endsOn.replace("{date}", formatDate(subscription.endsOn))}</Badge>
                )}
                <span className="text-xs text-content-muted">
                  {t.method}:{" "}
                  {subscription.paymentMethod ? strings.billing[subscription.paymentMethod === "CARD" ? "methodCard" : "methodTransfer"] : t.methodNone}
                  {subscription.cardPanMasked &&
                    ` · ${t.savedCard
                      .replace("{pan}", subscription.cardPanMasked)
                      .replace("{expiry}", subscription.cardExpiry ?? "—")}`}
                </span>
              </>
            ) : (
              <p className="text-sm text-content-muted">{t.none}</p>
            )}
          </div>
          <p className="text-xs text-content-muted">{t.hint}</p>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <Label htmlFor="sub-plan">{t.plan}</Label>
              {isConsultancy ? (
                <p id="sub-plan" className="mt-2 text-sm text-content-strong">{t.plans.CONSULTANCY}</p>
              ) : (
                <>
                  <Select
                    id="sub-plan"
                    value={currentPlan}
                    onChange={(e) => setPlan(e.target.value as SubscriptionPlan)}
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
              <DateInput id="sub-start" value={currentStart} onChange={(e) => setStartedAt(e.target.value)} />
              <p className="mt-1 text-xs text-content-muted">{t.startedAtHint}</p>
            </div>
          </div>

          <label className="flex items-start gap-2 text-sm text-content-strong">
            <input
              type="checkbox"
              className="mt-0.5 h-4 w-4 rounded border-line-strong text-brand focus:ring-brand"
              checked={currentFounder}
              onChange={(e) => setFounder(e.target.checked)}
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
                  value={currentEmail}
                  onChange={(e) => setBillingEmail(e.target.value)}
                />
                {!isConsultancy && <p className="mt-1 text-xs text-content-muted">{t.billingEmailHint}</p>}
              </div>
              <div>
                <Label htmlFor="sub-county">{t.billingCounty}</Label>
                <Select id="sub-county" value={currentCounty} onChange={(e) => setBillingCounty(e.target.value)}>
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
                  value={currentCity}
                  onChange={(e) => setBillingCity(e.target.value)}
                />
              </div>
              <div>
                <Label htmlFor="sub-address">{t.billingAddress}</Label>
                <Input
                  id="sub-address"
                  maxLength={500}
                  value={currentAddress}
                  onChange={(e) => setBillingAddress(e.target.value)}
                />
              </div>
            </div>
          </fieldset>

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
              {subscription.invoices.length === 0 ? (
                <p className="text-sm text-content-muted">{t.noInvoices}</p>
              ) : (
                <ul className="divide-y divide-line rounded-md border border-line">
                  {subscription.invoices.map((invoice) => (
                    <InvoiceRow key={invoice.id} invoice={invoice} />
                  ))}
                </ul>
              )}
            </div>
          )}
        </form>
      )}
    </Dialog>
  );
}

function InvoiceRow({ invoice }: { invoice: SubscriptionInvoice }) {
  return (
    <li className="space-y-1 p-3 text-sm">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span className="text-content-strong">
          {formatDate(invoice.periodStart)} – {formatDate(invoice.periodEnd)}
          {invoice.fgoNumar && (
            <span className="ml-2 text-content-muted">
              {invoice.fgoSerie} {invoice.fgoNumar}
            </span>
          )}
        </span>
        <span className="flex items-center gap-2">
          <span className="tabular-nums">{lei(invoice.total)}</span>
          <Badge variant={INVOICE_BADGE[invoice.status]}>{t.invoiceStatus[invoice.status]}</Badge>
        </span>
      </div>
      <div className="flex flex-wrap gap-3 text-xs text-content-muted">
        {invoice.dueDate && invoice.status === "ISSUED" && (
          <span>{t.invoiceDue.replace("{date}", formatDate(invoice.dueDate))}</span>
        )}
        {invoice.status === "PAID" && invoice.paidBy === "CARD" && <span>{t.paidByCard}</span>}
        {invoice.status === "PAID" && invoice.paidBy === "CARD" && !invoice.fgoCollectedAt && (
          <span className="text-amber-700">{t.notInFgo}</span>
        )}
        {invoice.fgoLink && (
          <a href={invoice.fgoLink} target="_blank" rel="noreferrer" className="text-brand underline">
            {t.invoicePdf}
          </a>
        )}
      </div>
      {invoice.status === "DRAFT" && invoice.lastError && (
        <p className="text-xs text-red-600">{invoice.lastError}</p>
      )}
      {invoice.status === "ISSUED" && invoice.lastCardError && (
        <p className="text-xs text-red-600">{strings.billing.cardRefused.replace("{reason}", invoice.lastCardError)}</p>
      )}
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
