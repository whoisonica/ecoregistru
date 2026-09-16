import { ExternalLink, Paperclip } from "lucide-react";
import type { WasteMovement } from "@/lib/types";
import { strings } from "@/lib/strings";
import { formatDate } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { useAttachmentOpen } from "@/hooks/useAttachment";

const t = strings.movements;

/**
 * Atașamentele unei mișcări, deschise din coloana „📎 N".
 *
 * <p>Până acum coloana arăta numărul și nimic mai mult: ca să vezi *ce* document e acolo trebuia
 * deschis formularul de editare, cu treizeci de rubrici — iar un VIEWER nu-l poate deschide deloc,
 * fiindcă butonul „Editează" stă sub `canWrite`. Deci pe rolul care există tocmai ca să citească,
 * avizul urcat lângă predare era o cifră.
 *
 * <p>Numai citire, dinadins: ștergerea rămâne în formular, lângă urcare, unde e și confirmarea și
 * regula de rol. Un coș de gunoi într-o vedere deschisă de oriunde ar fi cea mai ușoară apăsare
 * greșită din ecran.
 */
export function AttachmentsDialog({
  movement,
  onClose,
}: {
  movement: WasteMovement;
  onClose: () => void;
}) {
  const { open: openAttachment, openingId } = useAttachmentOpen();
  return (
    <Dialog
      open
      onClose={onClose}
      title={t.attachmentsDialogTitle}
      // Care mișcare — altfel dialogul deschis de pe al zecelea rând nu spune al cui e fișierul.
      description={`${movement.wasteCode} · ${formatDate(movement.date)} · ${movement.workPointName}`}
      size="md"
      footer={
        <Button variant="outline" onClick={onClose}>
          {strings.common.close}
        </Button>
      }
    >
      <ul className="space-y-1">
        {movement.attachments.map((a) => (
          <li key={a.id}>
            <button
              type="button"
              onClick={() => openAttachment(movement.id, a)}
              disabled={openingId === a.id}
              className="flex w-full items-center gap-2 rounded border border-line px-2 py-1.5 text-left text-sm text-brand hover:bg-surface-sunken hover:underline disabled:opacity-60"
            >
              <Paperclip className="h-3.5 w-3.5 shrink-0" />
              <span className="min-w-0 flex-1 truncate">{a.fileName}</span>
              {openingId === a.id ? (
                <span className="shrink-0 text-xs text-content-subtle">{t.attachmentOpening}</span>
              ) : (
                <ExternalLink className="h-3.5 w-3.5 shrink-0 text-content-subtle" />
              )}
            </button>
          </li>
        ))}
      </ul>
      <p className="mt-3 text-xs text-content-subtle">{t.attachmentsDialogHint}</p>
    </Dialog>
  );
}

