import { useState } from "react";
import { api, apiBlobErrorMessage } from "@/lib/api";
import { openPdfInTab } from "@/lib/openFileInTab";
import { movementPdfName } from "@/lib/movementPrint";
import type { WasteMovement } from "@/lib/types";
import { strings } from "@/lib/strings";
import { useToast } from "@/components/ui/toast";

// Regula și numele fișierului stau în `lib/` (M1e): le citește și telefonul, care nu vede `hooks/`.
export { canPrintAnexa3, canPrintAviz } from "@/lib/movementPrint";

/** Deschide PDF-ul unei mișcări într-un tab — Anexa 3 sau avizul, după `document`. */
function useMovementPdf(document: "anexa3" | "aviz", errorMessage: string) {
  const [downloadingId, setDownloadingId] = useState<string | null>(null);
  const { notify } = useToast();

  async function download(m: WasteMovement) {
    setDownloadingId(m.id);
    try {
      await openPdfInTab(
        async () =>
          (await api.get(`/api/v1/movements/${m.id}/${document}`, { responseType: "blob" }))
            .data as Blob,
        movementPdfName(document, m)
      );
    } catch (err) {
      notify(await apiBlobErrorMessage(err, errorMessage), "error");
    } finally {
      setDownloadingId(null);
    }
  }

  return { download, downloadingId };
}

export function useAnexa3Download() {
  return useMovementPdf("anexa3", strings.movements.anexa3Error);
}

export function useAvizDownload() {
  return useMovementPdf("aviz", strings.movements.avizError);
}
