import { Building2 } from "lucide-react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "@/auth/AuthContext";
import { strings } from "@/lib/strings";
import { REDIRECT_PARAM } from "@/lib/api";
import { EmptyState } from "@/components/ui/empty-state";
import { LinkButton } from "@/components/ui/button";
import type { ReactNode } from "react";

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return <div className="flex h-full items-center justify-center text-content-muted">{strings.common.loading}</div>;
  }
  if (!user) {
    /**
     * Unde voia să ajungă călătorește prin adresă, ca să nu se piardă la autentificare.
     *
     * <p>Până acum orice pagină cerută fără sesiune ducea la Panou după login. Un link trimis pe
     * mail către `/termene?an=2025`, sau un semn de carte pe evidența unei firme, se pierdea de
     * două ori: o dată la redirectarea către login, a doua oară după ce te autentificai.
     *
     * <p>Se duce calea, nu pagina: e o cale internă, scrisă de router, deci nu poate trimite pe alt
     * domeniu. `LoginPage` o verifică oricum înainte s-o folosească.
     */
    const target = `${location.pathname}${location.search}`;
    const to = target === "/" ? "/login" : `/login?${REDIRECT_PARAM}=${encodeURIComponent(target)}`;
    return <Navigate to={to} replace />;
  }
  return <>{children}</>;
}

/**
 * Ecranele de firmă, ținute închise cât timp nu s-a ales o firmă.
 *
 * <p>Privește numai `PLATFORM_ADMIN`: el e singurul care poate fi autentificat **fără** o firmă
 * curentă, fiindcă `switchTenant(null)` e o stare validă și fiindcă exact acolo ajunge după login.
 * Pentru toți ceilalți firma vine din token și nu lipsește niciodată.
 *
 * <p>Până acum ecranele se randau oricum. Cererile plecau fără `X-Tenant-Id`, backendul răspundea
 * `400`, iar TanStack le mai încerca o dată (`retry: 1`) — opt cereri roșii în consolă la fiecare
 * autentificare de administrator. Mai rău: Panoul citea listele căzute ca liste **goale** și scria
 * „Ești la zi" peste ele. Reparația din `DashboardPage` face afirmația onestă; asta scoate cu totul
 * cauza, pentru toate ecranele deodată — un ecran de firmă fără firmă n-are ce arăta.
 *
 * <p>`Clienți` nu trece pe aici, dinadins: e chiar ecranul de administrare a firmelor, deci singurul
 * care are ce spune înainte să fie aleasă vreuna.
 */
export function RequireTenant({ children }: { children: ReactNode }) {
  const { user, tenantId } = useAuth();
  if (user?.role === "PLATFORM_ADMIN" && !tenantId) {
    return (
      <EmptyState
        className="mt-6"
        icon={Building2}
        title={strings.header.pickCompanyTitle}
        description={strings.header.pickCompanyHint}
        action={
          <LinkButton to="/clienti" variant="outline">
            {strings.header.pickCompanyAction}
          </LinkButton>
        }
      />
    );
  }
  return <>{children}</>;
}
