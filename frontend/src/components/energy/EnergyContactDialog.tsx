import { useState } from "react";
import { Info } from "lucide-react";
import { useSaveEnergyContact } from "@/hooks/useEnergy";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import type { EnergyContact } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { DateInput } from "@/components/ui/date-input";
import { Dialog } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Tooltip } from "@/components/ui/tooltip";
import { useToast } from "@/components/ui/toast";

const t = strings.energy;

type TextField = Exclude<keyof EnergyContact, "attestedOn">;

const FIELDS: { key: TextField; label: string; type?: string; wide?: boolean }[] = [
  { key: "fax", label: t.contactFax },
  { key: "website", label: t.contactWebsite },
  { key: "activitySector", label: t.contactSector, wide: true },
  { key: "name", label: t.contactPerson, wide: true },
  { key: "email", label: t.contactEmail, type: "email" },
  { key: "phone", label: t.contactPhone, type: "tel" },
  { key: "mobile", label: t.contactMobile, type: "tel" },
];

/**
 * Datele de contact pe care le tipărește Anexa 1 și nu le are fișa firmei. Pornesc goale. Se montează
 * numai deschis, deci ciorna pornește de fiecare dată de la ce e salvat.
 */
export function EnergyContactDialog({
  open,
  onClose,
  contact,
}: {
  open: boolean;
  onClose: () => void;
  contact: EnergyContact;
}) {
  const [form, setForm] = useState<EnergyContact>(contact);
  const [error, setError] = useState<string | null>(null);
  const save = useSaveEnergyContact();
  const { notify } = useToast();

  function submit() {
    const clean = Object.fromEntries(
      Object.entries(form).map(([k, v]) => [k, typeof v === "string" && v.trim() !== "" ? v.trim() : null])
    ) as unknown as EnergyContact;
    save.mutate(clean, {
      onSuccess: () => {
        notify(t.contactSaved, "success");
        onClose();
      },
      onError: (err) => setError(apiErrorMessage(err, t.contactError)),
    });
  }

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={t.contactTitle}
      description={t.contactDescription}
      size="xl"
      busy={save.isPending}
      footer={
        <>
          <Button variant="outline" onClick={onClose} disabled={save.isPending}>
            {strings.common.cancel}
          </Button>
          <Button onClick={submit} loading={save.isPending}>
            {strings.common.save}
          </Button>
        </>
      }
    >
      <div className="grid gap-3 sm:grid-cols-2">
        {FIELDS.map((f) => (
          <div key={f.key} className={f.wide ? "sm:col-span-2" : undefined}>
            <Label htmlFor={`energie-${f.key}`}>{f.label}</Label>
            <Input
              id={`energie-${f.key}`}
              type={f.type ?? "text"}
              value={form[f.key] ?? ""}
              onChange={(e) => setForm((prev) => ({ ...prev, [f.key]: e.target.value }))}
            />
          </div>
        ))}
        <div>
          <div className="flex items-center gap-1">
            <Label htmlFor="energie-attestedOn">{t.contactAttestedOn}</Label>
            <span className="mb-1">
              <Tooltip content={t.tooltipAttested}>
                <Info className="h-3.5 w-3.5 text-content-subtle" aria-label={t.tooltipAttested} />
              </Tooltip>
            </span>
          </div>
          <DateInput
            id="energie-attestedOn"
            value={form.attestedOn ?? ""}
            onChange={(e) => setForm((prev) => ({ ...prev, attestedOn: e.target.value || null }))}
          />
        </div>
        {error && (
          <p role="alert" className="text-sm text-state-bad-text sm:col-span-2">
            {error}
          </p>
        )}
      </div>
    </Dialog>
  );
}
