import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { saveBlob } from "@/lib/download";
import { openPdfInTab } from "@/lib/openFileInTab";
import type { EvidenceFilters, EvidenceRegenerationResponse, MonthlyEvidence } from "@/lib/types";

/**
 * TanStack Query hooks for monthly evidence — same per-resource pattern as
 * useMovements. Evidence is a regenerable cache: the list reflects the last
 * regeneration, so useRegenerateEvidence invalidates the whole family on success.
 */
export const evidencesRoot = ["evidences"] as const;
export const evidencesKey = (filters: EvidenceFilters) => [...evidencesRoot, filters] as const;

/**
 * @param enabled off for a query that is only needed in some states — the dossier checks one year
 *                per option in its range and leaves the rest unfetched, rather than pulling five
 *                years of lines to warn about one.
 */
export function useEvidences(filters: EvidenceFilters, enabled = true) {
  return useQuery({
    enabled,
    queryKey: evidencesKey(filters),
    queryFn: async () => {
      const params: Record<string, string | number> = { year: filters.year };
      if (filters.month != null) params.month = filters.month;
      if (filters.workPointId) params.workPointId = filters.workPointId;
      return (await api.get<MonthlyEvidence[]>("/api/v1/evidences", { params })).data;
    },
  });
}

export function useRegenerateEvidence() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (year: number) =>
      (
        await api.post<EvidenceRegenerationResponse>("/api/v1/evidences/regenerate", null, {
          params: { year },
        })
      ).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: evidencesRoot }),
  });
}

/**
 * Downloads the evidence export (generic, unofficial table) as an xlsx/pdf file. Not a query —
 * it streams a blob and triggers a browser download, so it lives outside TanStack's cache.
 * Uses the same filters as the list so the file matches what's on screen.
 */
/**
 * The waste-management record itself (HG 856/2002, anexa 1) — the document the client files,
 * one page per waste code per work point. Named after the document, not after the annex: since
 * 24.08.2026 "Anexa 1" means the packaging declaration in this application.
 * Separate from the "export" below, which is an unofficial working summary and says so on its own
 * header; mixing the two into one button is how somebody ends up filing the wrong paper.
 */
export async function downloadAnexa1Form(filters: EvidenceFilters): Promise<void> {
  const params: Record<string, string | number> = { year: filters.year };
  if (filters.workPointId) params.workPointId = filters.workPointId;

  await openPdfInTab(
    async () =>
      (await api.get("/api/v1/evidences/anexa1", { params, responseType: "blob" })).data as Blob,
    `evidenta-gestiunii-deseurilor-${filters.year}.pdf`
  );
}

/**
 * „Evidența gestiunii deșeurilor centralizată" (fosta declarație anuală, redenumită pe 15.09.2026)
 * — the summary page in front of the record sheets: one line per waste code, one page per work
 * point. Same figures as the fişa, folded to the year.
 */
export async function downloadAnnualDeclaration(filters: EvidenceFilters): Promise<void> {
  const params: Record<string, string | number> = { year: filters.year };
  if (filters.workPointId) params.workPointId = filters.workPointId;

  await openPdfInTab(
    async () =>
      (await api.get("/api/v1/evidences/declaratie-anuala", { params, responseType: "blob" }))
        .data as Blob,
    `evidenta-centralizata-${filters.year}.pdf`
  );
}

/**
 * Evidența cronologică lunară a deșeurilor preluate de la terți (OUG 92/2021 art. 48 alin. (1)) —
 * tabelul cronologic plus totalurile anului în forma chestionarului SIM „Colectare/Tratare”.
 * `.xlsx` ca să copiezi în portal, PDF pentru control. Pe anul întreg: evidența e anuală.
 */
export async function downloadArt48Register(
  year: number,
  workPointId: string | undefined,
  format: "xlsx" | "pdf"
): Promise<void> {
  const params: Record<string, string | number> = { year, format };
  if (workPointId) params.workPointId = workPointId;
  const fetchFile = async () =>
    (await api.get("/api/v1/evidences/registru-cronologic", { params, responseType: "blob" }))
      .data as Blob;
  if (format === "pdf") {
    await openPdfInTab(fetchFile, `evidenta-cronologica-${year}.pdf`);
  } else {
    saveBlob(await fetchFile(), `evidenta-cronologica-${year}.xlsx`);
  }
}

export async function downloadEvidenceExport(
  filters: EvidenceFilters,
  format: "xlsx" | "pdf"
): Promise<void> {
  const params: Record<string, string | number> = { year: filters.year, format };
  if (filters.month != null) params.month = filters.month;
  if (filters.workPointId) params.workPointId = filters.workPointId;

  const fetchFile = async () =>
    (await api.get("/api/v1/evidences/export", { params, responseType: "blob" })).data as Blob;
  if (format === "pdf") {
    await openPdfInTab(fetchFile, `evidenta-${filters.year}.pdf`);
  } else {
    saveBlob(await fetchFile(), `evidenta-${filters.year}.${format}`);
  }
}
