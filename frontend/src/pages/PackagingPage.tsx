import { Fragment, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { AlertTriangle, FileSpreadsheet, FileText, Plus } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import {
  downloadPackagingDeclaration,
  usePackagingHandovers,
  usePackagingMarket,
  usePackagingMovements,
  usePackagingTable1,
  usePackagingUnclassified,
  useSavePackagingMarket,
} from "@/hooks/usePackaging";
import type {
  PackagingMarketRow,
  PackagingMaterial,
  PackagingTable1Row,
  PackagingUnclassifiedRow,
  WasteMovement,
} from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { useToast } from "@/components/ui/toast";

const t = strings.packaging;
const materialLabels = strings.enums.packagingMaterial;
const categoryLabels = strings.enums.packagingCategory;

/** Year options: current year down to five years back, same as the other documents. */
function yearOptions(): number[] {
  const now = new Date().getFullYear();
  return Array.from({ length: 6 }, (_, i) => now - i);
}

const kgFormat = new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 3 });
function kg(value: number | null | undefined) {
  return value == null ? "—" : kgFormat.format(value);
}

/** The six figures of a material row; "Total (col. 3+5)" is a sum and is never typed. */
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
 * Rândurile de material, în ordinea actului, cu cele trei sume intercalate — exact aşa cum le
 * desenează formularul: PET + Alte plastice = Total plastic, Aluminiu + Oţel = Total metal, iar
 * Sticla, Hârtia carton, Lemnul şi Altele stau singure.
 */
const MATERIAL_ORDER: PackagingMaterial[] = [
  "STICLA",
  "PET",
  "ALTE_PLASTICE",
  "HARTIE_CARTON",
  "ALUMINIU",
  "OTEL",
  "LEMN",
  "ALTELE",
];
const PLASTIC_PARTS: PackagingMaterial[] = ["PET", "ALTE_PLASTICE"];
const METAL_PARTS: PackagingMaterial[] = ["ALUMINIU", "OTEL"];

/** Col. 2 of the form: a sum of columns 3 and 5, empty when both are. */
function packagedGoodsTotal(row: PackagingTable1Row | undefined) {
  if (!row || (row.primaryTotal == null && row.secondaryTotal == null)) return null;
  return (row.primaryTotal ?? 0) + (row.secondaryTotal ?? 0);
}

/**
 * A sum over several material rows that stays empty when every one of them is: adding nothing to
 * nothing is not zero on this form, it is still "nobody answered".
 */
function sumOver(
  rows: PackagingTable1Row[],
  parts: PackagingMaterial[],
  pick: (row: PackagingTable1Row) => number | null
) {
  let total: number | null = null;
  for (const row of rows) {
    if (!parts.includes(row.material)) continue;
    const value = pick(row);
    if (value != null) total = (total ?? 0) + value;
  }
  return total;
}

function fill(template: string, n: number) {
  return template.replace("{n}", String(n));
}

/**
 * Tabul **Ambalaje** — Anexa 1 Ambalaje (Ordinul 794/2012) şi tot ce ţine de ea.
 *
 * <p>Ecranul are o singură sursă: **mişcările pe coduri 15 01 xx**. Registrul le arată aşa cum
 * sunt, cu ce le lipseşte scris pe fiecare rând; cele două tabele ale declaraţiei se însumează din
 * ele; iar butonul de descărcare scoate exact ce va citi agenţia — un `.xls` cu două foi, formatul
 * pe care art. 6 din ordin îl cere pe nume.
 *
 * <p>Grila în care se completau cele şaizeci şi şase de celule ale tabelului 1 nu mai e ecranul
 * principal: rămâne, pliată, ca **suprascriere** pe material, fiindcă tabelul e legal despre marfa
 * pusă pe piaţă şi o firmă poate şti că cifra ei diferă de ce arată deşeul.
 */
