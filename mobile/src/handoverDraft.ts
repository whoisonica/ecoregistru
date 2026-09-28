/**
 * Ciorna predării (F4, 27.09.2026): un formular închis pe la mijloc — a sunat cineva, a venit camionul
 * următor — se păstrează pe telefon și se reia de pe „Adaugă” („Continui predarea de la 14:20?”).
 * Stă în tabela `cache` din SQLite (aceeași ca listele fără semnal), o singură ciornă pe cont și firmă.
 *
 * <p>Numai regulile pure sunt aici, ca să se probeze fără telefon; baza intră ca parametru (interfața
 * pe care o are și SQLite-ul din `outbox.ts`). Hook-ul pentru ecran e în `useDraft.ts`.
 */
export interface HandoverDraft {
  savedAt: number;
  step: 1 | 2 | 3;
  photo: string | null;
  /** Stările formularului, cu numele lor din `predare.tsx`; ce lipsește rămâne pe valoarea de pornire. */
  fields: Record<string, unknown>;
  /** Rubricile citite din poză și încă neconfirmate. */
  pending: string[];
  /** Pentru cardul de pe „Adaugă”: ce se vede fără să deschizi formularul. */
  summary: { wasteCode: string | null; quantity: string };
}

export interface DraftDb {
  runAsync(sql: string, ...args: unknown[]): Promise<unknown>;
  getFirstAsync<T>(sql: string, ...args: unknown[]): Promise<T | null>;
}

export function draftKey(owner: string, tenantId: string) {
  return `draft:${owner}:${tenantId}`;
}

export async function saveDraft(db: DraftDb, owner: string, tenantId: string, draft: HandoverDraft) {
  await db.runAsync(
    "INSERT OR REPLACE INTO cache (key, value, saved_at) VALUES (?, ?, ?)",
    draftKey(owner, tenantId),
    JSON.stringify(draft),
    draft.savedAt,
  );
}

/** `null` când nu e nimic sau ce e pe disc nu mai e o ciornă (JSON stricat, forma veche). */
export async function loadDraft(db: DraftDb, owner: string, tenantId: string): Promise<HandoverDraft | null> {
  const row = await db.getFirstAsync<{ value: string }>("SELECT value FROM cache WHERE key = ?", draftKey(owner, tenantId));
  if (!row) return null;
  try {
    const parsed = JSON.parse(row.value) as Partial<HandoverDraft>;
    if (![1, 2, 3].includes(parsed.step as number) || typeof parsed.fields !== "object" || !parsed.fields) return null;
    return parsed as HandoverDraft;
  } catch {
    return null;
  }
}

export async function clearDraft(db: DraftDb, owner: string, tenantId: string) {
  await db.runAsync("DELETE FROM cache WHERE key = ?", draftKey(owner, tenantId));
}
