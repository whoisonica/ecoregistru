import { Dialog } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { strings } from "@/lib/strings";
import { withCount } from "@/lib/utils";
import type { MonthlyEvidence } from "@/lib/types";

const t = strings.awaitingWeighing;

/**
 * The warning before a document is generated with quantities still missing.
 *
 * <p>A movement ticked "se cântărește la descărcare" leaves the site without a weight: legitimate
 * while the load is on the road, wrong once the document is filed. The client asked on 24.08.2026
 * to be warned at the moment of generating — and only for the lines the document actually
 * contains, which is why the caller passes the lines it is about to print rather than everything
 * the tenant has.
 *
 * <p>It warns; it does not block. The figure may genuinely not exist yet, and a dossier prepared
 * in advance is still worth having — so the choice stays with the person who knows.
 */
export function AwaitingWeighingDialog({
  documentName,
  lines,
  onConfirm,
  onCancel,
}: {
  /** Numele documentului care se descarcă, ca omul să știe de ce a apărut dialogul în locul fișierului. */
  documentName: string;
  lines: MonthlyEvidence[];
  onConfirm: () => void;
  onCancel: () => void;
}) {
  const shown = lines.slice(0, 6);
  const rest = lines.length - shown.length;

  return (
    <Dialog
      open
      onClose={onCancel}
      title={t.title.replace("{document}", documentName)}
      footer={
        <>
          <Button variant="ghost" onClick={onCancel}>
            {t.cancel}
          </Button>
          <Button onClick={onConfirm}>{t.generateAnyway}</Button>
        </>
      }
    >
      <div className="space-y-3">
        <p className="text-sm text-content-strong">
          {withCount(t.body, lines.length, "linie", "linii")}
        </p>
        <ul className="space-y-1 rounded-md border border-line bg-surface-muted px-3 py-2 text-sm text-content">
          {shown.map((l) => (
            <li key={l.id} className="flex items-start gap-2">
              <span aria-hidden className="mt-1.5 h-2 w-2 shrink-0 rounded-[1px] bg-state-warn" />
              <span className="whitespace-nowrap font-mono">{l.wasteCode}</span> — {strings.months[l.month - 1]} {l.year}, {l.workPointName}
            </li>
          ))}
          {rest > 0 && (
            <li className="text-content-muted">{withCount(t.andMore, rest, "linie", "linii")}</li>
          )}
        </ul>
        <p className="text-xs text-content-muted">{t.hint}</p>
      </div>
    </Dialog>
  );
}
