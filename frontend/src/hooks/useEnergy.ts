import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { saveBlob } from "@/lib/download";
import { openPdfInTab } from "@/lib/openFileInTab";
import type {
  EnergyCarrier,
  EnergyContact,
  EnergyDeclaration,
  EnergySheet,
  EnergyYearSummary,
} from "@/lib/types";

/**
 * Fișa de energie (Legea 121/2014). Toate scrierile întorc fișa anului, dar totalurile, „N din 12”
 * și pragul trăiesc pe server, deci orice salvare invalidează toată familia `["energy"]`.
 */
const energyRoot = ["energy"] as const;
const base = "/api/v1/energy";

export function useEnergySheet(year: number, enabled = true) {
  return useQuery({
    enabled,
    queryKey: [...energyRoot, "sheet", year] as const,
    queryFn: async () => (await api.get<EnergySheet>(base, { params: { year } })).data,
  });
}

/** Anii cu date, cei mai noi întâi, pentru tabul „Energie” al dosarului. */
export function useEnergyYears(enabled = true) {
  return useQuery({
    enabled,
    queryKey: [...energyRoot, "years"] as const,
    queryFn: async () => (await api.get<EnergyYearSummary[]>(`${base}/years`)).data,
  });
}

export function useSaveEnergyCarriers() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (carriers: EnergyCarrier[]) =>
      (await api.put<EnergySheet>(`${base}/carriers`, { carriers })).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: energyRoot }),
  });
}

/**
 * O celulă (rubrică × lună). `quantity: null` șterge celula. `tep` se trimite numai la COAL și
 * OTHER_FUEL: la celelalte îl calculează serverul și un `tep` trimis dă 400.
 */
export interface EnergyCellInput {
  year: number;
  carrier: EnergyCarrier;
  month: number;
  quantity: number | null;
  tep?: number | null;
}

export function useSaveEnergyCell() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: EnergyCellInput) =>
      (await api.put<EnergySheet>(`${base}/consumption`, input)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: energyRoot }),
  });
}

/** Înlocuiește tot: răspunsurile și măsurile anului. Se trimite obiectul întreg. */
export type EnergyDeclarationInput = EnergyDeclaration & { year: number };

export function useSaveEnergyDeclaration() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: EnergyDeclarationInput) =>
      (await api.put<EnergySheet>(`${base}/declaration`, input)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: energyRoot }),
  });
}

/** Înlocuiește tot: datele de contact ale firmei, cele din Anexa 1. */
export function useSaveEnergyContact() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: EnergyContact) =>
      (await api.put<EnergySheet>(`${base}/contact`, input)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: energyRoot }),
  });
}

export function useUploadEnergyReceipt() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ year, file }: { year: number; file: File }) => {
      const form = new FormData();
      form.append("file", file);
      return (await api.post<EnergySheet>(`${base}/recipisa`, form, { params: { year } })).data;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: energyRoot }),
  });
}

async function fetchBlob(path: string, year: number): Promise<Blob> {
  return (await api.get(`${base}/${path}`, { params: { year }, responseType: "blob" })).data as Blob;
}

export async function downloadEnergyAnnex1(year: number): Promise<void> {
  saveBlob(await fetchBlob("anexa1", year), `Anexa 1 energie ${year}.xlsx`);
}

export async function downloadEnergyDeclaration(year: number): Promise<void> {
  saveBlob(await fetchBlob("declaratie", year), `Declaratie energie ${year}.docx`);
}

export async function downloadEnergyDossier(year: number): Promise<void> {
  saveBlob(await fetchBlob("dosar", year), `Dosar energie ${year}.zip`);
}

export async function openEnergyReceipt(year: number): Promise<void> {
  await openPdfInTab(() => fetchBlob("recipisa/continut", year), `recipisa-energie-${year}.pdf`);
}
