import { useState } from "react";
import { api, apiBlobErrorMessage } from "@/lib/api";
import { openPdfInTab } from "@/lib/openFileInTab";
import type { WasteMovement } from "@/lib/types";
import { strings } from "@/lib/strings";
import { useToast } from "@/components/ui/toast";

/**
 * Anexa 3 la HG 1061/2008 is printed from two screens — the movements list and the handover
 * register — so the rule for when it may be printed lives here, once.
 *
 * <p>The form covers a handover of NON-hazardous waste: its own title says "nepericuloase", and it
 * names an expeditor and a destinatar. The backend refuses the other cases with a message; the
 * button simply does not offer them.
 */
export function canPrintAnexa3(m: WasteMovement): boolean {
  return !m.hazardous && canPrintAviz(m);
}

/**
 * Avizul de însoțire (15.09.2026): orice predare către un partener, periculoasă sau nu — avizul
 * însoțește marfa, nu descrie deșeul.
 */
export function canPrintAviz(m: WasteMovement): boolean {
  return m.partnerId != null && (m.operation === "RECOVERED" || m.operation === "DISPOSED");
}

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
        `${document}-${m.wasteCode.replace(/\s/g, "")}-${m.date}.pdf`
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
