import { useCallback, useEffect, useState } from "react";

import { clearDraft, loadDraft, saveDraft, type HandoverDraft } from "./handoverDraft";
import { db } from "./outbox";

const listeners = new Set<() => void>();
function changed() {
  listeners.forEach((fn) => fn());
}

/** Ciorna contului pe firma deschisă, ținută la zi pe ecran („Adaugă” o arată, formularul o scrie). */
export function useDraft(owner: string | undefined, tenantId: string | undefined) {
  const [draft, setDraft] = useState<HandoverDraft | null>(null);
  useEffect(() => {
    if (!owner || !tenantId) return setDraft(null);
    const load = () =>
      db()
        .then((d) => loadDraft(d, owner, tenantId))
        .then(setDraft)
        .catch(() => {});
    load();
    listeners.add(load);
    return () => {
      listeners.delete(load);
    };
  }, [owner, tenantId]);
  const discard = useCallback(async () => {
    if (!owner || !tenantId) return;
    await clearDraft(await db(), owner, tenantId);
    changed();
  }, [owner, tenantId]);
  return { draft, discard };
}

export async function writeDraft(owner: string, tenantId: string, draft: HandoverDraft) {
  await saveDraft(await db(), owner, tenantId, draft);
  changed();
}

export async function dropDraft(owner: string, tenantId: string) {
  await clearDraft(await db(), owner, tenantId);
  changed();
}
