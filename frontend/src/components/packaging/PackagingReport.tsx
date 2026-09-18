import { Fragment, useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { AlertTriangle, FileSpreadsheet, FileText, Package } from "lucide-react";
import { useCanWrite } from "@/hooks/useBillingAccess";
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
} from "@/lib/types";
import { useCurrentCompany } from "@/hooks/useCompanies";
import { apiBlobErrorMessage, apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { withCount } from "@/lib/utils";
import { Badge } from "@/components/ui/badge";
import { Menu, MenuItem } from "@/components/ui/menu";
import { DocAction } from "@/components/ui/doc-action";
import { Input } from "@/components/ui/input";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { TableFallbackRow, TableSkeletonRows } from "@/components/ui/table-fallback";
import { useToast } from "@/components/ui/toast";
import { Anexa3Section } from "@/components/packaging/Anexa3Section";
import { countMovements, kg, materialLabels } from "@/components/packaging/packagingFormat";

const t = strings.packaging;
const m = strings.movements;

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
 * Numele coloanei, pentru numele accesibil al celulei.
 *
 * <p>Grila de suprascriere are șaizeci și șase de câmpuri numerice fără nicio etichetă: un
 * cititor de ecran anunța „câmp de editare" de șaizeci și șase de ori, fără să spună nici
 * materialul, nici coloana. Capul de tabel se **vede**, dar nu se aude.
 */
const COLUMN_LABELS: Record<Column, string> = {
  salesPackaging: t.colSales,
  primaryTotal: t.colPrimary,
  primaryReusable: t.colPrimaryReusable,
  secondaryTotal: t.colSecondary,
  secondaryReusable: t.colSecondaryReusable,
  hazardousContent: t.colHazardous,
};

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

/**
 * Semnalele de pe ecranul de ambalaje numără toate acelaşi lucru — mişcări —, deci substantivul
 * stă aici o dată, nu la fiecare apel. Restul numărătorilor din pagină trec prin `withCount`.
 */

/**
 * Ce s-a întâmplat cu rândul de suprascriere: e ceva tastat şi neplecat, pleacă chiar acum, sau a
 * ajuns. Grila se salvează singură la ieşirea din celulă, iar până pe 08.09.2026 se vedeau numai
 * erorile — deci cine completa şaizeci şi şase de cifre n-avea de unde şti câte au ajuns.
 *
 * <p>Ordinea contează: cât timp salvarea zboară se spune asta, chiar dacă s-a mai tastat ceva între
 * timp; ce s-a tastat rămâne „nesalvat" după ce răspunsul vine, fiindcă aia e situaţia. Un rând
 * neatins nu spune nimic — un „—" pe opt rânduri ar fi opt afirmaţii despre nimic.
 */
function RowStatus({ state, dirty }: { state?: "saving" | "saved"; dirty: boolean }) {
  if (state === "saving") return <span className="text-content-muted">{t.overrideSaving}</span>;
  if (dirty) return <Badge variant="warning">{t.overrideDirty}</Badge>;
  if (state === "saved") return <Badge variant="success">{t.overrideSaved}</Badge>;
  return null;
}

/**
 * **Ambalajele unui an** — Anexa 1 Ambalaje (Ordinul 794/2012), Anexa 3 şi tot ce se însumează din
 * mişcările pe coduri {@code 15 01 xx}.
 *
 * <p>Totul pleacă dintr-o singură sursă: mişcările. Până pe 18.09.2026, ecranul şi le arăta şi pe
 * ele, într-un registru propriu — al treilea tabel de mişcări din aplicaţie, cu aceleaşi rânduri
 * ca „Generare" şi fără căutare sau sortare la server. Registrul a rămas unul singur, iar
 * întrebările lui („numai ambalajele", „numai ce-am pus noi pe piaţă", „ce e de completat") au
 * devenit tastele de deasupra listei de mişcări. Ce e aici nu se mai găseşte nicăieri altundeva:
 * tabelele, suprascrierea şi cele două documente.
 *
 * <p>Grila în care se completau cele şaizeci şi şase de celule ale tabelului 1 nu e ecranul
 * principal: rămâne, pliată, ca **suprascriere** pe material, fiindcă tabelul e legal despre marfa
 * pusă pe piaţă şi o firmă poate şti că cifra ei diferă de ce arată deşeul.
 *
 * @param year anul raportat; vine din filtrul de deasupra, nu din starea componentei
 * @param movementsPath ecranul pe care se repară mişcările — „Generare" la generator, „Ieşiri" la
 *        colectorul pur. Semnalele de sus trimit acolo, cu tasta de ambalaje deja apăsată.
 */
export function PackagingReport({
  year,
  movementsPath,
}: {
  year: number;
  movementsPath: string;
}) {
  const canWrite = useCanWrite();
  // Anexa 3 apare la toți: la colector cu preluări și ieșiri, la generator numai cu ieșirile
  // (proprietarul, 16.09.2026; până atunci era ascunsă generatorilor). Cât timp firma nu s-a
  // încărcat, secțiunea nu apare — ca butonul Anexei 2.
  const { data: company } = useCurrentCompany();
  const companyLoaded = company != null;
  const { data: movements } = usePackagingMovements(year);
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
  /**
   * Ce face rândul chiar acum: pleacă spre server, sau tocmai a ajuns. Starea e **per material**,
   * nu pe mutaţie: `saveMut.isPending` e unul singur pentru toată grila, deci ar aprinde toate cele
   * opt rânduri la fiecare salvare. „Nesalvat" nu stă aici — ăla se citeşte din `draft`, care e
   * chiar definiţia lui.
   */
  const [rowState, setRowState] = useState<Record<string, "saving" | "saved">>({});
  // „Salvat" se stinge singur; fără curăţarea asta, un cronometru ar scrie într-o componentă
  // demontată la schimbarea anului sau la plecarea de pe ecran.
  const savedTimers = useRef<Record<string, ReturnType<typeof setTimeout>>>({});
  useEffect(() => {
    const timers = savedTimers.current;
    return () => {
      for (const id of Object.values(timers)) clearTimeout(id);
    };
  }, []);
  useEffect(() => {
    setDraft({});
    setRowState({});
  }, [year]);

  const rows = table1 ?? [];
  /** Câte rânduri au cifre tastate şi neplecate. Zero e starea normală, deci nu se scrie nimic. */
  const dirtyRows = Object.keys(draft).length;

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

  async function handleDownload(format: "xls" | "pdf") {
    setDownloading(format);
    try {
      await downloadPackagingDeclaration(year, format);
    } catch (err) {
      notify(await apiBlobErrorMessage(err, t.downloadError), "error");
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

  function markSaved(material: string) {
    setRowState((prev) => ({ ...prev, [material]: "saved" }));
    clearTimeout(savedTimers.current[material]);
    savedTimers.current[material] = setTimeout(() => {
      setRowState((prev) => {
        // Numai dacă între timp n-a început altă salvare pe rândul ăsta: altfel „se salvează…"
        // s-ar stinge de la cronometrul salvării dinainte.
        if (prev[material] !== "saved") return prev;
        const next = { ...prev };
        delete next[material];
        return next;
      });
    }, 4000);
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
    // Ce pleacă acum, exact aşa cum e în clipa asta. Se ţine minte fiindcă răspunsul vine mai
    // târziu, iar între timp se poate tasta în altă celulă a aceluiaşi rând.
    const sent = { ...pending };
    setRowState((prev) => ({ ...prev, [row.material]: "saving" }));
    clearTimeout(savedTimers.current[row.material]);
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
            const current = prev[row.material];
            if (!current) return prev;
            // Se şterge din ciornă **numai ce s-a trimis şi n-a fost tastat între timp**. Înainte
            // se ştergea rândul întreg: cine tasta în altă celulă cât zbura salvarea îşi pierdea
            // cifra tăcut, la reîmprospătarea de după răspuns. Ce rămâne aici e chiar ce n-a plecat,
            // deci rândul rămâne pe „nesalvat" şi pleacă la ieşirea din celula lui.
            const rest = Object.fromEntries(
              Object.entries(current).filter(([column, value]) => sent[column] !== value)
            );
            const next = { ...prev };
            if (Object.keys(rest).length === 0) delete next[row.material];
            else next[row.material] = rest;
            return next;
          });
          markSaved(row.material);
        },
        onError: (err) => {
          // Ciorna rămâne pe loc: cifra tastată nu se pierde fiindcă serverul a refuzat-o, iar
          // rândul se întoarce la „nesalvat", care e adevărul.
          setRowState((prev) => {
            const next = { ...prev };
            delete next[row.material];
            return next;
          });
          notify(apiErrorMessage(err, t.saveError), "error");
        },
      }
    );
  }

  return (
    <div>
      {/* ---- Ce blochează declaraţia, spus înainte de tabele ----
           Fiecare semnal duce la rândurile lui. Până pe 18.09.2026 ducea la registrul de dedesubt;
           de când registrul e unul singur, duce în „Mişcări", cu tasta potrivită deja apăsată —
           altfel semnalul ar numi vinovatul şi s-ar opri acolo, iar omul ar căuta patru rânduri
           într-un an de mişcări. */}
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
              <li>
                {countMovements(t.blockedMissingMaterial, signals.missingMaterial)}{" "}
                <FixLink to={`${movementsPath}?luna=${year}&ambalaje=de-completat`} />
              </li>
            )}
            {signals.missingCategory > 0 && (
              <li>
                {countMovements(t.blockedMissingCategory, signals.missingCategory)}{" "}
                <FixLink to={`${movementsPath}?luna=${year}&ambalaje=de-completat`} />
              </li>
            )}
            {signals.missingOperation > 0 && (
              <li>
                {countMovements(t.missingOperation, signals.missingOperation)}{" "}
                <FixLink to={`${movementsPath}?luna=${year}&problema=cod-rd`} />
              </li>
            )}
            {signals.awaitingWeighing > 0 && (
              <li>
                {countMovements(t.awaitingWeighing, signals.awaitingWeighing)}{" "}
                {/* „Toate ambalajele", nu „pus de noi pe piață": semnalul e despre kilograme care
                    lipsesc, iar tasta „pe piață" nici nu există pe ecranul colectorului. */}
                <FixLink to={`${movementsPath}?luna=${year}&ambalaje=toate`} />
              </li>
            )}
          </ul>
        </section>
      )}

      {/* ---- Documentele anului, deasupra tabelelor (aşezarea aprobată pe „Totalul anului",
           18.09.2026): butonul poartă numele documentului, explicaţia stă dedesubt. Anexa 3 îşi
           are butoanele ei, lângă tabelul ei, fiindcă se descarcă pe punct de lucru. ---- */}
      <div className="mt-6 sm:max-w-sm">
        <DocAction
          hint={t.downloadHint}
          action={
            <Menu label={t.download} align="left" disabled={downloading != null}>
              <MenuItem icon={FileSpreadsheet} onClick={() => handleDownload("xls")} hint={t.downloadXlsHint}>
                {t.downloadXls}
              </MenuItem>
              <MenuItem icon={FileText} onClick={() => handleDownload("pdf")} hint={t.downloadPdfHint}>
                {t.downloadPdf}
              </MenuItem>
            </Menu>
          }
        />
      </div>

      {/* ---- Tabelul 1, însumat din registrul de mai sus ---- */}
      <section id="tabelul-1" className="mt-10 scroll-mt-20">
        <h2 className="text-lg font-semibold">{t.table1Title}</h2>
        <p className="mt-1 max-w-3xl text-sm text-content-muted">{t.table1Hint}</p>
        <div className="mt-3">
          <Table stickyHeader>
            <THead sticky>
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
              {loadingTable1 && <TableSkeletonRows columns={8} rows={8} />}
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
                        <TD className="text-right text-content-muted">{kg(packagedGoodsTotal(row))}</TD>
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
              <div className="mt-3 rounded-lg border border-line p-4">
                <p className="max-w-3xl text-sm text-content-muted">{t.table1Override}</p>
                <p className="mt-1 text-xs text-content-subtle">{t.overrideClear}</p>
                <div className="mt-3">
                  <Table stickyHeader>
                    <THead sticky>
                      <TR>
                        <TH>{t.material}</TH>
                        <TH className="text-right">{t.colSales}</TH>
                        <TH className="text-right">{t.colPrimary}</TH>
                        <TH className="text-right">{t.colPrimaryReusable}</TH>
                        <TH className="text-right">{t.colSecondary}</TH>
                        <TH className="text-right">{t.colSecondaryReusable}</TH>
                        <TH className="text-right">{t.colHazardous}</TH>
                        <TH>{t.overrideStatus}</TH>
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
                                aria-label={`${materialLabels[row.material]} — ${COLUMN_LABELS[column]}`}
                                className="w-28 text-right"
                                value={cellValue(row, column)}
                                onChange={(ev) => edit(row.material, column, ev.target.value)}
                                onBlur={() => saveOverride(row)}
                              />
                            </TD>
                          ))}
                          {/* Ce s-a întâmplat cu rândul, în cuvinte. `aria-live` fiindcă starea se
                              schimbă singură, fără ca nimeni să apese ceva: altfel un cititor de
                              ecran n-ar afla niciodată că cifra a plecat. */}
                          <TD className="whitespace-nowrap text-xs" aria-live="polite">
                            <RowStatus state={rowState[row.material]} dirty={Boolean(draft[row.material])} />
                          </TD>
                        </TR>
                      ))}
                    </TBody>
                  </Table>
                </div>
                {/* Cifra de care întreba felia: „câte au ajuns". Coloana o spune pe rând, dar un
                    rând nesalvat poate fi derulat afară din ochi — aici se vede oricum. */}
                {dirtyRows > 0 && (
                  <p className="mt-2 text-xs text-amber-700">
                    {withCount(t.overrideUnsavedRows, dirtyRows, "rând", "rânduri")}
                  </p>
                )}
              </div>
            )}
          </div>
        )}
      </section>

      {/* ---- Tabelul 2, calculat din predări ---- */}
      <section id="tabelul-2" className="mt-10 scroll-mt-20">
        <h2 className="text-lg font-semibold">{t.table2Title}</h2>
        <p className="mt-1 max-w-3xl text-sm text-content-muted">{t.table2Hint}</p>
        <div className="mt-3">
          <Table stickyHeader>
            <THead sticky>
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
                <TableFallbackRow columns={5} loading={false} icon={Package} title={t.noHandovers} />
              )}
              {(handovers ?? []).map((row, i) => (
                <TR key={`${row.material}-${row.operatorCui}-${row.operation}-${i}`}>
                  <TD className="whitespace-nowrap">{materialLabels[row.material]}</TD>
                  <TD className="text-right">{kg(row.quantity)}</TD>
                  <TD>
                    {row.operatorName}
                    {row.operatorAddress ? (
                      <span className="block text-xs text-content-muted">{row.operatorAddress}</span>
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

      {companyLoaded && <Anexa3Section year={year} />}
    </div>
  );
}


/** „Vezi mişcările" — acelaşi link pe fiecare semnal, deci scris o dată. */
function FixLink({ to }: { to: string }) {
  return (
    <Link to={to} className="font-medium underline hover:no-underline">
      {m.packagingSeeMovements}
    </Link>
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
    <TR className="bg-surface-muted">
      <TD className="font-semibold">{label}</TD>
      {columns.map((pick, i) => (
        <TD key={i} className="text-right font-semibold">
          {kg(sumOver(rows, parts, pick))}
        </TD>
      ))}
    </TR>
  );
}
