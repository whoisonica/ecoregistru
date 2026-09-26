import { useMemo, useState } from "react";
import { Boxes, SlidersHorizontal } from "lucide-react";
import { Button } from "@/components/ui/button";
import { StockSettingsDialog } from "@/components/depot/StockSettingsDialog";
import { useWorkPoints } from "@/hooks/useWorkPoints";
import { useStock } from "@/hooks/useStock";
import { strings } from "@/lib/strings";
import { todayIso } from "@/lib/utils";
import { Badge } from "@/components/ui/badge";
import { BinSwatch } from "@/components/ui/bin-swatch";
import { DateInput } from "@/components/ui/date-input";
import { Label } from "@/components/ui/label";
import { PillGroup } from "@/components/ui/pill-group";
import { Select } from "@/components/ui/select";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { TablePagination, TableToolbar } from "@/components/ui/table-toolbar";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { Tooltip } from "@/components/ui/tooltip";
import { useTableView } from "@/hooks/useTableView";

const t = strings.weighing;
const kgFormat = new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 3 });
const kg = (value: number) => kgFormat.format(value);

/**
 * F3 (D3.1, D3.2) — stocul, pe depozit sau pe firmă, la zi sau la o dată din trecut. Sold, în tranzit, angajat și
 * disponibil; un sold negativ se vede („Negativ”) și are filtrul lui — raportul „de corectat”, fără să blocheze nimic.
 */
