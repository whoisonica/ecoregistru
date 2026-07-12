import { useMemo, useState } from "react";
import { Download, RefreshCw } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { useWorkPoints } from "@/hooks/useWorkPoints";
import {
  downloadEvidenceExport,
  useEvidences,
  useRegenerateEvidence,
} from "@/hooks/useEvidences";
import type { EvidenceFilters } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Select } from "@/components/ui/select";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { useToast } from "@/components/ui/toast";

const t = strings.evidences;
const op = strings.enums.wasteOperation;

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

  const filters: EvidenceFilters = useMemo(() => {
    const f: EvidenceFilters = { year };
    if (month) f.month = Number(month);
    if (workPointId) f.workPointId = workPointId;
    return f;
  }, [year, month, workPointId]);

  const { data: evidences, isLoading, isError } = useEvidences(filters);
  const regenerateMut = useRegenerateEvidence();
  const { notify } = useToast();
  const [exporting, setExporting] = useState<"xlsx" | "pdf" | null>(null);

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
          t.regenerated
            .replace("{count}", String(res.linesGenerated))
            .replace("{year}", String(res.year)),
          "success"
        ),
      onError: (err) => notify(apiErrorMessage(err, t.regenerateError), "error"),
    });
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
      <div className="mt-6 flex flex-wrap items-end gap-3">
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
                  <TH className="text-right">{op.GENERATED}</TH>
                  <TH className="text-right">{op.COLLECTED}</TH>
                  <TH className="text-right">{op.HANDED_OVER}</TH>
                  <TH className="text-right">{op.RECOVERED}</TH>
                  <TH className="text-right">{op.DISPOSED}</TH>
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
                      <span className="block max-w-xs truncate text-xs text-gray-400">
                        {r.wasteCodeName}
                      </span>
                    </TD>
                    <TD className="text-right">{kg(r.totalGenerated)}</TD>
                    <TD className="text-right">{kg(r.totalCollected)}</TD>
                    <TD className="text-right">{kg(r.totalHandedOver)}</TD>
                    <TD className="text-right">{kg(r.totalRecovered)}</TD>
                    <TD className="text-right">{kg(r.totalDisposed)}</TD>
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
      </section>
    </div>
  );
}
