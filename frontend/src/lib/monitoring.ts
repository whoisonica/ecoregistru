import * as Sentry from "@sentry/react";

/**
 * P0.6 — colectorul de erori, capătul de browser.
 *
 * <p>`ErrorBoundary` o spunea singur: *„Nu există colector de erori în producție (nici Sentry, nici
 * altceva). Consola e tot ce avem."* Adevărat cât timp singurul utilizator era cel care a scris-o.
 * Cu clienți, consola e a lor — deci un defect se află doar dacă cineva sună, iar cei mai mulți nu
 * sună, ci renunță.
 *
 * <p>**Fără `VITE_SENTRY_DSN` nu se inițializează nimic.** `Sentry.captureException` rămâne
 * apelabil și nu face nimic, deci dev-ul, suita e2e și un build fără variabilă se comportă exact ca
 * înainte — nicio cerere de rețea în plus, niciun cookie.
 */
export function initMonitoring() {
  const dsn = import.meta.env.VITE_SENTRY_DSN;
  if (!dsn) return;

  Sentry.init({
    dsn,
    environment: import.meta.env.VITE_SENTRY_ENVIRONMENT || "productie",
    // Numai erori. Fără `browserTracingIntegration` și fără Session Replay: replay-ul ar filma
    // ecranul unui client — mișcări, parteneri, cantități — și l-ar trimite la un terț. Aplicația
    // e multi-tenant și ține date de firmă; ce apărăm nu se pune într-un raport de defect.
    integrations: [],
    tracesSampleRate: 0,
    // Adresa paginii ajunge oricum în raport; parametrii ei nu. `/miscari?an=2026` e util,
    // un token dintr-un link de resetare nu are ce căuta acolo.
    beforeSend(event) {
      if (event.request?.url) {
        event.request.url = event.request.url.split("?")[0];
      }
      return event;
    },
  });
}

/**
 * Raportează o excepție prinsă de noi (deci una din care aplicația și-a revenit). Un no-op cât timp
 * DSN-ul lipsește, ca să poată fi chemată necondiționat de la locul greșelii.
 */
export function reportError(error: unknown, context?: Record<string, unknown>) {
  Sentry.captureException(error, context ? { extra: context } : undefined);
}