export function PackagingPage() {
  const { user } = useAuth();
  const canWrite =
    user?.role === "PLATFORM_ADMIN" || user?.role === "ADMIN" || user?.role === "OPERATOR";

  const [year, setYear] = useState(() => new Date().getFullYear());
  const { data: movements, isLoading: loadingMovements } = usePackagingMovements(year);
  const { data: table1, isLoading: loadingTable1 } = usePackagingTable1(year);
  const { data: handovers } = usePackagingHandovers(year);
  const { data: unclassified } = usePackagingUnclassified(year);
  const { data: overrides } = usePackagingMarket(year);
  const saveMut = useSavePackagingMarket();
  const { notify } = useToast();
  const [downloading, setDownloading] = useState<"xls" | "pdf" | null>(null);
  const [overridesOpen, setOverridesOpen] = useState(false);

  // Ce s-a tastat şi nu s-a salvat încă, per material. Salvarea e pe rând, la ieşirea din câmp:
  // grila are şaizeci şi şase de celule, iar un buton „salvează tot" ar face o greşeală invizibilă.
  const [draft, setDraft] = useState<Record<string, Record<string, string>>>({});
  useEffect(() => {
    setDraft({});
  }, [year]);

  const rows = table1 ?? [];
  const rowFor = (material: PackagingMaterial) => rows.find((r) => r.material === material);

  const signals = useMemo(() => {
    const list = unclassified ?? [];
    // Semnalele privesc doar ce hrăneşte declaraţia. O mişcare pe marfă preluată nu intră în Anexa 1
    // oricât de completă ar fi, deci a o număra la „de cântărit" ar cere o reparaţie fără efect.
    // Semnalele privesc doar ce hrăneşte declaraţia: marfa preluată şi ambalajul pe care nu l-am
    // pus noi pe piaţă n-au ce repara acolo, oricât de incomplete ar fi.
    const all = (movements ?? []).filter(
      (m) => m.register !== "ART_48" && m.countsForAnexa1Packaging
    );
    return {
      missingMaterial: list.filter((r) => r.missingMaterial).length,
      missingCategory: list.filter((r) => !r.missingMaterial && r.missingCategory).length,
      awaitingWeighing: all.filter((m) => m.quantity == null).length,
      missingOperation: all.filter(
        (m) => (m.operation === "RECOVERED" || m.operation === "DISPOSED") && !m.operationCode
      ).length,
    };
  }, [unclassified, movements]);

  const unclassifiedIds = useMemo(
    () => new Set((unclassified ?? []).map((r: PackagingUnclassifiedRow) => r.movementId)),
    [unclassified]
  );

  async function handleDownload(format: "xls" | "pdf") {
    setDownloading(format);
    try {
      await downloadPackagingDeclaration(year, format);
    } catch (err) {
      notify(apiErrorMessage(err, t.downloadError), "error");
    } finally {
      setDownloading(null);
    }
  }

  function cellValue(row: PackagingMarketRow, column: Column) {
    const pending = draft[row.material]?.[column];
    if (pending !== undefined) return pending;
    return row[column] == null ? "" : String(row[column]);
  }

  function edit(material: PackagingMaterial, column: Column, value: string) {
    setDraft((prev) => ({ ...prev, [material]: { ...prev[material], [column]: value } }));
  }

  function saveOverride(row: PackagingMarketRow) {
    const pending = draft[row.material];
    if (!pending) return;
    // O celulă goală înseamnă „nu suprascriu", şi se trimite null, nu zero. Un rând golit de tot
    // şterge suprascrierea şi lasă cifra din mişcări să revină.
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

  return (
    <div>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">{t.title}</h1>
          <p className="mt-1 max-w-3xl text-sm text-gray-500">{t.subtitle}</p>
        </div>
        <div className="flex flex-col items-end gap-2">
          <div className="flex gap-2">
            <Button onClick={() => handleDownload("xls")} disabled={downloading != null}>
              <FileSpreadsheet className="mr-2 h-4 w-4" />
              {downloading === "xls" ? strings.common.loading : t.downloadXls}
            </Button>
            <Button
              variant="outline"
              onClick={() => handleDownload("pdf")}
              disabled={downloading != null}
            >
              <FileText className="mr-2 h-4 w-4" />
              {downloading === "pdf" ? strings.common.loading : t.downloadPdf}
            </Button>
          </div>
          <p className="max-w-sm text-right text-xs text-gray-500">{t.downloadHint}</p>
        </div>
      </div>

      <div className="mt-6 w-40">
        <Label htmlFor="pk-year">{t.year}</Label>
        <Select id="pk-year" value={String(year)} onChange={(ev) => setYear(Number(ev.target.value))}>
          {yearOptions().map((y) => (
            <option key={y} value={y}>
              {y}
            </option>
          ))}
        </Select>
      </div>

      {/* ---- Ce blochează declaraţia, spus înainte de tabele ---- */}
      {(signals.missingMaterial > 0 ||
        signals.missingCategory > 0 ||
        signals.awaitingWeighing > 0 ||
        signals.missingOperation > 0) && (
        <section className="mt-6 rounded-lg border border-amber-200 bg-amber-50 p-4">
          <h2 className="flex items-center gap-2 text-sm font-semibold text-amber-900">
            <AlertTriangle className="h-4 w-4" />
            {t.blockedTitle}
          </h2>
          <ul className="mt-2 list-disc space-y-1 pl-5 text-sm text-amber-900">
            {signals.missingMaterial > 0 && (
              <li>{fill(t.blockedMissingMaterial, signals.missingMaterial)}</li>
            )}
            {signals.missingCategory > 0 && (
              <li>{fill(t.blockedMissingCategory, signals.missingCategory)}</li>
            )}
            {signals.missingOperation > 0 && (
              <li>{fill(t.missingOperation, signals.missingOperation)}</li>
            )}
            {signals.awaitingWeighing > 0 && (
              <li>{fill(t.awaitingWeighing, signals.awaitingWeighing)}</li>
            )}
          </ul>
        </section>
      )}

      {/* ---- Registrul: mişcările din care iese totul ---- */}
      <section className="mt-8">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold">{t.registerTitle}</h2>
            <p className="mt-1 max-w-3xl text-sm text-gray-500">{t.registerHint}</p>
          </div>
          {canWrite && (
            <Link
              to="/miscari"
              className="inline-flex h-10 items-center rounded-md border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              <Plus className="mr-2 h-4 w-4" />
              {t.addMovement}
            </Link>
          )}
        </div>
        <div className="mt-3 overflow-x-auto">
          <Table>
            <THead>
              <TR>
                <TH>{t.date}</TH>
                <TH>{t.code}</TH>
                <TH>{t.material}</TH>
                <TH>{t.kind}</TH>
                <TH className="text-right">{t.quantity}</TH>
                <TH>{t.partner}</TH>
                <TH>{t.operation}</TH>
                <TH>{t.inAnexa1}</TH>
                <TH>{t.origin}</TH>
                <TH>{t.workPoint}</TH>
              </TR>
            </THead>
            <TBody>
              {loadingMovements && (
                <TR>
                  <TD colSpan={10}>{strings.common.loading}</TD>
                </TR>
              )}
              {!loadingMovements && (movements ?? []).length === 0 && (
                <TR>
                  <TD colSpan={10} className="text-gray-500">
                    {t.registerEmpty}
                  </TD>
                </TR>
              )}
              {(movements ?? []).map((m: WasteMovement) => (
                <TR
                  key={m.id}
                  className={
                    m.register === "ART_48" || m.packagingOnMarket === false
                      ? "text-gray-400"
                      : unclassifiedIds.has(m.id)
                        ? "bg-amber-50/60"
                        : undefined
                  }
                >
                  <TD className="whitespace-nowrap">{m.date}</TD>
                  <TD className="whitespace-nowrap font-mono text-xs">{m.wasteCode}</TD>
                  <TD className="whitespace-nowrap">
                    {m.effectivePackagingMaterial ? (
                      <>
                        {materialLabels[m.effectivePackagingMaterial]}
                        {m.packagingMaterial == null && (
                          <span className="ml-1 text-xs text-gray-400">({t.fromCode})</span>
                        )}
                      </>
                    ) : (
                      <Badge variant="warning">{t.fix}</Badge>
                    )}
                  </TD>
                  <TD className="whitespace-nowrap">
                    {m.packagingCategory ? (
                      <>
                        {categoryLabels[m.packagingCategory]}
                        {m.packagingReusable && (
                          <span className="ml-1 text-xs text-gray-500">· reutilizabil</span>
                        )}
                        {m.packagingHazardousContent && (
                          <span className="ml-1 text-xs text-gray-500">· periculos</span>
                        )}
                      </>
                    ) : (
                      <Badge variant="warning">{t.fix}</Badge>
                    )}
                  </TD>
                  <TD className="text-right">
                    {m.quantity == null ? (
                      <Badge variant="warning">{strings.movements.awaitingWeighing}</Badge>
                    ) : (
                      `${kg(m.quantity)} ${m.unit === "TONS" ? "t" : "kg"}`
                    )}
                  </TD>
                  <TD>{m.partnerName ?? "—"}</TD>
                  <TD>
                    {m.operationCode ?? (
                      (m.operation === "RECOVERED" || m.operation === "DISPOSED") ? (
                        <Badge variant="danger">{t.fix}</Badge>
                      ) : (
                        "—"
                      )
                    )}
                  </TD>
                  {/* Bifa de pe mişcare decide dacă rândul hrăneşte declaraţia. Se arată aici,
                      fiindcă tabul e locul unde omul verifică ce va fi depus. */}
                  <TD className="whitespace-nowrap">
                    {m.packagingOnMarket === false ? (
                      <Badge variant="muted" title={t.inAnexa1NoHint}>
                        {t.inAnexa1No}
                      </Badge>
                    ) : m.packagingOnMarket == null ? (
                      <Badge variant="warning" title={t.inAnexa1LegacyHint}>
                        {t.inAnexa1Legacy}
                      </Badge>
                    ) : (
                      <span className="text-xs text-gray-500">{t.inAnexa1Yes}</span>
                    )}
                  </TD>
                  {/* Mişcările pe marfă preluată apar în registru fiindcă sunt ambalaj, dar nu
                      hrănesc niciun tabel: Anexa 1 e despre deşeul propriu. Se spune pe rând, ca
                      să nu pară că lipsesc din calcul dintr-o eroare. */}
                  <TD className="whitespace-nowrap">
                    {m.register === "ART_48" ? (
                      <Badge variant="muted" title={t.originTakeoverInTab}>
                        {t.originTakeoverShort}
                      </Badge>
                    ) : (
                      <span className="text-xs text-gray-500">{t.originOwnShort}</span>
                    )}
                  </TD>
                  <TD>{m.workPointName}</TD>
                </TR>
              ))}
            </TBody>
          </Table>
        </div>
      </section>

      {/* ---- Tabelul 1, însumat din registrul de mai sus ---- */}
      <section className="mt-10">
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
              {loadingTable1 && (
                <TR>
                  <TD colSpan={8}>{strings.common.loading}</TD>
                </TR>
              )}
              {!loadingTable1 &&
                MATERIAL_ORDER.map((material) => {
                  const row = rowFor(material);
                  return (
                    <Fragment key={material}>
                      <TR>
                        <TD className="whitespace-nowrap font-medium">
                          {materialLabels[material]}
                          {row?.overridden && (
                            <Badge className="ml-2" variant="default">
                              {t.overriddenBadge}
                            </Badge>
                          )}
                        </TD>
                        <TD className="text-right">{kg(row?.salesPackaging)}</TD>
                        <TD className="text-right text-gray-500">{kg(packagedGoodsTotal(row))}</TD>
                        <TD className="text-right">{kg(row?.primaryTotal)}</TD>
                        <TD className="text-right">{kg(row?.primaryReusable)}</TD>
                        <TD className="text-right">{kg(row?.secondaryTotal)}</TD>
                        <TD className="text-right">{kg(row?.secondaryReusable)}</TD>
                        <TD className="text-right">{kg(row?.hazardousContent)}</TD>
                      </TR>
                      {material === "ALTE_PLASTICE" && sumRow(rows, t.totalPlastic, PLASTIC_PARTS)}
                      {material === "OTEL" && sumRow(rows, t.totalMetal, METAL_PARTS)}
                    </Fragment>
                  );
                })}
              {!loadingTable1 && sumRow(rows, t.total, MATERIAL_ORDER)}
            </TBody>
          </Table>
        </div>

        {/* Suprascrierea: pliată, fiindcă e excepţia, nu regula. */}
        {canWrite && (
          <div className="mt-4">
            <button
              type="button"
              className="text-sm font-medium text-emerald-700 hover:underline"
              onClick={() => setOverridesOpen((open) => !open)}
            >
              {overridesOpen ? t.overrideClose : t.overrideOpen}
            </button>
            {overridesOpen && (
              <div className="mt-3 rounded-lg border border-gray-200 p-4">
                <p className="max-w-3xl text-sm text-gray-500">{t.table1Override}</p>
                <p className="mt-1 text-xs text-gray-400">{t.overrideClear}</p>
                <div className="mt-3 overflow-x-auto">
                  <Table>
                    <THead>
                      <TR>
                        <TH>{t.material}</TH>
                        <TH className="text-right">{t.colSales}</TH>
                        <TH className="text-right">{t.colPrimary}</TH>
                        <TH className="text-right">{t.colPrimaryReusable}</TH>
                        <TH className="text-right">{t.colSecondary}</TH>
                        <TH className="text-right">{t.colSecondaryReusable}</TH>
                        <TH className="text-right">{t.colHazardous}</TH>
                      </TR>
                    </THead>
                    <TBody>
                      {(overrides ?? []).map((row) => (
                        <TR key={row.material}>
                          <TD className="whitespace-nowrap font-medium">
                            {materialLabels[row.material]}
                          </TD>
                          {COLUMNS.map((column) => (
                            <TD key={column} className="text-right">
                              <Input
                                type="number"
                                step="0.001"
                                min="0"
                                className="w-28 text-right"
                                value={cellValue(row, column)}
                                onChange={(ev) => edit(row.material, column, ev.target.value)}
                                onBlur={() => saveOverride(row)}
                              />
                            </TD>
                          ))}
                        </TR>
                      ))}
                    </TBody>
                  </Table>
                </div>
              </div>
            )}
          </div>
        )}
      </section>

      {/* ---- Tabelul 2, calculat din predări ---- */}
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

/** Un rând de sumă din formular — Total plastic, Total metal, TOTAL. */
function sumRow(rows: PackagingTable1Row[], label: string, parts: PackagingMaterial[]) {
  const columns: ((row: PackagingTable1Row) => number | null)[] = [
    (r) => r.salesPackaging,
    (r) => packagedGoodsTotal(r),
    (r) => r.primaryTotal,
    (r) => r.primaryReusable,
    (r) => r.secondaryTotal,
    (r) => r.secondaryReusable,
    (r) => r.hazardousContent,
  ];
  return (
    <TR className="bg-gray-50">
      <TD className="font-semibold">{label}</TD>
      {columns.map((pick, i) => (
        <TD key={i} className="text-right font-semibold">
          {kg(sumOver(rows, parts, pick))}
        </TD>
      ))}
    </TR>
  );
}
