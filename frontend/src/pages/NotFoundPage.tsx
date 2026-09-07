import { Link, useLocation } from "react-router-dom";
import { Compass } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { strings } from "@/lib/strings";

const t = strings.notFound;

/**
 * Ce se vede la o adresă care nu există.
 *
 * <p>Nu exista nimic: `<Routes>` fără o rută `*` nu randează **nimic**, deci `/miscarii`, un link
 * vechi sau o literă în plus dădeau o pagină albă, fără antet, fără meniu, fără mesaj. Aplicația
 * arăta căzută pentru o greșeală de tastare.
 *
 * <p>Stă în afara `AppShell` dinadins, ca să răspundă la fel și fără sesiune: pagina de aici nu
 * cere nimic de la server, iar a o pune în spatele autentificării ar fi însemnat că o adresă
 * greșită te aruncă în login în loc să-ți spună că adresa e greșită. Cine e autentificat primește
 * drumul înapoi la Panou; cine nu, la autentificare.
 */
export function NotFoundPage() {
  const { user } = useAuth();
  const location = useLocation();

  return (
    <div className="flex min-h-screen items-center justify-center p-4">
      <Card className="w-full max-w-md p-8 text-center">
        <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-surface-muted">
          <Compass className="h-6 w-6 text-content-subtle" aria-hidden />
        </div>
        <div className="text-2xl font-bold text-brand">{strings.appName}</div>
        <h1 className="mt-4 text-lg font-semibold text-content">{t.title}</h1>
        <p className="mt-2 text-sm text-content-muted">{t.body}</p>
        {/* Adresa cerută, scrisă întreagă: de cele mai multe ori greșeala se vede citind-o. */}
        <p className="mt-3 break-all rounded-md bg-surface-muted px-3 py-2 font-mono text-xs text-content-subtle">
          {location.pathname}
        </p>
        <Link to={user ? "/" : "/login"} className="mt-6 inline-block">
          <Button>{user ? t.toDashboard : t.toLogin}</Button>
        </Link>
      </Card>
    </div>
  );
}
