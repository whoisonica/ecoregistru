import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Download, FileSpreadsheet, FileText, RefreshCw } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { useWorkPoints } from "@/hooks/useWorkPoints";
import {
  downloadAnexa1Form,
  downloadAnnualDeclaration,
  downloadEvidenceExport,
  useEvidences,
  useRegenerateEvidence,
} from "@/hooks/useEvidences";
import type { EvidenceFilters, MovementFilters } from "@/lib/types";
import { HandoverRegister } from "@/components/HandoverRegister";
import { AwaitingWeighingDialog } from "@/components/AwaitingWeighingDialog";
import { apiBlobErrorMessage, apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { withCount } from "@/lib/utils";
import { useUrlNumber, useUrlState } from "@/hooks/useUrlState";
import { formatTonnes } from "@/lib/units";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { Menu, MenuItem } from "@/components/ui/menu";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Tooltip } from "@/components/ui/tooltip";
import { Select } from "@/components/ui/select";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { SortableTH } from "@/components/ui/table";
import { TablePagination, TableToolbar } from "@/components/ui/table-toolbar";
import { useTableView } from "@/hooks/useTableView";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { useToast } from "@/components/ui/toast";

const t = strings.evidences;

/** Quantities from the backend are in KG; format with the Romanian locale. */
const kgFormat = new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 3 });
function kg(value: number) {
  return kgFormat.format(value);
}

function monthName(month: number) {
  return strings.months[month - 1] ?? String(month);
}

/** Year options: current year down to five years back. */
function yearOptions(): number[] {
  const now = new Date().getFullYear();
  return Array.from({ length: 6 }, (_, i) => now - i);
}

