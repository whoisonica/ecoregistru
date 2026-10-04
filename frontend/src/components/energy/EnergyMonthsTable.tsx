import { useState } from "react";
import { Info } from "lucide-react";
import { useSaveEnergyCell } from "@/hooks/useEnergy";
import { apiErrorMessage } from "@/lib/api";
import { ENERGY_CARRIERS } from "@/lib/energy";
import { strings } from "@/lib/strings";
import type { EnergyCarrier, EnergySheet } from "@/lib/types";
import { formatDecimal } from "@/lib/units";
import { cn } from "@/lib/utils";
import { Input } from "@/components/ui/input";
import { Table, TBody, TD, TH, THead, TR } from "@/components/ui/table";
import { Tooltip } from "@/components/ui/tooltip";

const t = strings.energy;
const MONTHS = Array.from({ length: 12 }, (_, i) => i + 1);

/** Explicația de lângă rubrică: de unde vine cifra. */
const CARRIER_TIP: Partial<Record<EnergyCarrier, string>> = {
  ELECTRICITY: t.tooltipElectricity,
  PETROL: t.tooltipFuel,
  DIESEL: t.tooltipFuel,
};

/** Cifră de transcris: virgulă, trei zecimale; „?” cât lipsesc luni. */
export const figure = (n: number | null | undefined) => (n == null ? "?" : formatDecimal(n, 3));

/** Valoarea din câmp: virgula românească, fără separator de mii — omul o retastează. */
const raw = (n: number | null | undefined) => (n == null ? "" : String(n).replace(".", ","));

/** `""` → null (celula se șterge); altfel numărul, sau `NaN` pentru ce nu e un număr ≥ 0. */
function parse(text: string): number | null {
  const s = text.trim().replace(/\s/g, "").replace(",", ".");
  if (s === "") return null;
  const n = Number(s);
  return Number.isFinite(n) && n >= 0 ? n : NaN;
}

type Field = "q" | "t";
const keyOf = (carrier: EnergyCarrier, month: number, field: Field) => `${carrier}-${month}-${field}`;

/**
 * Tabelul anului: un rând pe rubrică bifată, douăsprezece luni, „Total an” și „tep”. Celula se
 * salvează la ieșirea din ea; golită, se șterge (lipsa rândului = necompletat, nu 0). La cărbune și
 * la alți combustibili, sub cantitate stă tep-ul, scris de om.
 */
