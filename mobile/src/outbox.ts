import { Directory, File, Paths } from "expo-file-system";
import * as SQLite from "expo-sqlite";
import { useEffect, useState } from "react";

import * as api from "./api";

/**
 * M1b — coada predărilor care n-au plecat încă. Rampa n-are semnal; predarea se salvează pe telefon
 * și pleacă singură când se poate.
 *
 * <p>Reguli, fiecare cu motivul ei (todo-mobil §5, §10):
 * - **`clientGeneratedId` se dă o dată, la „Salvează”**, și e cheia rândului. La fiecare reîncercare
 *   pleacă același, deci dacă serverul a primit cererea dar răspunsul s-a pierdut, a doua trimitere
 *   îi întoarce predarea deja făcută (`WasteMovementService.create`), nu una nouă.
 * - **Poza pleacă după ce predarea are id**, într-un pas separat. Id-ul primit se scrie în rând înainte
 *   de poză, ca o poză căzută să nu retrimită predarea.
 * - **Un refuz al serverului (4xx) nu se reîncearcă.** Rândul rămâne „refuzat”, cu propoziția
 *   serverului, până îl scoate omul: aceeași cerere ar fi refuzată la nesfârșit.
 * - **Fără rețea, 5xx, 429: se reîncearcă**, cu pauze tot mai lungi, până la zece minute.
 * - **Un rând e al contului care l-a salvat.** Dacă pe telefon intră alt cont, rândurile rămân pe loc
 *   și nu pleacă în numele lui.
 * - **Data predării e cea aleasă de om**, scrisă în cerere la salvare; ora trimiterii nu contează.
 */

export type OutboxState = "PENDING" | "REJECTED";

/** Ce arată lista „De trimis”, fără să citească cererea. */
export interface OutboxSummary {
  wasteCode: string;
  quantity: string;
  partnerName: string | null;
  date: string;
}

export interface OutboxItem {
  id: string;
  owner: string;
  tenantId: string | null;
  payload: Record<string, unknown>;
  photoUri: string | null;
  movementId: string | null;
  state: OutboxState;
  attempts: number;
  nextAt: number;
  error: string | null;
  summary: OutboxSummary;
  createdAt: number;
}

let dbPromise: Promise<SQLite.SQLiteDatabase> | null = null;

export function db() {
  dbPromise ??= SQLite.openDatabaseAsync("wastehouse.db").then(async (d) => {
    await d.execAsync(`
      CREATE TABLE IF NOT EXISTS outbox (
        id TEXT PRIMARY KEY NOT NULL,
        owner TEXT NOT NULL,
        tenant_id TEXT,
        payload TEXT NOT NULL,
        photo_uri TEXT,
        movement_id TEXT,
        state TEXT NOT NULL DEFAULT 'PENDING',
        attempts INTEGER NOT NULL DEFAULT 0,
        next_at INTEGER NOT NULL DEFAULT 0,
        error TEXT,
        summary TEXT NOT NULL,
        created_at INTEGER NOT NULL
      );
      CREATE TABLE IF NOT EXISTS cache (
        key TEXT PRIMARY KEY NOT NULL,
        value TEXT NOT NULL,
        saved_at INTEGER NOT NULL
      );
    `);
    return d;
  });
  return dbPromise;
}

// ── cine ascultă ─────────────────────────────────────────────────────────────

const listeners = new Set<() => void>();
function changed() {
  listeners.forEach((l) => l());
}

interface Row {
  id: string;
  owner: string;
  tenant_id: string | null;
  payload: string;
  photo_uri: string | null;
  movement_id: string | null;
  state: OutboxState;
  attempts: number;
  next_at: number;
  error: string | null;
  summary: string;
  created_at: number;
}

function fromRow(r: Row): OutboxItem {
  return {
    id: r.id,
    owner: r.owner,
    tenantId: r.tenant_id,
    payload: JSON.parse(r.payload),
    photoUri: r.photo_uri,
    movementId: r.movement_id,
    state: r.state,
    attempts: r.attempts,
    nextAt: r.next_at,
    error: r.error,
    summary: JSON.parse(r.summary),
    createdAt: r.created_at,
  };
}

export async function list(owner: string): Promise<OutboxItem[]> {
  const rows = await (await db()).getAllAsync<Row>("SELECT * FROM outbox WHERE owner = ? ORDER BY created_at", owner);
  return rows.map(fromRow);
}

/** Rândurile contului, ținute la zi pe ecran. */
export function useOutbox(owner: string | undefined) {
  const [items, setItems] = useState<OutboxItem[]>([]);
  useEffect(() => {
    if (!owner) return setItems([]);
    const load = () =>
      list(owner)
        .then(setItems)
        .catch(() => {});
    load();
    listeners.add(load);
    return () => {
      listeners.delete(load);
    };
  }, [owner]);
  return items;
}

// ── scrierea ─────────────────────────────────────────────────────────────────

/**
 * Poza stă în folderul documentelor aplicației, nu în cache: sistemul golește cache-ul când are
 * nevoie de loc, iar o predare care așteaptă semnal până mâine și-ar fi pierdut dovada.
 */
function keepPhoto(id: string, uri: string): string {
  const dir = new Directory(Paths.document, "outbox");
  if (!dir.exists) dir.create();
  const target = new File(dir, `${id}.jpg`);
  if (target.exists) target.delete();
  new File(uri).copy(target);
  return target.uri;
}

function dropPhoto(uri: string | null) {
  if (!uri) return;
  try {
    const f = new File(uri);
    if (f.exists) f.delete();
  } catch {
    // O poză care nu mai e pe disc nu trebuie să oprească nimic.
  }
}

