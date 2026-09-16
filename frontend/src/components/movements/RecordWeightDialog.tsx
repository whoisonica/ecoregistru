import { useState, type FormEvent } from "react";
import { useRecordWeight } from "@/hooks/useMovements";
import { useDeclaration } from "@/hooks/useDeadlines";
import { declaredText } from "@/lib/deadlines";
import type { Unit, WasteMovement } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { formatDate } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Dialog } from "@/components/ui/dialog";
import { useToast } from "@/components/ui/toast";

const t = strings.movements;
const e = strings.enums;

/**
 * Fills in the weight the recipient sent back, and nothing else.
 *
 * <p>Asked for on 24.08.2026: the movement form greys the quantity out while "se cântărește la
 * descărcare" is ticked, so the only way to add the figure later was to untick the box — which
 * threw away the fact that the recipient did the weighing. One field, one call, and the monthly
 * line stops being provisional.
 */
export function RecordWeightDialog({
  movement,
  onClose,
}: {
  movement: WasteMovement;
  onClose: () => void;
}) {
  const { notify } = useToast();
  const recordMut = useRecordWeight();
  const year = Number(movement.date.slice(0, 4));
  const declaration = useDeclaration(year);
  const [quantity, setQuantity] = useState("");
  const [unit, setUnit] = useState<Unit>(movement.unit);

  function submit(ev: FormEvent) {
    ev.preventDefault();
    const value = Number(quantity);
    if (!Number.isFinite(value) || value <= 0) {
      notify(t.recordWeightError, "error");
      return;
    }
    recordMut.mutate(
      { id: movement.id, quantity: value, unit },
      {
        onSuccess: () => {
          notify(t.recordWeightSaved, "success");
          onClose();
        },
        onError: (err) => notify(apiErrorMessage(err, t.recordWeightError), "error"),
      }
    );
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title={t.recordWeightTitle}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {strings.common.cancel}
          </Button>
          <Button type="submit" form="weight-form" disabled={recordMut.isPending}>
            {recordMut.isPending ? strings.common.saving : strings.common.save}
          </Button>
        </>
      }
    >
      <form id="weight-form" onSubmit={submit} className="space-y-3">
        <p className="text-sm text-content-strong">
          {movement.wasteCode} — {movement.wasteCodeName}
          {movement.partnerName ? `, ${movement.partnerName}` : ""}, {formatDate(movement.date)}
        </p>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div>
            <Label htmlFor="wg-qty">{t.quantity}</Label>
            <Input
              id="wg-qty"
              type="number"
              step="0.001"
              min="0"
              autoFocus
              value={quantity}
              onChange={(ev) => setQuantity(ev.target.value)}
            />
          </div>
          <div>
            <Label htmlFor="wg-unit">{t.unit}</Label>
            <Select id="wg-unit" value={unit} onChange={(ev) => setUnit(ev.target.value as Unit)}>
              <option value="KG">{e.unit.KG}</option>
              <option value="TONS">{e.unit.TONS}</option>
            </Select>
          </div>
        </div>
        <p className="text-xs text-content-muted">{t.recordWeightHint}</p>
        {declaration && (
          <p className="text-xs font-medium text-content" data-testid="declared-year">
            {declaredText(t.declaredWeight, year, declaration)}
          </p>
        )}
      </form>
    </Dialog>
  );
}
