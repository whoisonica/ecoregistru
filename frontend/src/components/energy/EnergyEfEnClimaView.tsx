import { useState } from "react";
import { Info, Trash2 } from "lucide-react";
import { useSaveEnergyDeclaration } from "@/hooks/useEnergy";
import { apiErrorMessage } from "@/lib/api";
import { EFENCLIMA_ORDER, ENERGY_CARRIERS } from "@/lib/energy";
import { strings } from "@/lib/strings";
import type { EnergyDeclaration, EnergyMeasure, EnergySheet } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { DateInput } from "@/components/ui/date-input";
import { FormSection } from "@/components/ui/form-section";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PillGroup } from "@/components/ui/pill-group";
import { Table, TBody, TD, TH, THead, TR } from "@/components/ui/table";
import { Tooltip } from "@/components/ui/tooltip";
import { useToast } from "@/components/ui/toast";
import { figure } from "@/components/energy/EnergyMonthsTable";

const t = strings.energy;
const MAX_MEASURES = 20;

const MEASURE_FIELDS = [
  "costEstimated",
  "costActual",
  "savingsTepEstimated",
  "savingsTepActual",
  "savingsCostEstimated",
  "savingsCostActual",
] as const;
type MeasureField = (typeof MEASURE_FIELDS)[number];
type MeasureDraft = { name: string } & Record<MeasureField, string>;

const text = (n: number | null) => (n == null ? "" : String(n).replace(".", ","));
/** `""` → null; altfel numărul, sau `NaN` pentru ce nu e un număr ≥ 0. */
function num(s: string): number | null {
  const v = s.trim().replace(/\s/g, "").replace(",", ".");
  if (v === "") return null;
  const n = Number(v);
  return Number.isFinite(n) && n >= 0 ? n : NaN;
}
const toDraft = (m: EnergyMeasure): MeasureDraft => ({
  name: m.name ?? "",
  ...(Object.fromEntries(MEASURE_FIELDS.map((f) => [f, text(m[f])])) as Record<MeasureField, string>),
});
const emptyMeasure = (): MeasureDraft => ({
  name: "",
  ...(Object.fromEntries(MEASURE_FIELDS.map((f) => [f, ""])) as Record<MeasureField, string>),
});

const fieldName = (f: MeasureField) =>
  `${f.startsWith("cost") ? t.measureCost : f.startsWith("savingsTep") ? t.measureSavingsTep : t.measureSavingsCost}, ${
    f.endsWith("Estimated") ? t.estimated : t.actual
  }`;

/** Da / Nu ca taste; apăsat a doua oară, răspunsul se golește (un răspuns lipsă rămâne gol). */
function YesNo({
  name,
  value,
  onChange,
  disabled,
  labelledBy,
}: {
  name: string;
  value: boolean | null;
  onChange: (v: boolean | null) => void;
  disabled: boolean;
  labelledBy: string;
}) {
  const selected = value == null ? [] : [value ? "da" : "nu"];
  return (
    <PillGroup<"da" | "nu">
      name={name}
      aria-labelledby={labelledBy}
      disabled={disabled}
      options={[
        { value: "da", label: t.yes },
        { value: "nu", label: t.no },
      ]}
      selected={selected as ("da" | "nu")[]}
      onToggle={(v) => onChange(selected[0] === v ? null : v === "da")}
    />
  );
}

/**
 * Ce se transcrie pe EfEnClima.ro: totalurile anului în ordinea formularului platformei, IMM-ul,
 * auditul și măsurile (la non-IMM) și cele două întrebări POIM, care stau doar pe Anexa 1.
 * Se montează cu `key={year}`, deci ciorna pornește de la fișa anului ales.
 */
