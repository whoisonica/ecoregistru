import type {
  Company,
  Deadline,
  Driver,
  MonthlyEvidence,
  MovementSummary,
  MovementTotals,
  PackagingHandoverRow,
  PackagingTable1Row,
  Partner,
  PartnerType,
  WasteCode,
  WasteMovement,
  WasteRegister,
  WorkPoint,
} from "@web/types";

import { File, Paths } from "expo-file-system";

import type { AuthResponse, DeviceSessionRow } from "./auth";

/**
 * **Serverul adevărat, cel de pe Heroku.** Aplicația se leagă la producție din prima, fără nimic de
 * pornit pe Mac: cine ia telefonul în mână vede datele firmei lui, nu o bază de probă.
 *
 * <p>Pentru lucrul local se pune `EXPO_PUBLIC_API_URL` în `mobile/.env.local` (fișier ignorat de git):
 * `http://localhost:8080` pe simulatorul iOS, `http://10.0.2.2:8080` pe emulatorul Android — acolo
 * Mac-ul se vede la altă adresă —, sau IP-ul Mac-ului în rețea pentru un telefon adevărat. Expo citește
 * variabilele `EXPO_PUBLIC_*` la pornirea lui Metro, deci schimbarea cere o repornire a bundler-ului.
 *
 * <p>API-ul n-are domeniu propriu; `app.wastehouse.ro` e frontendul, nu el.
 */
const PRODUCTION_URL = "https://ecoregistru-api-5ba7c1d5e3e3.herokuapp.com";

const BASE_URL = process.env.EXPO_PUBLIC_API_URL ?? PRODUCTION_URL;

/** 401 pe o cerere cu token, după ce reîmprospătarea a fost încercată și n-a mers: omul iese din cont. */
export class UnauthorizedError extends Error {}

