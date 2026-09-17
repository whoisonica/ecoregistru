import { useMemo, useState, type ReactNode } from "react";
import { Download, FileSpreadsheet, FileText, RefreshCw } from "lucide-react";
import { Link } from "react-router-dom";
import {
  downloadAnexa1Form,
  downloadAnnualDeclaration,
  downloadEvidenceExport,
  useEvidences,
  useRegenerateEvidence,
} from "@/hooks/useEvidences";
import { useCanWrite } from "@/hooks/useBillingAccess";
import { AwaitingWeighingDialog } from "@/components/AwaitingWeighingDialog";
import { apiBlobErrorMessage, apiErrorMessage } from "@/lib/api";
import type { EvidenceFilters } from "@/lib/types";
import { byCode } from "@/lib/annualTotals";
import { strings } from "@/lib/strings";
import { withCount } from "@/lib/utils";
import { Badge } from "@/components/ui/badge";
import { BinSwatch } from "@/components/ui/bin-swatch";
import { Button } from "@/components/ui/button";
import { Menu, MenuItem } from "@/components/ui/menu";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { EmptyState } from "@/components/ui/empty-state";
import { useToast } from "@/components/ui/toast";

const t = strings.evidences;

/** Cantitățile vin în kilograme; se scriu cu locale-ul românesc, fără unitate pe fiecare celulă. */
const kgFormat = new Intl.NumberFormat("ro-RO", { maximumFractionDigits: 3 });
const kg = (value: number) => kgFormat.format(value);

/**
 * Totalul anului pe cod de deșeu — cifrele care se tastează în SIM pe 15 martie.
 *
 * <p>Tabul al doilea al ecranului „Generare”, din 18.09.2026. Până atunci, tabelul ăsta stătea în
 * josul unui ecran propriu („Evidențe”), sub vederea lunară, deci pe vederea implicită nu se vedea
 * deloc. Ecranul acela a fost scos: agrega exact mișcările registrului `ANEXA_1`, adică exact
 * rândurile listate aici, la un clic distanță.
 *
 * <p>Nu socotește nimic nou și nu cere niciun endpoint nou: aceleași linii de evidență
 * (`GET /api/v1/evidences`) pe care le citesc deja Acasă, Termene și Dosarul de control.
 */
