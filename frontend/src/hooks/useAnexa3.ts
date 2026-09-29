import { useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { api, apiBlobErrorMessage } from "@/lib/api";
import { openPdfInTab } from "@/lib/openFileInTab";
import { movementPdfName } from "@/lib/movementPrint";
import { flightGuard } from "@/lib/flightGuard";
import type { WasteMovement } from "@/lib/types";
import { strings } from "@/lib/strings";
import { useToast } from "@/components/ui/toast";

// Regula și numele fișierului stau în `lib/` (M1e): le citește și telefonul, care nu vede `hooks/`.
export { canPrintAnexa3, canPrintAviz } from "@/lib/movementPrint";

/** Deschide PDF-ul unei mișcări într-un tab — Anexa 3 sau avizul, după `document`. */
function useMovementPdf(document: "anexa3" | "aviz", errorMessage: string) {
  const [downloadingId, setDownloadingId] = useState<string | null>(null);
  const { notify } = useToast();
  const queryClient = useQueryClient();
  /**
   * Un clic pe mișcare, cât PDF-ul e în drum (29.09.2026). `disabled={downloadingId === m.id}` venea
   * abia după randare: la dublu-clic pe **prima** Anexa 3, a doua cerere aloca numărul pe o versiune
   * veche a mișcării și serverul răspundea 409. Vezi `flightGuard`.
   */
  const inFlight = useRef(flightGuard());

  async function download(m: WasteMovement) {
    await inFlight.current.run(m.id, async () => {
      setDownloadingId(m.id);
      try {
        await openPdfInTab(
          async () =>
            (await api.get(`/api/v1/movements/${m.id}/${document}`, { responseType: "blob" }))
              .data as Blob,
          movementPdfName(document, m)
        );
        // Prima tipărire a Anexei 3 alocă numărul: lista îl recitește, altfel rândul l-ar arăta încă
        // fără număr (și ștergerea n-ar ști să avertizeze de golul din registru).
        if (document === "anexa3" && m.anexa3Number == null) {
          void queryClient.invalidateQueries({ queryKey: ["movements"] });
        }
      } catch (err) {
        notify(await apiBlobErrorMessage(err, errorMessage), "error");
      } finally {
        setDownloadingId(null);
      }
    });
  }

  return { download, downloadingId };
}

export function useAnexa3Download() {
  return useMovementPdf("anexa3", strings.movements.anexa3Error);
}

export function useAvizDownload() {
  return useMovementPdf("aviz", strings.movements.avizError);
}
