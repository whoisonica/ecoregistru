/**
 * Serverul aplicației. `EXPO_PUBLIC_API_URL` din `mobile/.env.local` se citește la build, deci un build
 * Release făcut pe Mac cu fișierul uitat pe loc (ca pentru iPhone-ul proprietarului) ar fi purtat
 * `http://localhost:8096` și n-ar fi găsit niciun server pe telefon. Un build Release vorbește numai
 * prin https; adresele fără el sunt pentru lucrul local.
 */
export function apiBaseUrl(envUrl: string | undefined, dev: boolean, production: string): string {
  if (!envUrl) return production;
  return dev || envUrl.startsWith("https://") ? envUrl : production;
}
