import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { useAuth } from "@/auth/AuthContext";
import { canWeigh as roleCanWeigh, canWrite as roleCanWrite } from "@/lib/roles";
import type { BillingAccess } from "@/lib/types";

export const billingAccessKey = ["billing", "access"] as const;

/**
 * F4 — starea abonamentului care plătește contul, pentru orice rol: bannerul de sus și butoanele de scriere.
 * Serverul refuză oricum scrierea (`SubscriptionAccessFilter`); aici e doar ca omul să nu apese degeaba.
 */
export function useBillingAccess() {
  const { user } = useAuth();
  return useQuery({
    queryKey: billingAccessKey,
    queryFn: async () => (await api.get<BillingAccess>("/api/v1/billing/access")).data,
    enabled: Boolean(user),
    staleTime: 60_000,
  });
}

/**
 * Scrie înregistrări: pragul de rol și, peste el, doar-citirea abonamentului.
 *
 * <p>⚠️ Importul din `roles` e aliasat: `const canWrite = canWrite(...)` trece de build și cade la randare
 * (capcana din P2.13). De aceea paginile cheamă `useCanWrite()`, nu funcția.
 */
export function useCanWrite(): boolean {
  const { canWrite, readOnly } = usePrintAccess();
  return canWrite && !readOnly;
}

/**
 * A4 — cele două praguri despărțite, pentru tipăriri (`lib/movementPrint.ts`): documentele sunt GET-uri, pe
 * care doar-citirea nu le oprește.
 */
export function usePrintAccess(): { canWrite: boolean; readOnly: boolean } {
  const { user } = useAuth();
  const { data: access } = useBillingAccess();
  return { canWrite: roleCanWrite(user?.role), readOnly: access?.readOnly === true };
}

/** Cântărește acum: pragul de cântar (`canWeigh`) și, peste el, doar-citirea abonamentului. */
export function useCanWeigh(): boolean {
  const { user } = useAuth();
  const { data: access } = useBillingAccess();
  return roleCanWeigh(user?.role) && !access?.readOnly;
}
