// Ciorna predării (F4): `npm run test:draft`. Baza e falsă — aceeași interfață ca SQLite-ul din `outbox.ts`.
import assert from "node:assert/strict";
import { test } from "node:test";

import { clearDraft, clearListsKeepingDrafts, draftKey, loadDraft, saveDraft, type HandoverDraft } from "./handoverDraft.ts";

function fakeDb() {
  const rows = new Map<string, string>();
  return {
    rows,
    async runAsync(sql: string, ...args: unknown[]) {
      if (sql.startsWith("INSERT")) rows.set(String(args[0]), String(args[1]));
      else if (sql.startsWith("DELETE")) rows.delete(String(args[0]));
    },
    async getFirstAsync<T>(_sql: string, key: unknown): Promise<T | null> {
      const value = rows.get(String(key));
      return value == null ? null : ({ value } as T);
    },
  };
}

const draft: HandoverDraft = {
  savedAt: 1_000,
  step: 2,
  photo: "file:///tmp/aviz.jpg",
  fields: { wasteCodeId: "wc-1", quantity: "1250", unit: "KG" },
  pending: ["partner"],
  summary: { wasteCode: "15 01 02", quantity: "1.250 kg" },
};

test("cheia poartă contul și firma: consultantul nu reia ciorna altui client", () => {
  assert.equal(draftKey("ana@cabinet.ro", "t-1"), "draft:ana@cabinet.ro:t-1");
  assert.notEqual(draftKey("ana@cabinet.ro", "t-1"), draftKey("ana@cabinet.ro", "t-2"));
});

test("ciorna se scrie, se citește la fel și se șterge", async () => {
  const db = fakeDb();
  await saveDraft(db, "a@b.ro", "t-1", draft);
  assert.deepEqual(await loadDraft(db, "a@b.ro", "t-1"), draft);
  assert.equal(await loadDraft(db, "a@b.ro", "t-2"), null);
  await clearDraft(db, "a@b.ro", "t-1");
  assert.equal(await loadDraft(db, "a@b.ro", "t-1"), null);
});

test("o ciornă stricată pe disc nu strică ecranul: e ca și cum n-ar fi", async () => {
  const db = fakeDb();
  db.rows.set(draftKey("a@b.ro", "t-1"), "{nu e json");
  assert.equal(await loadDraft(db, "a@b.ro", "t-1"), null);
  db.rows.set(draftKey("a@b.ro", "t-1"), JSON.stringify({ step: 9 }));
  assert.equal(await loadDraft(db, "a@b.ro", "t-1"), null);
});

// B5 (28.09.2026): la ieșirea din cont se golesc listele ținute fără semnal, dar ciorna rămâne, pe contul ei.
test("ieșirea din cont golește listele, dar păstrează ciornele", async () => {
  const { DatabaseSync } = await import("node:sqlite");
  const raw = new DatabaseSync(":memory:");
  raw.exec("CREATE TABLE cache (key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL, saved_at INTEGER NOT NULL)");
  const db = {
    async runAsync(sql: string, ...args: unknown[]) {
      raw.prepare(sql).run(...(args as never[]));
    },
    async getFirstAsync<T>(sql: string, ...args: unknown[]): Promise<T | null> {
      return (raw.prepare(sql).get(...(args as never[])) as T | undefined) ?? null;
    },
  };
  await saveDraft(db, "a@b.ro", "t-1", draft);
  await db.runAsync("INSERT INTO cache (key, value, saved_at) VALUES (?, ?, ?)", "partners:t-1", "[]", 1);
  await clearListsKeepingDrafts(db);
  assert.deepEqual(await loadDraft(db, "a@b.ro", "t-1"), draft, "ciorna rămâne");
  assert.equal(await db.getFirstAsync("SELECT value FROM cache WHERE key = ?", "partners:t-1"), null, "listele pleacă");
});
