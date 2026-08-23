/**
 * API types mirroring the backend DTOs. Enums match the Java enums by value
 * (constants are English; Romanian labels live in strings.ts `enums`).
 */

/**
 * There is no "handed over": HG 856/2002 anexa nr. 1 cap. 1 has no such column, and cap. 3 / cap. 4
 * report a quantity together with its R/D operation AND the operator who performed it. Handing
 * waste to a recycler is a RECOVERED with a partner named; to a landfill, a DISPOSED with one.
 * UNCLASSIFIED_OUT is never offered — it is the state of rows written before the R/D code was
 * required, which leave the stock but enter no official column.
 */
export type WasteOperation =
  | "GENERATED"
  | "COLLECTED"
  | "RECOVERED"
  | "DISPOSED"
  | "UNCLASSIFIED_OUT";

/**
 * Which legal register a quantity belongs to. ANEXA_1 = waste generated in the company's own
 * activity; ART_48 = goods taken over from third parties, which HG 856/2002 art. 2 alin. (1)
 * keeps out of Anexa 1 and OUG 92/2021 art. 48 sends to a separate chronological register.
 */
export type WasteRegister = "ANEXA_1" | "ART_48";

/**
 * "Scopul" — HG 856/2002 anexa nr. 1, cap. 2, nota 3. Derived from the R/D operation code, never
 * entered. Only V is written: every filled Anexa 1 we hold puts "V" on recovery sheets and a dash
 * on disposal sheets, so a disposal is null here and the cell prints empty. What identifies it is
 * its D code in cap. 4, next to the operator.
 */
export type TreatmentPurpose = "V";

export type Unit = "KG" | "TONS";

export type PhysicalState = "SOLID" | "LIQUID" | "SLUDGE" | "PASTY" | "POWDER" | "GASEOUS";

/** Anexa 1 cap. 2, nota 1 — what the waste sits in until it leaves. */
export type StorageType =
  | "RM" | "RP" | "BZ" | "CT" | "CF" | "S" | "PD" | "VN" | "VA" | "RL" | "A";

/**
 * Anexa 1 cap. 2, nota 2 — what is done to the waste on site. "D" here is deshidratare, not a
 * disposal code: the abbreviation collision is the form's, and the two live in different columns.
 */
export type TreatmentMethod = "TM" | "TC" | "TMC" | "TB" | "TT" | "D" | "A";

/**
 * The "Destinat:" ticks of Anexa 3 la HG 1061/2008. More than one is normal — the filled model
 * has an X on both "Colectării" and "Valorificării".
 */
export type TransportDestination =
  | "COLECTARE"
  | "STOCARE_TEMPORARA"
  | "TRATARE"
  | "VALORIFICARE"
  | "ELIMINARE";

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

  // --- The account profile: the answers support transcribed from the intake form. ---
  /** Empty means "not answered yet": the screens then offer everything, not nothing. */
  authorizedOperationCodes?: WasteOperationCode[];
  authorizedWasteCodes?: WasteCode[];
  /** Asked of a collector only. */
  transportMeans?: string | null;
  transportLicenseNumber?: string | null;
  transportLicenseExpiry?: string | null; // yyyy-MM-dd
  /** Printed by Anexa 3 next to the CUI, and the series of this company's forms. */
  tradeRegisterNumber?: string | null;
  anexa3Series?: string | null;
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
  authorizedOperationCodes?: WasteOperationCode[];
  /** Sent as ids; the backend resolves them against the nomenclator. */
  authorizedWasteCodeIds?: string[];
  transportMeans?: string | null;
  transportLicenseNumber?: string | null;
  transportLicenseExpiry?: string | null; // yyyy-MM-dd
  tradeRegisterNumber?: string | null;
  anexa3Series?: string | null;
}

// --- Account requests (the intake form) ---

export type AccountRequestStatus = "NEW" | "APPROVED" | "REJECTED";

/**
 * What a prospective client answers before an account exists. EcoRegistru is a closed register:
 * this is the only way in, and submitting it creates a request, never a login.
 */
export interface AccountRequestInput {
  companyName: string;
  cui: string;
  companyType: CompanyType;
  companyAddress?: string | null;
  workPointName?: string | null;
  workPointAddress?: string | null;
  contactName?: string | null;
  contactEmail: string;
  contactPhone?: string | null;
  environmentalAuthNumber?: string | null;
  environmentalAuthExpiry?: string | null; // yyyy-MM-dd
  /** Asked only of a collector. */
  transportMeans?: string | null;
  transportLicenseNumber?: string | null;
  transportLicenseExpiry?: string | null; // yyyy-MM-dd
  operationCodes?: WasteOperationCode[];
  /** Free text: the nomenclator is behind auth, and "carton, folie" beats a guessed code. */
  wasteCodesText?: string | null;
  notes?: string | null;
}

