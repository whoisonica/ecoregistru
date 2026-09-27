import { strings } from "@web/strings";
import * as Sharing from "expo-sharing";

import { ApiError, downloadAuditFile, UnauthorizedError, type Auth } from "./api";

const m = strings.mobile;

/**
 * D5 — dosarul prin foaia de partajare a telefonului: zero backend nou, iar omul alege singur Mail,
 * WhatsApp sau Drive. Întoarce propoziția de arătat când n-a mers; `null` când a plecat.
 * `UnauthorizedError` urcă mai departe: ecranul îl scoate din cont.
 */
export async function shareDossier(auth: Auth, year: number, years: 1 | 3): Promise<string | null> {
  if (!(await Sharing.isAvailableAsync())) return m.shareUnavailable;
  try {
    const uri = await downloadAuditFile(auth, year, years);
    await Sharing.shareAsync(uri, { mimeType: "application/zip", UTI: "public.zip-archive", dialogTitle: m.sendDossier });
    return null;
  } catch (e) {
    if (e instanceof UnauthorizedError) throw e;
    return e instanceof ApiError ? (e.serverMessage ?? strings.auditFile.downloadError) : m.sendDossierOffline;
  }
}