export function EnergyMonthsTable({ sheet, canWrite }: { sheet: EnergySheet; canWrite: boolean }) {
  const save = useSaveEnergyCell();
  const [draft, setDraft] = useState<Record<string, string>>({});
  const [errors, setErrors] = useState<Record<string, string>>({});

  const now = new Date();
  const future = (month: number) =>
    sheet.year > now.getFullYear() || (sheet.year === now.getFullYear() && month > now.getMonth() + 1);
  const cellOf = (carrier: EnergyCarrier, month: number) =>
    sheet.cells.find((c) => c.carrier === carrier && c.month === month);
  const valueOf = (carrier: EnergyCarrier, month: number, field: Field) => {
    const pending = draft[keyOf(carrier, month, field)];
    if (pending !== undefined) return pending;
    const cell = cellOf(carrier, month);
    return raw(field === "q" ? cell?.quantity : cell?.tep);
  };
  const setError = (key: string, message: string | null) =>
    setErrors((prev) => {
      const next = { ...prev };
      if (message) next[key] = message;
      else delete next[key];
      return next;
    });

  function commit(carrier: EnergyCarrier, month: number, field: Field, byHand: boolean) {
    const key = keyOf(carrier, month, field);
    const sent = draft[key];
    if (sent === undefined) return;
    const cell = cellOf(carrier, month);
    const quantity = parse(valueOf(carrier, month, "q"));
    const tep = byHand ? parse(valueOf(carrier, month, "t")) : null;
    if (Number.isNaN(quantity) || Number.isNaN(tep)) {
      // Pe câmpul greșit, nu pe cel din care s-a ieșit.
      setError(keyOf(carrier, month, Number.isNaN(quantity) ? "q" : "t"), t.cellInvalid);
      return;
    }
    // Neschimbat → nicio cerere.
    if (quantity === (cell?.quantity ?? null) && (!byHand || tep === (cell?.tep ?? null))) {
      setDraft(({ [key]: _, ...rest }) => rest);
      setError(key, null);
      return;
    }
    save.mutate(
      // tep-ul pleacă numai la cărbune și la alți combustibili: la celelalte îl socotește serverul.
      byHand
        ? { year: sheet.year, carrier, month, quantity, tep: quantity == null ? null : tep }
        : { year: sheet.year, carrier, month, quantity },
      {
        onSuccess: () => {
          // Numai dacă între timp nu s-a tastat altceva în aceeași celulă.
          setDraft((prev) => {
            if (prev[key] !== sent) return prev;
            const { [key]: _, ...rest } = prev;
            return rest;
          });
          setError(key, null);
        },
        onError: (err) => setError(key, apiErrorMessage(err, t.cellError)),
      }
    );
  }

  function cellInput(carrier: EnergyCarrier, month: number, field: Field, byHand: boolean) {
    // Cine doar citește vede cifrele, nu câmpuri stinse.
    if (!canWrite) {
      return <span className="block h-8 w-[3.5rem] px-1.5 text-right leading-8">{valueOf(carrier, month, field)}</span>;
    }
    const key = keyOf(carrier, month, field);
    const name = t.carrier[carrier];
    const monthName = strings.months[month - 1].toLowerCase();
    const error = errors[key];
    // Fără cantitate, tep-ul n-are ce însoți: celula se șterge cu totul.
    const tepWithoutQuantity = field === "t" && valueOf(carrier, month, "q").trim() === "";
    return (
      <>
        <Input
          inputMode="decimal"
          aria-label={(field === "q" ? t.cellLabel : t.cellTepLabel)
            .replace("{carrier}", name)
            .replace("{month}", monthName)}
          aria-invalid={error ? true : undefined}
          placeholder={field === "t" ? t.tepPlaceholder : undefined}
          disabled={future(month) || tepWithoutQuantity}
          value={valueOf(carrier, month, field)}
          onChange={(e) => setDraft((prev) => ({ ...prev, [key]: e.target.value }))}
          onBlur={() => commit(carrier, month, field, byHand)}
          className={cn("h-8 w-[3.5rem] px-1.5 text-right", error && "border-state-bad")}
        />
        {error && (
          <p role="alert" className="mt-0.5 max-w-[3.5rem] text-[0.6875rem] leading-tight text-state-bad-text">
            {error}
          </p>
        )}
      </>
    );
  }

  const totalKnown = sheet.totals.every((tot) => tot.tep != null);

  return (
    <section className="mt-4">
      <Table>
        <THead>
          <TR>
            <TH>{t.colCarrier}</TH>
            {MONTHS.map((m) => (
              <TH key={m} className={cn("px-0.5 text-right", future(m) && "text-content-subtle opacity-60")}>
                {strings.months[m - 1].slice(0, 3)}
              </TH>
            ))}
            <TH className="whitespace-nowrap text-right">{t.colTotal}</TH>
            <TH className="text-right">{t.colTep}</TH>
          </TR>
        </THead>
        <TBody>
          {sheet.carriers.map((carrier) => {
            const meta = ENERGY_CARRIERS.find((c) => c.id === carrier);
            const byHand = meta?.coefficient == null;
            const total = sheet.totals.find((tot) => tot.carrier === carrier);
            const tip = CARRIER_TIP[carrier];
            return (
              <TR key={carrier}>
                {/* Unitatea pe același rând cu numele: pe un rând separat, unsprezece rubrici împingeau
                    pagina peste 900px (măsurat, 04.10.2026). */}
                <TD className="min-w-[9rem] py-1 font-medium leading-tight">
                  {t.carrier[carrier]}
                  <span className="ml-1 whitespace-nowrap font-mono text-[0.6875rem] font-normal text-content-subtle">
                    {meta?.unit}
                    {byHand && ` · ${t.colTep}`}
                  </span>
                  {tip && (
                    <span className="ml-1 align-middle">
                      <Tooltip content={tip}>
                        <Info className="h-3.5 w-3.5 text-content-subtle" aria-label={tip} />
                      </Tooltip>
                    </span>
                  )}
                </TD>
                {MONTHS.map((m) => (
                  <TD key={m} className={cn("px-0.5 py-1 align-top", future(m) && "opacity-60")}>
                    {cellInput(carrier, m, "q", byHand)}
                    {byHand && <div className="mt-0.5">{cellInput(carrier, m, "t", byHand)}</div>}
                  </TD>
                ))}
                <TD className="whitespace-nowrap py-1 text-right font-mono">{figure(total?.quantity)}</TD>
                <TD className="whitespace-nowrap py-1 text-right font-mono">{figure(total?.tep)}</TD>
              </TR>
            );
          })}
        </TBody>
      </Table>
      <p className="mt-3 text-right font-semibold">
        {t.totalLine.replace("{tep}", totalKnown ? formatDecimal(sheet.totalTep, 3) : "?")}
      </p>
    </section>
  );
}
