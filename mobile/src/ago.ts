/**
 * Vârsta unei cifre: „actualizat acum”, „acum 3 min”, „acum 2 ore”, „ieri”. Cifra de pe ecran are o
 * vârstă și omul o vede — altfel un total de ieri arată ca unul de acum (propunerea de refresh, §3.8).
 *
 * <p>Întoarce felul și numărul, nu propoziția: textul stă în `strings.ts`, iar aici nu se poate
 * importa (`node --test` nu știe aliasurile). `null` = nu s-a încărcat niciodată.
 */
export type Ago = { kind: "now" } | { kind: "minutes"; n: number } | { kind: "hours"; n: number } | { kind: "days"; n: number };

export function ago(updatedAt: number, now = Date.now()): Ago | null {
  if (!updatedAt) return null;
  const seconds = Math.max(0, Math.floor((now - updatedAt) / 1000));
  if (seconds < 60) return { kind: "now" };
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return { kind: "minutes", n: minutes };
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return { kind: "hours", n: hours };
  return { kind: "days", n: Math.floor(hours / 24) };
}
