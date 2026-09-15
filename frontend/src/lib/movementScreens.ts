import type { CompanyType, MovementDirection, WasteRegister } from "@/lib/types";

/**
 * Cele trei ecrane de mișcări (proprietarul, 15.09.2026): „Generare" pentru deșeul firmei (Anexa 1),
 * „Intrări" și „Ieșiri" pentru marfa preluată de la terți (registrul art. 48), **separate** — „să poți
 * să vezi tot frumos". Până pe 15.09 intrările și ieșirile stăteau pe un singur ecran.
 */
export type MovementScreen = "GENERATED" | "IN" | "OUT";

export const SCREEN_PATH: Record<MovementScreen, string> = {
  GENERATED: "/generare",
  IN: "/intrari",
  OUT: "/iesiri",
};

/** Registrul în care intră rândurile ecranului. */
export function registerOf(screen: MovementScreen): WasteRegister {
  return screen === "GENERATED" ? "ANEXA_1" : "ART_48";
}

/** Direcția pe care o filtrează ecranul; „Generare" le arată pe amândouă (stoc și predare). */
export function directionOf(screen: MovementScreen): MovementDirection | undefined {
  return screen === "IN" ? "IN" : screen === "OUT" ? "OUT" : undefined;
}

/**
 * Ce ecrane vede un tip de firmă: generatorul pe al lui, colectorul pe ale lui, „Generator și
 * colector" pe toate trei. Primul e cel pe care duce `/miscari`.
 *
 * <p>⚠️ Un colector pur nu vede „Generare", deși HG 856/2002 art. 2 alin. (1) îi cere Anexa 1
 * pentru deșeul din activitatea proprie — decizia proprietarului: o firmă care are și deșeu propriu
 * se trece pe `BOTH`.
 */
export function screensFor(type: CompanyType | undefined): MovementScreen[] {
  if (type === "GENERATOR") return ["GENERATED"];
  if (type === "COLLECTOR") return ["IN", "OUT"];
  return ["GENERATED", "IN", "OUT"];
}

/** Ce registre ține firma — Setări și Parteneri întreabă „are depozit (art. 48)?". */
export function registersFor(type: CompanyType | undefined): WasteRegister[] {
  const out: WasteRegister[] = [];
  for (const s of screensFor(type)) {
    const r = registerOf(s);
    if (!out.includes(r)) out.push(r);
  }
  return out;
}

/**
 * Pe ce ecran se deschide o mișcare anume: după registru și, pe art. 48, după direcție. O intrare
 * fără cod R/D e tot o ieșire (a plecat), deci stă pe „Ieșiri", unde o caută cine o repară.
 */
export function screenOfMovement(
  register: WasteRegister,
  operation: string,
  type: CompanyType | undefined
): MovementScreen {
  const visible = screensFor(type);
  if (register === "ANEXA_1") return visible.includes("GENERATED") ? "GENERATED" : visible[0];
  const screen: MovementScreen = operation === "COLLECTED" ? "IN" : "OUT";
  return visible.includes(screen) ? screen : visible[0];
}
