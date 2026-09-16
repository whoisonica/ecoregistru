import * as SecureStore from "expo-secure-store";
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";

import * as api from "./api";
import type { AuthResponse } from "./auth";

// Tokenul stă în Keychain / Keystore, niciodată în AsyncStorage (todo-mobil §5).
const SESSION_KEY = "wh_session";

interface SessionValue {
  session: AuthResponse | null;
  ready: boolean;
  signIn: (email: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
}

const SessionContext = createContext<SessionValue | undefined>(undefined);

export function SessionProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthResponse | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    SecureStore.getItemAsync(SESSION_KEY)
      .then((raw) => setSession(raw ? (JSON.parse(raw) as AuthResponse) : null))
      .catch(() => setSession(null))
      .finally(() => setReady(true));
  }, []);

  const signIn = useCallback(async (email: string, password: string) => {
    const res = await api.login(email.trim(), password);
    await SecureStore.setItemAsync(SESSION_KEY, JSON.stringify(res));
    setSession(res);
  }, []);

  const signOut = useCallback(async () => {
    await SecureStore.deleteItemAsync(SESSION_KEY);
    setSession(null);
  }, []);

  const value = useMemo(() => ({ session, ready, signIn, signOut }), [session, ready, signIn, signOut]);
  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession() {
  const ctx = useContext(SessionContext);
  if (!ctx) throw new Error("useSession în afara SessionProvider");
  return ctx;
}