export async function enqueue(item: {
  id: string;
  owner: string;
  tenantId: string | null;
  payload: Record<string, unknown>;
  photoUri: string | null;
  summary: OutboxSummary;
}) {
  const photo = item.photoUri ? keepPhoto(item.id, item.photoUri) : null;
  await (
    await db()
  ).runAsync(
    `INSERT INTO outbox (id, owner, tenant_id, payload, photo_uri, summary, created_at)
     VALUES (?, ?, ?, ?, ?, ?, ?)`,
    item.id,
    item.owner,
    item.tenantId,
    JSON.stringify({ ...item.payload, clientGeneratedId: item.id }),
    photo,
    JSON.stringify(item.summary),
    Date.now(),
  );
  changed();
}

/** Scoaterea unui rând refuzat, la cererea omului. */
export async function remove(id: string) {
  const d = await db();
  const row = await d.getFirstAsync<Row>("SELECT * FROM outbox WHERE id = ?", id);
  await d.runAsync("DELETE FROM outbox WHERE id = ?", id);
  dropPhoto(row?.photo_uri ?? null);
  changed();
}

// ── trimiterea ───────────────────────────────────────────────────────────────

/** 5 s, 10 s, 20 s … până la zece minute. */
export function backoffMs(attempts: number) {
  return Math.min(5_000 * 2 ** Math.max(attempts - 1, 0), 10 * 60_000);
}

/** Refuzul serverului se păstrează; restul (rețea, 5xx, 429) e de reîncercat. */
function isRejection(error: unknown): error is api.ApiError {
  return error instanceof api.ApiError && error.status >= 400 && error.status < 500 && error.status !== 429;
}

let draining: Promise<number> | null = null;

/**
 * Trimite ce se poate. Întoarce câte predări au ajuns pe server, ca ecranele să-și reîmprospăteze
 * cifrele. O singură trimitere în aer: rețeaua revenită, aplicația adusă în față și butonul pot
 * cere toate în aceeași clipă, iar două trimiteri paralele ar fi urcat aceeași poză de două ori.
 */
export function drain(auth: api.Auth, owner: string): Promise<number> {
  draining ??= run(auth, owner).finally(() => {
    draining = null;
  });
  return draining;
}

async function run(auth: api.Auth, owner: string): Promise<number> {
  const d = await db();
  const rows = await d.getAllAsync<Row>(
    "SELECT * FROM outbox WHERE owner = ? AND state = 'PENDING' AND next_at <= ? ORDER BY created_at",
    owner,
    Date.now(),
  );
  let sent = 0;
  for (const item of rows.map(fromRow)) {
    // Firma e a rândului, nu a sesiunii: consultantul poate fi trecut între timp pe alt client.
    const itemAuth = { ...auth, tenantId: item.tenantId };
    let movementId = item.movementId;
    try {
      if (!movementId) {
        movementId = (await api.createMovement(itemAuth, item.payload)).id;
        await d.runAsync(
          "UPDATE outbox SET movement_id = ?, attempts = 0, error = NULL WHERE id = ?",
          movementId,
          item.id,
        );
        sent++;
      }
      if (item.photoUri) await api.uploadAttachment(itemAuth, movementId, item.photoUri);
      await d.runAsync("DELETE FROM outbox WHERE id = ?", item.id);
      dropPhoto(item.photoUri);
      changed();
    } catch (error) {
      if (error instanceof api.UnauthorizedError) break; // omul iese din cont; rândul așteaptă
      if (isRejection(error)) {
        // `movement_id` e scris deja dacă a căzut numai poza: ecranul spune atunci că predarea e pe server.
        await d.runAsync(
          "UPDATE outbox SET state = 'REJECTED', error = ?, movement_id = ? WHERE id = ?",
          error.serverMessage ?? `HTTP ${error.status}`,
          movementId,
          item.id,
        );
        changed();
        continue;
      }
      const attempts = item.attempts + 1;
      await d.runAsync(
        "UPDATE outbox SET attempts = ?, next_at = ? WHERE id = ?",
        attempts,
        Date.now() + backoffMs(attempts),
        item.id,
      );
      changed();
      // Fără rețea n-are rost să le încercăm și pe celelalte acum.
      break;
    }
  }
  return sent;
}

/** Cât timp a trecut peste pauza unui rând: butonul „Trimite acum” o sare. */
export async function retryNow(owner: string) {
  await (await db()).runAsync("UPDATE outbox SET next_at = 0 WHERE owner = ? AND state = 'PENDING'", owner);
}

// ── cache-ul listelor ────────────────────────────────────────────────────────

/**
 * Listele de care are nevoie formularul fără semnal: punctele de lucru, partenerii, profilul firmei,
 * ultimele predări. Se iau de la server când se poate și se țin aici pentru când nu.
 *
 * <p>Cheia poartă firma, ca un consultant să nu vadă partenerii altui client pe formular.
 */
export async function cached<T>(key: string, fetcher: () => Promise<T>): Promise<T> {
  const d = await db();
  try {
    const value = await fetcher();
    await d.runAsync(
      "INSERT OR REPLACE INTO cache (key, value, saved_at) VALUES (?, ?, ?)",
      key,
      JSON.stringify(value),
      Date.now(),
    );
    return value;
  } catch (error) {
    // Un refuz sau o sesiune moartă nu se ascunde sub o listă veche; numai lipsa rețelei.
    if (error instanceof api.ApiError || error instanceof api.UnauthorizedError) throw error;
    const row = await d.getFirstAsync<{ value: string }>("SELECT value FROM cache WHERE key = ?", key);
    if (row) return JSON.parse(row.value) as T;
    throw error;
  }
}