export function StockTab({ canManage }: { canManage: boolean }) {
  const workPoints = useWorkPoints();
  const depots = useMemo(() => (workPoints.data ?? []).filter((w) => w.active), [workPoints.data]);
  const [depot, setDepot] = useState<string>("");
  const [date, setDate] = useState(todayIso());
  const [onlyNegative, setOnlyNegative] = useState(false);
  const { data, isLoading, isError } = useStock(depot || null, date);
  const all = !depot;
  const [settingsOpen, setSettingsOpen] = useState(false);
  const depotName = depots.find((w) => w.id === depot)?.name ?? "";

  const rows = useMemo(
    () => (data?.rows ?? []).filter((r) => !onlyNegative || r.negative),
    [data, onlyNegative]
  );
  const view = useTableView(rows, {
    searchText: (r) => [r.wasteCode, r.wasteName, r.articleName].filter(Boolean).join(" "),
  });

  return (
    <div>
      <div className="mb-3 flex flex-wrap items-end gap-3">
        <div>
          <Label htmlFor="st-depot">{t.stockDepot}</Label>
          <Select id="st-depot" value={depot} onChange={(e) => setDepot(e.target.value)}>
            <option value="">{t.stockAllDepots}</option>
            {depots.map((w) => (
              <option key={w.id} value={w.id}>
                {w.name}
              </option>
            ))}
          </Select>
        </div>
        <div>
          <Label htmlFor="st-date">{t.stockDate}</Label>
          <DateInput id="st-date" value={date} onChange={(e) => setDate(e.target.value || todayIso())} />
        </div>
        <div>
          <Label id="st-filter-label">{t.stockShow}</Label>
          <PillGroup
            name="st-filter"
            aria-labelledby="st-filter-label"
            options={[
              { value: "ALL", label: t.stockAll },
              { value: "NEGATIVE", label: t.stockOnlyNegative },
            ]}
            selected={[onlyNegative ? "NEGATIVE" : "ALL"]}
            onToggle={(value) => setOnlyNegative(value === "NEGATIVE")}
          />
        </div>
        <Tooltip content={t.stockHint}>
          <span className="mb-2 cursor-help font-mono text-content-subtle">?</span>
        </Tooltip>
        {canManage && !all && (
          <Button variant="outline" className="ml-auto" onClick={() => setSettingsOpen(true)}>
            <SlidersHorizontal className="mr-2 h-4 w-4" />
            {t.stockSettings}
          </Button>
        )}
      </div>

      {all && <p className="mb-3 text-xs text-content-muted">{t.stockPickDepot}</p>}

      {!all && data && data.limits.length > 0 && (
        <section aria-label={t.stockLimitsTitle} className="mb-3 border border-line bg-surface-sunken px-4 py-3">
          <h2 className="mb-2 text-xs font-medium uppercase tracking-wide text-content-muted">{t.stockLimitsTitle}</h2>
          <ul className="space-y-1 text-sm">
            {data.limits.map((l) => (
              <li key={l.id} className="flex flex-wrap items-baseline gap-x-3">
                <span>
                  {t.stockLimitKind[l.kind]} · {l.wasteCode ?? t.stockLimitAllCodes}
                </span>
                <span className="font-mono tabular-nums">
                  {l.usedKg != null && `${kg(l.unit === "T" ? l.usedKg / 1000 : l.usedKg)} / `}
                  {l.quantity} {l.unit === "M3" ? "m³" : l.unit.toLowerCase()} {t.stockLimitPeriod[l.period]}
                </span>
                {l.comparable ? (
                  <Badge variant={l.exceeded ? "danger" : "success"}>
                    {l.exceeded ? t.stockLimitExceeded : t.stockLimitOk}
                  </Badge>
                ) : (
                  <span className="text-xs text-content-muted">{t.stockLimitNotCompared}</span>
                )}
              </li>
            ))}
          </ul>
        </section>
      )}

      {data && data.negativeRows > 0 && (
        <p role="status" className="mb-3 text-sm text-state-warn-text">
          {t.stockNegativeCount.replace("{n}", String(data.negativeRows))}
        </p>
      )}
      {isError && <p className="text-sm text-state-bad-text">{t.stockLoadError}</p>}

      {!isError && (
        <>
          <TableToolbar view={view} placeholder={t.searchPlaceholder} />
          <Table stickyHeader>
            <THead sticky>
              <TR>
                <TH>{t.stockCode}</TH>
                <TH>{t.stockArticle}</TH>
                <TH className="text-right">{t.stockKg}</TH>
                {!all && <TH className="text-right">{t.stockInTransit}</TH>}
                <TH className="text-right">{t.stockCommitted}</TH>
                <TH className="text-right">{t.stockAvailable}</TH>
                {!all && (
                  <TH className="text-right">
                    {t.stockAge}
                    <Tooltip content={t.stockAgeHint}>
                      <span className="ml-1 cursor-help text-content-subtle">?</span>
                    </Tooltip>
                  </TH>
                )}
              </TR>
            </THead>
            <TBody>
              {(isLoading || view.visible.length === 0) && (
                <TableFallbackRow
                  columns={all ? 5 : 7}
                  loading={isLoading}
                  icon={Boxes}
                  title={view.emptiedBySearch ? strings.common.noResults : t.stockEmpty}
                  description={view.emptiedBySearch ? strings.common.noResultsHint : t.stockEmptyHint}
                />
              )}
              {view.visible.map((r) => (
                <TR key={`${r.articleId}|${r.wasteCodeId}`}>
                  <TD className="whitespace-nowrap">
                    <span className="inline-flex items-center font-mono text-xs">
                      <BinSwatch code={r.wasteCode} hazardous={r.hazardous} />
                      {r.wasteCode}
                    </span>
                  </TD>
                  <TD>
                    {r.articleName ?? <span className="text-content-muted">{r.wasteName}</span>}
                  </TD>
                  <TD className="text-right font-mono tabular-nums">
                    {r.negative && (
                      <Badge variant="danger" className="mr-2">
                        {t.stockNegative}
                      </Badge>
                    )}
                    {r.belowMin && (
                      <Badge variant="warning" className="mr-2">
                        {t.stockBelowMin}
                      </Badge>
                    )}
                    {r.aboveMax && (
                      <Badge variant="warning" className="mr-2">
                        {t.stockAboveMax}
                      </Badge>
                    )}
                    {kg(r.stockKg)}
                  </TD>
                  {!all && <TD className="text-right font-mono tabular-nums">{r.inTransitKg ? kg(r.inTransitKg) : "—"}</TD>}
                  <TD className="text-right font-mono tabular-nums">{r.committedKg ? kg(r.committedKg) : "—"}</TD>
                  <TD className="text-right font-mono tabular-nums text-content-strong">{kg(r.availableKg)}</TD>
                  {!all && (
                    <TD className="whitespace-nowrap text-right font-mono tabular-nums">
                      {r.ageFlag && (
                        <Badge variant={r.ageFlag === "ONE_YEAR" ? "warning" : "danger"} className="mr-2">
                          {t.stockAgeFlag[r.ageFlag]}
                        </Badge>
                      )}
                      {r.oldestDays == null ? "—" : t.stockAgeDays.replace("{n}", String(r.oldestDays))}
                    </TD>
                  )}
                </TR>
              ))}
            </TBody>
          </Table>
          <TablePagination view={view} />
        </>
      )}

      {settingsOpen && (
        <StockSettingsDialog
          workPointId={depot}
          depotName={depotName}
          limits={data?.limits ?? []}
          onClose={() => setSettingsOpen(false)}
        />
      )}
    </div>
  );
}
