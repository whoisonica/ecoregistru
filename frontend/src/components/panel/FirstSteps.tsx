import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "@/auth/AuthContext";
import { useCurrentCompany } from "@/hooks/useCompanies";
import { useDashboardData } from "@/hooks/useDashboardData";
import { firstSteps, showFirstSteps, type CompanyField, type FirstStep } from "@/lib/firstSteps";
import { SCREEN_PATH, screensFor } from "@/lib/movementScreens";
import { canImport, canWrite, isMultiCompany } from "@/lib/roles";
import { strings } from "@/lib/strings";
import { cn } from "@/lib/utils";
import { Badge } from "@/components/ui/badge";
import { Card, CardHeader } from "@/components/ui/card";

const t = strings.dashboard.firstSteps;

const FIELD_LABEL: Record<CompanyField, string> = {
  address: t.fieldAddress,
  caenCode: t.fieldCaen,
  wasteManagerName: t.fieldWasteManager,
};

/** Ascunsă pe firmă, în browserul ăsta. Fără stocare (fereastră privată), lista doar reapare. */
const hiddenKey = (companyId: string) => `wh.firstSteps.hidden.${companyId}`;

function readHidden(companyId: string | undefined): boolean {
  if (!companyId) return false;
  try {
    return localStorage.getItem(hiddenKey(companyId)) === "1";
  } catch {
    return false;
  }
}

type Row = { title: string; hint: string; links: { to: string; label: string }[] };

/**
 * „Primii pași” pe Acasă: până e gata tot, lista drumului de la contul aprobat la primul document.
 *
 * <p>Banda „Următoarea acțiune” spune un singur lucru; lista spune câți pași sunt și unde e omul
 * pe drum. Dispare singură când lucrul e pornit (`showFirstSteps`), sau când e ascunsă. N-o vede cine
 * nu poate face niciun pas (vizualizatorul).
 */
export function FirstSteps() {
  const { user } = useAuth();
  const { data: company } = useCurrentCompany();
  const d = useDashboardData();
  const [hidden, setHidden] = useState(() => readHidden(company?.id));

  if (!canWrite(user?.role) || !company || hidden || readHidden(company.id)) return null;
  if (d.failedMovements || d.failedPartners || d.failedEvidences) return null;

  const steps = firstSteps({
    company,
    workPoints: d.workPoints,
    partners: d.partners,
    evidences: d.evidences,
    movementCount: d.summary ? d.movementCount : undefined,
  });
  if (!showFirstSteps(steps)) return null;

  const multi = isMultiCompany(user?.role);
  const firstScreen = screensFor(company.type)[0];

  function row(step: FirstStep): Row {
    switch (step.id) {
      case "company": {
        if (step.done) {
          return { title: t.company, hint: t.companyHintDone, links: [] };
        }
        const fields = step.missing.map((f) => FIELD_LABEL[f]).join(", ");
        return {
          title: t.company,
          hint: `${t.companyHintMissing.replace("{fields}", fields)} ${multi ? t.companyHintClients : t.companyHintAskUs}`,
          links: [multi ? { to: "/clienti", label: t.companyClientsCta } : { to: "/setari#datele-firmei", label: t.companyCta }],
        };
      }
      case "workPoint":
        return { title: t.workPoint, hint: t.workPointHint, links: [{ to: "/setari#puncte-de-lucru", label: t.workPointCta }] };
      case "partner":
        return { title: t.partner, hint: t.partnerHint, links: [{ to: "/parteneri", label: t.partnerCta }] };
      case "movement":
        return {
          title: t.movement,
          // Importul îl facem noi (16.09.2026): clientul află unde trimite Excelul, platforma îl deschide.
          hint: `${t.movementHint} ${canImport(user?.role) ? t.importHintPlatform : t.importHintSendUs}`,
          links: [
            { to: `${SCREEN_PATH[firstScreen]}?nou=1`, label: t.movementCta },
            ...(canImport(user?.role) ? [{ to: "/import", label: t.importCta }] : []),
          ],
        };
    }
  }

  function hide() {
    try {
      localStorage.setItem(hiddenKey(company!.id), "1");
    } catch {
      // Fără stocare, ascunderea ține doar până la reîncărcare.
    }
    setHidden(true);
  }

  const doneCount = steps.filter((s) => s.done).length;

  return (
    <Card className="mt-4" data-testid="first-steps">
      <CardHeader
        title={t.title}
        description={
          <span className="font-mono text-xs">
            {t.progress.replace("{done}", String(doneCount)).replace("{total}", String(steps.length))}
          </span>
        }
        action={
          <button type="button" onClick={hide} className="text-xs font-semibold text-content-muted hover:underline">
            {t.hide}
          </button>
        }
      />
      <ol className="mt-3">
        {steps.map((step, i) => {
          const r = row(step);
          return (
            <li
              key={step.id}
              data-step={step.id}
              data-done={step.done}
              className="flex flex-col gap-2 border-t border-line py-3 first:border-t-0 sm:flex-row sm:items-start sm:gap-3"
            >
              <span className="hidden w-5 shrink-0 font-mono text-sm text-content-subtle sm:block">{i + 1}</span>
              <div className="min-w-0 flex-1">
                <p className={cn("text-sm font-medium", step.done ? "text-content-muted" : "text-content")}>
                  <Badge variant={step.done ? "success" : "muted"} className="mr-2 align-middle">
                    {step.done ? t.done : t.todo}
                  </Badge>
                  {r.title}
                </p>
                <p className="mt-0.5 text-xs text-content-muted">{r.hint}</p>
              </div>
              {!step.done && r.links.length > 0 && (
                <div className="flex shrink-0 gap-4">
                  {r.links.map((l) => (
                    <Link key={l.to} to={l.to} className="whitespace-nowrap text-xs font-semibold text-brand-700 hover:underline">
                      {l.label}
                    </Link>
                  ))}
                </div>
              )}
            </li>
          );
        })}
      </ol>
    </Card>
  );
}
