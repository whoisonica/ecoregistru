import { useState } from "react";
import {
  FolderArchive,
  Download,
  FileSpreadsheet,
  FileCheck2,
  ShieldCheck,
  Paperclip,
} from "lucide-react";
import { downloadAuditFile } from "@/hooks/useAuditFile";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { useToast } from "@/components/ui/toast";

const t = strings.auditFile;

/** Year options: current year down to five years back. */
function yearOptions(): number[] {
  const now = new Date().getFullYear();
  return Array.from({ length: 6 }, (_, i) => now - i);
}

export function AuditFilePage() {
  const [year, setYear] = useState(() => new Date().getFullYear());
  const [years, setYears] = useState(1);
  const [downloading, setDownloading] = useState(false);
  const { notify } = useToast();

  async function handleDownload() {
    setDownloading(true);
    try {
      await downloadAuditFile(year, years);
    } catch (err) {
      notify(apiErrorMessage(err, t.downloadError), "error");
    } finally {
      setDownloading(false);
    }
  }

  return (
    <div>
      <h1 className="text-2xl font-bold">{t.title}</h1>
      <p className="mt-1 text-sm text-gray-500">{t.subtitle}</p>

      <div className="mt-6 max-w-xl rounded-xl border border-gray-200 bg-white p-6">
        <div className="flex flex-wrap items-end gap-3">
          <div>
            <Label htmlFor="af-year">{t.filterYear}</Label>
            <Select
              id="af-year"
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
            <Label htmlFor="af-years">{t.filterYears}</Label>
            <Select
              id="af-years"
              value={String(years)}
              onChange={(ev) => setYears(Number(ev.target.value))}
              className="w-64"
            >
              <option value="1">{t.yearsOne}</option>
              <option value="2">{t.yearsTwo}</option>
              <option value="3">{t.yearsThree}</option>
            </Select>
          </div>
          <Button onClick={handleDownload} disabled={downloading} className="shrink-0 whitespace-nowrap">
            {downloading ? (
              <FolderArchive className="mr-2 h-4 w-4 animate-pulse" />
            ) : (
              <Download className="mr-2 h-4 w-4" />
            )}
            {downloading ? t.downloading : t.download}
          </Button>
        </div>

        <p className="mt-2 text-xs text-gray-500">{t.yearsHint}</p>

        <div className="mt-6 border-t border-gray-100 pt-4">
          <p className="text-sm font-medium text-gray-700">{t.contents}</p>
          <ul className="mt-3 space-y-2 text-sm text-gray-600">
            <li className="flex items-start gap-2">
              <FileCheck2 className="mt-0.5 h-4 w-4 shrink-0 text-brand" />
              <span className="font-medium text-gray-800">{t.contentAnexa1}</span>
            </li>
            <li className="flex items-start gap-2">
              <FileCheck2 className="mt-0.5 h-4 w-4 shrink-0 text-brand" />
              <span className="font-medium text-gray-800">{t.contentAnnualDeclaration}</span>
            </li>
            <li className="flex items-center gap-2">
              <FileSpreadsheet className="h-4 w-4 text-brand" />
              {t.contentEvidence}
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
      </div>

      <p className="mt-4 max-w-xl rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
        {t.note}
      </p>
    </div>
  );
}
