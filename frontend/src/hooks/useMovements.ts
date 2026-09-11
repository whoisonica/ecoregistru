import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { PageSlice, RemoteTableParams } from "@/hooks/useTableView";
import type {
  Attachment,
  MovementFilters,
  MovementSummary,
  Unit,
  WasteMovement,
  WasteMovementInput,
} from "@/lib/types";

/**
 * TanStack Query hooks for waste movements — same per-resource pattern as
 * useWorkPoints. The list key embeds the active filters so each filter combo
 * caches independently; every mutation invalidates the whole family.
 */
const movementsRoot = ["movements"] as const;
export const movementsKey = (filters: MovementFilters, table?: RemoteTableParams) =>
  [...movementsRoot, filters, table ?? null] as const;

/**
 * O **pagină** de mișcări, nu toate.
 *
 * <p>Până la P3.1, ecranul aducea tot ce lăsau filtrele să treacă și browserul făcea restul —
 * căutare, sortare, paginare. Mergea cât timp un client avea o lună de date; la doi ani, nu. Acum
 * cererea poartă și ce a tastat omul în bară, după ce coloană sortează și a câta pagină vrea, iar
 * răspunsul spune câte sunt cu totul — singurul număr din care se poate desena paginarea.
 *
 * <p>`placeholderData` ține rândurile vechi pe ecran cât timp vine pagina nouă: fără el, fiecare
 * literă tastată în căutare ar goli tabelul și l-ar umple la loc, iar ecranul ar clipi la fiecare
 * tastă.
 */
export function useMovements(filters: MovementFilters, table: RemoteTableParams) {
  return useQuery({
    queryKey: movementsKey(filters, table),
    placeholderData: keepPreviousData,
    queryFn: async () => {
      const params: Record<string, string | number | boolean> = {
        page: table.page,
        size: table.size,
      };
      if (filters.year != null) params.year = filters.year;
      if (filters.month != null) params.month = filters.month;
      if (filters.workPointId) params.workPointId = filters.workPointId;
      if (filters.wasteCodeId) params.wasteCodeId = filters.wasteCodeId;
      if (filters.leftSite) params.leftSite = true;
      if (filters.missingOperationCode) params.missingOperationCode = true;
      if (table.search) params.search = table.search;
      if (table.sort) {
        params.sort = table.sort;
        params.asc = table.asc;
      }
      return (await api.get<PageSlice<WasteMovement>>("/api/v1/movements", { params })).data;
    },
  });
}

/**
 * O singură mișcare, cerută după id — pentru linkurile care numesc un rând (`?miscare=…`).
 *
 * <p>Necesară de când lista vine pe pagini: rândul pe care îl numește un raport poate să nu fie
 * printre cele 25 aduse, iar până acum ecranul spunea „mișcarea nu mai există" tocmai fiindcă nu
 * era pe pagina întâi. Se cere numai când există un id, și numai o dată.
 */
export function useMovement(id: string | null) {
  return useQuery({
    queryKey: [...movementsRoot, "one", id] as const,
    enabled: Boolean(id),
    queryFn: async () => (await api.get<WasteMovement>(`/api/v1/movements/${id}`)).data,
  });
}

/**
 * Cele două cifre ale Panoului despre luna curentă: câte mișcări și câte kilograme.
 *
 * <p>Se cer anume, fiindcă nu se mai pot aduna acasă. Până la P3.1 panoul cerea toate mișcările
 * lunii și le însuma în browser; cu o pagină de 25 de rânduri aceeași însumare ar fi dat totalul
 * **paginii** sub titlul „luna aceasta" — un număr mai mic decât adevărul, care nu spune că e mai
 * mic. Serverul socotește peste luna întreagă, în kilograme.
 */
export function useMovementSummary(year: number, month: number) {
  return useQuery({
    queryKey: [...movementsRoot, "summary", year, month] as const,
    queryFn: async () =>
      (
        await api.get<MovementSummary>("/api/v1/movements/summary", {
          params: { year, month },
        })
      ).data,
  });
}

function invalidateAll(qc: ReturnType<typeof useQueryClient>) {
  return qc.invalidateQueries({ queryKey: movementsRoot });
}

export function useCreateMovement() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: WasteMovementInput) =>
      (await api.post<WasteMovement>("/api/v1/movements", input)).data,
    onSuccess: () => invalidateAll(qc),
  });
}

export function useUpdateMovement() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, input }: { id: string; input: WasteMovementInput }) =>
      (await api.put<WasteMovement>(`/api/v1/movements/${id}`, input)).data,
    onSuccess: () => invalidateAll(qc),
  });
}

/**
 * The weight, once the recipient sent it back. A separate call from the full update on purpose:
 * the movement form greys the quantity out while "se cântărește la descărcare" is ticked, so
 * before this there was no way in that did not also throw away the fact of who weighed it.
 */
export function useRecordWeight() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, quantity, unit }: { id: string; quantity: number; unit: Unit }) =>
      (
        await api.post<WasteMovement>(`/api/v1/movements/${id}/weight`, { quantity, unit })
      ).data,
    onSuccess: () => invalidateAll(qc),
  });
}

export function useDeleteMovement() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/v1/movements/${id}`);
    },
    onSuccess: () => invalidateAll(qc),
  });
}

export function useAddAttachment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ movementId, file }: { movementId: string; file: File }) => {
      const form = new FormData();
      form.append("file", file);
      return (
        await api.post<Attachment>(`/api/v1/movements/${movementId}/attachments`, form)
      ).data;
    },
    onSuccess: () => invalidateAll(qc),
  });
}

export function useDeleteAttachment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ movementId, attachmentId }: { movementId: string; attachmentId: string }) => {
      await api.delete(`/api/v1/movements/${movementId}/attachments/${attachmentId}`);
    },
    onSuccess: () => invalidateAll(qc),
  });
}
