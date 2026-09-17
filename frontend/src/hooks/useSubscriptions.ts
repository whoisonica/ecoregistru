import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type {
  BillingAccount,
  InvoiceFilter,
  InvoiceMoney,
  InvoicePage,
  LastBillingRun,
  CardPaymentResult,
  PaymentMethod,
  BillingRunResult,
  Subscription,
  SubscriptionInput,
  SubscriptionOwner,
} from "@/lib/types";

/**
 * F2 — abonamentul pe care îl plătește contul, pe `/abonament`: al cabinetului pentru un consultant,
 * al firmei pentru administratorul ei. 204 (nimic de plătit) devine `null`.
 */
export function useBillingAccount() {
  return useQuery({
    queryKey: ["billing"],
    queryFn: async () => {
      const res = await api.get<BillingAccount>("/api/v1/billing");
      return res.status === 204 ? null : res.data;
    },
  });
}

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

/** F2 — rularea zilnică a facturării, acum. De două ori la rând nu emite nimic de două ori. */
export function useRunBilling() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async () => (await api.post<BillingRunResult>("/api/v1/subscriptions/billing/run")).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["subscriptions"] });
    },
  });
}

/** F-A — ultima rulare salvată; `null` înainte de prima. Numai platforma. */
export function useLastBillingRun() {
  return useQuery({
    queryKey: ["subscriptions", "runs", "last"],
    queryFn: async () => {
      const res = await api.get<LastBillingRun>("/api/v1/subscriptions/billing/runs/last");
      return res.status === 204 ? null : res.data;
    },
  });
}

export interface InvoiceQuery {
  filter: InvoiceFilter;
  /** `2026-09`; gol = toate lunile. */
  month: string;
  q: string;
  page: number;
  size: number;
}

/**
 * F-B2 — o pagină din facturile tuturor clienților, tăiată pe server. Pagina de dinainte rămâne pe ecran cât vine
 * următoarea, ca tabelul să nu clipească la fiecare tastă din căutare.
 */
export function useInvoicePage(query: InvoiceQuery) {
  return useQuery({
    queryKey: ["subscriptions", "invoices", query],
    queryFn: async () =>
      (
        await api.get<InvoicePage>("/api/v1/subscriptions/invoices", {
          params: { ...query, month: query.month || undefined, q: query.q.trim() || undefined },
        })
      ).data,
    placeholderData: keepPreviousData,
  });
}

/** F-B — încasat luna asta și de încasat, pentru cifrele de pe Clienți. */
export function useInvoiceMoney(enabled: boolean) {
  return useQuery({
    queryKey: ["subscriptions", "invoices", "money"],
    queryFn: async () => (await api.get<InvoiceMoney>("/api/v1/subscriptions/invoices/money")).data,
    enabled,
  });
}

/** F-A — „Verifică plata acum” pe o factură: FGO întrebat doar de ea. */
export function useCheckInvoicePayment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (invoiceId: string) => {
      await api.post(`/api/v1/subscriptions/invoices/${invoiceId}/check-payment`);
    },
    onSettled: () => qc.invalidateQueries({ queryKey: ["subscriptions"] }),
  });
}

/** F-A — „Oprește” pe o factură refuzată de FGO: pleacă ea și abonamentul ei. */
export function useDiscardInvoice() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (invoiceId: string) => {
      await api.post(`/api/v1/subscriptions/invoices/${invoiceId}/discard`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ["subscriptions"] }),
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

/** F3 — cardul sau transferul, ales de cine plătește. */
export function useChoosePaymentMethod() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (paymentMethod: PaymentMethod) => {
      await api.put("/api/v1/billing/payment-method", { paymentMethod });
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ["billing"] }),
  });
}

/** F3 — pagina Netopia pentru o factură emisă. Apelantul duce omul acolo. */
export function usePayInvoiceByCard() {
  return useMutation({
    mutationFn: async (invoiceId: string) =>
      (await api.post<{ paymentUrl: string }>(`/api/v1/billing/invoices/${invoiceId}/card`)).data.paymentUrl,
  });
}

/**
 * F3 — plata la care Netopia întoarce omul (`/abonament?plata=…`). Se citește din 3 în 3 secunde cât
 * notificarea n-a ajuns încă: redirecționarea vine de obicei înaintea ei.
 */
export function useCardPayment(id: string | null) {
  return useQuery({
    queryKey: ["billing", "card-payment", id],
    queryFn: async () => (await api.get<CardPaymentResult>(`/api/v1/billing/card-payments/${id}`)).data,
    enabled: Boolean(id),
    refetchInterval: (query) => (query.state.data?.status === "STARTED" ? 3000 : false),
  });
}

/** F4, §9.3 — oprirea cu preaviz și anularea ei. Numai platforma. */
export function useCancelSubscription(owner: SubscriptionOwner) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (action: "cancel" | "resume") =>
      (await api.post<Subscription>(`${pathOf(owner)}/${action}`)).data,
    onSuccess: (saved) => qc.setQueryData(subscriptionKey(owner), saved),
  });
}
