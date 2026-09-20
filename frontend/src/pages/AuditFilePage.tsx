import { useMemo, useState, type ReactNode } from "react";
import { Download } from "lucide-react";
import {
  downloadAuditFile,
  useAuditFileContents,
  useAuditFileSize,
  type AuditFileContents,
  type AuditFileSize,
} from "@/hooks/useAuditFile";
import {
  downloadAnexa1Form,
  downloadAnnualDeclaration,
  downloadEvidenceExport,
  useEvidences,
} from "@/hooks/useEvidences";
import { downloadPackagingAnexa3, downloadPackagingDeclaration } from "@/hooks/usePackaging";
import { Menu, MenuItem } from "@/components/ui/menu";
import { AwaitingWeighingDialog } from "@/components/AwaitingWeighingDialog";
import { apiBlobErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { useUrlNumber } from "@/hooks/useUrlState";
import { countOf } from "@/lib/utils";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Card } from "@/components/ui/card";
import { PageHeader } from "@/components/ui/page-header";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { useToast } from "@/components/ui/toast";

const t = strings.auditFile;
const ev = strings.evidences;

/** Documentele care se iau separat din dosar. Arhiva întreagă are butonul ei, deasupra. */
type DocKey = "sheet" | "centralized" | "packaging" | "anexa3" | "xlsx" | "pdf";

/** „1,4 MB”. Sub 1 MB, în KB: un dosar fără poze nu e „0,0 MB”. */
function formatSize(bytes: number): string {
  const mb = bytes / (1024 * 1024);
  return mb >= 1
    ? `${mb.toLocaleString("ro-RO", { maximumFractionDigits: 1 })} MB`
    : `${Math.max(1, Math.round(bytes / 1024)).toLocaleString("ro-RO")} KB`;
}

function sizeLine(size: AuditFileSize): string {
  if (size.attachments === 0) return t.sizeNone;
  const known = size.attachments - size.unknownSize;
  if (known === 0) return t.sizeAllUnknown.replace("{count}", String(size.attachments));
  const template = size.unknownSize > 0 ? t.sizeUnknown : t.sizeKnown;
  return template
    .replace("{count}", String(size.unknownSize > 0 ? known : size.attachments))
    .replace("{size}", formatSize(size.attachmentBytes))
    .replace("{unknown}", String(size.unknownSize));
}

type RowState = "in" | "empty" | "out" | "missing";

const STATE: Record<RowState, { variant: "success" | "warning" | "muted" | "danger"; label: string }> = {
  in: { variant: "success", label: t.stateIn },
  empty: { variant: "warning", label: t.stateEmpty },
  out: { variant: "muted", label: t.stateOut },
  missing: { variant: "danger", label: t.stateMissing },
};

/**
 * Un document al anului: starea lui ca LED, numele, de ce intră sau nu — și, din 18.09.2026,
 * butonul care îl ia separat.
 *
 * <p>`action` lipsește pentru ce există numai înăuntrul arhivei (autorizațiile partenerilor,
 * atașamentele): acolo rândul spune „în arhivă", nu oferă un buton care n-ar avea ce descărca.
 */
function ContentRow({ state, title, testId, action, zipOnly, children }: {
  state: RowState;
  title: string;
  testId?: string;
  action?: ReactNode;
  /** Documentul se naște numai înăuntrul arhivei: n-are endpoint propriu, deci nici buton. */
  zipOnly?: boolean;
  children: ReactNode;
}) {
  return (
    <li className="grid gap-1 sm:grid-cols-[8.5rem_1fr_auto] sm:gap-3">
      <Badge variant={STATE[state].variant} className="self-start sm:mt-0.5">{STATE[state].label}</Badge>
      {/* Cardul se întinde cât pagina (18.09.2026); fără limită aici, explicațiile ar ajunge
          la ~140 de caractere pe rând, iar butonul ar pluti departe de textul lui. */}
      <div className="min-w-0 max-w-[70ch]">
        <p className="text-sm font-medium text-content-strong">{title}</p>
        <div data-testid={testId} className="mt-0.5 space-y-0.5 text-sm text-content-muted">{children}</div>
      </div>
      {action ? (
        <div className="sm:self-start">{action}</div>
      ) : (
        // „în arhivă" numai unde e adevărat: pe un document care nu se aplică anului, ar fi spus
        // exact pe dos față de eticheta „Nu intră" de lângă el.
        <span className="text-xs text-content-subtle sm:self-start sm:pt-1">
          {zipOnly ? t.rowInZip : t.rowNothing}
        </span>
      )}
    </li>
  );
}

const movements = (n: number) => countOf(n, t.movementOne, t.movementMany);

