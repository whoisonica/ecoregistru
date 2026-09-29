/** Regulile pure ale formularului de mișcare, fără React: ce operațiuni, coduri și casete se oferă. */
import { strings } from "@/lib/strings";
import type { MovementDirection, PackagingMaterial, PartnerType, TransportDestination, Unit, WasteDestination, WasteOperation, WasteOperationCode, WasteRegister } from "@/lib/types";

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
  // întâmplă cu deşeul după se alege mai jos. Cererea specialistei, 25.08.2026: „aici, la
  // operaţiune, trebuie să rămână Generator [...] şi după, mai jos, trebuie pus în tab cu
  // Valorificare/Eliminare [...] să apară următoarele taburi cu codurile". De pe 29.09.2026 blocul
  // stă înaintea transportului: „prima dată să te pună să alegi valorificare/eliminare, codurile și
  // după unde merg deșeurile" (Andreea) — vezi `destinationsOpen`. Pe art. 48 ieşirea e directă: marfa preluată n-a fost generată de firmă,
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
 * Ce formular de transport tipărește predarea, sau `null` când nu tipărește niciunul (30.09.2026).
 *
 * <p>Anexa 3 e a nepericuloaselor, Anexa 2 a periculoaselor — și pe aceasta o întocmește colectorul
 * („anexa 2 o păstrăm doar pentru colectori", specialista, 14.09.2026): generatorul de periculoase o
 * primește de la el și tipărește doar avizul. Cap. 18 n-are niciunul: formularul e al
 * transportatorului (art. 24). Fără formular, blocul de transport păstrează doar ce scrie avizul —
 * transportatorul, șoferul, mașina —, nu rubricile unei hârtii care nu se tipărește.
 */
export function transportForm(code: {
  hazardous: boolean;
  medical: boolean;
  collectorForms: boolean;
}): "ANEXA_3" | "ANEXA_2" | null {
  if (!code.hazardous) return "ANEXA_3";
  if (code.medical || !code.collectorForms) return null;
  return "ANEXA_2";
}

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


/**
 * Ce destinații din nota 5 se potrivesc cu operațiunea aleasă (20.09.2026, proprietarul).
 *
 * <p>Până acum cele două rubrici se alegeau independent, deci se putea salva „valorificare, R3" cu
 * destinația `DO` — groapa de gunoi a orașului — iar fișa o tipărea așa. Pe hârtie nu se vedea,
 * fiindcă **cap. 2 se tipărește pe lună**, unde „DO, Vr" e legitim: neconcordanța era în rândul
 * din aplicație.
 *
 * <p>Gruparea **nu e o deducție**, e chiar textul notei 5: `I` scrie „Incinerarea în scopul
 * **eliminării**", `Vr` și `Ve` scriu „**Valorificare** …", `HP`/`HC` sunt halde, `DO` e depozitul
 * de gunoi. `A` („Altele") stă în amândouă: e rubrica pentru ce nu intră nicăieri.
 *
 * <p>Cât timp operațiunea nu e aleasă (o intrare, un rând vechi fără soartă), se oferă toate opt:
 * n-ai după ce filtra, iar o listă scurtată fără motiv ascunde valori pe care nota tipărită le are.
 * Pe „Generare” rândul nou nici nu le vede până nu alege (`destinationsOpen`).
 */
export function destinationsFor(operation: WasteOperation | ""): WasteDestination[] {
  if (operation === "RECOVERED") return ["Vr", "P", "Ve", "A"];
  if (operation === "DISPOSED") return ["DO", "HP", "HC", "I", "A"];
  return ["DO", "HP", "HC", "I", "Vr", "P", "Ve", "A"];
}

/**
 * Dacă formularul arată deja destinația (nota 5). Pe „Generare” întrebările vin în ordinea
 * specialistei: „prima dată să te pună să alegi valorificare/eliminare, codurile și după unde merg
 * deșeurile" (Andreea, 29.09.2026) — deci până la alegere nu se oferă nimic, fiindcă tabăra
 * destinațiilor atârnă de ea. Un rând care are deja destinația (redeschis, duplicat, „La fel ca
 * data trecută", ori unul vechi `UNCLASSIFIED_OUT`) o arată, ca nimic salvat să nu dispară de pe
 * ecran. Celelalte ecrane rămân cum erau: acolo destinația se oferă mereu.
 */
export function destinationsOpen(
  screen: WasteRegister,
  choosesFate: boolean,
  fate: WasteOperation | "",
  destination: WasteDestination | ""
): boolean {
  if (screen !== "ANEXA_1" || !choosesFate) return true;
  return fate !== "" || destination !== "";
}

/**
 * Data mișcării e după azi (29.09.2026). Doar un avertisment pe formular: serverul primește până la
 * zece ani înainte, iar probele e2e scriu dinadins în 2033 — dar pe o mișcare obișnuită, un an greșit
 * tastat („2062” în loc de „2026”) o ascundea de evidența anului. ISO, deci se compară ca șiruri.
 */
export function isAfterToday(date: string, today: string): boolean {
  return date !== "" && date > today;
}

/**
 * O editare care scoate din Registrul Anexa 3 (transport) o predare al cărei formular a fost deja emis
 * (29.09.2026). Registrul ia rândurile cu număr alocat, pe o ieșire (valorificare/eliminare) către un
 * partener, în anul datei (`Anexa3RegisterBuilder`) — deci fără partener, fără ieșire sau cu data mutată
 * în alt an, numărul emis rămâne gol în registru. Nu se oprește nimic: se spune înainte de salvare.
 */
export function leavesAnexa3Gap(
  original: { anexa3Number: number | null; date: string } | null,
  next: { date: string; partnerId: string | null; operation: WasteOperation }
): boolean {
  if (!original || original.anexa3Number == null) return false;
  if (!next.partnerId || !isExit(next.operation)) return true;
  return next.date.slice(0, 4) !== original.date.slice(0, 4);
}

/**
 * Id-ul copiat de pe o mișcare-sursă, sau gol dacă între timp partenerul a fost dezactivat (29.09.2026):
 * select-ul nu-l arată, iar serverul l-ar refuza la salvare.
 */
export function keepIfActive(id: string, partners: ReadonlyArray<{ id: string; active: boolean }>): string {
  return id === "" || partners.some((p) => p.id === id && p.active) ? id : "";
}

/**
 * Ce e greșit la cantitatea tastată, sau `null` (30.09.2026, proprietarul: rubrica primea „,000” și la
 * kilograme). Nimeni nu cântărește la gram, deci în kilograme numai numere întregi; în tone rămân trei
 * zecimale, adică tot kilogramul (0,250 t). Valoarea vine din `<input type="number">`, cu punct.
 */
export function quantityProblem(raw: string, unit: Unit): "required" | "wholeKg" | "tooPrecise" | null {
  const value = Number(raw);
  if (raw.trim() === "" || !Number.isFinite(value) || value <= 0) return "required";
  if (unit === "KG" && !Number.isInteger(value)) return "wholeKg";
  if (unit === "TONS" && !Number.isInteger(Math.round(value * 1e6) / 1e3)) return "tooPrecise";
  return null;
}
