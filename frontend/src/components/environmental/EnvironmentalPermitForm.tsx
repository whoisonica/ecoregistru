import { useState } from "react";
import { Info } from "lucide-react";
import { useSaveEnvironmentalPermit, type EnvironmentalPermitInput } from "@/hooks/useEnvironmentalReport";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import type { EnvironmentalPermit } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { DateInput } from "@/components/ui/date-input";
import { FormSection } from "@/components/ui/form-section";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Tooltip } from "@/components/ui/tooltip";
import { useToast } from "@/components/ui/toast";

const t = strings.environmentalReport;

/** „21 octombrie”. */
const dayName = (month: number, day: number) => `${day} ${strings.months[month - 1].toLowerCase()}`;

/**
 * Autorizația de mediu a firmei, cum o tipărește RAM-ul, și cele două termene care se iau din ea: raportul (ziua din
 * autorizație, 15 ianuarie implicit) și cererea de viză (socotită din data emiterii; se poate scrie altă zi).
 * Salvată, mută termenele deschise din „Termene”.
 */
export function EnvironmentalPermitForm({
  year,
  permit,
  canWrite,
}: {
  year: number;
  permit: EnvironmentalPermit;
  canWrite: boolean;
}) {
  const [form, setForm] = useState<EnvironmentalPermitInput>({
    number: permit.number,
    issuer: permit.issuer,
    issuedOn: permit.issuedOn,
    revisedOn: permit.revisedOn,
    visaOn: permit.visaOn,
    ramDueMonth: permit.ramDueMonth,
    ramDueDay: permit.ramDueDay,
    visaDueMonth: permit.visaDueMonth,
    visaDueDay: permit.visaDueDay,
    copyTo: permit.copyTo,
    administratorName: permit.administratorName,
  });
  const save = useSaveEnvironmentalPermit();
  const { notify } = useToast();
  const off = !canWrite;
  const set = <K extends keyof EnvironmentalPermitInput>(k: K, v: EnvironmentalPermitInput[K]) =>
    setForm((f) => ({ ...f, [k]: v }));
  const ownVisaDay = form.visaDueMonth != null;
  // Ziua socotită se citește din ce a salvat serverul; o dată a emiterii schimbată pe ecran o mută la salvare.
  const computed =
    permit.visaComputedMonth != null && permit.visaComputedDay != null
      ? dayName(permit.visaComputedMonth, permit.visaComputedDay)
      : null;

  function submit() {
    save.mutate(
      { year, permit: form },
      {
        onSuccess: () => notify(t.permitSaved, "success"),
        onError: (err) => notify(apiErrorMessage(err, t.permitError), "error"),
      }
    );
  }

  const text = (id: string, label: string, k: "number" | "issuer" | "copyTo" | "administratorName", placeholder?: string) => (
    <div>
      <Label htmlFor={id}>{label}</Label>
      <Input id={id} value={form[k] ?? ""} placeholder={placeholder} disabled={off}
        onChange={(e) => set(k, e.target.value || null)} />
    </div>
  );
  const date = (id: string, label: string, k: "issuedOn" | "revisedOn" | "visaOn") => (
    <div>
      <Label htmlFor={id}>{label}</Label>
      <DateInput id={id} value={form[k] ?? ""} disabled={off} onChange={(e) => set(k, e.target.value || null)} />
    </div>
  );

  return (
    <div className="mt-5 max-w-4xl space-y-6">
      <FormSection title={t.tabPermit}>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {text("ram-aut-nr", t.permitNumber, "number")}
          <div className="lg:col-span-2">{text("ram-aut-emitent", t.permitIssuer, "issuer", t.permitIssuerPlaceholder)}</div>
          {date("ram-aut-emisa", t.permitIssuedOn, "issuedOn")}
          {date("ram-aut-revizuita", t.permitRevisedOn, "revisedOn")}
          {date("ram-aut-viza", t.permitVisaOn, "visaOn")}
          <div className="lg:col-span-2">{text("ram-aut-stiinta", t.copyTo, "copyTo", t.copyToPlaceholder)}</div>
          {text("ram-aut-admin", t.administrator, "administratorName")}
        </div>
      </FormSection>

      <FormSection title={t.ramDue}>
        <div className="flex flex-wrap items-end gap-3">
          <MonthDay
            id="ram-termen"
            month={form.ramDueMonth}
            day={form.ramDueDay}
            onChange={(m, d) => setForm((f) => ({ ...f, ramDueMonth: m, ramDueDay: d }))}
            off={off}
          />
          <Tooltip content={t.ramDueHint}>
            <Info className="mb-3 h-3.5 w-3.5 text-content-subtle" aria-label={t.ramDueHint} />
          </Tooltip>
        </div>
      </FormSection>

      <FormSection title={t.visaDue}>
        {ownVisaDay ? (
          <div className="flex flex-wrap items-end gap-3">
            <MonthDay
              id="ram-viza"
              month={form.visaDueMonth ?? 1}
              day={form.visaDueDay ?? 1}
              onChange={(m, d) => setForm((f) => ({ ...f, visaDueMonth: m, visaDueDay: d }))}
              off={off}
            />
            {!off && (
              <Button variant="ghost" size="sm" className="mb-1"
                onClick={() => setForm((f) => ({ ...f, visaDueMonth: null, visaDueDay: null }))}>
                {t.visaComputedBack}
              </Button>
            )}
          </div>
        ) : (
          <div className="flex flex-wrap items-center gap-3">
            <p className="text-sm">{computed ? t.visaComputed.replace("{date}", computed) : t.visaNoDate}</p>
            {!off && (
              <Button variant="outline" size="sm"
                onClick={() =>
                  setForm((f) => ({
                    ...f,
                    visaDueMonth: permit.visaComputedMonth ?? 1,
                    visaDueDay: permit.visaComputedDay ?? 1,
                  }))
                }>
                {t.visaOwnDay}
              </Button>
            )}
          </div>
        )}
      </FormSection>

      {canWrite && (
        <Button onClick={submit} loading={save.isPending}>
          {strings.common.save}
        </Button>
      )}
    </div>
  );
}

/** Ziua și luna unui termen anual. O zi care nu există în lună (30 februarie) o refuză serverul. */
function MonthDay({
  id,
  month,
  day,
  onChange,
  off,
}: {
  id: string;
  month: number;
  day: number;
  onChange: (month: number, day: number) => void;
  off: boolean;
}) {
  return (
    <>
      <div>
        <Label htmlFor={`${id}-zi`}>{t.day}</Label>
        <Select id={`${id}-zi`} className="w-20" value={String(day)} disabled={off}
          onChange={(e) => onChange(month, Number(e.target.value))}>
          {Array.from({ length: 31 }, (_, i) => (
            <option key={i} value={i + 1}>
              {i + 1}
            </option>
          ))}
        </Select>
      </div>
      <div>
        <Label htmlFor={`${id}-luna`}>{t.month}</Label>
        <Select id={`${id}-luna`} className="w-40" value={String(month)} disabled={off}
          onChange={(e) => onChange(Number(e.target.value), day)}>
          {strings.months.map((name, i) => (
            <option key={name} value={i + 1}>
              {name}
            </option>
          ))}
        </Select>
      </div>
    </>
  );
}
