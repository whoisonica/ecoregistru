import type { Role } from "@/auth/AuthContext";
import type { InviteRole } from "@/lib/types";

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

/**
 * Cântărește la depozit: operațiunile în lucru, balotarea și tipăriturile lor (`WeighingOperationController.CAN_WEIGH`).
 * „Operator”-ul de birou nu mai cântărește (proprietarul, 28.09.2026): la cântar lucrează operatorul de cântar.
 */
export function canWeigh(role: Role | undefined): boolean {
  return role === "PLATFORM_ADMIN" || role === "CONSULTANT" || role === "ADMIN" || role === "SCALE_OPERATOR";
}

/** Vede și ține cântarele, cu verificările BRML (`ScaleController.CAN_MANAGE`): aceiași care cântăresc. */
export function canManageScales(role: Role | undefined): boolean {
  return canWeigh(role);
}

/** Rolurile de la invitație; operatorul de cântar doar la o firmă cu depozit. */
export function inviteRoles(hasDepot: boolean): InviteRole[] {
  return hasDepot ? ["ADMIN", "OPERATOR", "SCALE_OPERATOR", "CLIENT_VIEWER"] : ["ADMIN", "OPERATOR", "CLIENT_VIEWER"];
}

/** Administrează firma: puncte de lucru, utilizatori, jurnalul de audit. */
export function canManage(role: Role | undefined): boolean {
  return role === "PLATFORM_ADMIN" || role === "CONSULTANT" || role === "ADMIN";
}

/**
 * Aduce istoricul din Excel (`ImportController`, `CAN_IMPORT`). Numai platforma: importul e partea
 * noastră din implementare (proprietarul, 16.09.2026). Clientul și consultantul ne trimit fișierul.
 */
export function canImport(role: Role | undefined): boolean {
  return role === "PLATFORM_ADMIN";
}

/**
 * Lucrează pe mai multe firme, deci alege una: comutatorul din bara laterală, ecranul Clienți și
 * starea „nicio firmă aleasă". Pentru toți ceilalți firma vine din token.
 */
export function isMultiCompany(role: Role | undefined): boolean {
  return role === "PLATFORM_ADMIN" || role === "CONSULTANT";
}
