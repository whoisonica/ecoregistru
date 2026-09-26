import { useMemo, useState } from "react";
import { useReceiveTransfer } from "@/hooks/useWeighingOperations";
import { useScales } from "@/hooks/useScales";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { todayIso } from "@/lib/utils";
import type { WeighingOperation } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { BinSwatch } from "@/components/ui/bin-swatch";
import { DateInput } from "@/components/ui/date-input";
import { Dialog } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PillGroup } from "@/components/ui/pill-group";
import { Textarea } from "@/components/ui/textarea";
import { useToast } from "@/components/ui/toast";
import { ScaleStateBadge } from "@/components/depot/ScaleStateBadge";

const t = strings.weighing;
const kgFormat = new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 3 });

function num(value: string): number | null {
  const cleaned = value.replace(",", ".").trim();
  if (cleaned === "") return null;
  const parsed = Number(cleaned);
  return Number.isFinite(parsed) ? parsed : null;
}

interface Weighed {
  gross: string;
  tare: string;
  net: string;
}

/**
 * D2.5 — recepția unui transfer: depozitul de destinație cântărește din nou fiecare linie plecată, cu cântarul lui.
 * Diferența față de plecare se vede pe loc; când nu e zero se pot scrie NIR-ul și decizia comisiei. Dacă diferența
 * trece de toleranța celor două cântare, serverul le cere (regula e acolo, cu clasa și diviziunea cântarelor), iar
 * ce spune el ajunge aici ca mesaj. Nicio greutate nu se precompletează: cea de la plecare stă alături, ca reper.
 */
