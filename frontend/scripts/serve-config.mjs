// BUG-020 — antetele de securitate ale aplicației web, scrise în `dist/serve.json` după build.
//
// `serve -s dist` (scriptul `start`, pe Heroku) citește `serve.json` din directorul servit. Fără el,
// răspunsul avea doar antetele Heroku: nicio CSP, deci un XSS ar fi putut citi tokenul din
// `localStorage` (`eco_token`, `lib/api.ts`) și l-ar fi trimis oriunde.
//
// Se generează, nu se scrie de mână: `connect-src` trebuie să numească host-ul API-ului și pe al
// Sentry, iar amândouă vin din aceleași variabile `VITE_*` pe care le coace Vite în bundle. Un build
// local cu `VITE_API_BASE_URL=http://localhost:8080` primește deci exact politica de pe producție,
// cu alt host — așa se probează cu suita e2e.
import { writeFileSync } from "node:fs";

const origin = (url) => {
  try {
    return url ? new URL(url).origin : null;
  } catch {
    throw new Error(`serve-config: adresă invalidă „${url}”`);
  }
};

const connect = ["'self'", origin(process.env.VITE_API_BASE_URL), origin(process.env.VITE_SENTRY_DSN)]
  .filter(Boolean)
  .join(" ");

const csp = [
  "default-src 'self'",
  "script-src 'self'",
  // Stilurile vin din fișierul construit de Tailwind; `style={}` din React trece prin CSSOM, pe care
  // CSP nu-l oprește, deci nu trebuie `'unsafe-inline'`.
  "style-src 'self'",
  // Fonturile IBM Plex sunt în `public/fonts`, nu la Google.
  "font-src 'self'",
  // `data:` = favicon-ul din `index.html`; `blob:` = logoul cabinetului, cerut cu tokenul.
  "img-src 'self' data: blob:",
  `connect-src ${connect}`,
  // PDF-urile și atașamentele se deschid în tab ca `blob:` (`lib/openFileInTab.ts`).
  "frame-src 'self' blob:",
  "object-src 'none'",
  "base-uri 'self'",
  "form-action 'self'",
  "frame-ancestors 'none'",
].join("; ");

const config = {
  headers: [
    {
      source: "**",
      headers: [
        { key: "Content-Security-Policy", value: csp },
        { key: "Strict-Transport-Security", value: "max-age=31536000; includeSubDomains" },
        { key: "X-Frame-Options", value: "DENY" },
        { key: "X-Content-Type-Options", value: "nosniff" },
        { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
        { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
      ],
    },
  ],
};

writeFileSync(new URL("../dist/serve.json", import.meta.url), JSON.stringify(config, null, 2) + "\n");
console.log(`serve.json: connect-src ${connect}`);
