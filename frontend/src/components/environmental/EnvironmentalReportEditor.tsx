import { useState } from "react";
import { Info, Trash2 } from "lucide-react";
import { useSaveEnvironmentalReport } from "@/hooks/useEnvironmentalReport";
import { useWorkPoints } from "@/hooks/useWorkPoints";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import type { EnvironmentalInspection, EnvironmentalMonitoring, EnvironmentalReport } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { DateInput } from "@/components/ui/date-input";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PillGroup } from "@/components/ui/pill-group";
import { Textarea } from "@/components/ui/textarea";
import { Tooltip } from "@/components/ui/tooltip";
import { useToast } from "@/components/ui/toast";

const t = strings.environmentalReport;
const MAX_ROWS = 50;
const WORK_POINTS_LIST = "ram-puncte-de-lucru";

type Answers = Omit<EnvironmentalReport, "year" | "permit" | "inspections" | "monitorings">;

const emptyInspection = (): EnvironmentalInspection => ({
  workPoint: null,
  institution: null,
  reportNumber: null,
  reportDate: null,
  measures: null,
});
const emptyMonitoring = (): EnvironmentalMonitoring => ({
  workPoint: null,
  factor: null,
  performedOn: null,
  testReportNumber: null,
  testReportDate: null,
  result: null,
});
const filled = (o: object) => Object.values(o).some((v) => v != null && String(v).trim() !== "");

/** Da / Nu ca taste; apăsat a doua oară, răspunsul se golește (un răspuns lipsă rămâne gol pe raport). */
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
 * Anul raportului: întrebările, controalele și monitorizările, într-o singură ciornă pentru toate cele trei taburi —
 * trecând de la unul la altul nu se pierde nimic, iar „Salvează” trimite anul întreg. Se montează cu `key={year}`.
 */
