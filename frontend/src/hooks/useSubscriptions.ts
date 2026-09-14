import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { Subscription, SubscriptionInput, SubscriptionOwner } from "@/lib/types";

/**
 * Plata abonamentelor, F1 — abonamentul unei firme directe sau al unui cabinet. Numai platforma.
 * Serverul răspunde 204 pentru un client fără abonament, adică nefacturat: aici devine `null`.
 */
const subscriptionKey = (owner: SubscriptionOwner) => ["subscriptions", owner.kind, owner.id] as const;
const foundersKey = ["subscriptions", "founders"] as const;
const pathOf = (owner: SubscriptionOwner) => `/api/v1/subscriptions/${owner.kind}/${owner.id}`;

export function useSubscription(owner: SubscriptionOwner) {
  return useQuery({
    queryKey: subscriptionKey(owner),
    queryFn: async () => {
      const res = await api.get<Subscription>(pathOf(owner));
      return res.status === 204 ? null : res.data;
    },
  });
}

/** „X din 30" lângă bifa de fondator. */
export function useFounderCount() {
  return useQuery({
    queryKey: foundersKey,
    queryFn: async () => (await api.get<number>("/api/v1/subscriptions/founders")).data,
  });
}

export function useSaveSubscription(owner: SubscriptionOwner) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: SubscriptionInput) =>
      (await api.put<Subscription>(pathOf(owner), input)).data,
    onSuccess: (saved) => {
      qc.setQueryData(subscriptionKey(owner), saved);
      qc.invalidateQueries({ queryKey: foundersKey });
    },
  });
}

export function useDeleteSubscription(owner: SubscriptionOwner) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      await api.delete(pathOf(owner));
    },
    onSuccess: () => {
      qc.setQueryData(subscriptionKey(owner), null);
      qc.invalidateQueries({ queryKey: foundersKey });
    },
  });
}
