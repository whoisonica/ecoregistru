import axios from "axios";

const TOKEN_KEY = "eco_token";
const TENANT_KEY = "eco_tenant"; // used by PLATFORM_ADMIN tenant switcher

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
      tokenStore.clear();
      if (window.location.pathname !== "/login") {
        window.location.href = "/login";
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
