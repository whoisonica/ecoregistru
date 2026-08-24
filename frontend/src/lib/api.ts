import axios from "axios";

const TOKEN_KEY = "eco_token";
const TENANT_KEY = "eco_tenant"; // used by PLATFORM_ADMIN tenant switcher
const USER_KEY = "eco_user";

/**
 * Why the login page is being shown. It travels as a query parameter and not in storage: the
 * redirect below is a full page load, and a stored flag would have to be "consumed" exactly once
 * — which React's StrictMode double-invocation makes surprisingly hard to get right, and which
 * leaves a stale flag behind if the reload never happens. A parameter is read where it is
 * rendered, disappears on the next navigation, and carries nothing personal.
 */
export const LOGIN_EXPIRED_PARAM = "expirat";

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t: string) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

export const tenantStore = {
  get: () => localStorage.getItem(TENANT_KEY),
  set: (id: string) => localStorage.setItem(TENANT_KEY, id),
  clear: () => localStorage.removeItem(TENANT_KEY),
};

export const userStore = {
  get: () => localStorage.getItem(USER_KEY),
  set: (json: string) => localStorage.setItem(USER_KEY, json),
  clear: () => localStorage.removeItem(USER_KEY),
};

/** Everything that says "someone is logged in". Cleared together, or not at all. */
export function clearSession() {
  tokenStore.clear();
  tenantStore.clear();
  userStore.clear();
}

/**
 * Pages that work without a token. A 401 from a background request must not throw the user off
 * one of these: the intake form has six sections, and losing it to someone else's expired session
 * is exactly the accident this guard prevents.
 */
const PUBLIC_PATHS = ["/login", "/cerere-cont", "/parola-uitata", "/reseteaza-parola"];

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "",
});

api.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  // Only meaningful for PLATFORM_ADMIN; harmless otherwise (backend ignores it for scoped users).
  const tenant = tenantStore.get();
  if (tenant) {
    config.headers["X-Tenant-Id"] = tenant;
  }
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401) {
      // A 401 on a request that carried no token is a failed login or an expired reset link —
      // the page that made it shows its own error. Only an authenticated request going stale
      // is a session expiring.
      const wasAuthenticated = Boolean(error.config?.headers?.Authorization);
      if (wasAuthenticated) {
        clearSession();
        if (!PUBLIC_PATHS.includes(window.location.pathname)) {
          window.location.href = `/login?${LOGIN_EXPIRED_PARAM}=1`;
        }
      }
    }
    return Promise.reject(error);
  }
);

/** Extract the Romanian error message from the backend's error envelope. */
export function apiErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { "error-message"?: string } | undefined;
    if (data && data["error-message"]) {
      return data["error-message"];
    }
  }
  return fallback;
}
