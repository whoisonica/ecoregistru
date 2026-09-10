import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { api, apiBlobErrorMessage } from "@/lib/api";
import { saveBlob } from "@/lib/download";
import type { Anexa2Threshold, WasteMovement } from "@/lib/types";
import { strings } from "@/lib/strings";
import { useToast } from "@/components/ui/toast";

/**
 * Anexa 2 la HG 1061/2008 — formularul de expediție/transport **deșeuri periculoase**.
 *
 * Perechea lui `useAnexa3`, și scris ca el fiindcă e aceeași întrebare cu răspuns opus: același
 * transport, celălalt fel de deșeu. Butonul apare exact unde nu apare cel de Anexa 3.
 *
 * ⚠️ **Deșeurile medicale nu trec pe aici.** Art. 24 dă formularul **transportatorului** — „chiar
 * dacă acesta este și destinatar" — pe cantitatea cumulată a unei rute, cu o anexă a expeditorilor.
 * Butonul nu se oferă pe capitolul 18, iar backendul refuză oricum: e alt document, nu o variantă.
 */
export function canPrintAnexa2(m: WasteMovement): boolean {
  return (
    m.hazardous &&
    !isMedicalWaste(m) &&
    m.partnerId != null &&
    (m.operation === "RECOVERED" || m.operation === "DISPOSED")
  );
}

/**
 * Capitolul 18 al nomenclatorului e „deșeuri rezultate din activități de îngrijire a sănătății
 * umane sau veterinare" — adică exact subiectul art. 24. Se citește capitolul, nu o listă de
 * coduri: o listă ar trebui ținută la zi față de nomenclator, iar un cod uitat ar tipări
 * documentul greșit pentru o clinică.
 */
function isMedicalWaste(m: WasteMovement): boolean {
  return m.hazardous && m.wasteCode.startsWith("18");
}

export function useAnexa2Download() {
  const [downloadingId, setDownloadingId] = useState<string | null>(null);
  const { notify } = useToast();

  async function download(m: WasteMovement) {
    setDownloadingId(m.id);
    try {
      const res = await api.get(`/api/v1/movements/${m.id}/anexa2`, { responseType: "blob" });
      saveBlob(res.data as Blob, `anexa2-${m.wasteCode.replace(/\s/g, "")}-${m.date}.pdf`);
    } catch (err) {
      notify(await apiBlobErrorMessage(err, strings.movements.anexa2Error), "error");
    } finally {
      setDownloadingId(null);
    }
  }

  return { download, downloadingId };
}

/**
 * Cifra din spatele bifei „< 1t/an", cerută doar când mișcarea e salvată și chiar poate tipări
 * formularul — pragul se citește din evidența anului, deci n-are ce răspunde pentru o mișcare care
 * încă nu există.
 */
export function useAnexa2Threshold(movementId: string | undefined, enabled: boolean) {
  return useQuery({
    queryKey: ["movements", movementId, "anexa2", "prag"] as const,
    enabled: Boolean(movementId) && enabled,
    queryFn: async () =>
      (await api.get<Anexa2Threshold>(`/api/v1/movements/${movementId}/anexa2/prag`)).data,
  });
}
