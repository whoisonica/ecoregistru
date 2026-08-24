import { Fragment, useEffect, useMemo, useState } from "react";
import { FileText } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import {
  downloadPackagingDeclaration,
  usePackagingHandovers,
  usePackagingMarket,
  useSavePackagingMarket,
} from "@/hooks/usePackaging";
import type { PackagingMarketRow, PackagingMaterial } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { useToast } from "@/components/ui/toast";

const t = strings.packaging;
const materialLabels = strings.enums.packagingMaterial;

/** Year options: current year down to five years back, same as the other documents. */
function yearOptions(): number[] {
  const now = new Date().getFullYear();
  return Array.from({ length: 6 }, (_, i) => now - i);
}

const kgFormat = new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 3 });
function kg(value: number | null) {
  return value == null ? "—" : kgFormat.format(value);
}

/** The six figures the client answers; "Total (col. 3+5)" is a sum and is never typed. */
const COLUMNS = [
  "salesPackaging",
  "primaryTotal",
  "primaryReusable",
  "secondaryTotal",
  "secondaryReusable",
  "hazardousContent",
] as const;
type Column = (typeof COLUMNS)[number];

/**
 * Anexa 1 Ambalaje (Ordinul 794/2012) — the packaging declaration.
 *
 * <p>The screen shows the two tables the way the form has them, and it is honest about where each
 * comes from: <b>tabelul 1</b> is typed here, because nothing in the application knows how much
 * packaging left with the goods sold; <b>tabelul 2</b> is read-only, computed from the handovers
 * already recorded on codes 15 01 xx.
 */
