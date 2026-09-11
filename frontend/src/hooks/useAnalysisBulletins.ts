import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, apiBlobErrorMessage } from "@/lib/api";
import { openBlankTab, openBlobInTab } from "@/lib/openFileInTab";
import { strings } from "@/lib/strings";
import { useToast } from "@/components/ui/toast";
import type { AnalysisBulletin } from "@/lib/types";

const KEY = ["analysis-bulletins"];

/**
 * Buletinele de analiză ale firmei — OUG 92/2021 art. 8 alin. (4) și art. 48 alin. (2).
 *
 * <p>Invalidarea atinge **și** mișcările: un buletin nou pe un cod-oglindă stinge badge-ul
 * „Cod-oglindă" de pe toate mișcările acelui cod (G-4 și-a mutat sursa aici), iar un registru
 * rămas cu badge-ul aprins după ce dovada tocmai a fost încărcată ar arăta ca un defect.
 */
export function useAnalysisBulletins() {
  return useQuery({
    queryKey: KEY,
    queryFn: async () => (await api.get<AnalysisBulletin[]>("/api/v1/analysis-bulletins")).data,
  });
}

export function useCreateAnalysisBulletin() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: {
      wasteCodeId: string;
      issueDate: string;
      laboratory: string;
      file: File;
    }) => {
      const form = new FormData();
      form.append("wasteCodeId", input.wasteCodeId);
      form.append("issueDate", input.issueDate);
      form.append("laboratory", input.laboratory);
      form.append("file", input.file);
      return (await api.post<AnalysisBulletin>("/api/v1/analysis-bulletins", form)).data;
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: KEY, refetchType: "all" });
      qc.invalidateQueries({ queryKey: ["movements"], refetchType: "all" });
    },
  });
}

export function useDeleteAnalysisBulletin() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/v1/analysis-bulletins/${id}`);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: KEY, refetchType: "all" });
      qc.invalidateQueries({ queryKey: ["movements"], refetchType: "all" });
    },
  });
}

/** Deschide buletinul în tab, cu sesiunea omului. Aceeași regulă ca la atașamente — vezi `openFileInTab`. */
export function useBulletinOpen() {
  const [openingId, setOpeningId] = useState<string | null>(null);
  const { notify } = useToast();

  async function open(b: AnalysisBulletin) {
    setOpeningId(b.id);
    const tab = openBlankTab();
    try {
      const res = await api.get(`/api/v1/analysis-bulletins/${b.id}/continut`, {
        responseType: "blob",
      });
      openBlobInTab(tab, res.data as Blob, b.fileName || "buletin-analiza");
    } catch (err) {
      tab?.close();
      notify(
        await apiBlobErrorMessage(err, strings.settings.bulletins.openError),
        "error",
      );
    } finally {
      setOpeningId(null);
    }
  }

  return { open, openingId };
}