/** Fișa și centralizata: au date dacă anul are mișcări; pe mai mulți ani, câte un rând pe an. */
function sheetRow(c: AuditFileContents, title: string, single: string, action?: ReactNode) {
  const empty = c.years.every((y) => y.movements === 0);
  if (c.years.length === 1) {
    const y = c.years[0];
    return (
      <ContentRow state={y.movements > 0 ? "in" : "empty"} title={title} action={action}>
        <p>
          {y.movements > 0
            ? single
                .replace("{count}", movements(y.movements))
                .replace("{year}", String(y.year))
                .replace("{next}", String(y.year + 1))
            : t.noMovements.replace("{year}", String(y.year))}
        </p>
      </ContentRow>
    );
  }
  return (
    <ContentRow state={empty ? "empty" : "in"} title={title} action={action}>
      {c.years.map((y) => (
        <p key={y.year}>
          {y.movements > 0
            ? t.yearMovements.replace("{year}", String(y.year)).replace("{count}", movements(y.movements))
            : t.yearNoMovements.replace("{year}", String(y.year))}
        </p>
      ))}
    </ContentRow>
  );
}

function PackagingRow({ c, action }: { c: AuditFileContents; action?: ReactNode }) {
  const d = c.packagingDeclaration;
  return (
    <ContentRow state={d === "INCLUDED" ? "in" : "out"} title={t.docPackaging} action={d === "INCLUDED" ? action : undefined}>
      <p>{d === "INCLUDED" ? t.packagingIncluded : d === "TRADER_ONLY" ? t.packagingTrader : t.packagingNotAnswered}</p>
    </ContentRow>
  );
}

function Anexa3Row({ c, action }: { c: AuditFileContents; action?: ReactNode }) {
  const withPoints = c.years.filter((y) => y.anexa3WorkPoints.length > 0);
  const missing = c.years.filter((y) => y.anexa3RoleMissing);
  const state: RowState = withPoints.length > 0 ? "in" : missing.length > 0 ? "missing" : "out";
  const single = c.years.length === 1;
  return (
    <ContentRow state={state} title={t.docAnexa3} action={withPoints.length > 0 ? action : undefined}>
      {withPoints.length > 0 &&
        (single ? (
          <p>{t.anexa3Yes.replace("{points}", withPoints[0].anexa3WorkPoints.join(", "))}</p>
        ) : (
          withPoints.map((y) => (
            <p key={y.year}>
              {t.anexa3YesYear.replace("{year}", String(y.year)).replace("{points}", y.anexa3WorkPoints.join(", "))}
            </p>
          ))
        ))}
      {withPoints.length > 0 && <p>{c.anexa3ExitsOnly ? t.anexa3ExitsOnly : t.anexa3Deadline}</p>}
      {missing.map((y) => (
        <p key={`m${y.year}`}>{t.anexa3RoleMissing.replace("{year}", String(y.year))}</p>
      ))}
      {withPoints.length === 0 && missing.length === 0 && (
        <p>{single ? t.anexa3None.replace("{year}", String(c.years[0].year)) : t.anexa3NonePeriod}</p>
      )}
    </ContentRow>
  );
}

function PartnersRow({ c }: { c: AuditFileContents }) {
  if (c.partners === 0) {
    return (
      <ContentRow state="empty" title={t.docPartners} zipOnly>
        <p>{t.partnersNone}</p>
      </ContentRow>
    );
  }
  return (
    <ContentRow state="in" title={t.docPartners} zipOnly>
      <p>{t.partnersYes.replace("{count}", countOf(c.partners, t.partnerOne, t.partnerMany))}</p>
      {c.partnersExpired > 0 && (
        <p className="text-state-bad-text">
          {t.partnersExpired.replace("{count}", countOf(c.partnersExpired, t.expiredOne, t.expiredMany))}
        </p>
      )}
      {c.partnersExpiringSoon > 0 && (
        <p>{t.partnersSoon.replace("{count}", countOf(c.partnersExpiringSoon, t.expiredOne, t.expiredMany))}</p>
      )}
    </ContentRow>
  );
}

/** Butonul unui rând: același text pe toate, fiindcă numele documentului e chiar lângă el. */
function DocButton({ label, loading, disabled, onClick }: {
  label: string;
  loading: boolean;
  disabled: boolean;
  onClick: () => void;
}) {
  return (
    <Button variant="outline" size="sm" loading={loading} disabled={disabled} onClick={onClick} aria-label={label}>
      {!loading && <Download className="mr-1.5 h-3.5 w-3.5" />}
      {t.rowDownload}
    </Button>
  );
}

