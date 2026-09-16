import * as Device from "expo-device";
import * as SecureStore from "expo-secure-store";
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { Platform } from "react-native";

import * as api from "./api";
import type { AuthResponse } from "./auth";

// Tokenul stă în Keychain / Keystore, niciodată în AsyncStorage (todo-mobil §5).
const SESSION_KEY = "wh_session";

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
  const save = useCallback(async (next: Stored | null) => {
    latest.current = next;
    setStored(next);
    if (next) await SecureStore.setItemAsync(SESSION_KEY, JSON.stringify(next));
    else await SecureStore.deleteItemAsync(SESSION_KEY);
  }, []);

  useEffect(() => {
    SecureStore.getItemAsync(SESSION_KEY)
      .then((raw) => {
        const parsed = raw ? (JSON.parse(raw) as Stored) : null;
        latest.current = parsed;
        setStored(parsed);
      })
      .catch(() => setStored(null))
      .finally(() => setReady(true));
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
      const current = latest.current;
      if (!current?.auth.refreshToken) return null;
      try {
        const fresh = await api.refreshSession(current.auth.refreshToken);
        await save({ ...current, auth: fresh });
        return fresh.token;
      } catch {
        await save(null);
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
    if (token) await api.logout(token).catch(() => {});
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
