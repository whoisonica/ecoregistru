import { useState } from "react";
import { useConfirmSiatd } from "@/hooks/useSiatd";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useToast } from "@/components/ui/toast";

const t = strings.siatd;

/**
 * F6a — „Confirmat în SIATD” pentru una sau mai multe recepții. Codul SIATD e unic pe tranzacție, deci rubrica apare doar
 * când e una singură; serverul refuză la fel.
 */
export function SiatdConfirmDialog({
  operationIds,
  onClose,
  onDone,
}: {
  operationIds: string[];
  onClose: () => void;
  onDone?: () => void;
}) {
  const confirmMut = useConfirmSiatd();
  const { notify } = useToast();
  const [code, setCode] = useState("");
  const single = operationIds.length === 1;

  async function submit() {
    try {
      await confirmMut.mutateAsync({ operationIds, code: single && code.trim() ? code.trim() : undefined });
      notify(single ? t.confirmed : t.confirmedMany.replace("{count}", String(operationIds.length)), "success");
      onDone?.();
      onClose();
    } catch (err) {
      notify(apiErrorMessage(err, t.error), "error");
    }
  }

  return (
    <Dialog
      open
      onClose={onClose}
      busy={confirmMut.isPending}
      size="md"
      title={single ? t.confirmTitle : t.confirmTitleMany.replace("{count}", String(operationIds.length))}
      description={single ? t.confirmText : t.confirmTextMany}
      footer={
        <>
          <Button variant="outline" onClick={onClose} disabled={confirmMut.isPending}>
            {strings.common.cancel}
          </Button>
          <Button onClick={submit} disabled={confirmMut.isPending}>
            {t.confirm}
          </Button>
        </>
      }
    >
      {single && (
        <div>
          <Label htmlFor="siatd-code">{t.code}</Label>
          <Input
            id="siatd-code"
            value={code}
            maxLength={60}
            onChange={(e) => setCode(e.target.value)}
            className="font-mono"
            aria-describedby="siatd-code-hint"
          />
          <p id="siatd-code-hint" className="mt-1 text-xs text-content-muted">
            {t.codeHint}
          </p>
        </div>
      )}
    </Dialog>
  );
}
