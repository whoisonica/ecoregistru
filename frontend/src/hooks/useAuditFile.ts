import { api } from "@/lib/api";

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
  const url = URL.createObjectURL(res.data as Blob);
  try {
    const a = document.createElement("a");
    a.href = url;
    a.download =
      years === 1
        ? `dosar-control-${year}.zip`
        : `dosar-control-${year - years + 1}-${year}.zip`;
    document.body.appendChild(a);
    a.click();
    a.remove();
  } finally {
    URL.revokeObjectURL(url);
  }
}
