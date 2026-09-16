import { useQuery } from "@tanstack/react-query";
import { screensFor, type MovementScreen } from "@/lib/movementScreens";
import { strings } from "@web/strings";

import * as api from "./api";
import { cached } from "./outbox";
import { useSession } from "./session";

/**
 * Tipul firmei pe care lucrează sesiunea — generator, colector sau amândouă.
 *
 * <p>Nu vine în răspunsul de login (nici pe web), fiindcă un consultant schimbă firma fără să se
 * relogheze. Se cere de la `/companies/current`, care răspunde pe firma din `X-Tenant-Id`.
 *
 * <p>Ce hotărăște: ce ecrane de mișcări are bara de jos și ce direcție arată afișajul lunii.
 * Regula e chiar cea de pe web (`lib/movementScreens.ts`), importată, nu rescrisă — altfel
 * telefonul ar fi ajuns să spună altceva decât ecranul pe care omul îl știe.
 */
export function useCompany() {
  const { auth, session } = useSession();
  return useQuery({
    queryKey: ["company", "current", session?.tenantId],
    // Ținut și pe telefon: formularul de predare are nevoie de profilul firmei și fără semnal.
    queryFn: () => cached(`${session?.tenantId}:company`, () => api.currentCompany(auth!)),
    enabled: !!auth && !!session?.tenantId,
    staleTime: 5 * 60 * 1000,
  });
}

/** Ecranele de mișcări ale firmei, în ordinea de pe web. Gol cât timp tipul firmei nu se știe. */
export function useMovementScreens(): MovementScreen[] {
  const company = useCompany();
  return company.data ? screensFor(company.data.type) : [];
}

/** Eticheta fiecărui ecran de mișcări — în bara de jos, pe comutator și în antet. */
export const SCREEN_LABEL: Record<MovementScreen, string> = {
  GENERATED: strings.nav.movementsGenerator,
  IN: strings.mobile.tabIn,
  OUT: strings.mobile.tabOut,
};