export function AnnualTotals({
  year,
  workPointId,
}: {
  year: number;
  workPointId?: string;
}) {
  const canManage = useCanWrite();
  const { notify } = useToast();
  const filters: EvidenceFilters = useMemo(
    () => (workPointId ? { year, workPointId } : { year }),
    [year, workPointId]
  );
  const { data: rows, isLoading, isError } = useEvidences(filters);
  const regenerateMut = useRegenerateEvidence();

  /**
   * Ce document se pregătește acum. Patru valori, nu trei: fișa și rezumatul PDF ar fi împărțit
   * altfel aceeași rotiță, iar apăsarea pe al doilea ar fi învârtit-o pe primul — adică pe
   * documentul oficial, exact confuzia pe care cele două butoane există ca s-o evite.
   */
  const [exporting, setExporting] = useState<"anexa1" | "declaration" | "xlsx" | "pdf" | null>(null);
  const [pendingDoc, setPendingDoc] = useState<null | "anexa1" | "declaration">(null);

  const lines = rows ?? [];
  const pendingWeighing = useMemo(() => lines.filter((r) => r.awaitingWeighing), [lines]);
  const missingCodeKg = useMemo(
    () => lines.reduce((sum, r) => sum + r.totalUnclassifiedOut, 0),
    [lines]
  );

  const codes = useMemo(() => byCode(lines), [lines]);
  const totals = useMemo(
    () =>
      codes.reduce(
        (acc, c) => ({
          generated: acc.generated + c.generated,
          recovered: acc.recovered + c.recovered,
          disposed: acc.disposed + c.disposed,
          stock: acc.stock + c.stock,
        }),
        { generated: 0, recovered: 0, disposed: 0, stock: 0 }
      ),
    [codes]
  );

  /** Ecranul pe care se repară rândul: mișcările anului, restrânse la ce n-are cod R/D. */
  const missingCodeHref = `/generare?luna=${year}&problema=cod-rd${
    workPointId ? `&punct=${workPointId}` : ""
  }`;
  const awaitingHref = `/generare?luna=${year}${workPointId ? `&punct=${workPointId}` : ""}`;

  async function runDownload(
    which: "anexa1" | "declaration" | "xlsx" | "pdf",
    run: () => Promise<void>,
    fallback: string
  ) {
    setExporting(which);
    try {
      await run();
    } catch (err) {
      notify(await apiBlobErrorMessage(err, fallback), "error");
    } finally {
      setExporting(null);
    }
  }

  /** Fișa și centralizata sunt anuale: se cer pe an și pe punctul de lucru, niciodată pe lună. */
  function official(which: "anexa1" | "declaration") {
    return which === "anexa1"
      ? runDownload("anexa1", () => downloadAnexa1Form(filters), t.anexa1Error)
      : runDownload("declaration", () => downloadAnnualDeclaration(filters), t.annualDeclarationError);
  }

  function handleOfficial(which: "anexa1" | "declaration") {
    // Spus înainte de clic: altfel butonul deschide un dialog în locul fișierului și pare stricat.
    if (pendingWeighing.length > 0) {
      setPendingDoc(which);
      return;
    }
    void official(which);
  }

  function handleRegenerate() {
    regenerateMut.mutate(year, {
      onSuccess: (res) =>
        notify(
          // Stocul se reportează, deci o regenerare rescrie și anii de după — se spune, altfel
          // numărul de linii pare greșit pentru anul cerut. Zero linii are șir propriu: nu e o
          // eroare, dar „0 de linii" se citește ca una.
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

  if (isError) return <p className="mt-6 text-sm text-state-bad-text">{t.loadError}</p>;

  return (
    <section className="mt-5">
      {pendingDoc && (
        <AwaitingWeighingDialog
          documentName={pendingDoc === "anexa1" ? t.anexa1 : t.annualDeclaration}
          lines={pendingWeighing}
          onCancel={() => setPendingDoc(null)}
          onConfirm={() => {
            const doc = pendingDoc;
            setPendingDoc(null);
            void official(doc);
          }}
        />
      )}

      <p className="max-w-[70ch] text-sm text-content-muted">
        {t.annualIntro.replace("{year}", String(year))}
      </p>

      {/* Pe telefon, câte un card pe cod: tabelul are șase coloane, iar pe 375px se vedea „GENERAT"
          tăiat în două. Aceeași soluție ca pe Termene, din 16.09.2026. */}
      <div className="mt-4 sm:hidden">
        {!isLoading && codes.length === 0 && (
          <EmptyState
            icon={FileSpreadsheet}
            title={t.empty.replace("{year}", String(year))}
            description={t.annualEmptyHint}
          />
        )}
      </div>
      <ul className="mt-4 divide-y divide-line border-y border-line sm:hidden" data-testid="annual-cards">
        {codes.map((c) => (
          <li key={c.wasteCode} className="py-3">
            <div className="flex items-start justify-between gap-3">
              <span className="min-w-0 font-medium text-content">
                <BinSwatch code={c.wasteCode} hazardous={c.hazardous} />
                {c.wasteCode}
                <span className="block truncate text-xs font-normal text-content-subtle">
                  {c.wasteCodeName}
                </span>
              </span>
              {c.unclassified > 0 ? (
                <Link to={missingCodeHref}>
                  <Badge variant="danger">{t.stateMissingCode.replace("{kg}", kg(c.unclassified))}</Badge>
                </Link>
              ) : c.awaiting > 0 ? (
                <Badge variant="warning">{withCount(t.stateAwaiting, c.awaiting, "linie", "linii")}</Badge>
              ) : (
                <Badge variant="success">{t.stateReady}</Badge>
              )}
            </div>
            <dl className="mt-2 flex flex-wrap gap-x-5 gap-y-1 text-xs text-content-muted">
              <div>
                <dt className="eyebrow">{t.colGenerated}</dt>
                <dd className="font-mono text-sm text-content">{kg(c.generated)}</dd>
              </div>
              <div>
                <dt className="eyebrow">{t.colRecovered}</dt>
                <dd className="font-mono text-sm text-content">{kg(c.recovered)}</dd>
              </div>
              <div>
                <dt className="eyebrow">{t.colDisposed}</dt>
                <dd className="font-mono text-sm text-content">{kg(c.disposed)}</dd>
              </div>
              <div>
                <dt className="eyebrow">{t.colStock}</dt>
                <dd className="font-mono text-sm text-content">{kg(c.stock)}</dd>
              </div>
            </dl>
          </li>
        ))}
      </ul>

      <div className="mt-4 hidden sm:block">
        <Table stickyHeader>
          <THead sticky>
            <TR>
              <TH>{t.colWasteCode}</TH>
              <TH className="text-right">{t.colGenerated}</TH>
              <TH className="text-right">{t.colRecovered}</TH>
              <TH className="text-right">{t.colDisposed}</TH>
              <TH className="text-right">{t.colStock}</TH>
              <TH>{t.colState}</TH>
            </TR>
          </THead>
          <TBody>
            {(isLoading || codes.length === 0) && (
              <TableFallbackRow
                columns={6}
                loading={isLoading}
                icon={FileSpreadsheet}
                title={t.empty.replace("{year}", String(year))}
                description={t.annualEmptyHint}
              />
            )}
            {codes.map((c) => (
              <TR key={c.wasteCode}>
                <TD>
                  <span className="font-medium text-content">
                    <BinSwatch code={c.wasteCode} hazardous={c.hazardous} />
                    {c.wasteCode}
                  </span>
                  <span className="block max-w-xs truncate text-xs text-content-subtle">
                    {c.wasteCodeName}
                  </span>
                </TD>
                <TD className="text-right">{kg(c.generated)}</TD>
                <TD className="text-right">{kg(c.recovered)}</TD>
                <TD className="text-right">{kg(c.disposed)}</TD>
                <TD className={`text-right ${c.stock < 0 ? "text-state-bad-text" : ""}`}>
                  {kg(c.stock)}
                </TD>
                <TD>
                  {/* Roșu = nu se poate depune așa; galben = o așteptare legitimă (cântarul). */}
                  {c.unclassified > 0 ? (
                    <Link to={missingCodeHref} className="rounded focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand">
                      <Badge variant="danger" className="underline decoration-dotted underline-offset-2">
                        {t.stateMissingCode.replace("{kg}", kg(c.unclassified))}
                      </Badge>
                    </Link>
                  ) : c.awaiting > 0 ? (
                    <Badge variant="warning">
                      {withCount(t.stateAwaiting, c.awaiting, "linie", "linii")}
                    </Badge>
                  ) : (
                    <Badge variant="success">{t.stateReady}</Badge>
                  )}
                </TD>
              </TR>
            ))}
            {codes.length > 0 && (
              <TR className="border-t-2 border-content font-semibold">
                <TD>{withCount(t.annualTotalRow, codes.length, "cod", "coduri")}</TD>
                <TD className="text-right">{kg(totals.generated)}</TD>
                <TD className="text-right">{kg(totals.recovered)}</TD>
                <TD className="text-right">{kg(totals.disposed)}</TD>
                <TD className="text-right">{kg(totals.stock)}</TD>
                <TD />
              </TR>
            )}
          </TBody>
        </Table>
      </div>

      {(missingCodeKg > 0 || pendingWeighing.length > 0) && (
        <div data-testid="pending-weighing-note" className="mt-4 flex flex-wrap items-center gap-x-6 gap-y-2 text-sm">
          {missingCodeKg > 0 && (
            <span className="flex items-center gap-2">
              <Badge variant="danger">{t.blockerMissingCode.replace("{kg}", kg(missingCodeKg))}</Badge>
              <Link to={missingCodeHref} className="font-medium text-brand-700 underline">
                {t.fixMissingCode}
              </Link>
            </span>
          )}
          {pendingWeighing.length > 0 && (
            <span className="flex items-center gap-2">
              <Badge variant="warning">
                {withCount(t.blockerAwaiting, pendingWeighing.length, "linie", "linii")}
              </Badge>
              <Link to={awaitingHref} className="font-medium text-brand-700 underline">
                {t.pendingWeighingShow}
              </Link>
            </span>
          )}
        </div>
      )}

      {/* Documentele, lângă cifrele din care ies. Aceleași două formulare se iau și din Dosarul de
          control, care le adună pe toate — un buton nu e un ecran, iar Termene îl are deja pe primul
          pe termenul de 15 martie. */}
      <div className="mt-6 divide-y divide-line rounded-lg border border-line">
        <DocRow
          title={t.anexa1}
          hint={t.anexa1Hint}
          action={
            <Button
              variant="outline"
              onClick={() => handleOfficial("anexa1")}
              aria-label={t.anexa1}
              disabled={codes.length === 0 || exporting !== null}
              loading={exporting === "anexa1"}
            >
              {exporting !== "anexa1" && <FileText className="mr-2 h-4 w-4" />}
              {t.download}
            </Button>
          }
        />
        <DocRow
          title={t.annualDeclaration}
          hint={t.annualDeclarationHint}
          action={
            <Button
              variant="outline"
              onClick={() => handleOfficial("declaration")}
              aria-label={t.annualDeclaration}
              disabled={codes.length === 0 || exporting !== null}
              loading={exporting === "declaration"}
            >
              {exporting !== "declaration" && <FileText className="mr-2 h-4 w-4" />}
              {t.download}
            </Button>
          }
        />
        <div className="flex flex-wrap items-center justify-between gap-3 bg-surface-muted px-4 py-3">
          <p className="text-xs text-content-muted">{t.exportsHint}</p>
          <Menu label={t.exportsMenu} align="right" disabled={codes.length === 0 || exporting !== null}>
            <MenuItem
              icon={Download}
              onClick={() => void runDownload("xlsx", () => downloadEvidenceExport(filters, "xlsx"), t.exportError)}
            >
              {t.exportExcel}
            </MenuItem>
            <MenuItem
              icon={Download}
              onClick={() => void runDownload("pdf", () => downloadEvidenceExport(filters, "pdf"), t.exportError)}
            >
              {t.exportPdf}
            </MenuItem>
          </Menu>
        </div>
      </div>

      {canManage && (
        <p className="mt-4 text-xs text-content-muted">
          {t.staleNote}{" "}
          <button
            type="button"
            onClick={handleRegenerate}
            disabled={regenerateMut.isPending}
            className="inline-flex items-center gap-1 font-medium text-brand-700 underline disabled:opacity-60"
          >
            <RefreshCw className={`h-3 w-3 ${regenerateMut.isPending ? "animate-spin" : ""}`} />
            {regenerateMut.isPending ? t.regenerating : t.regenerateNow}
          </button>
        </p>
      )}
    </section>
  );
}

function DocRow({ title, hint, action }: { title: string; hint: string; action: ReactNode }) {
  return (
    <div className="flex flex-wrap items-center gap-3 px-4 py-3">
      <div className="min-w-0 flex-1 basis-80">
        <p className="text-sm font-medium text-content-strong">{title}</p>
        <p className="text-xs text-content-muted">{hint}</p>
      </div>
      {action}
    </div>
  );
}