export function EnvironmentalReportEditor({
  report,
  tab,
  canWrite,
}: {
  report: EnvironmentalReport;
  tab: string;
  canWrite: boolean;
}) {
  const [answers, setAnswers] = useState<Answers>({
    incidents: report.incidents,
    incidentsText: report.incidentsText,
    complaints: report.complaints,
    complaintsText: report.complaintsText,
    inspected: report.inspected,
    damage: report.damage,
    damageText: report.damageText,
    otherRequirements: report.otherRequirements,
  });
  const [inspections, setInspections] = useState<EnvironmentalInspection[]>(report.inspections);
  const [monitorings, setMonitorings] = useState<EnvironmentalMonitoring[]>(report.monitorings);
  const save = useSaveEnvironmentalReport();
  const workPoints = useWorkPoints();
  const { notify } = useToast();
  const off = !canWrite;
  const set = <K extends keyof Answers>(k: K, v: Answers[K]) => setAnswers((a) => ({ ...a, [k]: v }));

  function submit() {
    save.mutate(
      {
        year: report.year,
        ...answers,
        // Un control pe „Nu” n-are ce lista: rândurile rămase s-ar tipări sub „nu au avut loc controale”.
        inspections: answers.inspected === false ? [] : inspections.filter(filled),
        monitorings: monitorings.filter(filled),
      },
      {
        onSuccess: () => notify(t.saved, "success"),
        onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
      }
    );
  }

  return (
    <div className="mt-5 max-w-4xl space-y-5">
      <datalist id={WORK_POINTS_LIST}>
        {(workPoints.data ?? []).map((w) => (
          <option key={w.id} value={w.name} />
        ))}
      </datalist>

      {tab === "" && (
        <>
          <Question
            id="ram-incidente"
            question={t.incidentsQuestion}
            value={answers.incidents}
            onChange={(v) => set("incidents", v)}
            text={answers.incidentsText}
            onText={(v) => set("incidentsText", v)}
            off={off}
          />
          <Question
            id="ram-reclamatii"
            question={t.complaintsQuestion}
            value={answers.complaints}
            onChange={(v) => set("complaints", v)}
            text={answers.complaintsText}
            onText={(v) => set("complaintsText", v)}
            off={off}
          />
          <Question
            id="ram-prejudiciu"
            question={t.damageQuestion}
            value={answers.damage}
            onChange={(v) => set("damage", v)}
            text={answers.damageText}
            onText={(v) => set("damageText", v)}
            off={off}
          />
          <div>
            <Label htmlFor="ram-alte-cerinte" className="inline-flex items-center gap-1">
              {t.otherRequirements}
              <Tooltip content={t.otherRequirementsHint}>
                <Info className="h-3.5 w-3.5 text-content-subtle" aria-label={t.otherRequirementsHint} />
              </Tooltip>
            </Label>
            <Textarea
              id="ram-alte-cerinte"
              rows={3}
              value={answers.otherRequirements ?? ""}
              onChange={(e) => set("otherRequirements", e.target.value)}
              disabled={off}
            />
          </div>
        </>
      )}

      {tab === "controale" && (
        <>
          <div className="flex flex-wrap items-center gap-3">
            <span id="ram-control" className="text-sm font-medium">
              {t.inspectedQuestion}
            </span>
            <YesNo
              name="control"
              labelledBy="ram-control"
              value={answers.inspected}
              onChange={(v) => set("inspected", v)}
              disabled={off}
            />
          </div>
          {answers.inspected === true && (
            <Rows
              items={inspections}
              setItems={setInspections}
              empty={emptyInspection}
              title={t.inspection}
              addLabel={t.addInspection}
              removeLabel={t.removeInspection}
              noneLabel={t.noInspections}
              off={off}
              render={(item, i, change) => (
                <>
                  <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                    <Field id={`ram-c${i}-inst`} label={t.institution}>
                      <Input id={`ram-c${i}-inst`} value={item.institution ?? ""} disabled={off}
                        onChange={(e) => change({ institution: e.target.value })} />
                    </Field>
                    <Field id={`ram-c${i}-punct`} label={t.workPoint}>
                      <Input id={`ram-c${i}-punct`} list={WORK_POINTS_LIST} value={item.workPoint ?? ""} disabled={off}
                        onChange={(e) => change({ workPoint: e.target.value })} />
                    </Field>
                    <Field id={`ram-c${i}-nr`} label={t.reportNumber}>
                      <Input id={`ram-c${i}-nr`} value={item.reportNumber ?? ""} disabled={off}
                        onChange={(e) => change({ reportNumber: e.target.value })} />
                    </Field>
                    <Field id={`ram-c${i}-data`} label={t.reportDate}>
                      <DateInput id={`ram-c${i}-data`} value={item.reportDate ?? ""} disabled={off}
                        onChange={(e) => change({ reportDate: e.target.value || null })} />
                    </Field>
                  </div>
                  <Field id={`ram-c${i}-masuri`} label={t.measures}>
                    <Textarea id={`ram-c${i}-masuri`} rows={2} value={item.measures ?? ""} disabled={off}
                      onChange={(e) => change({ measures: e.target.value })} />
                  </Field>
                </>
              )}
            />
          )}
        </>
      )}

      {tab === "monitorizari" && (
        <Rows
          items={monitorings}
          setItems={setMonitorings}
          empty={emptyMonitoring}
          title={t.monitoring}
          addLabel={t.addMonitoring}
          removeLabel={t.removeMonitoring}
          noneLabel={t.noMonitorings}
          off={off}
          render={(item, i, change) => (
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              <Field id={`ram-m${i}-factor`} label={t.factor}>
                <Input id={`ram-m${i}-factor`} placeholder={t.factorPlaceholder} value={item.factor ?? ""}
                  disabled={off} onChange={(e) => change({ factor: e.target.value })} />
              </Field>
              <Field id={`ram-m${i}-punct`} label={t.workPoint}>
                <Input id={`ram-m${i}-punct`} list={WORK_POINTS_LIST} value={item.workPoint ?? ""} disabled={off}
                  onChange={(e) => change({ workPoint: e.target.value })} />
              </Field>
              <Field id={`ram-m${i}-data`} label={t.performedOn}>
                <DateInput id={`ram-m${i}-data`} value={item.performedOn ?? ""} disabled={off}
                  onChange={(e) => change({ performedOn: e.target.value || null })} />
              </Field>
              <Field id={`ram-m${i}-nr`} label={t.testReportNumber}>
                <Input id={`ram-m${i}-nr`} value={item.testReportNumber ?? ""} disabled={off}
                  onChange={(e) => change({ testReportNumber: e.target.value })} />
              </Field>
              <Field id={`ram-m${i}-datar`} label={t.testReportDate}>
                <DateInput id={`ram-m${i}-datar`} value={item.testReportDate ?? ""} disabled={off}
                  onChange={(e) => change({ testReportDate: e.target.value || null })} />
              </Field>
              <Field id={`ram-m${i}-rez`} label={t.result}>
                <Input id={`ram-m${i}-rez`} placeholder={t.resultPlaceholder} value={item.result ?? ""}
                  disabled={off} onChange={(e) => change({ result: e.target.value })} />
              </Field>
            </div>
          )}
        />
      )}

      {canWrite && (
        <Button onClick={submit} loading={save.isPending}>
          {strings.common.save}
        </Button>
      )}
    </div>
  );
}

