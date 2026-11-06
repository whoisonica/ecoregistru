import type { Role } from "@/auth/AuthContext";
import type { CompanyType } from "@/lib/types";
import { SCREEN_PATH, screensFor } from "@/lib/movementScreens";
import { canManage, canWrite } from "@/lib/roles";
import { strings } from "@/lib/strings";

/**
 * Turul aplicației (06.11.2026): la prima intrare a unui client, turul trece prin ecranele reale ale
 * generatorului și pune lângă fiecare lucru o casetă scurtă. Fișierul ține numai pașii, fără DOM și
 * fără React, ca să se poată proba sub `npm test`; motorul e în `components/tour/`.
 *
 * <p>Ținta unui pas e un element marcat cu `data-tour="<ținta>"` în pagină. Un pas care **cere**
 * ținta se sare când ea lipsește (rolul n-o vede, lista „Primii pași” e deja gata); unul care n-o
 * cere își arată caseta în mijloc. Depozitul și aplicația mobilă nu intră în tur (proprietarul,
 * 17.09.2026: scopul e generatorul).
 */
export type TourTarget =
  | "nav"
  | "search"
  | "first-steps"
  | "add-waste"
  | "movements-list"
  | "tab-total"
  | "tab-ambalaje"
  | "deadlines-todo"
  | "audit-download"
  | "partners-list"
  | "page-header";

export interface TourStep {
  id: string;
  /** Ecranul pe care stă pasul: turul navighează acolo înainte să arate caseta. */
  path: string;
  /** Ce luminează (`[data-tour="…"]`); `null` = caseta în mijloc, fără decupaj. */
  target: TourTarget | null;
  /** `true`: fără țintă în pagină, pasul se sare. `false`: fără țintă, caseta stă în mijloc. */
  requiresTarget: boolean;
  /** Numai pe ecran lat (≥ 1024px): meniul cu cifre și Ctrl K nu există pe telefon. */
  desktopOnly?: boolean;
  title: string;
  body: string;
}

/** Rolurile cărora turul le pornește singur, la prima intrare: oamenii firmei, fără depozit. */
const AUTO_START_ROLES: readonly Role[] = ["ADMIN", "OPERATOR", "CLIENT_VIEWER"];

/** Turul e al firmei cu ecranul „Generare” (generator, sau generator și colector). */
export function hasTour(companyType: CompanyType | undefined): boolean {
  return Boolean(companyType) && screensFor(companyType).includes("GENERATED");
}

/**
 * Pornește singur numai la cine n-a văzut turul încă. `tourSeen` lipsă (o sesiune salvată înainte
 * de tur) înseamnă „văzut”: cine lucra deja în aplicație nu primește turul peste ecran.
 */
export function shouldAutoStart(
  user: { role: Role; tourSeen?: boolean } | null | undefined,
  companyType: CompanyType | undefined
): boolean {
  return Boolean(user) && user!.tourSeen === false && AUTO_START_ROLES.includes(user!.role) && hasTour(companyType);
}

/** Pașii turului pentru rolul și firma date; listă goală unde turul nu există (colectorul pur). */
export function tourSteps(input: { role: Role | undefined; companyType: CompanyType | undefined }): TourStep[] {
  const { role, companyType } = input;
  if (!hasTour(companyType)) return [];
  const t = strings.tour;
  const writer = canWrite(role);
  const generare = SCREEN_PATH.GENERATED;

  return [
    { id: "welcome", path: "/", target: null, requiresTarget: false, title: t.welcomeTitle, body: t.welcomeBody },
    { id: "nav", path: "/", target: "nav", requiresTarget: true, desktopOnly: true, title: t.navTitle, body: t.navBody },
    {
      id: "first-steps",
      path: "/",
      target: "first-steps",
      requiresTarget: true,
      title: t.firstStepsTitle,
      body: t.firstStepsBody,
    },
    // Fără „Adaugă deșeuri” pe ecran (abonamentul în doar-citire îl stinge), pasul rămâne, cu caseta în mijloc:
    // predările sunt ecranul principal, nu se sar.
    writer
      ? { id: "movements", path: generare, target: "add-waste", requiresTarget: false, title: t.movementsTitle, body: t.movementsBody }
      : {
          id: "movements",
          path: generare,
          target: "movements-list",
          requiresTarget: false,
          title: t.movementsTitle,
          body: t.movementsBodyViewer,
        },
    { id: "annual", path: generare, target: "tab-total", requiresTarget: false, title: t.annualTitle, body: t.annualBody },
    {
      id: "packaging",
      path: generare,
      target: "tab-ambalaje",
      requiresTarget: false,
      title: t.packagingTitle,
      body: t.packagingBody,
    },
    {
      id: "deadlines",
      path: "/termene",
      target: "deadlines-todo",
      requiresTarget: false,
      title: t.deadlinesTitle,
      body: t.deadlinesBody,
    },
    { id: "energy", path: "/energie", target: "page-header", requiresTarget: false, title: t.energyTitle, body: t.energyBody },
    {
      id: "environmental-report",
      path: "/raport-mediu",
      target: "page-header",
      requiresTarget: false,
      title: t.environmentalReportTitle,
      body: t.environmentalReportBody,
    },
    {
      id: "audit-file",
      path: "/dosar-control",
      target: "audit-download",
      requiresTarget: false,
      title: t.auditFileTitle,
      body: t.auditFileBody,
    },
    {
      id: "partners",
      path: "/parteneri",
      target: "partners-list",
      requiresTarget: false,
      title: t.partnersTitle,
      body: t.partnersBody,
    },
    {
      id: "settings",
      path: "/setari",
      target: "page-header",
      requiresTarget: false,
      title: t.settingsTitle,
      body: canManage(role) ? t.settingsBody : t.settingsBodyNoTeam,
    },
    {
      id: "search",
      path: "/setari",
      target: "search",
      requiresTarget: true,
      desktopOnly: true,
      title: t.searchTitle,
      body: t.searchBody,
    },
    { id: "end", path: "/", target: null, requiresTarget: false, title: t.endTitle, body: t.endBody },
  ];
}

/**
 * Pasul următor (sau precedent) care se poate arăta: sare peste pașii „numai pe ecran lat” pe
 * telefon. `null` = nu mai e niciunul în direcția aceea. Lipsa țintei se vede doar în pagină, deci
 * o hotărăște motorul, cu aceeași funcție chemată din nou de la pasul sărit.
 */
export function nextVisibleIndex(steps: readonly TourStep[], from: number, direction: 1 | -1, wide: boolean): number | null {
  for (let i = from + direction; i >= 0 && i < steps.length; i += direction) {
    if (!steps[i].desktopOnly || wide) return i;
  }
  return null;
}
