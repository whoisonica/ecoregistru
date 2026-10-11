import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { saveBlob } from "@/lib/download";
import { strings } from "@/lib/strings";
import type { EnvironmentalPermit, EnvironmentalReport } from "@/lib/types";

/**
 * Raportul anual de mediu (V86). Autorizația e a firmei, nu a anului: salvată, schimbă și termenele RAM și viză, deci
 * invalidează și `["deadlines"]`.
 */
const root = ["environmental-report"] as const;
const base = "/api/v1/environmental-report";

export function useEnvironmentalReport(year: number) {
  return useQuery({
    queryKey: [...root, year] as const,
    queryFn: async () => (await api.get<EnvironmentalReport>(base, { params: { year } })).data,
  });
}

/** Înlocuiește tot anul: răspunsurile, controalele și monitorizările. */
export type EnvironmentalReportInput = Omit<EnvironmentalReport, "permit">;

export function useSaveEnvironmentalReport() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: EnvironmentalReportInput) => (await api.put<EnvironmentalReport>(base, input)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: root }),
  });
}

export type EnvironmentalPermitInput = Omit<EnvironmentalPermit, "expiry" | "visaComputedMonth" | "visaComputedDay">;

export function useSaveEnvironmentalPermit() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ year, permit }: { year: number; permit: EnvironmentalPermitInput }) =>
      (await api.put<EnvironmentalReport>(`${base}/permit`, permit, { params: { year } })).data,
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: root });
      void qc.invalidateQueries({ queryKey: ["deadlines"] });
    },
  });
}

export async function downloadEnvironmentalReport(year: number): Promise<void> {
  const blob = (await api.get(`${base}/document`, { params: { year }, responseType: "blob" })).data as Blob;
  saveBlob(blob, strings.environmentalReport.fileName.replace("{year}", String(year)));
}
