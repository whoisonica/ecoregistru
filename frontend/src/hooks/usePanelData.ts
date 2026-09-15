import { useAuth } from "@/auth/AuthContext";
import { useCurrentCompany } from "@/hooks/useCompanies";
import { useDashboardData } from "@/hooks/useDashboardData";
import { useMovementTotals } from "@/hooks/useMovements";
import { isMultiCompany } from "@/lib/roles";
import { directionOf, registerOf, screensFor, type MovementScreen } from "@/lib/movementScreens";
import { strings } from "@/lib/strings";

const t = strings.panel;

/** Un indicator din dreapta unei intrări de meniu. `unknown` = sursa n-a răspuns → „?" (decizia 68). */
export interface Indicator {
  tone: "warn" | "bad" | "unknown";
  text: string;
}

/**
 * Cifrele panoului: afișajul lunii și indicatorii de pe intrările de meniu.
 *
 * <p>Toate sunt cifre calculate deja — pe Acasă (`useDashboardData`) sau de server, ca totaluri pe
 * anul curent ale fiecărui ecran de mișcări. Nicio regulă nouă de business: panoul doar le
 * pune la vedere. Un indicator care n-a putut încărca arată „?", nu „0", și nu se ascunde.
 *
 * <p>Se cer numai când există o firmă: consultantul fără firmă aleasă n-are ce citi, iar cererile
 * fără `X-Tenant-Id` ar cădea pe 400 (vezi `RequireTenant`).
 */
export function usePanelData() {
  const { user, tenantId } = useAuth();
  const hasCompany = !isMultiCompany(user?.role) || Boolean(tenantId);
  const { data: company } = useCurrentCompany(hasCompany);
  const screens = company ? screensFor(company.type) : [];
  const dashboard = useDashboardData(hasCompany);

  // Totalurile pe an, pe fiecare ecran de mișcări: „de cântărit" pe Generare și Intrări, „fără cod
  // R/D" pe Ieșiri. Un hook pe ecran, cu `enabled` după ce vede firma — numărul de hook-uri e fix.
  const year = dashboard.year;
  const generated = useMovementTotals({ year, register: registerOf("GENERATED") }, hasCompany && screens.includes("GENERATED"));
  const inbound = useMovementTotals({ year, register: registerOf("IN"), direction: directionOf("IN") }, hasCompany && screens.includes("IN"));
  const outbound = useMovementTotals({ year, register: registerOf("OUT"), direction: directionOf("OUT") }, hasCompany && screens.includes("OUT"));

  // Afișajul lunii: generatorul vede ce a generat, colectorul ce a intrat — cifra care contează
  // pentru fiecare, pe luna curentă.
  const monthScreen: MovementScreen | null = screens.includes("IN") ? "IN" : screens.includes("GENERATED") ? "GENERATED" : null;
  const month = useMovementTotals(
    { year, month: dashboard.month, register: monthScreen ? registerOf(monthScreen) : undefined, direction: monthScreen ? directionOf(monthScreen) : undefined },
    hasCompany && monthScreen != null
  );

  function count(n: number, one: string, many: string): string {
    return n === 1 ? one : many.replace("{n}", String(n));
  }

  function indicatorFor(screen: MovementScreen | undefined, to: string): Indicator | null {
    if (!hasCompany) return null;
    if (screen === "GENERATED" || screen === "IN") {
      const q = screen === "GENERATED" ? generated : inbound;
      if (q.isError) return { tone: "unknown", text: t.indUnknown };
      const n = q.data?.awaitingWeighing ?? 0;
      return n > 0 ? { tone: "warn", text: t.indAwaitingWeighing.replace("{n}", String(n)) } : null;
    }
    if (screen === "OUT") {
      if (outbound.isError) return { tone: "unknown", text: t.indUnknown };
      const n = outbound.data?.missingOperationCode ?? 0;
      return n > 0 ? { tone: "bad", text: t.indMissingCode.replace("{n}", String(n)) } : null;
    }
    if (to === "/termene") {
      if (dashboard.failedDeadlines) return { tone: "unknown", text: t.indUnknown };
      const n = dashboard.overdueCount;
      return n > 0 ? { tone: "bad", text: count(n, t.indOverdueOne, t.indOverdue) } : null;
    }
    if (to === "/parteneri") {
      if (dashboard.failedPartners) return { tone: "unknown", text: t.indUnknown };
      const n = dashboard.expiringPartners.length;
      return n > 0 ? { tone: "warn", text: t.indExpiring.replace("{n}", String(n)) } : null;
    }
    return null;
  }

  return {
    company,
    hasCompany,
    screens,
    dashboard,
    monthLabel: monthScreen === "IN" ? t.monthReceived : t.monthGenerated,
    monthKg: month.isError ? null : (month.data?.quantityKg ?? null),
    monthLoading: month.isLoading,
    monthFailed: month.isError,
    indicatorFor,
  };
}
