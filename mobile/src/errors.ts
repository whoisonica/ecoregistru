/**
 * Textul de sub butonul de login, ca pe web (`apiErrorMessage` în `LoginPage`): propoziția serverului
 * când a scris una — cont oprit, cont neactivat, prea multe încercări —, altfel textul general. Înainte
 * orice răspuns al serverului dădea același „datele nu se potrivesc”, și omul blocat 15 minute tot
 * încerca parola. Fără răspuns: n-am ajuns la server. Forma erorii e cea a lui `ApiError` (`api.ts`).
 */
export function loginErrorText(error: unknown, texts: { generic: string; unreachable: string }): string {
  const e = error as { status?: unknown; serverMessage?: string | null } | null;
  if (typeof e?.status !== "number") return texts.unreachable;
  return e.serverMessage || texts.generic;
}
