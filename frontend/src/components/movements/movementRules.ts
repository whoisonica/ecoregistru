/** Regulile pure ale formularului de mișcare, fără React: ce operațiuni, coduri și casete se oferă. */
import { strings } from "@/lib/strings";
import type { MovementDirection, PackagingMaterial, PartnerType, TransportDestination, WasteOperation, WasteOperationCode, WasteRegister } from "@/lib/types";

const e = strings.enums;


/** Rândurile de material ale Anexei 1 Ambalaje, în ordinea formularului. */
export const PACKAGING_MATERIALS: PackagingMaterial[] = [
  "STICLA",
  "PET",
  "ALTE_PLASTICE",
  "HARTIE_CARTON",
  "ALUMINIU",
  "OTEL",
  "LEMN",
  "ALTELE",
];

/**
 * Ce material propune codul de deşeu, acolo unde îl decide singur. `15 01 04` nu apare aici
 * dinadins: „ambalaje metalice" acoperă şi aluminiul, şi oţelul, iar formularul are rând pentru
 * fiecare — deci întreabă, nu ghiceşte. `15 01 02` propune „Alte plastice", fiindcă PET-ul e
 * afirmaţia mai îngustă şi e a clientului.
 */
export function suggestedPackagingMaterial(codeLabel: string): PackagingMaterial | null {
  if (codeLabel.startsWith("15 01 01")) return "HARTIE_CARTON";
  if (codeLabel.startsWith("15 01 02")) return "ALTE_PLASTICE";
  if (codeLabel.startsWith("15 01 03")) return "LEMN";
  if (codeLabel.startsWith("15 01 07")) return "STICLA";
  return null;
}

/**
 * Which operations the account may record, by company type — the same rule the backend enforces
 * through CompanyType.allowedOperations() — here narrowed by the screen: „Generare" records the
 * company's own waste (Anexa 1), „Intrări și ieșiri" the goods taken over from third parties
 * (art. 48). UNCLASSIFIED_OUT is in no list: it is the state of legacy rows, written by a migration.
 */
export function operationsFor(screen: WasteRegister, direction: MovementDirection | undefined): WasteOperation[] {
  // Pe „Generare" rămâne o singură opţiune, fiindcă mişcarea porneşte mereu de la generare: ce se
  // întâmplă cu deşeul după se alege mai jos, sub transport. Cererea specialistei, 25.08.2026:
  // „aici, la operaţiune, trebuie să rămână Generator [...] şi după, mai jos, trebuie pus în tab cu
  // Valorificare/Eliminare [...] după ce alegi la Transport spre Valorificare să apară următoarele
  // taburi cu codurile". Pe art. 48 ieşirea e directă: marfa preluată n-a fost generată de firmă,
  // deci nu se poate scrie ca generare urmată de predare. De pe 15.09.2026 ecranul spune și
  // direcția: „Intrări" oferă preluarea, „Ieșiri" valorificarea și eliminarea.
  if (screen === "ANEXA_1") return ["GENERATED"];
  if (direction === "IN") return ["COLLECTED"];
  if (direction === "OUT") return ["RECOVERED", "DISPOSED"];
  return ["COLLECTED", "RECOVERED", "DISPOSED"];
}

/**
 * Ce rubrică a formularului de mişcare e greşită. `form` e pentru ce nu ţine de o rubrică anume.
 */
export type FieldErrors = Partial<
  Record<
    | "workPointId"
    | "date"
    | "wasteCode"
    | "quantity"
    | "partnerId"
    | "fate"
    | "operationCode"
    | "wasteDestination"
    | "physicalState"
    | "storageType"
    | "transportMeans"
    | "packagingMaterial"
    | "packagingCategory"
    | "form",
    string
  >
>;

/** Cele două operaţiuni care scot cantitatea de pe amplasament. */
export type ExitOperation = "RECOVERED" | "DISPOSED";

export function isExit(operation: WasteOperation): boolean {
  return operation === "RECOVERED" || operation === "DISPOSED";
}
export const ALL_CODES = Object.keys(e.wasteOperationCode) as WasteOperationCode[];
export const R_CODES = ALL_CODES.filter((c) => c.startsWith("R"));
export const D_CODES = ALL_CODES.filter((c) => c.startsWith("D"));

/**
 * Ce se bifează la "Destinat:" pe Anexa 3, după ce este destinatarul.
 *
 * <p>Răspunsul specialistei din 24.08.2026 (A3.1), verbatim: „când pleacă la colector se pot bifa
 * valorificării şi colectării, dacă se poate valorifica. Iar când pleacă la valorificator, doar
 * valorificării."
 *
 * <p><b>De ce după partener și nu după codul R/D.</b> Prima variantă a feliei prebifa din familia
 * codului — R la valorificare, D la eliminare — și era greșită: pe Anexa 3 primită de la Hamburger
 * Recycling, marfa pleacă la un colector sub codul 15 01 01 și caseta pretipărită e „colectării".
 * Caseta spune ce face destinatarul cu marfa, nu ce cod a ales expeditorul. De asta a fost nevoie
 * de tipul de partener „Valorificator": codul nu poate face diferența.
 *
 * <p>Eliminarea nu se prebifează: n-am întrebat-o și nu se ghicește pe un formular oficial. La un
 * generator, caseta rămâne goală până o bifează omul.
 *
 * @returns casetele sugerate, sau o listă goală când nu avem ce sugera
 */
export function suggestedDestinations(
  partnerType: PartnerType | null | undefined,
  operation: WasteOperation
): TransportDestination[] {
  if (operation !== "RECOVERED") return [];
  if (partnerType === "COLLECTOR") return ["COLECTARE", "VALORIFICARE"];
  if (partnerType === "RECOVERER") return ["VALORIFICARE"];
  return [];
}

export { todayIso } from "@/lib/utils";
