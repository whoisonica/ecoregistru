import { useRef, useState } from "react";
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
type Base = { quantity: number | null; tep: number | null };
const keyOf = (carrier: EnergyCarrier, month: number, field: Field) => `${carrier}-${month}-${field}`;

/**
 * Tabelul anului: un rând pe rubrică bifată, douăsprezece luni, „Total an” și „tep”. Celula se
 * salvează la ieșirea din ea; golită, se șterge (lipsa rândului = necompletat, nu 0). La cărbune și
 * la alți combustibili, sub cantitate stă tep-ul, scris de om.
 */
export function EnergyMonthsTable({ sheet, canWrite }: { sheet: EnergySheet; canWrite: boolean }) {
  const save = useSaveEnergyCell();
  const [draft, setDraftState] = useState<Record<string, string>>({});
  const [errors, setErrors] = useState<Record<string, string>>({});
  /**
   * Ce a răspuns serverul despre o celulă la ultima ei salvare, pe cheia `${carrier}-${month}`.
   *
   * <p>Fișa din cache nu ajunge: fiecare salvare o reîncarcă, iar o reîncărcare pornită de salvarea
   * altei celule poate răspunde **după** salvarea asta, cu celula încă goală. Ciorna ștearsă după
   * succes lăsa atunci câmpul gol, iar salvarea următoare din aceeași celulă trimitea `quantity: null`
   * — adică ștergea cifra (prins în browser, 04.10.2026). Pentru celula proprie, ultimul ei răspuns
   * e adevărul; fișa rămâne sursa pentru celulele neatinse.
   */
  const [known, setKnownState] = useState<Record<string, Base>>({});
  /** Ce citește o salvare pusă la coadă când îi vine rândul: ciorna de atunci, nu cea de la ieșire. */
  const draftRef = useRef(draft);
  const knownRef = useRef(known);
  /**
   * O coadă pe celulă (rubrică × lună). Cantitatea și tep-ul cărbunelui pleacă în același PUT, deci
   * două ieșiri la rând din aceeași celulă ar trimite două cereri care pot ajunge invers și ar pune
   * tep-ul vechi la loc. Celulele diferite nu se așteaptă între ele.
   */
  const queues = useRef(new Map<string, Promise<void>>());

  const setDraft = (update: (prev: Record<string, string>) => Record<string, string>) => {
    draftRef.current = update(draftRef.current);
    setDraftState(draftRef.current);
  };
  const dropDrafts = (keys: string[], unlessChangedFrom?: Record<string, string | undefined>) =>
    setDraft((prev) => {
      const next = { ...prev };
      for (const k of keys) {
        if (unlessChangedFrom && prev[k] !== unlessChangedFrom[k]) continue;
        delete next[k];
      }
      return next;
    });

  const now = new Date();
  const future = (month: number) =>
    sheet.year > now.getFullYear() || (sheet.year === now.getFullYear() && month > now.getMonth() + 1);
  const baseOf = (carrier: EnergyCarrier, month: number, knownNow: Record<string, Base> = known): Base => {
    const mine = knownNow[`${carrier}-${month}`];
    if (mine) return mine;
    const cell = sheet.cells.find((c) => c.carrier === carrier && c.month === month);
    return { quantity: cell?.quantity ?? null, tep: cell?.tep ?? null };
  };
  const valueOf = (
    carrier: EnergyCarrier,
    month: number,
    field: Field,
    from: { draft: Record<string, string>; known: Record<string, Base> } = { draft, known }
  ) => {
    const pending = from.draft[keyOf(carrier, month, field)];
    if (pending !== undefined) return pending;
    const base = baseOf(carrier, month, from.known);
    return raw(field === "q" ? base.quantity : base.tep);
  };
  const setError = (key: string, message: string | null) =>
    setErrors((prev) => {
      const next = { ...prev };
      if (message) next[key] = message;
      else delete next[key];
      return next;
    });

  /** Ce ar pleca acum din celulă: cifrele din ciornă peste cele salvate, sau `NaN` la ce nu e număr. */
  function readCell(carrier: EnergyCarrier, month: number, byHand: boolean) {
    const from = { draft: draftRef.current, known: knownRef.current };
    return {
      quantity: parse(valueOf(carrier, month, "q", from)),
      tep: byHand ? parse(valueOf(carrier, month, "t", from)) : null,
      base: baseOf(carrier, month, from.known),
    };
  }

  /** O salvare, rulată când îi vine rândul în coada celulei. */
  async function send(carrier: EnergyCarrier, month: number, field: Field, byHand: boolean) {
    const qKey = keyOf(carrier, month, "q");
    const tKey = keyOf(carrier, month, "t");
    const key = keyOf(carrier, month, field);
    const { quantity, tep, base } = readCell(carrier, month, byHand);
    // Între timp s-a tastat ceva greșit: eroarea e deja pe câmp, nu se trimite nimic.
    if (Number.isNaN(quantity) || Number.isNaN(tep)) return;
    // Neschimbat față de ce e pe server → nicio cerere.
    if (quantity === base.quantity && (!byHand || tep === base.tep)) {
      dropDrafts([qKey, tKey]);
      setError(qKey, null);
      setError(tKey, null);
      return;
    }
    const sent = { [qKey]: draftRef.current[qKey], [tKey]: draftRef.current[tKey] };
    try {
      // `mutateAsync`, nu `mutate` cu callback-uri: la `mutate`, a doua celulă salvată înainte să
      // răspundă prima înlocuiește callback-urile primeia, iar eroarea ei nu mai ajunge nicăieri.
      const next = await save.mutateAsync(
        // tep-ul pleacă numai la cărbune și la alți combustibili: la celelalte îl socotește serverul.
        byHand
          ? { year: sheet.year, carrier, month, quantity, tep: quantity == null ? null : tep }
          : { year: sheet.year, carrier, month, quantity }
      );
      const saved = next.cells.find((c) => c.carrier === carrier && c.month === month);
      knownRef.current = {
        ...knownRef.current,
        [`${carrier}-${month}`]: { quantity: saved?.quantity ?? null, tep: saved?.tep ?? null },
      };
      setKnownState(knownRef.current);
      // Numai ce s-a trimis și n-a fost retastat între timp.
      dropDrafts([qKey, tKey], sent);
      setError(qKey, null);
      setError(tKey, null);
    } catch (err) {
      setError(key, apiErrorMessage(err, t.cellError));
    }
  }

  function commit(carrier: EnergyCarrier, month: number, field: Field, byHand: boolean) {
    if (draftRef.current[keyOf(carrier, month, field)] === undefined) return;
    const { quantity, tep } = readCell(carrier, month, byHand);
    if (Number.isNaN(quantity) || Number.isNaN(tep)) {
      // Pe câmpul greșit, nu pe cel din care s-a ieșit.
      setError(keyOf(carrier, month, Number.isNaN(quantity) ? "q" : "t"), t.cellInvalid);
      return;
    }
    const cellKey = `${carrier}-${month}`;
    const queued = (queues.current.get(cellKey) ?? Promise.resolve()).then(() =>
      send(carrier, month, field, byHand)
    );
    queues.current.set(cellKey, queued);
    void queued.finally(() => {
      if (queues.current.get(cellKey) === queued) queues.current.delete(cellKey);
    });
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
          onChange={(e) => {
            const value = e.target.value;
            setDraft((prev) => {
              const next = { ...prev, [key]: value };
              // Cantitatea golită șterge celula întreagă: tep-ul tastat lângă ea pleacă și el.
              if (field === "q" && value.trim() === "") delete next[keyOf(carrier, month, "t")];
              return next;
            });
          }}
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
        {/* Peste prag cu luni lipsă: suma rubricilor complete e deja o margine de jos. */}
        {!totalKnown && sheet.overThreshold && (
          <> {t.totalLowerBound.replace("{tep}", formatDecimal(sheet.totalTep, 3))}</>
        )}
      </p>
    </section>
  );
}
