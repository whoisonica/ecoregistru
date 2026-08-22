/**
 * API types mirroring the backend DTOs. Enums match the Java enums by value
 * (constants are English; Romanian labels live in strings.ts `enums`).
 */

export type WasteOperation = "GENERATED" | "COLLECTED" | "HANDED_OVER" | "RECOVERED" | "DISPOSED";

/**
 * Which legal register a quantity belongs to. ANEXA_1 = waste generated in the company's own
 * activity; ART_48 = goods taken over from third parties, which HG 856/2002 art. 2 alin. (1)
 * keeps out of Anexa 1 and OUG 92/2021 art. 48 sends to a separate chronological register.
 */
export type WasteRegister = "ANEXA_1" | "ART_48";

/**
 * "Scopul" V/E — HG 856/2002 anexa nr. 1, cap. 2, nota 3. Derived from the R/D operation code,
 * never entered: it says which cap. 1 column the quantity feeds, "valorificată" (V) or
 * "eliminată final" (E). There is no "handed over" column on the form.
 */
export type TreatmentPurpose = "V" | "E";

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
  // Extra fields are present on the platform-admin management endpoint; the tenant
  // switcher only reads id/name, so they are optional for older callers.
  afmObligation?: boolean;
  environmentalAuthNumber?: string | null;
  environmentalAuthExpiry?: string | null; // yyyy-MM-dd
  address?: string | null;
  contactName?: string | null;
  contactEmail?: string | null;
  contactPhone?: string | null;
}

/** Create/update payload for a company (PLATFORM_ADMIN only). */
export interface CompanyInput {
  name: string;
  cui: string;
  type: CompanyType;
  afmObligation: boolean;
  environmentalAuthNumber?: string | null;
  environmentalAuthExpiry?: string | null; // yyyy-MM-dd
  address?: string | null;
  contactName?: string | null;
  contactEmail?: string | null;
  contactPhone?: string | null;
}

/** Tenant roles that can be invited (never PLATFORM_ADMIN). */
export type InviteRole = "ADMIN" | "OPERATOR" | "CLIENT_VIEWER";

export interface InviteUserInput {
  email: string;
  role: InviteRole;
  firstName?: string | null;
  lastName?: string | null;
}

/** Mirrors backend CompanyUserResponse. */
export interface CompanyUser {
  id: string;
  email: string;
  role: InviteRole;
  firstName: string | null;
  lastName: string | null;
  enabled: boolean;
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
  register: WasteRegister;
  /** Derived server-side from operationCode; read-only. */
  treatmentPurpose: TreatmentPurpose | null;
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
  /** Optional: the backend derives it from the operation unless the goods are third-party. */
  register?: WasteRegister | null;
  physicalState?: PhysicalState | null;
  /** Required for HANDED_OVER, RECOVERED and DISPOSED; rejected on the other operations. */
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

/**
 * Mirrors backend MonthlyEvidenceResponse — one line of Anexa 1 (HG 856/2002), in the order the
 * form reads. All quantities are in KG. Goods taken over from third parties are not here: they
 * belong to the art. 48 register (art. 2 alin. (1)).
 */
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
  totalRecovered: number;
  totalDisposed: number;
  /** Memo: the part of recovered + disposed that left as a handover. Already counted in those. */
  totalHandedOver: number;
  /** Left the site with no operation code, so it is in neither official column. */
  totalUnclassifiedOut: number;
  /** True when totalUnclassifiedOut > 0: the line cannot be reported as it stands. */
  incomplete: boolean;
  /** These handovers may be passing on third-party goods; for review, not for reporting. */
  resaleSuspected: boolean;
  closingStock: number; // may be negative (exits exceeding intake in a window)
  generatedAt: string;
}

/** Mirrors backend EvidenceRegenerationResponse. */
export interface EvidenceRegenerationResponse {
  year: number;
  /** Lines written for the requested year and the cascaded ones together. */
  linesGenerated: number;
  /** Later years rebuilt as well, because stock carries across years. */
  cascadedYears: number[];
}

/** Filters for the evidence list query; empty fields are omitted from the request. */
export interface EvidenceFilters {
  year: number; // required
  month?: number; // 1-12
  workPointId?: string;
}

// --- Reporting deadlines (FAZA TERMENE) ---

export type ReportType = "SIM_ANNUAL" | "AFM_MONTHLY" | "OTHER";

export type DeadlineStatus = "UPCOMING" | "DONE" | "OVERDUE";

/** Mirrors backend DeadlineResponse. `status` is the effective status (OVERDUE derived from due date). */
export interface Deadline {
  id: string;
  reportType: ReportType;
  dueDate: string; // yyyy-MM-dd
  status: DeadlineStatus;
  completedAt: string | null;
  completionNote: string | null;
}

/** Mirrors backend DeadlineGenerationResponse. */
export interface DeadlineGenerationResponse {
  year: number;
  generated: number;
}
