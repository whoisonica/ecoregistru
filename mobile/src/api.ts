import { Platform } from "react-native";

import type { Company, Deadline, MovementSummary, MovementTotals, WasteMovement, WasteRegister } from "@web/types";

import type { AuthResponse, DeviceSessionRow } from "./auth";

/**
 * Backendul local din profilul dev. Emulatorul Android vede Mac-ul la 10.0.2.2, simulatorul iOS la localhost.
 * `EXPO_PUBLIC_API_URL` bate amândouă (telefon real în aceeași rețea).
 */
const BASE_URL =
  process.env.EXPO_PUBLIC_API_URL ??
  (Platform.OS === "android" ? "http://10.0.2.2:8080" : "http://localhost:8080");

/** 401 pe o cerere cu token, după ce reîmprospătarea a fost încercată și n-a mers: omul iese din cont. */
export class UnauthorizedError extends Error {}

export class ApiError extends Error {
  constructor(readonly status: number) {
    super(`HTTP ${status}`);
  }
}

/** Ce trebuie ca să vorbești cu serverul în numele cuiva, pe firma lui. */
export interface Auth {
  token: string;
  tenantId: string | null;
}

/**
 * G1 — cine știe să ceară un token de acces nou. Îl pune {@code SessionProvider} la pornire; aici stă
 * ca variabilă de modul fiindcă orice cerere poate fi cea care dă peste 401, nu doar una anume.
 *
 * <p>Întoarce tokenul nou, sau null când sesiunea lungă e și ea moartă (revocată, expirată, cont oprit).
 */
type Refresher = () => Promise<string | null>;
let refresher: Refresher | null = null;
export function setRefresher(fn: Refresher | null) {
  refresher = fn;
}

/**
 * O singură reîmprospătare în aer, oricâte cereri ar da peste 401 în aceeași clipă.
 *
 * <p>Acasă cere trei lucruri deodată. Fără asta, la prima deschidere de dimineață toate trei ar fi
 * rotit tokenul pe rând, iar primele două l-ar fi invalidat pe al treilea — telefonul ar fi ieșit din
 * cont exact în momentul în care trebuia să rămână în el.
 */
let inFlight: Promise<string | null> | null = null;
function refreshOnce(): Promise<string | null> {
  if (!refresher) return Promise.resolve(null);
  inFlight ??= refresher().finally(() => {
    inFlight = null;
  });
  return inFlight;
}

interface Options extends Omit<RequestInit, "body"> {
  auth?: Auth;
  body?: unknown;
  /** Adevărat numai pe a doua încercare, ca să nu intrăm în buclă. */
  retried?: boolean;
}

async function request<T>(path: string, options: Options = {}): Promise<T> {
  const { auth, body, headers, retried, ...rest } = options;
  const res = await fetch(`${BASE_URL}${path}`, {
    ...rest,
    body: body === undefined ? undefined : JSON.stringify(body),
    headers: {
      "Content-Type": "application/json",
      ...(auth ? { Authorization: `Bearer ${auth.token}` } : {}),
      // Ca pe web (`frontend/src/lib/api.ts`): firma pe care lucrează sesiunea. Consultantul și
      // platforma o schimbă; pentru ceilalți e chiar firma lor și serverul o ignoră oricum.
      ...(auth?.tenantId ? { "X-Tenant-Id": auth.tenantId } : {}),
      ...(headers as Record<string, string>),
    },
  });

  if (res.status === 401 && auth && !retried) {
    const token = await refreshOnce();
    if (token) return request<T>(path, { ...options, auth: { ...auth, token }, retried: true });
    throw new UnauthorizedError();
  }
  if (res.status === 401 && auth) throw new UnauthorizedError();
  if (!res.ok) throw new ApiError(res.status);
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

// ── sesiune ──────────────────────────────────────────────────────────────────

export function login(email: string, password: string, deviceName: string, devicePlatform: "IOS" | "ANDROID") {
  return request<AuthResponse>("/api/v1/auth/login", {
    method: "POST",
    body: { email, password, deviceName, devicePlatform },
  });
}

/** G1 — sesiunea lungă pe alta, plus încă opt ore de acces. Fără token de acces: tocmai a murit. */
export function refreshSession(refreshToken: string) {
  return request<AuthResponse>("/api/v1/auth/refresh", { method: "POST", body: { refreshToken } });
}

/** Ieșirea din cont stinge sesiunea și pe server, nu doar Keychain-ul. */
export function logout(refreshToken: string) {
  return request<void>("/api/v1/auth/logout", { method: "POST", body: { refreshToken } });
}

export function devices(auth: Auth) {
  return request<DeviceSessionRow[]>("/api/v1/auth/devices", { auth });
}

// ── firma ────────────────────────────────────────────────────────────────────

/** Tipul firmei (generator / colector) nu vine în login; de aici îl ia bara de jos. */
export function currentCompany(auth: Auth) {
  return request<Company>("/api/v1/companies/current", { auth });
}

/** Comutatorul de firmă al consultantului și al platformei. 403 pentru oricine altcineva. */
export function companies(auth: Auth) {
  return request<Company[]>("/api/v1/companies", { auth });
}

// ── luna ─────────────────────────────────────────────────────────────────────

export function movementSummary(auth: Auth, year: number, month: number) {
  return request<MovementSummary>(`/api/v1/movements/summary?year=${year}&month=${month}`, { auth });
}

/**
 * Filtrele unui ecran de mișcări. **Registrul face parte din ele**: fără el „Intrări" ar arăta și
 * rândurile Anexei 1, adică deșeul propriu al firmei sub titlul mărfii preluate de la terți.
 */
export interface MovementFilters {
  year: number;
  month: number;
  register?: WasteRegister;
  direction?: "IN" | "OUT";
}

/** Cifrele peste toate rândurile filtrului, nu peste pagina adusă — ca pe web. */
export function movementTotals(auth: Auth, params: MovementFilters) {
  return request<MovementTotals>(`/api/v1/movements/totals?${query({ ...params })}`, { auth });
}

/** Pe telefon lista lunii e scurtă și se citește cu degetul; o pagină de 50 ajunge. */
export function movements(auth: Auth, params: MovementFilters) {
  return request<PageSlice<WasteMovement>>(`/api/v1/movements?${query({ ...params, size: 50 })}`, { auth });
}

/** `year` e obligatoriu — §3 din todo-mobil spunea altceva, serverul răspunde 400 fără el. */
export function deadlines(auth: Auth, year: number) {
  return request<Deadline[]>(`/api/v1/deadlines?year=${year}`, { auth });
}

/**
 * Ce întoarce `/movements`: o pagină, nu lista. Aceleași câmpuri ca `PageSlice` de pe web
 * (`frontend/src/hooks/useTableView.ts`), care stă într-un fișier cu React în el, deci nu se importă.
 */
export interface PageSlice<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

function query(params: Record<string, string | number | undefined>) {
  return Object.entries(params)
    .filter(([, v]) => v !== undefined)
    .map(([k, v]) => `${k}=${encodeURIComponent(String(v))}`)
    .join("&");
}
