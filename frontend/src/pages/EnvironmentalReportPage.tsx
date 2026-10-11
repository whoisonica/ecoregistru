import { useState } from "react";
import { useCanWrite } from "@/hooks/useBillingAccess";
import { downloadEnvironmentalReport, useEnvironmentalReport } from "@/hooks/useEnvironmentalReport";
import { useUrlNumber, useUrlState } from "@/hooks/useUrlState";
import { apiErrorMessage } from "@/lib/api";
import { RAM_TABS } from "@/lib/screenTabs";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { LoadError } from "@/components/ui/load-error";
import { PageHeader } from "@/components/ui/page-header";
import { PageTabs } from "@/components/ui/page-tabs";
import { Select } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { useToast } from "@/components/ui/toast";
import { EnvironmentalPermitForm } from "@/components/environmental/EnvironmentalPermitForm";
import { EnvironmentalReportEditor } from "@/components/environmental/EnvironmentalReportEditor";

const t = strings.environmentalReport;
/** Primul an pe care îl primește serverul (`EnvironmentalReportService.FIRST_YEAR`). */
const FIRST_YEAR = 2020;

/**
 * Raportul anual de mediu — `/raport-mediu?an=YYYY&tab=` (V86, Andreea 09.10.2026). Anul implicit e cel trecut: în
 * ianuarie se raportează anul de dinainte. Trei taburi pe anul ales și unul, „Autorizația”, al firmei.
 */
export function EnvironmentalReportPage() {
  const thisYear = new Date().getFullYear();
  const [year, setYear] = useUrlNumber("an", thisYear - 1);
  const [tab, setTab] = useUrlState("tab");
  const [downloading, setDownloading] = useState(false);
  const canWrite = useCanWrite();
  const { notify } = useToast();
  const q = useEnvironmentalReport(year);
  const report = q.data;
  const years = Array.from({ length: thisYear - FIRST_YEAR + 1 }, (_, i) => thisYear - i);

  async function download() {
    setDownloading(true);
    try {
      await downloadEnvironmentalReport(year);
    } catch (err) {
      notify(apiErrorMessage(err, t.downloadError), "error");
    } finally {
      setDownloading(false);
    }
  }

  return (
    <div>
      <PageHeader
        title={`${t.title} · ${year}`}
        description={t.description}
        actions={
          <>
            <label htmlFor="ram-an" className="sr-only">
              {t.yearLabel}
            </label>
            <Select id="ram-an" value={String(year)} onChange={(e) => setYear(Number(e.target.value))} className="w-28">
              {years.map((y) => (
                <option key={y} value={y}>
                  {y}
                </option>
              ))}
            </Select>
            <Button variant="outline" onClick={() => void download()} loading={downloading} disabled={!report}>
              {t.download}
            </Button>
          </>
        }
      />

      {q.isError && <LoadError className="mt-6" message={t.loadError} onRetry={() => void q.refetch()} />}
      {q.isLoading && <Skeleton className="mt-6 h-64 w-full" />}

      {report && (
        <>
          {!report.permit.number && tab !== "autorizatie" && (
            <p role="status" className="mt-4 flex items-start gap-2 text-sm text-content">
              <span className="mt-1.5 h-2 w-2 shrink-0 bg-state-warn" aria-hidden />
              {t.noPermit}
            </p>
          )}
          <PageTabs tabs={RAM_TABS} selected={tab} onSelect={setTab} label={t.tabsLabel} />
          {tab === "autorizatie" ? (
            <EnvironmentalPermitForm key={`aut-${year}`} year={year} permit={report.permit} canWrite={canWrite} />
          ) : (
            <EnvironmentalReportEditor key={year} report={report} tab={tab} canWrite={canWrite} />
          )}
        </>
      )}
    </div>
  );
}