/**
 * Un răspuns de eroare de la server. `serverMessage` e propoziția scrisă de backend în plicul lui
 * (`error-message`, `AdviceController`) — pe ea o vede omul când o predare din coadă e refuzată,
 * fiindcă ea spune ce e de schimbat.
 */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly serverMessage: string | null = null,
  ) {
    super(serverMessage ?? `HTTP ${status}`);
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

/** Cererea cu tot ce ține de sesiune (token, firmă, reîmprospătare, plicul de eroare), fără să citească corpul. */
async function send(path: string, options: Options = {}): Promise<Response> {
  const { auth, body, headers, retried, ...rest } = options;
  // O poză pleacă `multipart/form-data`, iar granița o pune `fetch` singur — deci fără antet scris.
  const multipart = body instanceof FormData;
  const res = await fetch(`${BASE_URL}${path}`, {
    ...rest,
    body: body === undefined ? undefined : multipart ? body : JSON.stringify(body),
    headers: {
      ...(multipart ? {} : { "Content-Type": "application/json" }),
      ...(auth ? { Authorization: `Bearer ${auth.token}` } : {}),
      // Ca pe web (`frontend/src/lib/api.ts`): firma pe care lucrează sesiunea. Consultantul și
      // platforma o schimbă; pentru ceilalți e chiar firma lor și serverul o ignoră oricum.
      ...(auth?.tenantId ? { "X-Tenant-Id": auth.tenantId } : {}),
      ...(headers as Record<string, string>),
    },
  });

  if (res.status === 401 && auth && !retried) {
    const token = await refreshOnce();
    if (token) return send(path, { ...options, auth: { ...auth, token }, retried: true });
    throw new UnauthorizedError();
  }
  if (res.status === 401 && auth) throw new UnauthorizedError();
  if (!res.ok) {
    const envelope = (await res.json().catch(() => null)) as { "error-message"?: string } | null;
    throw new ApiError(res.status, envelope?.["error-message"] ?? null);
  }
  return res;
}

async function request<T>(path: string, options: Options = {}): Promise<T> {
  const res = await send(path, options);
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

/** Scoate un telefon din cont — numai de pe contul propriu (serverul refuză altfel, `DeviceSessionIT`). */
export function removeDevice(auth: Auth, id: string) {
  return request<void>(`/api/v1/auth/devices/${id}`, { method: "DELETE", auth });
}

/**
 * G2 — tokenul de push al telefonului, pe sesiunea lui de dispozitiv. Numai sesiunea proprie; `null`
 * îl șterge. Fără `X-Tenant-Id`: e despre telefon, nu despre o firmă.
 */
export function registerPushToken(auth: Auth, deviceSessionId: string, pushToken: string | null) {
  return request<void>(`/api/v1/auth/devices/${deviceSessionId}/push-token`, {
    method: "PUT",
    auth,
    body: { token: pushToken },
  });
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
 * Anul în curs și cel următor, într-o listă (16.09.2026): serverul ține doar următorul termen al
 * fiecărui fel, iar acela e des în anul următor — 15 martie pentru datele de anul ăsta. Și anul
 * trecut: un termen ratat rămâne depășit până se bifează, chiar dacă a fost în decembrie.
 */
export async function upcomingDeadlines(auth: Auth) {
  const year = new Date().getFullYear();
  const lists = await Promise.all([year - 1, year, year + 1].map((y) => deadlines(auth, y)));
  return lists.flat();
}

/** Tabul „Trecute” (ca pe web): anul în curs până ieri, bifate sau nu. */
export function pastDeadlines(auth: Auth) {
  return request<Deadline[]>("/api/v1/deadlines/past", { auth });
}

// ── tabul „Ambalaje” (aceleași cereri ca `hooks/usePackaging.ts` de pe web) ──

/** Tabelul 1 al Anexei 1 Ambalaje: pus pe piață, pe material. */
export function packagingTable1(auth: Auth, year: number) {
  return request<PackagingTable1Row[]>(`/api/v1/packaging/table1?year=${year}`, { auth });
}

/** Tabelul 2: ce s-a predat, pe material și operator. */
export function packagingHandovers(auth: Auth, year: number) {
  return request<PackagingHandoverRow[]>(`/api/v1/packaging/handovers?year=${year}`, { auth });
}

// ── controlul (M1c) ─────────────────────────────────────────────────────────

/** Evidența anului, linie cu linie — din ea vin „fără cod R/D” și „așteaptă cântarul”, ca pe Panoul web. */
export function evidences(auth: Auth, year: number) {
  return request<MonthlyEvidence[]>(`/api/v1/evidences?year=${year}`, { auth });
}

/**
 * Dosarul de control (`GET /api/v1/audit-file`), scris ca ZIP în cache-ul aplicației, de unde îl ia
 * foaia de partajare (D5). Cache, nu documente: arhiva se refă la fiecare trimitere, iar o copie veche
 * uitată pe telefon ar fi încă un loc cu datele firmei.
 *
 * <p>Numele e cel de pe web (`useAuditFile.ts`), ca inspectorul să primească același fișier de oriunde.
 */
export async function downloadAuditFile(auth: Auth, year: number, years: number): Promise<string> {
  const name = years === 1 ? `dosar-control-${year}.zip` : `dosar-control-${year - years + 1}-${year}.zip`;
  return saveToCache(await send(`/api/v1/audit-file?year=${year}&years=${years}`, { auth }), name);
}

/** Corpul unui răspuns, scris în cache sub `name` (înlocuiește o copie mai veche); întoarce adresa fișierului. */
async function saveToCache(res: Response, name: string): Promise<string> {
  const bytes = new Uint8Array(await res.arrayBuffer());
  const file = new File(Paths.cache, name);
  if (file.exists) file.delete();
  file.create();
  file.write(bytes);
  return file.uri;
}

// ── predarea deschisă (M1e) ─────────────────────────────────────────────────

export function movement(auth: Auth, id: string) {
  return request<WasteMovement>(`/api/v1/movements/${id}`, { auth });
}

/**
 * Anexa 3 sau avizul (`GET /movements/{id}/anexa3|aviz`, `CAN_WRITE`), în cache, sub numele de pe web —
 * de acolo îl deschide foaia de partajare. Cache, ca dosarul: se refac la fiecare cerere.
 */
export async function downloadMovementPdf(auth: Auth, id: string, document: "anexa3" | "aviz", name: string) {
  return saveToCache(await send(`/api/v1/movements/${id}/${document}`, { auth }), name);
}

/** Un atașament al mișcării (poza avizului, un buletin), prin server — nu există adresă publică la Cloudinary. */
export async function downloadAttachment(auth: Auth, movementId: string, attachmentId: string, name: string) {
  return saveToCache(await send(`/api/v1/movements/${movementId}/attachments/${attachmentId}/continut`, { auth }), name);
}

// ── predarea (M1b) ──────────────────────────────────────────────────────────

export function workPoints(auth: Auth) {
  return request<WorkPoint[]>("/api/v1/work-points", { auth });
}

/** Toată lista, fără filtru (G4): telefonul o ține în cache și caută în ea CUI-ul citit. */
export function partners(auth: Auth) {
  return request<Partner[]>("/api/v1/partners", { auth });
}

/** Șoferii firmei — ai noștri (`partnerId` null) și ai transportatorilor —, pentru rubricile Anexei 3. */
export function drivers(auth: Auth) {
  return request<Driver[]>("/api/v1/drivers", { auth });
}

/**
 * Ultimele ieșiri ale deșeului propriu, cele mai noi întâi — de aici vine „La fel ca data trecută”
 * pentru codul R/D (G3). Fără an: o predare de anul trecut către același partener e tot un răspuns.
 */
export function recentHandovers(auth: Auth) {
  return request<PageSlice<WasteMovement>>(
    `/api/v1/movements?${query({ register: "ANEXA_1", direction: "OUT", size: 100 })}`,
    { auth },
  );
}

/** Nomenclatorul, pentru un cod care nu e în profilul firmei. Numai cu semnal. */
export function wasteCodes(auth: Auth, q: string) {
  return request<WasteCode[]>(`/api/v1/waste-codes?${query({ q })}`, { auth });
}

/** Ce știe ANAF despre un CUI. Nu salvează nimic. */
export interface CompanyLookup {
  cui: string;
  name: string;
  address: string | null;
  tradeRegisterNumber: string | null;
  inactive: boolean;
}

export function companyLookup(auth: Auth, cui: string) {
  return request<CompanyLookup>(`/api/v1/company-lookup/${encodeURIComponent(cui)}`, { auth });
}

/** Partenerul nou, numai cu ce a arătat ANAF și ce a ales omul. Restul se completează pe web. */
export interface NewPartner {
  name: string;
  cui: string;
  address: string | null;
  tradeRegisterNumber: string | null;
  authorizationNumber: string;
  type: PartnerType;
  client: boolean;
  supplier: boolean;
}

export function createPartner(auth: Auth, partner: NewPartner) {
  return request<Partner>("/api/v1/partners", { method: "POST", auth, body: { carrier: false, ...partner } });
}

export function createMovement(auth: Auth, body: unknown) {
  return request<WasteMovement>("/api/v1/movements", { method: "POST", auth, body });
}

/** Corectura (M1f): înlocuiește predarea întreagă — cererea se face cu `editBody` din `movementEdit.ts`. */
export function updateMovement(auth: Auth, id: string, body: unknown) {
  return request<WasteMovement>(`/api/v1/movements/${id}`, { method: "PUT", auth, body });
}

/**
 * @param clientUploadId aceeași la fiecare reîncercare a aceleiași poze (V67): serverul întoarce atunci
 *     atașamentul deja urcat. Un server de dinainte de V67 ignoră câmpul — deci nimic nu se strică.
 */
export function uploadAttachment(auth: Auth, movementId: string, photoUri: string, clientUploadId: string) {
  const form = new FormData();
  form.append("clientUploadId", clientUploadId);
  // Fișierul de pe disc ca Blob (`expo-file-system`). Forma veche `{ uri, name, type }` e refuzată de
  // `fetch`-ul din Expo 57 („Unsupported FormDataPart implementation”) — prinsă pe simulator la M1b.
  form.append("file", new File(photoUri), "aviz.jpg");
  return request<unknown>(`/api/v1/movements/${movementId}/attachments`, { method: "POST", auth, body: form });
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
