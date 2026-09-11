import { useState } from "react";
import { api, apiBlobErrorMessage } from "@/lib/api";
import { openBlankTab, openBlobInTab } from "@/lib/openFileInTab";
import { strings } from "@/lib/strings";
import { useToast } from "@/components/ui/toast";
import type { Attachment } from "@/lib/types";

/**
 * Deschide un atașament — cu sesiunea omului, nu de la un URL public.
 *
 * <p>Până la 11-bis fișierele stăteau la `secure_url`-ul de la Cloudinary, iar ecranul le punea
 * într-un `<a href>`: cine avea adresa deschidea documentul, fără cont, fără firmă, oricând. Acum
 * conținutul vine printr-un endpoint al nostru, care cere sesiune și verifică tenantul — și de
 * aceea nu mai poate fi un `href` simplu: un `<a>` nu duce cu el antetul `Authorization`.
 *
 * <p>Mecanica deschiderii — tabul cerut **înainte** de `await`, și de ce **fără `noopener`** — stă
 * în `lib/openFileInTab`, într-un singur loc, fiindcă o folosesc și buletinele de analiză (G-7).
 * Amândouă capcanele de acolo au fost plătite o dată, pe ecran; două copii ar fi fost două ocazii
 * ca numai una să fie reparată.
 */
export function useAttachmentOpen() {
  const [openingId, setOpeningId] = useState<string | null>(null);
  const { notify } = useToast();

  async function open(movementId: string, a: Attachment) {
    setOpeningId(a.id);
    const tab = openBlankTab();
    try {
      const res = await api.get(
        `/api/v1/movements/${movementId}/attachments/${a.id}/continut`,
        { responseType: "blob" },
      );
      openBlobInTab(tab, res.data as Blob, a.fileName || "atasament");
    } catch (err) {
      tab?.close();
      notify(
        await apiBlobErrorMessage(err, strings.movements.attachmentOpenError),
        "error",
      );
    } finally {
      setOpeningId(null);
    }
  }

  return { open, openingId };
}
