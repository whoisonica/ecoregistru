/**
 * Oglinda lui `AuthenticationResponse` din backend. Pe web tipul stă în `auth/AuthContext.tsx`, care importă React și
 * axios, deci nu se poate lua direct ca `strings.ts`/`types.ts`; e mic și se ține de mână.
 *
 * <p>`types.ts` are un singur `import("@/auth/AuthContext").Role`, doar de tip; `tsconfig.json` îl trimite aici.
 */
export type Role = "PLATFORM_ADMIN" | "CONSULTANT" | "ADMIN" | "OPERATOR" | "CLIENT_VIEWER";

export interface AuthResponse {
  token: string;
  role: Role;
  tenantId: string | null;
  tenantName: string | null;
  consultancyName: string | null;
  email: string;
  /** G1 — sesiunea lungă. Vine numai fiindcă loginul a spus cum cheamă telefonul. */
  refreshToken: string | null;
  /** G1 — care rând din „Dispozitive conectate” e telefonul ăsta. */
  deviceSessionId: string | null;
}

/** Un rând din `GET /auth/devices`. Oglinda lui `DeviceSessionResponse`. */
export interface DeviceSessionRow {
  id: string;
  deviceName: string;
  platform: "IOS" | "ANDROID";
  createdAt: string;
  lastUsedAt: string;
}

/** Aceleași praguri ca `frontend/src/lib/roles.ts`; backendul rămâne cel care refuză. */
export function canWrite(role: Role | undefined): boolean {
  return role === "PLATFORM_ADMIN" || role === "CONSULTANT" || role === "ADMIN" || role === "OPERATOR";
}

export function isMultiCompany(role: Role | undefined): boolean {
  return role === "PLATFORM_ADMIN" || role === "CONSULTANT";
}
