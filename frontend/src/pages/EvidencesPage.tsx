import { useMemo, useState } from "react";
import { Download, FileText, RefreshCw } from "lucide-react";
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
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Select } from "@/components/ui/select";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
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

  const [year, setYear] = useState(() => new Date().getFullYear());
  const [month, setMonth] = useState(""); // "" = all months
  const [workPointId, setWorkPointId] = useState(""); // "" = all work points
  /**
   * Two views of the same period. "Predări" is the default because it is what the meeting asked
   * the tab to show — quantity, handover date, partner, R/D code, and that is it. The monthly
   * Anexa 1 aggregate stays one click away: the running stock is the one figure that cannot be
   * reconstructed by reading the rows, and it is the one the form is built around.
   */
  const [view, setView] = useState<"handovers" | "monthly">("handovers");

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
  const [exporting, setExporting] = useState<"xlsx" | "pdf" | "declaration" | null>(null);

  // Stable display order: work point, then month, then waste code.
  const rows = useMemo(() => {
    return [...(evidences ?? [])].sort(
      (a, b) =>
        a.workPointName.localeCompare(b.workPointName, "ro") ||
        a.month - b.month ||
        a.wasteCode.localeCompare(b.wasteCode, "ro")
    );
  }, [evidences]);

  function handleRegenerate() {
    regenerateMut.mutate(year, {
      onSuccess: (res) =>
        notify(
          // Stock carries across years, so a regeneration rebuilds the later ones too — say so,
          // otherwise the line count looks wrong for the year that was asked for.
          (res.cascadedYears.length > 0 ? t.regeneratedCascade : t.regenerated)
            .replace("{count}", String(res.linesGenerated))
            .replace("{year}", String(res.year))
            .replace("{years}", res.cascadedYears.join(", ")),
          "success"
        ),
      onError: (err) => notify(apiErrorMessage(err, t.regenerateError), "error"),
    });
  }

  async function handleAnexa1() {
    setExporting("pdf");
    try {
      await downloadAnexa1Form(filters);
    } catch (err) {
      notify(apiErrorMessage(err, t.anexa1Error), "error");
    } finally {
      setExporting(null);
    }
  }

  async function handleAnnualDeclaration() {
    setExporting("declaration");
    try {
      await downloadAnnualDeclaration(filters);
    } catch (err) {
      notify(apiErrorMessage(err, t.annualDeclarationError), "error");
    } finally {
      setExporting(null);
    }
  }

  async function handleExport(format: "xlsx" | "pdf") {
    setExporting(format);
    try {
      await downloadEvidenceExport(filters, format);
    } catch (err) {
      notify(apiErrorMessage(err, t.exportError), "error");
    } finally {
      setExporting(null);
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">{t.title}</h1>
          <p className="mt-1 text-sm text-gray-500">{t.subtitle}</p>
        </div>
        <div className="flex items-center gap-2">
          {/* The official form first: it is the one the client actually files. */}
          <Button
            onClick={handleAnexa1}
            disabled={rows.length === 0 || exporting !== null}
            title={t.anexa1Hint}
          >
            <FileText className="mr-2 h-4 w-4" />
            {t.anexa1}
          </Button>
          {/* The summary that goes in front of it, and the page the authority reads first. */}
          <Button
            variant="outline"
            onClick={handleAnnualDeclaration}
            disabled={rows.length === 0 || exporting !== null}
            title={t.annualDeclarationHint}
          >
            <FileText className="mr-2 h-4 w-4" />
            {t.annualDeclaration}
          </Button>
          {/* Export is read-only: available to every tenant member, viewer included. */}
          <Button
            variant="outline"
            onClick={() => handleExport("xlsx")}
            disabled={rows.length === 0 || exporting !== null}
          >
            <Download className="mr-2 h-4 w-4" />
            {t.exportExcel}
          </Button>
          <Button
            variant="outline"
            onClick={() => handleExport("pdf")}
            disabled={rows.length === 0 || exporting !== null}
          >
            <Download className="mr-2 h-4 w-4" />
            {t.exportPdf}
          </Button>
          {canManage && (
            <Button onClick={handleRegenerate} disabled={regenerateMut.isPending}>
              <RefreshCw
                className={`mr-2 h-4 w-4 ${regenerateMut.isPending ? "animate-spin" : ""}`}
              />
              {regenerateMut.isPending ? t.regenerating : t.regenerate}
            </Button>
          )}
        </div>
      </div>

      {canManage && (
        <p className="mt-4 rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
          {t.staleNote}
        </p>
      )}

      {/* Filters */}
      <div className="mt-6 inline-flex rounded-lg border border-gray-200 bg-gray-50 p-0.5">
        {(["handovers", "monthly"] as const).map((v) => (
          <button
            key={v}
            type="button"
            onClick={() => setView(v)}
            className={
              view === v
                ? "rounded-md bg-white px-3 py-1.5 text-sm font-medium text-gray-900 shadow-sm"
                : "rounded-md px-3 py-1.5 text-sm text-gray-500 hover:text-gray-700"
            }
          >
            {v === "handovers" ? t.viewHandovers : t.viewMonthly}
          </button>
        ))}
      </div>

      <div className="mt-4 flex flex-wrap items-end gap-3">
        <div>
          <Label htmlFor="ev-year">{t.filterYear}</Label>
          <Select
            id="ev-year"
            value={String(year)}
            onChange={(ev) => setYear(Number(ev.target.value))}
            className="w-32"
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
            className="w-44"
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
            className="w-56"
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
        {isLoading && <p className="text-sm text-gray-500">{strings.common.loading}</p>}
        {isError && <p className="text-sm text-red-600">{t.loadError}</p>}

        {!isLoading && !isError && rows.length === 0 && (
          <div className="rounded-xl border border-dashed border-gray-300 bg-white p-8 text-center">
            <p className="text-gray-500">{t.empty.replace("{year}", String(year))}</p>
            {canManage && (
              <p className="mt-1 text-sm text-gray-400">
                {t.emptyHint.replace("{year}", String(year))}
              </p>
            )}
          </div>
        )}

        {!isLoading && !isError && rows.length > 0 && (
          <div className="overflow-x-auto">
            <Table>
              <THead>
                <TR>
                  <TH>{t.colWorkPoint}</TH>
                  <TH>{t.colMonth}</TH>
                  <TH>{t.colWasteCode}</TH>
                  <TH className="text-right">{t.colGenerated}</TH>
                  <TH className="text-right">{t.colRecovered}</TH>
                  <TH className="text-right">{t.colDisposed}</TH>
                  <TH className="text-right">{t.colUnclassified}</TH>
                  <TH className="text-right">{t.colStock}</TH>
                </TR>
              </THead>
              <TBody>
                {rows.map((r) => (
                  <TR key={r.id}>
                    <TD>{r.workPointName}</TD>
                    <TD className="whitespace-nowrap">{monthName(r.month)}</TD>
                    <TD>
                      <span className="font-medium text-gray-900">{r.wasteCode}</span>
                      {r.hazardous && (
                        <Badge variant="danger" className="ml-2">
                          {t.hazardous}
                        </Badge>
                      )}
                      {r.totalUnclassifiedOut > 0 && (
                        <Badge variant="danger" className="ml-2" title={t.missingCodeHint}>
                          {t.missingCode}
                        </Badge>
                      )}
                      {r.awaitingWeighing && (
                        <Badge variant="warning" className="ml-2" title={t.awaitingWeighingHint}>
                          {t.awaitingWeighing}
                        </Badge>
                      )}
                      <span className="block max-w-xs truncate text-xs text-gray-400">
                        {r.wasteCodeName}
                      </span>
                    </TD>
                    <TD className="text-right">{kg(r.totalGenerated)}</TD>
                    <TD className="text-right">{kg(r.totalRecovered)}</TD>
                    <TD className="text-right">{kg(r.totalDisposed)}</TD>
                    <TD
                      className={`text-right ${
                        r.totalUnclassifiedOut > 0 ? "font-medium text-red-600" : "text-gray-400"
                      }`}
                      title={r.totalUnclassifiedOut > 0 ? t.missingCodeHint : undefined}
                    >
                      {kg(r.totalUnclassifiedOut)}
                    </TD>
                    <TD
                      className={`text-right font-medium ${
                        r.closingStock < 0 ? "text-red-600" : "text-gray-900"
                      }`}
                    >
                      {kg(r.closingStock)}
                    </TD>
                  </TR>
                ))}
              </TBody>
            </Table>
          </div>
        )}
          </>
        )}
      </section>
    </div>
  );
}