/** Ambalajele ies în două formate: .xls pentru portal, PDF pentru dosar. */
function FormatMenu({ disabled, onPick }: { disabled: boolean; onPick: (format: "xls" | "pdf") => void }) {
  return (
    <Menu label={t.rowDownload} align="right" disabled={disabled}>
      <MenuItem icon={Download} onClick={() => onPick("xls")}>
        .xls
      </MenuItem>
      <MenuItem icon={Download} onClick={() => onPick("pdf")}>
        PDF
      </MenuItem>
    </Menu>
  );
}

/** Year options: current year down to five years back. */
function yearOptions(): number[] {
  const now = new Date().getFullYear();
  return Array.from({ length: 6 }, (_, i) => now - i);
}

export function AuditFilePage() {
  const [year, setYear] = useUrlNumber("an", new Date().getFullYear());
  const [years, setYears] = useUrlNumber("ani", 1);
  const [downloading, setDownloading] = useState(false);
  const { data: size } = useAuditFileSize(year, years);
  const contents = useAuditFileContents(year, years);
  const { notify } = useToast();

  /**
   * The dossier carries both official documents, so it gets the same warning as the buttons that
   * print them — scoped to the years it actually contains, and to nothing else.
   *
   * <p>Five hooks with a fixed order because that is what the rules require, and only the ones
   * inside the chosen range are enabled: picking "doar anul ales" fetches one year, not five.
   */
  const y0 = useEvidences({ year }, years > 0);
  const y1 = useEvidences({ year: year - 1 }, years > 1);
  const y2 = useEvidences({ year: year - 2 }, years > 2);
  const y3 = useEvidences({ year: year - 3 }, years > 3);
  const y4 = useEvidences({ year: year - 4 }, years > 4);
  const pendingWeighing = useMemo(
    () =>
      [y0.data, y1.data, y2.data, y3.data, y4.data]
        .flatMap((rows) => rows ?? [])
        .filter((r) => r.awaitingWeighing),
    [y0.data, y1.data, y2.data, y3.data, y4.data]
  );
  /**
   * Un an care n-a putut fi citit nu e un an fără linii necântărite.
   *
   * <p>Până pe 20.09.2026 se citea numai `.data`: la un 500 sau la rețea căzută, lista ieșea goală,
   * gărzile de mai jos săreau peste avertisment, iar fișa, evidența centralizată și arhiva plecau
   * **fără** el — tăcut, pe documente care se duc la control. Aici nu se ghicește: dacă n-am putut
   * verifica, nu se descarcă. Cererea se reia singură la următoarea apăsare (`refetch`).
   */
  const weighingUnknown = [y0, y1, y2, y3, y4].some((q) => q.isError);
  const recheckWeighing = () => {
    for (const q of [y0, y1, y2, y3, y4]) {
      if (q.isError) void q.refetch();
    }
  };
  const [confirming, setConfirming] = useState(false);
  /**
   * Documentul care se pregătește acum, sau cel care așteaptă confirmarea „sunt linii necântărite".
   * Câte o valoare pentru fiecare rând: două butoane care împart aceeași rotiță ar învârti-o pe
   * documentul greșit.
   */
  const [doc, setDoc] = useState<DocKey | null>(null);
  const [askingFor, setAskingFor] = useState<"sheet" | "centralized" | null>(null);

  async function runDoc(key: DocKey, run: () => Promise<void>, fallback: string) {
    setDoc(key);
    try {
      await run();
    } catch (err) {
      notify(await apiBlobErrorMessage(err, fallback), "error");
    } finally {
      setDoc(null);
    }
  }

  /** Fișa și centralizata poartă rubrici goale cât timp o ieșire n-a fost cântărită: se spune întâi. */
  function officialDoc(key: "sheet" | "centralized") {
    const run = () =>
      key === "sheet"
        ? downloadAnexa1Form({ year })
        : downloadAnnualDeclaration({ year });
    const fallback = key === "sheet" ? ev.anexa1Error : ev.annualDeclarationError;
    return runDoc(key, run, fallback);
  }

  function askOfficial(key: "sheet" | "centralized") {
    if (weighingUnknown) {
      notify(t.weighingCheckFailed, "error");
      recheckWeighing();
      return;
    }
    if (pendingWeighing.length > 0) {
      setAskingFor(key);
      return;
    }
    void officialDoc(key);
  }

  async function handleDownload() {
    if (weighingUnknown) {
      notify(t.weighingCheckFailed, "error");
      recheckWeighing();
      return;
    }
    if (pendingWeighing.length > 0) {
      setConfirming(true);
      return;
    }
    await download();
  }

  async function download() {
    setDownloading(true);
    try {
      await downloadAuditFile(year, years);
    } catch (err) {
      notify(await apiBlobErrorMessage(err, t.downloadError), "error");
    } finally {
      setDownloading(false);
    }
  }

  return (
    <div>
      {confirming && (
        <AwaitingWeighingDialog
          documentName={t.title}
          lines={pendingWeighing}
          onCancel={() => setConfirming(false)}
          onConfirm={() => {
            setConfirming(false);
            void download();
          }}
        />
      )}

      {askingFor && (
        <AwaitingWeighingDialog
          documentName={askingFor === "sheet" ? ev.anexa1 : ev.annualDeclaration}
          lines={pendingWeighing}
          onCancel={() => setAskingFor(null)}
          onConfirm={() => {
            const key = askingFor;
            setAskingFor(null);
            void officialDoc(key);
          }}
        />
      )}
      <PageHeader title={t.title} description={t.subtitle} />

      <Card className="mt-6 p-6">
        <div className="grid gap-3 sm:flex sm:flex-wrap sm:items-end">
          <div>
            <Label htmlFor="af-year">{t.filterYear}</Label>
            <Select
              id="af-year"
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
            <Label htmlFor="af-years">{t.filterYears}</Label>
            <Select
              id="af-years"
              value={String(years)}
              onChange={(ev) => setYears(Number(ev.target.value))}
              className="w-full sm:w-64"
            >
              <option value="1">{t.yearsOne}</option>
              <option value="2">{t.yearsTwo}</option>
              <option value="3">{t.yearsThree}</option>
              <option value="4">{t.yearsFour}</option>
              <option value="5">{t.yearsFive}</option>
            </Select>
          </div>
          <Button
            onClick={handleDownload}
            loading={downloading}
            className="shrink-0 whitespace-nowrap"
          >
            {!downloading && <Download className="mr-2 h-4 w-4" />}
            {downloading ? t.downloading : t.download}
          </Button>
        </div>

        <p className="mt-2 text-xs text-content-muted">{t.yearsHint}</p>

        <div className="mt-6 border-t border-line pt-4">
          <p className="text-sm font-medium text-content-strong">{t.contents}</p>
          <p className="mt-0.5 text-xs text-content-muted">{t.contentsHint}</p>
          {contents.isError ? (
            <p className="mt-3 text-sm text-content-muted">{t.contentsError}</p>
          ) : !contents.data ? (
            <div className="mt-3 space-y-3">
              {[0, 1, 2, 3].map((k) => (
                <Skeleton key={k} className="h-10 w-full" />
              ))}
            </div>
          ) : (
            <ul className="mt-3 space-y-4" data-testid="audit-file-contents">
              {sheetRow(
                contents.data,
                t.docSheet,
                t.sheetYes,
                <DocButton label={t.docSheet} loading={doc === "sheet"} disabled={doc !== null} onClick={() => askOfficial("sheet")} />
              )}
              {sheetRow(
                contents.data,
                t.docCentralized,
                t.centralizedYes,
                <DocButton
                  label={t.docCentralized}
                  loading={doc === "centralized"}
                  disabled={doc !== null}
                  onClick={() => askOfficial("centralized")}
                />
              )}
              <PackagingRow
                c={contents.data}
                action={
                  <FormatMenu
                    disabled={doc !== null}
                    onPick={(format) =>
                      void runDoc("packaging", () => downloadPackagingDeclaration(year, format), t.downloadError)
                    }
                  />
                }
              />
              <Anexa3Row
                c={contents.data}
                action={
                  <FormatMenu
                    disabled={doc !== null}
                    onPick={(format) =>
                      void runDoc("anexa3", () => downloadPackagingAnexa3(year, undefined, format), t.downloadError)
                    }
                  />
                }
              />
              <PartnersRow c={contents.data} />
              <ContentRow
                state={size && size.attachments === 0 ? "out" : "in"}
                title={t.docAttachments}
                zipOnly
                testId="audit-file-size"
              >
                <p>{size ? sizeLine(size) : "?"}</p>
              </ContentRow>
            </ul>
          )}
          <p className="mt-4 border-t border-line pt-3 text-xs text-content-muted">
            {t.workExports}{" "}
            <button
              type="button"
              disabled={doc !== null}
              onClick={() => void runDoc("xlsx", () => downloadEvidenceExport({ year }, "xlsx"), ev.exportError)}
              className="font-medium text-brand-700 underline disabled:opacity-60"
            >
              {ev.exportExcel}
            </button>{" "}
            ·{" "}
            <button
              type="button"
              disabled={doc !== null}
              onClick={() => void runDoc("pdf", () => downloadEvidenceExport({ year }, "pdf"), ev.exportError)}
              className="font-medium text-brand-700 underline disabled:opacity-60"
            >
              {ev.exportPdf}
            </button>
          </p>
        </div>
      </Card>

      <p className="mt-4 rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
        {t.note}
      </p>
    </div>
  );
}
