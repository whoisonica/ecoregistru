import { Copy, FileText, Scale } from "lucide-react";
import type { MovementDirection, WasteMovement, WasteRegister } from "@/lib/types";
import { strings } from "@/lib/strings";
import { formatDate } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { BinSwatch } from "@/components/ui/bin-swatch";
import { useCanWrite } from "@/hooks/useBillingAccess";
import { useCurrentCompany } from "@/hooks/useCompanies";
import { canPrintAnexa3, canPrintAviz, useAnexa3Download, useAvizDownload } from "@/hooks/useAnexa3";
import { canPrintAnexa2, useAnexa2Download } from "@/hooks/useAnexa2";

const t = strings.movements;
const e = strings.enums;

/**
 * Ce urmează după o mișcare nouă (regula 12 a formularelor: „salvarea spune pasul următor”).
 *
 * <p>Până acum salvarea închidea dialogul cu „Mișcare adăugată.” pe patru secunde, iar Anexa 3 — de
 * obicei chiar motivul pentru care omul a scris predarea — stătea în meniul „⋯” al rândului. Aici se
 * oferă exact documentele pe care rândul le poate tipări (aceleași reguli ca meniul), plus „Încă una
 * la fel”, care pornește formularul cu alegerile ei, fără cantitate, dată, document sau notițe.
 * Nu se schimbă nimic din ce s-a salvat.
 */
export function MovementSavedDialog({
  movement,
  screen,
  direction,
  onAnother,
  onClose,
}: {
  movement: WasteMovement;
  screen: WasteRegister;
  direction?: MovementDirection;
  onAnother: () => void;
  onClose: () => void;
}) {
  const { data: company } = useCurrentCompany();
  const canWrite = useCanWrite();
  const { download: downloadAnexa3, downloadingId: anexa3Busy } = useAnexa3Download();
  const { download: downloadAviz, downloadingId: avizBusy } = useAvizDownload();
  const { download: downloadAnexa2, downloadingId: anexa2Busy } = useAnexa2Download();

  const title =
    screen === "ANEXA_1" ? t.savedTitleGenerated : direction === "IN" ? t.savedTitleIn : t.savedTitleOut;
  const documents = [
    canPrintAnexa3(movement, canWrite) && {
      key: "anexa3",
      label: t.savedAnexa3,
      hint: t.anexa3Copies,
      busy: anexa3Busy === movement.id,
      run: () => downloadAnexa3(movement),
    },
    canPrintAviz(movement, canWrite) && {
      key: "aviz",
      label: t.savedAviz,
      hint: t.savedAvizHint,
      busy: avizBusy === movement.id,
      run: () => downloadAviz(movement),
    },
    canPrintAnexa2(movement, company?.type) && {
      key: "anexa2",
      label: t.savedAnexa2,
      hint: t.savedAnexa2Hint,
      busy: anexa2Busy === movement.id,
      run: () => downloadAnexa2(movement),
    },
  ].filter(Boolean) as { key: string; label: string; hint: string; busy: boolean; run: () => void }[];

  return (
    <Dialog
      open
      size="lg"
      onClose={onClose}
      title={title}
      footer={
        <>
          <Button variant="outline" className="sm:mr-auto" onClick={onAnother}>
            <Copy className="mr-2 h-4 w-4" aria-hidden />
            {t.savedAnother}
          </Button>
          <Button onClick={onClose} data-testid="movement-saved-done">
            {t.savedDone}
          </Button>
        </>
      }
    >
      <div className="space-y-5" data-testid="movement-saved">
        <div className="flex items-start gap-2.5">
          <span aria-hidden className="mt-1.5 inline-block h-2.5 w-2.5 shrink-0 rounded-sm bg-state-ok" />
          <p className="text-sm text-content-strong">{t.savedLead}</p>
        </div>

        {/* Bonul: ce s-a salvat, citit din răspunsul serverului, nu din formular. */}
        <dl className="grid grid-cols-[auto_minmax(0,1fr)] gap-x-4 gap-y-1.5 rounded-lg border border-line bg-surface-muted px-3.5 py-3 text-sm">
          <dt className="text-content-muted">{t.date}</dt>
          <dd className="font-mono text-content">{formatDate(movement.date)}</dd>
          <dt className="text-content-muted">{t.receiptCode}</dt>
          <dd className="min-w-0 text-content">
            <BinSwatch code={movement.wasteCode} hazardous={movement.hazardous} />
            <span className="font-mono">{movement.wasteCode}</span>
            <span className="block truncate text-xs text-content-muted">{movement.wasteCodeName}</span>
          </dd>
          <dt className="text-content-muted">{t.receiptQuantity}</dt>
          <dd className="font-mono text-content">
            {movement.quantity == null ? t.receiptAwaiting : `${movement.quantity} ${e.unit[movement.unit]}`}
          </dd>
          {movement.partnerName && (
            <>
              <dt className="text-content-muted">{t.receiptPartner}</dt>
              <dd className="truncate text-content">{movement.partnerName}</dd>
            </>
          )}
          {movement.operationCode && (
            <>
              <dt className="text-content-muted">{t.receiptFate}</dt>
              <dd className="text-content">
                {e.wasteOperation[movement.operation]}
                <span className="font-mono"> · {movement.operationCode}</span>
              </dd>
            </>
          )}
        </dl>

        {(documents.length > 0 || movement.quantity == null) && (
          <div>
            <div className="eyebrow text-content-muted">{t.savedNext}</div>
            <ul className="mt-2 space-y-2">
              {documents.map((doc) => (
                <li key={doc.key}>
                  <button
                    type="button"
                    onClick={doc.run}
                    disabled={doc.busy}
                    className="flex w-full items-start gap-3 rounded-lg border border-line-strong bg-surface p-3 text-left transition-colors hover:border-content-subtle disabled:opacity-60"
                  >
                    <span
                      aria-hidden
                      className="flex h-9 w-9 shrink-0 items-center justify-center rounded-md bg-surface-sunken text-content-strong"
                    >
                      <FileText className="h-4 w-4" />
                    </span>
                    <span className="min-w-0">
                      <span className="block text-sm font-semibold text-content">
                        {doc.busy ? t.anexa3Downloading : doc.label}
                      </span>
                      <span className="mt-0.5 block text-xs text-content-muted">{doc.hint}</span>
                    </span>
                  </button>
                </li>
              ))}
              {movement.quantity == null && (
                <li className="flex items-start gap-3 rounded-lg border border-dashed border-line-strong p-3">
                  <span
                    aria-hidden
                    className="flex h-9 w-9 shrink-0 items-center justify-center rounded-md bg-surface-sunken text-content-strong"
                  >
                    <Scale className="h-4 w-4" />
                  </span>
                  <span className="text-xs text-content-muted">
                    <span className="block text-sm font-semibold text-content">{t.savedWeighTitle}</span>
                    {t.savedWeighHint}
                  </span>
                </li>
              )}
            </ul>
          </div>
        )}
      </div>
    </Dialog>
  );
}