export function EnergyEfEnClimaView({ sheet, canWrite }: { sheet: EnergySheet; canWrite: boolean }) {
  const d = sheet.declaration;
  const [form, setForm] = useState<Omit<EnergyDeclaration, "measures" | "auditSharePct"> & { share: string }>({
    sme: d.sme,
    auditDate: d.auditDate,
    auditor: d.auditor,
    auditScope: d.auditScope,
    share: text(d.auditSharePct),
    poimInterest: d.poimInterest,
    poimProject: d.poimProject,
  });
  const [measures, setMeasures] = useState<MeasureDraft[]>(d.measures.map(toDraft));
  const [invalid, setInvalid] = useState(false);
  const save = useSaveEnergyDeclaration();
  const { notify } = useToast();
  const set = <K extends keyof typeof form>(k: K, v: (typeof form)[K]) => setForm((f) => ({ ...f, [k]: v }));
  const setMeasure = (i: number, k: keyof MeasureDraft, v: string) =>
    setMeasures((ms) => ms.map((m, j) => (j === i ? { ...m, [k]: v } : m)));
  const off = !canWrite;

  function submit() {
    // Auditul și măsurile sunt ale non-IMM-ului: numai „IMM = Da” le golește. „Nu se știe” le păstrează pe cele
    // salvate (ascunse pe ecran), ca o bifă scoasă din greșeală să nu șteargă auditul trecut.
    const audit = form.sme !== true;
    const share = audit ? num(form.share) : null;
    const parsed = (audit ? measures : [])
      .map((m) => ({
        name: m.name.trim() || null,
        ...(Object.fromEntries(MEASURE_FIELDS.map((f) => [f, num(m[f])])) as Record<MeasureField, number | null>),
      }))
      // Un rând gol s-ar tipări pe Anexa 1 ca rând numerotat fără nimic în el.
      .filter((m) => m.name != null || MEASURE_FIELDS.some((f) => m[f] != null))
      .map((m, i) => ({ position: i + 1, ...m }));
    const bad =
      Number.isNaN(share) ||
      (share != null && share > 100) ||
      parsed.some((m) => MEASURE_FIELDS.some((f) => Number.isNaN(m[f])));
    setInvalid(bad);
    if (bad) return;
    save.mutate(
      {
        year: sheet.year,
        sme: form.sme,
        auditDate: (audit && form.auditDate) || null,
        auditor: (audit && form.auditor?.trim()) || null,
        auditScope: (audit && form.auditScope?.trim()) || null,
        auditSharePct: share,
        poimInterest: form.poimInterest,
        poimProject: form.poimProject,
        measures: parsed,
      },
      {
        onSuccess: () => notify(t.declarationSaved, "success"),
        onError: (err) => notify(apiErrorMessage(err, t.declarationError), "error"),
      }
    );
  }

  return (
    <div className="mt-4 grid gap-6 lg:grid-cols-[minmax(16rem,22rem)_1fr]">
      <section aria-labelledby="ef-totaluri">
        <h2 id="ef-totaluri" className="flex items-center gap-1 text-[0.9375rem] font-semibold">
          {t.totalsTitle}
          <Tooltip content={t.tooltipMissing}>
            <Info className="h-3.5 w-3.5 text-content-subtle" aria-label={t.tooltipMissing} />
          </Tooltip>
        </h2>
        <Table className="mt-2">
          <TBody>
            {EFENCLIMA_ORDER.map((carrier) => {
              const used = sheet.carriers.includes(carrier);
              const total = sheet.totals.find((tot) => tot.carrier === carrier);
              const unit = ENERGY_CARRIERS.find((c) => c.id === carrier)?.unit;
              return (
                <TR key={carrier} className={used ? undefined : "text-content-subtle"}>
                  <TD className="py-1.5 text-inherit">{t.carrier[carrier]}</TD>
                  <TD className="whitespace-nowrap py-1.5 text-right font-mono text-inherit">
                    {used ? figure(total?.quantity) : figure(0)}
                  </TD>
                  <TD className="py-1.5 pl-0 font-mono text-[0.6875rem] text-content-subtle">{unit}</TD>
                </TR>
              );
            })}
          </TBody>
        </Table>
      </section>

      <div className="min-w-0 space-y-5">
        <div className="flex flex-wrap items-center gap-3">
          <span id="ef-imm" className="text-sm font-medium">
            {t.smeQuestion}
          </span>
          <YesNo name="imm" labelledBy="ef-imm" value={form.sme} onChange={(v) => set("sme", v)} disabled={off} />
        </div>

        {form.sme === false && (
          <FormSection title={t.auditTitle}>
            <div className="grid gap-3 sm:grid-cols-2">
              <div>
                <Label htmlFor="ef-audit-date">{t.auditDate}</Label>
                <DateInput
                  id="ef-audit-date"
                  value={form.auditDate ?? ""}
                  onChange={(e) => set("auditDate", e.target.value || null)}
                  disabled={off}
                />
              </div>
              <div>
                <Label htmlFor="ef-auditor">{t.auditor}</Label>
                <Input id="ef-auditor" value={form.auditor ?? ""} onChange={(e) => set("auditor", e.target.value)} disabled={off} />
              </div>
              <div>
                <Label htmlFor="ef-scope">{t.auditScope}</Label>
                <Input id="ef-scope" value={form.auditScope ?? ""} onChange={(e) => set("auditScope", e.target.value)} disabled={off} />
              </div>
              <div>
                <Label htmlFor="ef-share">{t.auditSharePct}</Label>
                <Input id="ef-share" inputMode="decimal" value={form.share} onChange={(e) => set("share", e.target.value)} disabled={off} />
              </div>
            </div>
            <MeasuresTable measures={measures} setMeasure={setMeasure} setMeasures={setMeasures} off={off} />
          </FormSection>
        )}

        <FormSection title={t.annexOnly}>
          {(
            [
              ["poimInterest", t.poimInterest],
              ["poimProject", t.poimProject],
            ] as const
          ).map(([k, question]) => (
            <div key={k} className="flex flex-wrap items-center justify-between gap-x-4 gap-y-2">
              <span id={`ef-${k}`} className="text-sm">
                {question}
              </span>
              <YesNo name={k} labelledBy={`ef-${k}`} value={form[k]} onChange={(v) => set(k, v)} disabled={off} />
            </div>
          ))}
        </FormSection>

        {canWrite && (
          <div className="flex flex-wrap items-center gap-3">
            <Button onClick={submit} loading={save.isPending}>
              {strings.common.save}
            </Button>
            {invalid && (
              <p role="alert" className="text-sm text-state-bad-text">
                {t.numbersInvalid}
              </p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

function MeasuresTable({
  measures,
  setMeasure,
  setMeasures,
  off,
}: {
  measures: MeasureDraft[];
  setMeasure: (i: number, k: keyof MeasureDraft, v: string) => void;
  setMeasures: (f: (ms: MeasureDraft[]) => MeasureDraft[]) => void;
  off: boolean;
}) {
  return (
    <div className="space-y-2">
      <h4 className="text-sm font-semibold">{t.measuresTitle}</h4>
      {measures.length === 0 ? (
        <p className="text-sm text-content-muted">{t.noMeasures}</p>
      ) : (
        <Table>
          <THead>
            <TR>
              <TH rowSpan={2}>{t.measureName}</TH>
              <TH colSpan={2} className="text-center">{t.measureCost}</TH>
              <TH colSpan={2} className="text-center">{t.measureSavingsTep}</TH>
              <TH colSpan={2} className="text-center">{t.measureSavingsCost}</TH>
              {!off && <TH rowSpan={2} sticky="right" />}
            </TR>
            <TR>
              {MEASURE_FIELDS.map((f) => (
                <TH key={f} className="px-1 text-right normal-case">
                  {f.endsWith("Estimated") ? t.estimated : t.actual}
                </TH>
              ))}
            </TR>
          </THead>
          <TBody>
            {measures.map((m, i) => (
              <TR key={i}>
                <TD className="min-w-[10rem] px-1 py-1">
                  <Input
                    aria-label={t.measureFieldLabel.replace("{n}", String(i + 1)).replace("{field}", t.measureName)}
                    value={m.name}
                    onChange={(e) => setMeasure(i, "name", e.target.value)}
                    disabled={off}
                    className="h-8"
                  />
                </TD>
                {MEASURE_FIELDS.map((f) => (
                  <TD key={f} className="px-1 py-1">
                    <Input
                      inputMode="decimal"
                      aria-label={t.measureFieldLabel.replace("{n}", String(i + 1)).replace("{field}", fieldName(f))}
                      value={m[f]}
                      onChange={(e) => setMeasure(i, f, e.target.value)}
                      disabled={off}
                      className="h-8 w-[4.5rem] px-1.5 text-right"
                    />
                  </TD>
                ))}
                {!off && (
                  <TD sticky="right" className="px-1 py-1">
                    <Button
                      variant="danger-ghost"
                      size="icon-sm"
                      aria-label={t.removeMeasure.replace("{n}", String(i + 1))}
                      onClick={() => setMeasures((ms) => ms.filter((_, j) => j !== i))}
                    >
                      <Trash2 className="h-4 w-4" aria-hidden />
                    </Button>
                  </TD>
                )}
              </TR>
            ))}
          </TBody>
        </Table>
      )}
      {!off && (
        <Button
          variant="outline"
          size="sm"
          disabled={measures.length >= MAX_MEASURES}
          onClick={() => setMeasures((ms) => [...ms, emptyMeasure()])}
        >
          {t.addMeasure}
        </Button>
      )}
    </div>
  );
}