export function ReceiveTransferDialog({
  operation,
  onClose,
  onReceived,
}: {
  operation: WeighingOperation;
  onClose: () => void;
  onReceived: () => void;
}) {
  const receive = useReceiveTransfer();
  const scales = useScales();
  const { notify } = useToast();
  const target = operation.transfer!.targetWorkPointId;

  const [receivedOn, setReceivedOn] = useState(todayIso());
  const [weighed, setWeighed] = useState<Record<string, Weighed>>(() =>
    Object.fromEntries(operation.lines.map((l) => [l.id, { gross: "", tare: "", net: "" }]))
  );
  const [nirNumber, setNirNumber] = useState("");
  const [differenceReason, setDifferenceReason] = useState("");
  const [scaleReason, setScaleReason] = useState("");

  const depotScales = useMemo(
    () => (scales.data ?? []).filter((s) => s.workPointId === target && s.status === "IN_USE"),
    [scales.data, target]
  );
  const [scaleChoice, setScaleChoice] = useState<string | null>(null);
  const scaleId = scaleChoice ?? (depotScales.length === 1 ? depotScales[0].id : "");
  const chosenScale = depotScales.find((s) => s.id === scaleId) ?? null;

  const netOf = (w: Weighed) => {
    const gross = num(w.gross);
    const tare = num(w.tare);
    return gross != null && tare != null ? Math.round((gross - tare) * 1000) / 1000 : num(w.net);
  };
  const sent = operation.lines.reduce((sum, l) => sum + (l.finalKg ?? 0), 0);
  const nets = operation.lines.map((l) => netOf(weighed[l.id]));
  const complete = nets.every((n) => n != null && n > 0);
  const received = nets.reduce<number>((sum, n) => sum + (n ?? 0), 0);
  const difference = Math.round((received - sent) * 1000) / 1000;

  function patch(id: string, change: Partial<Weighed>) {
    setWeighed((current) => ({ ...current, [id]: { ...current[id], ...change } }));
  }

  async function submit() {
    try {
      await receive.mutateAsync({
        id: operation.id,
        input: {
          receivedOn,
          scaleId: scaleId || null,
          grossKg: null,
          tareKg: null,
          lines: operation.lines.map((l) => {
            const w = weighed[l.id];
            const both = num(w.gross) != null && num(w.tare) != null;
            return {
              lineId: l.id,
              grossKg: num(w.gross),
              tareKg: num(w.tare),
              netKg: both ? null : num(w.net),
              finalKg: null,
            };
          }),
          nirNumber: nirNumber.trim() || null,
          differenceReason: differenceReason.trim() || null,
          scaleReason: scaleReason.trim() || null,
        },
      });
      notify(t.received, "success");
      onReceived();
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  const needsScaleReason = chosenScale != null && chosenScale.state !== "VALID";

  return (
    <Dialog
      open
      onClose={onClose}
      title={`${t.receiveTitle} · ${operation.number}`}
      description={t.receiveHint}
      size="lg"
      busy={receive.isPending}
      footer={
        <>
          <Button variant="outline" onClick={onClose} disabled={receive.isPending}>
            {strings.common.close}
          </Button>
          <Button onClick={submit} disabled={!complete || receive.isPending || (needsScaleReason && !scaleReason.trim())}>
            {receive.isPending ? strings.common.saving : t.receive}
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <p className="text-sm text-content">
          {operation.workPointName} → <strong>{operation.transfer!.targetWorkPointName}</strong>
        </p>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div>
            <Label htmlFor="rt-date">{t.receivedOn}</Label>
            <DateInput id="rt-date" value={receivedOn} onChange={(e) => setReceivedOn(e.target.value)} />
          </div>
          <div>
            <Label id="rt-scale-label">{t.scale}</Label>
            {depotScales.length > 0 ? (
              <div className="flex flex-wrap items-center gap-3">
                <PillGroup
                  name="rt-scale"
                  aria-labelledby="rt-scale-label"
                  options={depotScales.map((s) => ({ value: s.id, label: s.name }))}
                  selected={scaleId ? [scaleId] : []}
                  onToggle={(value) => setScaleChoice(value === scaleId ? "" : value)}
                />
                {chosenScale && <ScaleStateBadge state={chosenScale.state} validUntil={chosenScale.validUntil} />}
              </div>
            ) : (
              <p className="text-sm text-content-muted">{t.scaleNone}</p>
            )}
          </div>
        </div>
        {needsScaleReason && (
          <div>
            <Label htmlFor="rt-scale-reason">{t.scaleReasonLabel}</Label>
            <Textarea
              id="rt-scale-reason"
              rows={2}
              value={scaleReason}
              onChange={(e) => setScaleReason(e.target.value)}
              placeholder={t.scaleReasonPlaceholder}
            />
          </div>
        )}

        <div className="space-y-3">
          {operation.lines.map((l) => {
            const w = weighed[l.id];
            const both = Boolean(w.gross && w.tare);
            return (
              <div key={l.id} className="border border-line p-3">
                <p className="mb-2 flex flex-wrap items-center gap-x-3 text-sm">
                  <span className="inline-flex items-center">
                    <BinSwatch code={l.wasteCode} />
                    {l.articleName ?? l.wasteCode}
                  </span>
                  <span className="font-mono text-xs text-content-muted">
                    {t.receiptSent} {kgFormat.format(l.finalKg ?? 0)} kg
                  </span>
                </p>
                <div className="grid grid-cols-3 gap-3">
                  <div>
                    <Label htmlFor={`rt-g-${l.id}`}>{t.lineGross}</Label>
                    <Input id={`rt-g-${l.id}`} inputMode="decimal" value={w.gross}
                      onChange={(e) => patch(l.id, { gross: e.target.value })} />
                  </div>
                  <div>
                    <Label htmlFor={`rt-t-${l.id}`}>{t.lineTare}</Label>
                    <Input id={`rt-t-${l.id}`} inputMode="decimal" value={w.tare}
                      onChange={(e) => patch(l.id, { tare: e.target.value })} />
                  </div>
                  <div>
                    <Label htmlFor={`rt-n-${l.id}`}>{t.lineNet}</Label>
                    <Input
                      id={`rt-n-${l.id}`}
                      inputMode="decimal"
                      value={both ? String(netOf(w) ?? "") : w.net}
                      onChange={(e) => patch(l.id, { net: e.target.value })}
                      disabled={both}
                      className={both ? "bg-surface-sunken" : undefined}
                    />
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        <dl className="grid grid-cols-3 gap-x-6 border border-line bg-surface-sunken p-3">
          <div>
            <dt className="text-xs text-content-muted">{t.receiptSent}</dt>
            <dd className="font-mono tabular-nums text-content-strong">{kgFormat.format(sent)} kg</dd>
          </div>
          <div>
            <dt className="text-xs text-content-muted">{t.receiptReceived}</dt>
            <dd className="font-mono tabular-nums text-content-strong">
              {complete ? `${kgFormat.format(received)} kg` : "?"}
            </dd>
          </div>
          <div>
            <dt className="text-xs text-content-muted">{t.difference}</dt>
            <dd className="font-mono tabular-nums text-content-strong">
              {complete ? `${difference > 0 ? "+" : ""}${kgFormat.format(difference)} kg` : "?"}
            </dd>
          </div>
        </dl>

        {complete && difference !== 0 && (
          <div className="space-y-3">
            <p className="text-xs text-content-muted">{t.differenceHint}</p>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              <div>
                <Label htmlFor="rt-nir">{t.nirNumber}</Label>
                <Input id="rt-nir" maxLength={40} value={nirNumber} onChange={(e) => setNirNumber(e.target.value)} />
              </div>
              <div className="sm:col-span-2">
                <Label htmlFor="rt-reason">{t.differenceReason}</Label>
                <Textarea id="rt-reason" rows={2} maxLength={1000} value={differenceReason}
                  onChange={(e) => setDifferenceReason(e.target.value)} />
              </div>
            </div>
          </div>
        )}
      </div>
    </Dialog>
  );
}
