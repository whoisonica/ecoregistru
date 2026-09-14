import type { CompanyType, WasteRegister } from "@/lib/types";

/**
 * Mișcările se văd pe două ecrane, după registru (proprietarul, 14.09.2026): „Generare" pentru
 * deșeul firmei (Anexa 1, rapoartele de generator) și „Intrări și ieșiri" pentru marfa preluată de
 * la terți (registrul art. 48). `/miscari` rămâne adresa veche și trimite pe ecranul potrivit.
 */
export const MOVEMENTS_PATH: Record<WasteRegister, string> = {
  ANEXA_1: "/generare",
  ART_48: "/intrari-iesiri",
};

/**
 * Ce ecrane vede un tip de firmă: generatorul pe al lui, colectorul pe al lui, „Generator și
 * colector" pe amândouă. Primul e cel pe care duce `/miscari`.
 *
 * <p>⚠️ Un colector pur nu vede „Generare", deși HG 856/2002 art. 2 alin. (1) îi cere Anexa 1
 * pentru deșeul din activitatea proprie — decizia proprietarului: o firmă care are și deșeu propriu
 * se trece pe `BOTH`.
 */
export function registersFor(type: CompanyType | undefined): WasteRegister[] {
  if (type === "GENERATOR") return ["ANEXA_1"];
  if (type === "COLLECTOR") return ["ART_48"];
  return ["ANEXA_1", "ART_48"];
}
