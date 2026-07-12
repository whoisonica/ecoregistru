import { api } from "@/lib/api";

/**
 * Downloads the control dossier (dosar de control) for a year as a ZIP and triggers a browser
 * download. Not a query — it streams a blob, so it lives outside TanStack's cache (same pattern
 * as downloadEvidenceExport).
 */
export async function downloadAuditFile(year: number): Promise<void> {
  const res = await api.get("/api/v1/audit-file", {
    params: { year },
    responseType: "blob",
  });
  const url = URL.createObjectURL(res.data as Blob);
  try {
    const a = document.createElement("a");
    a.href = url;
    a.download = `dosar-control-${year}.zip`;
    document.body.appendChild(a);
    a.click();
    a.remove();
  } finally {
    URL.revokeObjectURL(url);
  }
}
