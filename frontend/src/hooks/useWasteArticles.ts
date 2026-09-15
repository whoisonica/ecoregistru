import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { WasteArticle, WasteArticleInput } from "@/lib/types";

/**
 * Catalogul de sortimente al depozitului (D1.6). GET-ul întoarce și sortimentele dezactivate, ca
 * Setările să le poată reactiva; formularul de operațiune le filtrează pe cele active.
 */
export const wasteArticlesKey = ["waste-articles"] as const;

export function useWasteArticles() {
  return useQuery({
    queryKey: wasteArticlesKey,
    queryFn: async () => (await api.get<WasteArticle[]>("/api/v1/waste-articles")).data,
  });
}

export function useCreateWasteArticle() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: WasteArticleInput) =>
      (await api.post<WasteArticle>("/api/v1/waste-articles", input)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: wasteArticlesKey }),
  });
}

export function useUpdateWasteArticle() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, input }: { id: string; input: WasteArticleInput }) =>
      (await api.put<WasteArticle>(`/api/v1/waste-articles/${id}`, input)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: wasteArticlesKey }),
  });
}

export function useDeactivateWasteArticle() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/v1/waste-articles/${id}`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: wasteArticlesKey }),
  });
}

export function useReactivateWasteArticle() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.post(`/api/v1/waste-articles/${id}/reactivate`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: wasteArticlesKey }),
  });
}
