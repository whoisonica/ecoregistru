import { useMemo, useState, type ReactNode } from "react";
import { Download } from "lucide-react";
import {
  downloadAuditFile,
  useAuditFileContents,
  useAuditFileSize,
  type AuditFileContents,
  type AuditFileSize,
} from "@/hooks/useAuditFile";
import { useEvidences } from "@/hooks/useEvidences";
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

/** Un document din arhivă: starea lui ca LED, numele și de ce intră sau nu. */
function ContentRow({ state, title, testId, children }: {
  state: RowState;
  title: string;
  testId?: string;
  children: ReactNode;
}) {
  return (
    <li className="grid gap-1 sm:grid-cols-[8.5rem_1fr] sm:gap-3">
      <Badge variant={STATE[state].variant} className="self-start sm:mt-0.5">{STATE[state].label}</Badge>
      <div className="min-w-0">
        <p className="text-sm font-medium text-content-strong">{title}</p>
        <div data-testid={testId} className="mt-0.5 space-y-0.5 text-sm text-content-muted">{children}</div>
      </div>
    </li>
  );
}

const movements = (n: number) => countOf(n, t.movementOne, t.movementMany);

/** Fișa și centralizata: au date dacă anul are mișcări; pe mai mulți ani, câte un rând pe an. */
function sheetRow(c: AuditFileContents, title: string, single: string) {
  const empty = c.years.every((y) => y.movements === 0);
  if (c.years.length === 1) {
    const y = c.years[0];
    return (
      <ContentRow state={y.movements > 0 ? "in" : "empty"} title={title}>
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
    <ContentRow state={empty ? "empty" : "in"} title={title}>
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

function PackagingRow({ c }: { c: AuditFileContents }) {
  const d = c.packagingDeclaration;
  return (
    <ContentRow state={d === "INCLUDED" ? "in" : "out"} title={t.docPackaging}>
      <p>{d === "INCLUDED" ? t.packagingIncluded : d === "TRADER_ONLY" ? t.packagingTrader : t.packagingNotAnswered}</p>
    </ContentRow>
  );
}

function Anexa3Row({ c }: { c: AuditFileContents }) {
  const withPoints = c.years.filter((y) => y.anexa3WorkPoints.length > 0);
  const missing = c.years.filter((y) => y.anexa3RoleMissing);
  const state: RowState = withPoints.length > 0 ? "in" : missing.length > 0 ? "missing" : "out";
  const single = c.years.length === 1;
  return (
    <ContentRow state={state} title={t.docAnexa3}>
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
      <ContentRow state="empty" title={t.docPartners}>
        <p>{t.partnersNone}</p>
      </ContentRow>
    );
  }
  return (
    <ContentRow state="in" title={t.docPartners}>
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
  const [confirming, setConfirming] = useState(false);

  async function handleDownload() {
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
      <PageHeader title={t.title} description={t.subtitle} />

      <Card className="mt-6 max-w-2xl p-6">
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
              {sheetRow(contents.data, t.docSheet, t.sheetYes)}
              {sheetRow(contents.data, t.docCentralized, t.centralizedYes)}
              <PackagingRow c={contents.data} />
              <Anexa3Row c={contents.data} />
              <PartnersRow c={contents.data} />
              <ContentRow
                state={size && size.attachments === 0 ? "out" : "in"}
                title={t.docAttachments}
                testId="audit-file-size"
              >
                <p>{size ? sizeLine(size) : "?"}</p>
              </ContentRow>
            </ul>
          )}
        </div>
      </Card>

      <p className="mt-4 max-w-2xl rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
        {t.note}
      </p>
    </div>
  );
}
