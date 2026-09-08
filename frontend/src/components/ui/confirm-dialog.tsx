import { useCallback, useState, type ReactNode } from "react";
import { AlertTriangle } from "lucide-react";
import { Dialog } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { strings } from "@/lib/strings";

interface ConfirmRequest {
  title: string;
  /** Ce se întâmplă, spus cu identitatea lucrului atins — nu „ești sigur?". */
  message: ReactNode;
  /** Eticheta butonului care duce fapta la capăt. Implicit „Șterge". */
  confirmLabel?: string;
  /** `danger` colorează butonul roșu. Pentru dezactivări, care se pot desface, lasă implicitul. */
  tone?: "danger" | "default";
  onConfirm: () => void;
}

/**
 * Confirmarea unei acțiuni care nu se ia ușor înapoi.
 *
 * <p>Cele cinci locuri care întrebau înainte de o ștergere foloseau `window.confirm`: o casetă a
 * browserului, cu butoane în limba sistemului de operare, care nu putea spune decât un șir de
 * text — deci nu spunea niciodată *ce* rând se șterge. Aici mesajul poartă identitatea lucrului
 * („Mișcarea 15 01 01 din 12.08.2026, 340 kg"), fiindcă asta e informația care oprește greșeala.
 *
 * <p>Se folosește ca o pereche, ca apelul de la locul faptei să rămână la fel de scurt ca înainte:
 *
 * <pre>
 *   const [confirm, confirmDialog] = useConfirm();
 *   confirm({ title: …, message: …, tone: "danger", onConfirm: () => mut.mutate(id) });
 *   // …iar `{confirmDialog}` se randează undeva în pagină.
 * </pre>
 */
export function useConfirm(): [(request: ConfirmRequest) => void, ReactNode] {
  const [request, setRequest] = useState<ConfirmRequest | null>(null);
  const close = useCallback(() => setRequest(null), []);
  // Stabil între randări, ca să poată sta liniștit în lista de dependențe a unui `useCallback`.
  const ask = useCallback((next: ConfirmRequest) => setRequest(next), []);

  const element = request ? (
    <Dialog
      open
      size="sm"
      onClose={close}
      title={request.title}
      footer={
        <>
          <Button variant="outline" onClick={close}>
            {strings.common.cancel}
          </Button>
          <Button
            variant={request.tone === "danger" ? "danger" : "default"}
            onClick={() => {
              // Închidem întâi: acțiunea poate deschide la rândul ei un dialog, iar două
              // suprapuse ar lăsa capcana de focus a celui de dedesubt să tragă înapoi.
              close();
              request.onConfirm();
            }}
          >
            {request.confirmLabel ?? strings.common.delete}
          </Button>
        </>
      }
    >
      <div className="flex gap-3">
        {request.tone === "danger" && (
          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-red-100">
            <AlertTriangle className="h-4 w-4 text-red-600" aria-hidden />
          </div>
        )}
        <div className="text-sm text-content-muted">{request.message}</div>
      </div>
    </Dialog>
  ) : null;

  return [ask, element];
}
