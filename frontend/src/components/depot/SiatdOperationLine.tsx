import { useState } from "react";
import { formatDate, todayIso } from "@/lib/dates";
import { formatDue } from "@/lib/siatdDue";
import { strings } from "@/lib/strings";
import type { WeighingSiatd } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { SiatdConfirmDialog } from "./SiatdConfirmDialog";

const t = strings.siatd;

/**
 * F6a — rândul SIATD din dialogul unei recepții: până când se confirmă, confirmată (cu codul), sau ratată; plus
 * „Confirmă” pentru cine aprobă.
 */
export function SiatdOperationLine({
  operationId,
  siatd,
  approver,
}: {
  operationId: string;
  siatd: WeighingSiatd;
  approver: boolean;
}) {
  const [confirming, setConfirming] = useState(false);
  const text =
    siatd.state === "CONFIRMED"
      ? t.dialogConfirmed.replace("{date}", formatDate(siatd.confirmedAt)) +
        (siatd.code ? ` · ${t.dialogCode.replace("{code}", siatd.code)}` : "")
      : siatd.state === "MISSED"
        ? t.dialogMissed.replace("{due}", formatDate(siatd.due))
        : t.dialogPending.replace("{due}", formatDue(siatd.due, todayIso()));
  const tone =
    siatd.state === "MISSED" ? "text-state-bad-text" : siatd.state === "CONFIRMED" ? "text-state-ok-text" : "text-state-warn-text";
  return (
    <span className="flex flex-wrap items-center gap-x-3 gap-y-1" data-testid="siatd-line">
      <span className={tone}>{text}</span>
      {approver && siatd.state !== "CONFIRMED" && (
        <Button variant="outline" size="sm" onClick={() => setConfirming(true)}>
          {t.confirm}
        </Button>
      )}
      {confirming && <SiatdConfirmDialog operationIds={[operationId]} onClose={() => setConfirming(false)} />}
    </span>
  );
}
