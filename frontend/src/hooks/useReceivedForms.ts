import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { saveBlob } from "@/lib/download";
import type { ReceivedForm, ReceivedFormInput } from "@/lib/types";

/** D2.6 — registrul formularelor primite al unui depozit, pe an. */
export const receivedFormsKey = ["received-forms"] as const;

export function useReceivedForms(workPointId: string, year: number) {
  return useQuery({
    enabled: Boolean(workPointId),
    queryKey: [...receivedFormsKey, workPointId, year],
    queryFn: async () =>
      (await api.get<ReceivedForm[]>("/api/v1/received-forms", { params: { workPointId, year } })).data,
  });
}

/** Un rând nou; cu `correctsId`, corectura rândului acela (registrul nu se rescrie). */
export function useRecordReceivedForm() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ correctsId, input }: { correctsId?: string; input: ReceivedFormInput }) =>
      (
        await api.post<ReceivedForm>(
          correctsId ? `/api/v1/received-forms/${correctsId}/correct` : "/api/v1/received-forms",
          input
        )
      ).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: receivedFormsKey }),
  });
}

export async function downloadReceivedFormsRegister(workPointId: string, year: number) {
  const res = await api.get("/api/v1/received-forms/registru", {
    params: { workPointId, year },
    responseType: "blob",
  });
  saveBlob(res.data as Blob, `registru-formulare-primite-${year}.pdf`);
}
