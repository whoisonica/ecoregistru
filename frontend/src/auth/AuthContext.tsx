import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { api, tokenStore, tenantStore } from "@/lib/api";

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

const USER_KEY = "eco_user";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = tokenStore.get();
    const stored = localStorage.getItem(USER_KEY);
    if (token && stored) {
      setUser(JSON.parse(stored) as AuthUser);
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
    localStorage.setItem(USER_KEY, JSON.stringify(authUser));
    if (data.tenantId) {
      tenantStore.set(data.tenantId);
    }
    setUser(authUser);
  }

  function logout() {
    tokenStore.clear();
    tenantStore.clear();
    localStorage.removeItem(USER_KEY);
    setUser(null);
  }

  const value = useMemo(() => ({ user, loading, login, logout }), [user, loading]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return ctx;
}
