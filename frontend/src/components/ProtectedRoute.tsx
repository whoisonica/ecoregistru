import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "@/auth/AuthContext";
import { strings } from "@/lib/strings";
import { REDIRECT_PARAM } from "@/lib/api";
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
