import { useState, type ReactNode } from "react";
import { Download } from "lucide-react";
import { useEvidences, downloadAnexa1Form } from "@/hooks/useEvidences";
import {
  downloadPackagingAnexa3,
  downloadPackagingDeclaration,
  usePackagingAnexa3,
  usePackagingUnclassified,
} from "@/hooks/usePackaging";
import { useWorkPoints } from "@/hooks/useWorkPoints";
import { evidenceReadiness, reportedYear } from "@/lib/deadlines";
import { formatKg } from "@/lib/units";
import { strings } from "@/lib/strings";
import { cn, countOf } from "@/lib/utils";
import type { Deadline } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { useToast } from "@/components/ui/toast";

const t = strings.deadlines.readiness;

/**
 * Pe un termen deschis: documentul care îl stinge, socotit din ce e deja înregistrat — cifrele anului
 * raportat, ce oprește depunerea și, când nu mai e nimic, „Gata de depus” cu descărcarea.
 *
 * <p>Nu face nicio socoteală nouă: citește aceleași liste ca Evidențele și Ambalajele, deci ce scrie
 * aici e ce ar vedea clientul pe ecranul documentului. Termenele fără document (AFM, 30 aprilie,
 * 31 mai) nu primesc nimic — n-avem ce să tipărim pentru ele.
 */
export function DeadlineReadiness({ deadline }: { deadline: Deadline }) {
  if (deadline.status === "DONE") return null;
  const year = reportedYear(deadline);
  switch (deadline.reportType) {
    case "SIM_ANNUAL":
      return <EvidenceReadiness year={year} />;
    case "PACKAGING_ANNUAL":
      return <PackagingDeclarationReadiness year={year} />;
    case "PACKAGING_ANNEX3":
      return <Anexa3Readiness year={year} />;
    default:
      return null;
  }
}

function EvidenceReadiness({ year }: { year: number }) {
  const { data, isLoading, isError } = useEvidences({ year });
  if (isLoading) return <Line>{t.loading}</Line>;
  if (isError || !data) return <Line tone="bad">{t.error}</Line>;
  if (data.length === 0)
    return <Line>{t.evidenceEmpty.replace("{year}", String(year))}</Line>;

  const r = evidenceReadiness(data, year);
  // Roșu = nu se poate depune așa; galben = o așteptare legitimă (tokenii `state` din Tailwind).
  const blockers: Blocker[] = [];
  if (r.missingCode > 0)
    blockers.push({
      tone: "bad",
      text: t.missingCode.replace(
        "{count}",
        countOf(r.missingCode, "linie", "linii"),
      ),
    });
  if (r.awaitingWeighing > 0)
    blockers.push({
      tone: "warn",
      text: t.awaitingWeighing.replace(
        "{count}",
        countOf(r.awaitingWeighing, "linie", "linii"),
      ),
    });

  return (
    <>
      <Line>
        {t.evidenceFigures
          .replace("{year}", String(year))
          .replace("{codes}", countOf(r.codes, "cod", "coduri"))
          .replace("{kg}", formatKg(r.generatedKg))}
      </Line>
      <Status
        yearOpen={r.yearOpen}
        year={year}
        blockers={blockers}
        ready={r.ready}
        onDownload={() => downloadAnexa1Form({ year })}
      />
    </>
  );
}

function PackagingDeclarationReadiness({ year }: { year: number }) {
  const { data, isLoading, isError } = usePackagingUnclassified(year);
  if (isLoading) return <Line>{t.loading}</Line>;
  if (isError || !data) return <Line tone="bad">{t.error}</Line>;

  const blockers: Blocker[] =
    data.length > 0
      ? [
          {
            tone: "bad",
            text: t.unclassified.replace(
              "{count}",
              countOf(data.length, "mișcare", "mișcări"),
            ),
          },
        ]
      : [];
  const yearOpen = year >= new Date().getFullYear();
  return (
    <Status
      yearOpen={yearOpen}
      year={year}
      blockers={blockers}
      ready={!yearOpen && blockers.length === 0}
      onDownload={() => downloadPackagingDeclaration(year, "xls")}
    />
  );
}

