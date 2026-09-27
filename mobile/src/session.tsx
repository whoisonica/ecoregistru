import { useQueryClient } from "@tanstack/react-query";
import * as Device from "expo-device";
import { File, Paths } from "expo-file-system";
import * as SecureStore from "expo-secure-store";
import * as SQLite from "expo-sqlite";
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { Platform } from "react-native";

import * as api from "./api";
import type { AuthResponse } from "./auth";
import { clearCache } from "./outbox";
import { afterRefresh, keepPendingLogout, sessionIsDead } from "./refresh";

// Tokenul stă în Keychain / Keystore, niciodată în AsyncStorage (todo-mobil §5).
const SESSION_KEY = "wh_session";
/** Ieșirile din cont care n-au ajuns la server (fără semnal): se trimit din nou la pornire. */
const PENDING_LOGOUT_KEY = "wh_logout_pending";

async function pendingLogouts(): Promise<string[]> {
  try {
    return JSON.parse((await SecureStore.getItemAsync(PENDING_LOGOUT_KEY)) ?? "[]") as string[];
  } catch {
    return [];
  }
}

/** Stinge sesiunea pe server; fără răspuns, o ține minte pentru data viitoare (`keepPendingLogout`). */
async function logoutOnServer(refreshToken: string) {
  const error = await api.logout(refreshToken).then(() => null, (e: unknown) => e ?? new Error("logout"));
  if (!keepPendingLogout(error)) return;
  const pending = await pendingLogouts();
  if (!pending.includes(refreshToken)) pending.push(refreshToken);
  await SecureStore.setItemAsync(PENDING_LOGOUT_KEY, JSON.stringify(pending.slice(-10)));
}

/** Ieșirile rămase de data trecută. Fiecare rămâne în listă numai dacă iar n-a primit răspuns. */
async function flushPendingLogouts() {
  const pending = await pendingLogouts();
  if (!pending.length) return;
  const left: string[] = [];
  for (const token of pending) {
    const error = await api.logout(token).then(() => null, (e: unknown) => e ?? new Error("logout"));
    if (keepPendingLogout(error)) left.push(token);
  }
  if (left.length) await SecureStore.setItemAsync(PENDING_LOGOUT_KEY, JSON.stringify(left));
  else await SecureStore.deleteItemAsync(PENDING_LOGOUT_KEY);
}

/**
 * Pe iPhone, Keychain-ul supraviețuiește dezinstalării: fără asta, o aplicație reinstalată (sau telefonul
 * dat altcuiva după ștergerea aplicației) pornea direct în contul vechi. Semnul e un fișier în folderul
 * aplicației, care pleacă odată cu ea; lipsa lui la pornire înseamnă o instalare nouă.
 */
async function forgetSessionFromPreviousInstall() {
  const marker = new File(Paths.document, "instalare");
  if (marker.exists) return;
  // Aplicațiile instalate înaintea semnului au deja baza cozii: acolo e o actualizare, nu o reinstalare,
  // iar omul rămâne în cont.
  const updated = new File(SQLite.defaultDatabaseDirectory, "wastehouse.db").exists;
  if (!updated) {
    await SecureStore.deleteItemAsync(SESSION_KEY);
    await SecureStore.deleteItemAsync(PENDING_LOGOUT_KEY);
  }
  marker.create();
}

/**
 * Firma pe care lucrează sesiunea. Separată de {@link AuthResponse} fiindcă pe ea o schimbă
 * consultantul din comutator, iar reîmprospătarea rescrie restul sesiunii cu ce spune serverul —
 * dacă ar sta în același obiect, prima reîmprospătare de dimineață l-ar fi trimis înapoi pe prima firmă.
 */
interface Stored {
  auth: AuthResponse;
  tenantId: string | null;
  tenantName: string | null;
}

