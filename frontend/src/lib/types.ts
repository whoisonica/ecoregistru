/**
 * API types mirroring the backend DTOs. Enums match the Java enums by value
 * (constants are English; Romanian labels live in strings.ts `enums`).
 */

export type WasteOperation = "GENERATED" | "COLLECTED" | "HANDED_OVER" | "RECOVERED" | "DISPOSED";

export type Unit = "KG" | "TONS";

export type PhysicalState = "SOLID" | "LIQUID" | "SLUDGE" | "PASTY" | "POWDER" | "GASEOUS";

export type WasteOperationCode =
  | "R1" | "R2" | "R3" | "R4" | "R5" | "R6" | "R7" | "R8" | "R9" | "R10" | "R11" | "R12" | "R13"
  | "D1" | "D2" | "D3" | "D4" | "D5" | "D6" | "D7" | "D8" | "D9" | "D10" | "D11" | "D12" | "D13" | "D14" | "D15";

// --- Companies (tenants; listed only by PLATFORM_ADMIN for the tenant switcher) ---

export type CompanyType = "GENERATOR" | "COLLECTOR" | "BOTH";

export interface Company {
  id: string;
  name: string;
  cui: string;
  type: CompanyType;
  active: boolean;
}

// --- Work points ---

export interface WorkPoint {
  id: string;
  name: string;
  address: string | null;
  active: boolean;
}

export interface WorkPointInput {
  name: string;
  address?: string | null;
}

// --- Waste codes (global nomenclator) ---

export interface WasteCode {
  id: string;
  code: string;
  name: string;
  hazardous: boolean;
}

// --- Partners ---

export type PartnerType = "COLLECTOR" | "CARRIER" | "BOTH";

export interface Partner {
  id: string;
  name: string;
  cui: string | null;
  authorizationNumber: string | null;
  authorizationExpiry: string | null; // yyyy-MM-dd
  type: PartnerType;
  active: boolean;
  expiringSoon: boolean;
}

export interface PartnerInput {
  name: string;
  cui?: string | null;
  authorizationNumber?: string | null;
  authorizationExpiry?: string | null; // yyyy-MM-dd
  type: PartnerType;
}

// --- Attachments ---

export interface Attachment {
  id: string;
  url: string;
  fileName: string;
  contentType: string;
}

// --- Waste movements ---

export interface WasteMovement {
  id: string;
  workPointId: string;
  workPointName: string;
  date: string; // yyyy-MM-dd
  wasteCodeId: string;
  wasteCode: string;
  wasteCodeName: string;
  hazardous: boolean;
  quantity: number;
  unit: Unit;
  operation: WasteOperation;
  physicalState: PhysicalState | null;
  operationCode: WasteOperationCode | null;
  partnerId: string | null;
  partnerName: string | null;
  documentReference: string | null;
  notes: string | null;
  attachments: Attachment[];
  clientGeneratedId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface WasteMovementInput {
  clientGeneratedId?: string;
  workPointId: string;
  date: string; // yyyy-MM-dd
  wasteCodeId: string;
  quantity: number;
  unit: Unit;
  operation: WasteOperation;
  physicalState?: PhysicalState | null;
  operationCode?: WasteOperationCode | null;
  partnerId?: string | null;
  documentReference?: string | null;
  notes?: string | null;
}

/** Filters for the movements list query; empty fields are omitted from the request. */
export interface MovementFilters {
  year?: number;
  month?: number; // 1-12
  workPointId?: string;
  wasteCodeId?: string;
}

// --- Monthly evidence (regenerable cache aggregated from movements) ---

/** Mirrors backend MonthlyEvidenceResponse. All quantities are in KG. */
export interface MonthlyEvidence {
  id: string;
  workPointId: string;
  workPointName: string;
  year: number;
  month: number; // 1-12
  wasteCodeId: string;
  wasteCode: string;
  wasteCodeName: string;
  hazardous: boolean;
  totalGenerated: number;
  totalCollected: number;
  totalHandedOver: number;
  totalRecovered: number;
  totalDisposed: number;
  closingStock: number; // may be negative (hand-overs exceeding intake in a window)
  generatedAt: string;
}

/** Mirrors backend EvidenceRegenerationResponse. */
export interface EvidenceRegenerationResponse {
  year: number;
  linesGenerated: number;
}

/** Filters for the evidence list query; empty fields are omitted from the request. */
export interface EvidenceFilters {
  year: number; // required
  month?: number; // 1-12
  workPointId?: string;
}
