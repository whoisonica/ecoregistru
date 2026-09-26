import * as Sentry from "@sentry/react-native";

/**
 * Colectorul de erori pe telefon (todo-mobil §5): același proiect Sentry ca webul
 * (`frontend/src/lib/monitoring.ts`), cu `environment=mobile`.
 *
 * <p>**Fără `EXPO_PUBLIC_SENTRY_DSN` nu se inițializează nimic** — dev-ul și fluxurile Maestro merg ca
 * înainte, iar `reportError` rămâne apelabil și nu face nimic.
 *
 * <p>Numai erori, ca pe web: fără tracing, fără Session Replay (ar filma mișcări, parteneri, cantități
 * — date de firmă la un terț), fără PII implicit. Din adrese cad parametrii: un token n-are ce căuta
 * într-un raport de defect.
 */
export function initMonitoring() {
  const dsn = process.env.EXPO_PUBLIC_SENTRY_DSN;
  if (!dsn) return;

  Sentry.init({
    dsn,
    environment: "mobile",
    sendDefaultPii: false,
    tracesSampleRate: 0,
    enableAutoSessionTracking: false,
    beforeSend(event) {
      if (event.request?.url) event.request.url = stripQuery(event.request.url);
      return event;
    },
    beforeBreadcrumb(crumb) {
      if (typeof crumb.data?.url === "string") crumb.data.url = stripQuery(crumb.data.url);
      return crumb;
    },
  });
}

function stripQuery(url: string) {
  return url.split("?")[0];
}

/** Raportează o excepție din care aplicația și-a revenit. No-op cât timp DSN-ul lipsește. */
export function reportError(error: unknown, context?: Record<string, unknown>) {
  Sentry.captureException(error, context ? { extra: context } : undefined);
}

/** Ecranul de bază, învelit ca o eroare de randare să ajungă în Sentry. */
export const wrap = Sentry.wrap;