export function PackagingPage() {
  const { user } = useAuth();
  const canWrite =
    user?.role === "PLATFORM_ADMIN" || user?.role === "ADMIN" || user?.role === "OPERATOR";

  const [year, setYear] = useState(() => new Date().getFullYear());
  const { data: marketRows, isLoading } = usePackagingMarket(year);
  const { data: handovers } = usePackagingHandovers(year);
  const saveMut = useSavePackagingMarket();
  const { notify } = useToast();
  const [downloading, setDownloading] = useState(false);

  // What is typed but not yet saved, per material. Saving is per row, on blur: the grid has
  // sixty-six cells and a single "save everything" button would make one typo invisible.
  const [draft, setDraft] = useState<Record<string, Record<string, string>>>({});
  useEffect(() => setDraft({}), [year]);

  const total = useMemo(() => {
    const rows = marketRows ?? [];
    const sum = (column: Column) =>
      rows.reduce<number | null>((acc, r) => {
        const value = r[column];
        return value == null ? acc : (acc ?? 0) + value;
      }, null);
    return Object.fromEntries(COLUMNS.map((c) => [c, sum(c)])) as Record<Column, number | null>;
  }, [marketRows]);

  function cellValue(row: PackagingMarketRow, column: Column) {
    const pending = draft[row.material]?.[column];
    if (pending !== undefined) return pending;
    return row[column] == null ? "" : String(row[column]);
  }

  function edit(material: PackagingMaterial, column: Column, value: string) {
    setDraft((prev) => ({ ...prev, [material]: { ...prev[material], [column]: value } }));
  }

  function saveRow(row: PackagingMarketRow) {
    const pending = draft[row.material];
    if (!pending) return;
    // An empty cell means "not answered" and is sent as null, not as zero: on this form the two
    // are different statements, and only the client may make either.
    const numeric = (column: Column) => {
      const raw = pending[column] ?? (row[column] == null ? "" : String(row[column]));
      if (raw.trim() === "") return null;
      const parsed = Number(raw.replace(",", "."));
      return Number.isFinite(parsed) ? parsed : null;
    };
    saveMut.mutate(
      {
        material: row.material,
        year,
        salesPackaging: numeric("salesPackaging"),
        primaryTotal: numeric("primaryTotal"),
        primaryReusable: numeric("primaryReusable"),
        secondaryTotal: numeric("secondaryTotal"),
        secondaryReusable: numeric("secondaryReusable"),
        hazardousContent: numeric("hazardousContent"),
      },
      {
        onSuccess: () => {
          setDraft((prev) => {
            const next = { ...prev };
            delete next[row.material];
            return next;
          });
        },
        onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
      }
    );
  }

  async function handleDownload() {
    setDownloading(true);
    try {
      await downloadPackagingDeclaration(year);
    } catch (err) {
      notify(apiErrorMessage(err, t.downloadError), "error");
    } finally {
      setDownloading(false);
    }
  }

  return (
    <div>
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold">{t.title}</h1>
          <p className="mt-1 max-w-3xl text-sm text-gray-500">{t.subtitle}</p>
        </div>
        <Button onClick={handleDownload} disabled={downloading} title={t.downloadHint}>
          <FileText className="mr-2 h-4 w-4" />
          {downloading ? strings.common.loading : t.download}
        </Button>
      </div>

      <div className="mt-6 w-40">
        <Label htmlFor="pk-year">{t.year}</Label>
        <Select
          id="pk-year"
          value={String(year)}
          onChange={(ev) => setYear(Number(ev.target.value))}
        >
          {yearOptions().map((y) => (
            <option key={y} value={y}>
              {y}
            </option>
          ))}
        </Select>
      </div>

      {/* ---- Tabel 1: answered by the client ---- */}
      <section className="mt-8">
        <h2 className="text-lg font-semibold">{t.table1Title}</h2>
        <p className="mt-1 max-w-3xl text-sm text-gray-500">{t.table1Hint}</p>
        <div className="mt-3 overflow-x-auto">
          <Table>
            <THead>
              <TR>
                <TH>{t.material}</TH>
                <TH className="text-right">{t.colSales}</TH>
                <TH className="text-right">{t.colTotal}</TH>
                <TH className="text-right">{t.colPrimary}</TH>
                <TH className="text-right">{t.colPrimaryReusable}</TH>
                <TH className="text-right">{t.colSecondary}</TH>
                <TH className="text-right">{t.colSecondaryReusable}</TH>
                <TH className="text-right">{t.colHazardous}</TH>
              </TR>
            </THead>
            <TBody>
              {isLoading && (
                <TR>
                  <TD colSpan={8}>{strings.common.loading}</TD>
                </TR>
              )}
              {(marketRows ?? []).map((row) => {
                const primary = Number(cellValue(row, "primaryTotal") || 0);
                const secondary = Number(cellValue(row, "secondaryTotal") || 0);
                const hasTotal =
                  cellValue(row, "primaryTotal") !== "" || cellValue(row, "secondaryTotal") !== "";
                return (
                  <TR key={row.material}>
                    <TD className="whitespace-nowrap font-medium">
                      {materialLabels[row.material]}
                    </TD>
                    {COLUMNS.map((column, index) => (
                      <Fragment key={column}>
                        {/* "Total (col. 3+5)" sits between column 1 and column 3 on the form,
                            and it is a sum — shown, never typed. */}
                        {index === 1 && (
                          <TD className="text-right text-gray-500">
                            {hasTotal ? kgFormat.format(primary + secondary) : "—"}
                          </TD>
                        )}
                        <TD className="text-right">
                          <Input
                            type="number"
                            step="0.001"
                            min="0"
                            className="w-28 text-right"
                            disabled={!canWrite}
                            value={cellValue(row, column)}
                            onChange={(ev) => edit(row.material, column, ev.target.value)}
                            onBlur={() => saveRow(row)}
                          />
                        </TD>
                      </Fragment>
                    ))}
                  </TR>
                );
              })}
              <TR>
                <TD className="font-semibold">{t.total}</TD>
                <TD className="text-right font-semibold">{kg(total.salesPackaging)}</TD>
                <TD className="text-right font-semibold">
                  {total.primaryTotal == null && total.secondaryTotal == null
                    ? "—"
                    : kgFormat.format((total.primaryTotal ?? 0) + (total.secondaryTotal ?? 0))}
                </TD>
                <TD className="text-right font-semibold">{kg(total.primaryTotal)}</TD>
                <TD className="text-right font-semibold">{kg(total.primaryReusable)}</TD>
                <TD className="text-right font-semibold">{kg(total.secondaryTotal)}</TD>
                <TD className="text-right font-semibold">{kg(total.secondaryReusable)}</TD>
                <TD className="text-right font-semibold">{kg(total.hazardousContent)}</TD>
              </TR>
            </TBody>
          </Table>
        </div>
      </section>

      {/* ---- Tabel 2: computed from the movements ---- */}
      <section className="mt-10">
        <h2 className="text-lg font-semibold">{t.table2Title}</h2>
        <p className="mt-1 max-w-3xl text-sm text-gray-500">{t.table2Hint}</p>
        <div className="mt-3 overflow-x-auto">
          <Table>
            <THead>
              <TR>
                <TH>{t.material}</TH>
                <TH className="text-right">{t.quantity}</TH>
                <TH>{t.operator}</TH>
                <TH>{t.operatorCui}</TH>
                <TH>{t.operation}</TH>
              </TR>
            </THead>
            <TBody>
              {(handovers ?? []).length === 0 && (
                <TR>
                  <TD colSpan={5} className="text-gray-500">
                    {t.noHandovers}
                  </TD>
                </TR>
              )}
              {(handovers ?? []).map((row, i) => (
                <TR key={`${row.material}-${row.operatorCui}-${row.operation}-${i}`}>
                  <TD className="whitespace-nowrap">{materialLabels[row.material]}</TD>
                  <TD className="text-right">{kg(row.quantity)}</TD>
                  <TD>
                    {row.operatorName}
                    {row.operatorAddress ? (
                      <span className="block text-xs text-gray-500">{row.operatorAddress}</span>
                    ) : null}
                  </TD>
                  <TD>{row.operatorCui ?? "—"}</TD>
                  <TD>{row.operation || "—"}</TD>
                </TR>
              ))}
            </TBody>
          </Table>
        </div>
      </section>
    </div>
  );
}
