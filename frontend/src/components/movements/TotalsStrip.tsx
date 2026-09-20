import type { MovementScreen } from "@/lib/movementScreens";
import type { MovementTotals } from "@/lib/types";
import { strings } from "@/lib/strings";
import { cn } from "@/lib/utils";

const t = strings.movements;

/**
 * Rotunjit la kilogram întreg, **dinadins** (G06, 20.09.2026): banda e un rezumat, nu o cifră de
 * transcris pe un formular. De aceea nu foloseşte `formatKg` şi nu se „aliniază" la el.
 *
 * <p>Lipsa virgulei e chiar semnul că e un rezumat: orice cifră de transcris trece prin
 * `formatQuantity` şi are mereu virgulă cu trei zecimale, deci „12.640" de aici nu se poate
 * confunda cu „35,125" din listă.
 */
const kgFormat = new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 0 });

/**
 * Cifrele lunii (sau ale anului) din filtru, pe patru celule: ce contează pe ecranul ăsta.
 *
 * <p>Generare: generat · valorificat · eliminat · de cântărit. Intrări: primit · de la firme · de la
 * persoane fizice · de cântărit. Ieșiri: plecat · valorificat · eliminat · fără cod R/D. Socotite de
 * server peste toate rândurile filtrului (`useMovementTotals`), nu peste pagina adusă. Fără
 * cântar = „de cântărit", nu zero, deci celula aia numără rânduri, nu kilograme.
 */
export function TotalsStrip({
  screen,
  totals,
  loading,
  failed,
}: {
  screen: MovementScreen;
  totals: MovementTotals | undefined;
  loading: boolean;
  failed: boolean;
}) {
  const kg = (n: number | undefined) => (n == null ? "—" : kgFormat.format(n));
  const rows = (n: number | undefined) => (n == null ? "—" : String(n));
  type Cell = { label: string; value: string; unit: string; tone?: "ok" | "warn" | "bad" };
  const cells: Cell[] =
    screen === "IN"
      ? [
          { label: t.totReceived, value: kg(totals?.quantityKg), unit: strings.panel.kg },
          {
            label: t.totFromCompanies,
            value: totals ? kg(totals.quantityKg - totals.fromNaturalPersonsKg) : "—",
            unit: strings.panel.kg,
          },
          { label: t.totFromPersons, value: kg(totals?.fromNaturalPersonsKg), unit: strings.panel.kg },
          {
            label: t.totAwaiting,
            value: rows(totals?.awaitingWeighing),
            unit: totals?.awaitingWeighing === 1 ? t.totRow : t.totRows,
            tone: totals && totals.awaitingWeighing > 0 ? "warn" : undefined,
          },
        ]
      : screen === "OUT"
        ? [
            { label: t.totLeft, value: kg(totals?.quantityKg), unit: strings.panel.kg },
            { label: t.totRecovered, value: kg(totals?.recoveredKg), unit: strings.panel.kg, tone: "ok" },
            { label: t.totDisposed, value: kg(totals?.disposedKg), unit: strings.panel.kg },
            {
              label: t.totMissingCode,
              value: rows(totals?.missingOperationCode),
              unit: totals?.missingOperationCode === 1 ? t.totRow : t.totRows,
              tone: totals && totals.missingOperationCode > 0 ? "bad" : undefined,
            },
          ]
        : [
            { label: t.totGenerated, value: kg(totals?.quantityKg), unit: strings.panel.kg },
            { label: t.totRecovered, value: kg(totals?.recoveredKg), unit: strings.panel.kg, tone: "ok" },
            { label: t.totDisposed, value: kg(totals?.disposedKg), unit: strings.panel.kg },
            {
              label: t.totAwaiting,
              value: rows(totals?.awaitingWeighing),
              unit: totals?.awaitingWeighing === 1 ? t.totRow : t.totRows,
              tone: totals && totals.awaitingWeighing > 0 ? "warn" : undefined,
            },
          ];
  return (
    <div
      data-testid="totals"
      className="mt-5 grid grid-cols-2 overflow-hidden rounded-md border border-line-strong sm:grid-cols-4"
    >
      {cells.map((c, i) => (
        <div
          key={c.label}
          className={cn(
            "px-4 py-2.5",
            i > 0 && "sm:border-l sm:border-line-strong",
            i % 2 === 1 && "border-l border-line-strong sm:border-l",
            i >= 2 && "border-t border-line-strong sm:border-t-0"
          )}
        >
          <div className="eyebrow truncate text-[0.625rem]">{c.label}</div>
          <div
            className={cn(
              "font-mono text-[1.375rem] font-medium leading-tight",
              failed
                ? "text-content-subtle"
                : c.tone === "ok"
                  ? "text-state-ok-text"
                  : c.tone === "warn"
                    ? "text-state-warn-text"
                    : c.tone === "bad"
                      ? "text-state-bad-text"
                      : "text-content"
            )}
          >
            {failed ? strings.panel.unknown : loading && !totals ? "…" : c.value}
            <span className="ml-1 text-xs font-normal text-content-muted">{c.unit}</span>
          </div>
        </div>
      ))}
    </div>
  );
}
