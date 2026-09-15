import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { useAuth } from "@/auth/AuthContext";
import { canWrite as roleCanWrite } from "@/lib/roles";
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
  const { user } = useAuth();
  const { data: access } = useBillingAccess();
  return roleCanWrite(user?.role) && !access?.readOnly;
}