function Question({
  id,
  question,
  value,
  onChange,
  text,
  onText,
  off,
}: {
  id: string;
  question: string;
  value: boolean | null;
  onChange: (v: boolean | null) => void;
  text: string | null;
  onText: (v: string) => void;
  off: boolean;
}) {
  return (
    <div className="space-y-2">
      <div className="flex flex-wrap items-center gap-3">
        <span id={id} className="inline-flex items-center gap-1 text-sm font-medium">
          {question}
          <Tooltip content={t.noAnswerHint}>
            <Info className="h-3.5 w-3.5 text-content-subtle" aria-label={t.noAnswerHint} />
          </Tooltip>
        </span>
        <YesNo name={id} labelledBy={id} value={value} onChange={onChange} disabled={off} />
      </div>
      {value === true && (
        <Field id={`${id}-text`} label={t.whatHappened}>
          <Textarea id={`${id}-text`} rows={2} value={text ?? ""} disabled={off}
            onChange={(e) => onText(e.target.value)} />
        </Field>
      )}
    </div>
  );
}

function Field({ id, label, children }: { id: string; label: string; children: React.ReactNode }) {
  return (
    <div>
      <Label htmlFor={id}>{label}</Label>
      {children}
    </div>
  );
}

/** O listă de blocuri (controale, monitorizări), fiecare cu numărul lui și „Șterge”. */
function Rows<T extends object>({
  items,
  setItems,
  empty,
  title,
  addLabel,
  removeLabel,
  noneLabel,
  off,
  render,
}: {
  items: T[];
  setItems: (f: (items: T[]) => T[]) => void;
  empty: () => T;
  title: string;
  addLabel: string;
  removeLabel: string;
  noneLabel: string;
  off: boolean;
  render: (item: T, i: number, change: (patch: Partial<T>) => void) => React.ReactNode;
}) {
  return (
    <div className="space-y-5">
      {items.length === 0 && <p className="text-sm text-content-muted">{noneLabel}</p>}
      {items.map((item, i) => (
        <section key={i} className="space-y-3" aria-label={title.replace("{n}", String(i + 1))}>
          {/* Titlul ca la `FormSection`, dar cu „Șterge” pe același rând, la dreapta. */}
          <div className="flex items-end justify-between gap-3 border-b-2 border-content pb-1.5">
            <h3 className="text-[0.9375rem] font-semibold text-content">{title.replace("{n}", String(i + 1))}</h3>
            {!off && (
              <Button
                variant="danger-ghost"
                size="sm"
                className="h-7"
                aria-label={removeLabel.replace("{n}", String(i + 1))}
                onClick={() => setItems((all) => all.filter((_, j) => j !== i))}
              >
                <Trash2 className="mr-1 h-3.5 w-3.5" aria-hidden />
                {t.remove}
              </Button>
            )}
          </div>
          {render(item, i, (patch) => setItems((all) => all.map((x, j) => (j === i ? { ...x, ...patch } : x))))}
        </section>
      ))}
      {!off && (
        <Button
          variant="outline"
          size="sm"
          disabled={items.length >= MAX_ROWS}
          onClick={() => setItems((all) => [...all, empty()])}
        >
          {addLabel}
        </Button>
      )}
    </div>
  );
}