function Anexa3Readiness({ year }: { year: number }) {
  const { data, isLoading, isError } = usePackagingAnexa3(year);
  const { data: workPoints } = useWorkPoints();
  if (isLoading) return <Line>{t.loading}</Line>;
  if (isError || !data) return <Line tone="bad">{t.error}</Line>;
  if (!data.printable) return <Line tone="warn">{t.roleMissing}</Line>;

  const empty =
    data.intake.length +
      data.handovers.length +
      data.treatments.length +
      data.unclassified.length ===
    0;
  if (empty)
    return <Line>{t.packagingEmpty.replace("{year}", String(year))}</Line>;

  const blockers: Blocker[] =
    data.unclassified.length > 0
      ? [
          {
            tone: "bad",
            text: t.unclassified.replace(
              "{count}",
              countOf(data.unclassified.length, "mișcare", "mișcări"),
            ),
          },
        ]
      : [];
  const yearOpen = year >= new Date().getFullYear();
  // Art. 4 alin. (4): un raport pe fiecare punct de lucru. Un fișier pe toată firma ar fi greșit de
  // depus acolo unde sunt mai multe, deci descărcarea rămâne pe ecranul Ambalaje, unde se alege punctul.
  const severalWorkPoints =
    (workPoints ?? []).filter((w) => w.active).length > 1;
  return (
    <>
      <Status
        yearOpen={yearOpen}
        year={year}
        blockers={blockers}
        ready={!yearOpen && blockers.length === 0}
        onDownload={
          severalWorkPoints
            ? undefined
            : () => downloadPackagingAnexa3(year, undefined, "xls")
        }
      />
      {severalWorkPoints && !yearOpen && blockers.length === 0 && (
        <Line>{t.perWorkPoint}</Line>
      )}
    </>
  );
}

type Blocker = { tone: "warn" | "bad"; text: string };

function Status({
  yearOpen,
  year,
  blockers,
  ready,
  onDownload,
}: {
  yearOpen: boolean;
  year: number;
  blockers: Blocker[];
  ready: boolean;
  onDownload?: () => Promise<void>;
}) {
  const { notify } = useToast();
  const [busy, setBusy] = useState(false);

  return (
    <>
      {blockers.map((b) => (
        <Line key={b.text} tone={b.tone}>
          {b.text}
        </Line>
      ))}
      {yearOpen && <Line>{t.yearOpen.replace("{year}", String(year))}</Line>}
      {ready && (
        <div className="mt-1 flex flex-wrap items-center gap-2">
          <span className="inline-flex items-center gap-1.5 text-xs font-medium text-content">
            <span
              aria-hidden
              className="h-1.5 w-1.5 rounded-[1px] bg-state-ok"
            />
            {t.ready}
          </span>
          {onDownload && (
            <Button
              variant="outline"
              size="sm"
              disabled={busy}
              onClick={async () => {
                setBusy(true);
                try {
                  await onDownload();
                } catch {
                  notify(t.downloadError, "error");
                } finally {
                  setBusy(false);
                }
              }}
            >
              <Download className="mr-1 h-3.5 w-3.5" aria-hidden />
              {t.download}
            </Button>
          )}
        </div>
      )}
    </>
  );
}

function Line({
  children,
  tone,
}: {
  children: ReactNode;
  tone?: "warn" | "bad";
}) {
  return (
    <p
      className={cn(
        "mt-1 flex items-start gap-1.5 text-xs",
        tone ? "text-content" : "text-content-subtle",
      )}
    >
      {tone && (
        <span
          aria-hidden
          className={cn(
            "mt-1 h-1.5 w-1.5 shrink-0 rounded-[1px]",
            tone === "warn" ? "bg-state-warn" : "bg-state-bad",
          )}
        />
      )}
      <span>{children}</span>
    </p>
  );
}
