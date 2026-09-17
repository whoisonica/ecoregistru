import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { saveBlob } from "@/lib/download";

/**
 * Downloads the control dossier (dosar de control) as a ZIP and triggers a browser download.
 * Not a query — it streams a blob, so it lives outside TanStack's cache (same pattern as
 * downloadEvidenceExport).
 *
 * `years` is how many consecutive years back to include, ending in `year`: 3 covers the whole
 * retention period an inspection may ask for (OUG 92/2021 art. 48 alin. (5)). The backend caps
 * it at 3 and folders each year separately.
 */
export async function downloadAuditFile(year: number, years = 1): Promise<void> {
  const res = await api.get("/api/v1/audit-file", {
    params: { year, years },
    responseType: "blob",
  });
  saveBlob(
    res.data as Blob,
    years === 1
      ? `dosar-control-${year}.zip`
      : `dosar-control-${year - years + 1}-${year}.zip`
  );
}

/** Mirrors backend `AuditFileService.AuditFileSize`. `unknownSize`: atașamente de dinainte să se țină mărimea. */
export interface AuditFileSize {
  attachments: number;
  attachmentBytes: number;
  unknownSize: number;
}

/** Cât cântărește dosarul, înainte de descărcare: numai atașamentele, restul sunt câteva sute de KB. */
export function useAuditFileSize(year: number, years: number) {
  return useQuery({
    queryKey: ["audit-file-size", year, years] as const,
    queryFn: async () =>
      (await api.get<AuditFileSize>("/api/v1/audit-file/size", { params: { year, years } })).data,
  });
}

/** Mirrors backend `AuditFileService.YearContents`. */
export interface AuditFileYearContents {
  year: number;
  movements: number;
  anexa3WorkPoints: string[];
  anexa3RoleMissing: boolean;
}

/** Mirrors backend `AuditFileService.AuditFileContents`: ce intră în dosar, citit după regulile arhivei. */
export interface AuditFileContents {
  years: AuditFileYearContents[];
  packagingDeclaration: "INCLUDED" | "TRADER_ONLY" | "NOT_ANSWERED";
  anexa3ExitsOnly: boolean;
  partners: number;
  partnersExpired: number;
  partnersExpiringSoon: number;
}

/** Ce documente intră în dosar pentru perioada aleasă și de ce — nu o listă fixă. */
export function useAuditFileContents(year: number, years: number) {
  return useQuery({
    queryKey: ["audit-file-contents", year, years] as const,
    queryFn: async () =>
      (await api.get<AuditFileContents>("/api/v1/audit-file/contents", { params: { year, years } })).data,
  });
}
