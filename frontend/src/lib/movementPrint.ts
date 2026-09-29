import type { WasteMovement } from "./types";

/**
 * Când se poate tipări Anexa 3 sau avizul unei mișcări — pe web (lista de mișcări, registrul de
 * predări, dialogul de după salvare) și pe telefon (ecranul predării, M1e). Cod pur, fără React: aici,
 * nu în `hooks/`, fiindcă telefonul vede doar `lib/` (`mobile/metro.config.js`).
 *
 * <p>Anexa 3 la HG 1061/2008 acoperă o predare de deșeu NEPERICULOS: titlul ei spune „nepericuloase”
 * și numește un expeditor și un destinatar. Serverul refuză celelalte cazuri cu un mesaj; butonul pur
 * și simplu nu le oferă.
 */
export function canPrintAnexa3(m: WasteMovement, canWrite: boolean, readOnly = false): boolean {
  // A4: în doar-citire se retipărește cu numărul deja alocat; prima tipărire alocă unul, deci e o scriere.
  return !m.hazardous && canPrintAviz(m, canWrite) && (!readOnly || m.anexa3Number != null);
}

/**
 * Avizul de însoțire (15.09.2026): orice predare către un partener, periculoasă sau nu — avizul
 * însoțește marfa, nu descrie deșeul.
 *
 * <p>`canWrite` e obligatoriu, nu opțional (BUG-053, 20.09.2026): serverul cere `CAN_WRITE` pe
 * amândouă PDF-urile — Anexa 3 fiindcă alocă numărul formularului, avizul fiindcă tipărește CNP-ul
 * șoferului întreg, pe care listele îl maschează pentru „Vizualizare". Cât timp regula stătea numai
 * pe server, butonul se vedea și dădea 403 la clic. Fiind parametru, un ecran nou nu-l poate uita.
 *
 * <p>`canWrite` e pragul de **rol**, fără doar-citirea abonamentului (A4, 28.09.2026): în doar-citire
 * `SubscriptionAccessFilter` lasă GET-urile („Poți vedea și descărca tot”), iar avizul doar tipărește ce e
 * salvat. Doar-citirea o primește numai `canPrintAnexa3`.
 */
export function canPrintAviz(m: WasteMovement, canWrite: boolean): boolean {
  return canWrite && m.partnerId != null && (m.operation === "RECOVERED" || m.operation === "DISPOSED");
}

/** Numele fișierului, același de pe web și de pe telefon: `anexa3-150101-2026-09-26.pdf`. */
export function movementPdfName(document: "anexa3" | "aviz", m: WasteMovement): string {
  return `${document}-${m.wasteCode.replace(/\s/g, "")}-${m.date}.pdf`;
}

/**
 * Numărul formularului Anexa 3 deja emis, cum stă în registru („HMB 17” sau „17”), ori `null` dacă
 * prima tipărire n-a avut loc. Pentru avertismentul de la ștergere și editare (29.09.2026).
 */
export function anexa3NumberLabel(m: Pick<WasteMovement, "anexa3Series" | "anexa3Number">): string | null {
  if (m.anexa3Number == null) return null;
  const series = m.anexa3Series?.trim();
  return series ? `${series} ${m.anexa3Number}` : String(m.anexa3Number);
}