/** A submitted request, as PLATFORM_ADMIN reads it. */
export interface AccountRequest extends AccountRequestInput {
  id: string;
  status: AccountRequestStatus;
  createdCompanyId: string | null;
  handledAt: string | null;
  createdAt: string;
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

// --- Internal generators (Anexa 1 cap. 2 "Secţia") ---

/**
 * The source inside a work point that produced the waste: the offices, the production hall, the
 * canteen. The third location level, and the only one without an address of its own — it sits
 * inside the work point's.
 */
export interface InternalGenerator {
  id: string;
  workPointId: string;
  workPointName: string;
  name: string;
  description: string | null;
  active: boolean;
}

export interface InternalGeneratorInput {
  workPointId: string;
  name: string;
  description?: string | null;
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
  /** What they are authorised to do with waste. */
  type: PartnerType;
  // --- What Anexa 3 prints about them, as recipient or as carrier ---
  address: string | null;
  tradeRegisterNumber: string | null;
  transportLicenseNumber: string | null;
  transportLicenseExpiry: string | null; // yyyy-MM-dd
  /** We hand waste over to them and we invoice them. */
  client: boolean;
  /** They perform the service and they invoice us. */
  supplier: boolean;
  active: boolean;
  expiringSoon: boolean;
}

export interface PartnerInput {
  name: string;
  cui?: string | null;
  authorizationNumber?: string | null;
  authorizationExpiry?: string | null; // yyyy-MM-dd
  type: PartnerType;
  /** At least one of the two is required; the backend rejects a partner with no role. */
  client: boolean;
  supplier: boolean;
  address?: string | null;
  tradeRegisterNumber?: string | null;
  transportLicenseNumber?: string | null;
  transportLicenseExpiry?: string | null; // yyyy-MM-dd
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
  /** Null while the recipient has not weighed the load yet. */
  quantity: number | null;
  /** The recipient weighs at unloading, so the quantity comes back afterwards. */
  weighedAtUnloading: boolean;
  volumeM3: number | null;
  unit: Unit;
  operation: WasteOperation;
  register: WasteRegister;
  /** Derived server-side from operationCode; read-only. */
  treatmentPurpose: TreatmentPurpose | null;
  physicalState: PhysicalState | null;
  /** Anexa 1 cap. 2 "Stocare: Tipul". */
  storageType: StorageType | null;
  /** Anexa 1 cap. 2 "Tratare: Modul". */
  treatmentMethod: TreatmentMethod | null;
  operationCode: WasteOperationCode | null;
  /** Who performed the operation, when it was not us. Null = on our own site. */
  partnerId: string | null;
  partnerName: string | null;
  /** The section it came from — "Secţia" of Anexa 1 cap. 2. */
  internalGeneratorId: string | null;
  internalGeneratorName: string | null;
  documentReference: string | null;
  notes: string | null;
  attachments: Attachment[];
  clientGeneratedId: string | null;
  // --- Anexa 3 la HG 1061/2008 ---
  unloadDate: string | null; // yyyy-MM-dd
  transportPartnerId: string | null;
  transportPartnerName: string | null;
  driverName: string | null;
  driverIdentification: string | null;
  vehicleRegistration: string | null;
  transportDestinations: TransportDestination[];
  /** Set once the form has been generated; a reprint keeps the same series and number. */
  anexa3Series: string | null;
  anexa3Number: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface WasteMovementInput {
  clientGeneratedId?: string;
  workPointId: string;
  date: string; // yyyy-MM-dd
  wasteCodeId: string;
  /** Omit when weighedAtUnloading is set: the recipient supplies it later. */
  quantity?: number | null;
  weighedAtUnloading?: boolean;
  volumeM3?: number | null;
  unit: Unit;
  operation: WasteOperation;
  /** Optional: the backend derives it from the operation unless the goods are third-party. */
  register?: WasteRegister | null;
  physicalState?: PhysicalState | null;
  storageType?: StorageType | null;
  treatmentMethod?: TreatmentMethod | null;
  /** Required for RECOVERED and DISPOSED; rejected on the other operations. */
  operationCode?: WasteOperationCode | null;
  /** Optional everywhere: names the operator when the operation was not performed by us. */
  partnerId?: string | null;
  internalGeneratorId?: string | null;
  documentReference?: string | null;
  notes?: string | null;
  // --- Anexa 3 ---
  unloadDate?: string | null; // yyyy-MM-dd
  transportPartnerId?: string | null;
  driverName?: string | null;
  driverIdentification?: string | null;
  vehicleRegistration?: string | null;
  transportDestinations?: TransportDestination[];
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
  /** Memo: the part of recovered + disposed a partner performed. Already counted in those. */
  totalHandedOver: number;
  /** Left the site with no operation code, so it is in neither official column. */
  totalUnclassifiedOut: number;
  /** True when totalUnclassifiedOut > 0: the line cannot be reported as it stands. */
  incomplete: boolean;
  /** These handovers may be passing on third-party goods; for review, not for reporting. */
  resaleSuspected: boolean;
  /** An exit this month is still waiting for the recipient's weighbridge. */
  awaitingWeighing: boolean;
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
