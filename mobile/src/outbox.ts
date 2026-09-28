import { strings } from "@web/strings";
import { Directory, File, Paths } from "expo-file-system";
import * as SQLite from "expo-sqlite";
import { useEffect, useState } from "react";

import * as api from "./api";
import { noteSent } from "./lastSaved";
import { reportError } from "./monitoring";
import { clearListsKeepingDrafts } from "./handoverDraft";
import { rebasePhoto } from "./photoPath";
import { coalesce, PhotoMissingError, rejectsRow, retryNote, skipsToNext, stuckOnServer } from "./outboxRules";

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

/** Adresa unei poze ținute pe disc, mutată sub containerul de acum al aplicației (`photoPath.ts`, B2). */
export function localPhoto(uri: string | null) {
  return rebasePhoto(uri, Paths.document.uri);
}

/** Mai e poza pe disc? O adresă stricată e socotită lipsă. */
export function photoExists(uri: string) {
  try {
    return new File(uri).exists;
  } catch {
    return false;
  }
}

function fromRow(r: Row): OutboxItem {
  return {
    id: r.id,
    owner: r.owner,
    tenantId: r.tenant_id,
    payload: JSON.parse(r.payload),
    photoUri: localPhoto(r.photo_uri),
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
  return useOutboxState(owner).items;
}

/**
 * Aceleași rânduri, cu semnul că prima citire s-a făcut: bonul de după „Salvează” nu poate spune „în
 * registru” doar fiindcă lista încă n-a venit de pe disc.
 */
export function useOutboxState(owner: string | undefined) {
  const [state, setState] = useState<{ items: OutboxItem[]; loaded: boolean }>({ items: [], loaded: false });
  useEffect(() => {
    if (!owner) return setState({ items: [], loaded: true });
    const load = () =>
      list(owner)
        .then((items) => setState({ items, loaded: true }))
        .catch(() => {});
    load();
    listeners.add(load);
    return () => {
      listeners.delete(load);
    };
  }, [owner]);
  return state;
}

// ── scrierea ─────────────────────────────────────────────────────────────────

/**
 * Poza stă în folderul documentelor aplicației, nu în cache: sistemul golește cache-ul când are
 * nevoie de loc, iar o predare care așteaptă semnal până mâine și-ar fi pierdut dovada.
 */
function keepPhoto(id: string, uri: string): string {
  const dir = new Directory(Paths.document, "outbox");
  if (!dir.exists) dir.create();
  const source = new File(uri);
  // Poza unei ciorne vechi poate să fi plecat din cache: formularul o scoate și spune de ce (B3).
  if (!source.exists) throw new PhotoMissingError();
  const target = new File(dir, `${id}.jpg`);
  if (target.exists) target.delete();
  source.copy(target);
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

/** Un rând al contului, pentru „Corectează” pe o predare refuzată. */
export async function get(id: string, owner: string): Promise<OutboxItem | null> {
  const row = await (await db()).getFirstAsync<Row>("SELECT * FROM outbox WHERE id = ? AND owner = ?", id, owner);
  return row ? fromRow(row) : null;
}

/**
 * Predarea refuzată, corectată de om: aceeași cheie (deci tot o singură predare pe server), cererea nouă,
 * iar rândul redevine „de trimis” de la zero. Poza rămâne cea de pe rând.
 */
export async function resubmit(id: string, payload: Record<string, unknown>, summary: OutboxSummary) {
  await (
    await db()
  ).runAsync(
    `UPDATE outbox SET payload = ?, summary = ?, state = 'PENDING', attempts = 0, next_at = 0, error = NULL
     WHERE id = ? AND movement_id IS NULL`,
    JSON.stringify({ ...payload, clientGeneratedId: id }),
    JSON.stringify(summary),
    id,
  );
  changed();
}

/** Scoaterea unui rând refuzat, la cererea omului. */
export async function remove(id: string) {
  const d = await db();
  const row = await d.getFirstAsync<Row>("SELECT * FROM outbox WHERE id = ?", id);
  await d.runAsync("DELETE FROM outbox WHERE id = ?", id);
  dropPhoto(localPhoto(row?.photo_uri ?? null));
  changed();
}

// ── trimiterea ───────────────────────────────────────────────────────────────

/** 5 s, 10 s, 20 s … până la zece minute. */
export function backoffMs(attempts: number) {
  return Math.min(5_000 * 2 ** Math.max(attempts - 1, 0), 10 * 60_000);
}

/** `fetch` din React Native cade cu `TypeError: Network request failed` când nu e semnal. */
function isNetworkFailure(error: unknown) {
  return error instanceof TypeError && /network request failed/i.test(error.message);
}

/** Propoziția de pe un rând refuzat: a serverului, sau „poza nu mai e pe telefon”. */
function rejectionNote(error: unknown) {
  if (error instanceof PhotoMissingError) return strings.mobile.outboxPhotoMissing;
  const e = error as api.ApiError;
  return e.serverMessage ?? `HTTP ${e.status}`;
}

let latest: { auth: api.Auth; owner: string } | null = null;
const drainOnce = coalesce(() => run(latest!.auth, latest!.owner));

/**
 * Trimite ce se poate. Întoarce câte predări au ajuns pe server, ca ecranele să-și reîmprospăteze
 * cifrele. O singură trimitere în aer: rețeaua revenită, aplicația adusă în față și butonul pot
 * cere toate în aceeași clipă, iar două trimiteri paralele ar fi urcat aceeași poză de două ori.
 * O cerere venită cât una e în aer primește o trecere nouă, după ea (B6): „Trimite acum” scoate rândurile
 * din pauză, iar trecerea veche își citise lista dinainte.
 */
export function drain(auth: api.Auth, owner: string): Promise<number> {
  latest = { auth, owner };
  return drainOnce();
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
        noteSent(item.id, movementId);
        await d.runAsync(
          "UPDATE outbox SET movement_id = ?, attempts = 0, error = NULL WHERE id = ?",
          movementId,
          item.id,
        );
        sent++;
      }
      // Cheia rândului e și cheia pozei (V67): o reîncercare după un răspuns pierdut nu mai dublează poza.
      if (item.photoUri) {
        if (!photoExists(item.photoUri)) throw new PhotoMissingError();
        await api.uploadAttachment(itemAuth, movementId, item.photoUri, item.id);
      }
      await d.runAsync("DELETE FROM outbox WHERE id = ?", item.id);
      dropPhoto(item.photoUri);
      changed();
    } catch (error) {
      if (error instanceof api.UnauthorizedError) break; // omul iese din cont; rândul așteaptă
      if (rejectsRow(error)) {
        // `movement_id` e scris deja dacă a căzut numai poza: ecranul spune atunci că predarea e pe server.
        await d.runAsync(
          "UPDATE outbox SET state = 'REJECTED', error = ?, movement_id = ? WHERE id = ?",
          rejectionNote(error),
          movementId,
          item.id,
        );
        changed();
        continue;
      }
      // O eroare care nu e nici de rețea, nici a serverului e a noastră (cum a fost FormData pe 16.09):
      // se reîncearcă tot, dar se raportează o dată, la prima cădere, nu la fiecare pauză.
      if (item.attempts === 0 && !(error instanceof api.ApiError) && !isNetworkFailure(error)) {
        reportError(error, { where: "outbox", step: movementId ? "photo" : "movement" });
      }
      const attempts = item.attempts + 1;
      if (stuckOnServer(error, attempts)) {
        reportError(error, { where: "outbox", step: movementId ? "photo" : "movement", attempts });
      }
      // Propoziția serverului pe rând (5xx, 429); fără semnal rândul rămâne fără cuvânt.
      await d.runAsync(
        "UPDATE outbox SET attempts = ?, next_at = ?, error = ? WHERE id = ?",
        attempts,
        Date.now() + backoffMs(attempts),
        retryNote(error),
        item.id,
      );
      changed();
      // Rândul ăsta cade (5xx, o eroare a telefonului): celelalte pleacă. Fără rețea n-are rost acum.
      if (skipsToNext(error)) continue;
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
 * La ieșirea din cont: listele ținute pentru lucrul fără semnal (parteneri, șoferi, profilul fiecărei firme
 * deschise de un consultant) nu rămân pe telefon pentru următorul. Coada rămâne: rândurile sunt ale
 * contului care le-a salvat și pleacă numai în numele lui, iar o predare netrimisă nu se pierde la o ieșire.
 */
export async function clearCache() {
  // Ciorna rămâne: e a contului care a început-o și o reia numai el (B5).
  await clearListsKeepingDrafts(await db());
}

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
