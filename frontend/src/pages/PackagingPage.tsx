import { Navigate } from "react-router-dom";
import { SCREEN_PATH, screensFor } from "@/lib/movementScreens";
import { useCurrentCompany } from "@/hooks/useCompanies";
import { strings } from "@/lib/strings";
import { useUrlNumber } from "@/hooks/useUrlState";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { PageHeader } from "@/components/ui/page-header";
import { PackagingReport } from "@/components/packaging/PackagingReport";

const t = strings.packaging;

/** Year options: current year down to five years back, same as the other documents. */
function yearOptions(): number[] {
  const now = new Date().getFullYear();
  return Array.from({ length: 6 }, (_, i) => now - i);
}

/**
 * `/ambalaje` — ecranul propriu al ambalajelor, rămas pentru firmele care **nu** au „Generare".
 *
 * <p>Pe 18.09.2026 ambalajele au intrat ca al treilea tab în „Generare", din același motiv pentru
 * care „Evidențe" intrase cu o zi înainte: Anexa 1 Ambalaje se însumează din deșeul propriu al
 * firmei, adică din chiar rândurile ecranului „Generare" — iar ecranul de aici își mai ținea și un
 * registru al lor, al treilea tabel de mișcări din aplicație.
 *
 * <p>Colectorul pur n-are „Generare" (`screensFor`), dar are Anexa 3 la Ordinul 794/2012 — raportul
 * deșeurilor de ambalaje preluate de la terți. Deci ecranul rămâne, pentru el: aceleași tabele, cu
 * un antet și un filtru de an în locul taburilor. Cine are „Generare" e trimis acolo, cu tot cu an.
 */
export function PackagingPage() {
  const { data: company, isLoading } = useCurrentCompany();
  const [year, setYear] = useUrlNumber("an", new Date().getFullYear());

  if (isLoading) return null;
  if (screensFor(company?.type).includes("GENERATED")) {
    return <Navigate replace to={`${SCREEN_PATH.GENERATED}?tab=ambalaje&luna=${year}`} />;
  }

  return (
    <div>
      <PageHeader title={t.title} description={t.subtitle} />

      <div className="mt-6 w-40">
        <Label htmlFor="pk-year">{t.year}</Label>
        <Select id="pk-year" value={String(year)} onChange={(ev) => setYear(Number(ev.target.value))}>
          {yearOptions().map((y) => (
            <option key={y} value={y}>
              {y}
            </option>
          ))}
        </Select>
      </div>

      {/* Mișcările de reparat se caută pe ecranul lor: la colectorul pur, „Ieșiri". */}
      <PackagingReport year={year} movementsPath={SCREEN_PATH.OUT} />
    </div>
  );
}
