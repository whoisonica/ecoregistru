import { useState } from "react";
import { api, apiBlobErrorMessage } from "@/lib/api";
import { saveBlob } from "@/lib/download";
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
 * <p>Tabul se deschide **înainte** de `await`, nu după. Browserele leagă permisiunea de a deschide
 * o fereastră de gestul care a produs-o, iar un `window.open` de după o cerere de rețea nu mai e
 * al clicului — e blocat ca reclamă. Deschidem un tab gol pe loc, îi punem adresa când sosesc
 * octeții, și dacă tot a fost blocat (`null`) salvăm fișierul, ca omul să nu rămână cu nimic.
 *
 * <p>⚠️ **Fără `noopener`.** Prima versiune cerea `"noopener,noreferrer"`, iar specificația HTML
 * spune că atunci `window.open` întoarce **`null`** — n-ai cum să primești un mâner către o
 * fereastră de care te-ai lepădat. Deci `tab` era null de fiecare dată, ramura de deasupra era cod
 * mort, și fiecare atașament ajungea pe calea de rezervă: se **descărca**, în loc să se deschidă.
 * Se vedea doar cu ochiul, pe aplicația pornită; `tsc` nu are ce să obiecteze, fiindcă tipul lui
 * `window.open` chiar include `null`. Ce voia `noopener` să apere — o pagină străină care citește
 * `window.opener` — nu se aplică aici: destinația e un `blob:` din propria origine. Îl tăiem
 * oricum, cu `tab.opener = null`, ca intenția să rămână scrisă.
 */
export function useAttachmentOpen() {
  const [openingId, setOpeningId] = useState<string | null>(null);
  const { notify } = useToast();

  async function open(movementId: string, a: Attachment) {
    setOpeningId(a.id);
    const tab = window.open("", "_blank");
    if (tab) tab.opener = null;
    try {
      const res = await api.get(
        `/api/v1/movements/${movementId}/attachments/${a.id}/continut`,
        { responseType: "blob" },
      );
      const blob = res.data as Blob;
      const url = URL.createObjectURL(blob);
      if (tab) {
        tab.location.href = url;
      } else {
        saveBlob(blob, a.fileName || "atasament");
      }
      // Adresa `blob:` trăiește cât tabul care o citește; o eliberăm târziu, nu pe tickul următor
      // ca la descărcări, fiindcă acolo browserul termină de citit în aceeași secundă.
      setTimeout(() => URL.revokeObjectURL(url), 60_000);
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
