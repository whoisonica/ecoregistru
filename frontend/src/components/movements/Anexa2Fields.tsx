import type { Anexa2Threshold } from "@/lib/types";
import { strings } from "@/lib/strings";
import { formatTonnesValue } from "@/lib/units";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";

const t = strings.movements;

/**
 * Cele patru rubrici pe care Anexa 2 (transportul deșeurilor periculoase, HG 1061/2008) le cere în
 * plus față de blocul de transport comun: pragul de 1 t/an, numărul aprobării, drumul aprobării
 * când ești peste prag și ambalajul. Starea rămâne în formularul de mișcare.
 */
export function Anexa2Fields({
  anexa2BelowOneTon,
  setAnexa2BelowOneTon,
  anexa2Threshold,
  anexa2Effective,
  anexa2ApprovalNumber,
  setAnexa2ApprovalNumber,
  anexa2Packaging,
  setAnexa2Packaging,
}: {
  anexa2BelowOneTon: "" | "true" | "false";
  setAnexa2BelowOneTon: (value: "" | "true" | "false") => void;
  /** Propunerea din evidența anului; lipsește la o mișcare încă nesalvată. */
  anexa2Threshold: Anexa2Threshold | undefined;
  /** Ce va tipări formularul: răspunsul omului, altfel propunerea, `null` când nu știm niciuna. */
  anexa2Effective: boolean | null;
  anexa2ApprovalNumber: string;
  setAnexa2ApprovalNumber: (value: string) => void;
  anexa2Packaging: string;
  setAnexa2Packaging: (value: string) => void;
}) {
  return (
    <div className="space-y-3 border-t border-line pt-3">
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <div>
          <Label htmlFor="mv-anexa2-threshold">{t.anexa2Threshold}</Label>
          <Select
            id="mv-anexa2-threshold"
            value={anexa2BelowOneTon}
            onChange={(ev) =>
              setAnexa2BelowOneTon(ev.target.value as "" | "true" | "false")
            }
          >
            {/* Implicitul e „cum reiese din evidență", nu o bifă pusă de noi: cumulul se
                mișcă singur când mai intră mișcări pe cod, iar o bifă înghețată azi ar
                rămâne sub prag și după ce anul îl trece. */}
            <option value="">
              {anexa2Threshold
                ? t.anexa2ThresholdFollows.replace(
                    "{proposal}",
                    anexa2Threshold.belowOneTon ? t.anexa2Below : t.anexa2Above
                  )
                : t.anexa2ThresholdFollowsUnknown}
            </option>
            <option value="true">{t.anexa2Below}</option>
            <option value="false">{t.anexa2Above}</option>
          </Select>
          <p className="mt-1 text-xs text-content-muted">
            {anexa2Threshold
              ? t.anexa2ThresholdProposed
                  .replace("{tons}", formatTonnesValue(anexa2Threshold.generatedTons))
                  .replace("{year}", String(anexa2Threshold.year))
                  .replace("{code}", anexa2Threshold.wasteCode)
              : t.anexa2ThresholdAfterSave}
          </p>
        </div>
        <div>
          <Label htmlFor="mv-anexa2-approval">{t.anexa2ApprovalNumber}</Label>
          <Input
            id="mv-anexa2-approval" maxLength={60}
            value={anexa2ApprovalNumber}
            onChange={(ev) => setAnexa2ApprovalNumber(ev.target.value)}
          />
          <p className="mt-1 text-xs text-content-muted">{t.anexa2ApprovalNumberHint}</p>
        </div>
      </div>
      {/* Singurul caz în care cuvântul nedefinit din act schimbă răspunsul: pe cod ești
          sub prag, pe grupă ești peste. Se avertizează, nu se decide. */}
      {anexa2Threshold?.groupWarning && (
        <p className="rounded-md border border-line bg-surface-sunken px-3 py-2 text-xs text-content-strong">
          {t.anexa2GroupWarning
            .replace("{group}", anexa2Threshold.groupCode)
            .replace("{tons}", formatTonnesValue(anexa2Threshold.groupTons))}
        </p>
      )}
      {/* Peste prag hârtia noastră nu e de ajuns, și e mai bine spus aici decât aflat la
          control: mai trebuie aprobarea din anexa 1, iar ea trece prin patru mâini.
          Pașii se numerotează cu `who` scos în față — cine face fiecare pas e chiar
          informația care lipsea, nu lista în sine. */}
      {anexa2Effective === false && (
        <div
          data-testid="anexa2-approval-road"
          className="space-y-2 rounded-md border border-line bg-surface-sunken px-3 py-2 text-xs text-content-strong"
        >
          <p>{t.anexa2ApprovalMissing}</p>
          <p className="font-medium">{t.anexa2ApprovalRoadTitle}</p>
          <ol className="list-decimal space-y-1 pl-5">
            {t.anexa2ApprovalRoad.map((step, i) => (
              <li key={i}>
                <span className="font-medium">{step.who}</span> {step.what}{" "}
                <span className="text-content-muted">({step.basis})</span>
              </li>
            ))}
          </ol>
          <p>{t.anexa2ApprovalRoadMine}</p>
          <p className="text-content-muted">{t.anexa2ApprovalValidity}</p>
        </div>
      )}
      <div>
        <Label htmlFor="mv-anexa2-packaging">{t.anexa2Packaging}</Label>
        <Input
          id="mv-anexa2-packaging" maxLength={255}
          value={anexa2Packaging}
          onChange={(ev) => setAnexa2Packaging(ev.target.value)}
          placeholder={t.anexa2PackagingPlaceholder}
        />
      </div>
      <p className="text-xs text-content-muted">{t.anexa2EmptyColumns}</p>
    </div>
  );
}
