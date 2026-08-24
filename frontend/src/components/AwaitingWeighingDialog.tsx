import { Dialog } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { strings } from "@/lib/strings";
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
  lines,
  onConfirm,
  onCancel,
}: {
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
      title={t.title}
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
        <p className="text-sm text-gray-700">
          {t.body.replace("{count}", String(lines.length))}
        </p>
        <ul className="space-y-1 rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900">
          {shown.map((l) => (
            <li key={l.id}>
              {l.wasteCode} — {strings.months[l.month - 1]} {l.year}, {l.workPointName}
            </li>
          ))}
          {rest > 0 && (
            <li className="text-amber-700">{t.andMore.replace("{count}", String(rest))}</li>
          )}
        </ul>
        <p className="text-xs text-gray-500">{t.hint}</p>
      </div>
    </Dialog>
  );
}
