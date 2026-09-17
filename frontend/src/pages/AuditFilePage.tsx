import { useMemo, useState } from "react";
import {
  Download,
  FileCheck2,
  ShieldCheck,
  Paperclip,
} from "lucide-react";
import { downloadAuditFile, useAuditFileSize, type AuditFileSize } from "@/hooks/useAuditFile";
import { useEvidences } from "@/hooks/useEvidences";
import { AwaitingWeighingDialog } from "@/components/AwaitingWeighingDialog";
import { apiBlobErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { useUrlNumber } from "@/hooks/useUrlState";
import { Button } from "@/components/ui/button";
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

      <Card className="mt-6 max-w-xl p-6">
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
        {size && (
          <p data-testid="audit-file-size" className="mt-1 text-xs text-content-muted">
            {sizeLine(size)}
          </p>
        )}

        <div className="mt-6 border-t border-line pt-4">
          <p className="text-sm font-medium text-content-strong">{t.contents}</p>
          <ul className="mt-3 space-y-2 text-sm text-content-strong">
            <li className="flex items-start gap-2">
              <FileCheck2 className="mt-0.5 h-4 w-4 shrink-0 text-brand" />
              <span className="font-medium text-content-strong">{t.contentAnexa1}</span>
            </li>
            <li className="flex items-start gap-2">
              <FileCheck2 className="mt-0.5 h-4 w-4 shrink-0 text-brand" />
              <span className="font-medium text-content-strong">{t.contentAnnualDeclaration}</span>
            </li>
            <li className="flex items-start gap-2">
              <FileCheck2 className="mt-0.5 h-4 w-4 shrink-0 text-brand" />
              <span className="font-medium text-content-strong">{t.contentPackaging}</span>
            </li>
            <li className="flex items-start gap-2">
              <FileCheck2 className="mt-0.5 h-4 w-4 shrink-0 text-brand" />
              <span className="font-medium text-content-strong">{t.contentPackagingAnexa3}</span>
            </li>
            <li className="flex items-center gap-2">
              <ShieldCheck className="h-4 w-4 text-brand" />
              {t.contentPartners}
            </li>
            <li className="flex items-center gap-2">
              <Paperclip className="h-4 w-4 text-brand" />
              {t.contentAttachments}
            </li>
          </ul>
        </div>
      </Card>

      <p className="mt-4 max-w-xl rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
        {t.note}
      </p>
    </div>
  );
}