interface SessionValue {
  session: (AuthResponse & { tenantId: string | null; tenantName: string | null }) | null;
  /** Ce se dă fiecărei cereri: tokenul și firma. Null cât timp nu e nimeni logat. */
  auth: api.Auth | null;
  ready: boolean;
  signIn: (email: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
  switchTenant: (id: string, name: string) => Promise<void>;
}

const SessionContext = createContext<SessionValue | undefined>(undefined);

/** Ce scrie în „Dispozitive conectate”: „iPhone 17”, „Pixel 8”, sau măcar sistemul. */
function deviceName() {
  return Device.modelName ?? (Platform.OS === "ios" ? "iPhone" : "Telefon Android");
}

export function SessionProvider({ children }: { children: ReactNode }) {
  const [stored, setStored] = useState<Stored | null>(null);
  const [ready, setReady] = useState(false);

  // Citit de reîmprospătare, care trăiește în afara randării și n-are cum să vadă starea de React.
  const latest = useRef<Stored | null>(null);
  const queryClient = useQueryClient();
  const save = useCallback(
    async (next: Stored | null) => {
      latest.current = next;
      setStored(next);
      if (next) return SecureStore.setItemAsync(SESSION_KEY, JSON.stringify(next));
      // Ce a încărcat omul care iese nu rămâne pentru următorul de pe același telefon: `["devices"]` și
      // `["companies"]` nu-l poartă în cheie (ca pe web, „Cache la deconectare”, 17.09.2026).
      queryClient.clear();
      await SecureStore.deleteItemAsync(SESSION_KEY);
      // La fel listele ținute pentru lucrul fără semnal și documentele descărcate.
      await clearCache().catch(() => {});
      try {
        api.clearDocuments();
      } catch {
        // un fișier ținut deschis de foaia de partajare; pleacă la următoarea ieșire
      }
    },
    [queryClient]
  );

  useEffect(() => {
    forgetSessionFromPreviousInstall()
      .catch(() => {})
      .then(() => SecureStore.getItemAsync(SESSION_KEY))
      .then((raw) => {
        const parsed = raw ? (JSON.parse(raw) as Stored) : null;
        latest.current = parsed;
        setStored(parsed);
      })
      .catch(() => setStored(null))
      .finally(() => {
        setReady(true);
        flushPendingLogouts().catch(() => {});
      });
  }, []);

  /**
   * G1 — ce face aplicația când tokenul de acces a murit peste noapte. Merge la server cu sesiunea
   * lungă; dacă și ea e moartă (revocată din Setări, cont oprit, 60 de zile în sertar), omul iese
   * din cont — o dată, cu parola, nu la fiecare tură.
   *
   * <p>Firma aleasă se păstrează peste reîmprospătare; restul vine de la server, fiindcă rolul unui
   * om se poate schimba între două deschideri ale aplicației.
   */
  useEffect(() => {
    api.setRefresher(async () => {
      const before = latest.current;
      if (!before?.auth.refreshToken) return null;
      try {
        const fresh = await api.refreshSession(before.auth.refreshToken);
        // Sesiunea de acum, nu cea de la pornirea cererii: firma aleasă între timp rămâne, iar un om
        // ieșit între timp rămâne afară — tokenul nou, al nimănui, se stinge pe server (`afterRefresh`).
        const next = afterRefresh(before, latest.current, fresh);
        if (!next) {
          if (fresh.refreshToken) await logoutOnServer(fresh.refreshToken);
          return null;
        }
        await save(next);
        return fresh.token;
      } catch (error) {
        // Fără semnal sau cu serverul căzut, eroarea urcă la cerere, iar ecranul spune „fără
        // legătură” în loc să scoată omul din cont (`refresh.ts`).
        if (!sessionIsDead(error)) throw error;
        // Scoate din cont numai sesiunea refuzată, nu una intrată între timp.
        if (latest.current?.auth.refreshToken === before.auth.refreshToken) await save(null);
        return null;
      }
    });
    return () => api.setRefresher(null);
  }, [save]);

  const signIn = useCallback(
    async (email: string, password: string) => {
      const auth = await api.login(
        email.trim(),
        password,
        deviceName(),
        Platform.OS === "ios" ? "IOS" : "ANDROID"
      );
      await save({ auth, tenantId: auth.tenantId, tenantName: auth.tenantName });
    },
    [save]
  );

  const signOut = useCallback(async () => {
    const token = latest.current?.auth.refreshToken;
    // Mai întâi Keychain-ul: dacă serverul nu răspunde, omul tot trebuie să poată ieși de pe telefon.
    await save(null);
    if (token) await logoutOnServer(token);
  }, [save]);

  const switchTenant = useCallback(
    async (id: string, name: string) => {
      const current = latest.current;
      if (current) await save({ ...current, tenantId: id, tenantName: name });
    },
    [save]
  );

  const value = useMemo<SessionValue>(
    () => ({
      session: stored
        ? { ...stored.auth, tenantId: stored.tenantId, tenantName: stored.tenantName }
        : null,
      auth: stored ? { token: stored.auth.token, tenantId: stored.tenantId } : null,
      ready,
      signIn,
      signOut,
      switchTenant,
    }),
    [stored, ready, signIn, signOut, switchTenant]
  );
  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession() {
  const ctx = useContext(SessionContext);
  if (!ctx) throw new Error("useSession în afara SessionProvider");
  return ctx;
}
