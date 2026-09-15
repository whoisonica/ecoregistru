import {
  ArrowDownToLine,
  ArrowUpFromLine,
  Briefcase,
  Building2,
  CalendarClock,
  Factory,
  FileSpreadsheet,
  FileUp,
  FolderArchive,
  Home,
  Package,
  Receipt,
  Settings,
  Users,
  type LucideIcon,
} from "lucide-react";
import type { Role } from "@/auth/AuthContext";
import type { CompanyType } from "@/lib/types";
import { canManage, isMultiCompany } from "@/lib/roles";
import { SCREEN_PATH, screensFor, type MovementScreen } from "@/lib/movementScreens";
import { strings } from "@/lib/strings";

export interface NavEntry {
  to: string;
  label: string;
  icon: LucideIcon;
  end?: boolean;
  /**
   * Cuvintele după care paleta (Ctrl+K) mai găsește ecranul, pe lângă numele lui. Stau aici, nu
   * într-o listă a paletei: meniul e singura sursă a ecranelor, iar o a doua listă care trebuie să
   * spună același lucru ajunge mereu să nu-l mai spună.
   */
  keywords?: string;
  /** Tasta din panou: cifrele 1–9, 0 în ordinea afișată; litere la grupul Cabinet. */
  hotkey?: string;
  /** Ecranul de mișcări din spatele intrării, când e unul: după el se aleg indicatorii. */
  screen?: MovementScreen;
}

export interface NavModel {
  /** Meniul principal, în ordinea afișată; fiecare cu tasta ei. */
  main: NavEntry[];
  /** Grupul Cabinet: numai consultantul și platforma. */
  cabinet: NavEntry[];
  /** Ecranele care există, dar nu stau în meniu: le găsește paleta și le duc linkurile din pagini. */
  hidden: NavEntry[];
}

const DIGITS = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "0"];

const SCREEN_ENTRY: Record<MovementScreen, Omit<NavEntry, "hotkey">> = {
  GENERATED: {
    to: SCREEN_PATH.GENERATED,
    label: strings.nav.movementsGenerator,
    icon: Factory,
    keywords: strings.nav.kwMovements,
    screen: "GENERATED",
  },
  IN: {
    to: SCREEN_PATH.IN,
    label: strings.nav.movementsIn,
    icon: ArrowDownToLine,
    keywords: strings.nav.kwMovementsIn,
    screen: "IN",
  },
  OUT: {
    to: SCREEN_PATH.OUT,
    label: strings.nav.movementsOut,
    icon: ArrowUpFromLine,
    keywords: strings.nav.kwMovementsOut,
    screen: "OUT",
  },
};

/**
 * Meniul panoului, după tipul firmei și rol — inventarul din `todo-ui-cantar.md` §5, citit din cod.
 *
 * <p>Fiecare ecran de lucru e intrare proprie, nu subpagină: Generare / Intrări / Ieșiri separate
 * (colectorul pur fără Generare — decizia proprietarului din 14.09.2026: firma cu deșeu propriu se
 * trece pe `BOTH`). Până se știe firma (consultantul fără firmă aleasă) rămân intrările comune.
 *
 * <p>Importul din Excel nu mai e în meniu: e un lucru de făcut o dată, la implementare, și stă la
 * Setări (link) și în paletă. Abonamentul stă jos în panou, nu în listă.
 */
export function buildNav(role: Role | undefined, companyType: CompanyType | undefined): NavModel {
  const main: Omit<NavEntry, "hotkey">[] = [
    { to: "/", label: strings.nav.dashboard, icon: Home, end: true, keywords: strings.nav.kwDashboard },
  ];
  if (companyType) {
    for (const screen of screensFor(companyType)) main.push(SCREEN_ENTRY[screen]);
  }
  main.push(
    { to: "/ambalaje", label: strings.nav.packaging, icon: Package, keywords: strings.nav.kwPackaging },
    { to: "/evidente", label: strings.nav.evidences, icon: FileSpreadsheet, keywords: strings.nav.kwEvidences },
    { to: "/termene", label: strings.nav.deadlines, icon: CalendarClock, keywords: strings.nav.kwDeadlines },
    { to: "/dosar-control", label: strings.nav.auditFile, icon: FolderArchive, keywords: strings.nav.kwAuditFile },
    { to: "/parteneri", label: strings.nav.partners, icon: Users, keywords: strings.nav.kwPartners },
    { to: "/setari", label: strings.nav.settings, icon: Settings, keywords: strings.nav.kwSettings }
  );

  const cabinet: NavEntry[] = [];
  if (role === "CONSULTANT") {
    cabinet.push({
      to: "/cabinet",
      label: strings.nav.consultancyOverview,
      icon: Briefcase,
      keywords: strings.nav.kwConsultancyOverview,
      hotkey: "F",
    });
  }
  if (isMultiCompany(role)) {
    cabinet.push({
      to: "/clienti",
      label: strings.nav.clients,
      icon: Building2,
      keywords: strings.nav.kwClients,
      hotkey: "C",
    });
  }

  const hidden: NavEntry[] = [];
  if (canManage(role)) {
    hidden.push(
      { to: "/import", label: strings.nav.importExcel, icon: FileUp, keywords: strings.nav.kwImport },
      { to: "/abonament", label: strings.nav.billing, icon: Receipt, keywords: strings.nav.kwBilling }
    );
  }

  return {
    main: main.map((entry, i) => ({ ...entry, hotkey: DIGITS[i] })),
    cabinet,
    hidden,
  };
}
