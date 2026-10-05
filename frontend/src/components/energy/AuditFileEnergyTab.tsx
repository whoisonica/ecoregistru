import { useState } from "react";
import { Download, Zap } from "lucide-react";
import { Link } from "react-router-dom";
import { downloadEnergyDossier, useEnergyYears } from "@/hooks/useEnergy";
import { apiBlobErrorMessage } from "@/lib/api";
import { energyYearStatus } from "@/lib/energy";
import { strings } from "@/lib/strings";
import { Badge } from "@/components/ui/badge";
import { Button, LinkButton } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";
import { LoadError } from "@/components/ui/load-error";
import { Skeleton } from "@/components/ui/skeleton";
import { useToast } from "@/components/ui/toast";

const t = strings.energy;

/**
 * Tabul „Energie” din Dosarul de control: un rând pe fiecare an cu date — anul, starea și arhiva
 * lui (Anexa 1, Declarația, recipisa dacă există). Starea o spune `energyYearStatus`, aceeași pe care
 * o citește și restul aplicației.
 */
export function AuditFileEnergyTab() {
  const years = useEnergyYears();
  const [busy, setBusy] = useState<number | null>(null);
  const { notify } = useToast();

  async function download(year: number) {
    setBusy(year);
    try {
      await downloadEnergyDossier(year);
    } catch (err) {
      notify(await apiBlobErrorMessage(err, t.dossierError), "error");
    } finally {
      setBusy(null);
    }
  }

  if (years.isError) {
    return <LoadError className="mt-6" message={t.loadError} onRetry={() => void years.refetch()} />;
  }
  if (!years.data) {
    return (
      <div className="mt-6 space-y-3">
        {[0, 1].map((k) => (
          <Skeleton key={k} className="h-10 w-full" />
        ))}
      </div>
    );
  }
  if (years.data.length === 0) {
    return (
      <EmptyState
        className="mt-6"
        icon={Zap}
        title={t.dossierEmptyTitle}
        description={t.dossierEmptyDescription}
        action={<LinkButton to="/energie">{t.dossierEmptyAction}</LinkButton>}
      />
    );
  }
  return (
    <ul className="mt-6 divide-y divide-line border-y border-line" data-testid="audit-file-energy">
      {years.data.map((y) => {
        const status = energyYearStatus(y);
        return (
          <li key={y.year} className="flex flex-wrap items-center gap-x-4 gap-y-2 py-3">
            <Link
              to={`/energie?an=${y.year}`}
              className="w-16 font-mono text-sm font-medium text-content-strong underline-offset-2 hover:underline"
            >
              {y.year}
            </Link>
            <Badge variant={status.tone}>{status.label}</Badge>
            <Button
              variant="outline"
              size="sm"
              className="sm:ml-auto"
              loading={busy === y.year}
              disabled={busy !== null}
              onClick={() => void download(y.year)}
            >
              {busy !== y.year && <Download className="mr-1.5 h-3.5 w-3.5" aria-hidden />}
              {t.dossierZip.replace("{year}", String(y.year))}
            </Button>
          </li>
        );
      })}
    </ul>
  );
}
