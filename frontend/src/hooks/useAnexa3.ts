import { useState } from "react";
import { api, apiErrorMessage } from "@/lib/api";
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
  return (
    !m.hazardous &&
    m.partnerId != null &&
    (m.operation === "RECOVERED" || m.operation === "DISPOSED")
  );
}

export function useAnexa3Download() {
  const [downloadingId, setDownloadingId] = useState<string | null>(null);
  const { notify } = useToast();

  async function download(m: WasteMovement) {
    setDownloadingId(m.id);
    try {
      const res = await api.get(`/api/v1/movements/${m.id}/anexa3`, { responseType: "blob" });
      const url = URL.createObjectURL(res.data as Blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `anexa3-${m.wasteCode.replace(/\s/g, "")}-${m.date}.pdf`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      notify(apiErrorMessage(err, strings.movements.anexa3Error), "error");
    } finally {
      setDownloadingId(null);
    }
  }

  return { download, downloadingId };
}
