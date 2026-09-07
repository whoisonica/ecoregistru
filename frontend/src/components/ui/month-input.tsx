import { useMemo } from "react";
import { strings } from "@/lib/strings";
import { Select } from "@/components/ui/select";
import { cn } from "@/lib/utils";

/**
 * Alegerea unei luni, din două select-uri native.
 *
 * <p>Era `<input type="month">`, iar pe hârtie era alegerea corectă: o rubrică nativă, cu
 * selector, cu validare, fără cod de scris. Numai că **Safari și Firefox nu o implementează** —
 * degradează la câmp text liber. Adică pe un Mac, filtrul principal al celui mai folosit ecran nu
 * avea selector, nu avea validare și cerea tastat `2026-06` exact, fără să spună asta nicăieri.
 * Nu e o lipsă de cizelare, e o funcție ruptă pe o familie întreagă de browsere.
 *
 * <p>Două `<select>` native rezolvă și problema de accesibilitate pe care ar fi adus-o un
 * calendar scris de mână: tastatura, cititorul de ecran și selectorul de pe telefon vin gata
 * făcute. Luna stă înaintea anului fiindcă așa se citește în românește — „Iunie 2026".
 *
 * <p>Valoarea e `yyyy-MM`, exact forma pe care o purtau adresa (`?luna=`) și `<input type="month">`
 * de dinainte, deci linkurile vechi din rapoarte continuă să deschidă aceeași lună. În plus,
 * `yyyy` singur înseamnă **anul întreg** — treapta de care are nevoie un ecran care pornește pe
 * luna curentă, ca să se poată căuta și înapoi fără a nimeri luna din prima.
 */
export interface MonthInputProps {
  /** Ajunge pe select-ul de lună, ca `<Label htmlFor>` să aibă ce eticheta. */
  id?: string;
  /** `yyyy-MM`. */
  value: string;
  onChange: (next: string) => void;
  /** Câți ani în urmă se oferă, pe lângă cel curent. Evidența se ține 5 ani (art. 49). */
  yearsBack?: number;
  /** Oferă „Tot anul" în select-ul de lună, adică `yyyy` fără lună. */
  allowWholeYear?: boolean;
  className?: string;
  disabled?: boolean;
}

/** Luna curentă, în forma pe care o poartă filtrele: `yyyy-MM`. */
export function currentMonth(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
}

/** `yyyy-MM`, cu luna între 01 și 12. Ce nu trece pe aici e adresă editată cu mâna. */
export const MONTH_PATTERN = /^\d{4}-(0[1-9]|1[0-2])$/;

/** `yyyy` — anul întreg. */
export const YEAR_PATTERN = /^\d{4}$/;

/** Ce se acceptă din bara de adrese: o lună sau un an. */
export function isMonthValue(value: string): boolean {
  return MONTH_PATTERN.test(value) || YEAR_PATTERN.test(value);
}

export function MonthInput({
  id,
  value,
  onChange,
  yearsBack = 5,
  allowWholeYear = false,
  className,
  disabled,
}: MonthInputProps) {
  // `month` e 0 pentru „tot anul": nicio lună nu poartă cifra asta, deci nu se poate confunda cu
  // un răspuns.
  const [year, month] = useMemo(() => {
    const safe = isMonthValue(value) ? value : currentMonth();
    const [y, m] = safe.split("-");
    return [Number(y), m ? Number(m) : 0];
  }, [value]);

  const years = useMemo(() => {
    const now = new Date().getFullYear();
    const list = Array.from({ length: yearsBack + 1 }, (_, i) => now - i);
    // Un link vechi poate purta o lună dinaintea ferestrei — sau, la un ceas trecut de Anul Nou pe
    // o mașină cu ora greșită, de după ea. Anul cerut intră în listă, altfel select-ul ar arăta
    // altceva decât filtrează pagina.
    if (!list.includes(year)) list.push(year);
    return list.sort((a, b) => b - a);
  }, [year, yearsBack]);

  const emit = (y: number, m: number) =>
    onChange(m === 0 ? String(y) : `${y}-${String(m).padStart(2, "0")}`);

  return (
    <div className={cn("flex gap-2", className)}>
      <Select
        id={id}
        aria-label={strings.common.month}
        value={String(month)}
        disabled={disabled}
        onChange={(ev) => emit(year, Number(ev.target.value))}
        className="w-36"
      >
        {allowWholeYear && <option value={0}>{strings.common.wholeYear}</option>}
        {strings.months.map((name, i) => (
          <option key={name} value={i + 1}>
            {name}
          </option>
        ))}
      </Select>
      <Select
        aria-label={strings.common.year}
        value={String(year)}
        disabled={disabled}
        onChange={(ev) => emit(Number(ev.target.value), month)}
        className="w-24"
      >
        {years.map((y) => (
          <option key={y} value={y}>
            {y}
          </option>
        ))}
      </Select>
    </div>
  );
}
