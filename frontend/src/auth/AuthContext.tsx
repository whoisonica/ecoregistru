import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { api, tokenStore, tenantStore, userStore, clearSession } from "@/lib/api";

export type Role = "PLATFORM_ADMIN" | "ADMIN" | "OPERATOR" | "CLIENT_VIEWER";

export interface AuthUser {
  email: string;
  role: Role;
  tenantId: string | null;
  tenantName: string | null;
}

interface AuthContextValue {
  user: AuthUser | null;
  loading: boolean;
  /**
   * The company every request is scoped to. Mirrors `tenantStore` (which the axios interceptor
   * reads) as React state, so that changing companies re-renders the app instead of waiting for
   * the user to reload the page. For a scoped user it is simply their own company.
   */
  tenantId: string | null;
  /** PLATFORM_ADMIN only: point the session at another company. `null` = none selected. */
  switchTenant: (id: string | null) => void;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

interface AuthResponse {
  token: string;
  role: Role;
  tenantId: string | null;
  tenantName: string | null;
  email: string;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [tenantId, setTenantId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = tokenStore.get();
    const stored = userStore.get();
    if (token && stored) {
      setUser(JSON.parse(stored) as AuthUser);
      setTenantId(tenantStore.get());
    }
    setLoading(false);
  }, []);

  async function login(email: string, password: string) {
    const { data } = await api.post<AuthResponse>("/api/v1/auth/login", { email, password });
    tokenStore.set(data.token);
    const authUser: AuthUser = {
      email: data.email,
      role: data.role,
      tenantId: data.tenantId,
      tenantName: data.tenantName,
    };
    userStore.set(JSON.stringify(authUser));
    if (data.tenantId) {
      tenantStore.set(data.tenantId);
    }
    setUser(authUser);
    setTenantId(data.tenantId);
  }

  function switchTenant(id: string | null) {
    if (id) {
      tenantStore.set(id);
    } else {
      tenantStore.clear();
    }
    setTenantId(id);
  }

  function logout() {
    clearSession();
    setUser(null);
    setTenantId(null);
  }

  const value = useMemo(
    () => ({ user, loading, tenantId, switchTenant, login, logout }),
    [user, loading, tenantId]
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return ctx;
}
