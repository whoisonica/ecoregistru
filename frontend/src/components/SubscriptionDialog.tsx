import { useState, type FormEvent } from "react";
import {
  useDeleteSubscription,
  useFounderCount,
  useSaveSubscription,
  useSubscription,
} from "@/hooks/useSubscriptions";
import type { InvoicePreview, SubscriptionOwner, SubscriptionPlan } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { formatDate } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { DateInput } from "@/components/ui/date-input";
import { Dialog } from "@/components/ui/dialog";
import { Badge } from "@/components/ui/badge";
import { useToast } from "@/components/ui/toast";

const t = strings.subscriptions;
const COMPANY_PLANS: SubscriptionPlan[] = ["GENERATOR", "GENERATOR_PACKAGING", "FULL_SERVICE"];

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
 * <p>Rubricile pornesc goale (`null`) și cad pe abonamentul încărcat, ca dialogul să nu aibă nevoie
 * de un efect care să le umple după încărcare.
 */
export function SubscriptionDialog({ owner, onClose }: { owner: SubscriptionOwner; onClose: () => void }) {
  const { data: subscription, isLoading, isError } = useSubscription(owner);
  const { data: founderCount } = useFounderCount();
  const saveMut = useSaveSubscription(owner);
  const deleteMut = useDeleteSubscription(owner);
  const { notify } = useToast();

  const isConsultancy = owner.kind === "consultancy";
  const [plan, setPlan] = useState<SubscriptionPlan | null>(null);
  const [startedAt, setStartedAt] = useState<string | null>(null);
  const [founder, setFounder] = useState<boolean | null>(null);

  const currentPlan = plan ?? subscription?.plan ?? (isConsultancy ? "CONSULTANCY" : "GENERATOR");
  const currentStart = startedAt ?? subscription?.startedAt ?? today();
  const currentFounder = founder ?? subscription?.founder ?? false;
  const busy = saveMut.isPending || deleteMut.isPending;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    try {
      await saveMut.mutateAsync({ plan: currentPlan, startedAt: currentStart, founder: currentFounder });
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
      notify(t.removed, "success");
    } catch (err) {
      notify(apiErrorMessage(err, t.removeError), "error");
    }
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title={t.title.replace("{name}", owner.name)}
      footer={
        <>
          {subscription && (
            <Button variant="outline" onClick={handleDelete} disabled={busy} className="mr-auto">
              {t.remove}
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
      {isError && <p className="text-sm text-red-600">{t.loadError}</p>}
      {isLoading && <p className="text-sm text-content-muted">{strings.common.loading}</p>}
      {!isLoading && !isError && (
        <form id="subscription-form" onSubmit={handleSubmit} className="space-y-4">
          <div className="flex flex-wrap items-center gap-2">
            {subscription ? (
              <Badge variant="warning">{t.status[subscription.status]}</Badge>
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
        </form>
      )}
    </Dialog>
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
