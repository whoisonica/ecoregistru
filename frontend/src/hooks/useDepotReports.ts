import { api } from "@/lib/api";
import { saveBlob } from "@/lib/download";

/** D4.7 — rapoartele fixe ale depozitului; `slug` e același ca pe server (`DepotReportKind`). */
export type DepotReportSlug =
  | "registru"
  | "jurnal-cantar"
  | "documente"
  | "anulate"
  | "fisa-stoc"
  | "transferuri"
  | "afm"
  | "impozit"
  | "numerar"
  | "persoane-fizice"
  | "partener";

export interface DepotReportQuery {
  from: string;
  to: string;
  workPointId?: string;
  articleId?: string;
  partnerId?: string;
}

/** Descarcă un raport ca `.xlsx` sau `.pdf`; numele fișierului îl dă serverul (perioada și depozitul). */
export async function downloadDepotReport(
  slug: DepotReportSlug,
  query: DepotReportQuery,
  format: "xlsx" | "pdf"
): Promise<void> {
  const params = Object.fromEntries(Object.entries({ ...query, format }).filter(([, v]) => v));
  const response = await api.get(`/api/v1/depot-reports/${slug}`, { params, responseType: "blob" });
  const disposition = String(response.headers["content-disposition"] ?? "");
  const named = /filename="?([^";]+)"?/.exec(disposition)?.[1];
  saveBlob(response.data as Blob, named ?? `${slug}-${query.from}_${query.to}.${format}`);
}
