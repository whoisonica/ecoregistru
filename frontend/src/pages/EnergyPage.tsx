import { useState } from "react";
import { useCanWrite } from "@/hooks/useBillingAccess";
import { useEnergySheet } from "@/hooks/useEnergy";
import { useUrlNumber, useUrlState } from "@/hooks/useUrlState";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { LoadError } from "@/components/ui/load-error";
import { PageHeader } from "@/components/ui/page-header";
import { PillGroup } from "@/components/ui/pill-group";
import { Select } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { EnergyCarriersCard, EnergyCarriersDialog } from "@/components/energy/EnergyCarriersDialog";
import { EnergyDocuments } from "@/components/energy/EnergyDocuments";
import { EnergyEfEnClimaView } from "@/components/energy/EnergyEfEnClimaView";
import { EnergyMonthsTable } from "@/components/energy/EnergyMonthsTable";

const t = strings.energy;
/** Primul an pe care îl primește serverul (`EnergyService.FIRST_YEAR`). */
const FIRST_YEAR = 2020;

/** „Luni” e vederea implicită, deci nu se scrie în adresă. */
type View = "" | "efenclima";

/**
 * Fișa de energie — `/energie?an=YYYY` (Legea 121/2014, Anexa 1 sub 1000 tep). Intrare proprie în
 * meniu, după Termene (05.10.2026); se ajunge și din rândul de pe Termene și din Ctrl K. Anul
 * implicit e cel trecut: în iunie se declară anul de dinainte.
 */
export function EnergyPage() {
  const thisYear = new Date().getFullYear();
  const [year, setYear] = useUrlNumber("an", thisYear - 1);
  const [view, setView] = useUrlState("vedere");
  const [carriersOpen, setCarriersOpen] = useState(false);
  const canWrite = useCanWrite();
  const sheetQ = useEnergySheet(year);
  const sheet = sheetQ.data;
  const years = Array.from({ length: thisYear - FIRST_YEAR + 1 }, (_, i) => thisYear - i);

  return (
    <div>
      <PageHeader
        title={`${t.title} · ${year}`}
        description={t.description}
        actions={
          <>
            <label htmlFor="energie-an" className="sr-only">
              {t.yearLabel}
            </label>
            <Select id="energie-an" value={String(year)} onChange={(e) => setYear(Number(e.target.value))} className="w-28">
              {years.map((y) => (
                <option key={y} value={y}>
                  {y}
                </option>
              ))}
            </Select>
            {canWrite && sheet && sheet.carriers.length > 0 && (
              <Button variant="outline" onClick={() => setCarriersOpen(true)}>
                {t.changeCarriers}
              </Button>
            )}
          </>
        }
      />

      {sheetQ.isError && (
        <LoadError className="mt-6" message={t.loadError} onRetry={() => void sheetQ.refetch()} />
      )}
      {sheetQ.isLoading && <Skeleton className="mt-6 h-64 w-full" />}

      {sheet && sheet.carriers.length === 0 && <EnergyCarriersCard key={year} year={year} canWrite={canWrite} />}

      {sheet && sheet.carriers.length > 0 && (
        <>
          <div className="relative mt-6 flex flex-wrap items-center justify-between gap-3">
            <EnergyDocuments sheet={sheet} canWrite={canWrite} />
            <span id="energie-vedere" className="sr-only">
              {t.viewLabel}
            </span>
            <PillGroup<View>
              name="vedere-energie"
              aria-labelledby="energie-vedere"
              options={[
                { value: "", label: t.viewMonths },
                { value: "efenclima", label: t.viewEfenclima },
              ]}
              selected={[view === "efenclima" ? "efenclima" : ""]}
              onToggle={(v) => setView(v)}
            />
          </div>

          {sheet.overThreshold && (
            <p role="status" className="mt-4 flex items-start gap-2 text-sm font-medium text-state-bad-text">
              <span className="mt-1.5 h-2 w-2 shrink-0 bg-state-bad" aria-hidden />
              {t.overThresholdWarning}
            </p>
          )}

          {view === "efenclima" ? (
            <EnergyEfEnClimaView key={year} sheet={sheet} canWrite={canWrite} />
          ) : (
            <EnergyMonthsTable key={year} sheet={sheet} canWrite={canWrite} />
          )}

          {canWrite && carriersOpen && (
            <EnergyCarriersDialog year={year} open onClose={() => setCarriersOpen(false)} current={sheet.carriers} />
          )}
        </>
      )}
    </div>
  );
}