export function EvidencesPage() {
  const { user } = useAuth();
  const canManage =
    user?.role === "PLATFORM_ADMIN" || user?.role === "ADMIN" || user?.role === "OPERATOR";

  const { data: workPoints } = useWorkPoints();
  const activeWorkPoints = useMemo(
    () => (workPoints ?? []).filter((w) => w.active),
    [workPoints]
  );

  const [year, setYear] = useUrlNumber("an", new Date().getFullYear());
  const [month, setMonth] = useUrlState("luna"); // "" = toate lunile
  const [workPointId, setWorkPointId] = useUrlState("punct"); // "" = toate punctele
  /**
   * Two views of the same period. "Predări" is the default because it is what the meeting asked
   * the tab to show — quantity, handover date, partner, R/D code, and that is it. The monthly
   * Anexa 1 aggregate stays one click away: the running stock is the one figure that cannot be
   * reconstructed by reading the rows, and it is the one the form is built around.
   */
  const [viewParam, setViewParam] = useUrlState("vedere", "handovers");
  const view = viewParam === "monthly" ? "monthly" : "handovers";
  const setView = setViewParam;

  const movementFilters: MovementFilters = useMemo(() => {
    const f: MovementFilters = { year };
    if (month) f.month = Number(month);
    if (workPointId) f.workPointId = workPointId;
    return f;
  }, [year, month, workPointId]);

  const filters: EvidenceFilters = useMemo(() => {
    const f: EvidenceFilters = { year };
    if (month) f.month = Number(month);
    if (workPointId) f.workPointId = workPointId;
    return f;
  }, [year, month, workPointId]);

  const { data: evidences, isLoading, isError } = useEvidences(filters);
  const regenerateMut = useRegenerateEvidence();
  const { notify } = useToast();
  /**
   * Ce document se pregătește acum. Patru valori, nu trei: „Evidența gestiunii deșeurilor" și
   * „Export PDF" foloseau amândouă `"pdf"`, deci apăsarea pe al doilea învârtea rotița pe primul —
   * adică pe documentul oficial, exact confuzia pe care cele două butoane există ca s-o evite.
   */
  const [exporting, setExporting] = useState<"anexa1" | "declaration" | "xlsx" | "pdf" | null>(
    null
  );

  // Stable display order: work point, then month, then waste code.
  const rows = useMemo(() => {
    return [...(evidences ?? [])].sort(
      (a, b) =>
        a.workPointName.localeCompare(b.workPointName, "ro") ||
        a.month - b.month ||
        a.wasteCode.localeCompare(b.wasteCode, "ro")
    );
  }, [evidences]);

  /**
   * Vederea lunară poate ajunge la sute de linii: codurile de deșeu × lunile × punctele de lucru.
   * Nu primește sortare implicită — `rows` vine deja așezat pe punct de lucru, lună și cod, adică
   * exact ordinea în care se citește fișa, iar o sortare proprie ar strica-o din pornire.
   */
  const monthlyView = useTableView(rows, {
    searchText: (r) => [r.wasteCode, r.wasteCodeName, r.workPointName].join(" "),
    comparators: {
      workPointName: (a, b) => a.workPointName.localeCompare(b.workPointName, "ro"),
      month: (a, b) => a.month - b.month,
      wasteCode: (a, b) => a.wasteCode.localeCompare(b.wasteCode, "ro"),
      totalGenerated: (a, b) => a.totalGenerated - b.totalGenerated,
      closingStock: (a, b) => a.closingStock - b.closingStock,
    },
  });

  function handleRegenerate() {
    regenerateMut.mutate(year, {
      onSuccess: (res) =>
        notify(
          // Stock carries across years, so a regeneration rebuilds the later ones too — say so,
          // otherwise the line count looks wrong for the year that was asked for.
          //
          // Zero linii are șir propriu, ca la Termene (`generatedNone`): nu e o eroare — anul chiar
          // n-are mișcări —, dar „Evidență regenerată: 0 de linii" se citește ca una. Se vede pe
          // orice firmă nouă, deci la primul contact al oricărui client.
          (res.linesGenerated === 0
            ? t.regeneratedNone
            : withCount(
                res.cascadedYears.length > 0 ? t.regeneratedCascade : t.regenerated,
                res.linesGenerated,
                "linie",
                "linii"
              )
          )
            .replace("{year}", String(res.year))
            .replace("{years}", res.cascadedYears.join(", ")),
          "success"
        ),
      onError: (err) => notify(apiErrorMessage(err, t.regenerateError), "error"),
    });
  }

  /**
   * Both official documents are yearly and cover every month, whatever month the screen is
   * filtered to — so the check that runs before them has to look at the whole year, scoped to the
   * work point being printed and nothing wider. That is the "doar unde impactează" part: a load
   * still on the road in another work point is not this document's problem.
   */
  const { data: yearRows } = useEvidences({ year, workPointId: workPointId || undefined });
  const pendingWeighing = useMemo(
    () => (yearRows ?? []).filter((r) => r.awaitingWeighing),
    [yearRows]
  );
  /**
   * Dacă **anul** are ceva de tipărit — nu luna de pe ecran.
   *
   * <p>Cele două documente oficiale sunt anuale și acoperă toate cele douăsprezece luni, oricum ar
   * fi filtrat ecranul. Butoanele se dezactivau pe `rows`, care e filtrat pe lună: alegeai o lună
   * fără mișcări și nu mai puteai descărca fișa anului, deși anul avea date. Exporturile generice
   * rămân pe `rows`, fiindcă ele chiar exportă ce se vede.
   */
  const hasYearData = (yearRows ?? []).length > 0;

  /**
   * Totalul anului per cod de deșeu — ce se încarcă în SIM pe 15 martie, iar OUG 92/2021 art. 48
   * alin. (1) îl cere **în tone**. Se calculează din rândurile anului, nu din cele filtrate pe
   * lună: depunerea acoperă anul întreg, oricum ar fi filtrat ecranul (aceeași logică pentru care
   * `yearRows` există deja, pentru verificarea de dinaintea documentelor).
   *
   * Punctul 7 al auditului. Vezi `lib/units.ts` pentru ce nu face: nu mută niciun formular pe tone.
   */
  const annualByCode = useMemo(() => {
    const acc = new Map<
      string,
      { wasteCode: string; wasteCodeName: string; hazardous: boolean; generated: number; recovered: number; disposed: number }
    >();
    for (const r of yearRows ?? []) {
      const entry = acc.get(r.wasteCode) ?? {
        wasteCode: r.wasteCode,
        wasteCodeName: r.wasteCodeName,
        hazardous: r.hazardous,
        generated: 0,
        recovered: 0,
        disposed: 0,
      };
      entry.generated += r.totalGenerated;
      entry.recovered += r.totalRecovered;
      entry.disposed += r.totalDisposed;
      acc.set(r.wasteCode, entry);
    }
    return [...acc.values()].sort((a, b) => a.wasteCode.localeCompare(b.wasteCode, "ro"));
  }, [yearRows]);
  const [pendingDoc, setPendingDoc] = useState<null | "anexa1" | "declaration">(null);

  async function handleAnexa1() {
    if (pendingWeighing.length > 0) {
      setPendingDoc("anexa1");
      return;
    }
    await generateAnexa1();
  }

  async function generateAnexa1() {
    setExporting("anexa1");
    try {
      await downloadAnexa1Form(filters);
    } catch (err) {
      notify(await apiBlobErrorMessage(err, t.anexa1Error), "error");
    } finally {
      setExporting(null);
    }
  }

  async function handleAnnualDeclaration() {
    if (pendingWeighing.length > 0) {
      setPendingDoc("declaration");
      return;
    }
    await generateAnnualDeclaration();
  }

  async function generateAnnualDeclaration() {
    setExporting("declaration");
    try {
      await downloadAnnualDeclaration(filters);
    } catch (err) {
      notify(await apiBlobErrorMessage(err, t.annualDeclarationError), "error");
    } finally {
      setExporting(null);
    }
  }

  async function handleExport(format: "xlsx" | "pdf") {
    setExporting(format);
    try {
      await downloadEvidenceExport(filters, format);
    } catch (err) {
      notify(await apiBlobErrorMessage(err, t.exportError), "error");
    } finally {
      setExporting(null);
    }
  }

  return (
    <div>
      {pendingDoc && (
        <AwaitingWeighingDialog
          lines={pendingWeighing}
          onCancel={() => setPendingDoc(null)}
          onConfirm={() => {
            const doc = pendingDoc;
            setPendingDoc(null);
            void (doc === "anexa1" ? generateAnexa1() : generateAnnualDeclaration());
          }}
        />
      )}
      <PageHeader
        title={t.title}
        description={t.subtitle}
        actions={
          <>
            {/* The official form first: it is the one the client actually files. */}
            <Button
              onClick={handleAnexa1}
              disabled={!hasYearData || exporting !== null}
              loading={exporting === "anexa1"}
              title={t.anexa1Hint}
            >
              {exporting !== "anexa1" && <FileText className="mr-2 h-4 w-4" />}
              {t.anexa1}
            </Button>
            {/* The summary that goes in front of it, and the page the authority reads first. */}
            <Button
              variant="outline"
              onClick={handleAnnualDeclaration}
              disabled={!hasYearData || exporting !== null}
              loading={exporting === "declaration"}
              title={t.annualDeclarationHint}
            >
              {exporting !== "declaration" && <FileText className="mr-2 h-4 w-4" />}
              {t.annualDeclaration}
            </Button>
            {/* Exporturile generice intră într-un meniu, fiindcă sunt de alt fel decât cele două de
                deasupra: pe ele scrie „rezumat generic (neoficial)", iar la agenție se depun
                celelalte. Cinci butoane la fel de vizibile ziceau că toate cinci sunt același
                lucru — și strângeau titlul paginii pe trei rânduri ca să încapă.

                Rămân la îndemâna oricui, viewer inclusiv: sunt o citire, nu o scriere. */}
            <Menu label={t.exportsMenu} disabled={rows.length === 0 || exporting !== null}>
              <MenuItem
                icon={Download}
                hint={t.exportsHint}
                onClick={() => handleExport("xlsx")}
                disabled={rows.length === 0 || exporting !== null}
              >
                {t.exportExcel}
              </MenuItem>
              {/* Nota stă pe amândouă, nu doar pe prima: sunt două fișiere de același fel, iar
                  cine se uită la al doilea vedea doar „Rezumat PDF" — adică exact cuvântul care
                  nu spune că nu se depune. Un avertisment pus o singură dată păzește un rând. */}
              <MenuItem
                icon={Download}
                hint={t.exportsHint}
                onClick={() => handleExport("pdf")}
                disabled={rows.length === 0 || exporting !== null}
              >
                {t.exportPdf}
              </MenuItem>
            </Menu>
            {canManage && (
              <Button onClick={handleRegenerate} disabled={regenerateMut.isPending}>
                <RefreshCw
                  className={`mr-2 h-4 w-4 ${regenerateMut.isPending ? "animate-spin" : ""}`}
                />
                {regenerateMut.isPending ? t.regenerating : t.regenerate}
              </Button>
            )}
          </>
        }
      />

      {/* Nota nu mai e un avertisment: de când citirea reconstruiește singură un an rămas în urmă
          (`EvidenceCalculator.list`), „Evidența nu se actualizează singură" era o afirmație falsă
          scrisă cu galben pe fiecare vizită — iar un avertisment permanent devine tapet exact
          până în ziua în care ar fi trebuit să apere ceva. */}
      {canManage && <p className="mt-4 text-sm text-content-muted">{t.staleNote}</p>}

      {/* Filters */}
      <div className="mt-6 inline-flex rounded-lg border border-line bg-surface-muted p-0.5">
        {(["handovers", "monthly"] as const).map((v) => (
          <button
            key={v}
            type="button"
            onClick={() => setView(v)}
            className={
              view === v
                ? "rounded-md bg-surface px-3 py-1.5 text-sm font-medium text-content shadow-sm"
                : "rounded-md px-3 py-1.5 text-sm text-content-muted hover:text-content-strong"
            }
          >
            {v === "handovers" ? t.viewHandovers : t.viewMonthly}
          </button>
        ))}
      </div>

      <div className="mt-4 grid gap-3 sm:flex sm:flex-wrap sm:items-end">
        <div>
          <Label htmlFor="ev-year">{t.filterYear}</Label>
          <Select
            id="ev-year"
            value={String(year)}
            onChange={(ev) => setYear(Number(ev.target.value))}
            className="w-full sm:w-32"
          >
            {yearOptions().map((y) => (
              <option key={y} value={y}>
                {y}
              </option>
            ))}
          </Select>
        </div>
        <div>
          <Label htmlFor="ev-month">{t.filterMonth}</Label>
          <Select
            id="ev-month"
            value={month}
            onChange={(ev) => setMonth(ev.target.value)}
            className="w-full sm:w-44"
          >
            <option value="">{t.allMonths}</option>
            {strings.months.map((name, i) => (
              <option key={i} value={i + 1}>
                {name}
              </option>
            ))}
          </Select>
        </div>
        <div>
          <Label htmlFor="ev-wp">{t.filterWorkPoint}</Label>
          <Select
            id="ev-wp"
            value={workPointId}
            onChange={(ev) => setWorkPointId(ev.target.value)}
            className="w-full sm:w-56"
          >
            <option value="">{t.allWorkPoints}</option>
            {activeWorkPoints.map((w) => (
              <option key={w.id} value={w.id}>
                {w.name}
              </option>
            ))}
          </Select>
        </div>
      </div>

      <section className="mt-4">
        {view === "handovers" && <HandoverRegister filters={movementFilters} />}

        {view === "monthly" && (
          <>
            {isError && <p className="text-sm text-red-600">{t.loadError}</p>}

            {!isError && (
              <div>
                <TableToolbar view={monthlyView} placeholder={t.searchPlaceholder} />
                <Table stickyHeader>
                  <THead sticky>
                    <TR>
                      <SortableTH
                        sortKey="workPointName"
                        sort={monthlyView.sort}
                        onSort={monthlyView.toggleSort}
                      >
                        {t.colWorkPoint}
                      </SortableTH>
                      <SortableTH
                        sortKey="month"
                        sort={monthlyView.sort}
                        onSort={monthlyView.toggleSort}
                      >
                        {t.colMonth}
                      </SortableTH>
                      <SortableTH
                        sortKey="wasteCode"
                        sort={monthlyView.sort}
                        onSort={monthlyView.toggleSort}
                      >
                        {t.colWasteCode}
                      </SortableTH>
                      <SortableTH
                        sortKey="totalGenerated"
                        sort={monthlyView.sort}
                        onSort={monthlyView.toggleSort}
                        align="right"
                      >
                        {t.colGenerated}
                      </SortableTH>
                      <TH className="text-right">{t.colRecovered}</TH>
                      <TH className="text-right">{t.colDisposed}</TH>
                      <TH className="text-right">{t.colUnclassified}</TH>
                      <SortableTH
                        sortKey="closingStock"
                        sort={monthlyView.sort}
                        onSort={monthlyView.toggleSort}
                        align="right"
                      >
                        {t.colStock}
                      </SortableTH>
                    </TR>
                  </THead>
                  <TBody>
                    {(isLoading || monthlyView.visible.length === 0) && (
                      <TableFallbackRow
                        columns={8}
                        loading={isLoading}
                        icon={FileSpreadsheet}
                        title={
                          monthlyView.emptiedBySearch
                            ? strings.common.noResults
                            : t.empty.replace("{year}", String(year))
                        }
                        description={canManage ? t.emptyHint.replace("{year}", String(year)) : undefined}
                        action={
                          canManage && (
                            <Button onClick={handleRegenerate} disabled={regenerateMut.isPending}>
                              <RefreshCw
                                className={`mr-2 h-4 w-4 ${regenerateMut.isPending ? "animate-spin" : ""}`}
                              />
                              {t.regenerate}
                            </Button>
                          )
                        }
                      />
                    )}
                    {monthlyView.visible.map((r) => (
                      <TR key={r.id}>
                        <TD>{r.workPointName}</TD>
                        <TD className="whitespace-nowrap">{monthName(r.month)}</TD>
                        <TD>
                          <span className="font-medium text-content">{r.wasteCode}</span>
                          {r.hazardous && (
                            <Badge variant="danger" className="ml-2">
                              {t.hazardous}
                            </Badge>
                          )}
                          {/* Rândul lunar e un agregat, deci nu poate deschide o mișcare — dar
                              poate duce la rândurile care îl compun. Badge-ul devine drumul spre
                              ele, restrâns la exact luna și punctul de lucru ale rândului. */}
                          {r.totalUnclassifiedOut > 0 && (
                            <Tooltip content={t.missingCodeHint}>
                              <Link
                                to={`/evidente?an=${r.year}&luna=${r.month}&punct=${r.workPointId}&vedere=handovers&problema=cod-rd`}
                                className="ml-2 inline-block rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand"
                              >
                                <Badge
                                  variant="danger"
                                  className="underline decoration-dotted underline-offset-2"
                                >
                                  {t.missingCode}
                                </Badge>
                              </Link>
                            </Tooltip>
                          )}
                          {r.awaitingWeighing && (
                            <Tooltip content={t.awaitingWeighingHint}>
                              <Badge variant="warning" className="ml-2">
                                {t.awaitingWeighing}
                              </Badge>
                            </Tooltip>
                          )}
                          <span className="block max-w-xs truncate text-xs text-content-subtle">
                            {r.wasteCodeName}
                          </span>
                        </TD>
                        <TD className="text-right">{kg(r.totalGenerated)}</TD>
                        <TD className="text-right">{kg(r.totalRecovered)}</TD>
                        <TD className="text-right">{kg(r.totalDisposed)}</TD>
                        <TD
                          className={`text-right ${
                            r.totalUnclassifiedOut > 0 ? "font-medium text-red-600" : "text-content-subtle"
                          }`}
                          title={r.totalUnclassifiedOut > 0 ? t.missingCodeHint : undefined}
                        >
                          {kg(r.totalUnclassifiedOut)}
                        </TD>
                        <TD
                          className={`text-right font-medium ${
                            r.closingStock < 0 ? "text-red-600" : "text-content"
                          }`}
                        >
                          {kg(r.closingStock)}
                        </TD>
                      </TR>
                    ))}
                  </TBody>
                </Table>
                <TablePagination view={monthlyView} />
              </div>
            )}

            {/* Ce se încarcă în SIM pe 15 martie, în unitatea pe care o cere actul. Stă lângă
                evidența în kg, nu în locul ei: fișa și declarația rămân în kilograme pe hârtie. */}
            {!isLoading && annualByCode.length > 0 && (
              <div className="mt-8 rounded-lg border border-line bg-surface-muted p-4">
                <h3 className="text-sm font-semibold text-content-strong">
                  {t.tonnesTitle.replace("{year}", String(year))}
                </h3>
                <p className="mt-1 text-xs text-content-muted">{t.tonnesHint}</p>
                <div className="mt-3">
                  <Table stickyHeader>
                    <THead sticky>
                      <TR>
                        <TH>{t.colWasteCode}</TH>
                        <TH className="text-right">{t.colGeneratedTonnes}</TH>
                        <TH className="text-right">{t.colRecoveredTonnes}</TH>
                        <TH className="text-right">{t.colDisposedTonnes}</TH>
                      </TR>
                    </THead>
                    <TBody>
                      {annualByCode.map((r) => (
                        <TR key={r.wasteCode}>
                          <TD>
                            <span className="font-medium text-content">{r.wasteCode}</span>
                            {r.hazardous && (
                              <Badge variant="danger" className="ml-2">
                                {t.hazardous}
                              </Badge>
                            )}
                            <span className="block max-w-xs truncate text-xs text-content-subtle">
                              {r.wasteCodeName}
                            </span>
                          </TD>
                          <TD className="whitespace-nowrap text-right">{formatTonnes(r.generated)}</TD>
                          <TD className="whitespace-nowrap text-right">{formatTonnes(r.recovered)}</TD>
                          <TD className="whitespace-nowrap text-right">{formatTonnes(r.disposed)}</TD>
                        </TR>
                      ))}
                    </TBody>
                  </Table>
                </div>
              </div>
            )}
          </>
        )}
      </section>
    </div>
  );
}
