import { useState } from "react";
import { useSaveEnergyCarriers } from "@/hooks/useEnergy";
import { apiErrorMessage } from "@/lib/api";
import { ENERGY_CARRIERS } from "@/lib/energy";
import { strings } from "@/lib/strings";
import type { EnergyCarrier } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Card, CardHeader } from "@/components/ui/card";
import { Dialog } from "@/components/ui/dialog";
import { PillGroup } from "@/components/ui/pill-group";
import { useToast } from "@/components/ui/toast";

const t = strings.energy;

/**
 * Cele 11 rubrici ale Anexei 1, ca taste bifabile. `ChoiceCards` nu permite alegeri multiple, deci
 * `PillGroup multiple` (abaterea de la §4.2, aceeași familie de primitive). Unitatea stă mică lângă nume.
 */
function CarrierPicker({
  selected,
  onToggle,
  disabled,
  labelledBy,
}: {
  selected: EnergyCarrier[];
  onToggle: (carrier: EnergyCarrier) => void;
  disabled?: boolean;
  labelledBy: string;
}) {
  return (
    <PillGroup<EnergyCarrier>
      name="rubrici-energie"
      multiple
      aria-labelledby={labelledBy}
      disabled={disabled}
      options={ENERGY_CARRIERS.map((c) => ({ value: c.id, label: t.carrier[c.id], code: c.unit }))}
      selected={selected}
      onToggle={onToggle}
    />
  );
}

/** Bifarea și salvarea, aceleași în cardul de la început și în dialog. Se salvează pe anul fișei. */
function useCarrierDraft(year: number, initial: EnergyCarrier[], onSaved?: () => void) {
  const [selected, setSelected] = useState<EnergyCarrier[]>(initial);
  const save = useSaveEnergyCarriers();
  const { notify } = useToast();
  const toggle = (carrier: EnergyCarrier) =>
    setSelected((prev) => (prev.includes(carrier) ? prev.filter((c) => c !== carrier) : [...prev, carrier]));
  const submit = () =>
    save.mutate(
      // În ordinea Anexei, nu în ordinea bifării.
      { year, carriers: ENERGY_CARRIERS.map((c) => c.id).filter((id) => selected.includes(id)) },
      {
        onSuccess: () => {
          notify(t.carriersSaved, "success");
          onSaved?.();
        },
        onError: (err) => notify(apiErrorMessage(err, t.carriersError), "error"),
      }
    );
  return { selected, toggle, submit, saving: save.isPending };
}

/** Prima dată, înainte de orice lună: „Ce energie folosește firma?”. */
export function EnergyCarriersCard({ year, canWrite }: { year: number; canWrite: boolean }) {
  const draft = useCarrierDraft(year, []);
  return (
    <Card className="mt-6">
      <CardHeader
        title={<span id="energie-rubrici">{t.pickCarriersTitle}</span>}
        description={canWrite ? t.pickCarriersHint : t.noCarriersReadOnly}
      />
      <div className="mt-4">
        <CarrierPicker
          labelledBy="energie-rubrici"
          selected={draft.selected}
          onToggle={draft.toggle}
          disabled={!canWrite}
        />
      </div>
      {canWrite && (
        <div className="mt-4">
          <Button onClick={draft.submit} loading={draft.saving} disabled={draft.selected.length === 0}>
            {strings.common.save}
          </Button>
        </div>
      )}
    </Card>
  );
}

/**
 * „Schimbă rubricile”: aceleași taste, pornite de la ce e bifat acum, salvate numai pe anul fișei (anii
 * dinainte își păstrează rubricile). Se montează numai deschis: o bifă lăsată nesalvată nu rămâne pentru
 * data viitoare. Fără nicio bifă nu se salvează: un an fără rubrici le-ar lua înapoi pe ale anului dinainte.
 */
export function EnergyCarriersDialog({
  year,
  open,
  onClose,
  current,
}: {
  year: number;
  open: boolean;
  onClose: () => void;
  current: EnergyCarrier[];
}) {
  const draft = useCarrierDraft(year, current, onClose);

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={t.changeCarriers}
      description={t.pickCarriersHint}
      size="xl"
      busy={draft.saving}
      footer={
        <>
          <Button variant="outline" onClick={onClose} disabled={draft.saving}>
            {strings.common.cancel}
          </Button>
          <Button onClick={draft.submit} loading={draft.saving} disabled={draft.selected.length === 0}>
            {strings.common.save}
          </Button>
        </>
      }
    >
      <span id="energie-rubrici-dialog" className="sr-only">
        {t.pickCarriersTitle}
      </span>
      <CarrierPicker labelledBy="energie-rubrici-dialog" selected={draft.selected} onToggle={draft.toggle} />
    </Dialog>
  );
}
