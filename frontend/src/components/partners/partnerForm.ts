/*
 * Constantele și ajutoarele fișei de partener, citite și de pagină, și de dialog. Erau amândouă în
 * `pages/PartnersPage.tsx`; când fișa a plecat în fișierul ei (18.09.2026), ar fi trebuit ori
 * duplicate, ori importate dintr-o pagină.
 */
import type { DriverInput } from "@/lib/types";

/** Cardul „Doar le transportă”: tipul gol (`""`), care nu poate fi valoarea unui radio. */
export const NONE_TYPE = "NONE" as const;

/** Pașii formularului: 0 detalii, 1 ce face, 2 autorizația, 3 puncte de lucru și șoferi. */
export const AUTH_STEP = 2;
export const PLACES_STEP = 3;
export const LAST_STEP = PLACES_STEP;

/** „1 șofer”, „3 șoferi”, „20 de șoferi”: de la 20 în sus româna cere „de”. */
export function countLabel(n: number, one: string, many: string): string {
  if (n === 1) return `1 ${one}`;
  const mod = n % 100;
  return mod === 0 || mod >= 20 ? `${n} de ${many}` : `${n} ${many}`;
}

export function emptyDriver(): DriverInput {
  return { name: "", identification: "", cnp: "", vehicleRegistration: "" };
}

/**
 * Aniversarea emiterii care vine după `after` — propunerea pentru perioada vizei. Procedura
 * (Ordinul 1150/2020, art. 5 alin. (4)) socotește anul de viză de la ziua și luna emiterii. Doar
 * propunere: perioada o scrie agenția pe decizie. 29 februarie cade pe 28 în anii fără el.
 */
export function nextAnniversary(issueDate: string, after: string): string {
  const month = issueDate.slice(5, 7);
  const day = month === "02" && issueDate.slice(8, 10) === "29" ? "28" : issueDate.slice(8, 10);
  const year = Number(after.slice(0, 4));
  const sameYear = `${year}-${month}-${day}`;
  return sameYear > after ? sameYear : `${year + 1}-${month}-${day}`;
}
