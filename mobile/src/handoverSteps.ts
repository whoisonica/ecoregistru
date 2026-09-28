/**
 * Cei trei pași ai formularului de predare (F4, valul B, 27.09.2026 — propunerea §3.2, decizia D7):
 * „Ce și cât” · „Cui și cum” · „Pe fișă și transport”. Formularul rămâne același ca date; aici e doar
 * împărțirea rubricilor pe pași și ce oprește „Continuă”, scoase din ecran ca să se probeze fără telefon.
 */
export type Step = 1 | 2 | 3;

/** Cheile rubricilor, aceleași ca ale `errors` și `pending` din `predare.tsx`. */
export type FieldKey =
  | "workPoint"
  | "date"
  | "wasteCode"
  | "quantity"
  | "fate"
  | "operationCode"
  | "partner"
  | "physicalState"
  | "storageType"
  | "transportMeans"
  | "wasteDestination"
  | "packagingMaterial"
  | "packagingCategory"
  | "loadDate"
  | "unloadDate"
  | "driverCnp"
  | "documentReference"
  | "vehicle";

export const STEP_FIELDS: Record<Step, readonly FieldKey[]> = {
  1: ["workPoint", "date", "wasteCode", "quantity"],
  2: ["fate", "operationCode", "partner"],
  3: [
    "physicalState",
    "storageType",
    "transportMeans",
    "wasteDestination",
    "packagingMaterial",
    "packagingCategory",
    "loadDate",
    "unloadDate",
    "driverCnp",
    "documentReference",
    "vehicle",
  ],
};

export function stepOf(key: FieldKey): Step {
  return STEP_FIELDS[1].includes(key) ? 1 : STEP_FIELDS[2].includes(key) ? 2 : 3;
}

type Errors = Partial<Record<FieldKey, string | undefined>>;
type Pending = Partial<Record<string, string>>;

/**
 * „Continuă” nu trece peste o eroare sau peste o rubrică citită din poză și neconfirmată de pe pasul
 * curent. Ce e pe alți pași nu-l privește: fiecare pas își verifică ale lui, iar „Salvează” verifică tot.
 */
export function stepBlocked(step: Step, errors: Errors, pending: Pending): boolean {
  return STEP_FIELDS[step].some((k) => !!errors[k] || pending[k] != null);
}

export type StepNote =
  | { kind: "pending"; n: number }
  | { kind: "singleWorkPoint"; name: string }
  | { kind: "nextSheet" }
  | { kind: "offline" }
  | { kind: "onlinePhoto" }
  | { kind: "onlineNoPhoto" }
  | { kind: "none" };

/** Propoziția de sub buton: întâi câte rubrici din poză mai sunt de confirmat pe pasul ăsta, apoi ce urmează. */
export function stepNote(
  step: Step,
  ctx: { pending: Pending; singleWorkPoint: string | null; online: boolean; hasPhoto: boolean },
): StepNote {
  const n = STEP_FIELDS[step].filter((k) => ctx.pending[k] != null).length;
  if (n > 0) return { kind: "pending", n };
  if (step === 1) return ctx.singleWorkPoint ? { kind: "singleWorkPoint", name: ctx.singleWorkPoint } : { kind: "none" };
  if (step === 2) return { kind: "nextSheet" };
  if (!ctx.online) return { kind: "offline" };
  return ctx.hasPhoto ? { kind: "onlinePhoto" } : { kind: "onlineNoPhoto" };
}
