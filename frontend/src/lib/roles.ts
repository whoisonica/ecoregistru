import type { Role } from "@/auth/AuthContext";

/**
 * Pragurile de rol, într-un singur loc.
 *
 * <p>Erau scrise de mână pe fiecare ecran — `role === "PLATFORM_ADMIN" || role === "ADMIN" || …` —
 * în opt locuri. Câtă vreme rolurile nu se schimbau, copiile spuneau același lucru. Cu P2.13 a
 * apărut `CONSULTANT`, și un ecran uitat ar fi fost un consultant care vede „Adaugă mișcare" pe o
 * firmă și nu-l poate apăsa. Oglindesc exact pragurile din backend (`CAN_WRITE`, `CAN_MANAGE`,
 * `MULTI_COMPANY`); backendul rămâne cel care refuză.
 */

/** Scrie înregistrări: mișcări, parteneri, evidențe, termene, ambalaje. */
export function canWrite(role: Role | undefined): boolean {
  return role === "PLATFORM_ADMIN" || role === "CONSULTANT" || role === "ADMIN" || role === "OPERATOR";
}

/** Administrează firma: puncte de lucru, utilizatori, jurnalul de audit. */
export function canManage(role: Role | undefined): boolean {
  return role === "PLATFORM_ADMIN" || role === "CONSULTANT" || role === "ADMIN";
}

/**
 * Lucrează pe mai multe firme, deci alege una: comutatorul din bara laterală, ecranul Clienți și
 * starea „nicio firmă aleasă". Pentru toți ceilalți firma vine din token.
 */
export function isMultiCompany(role: Role | undefined): boolean {
  return role === "PLATFORM_ADMIN" || role === "CONSULTANT";
}
