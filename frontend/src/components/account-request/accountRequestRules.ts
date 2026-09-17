/*
 * Regulile cererii de cont, scoase din `pages/AccountRequestPage.tsx` pe 18.09.2026 (pagina trecuse
 * de 1.000 de linii). Funcții pure peste valorile formularului, ca `movementRules.ts`: ce e
 * obligatoriu pe fiecare pas și pe ce pas stă o rubrică greșită. Nimic din React aici, deci se pot
 * proba fără browser — vezi `accountRequestRules.test.ts`.
 */
import type { CompanyType, MarketRole } from "@/lib/types";
import { isValidCui } from "@/lib/cui";
import { strings } from "@/lib/strings";

const t = strings.accountRequest;

/** Deliberat larg: validarea de email a browserului respinge deja ce e evident stricat. */
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export type Step = 1 | 2 | 3 | 4;

export type FieldErrors = Partial<
  Record<
    | "companyName"
    | "cui"
    | "companyAddress"
    | "caenCode"
    | "workPointName"
    | "workPointAddress"
    | "authNumber"
    | "authExpiry"
    | "transportMeans"
    | "transportLicenseNumber"
    | "transportLicenseExpiry"
    | "contactName"
    | "contactEmail"
    | "contactPhone"
    | "contactRole"
    | "marketRoles",
    string
  >
>;

/** Rubricile pe care le citesc regulile — un subset din ce ține formularul. */
export interface AccountRequestValues {
  companyName: string;
  cui: string;
  companyType: CompanyType;
  companyAddress: string;
  caenCode: string;
  workPointName: string;
  workPointAddress: string;
  authNumber: string;
  authExpiry: string;
  noEnvAuth: boolean;
  transportMeans: string;
  transportLicenseNumber: string;
  transportLicenseExpiry: string;
  contactName: string;
  contactEmail: string;
  contactPhone: string;
  contactRole: string;
  marketRoles: MarketRole[];
}

/** Numai cine ia deșeuri de la terți are transport de declarat. */
export const asksTransport = (companyType: CompanyType) => companyType !== "GENERATOR";

/** Doar cine generează are „tipul de generator” (producător / importator / comerciant). */
export const asksMarketRoles = (companyType: CompanyType) => companyType !== "COLLECTOR";

/** Bifa „n-avem nevoie de autorizație” există doar la generatorul pur; colectorul are întotdeauna. */
export const mayLackEnvAuth = (companyType: CompanyType) => companyType === "GENERATOR";

export const skipsEnvAuth = (v: Pick<AccountRequestValues, "companyType" | "noEnvAuth">) =>
  mayLackEnvAuth(v.companyType) && v.noEnvAuth;

/**
 * Rubricile obligatorii ale unui pas; `only` lipsă = toate (la trimitere). Aproape totul e
 * obligatoriu (proprietarul, 16.09.2026): ce lipsea cerea un telefon la aprobare. Rămân libere
 * doar textele („alte deșeuri”, observațiile) și lista de deșeuri, unde golul e un răspuns.
 */
export function validate(v: AccountRequestValues, only?: Step): FieldErrors {
  const errs: FieldErrors = {};
  const need = (value: string, key: keyof FieldErrors, message: string = t.errRequired) => {
    if (!value.trim()) errs[key] = message;
  };
  if (only === undefined || only === 1) {
    need(v.companyName, "companyName", t.errCompanyName);
    const normalizedCui = v.cui.replace(/\s/g, "").toUpperCase();
    if (!normalizedCui) errs.cui = t.errCui;
    else if (!isValidCui(normalizedCui)) errs.cui = t.errCuiFormat;
    need(v.companyAddress, "companyAddress");
    need(v.caenCode, "caenCode");
  }
  if (only === undefined || only === 2) {
    need(v.workPointName, "workPointName");
    need(v.workPointAddress, "workPointAddress");
    if (!skipsEnvAuth(v)) {
      need(v.authNumber, "authNumber");
      need(v.authExpiry, "authExpiry", t.errRequiredDate);
    }
    if (asksTransport(v.companyType)) {
      need(v.transportMeans, "transportMeans");
      need(v.transportLicenseNumber, "transportLicenseNumber");
      need(v.transportLicenseExpiry, "transportLicenseExpiry", t.errRequiredDate);
    }
  }
  if (only === undefined || only === 3) {
    need(v.contactName, "contactName");
    need(v.contactPhone, "contactPhone");
    need(v.contactRole, "contactRole");
    if (!v.contactEmail.trim()) errs.contactEmail = t.errContactEmail;
    else if (!EMAIL_PATTERN.test(v.contactEmail.trim())) errs.contactEmail = t.errContactEmailFormat;
  }
  if (only === undefined || only === 4) {
    if (asksMarketRoles(v.companyType) && v.marketRoles.length === 0) errs.marketRoles = t.errMarketRoles;
  }
  return errs;
}

/** Pe ce pas stă o rubrică greșită — ca trimiterea să ducă omul înapoi la ea, nu doar s-o marcheze. */
export function stepOf(errs: FieldErrors): Step {
  if (errs.companyName || errs.cui || errs.companyAddress || errs.caenCode) return 1;
  if (
    errs.workPointName ||
    errs.workPointAddress ||
    errs.authNumber ||
    errs.authExpiry ||
    errs.transportMeans ||
    errs.transportLicenseNumber ||
    errs.transportLicenseExpiry
  )
    return 2;
  if (errs.contactName || errs.contactEmail || errs.contactPhone || errs.contactRole) return 3;
  return 4;
}
