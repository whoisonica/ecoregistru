import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useCreateBaling, useCancelWeighingOperation } from "@/hooks/useWeighingOperations";
import { useWorkPoints } from "@/hooks/useWorkPoints";
import { useWasteArticles } from "@/hooks/useWasteArticles";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { formatDate, todayIso } from "@/lib/utils";
import type { WeighingOperation } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { DateInput } from "@/components/ui/date-input";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PillGroup } from "@/components/ui/pill-group";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm-dialog";
import { closedMonthLabel } from "@/lib/closedMonthLabel";

const t = strings.weighing;
const b = t.baling;
const kgFormat = new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 3 });

/**
 * F5 — fișa de balotare (proprietarul, 27.09.2026): operatorul scrie doar câți baloți a făcut. Greutatea unui balot vine
 * de pe sortimentul balotat, iar kilogramele trec din sortimentul vrac în cel balotat. Se salvează finalizată: n-are
 * preț, plată sau document. O balotare salvată doar se citește; cine aprobă o poate anula, cu motiv.
 */
export function BalingDialog({
  operation,
  canApprove,
  onClose,
}: {
  operation: WeighingOperation | null;
  canApprove: boolean;
  onClose: () => void;
}) {
  const createMut = useCreateBaling();
  const cancelMut = useCancelWeighingOperation();
  const workPoints = useWorkPoints();
  const articles = useWasteArticles();
  const { notify } = useToast();

  const depots = useMemo(() => (workPoints.data ?? []).filter((w) => w.active), [workPoints.data]);
  const baled = useMemo(
    () => (articles.data ?? []).filter((a) => a.active && a.sourceArticleId && a.baleWeightKg),
    [articles.data]
  );

  const [workPointId, setWorkPointId] = useState("");
  const [date, setDate] = useState(todayIso());
  const [articleId, setArticleId] = useState("");
  const [count, setCount] = useState("");
  const [notes, setNotes] = useState("");
  const [showErrors, setShowErrors] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [cancelReason, setCancelReason] = useState("");
  const [confirm, confirmDialog] = useConfirm();

  // Un singur depozit sau un singur sortiment balotat: alegerea e făcută.
  const depotId = workPointId || (depots.length === 1 ? depots[0].id : "");
  const chosenId = articleId || (baled.length === 1 ? baled[0].id : "");
  const chosen = baled.find((a) => a.id === chosenId);
  const bales = Number(count);
  const countOk = Number.isInteger(bales) && bales > 0;

  async function save() {
    setShowErrors(true);
    if (!depotId || !chosen || !countOk) return;
    try {
      const done = await createMut.mutateAsync({
        workPointId: depotId,
        date,
        articleId: chosen.id,
        baleCount: bales,
        notes: notes.trim() || null,
      });
      notify(b.saved, "success");
      const negative = (done.stockWarnings ?? []).map(
        (w) => `${w.articleName ?? w.wasteCode} (${w.stockKg.toLocaleString("ro-RO")} kg)`
      );
      if (negative.length > 0) notify(t.stockWarning.replace("{items}", negative.join(", ")), "info");
      if (done.baling?.r12NotAuthorized) notify(b.r12Missing, "info");
      onClose();
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  function cancel() {
    if (!operation || !cancelReason.trim()) return;
    // D2 — balotarea e finalizată de la creare: dintr-o lună încheiată cere a doua confirmare.
    const month = closedMonthLabel(operation.date);
    if (!month) {
      void cancelBaling(false);
      return;
    }
    setCancelling(false);
    confirm({
      title: t.pastPeriodTitle,
      message: t.pastPeriodBody(month),
      confirmLabel: t.pastPeriodConfirm(month),
      tone: "danger",
      onConfirm: () => void cancelBaling(true),
    });
  }

  async function cancelBaling(confirmPastPeriod: boolean) {
    if (!operation) return;
    try {
      await cancelMut.mutateAsync({ id: operation.id, reason: cancelReason.trim(), confirmPastPeriod });
      notify(t.cancelled, "success");
      onClose();
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  if (operation?.baling) {
    const x = operation.baling;
    return (
      <>
        <Dialog
          open
          onClose={onClose}
          title={b.viewTitle(operation.number)}
          size="md"
          description={
            operation.status === "CANCELLED" ? (
              <span className="text-state-bad-text">
                {t.cancelledBecause} {operation.cancelReason}
              </span>
            ) : undefined
          }
          footer={
            <>
              <Button variant="outline" onClick={onClose}>
                {strings.common.close}
              </Button>
              {canApprove && operation.status === "FINALIZED" && (
                <Button variant="outline" className="text-state-bad-text" onClick={() => setCancelling(true)}>
                  {t.cancelOperation}
                </Button>
              )}
            </>
          }
        >
          <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-2 text-sm">
            <dt className="text-content-muted">{t.date}</dt>
            <dd className="font-mono">{formatDate(operation.date)}</dd>
            <dt className="text-content-muted">{t.workPoint}</dt>
            <dd>{operation.workPointName}</dd>
            <dt className="text-content-muted">{b.fromTo}</dt>
            <dd>
              {x.sourceArticleName} → {x.articleName}
            </dd>
            <dt className="text-content-muted">{b.count}</dt>
            <dd className="font-mono tabular-nums">
              {b.bales(x.baleCount)} · {b.perBale(kgFormat.format(x.baleWeightKg))} · {kgFormat.format(x.kg)} kg
            </dd>
            {operation.notes && (
              <>
                <dt className="text-content-muted">{b.notes}</dt>
                <dd>{operation.notes}</dd>
              </>
            )}
          </dl>
          <p className="mt-4 text-xs text-content-muted">{b.treatmentNote}</p>
          {x.r12NotAuthorized && <p className="mt-2 text-xs text-state-warn-text">{b.r12Missing}</p>}
        </Dialog>

        <Dialog
          open={cancelling}
          onClose={() => setCancelling(false)}
          title={t.confirmCancelTitle}
          description={t.confirmCancelBody}
          size="md"
          footer={
            <>
              <Button variant="outline" onClick={() => setCancelling(false)}>
                {strings.common.close}
              </Button>
              <Button
                className="bg-state-bad text-white hover:bg-state-bad"
                onClick={cancel}
                disabled={!cancelReason.trim() || cancelMut.isPending}
              >
                {t.cancelOperation}
              </Button>
            </>
          }
        >
          <Label htmlFor="baling-cancel-reason">{t.cancelReason}</Label>
          <Textarea
            id="baling-cancel-reason"
            rows={3}
            value={cancelReason}
            onChange={(e) => setCancelReason(e.target.value)}
            placeholder={t.cancelReasonPlaceholder}
          />
        </Dialog>
        {confirmDialog}
      </>
    );
  }

  const noArticles = articles.isSuccess && baled.length === 0;

  return (
    <Dialog
      open
      onClose={onClose}
      title={b.title}
      size="md"
      busy={createMut.isPending}
      footer={
        <>
          <Button variant="outline" onClick={onClose} disabled={createMut.isPending}>
            {strings.common.close}
          </Button>
          <Button onClick={save} disabled={createMut.isPending || noArticles}>
            {createMut.isPending ? strings.common.saving : b.save}
          </Button>
        </>
      }
    >
      {noArticles ? (
        <p className="text-sm text-content-muted">
          {b.noArticles}{" "}
          <Link to="/setari/sortimente" className="font-medium text-brand-700 underline">
            {b.noArticlesLink}
          </Link>
        </p>
      ) : (
        <div className="space-y-4">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <Label htmlFor="baling-date">{t.date}</Label>
              <DateInput id="baling-date" value={date} onChange={(e) => setDate(e.target.value)} />
            </div>
            {depots.length > 1 && (
              <div>
                <Label htmlFor="baling-depot">{t.workPoint}</Label>
                <Select
                  id="baling-depot"
                  value={depotId}
                  onChange={(e) => setWorkPointId(e.target.value)}
                  aria-invalid={showErrors && !depotId}
                >
                  <option value="">{t.transferToPlaceholder}</option>
                  {depots.map((w) => (
                    <option key={w.id} value={w.id}>
                      {w.name}
                    </option>
                  ))}
                </Select>
              </div>
            )}
          </div>
          <div>
            <p id="baling-article-label" className="mb-1.5 text-sm font-medium text-content-strong">
              {b.article}
            </p>
            <PillGroup
              name="baling-article"
              aria-labelledby="baling-article-label"
              options={baled.map((a) => ({ value: a.id, label: a.name, code: a.wasteCode }))}
              selected={chosenId ? [chosenId] : []}
              onToggle={setArticleId}
            />
            {showErrors && !chosen && <p className="mt-1 text-xs text-red-600">{b.articleRequired}</p>}
          </div>
          <div>
            <Label htmlFor="baling-count">{b.count}</Label>
            <Input
              id="baling-count"
              inputMode="numeric"
              className="max-w-[10rem] font-mono"
              value={count}
              onChange={(e) => setCount(e.target.value.replace(/[^0-9]/g, ""))}
              aria-invalid={showErrors && !countOk}
              autoFocus
            />
            {showErrors && !countOk && <p className="mt-1 text-xs text-red-600">{b.countRequired}</p>}
            {chosen && countOk && (
              <p className="mt-2 font-mono text-sm tabular-nums text-content" data-testid="baling-computed">
                {b.computed(
                  bales,
                  kgFormat.format(chosen.baleWeightKg ?? 0),
                  kgFormat.format(bales * (chosen.baleWeightKg ?? 0)),
                  chosen.sourceArticleName ?? "",
                  chosen.name
                )}
              </p>
            )}
          </div>
          <div>
            <Label htmlFor="baling-notes">{b.notes}</Label>
            <Textarea id="baling-notes" rows={2} value={notes} onChange={(e) => setNotes(e.target.value)} />
          </div>
          <p className="text-xs text-content-muted">{b.treatmentNote}</p>
        </div>
      )}
    </Dialog>
  );
}
