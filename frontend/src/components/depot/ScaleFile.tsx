import { useRef, useState } from "react";
import { FileText, Paperclip, X } from "lucide-react";
import { api, apiBlobErrorMessage, apiErrorMessage } from "@/lib/api";
import { openBlankTab, openBlobInTab } from "@/lib/openFileInTab";
import { strings } from "@/lib/strings";
import type { ScaleDocument } from "@/lib/types";
import { useAttachScaleDocument, useDetachScaleDocument } from "@/hooks/useScales";
import { Button } from "@/components/ui/button";
import { useToast } from "@/components/ui/toast";

const t = strings.settings.scales;

/**
 * V70 — un fișier al cântarului: dovada BRML (fără `eventId`) sau buletinul unei verificări. Numele deschide
 * fișierul prin sesiune (ca atașamentele, `openFileInTab`); fără drept de scriere rămâne doar numele.
 */
export function ScaleFile({
  scaleId,
  eventId,
  document,
  canManage,
  label,
}: {
  scaleId: string;
  eventId?: string;
  document: ScaleDocument | null;
  canManage: boolean;
  label: string;
}) {
  const input = useRef<HTMLInputElement>(null);
  const attach = useAttachScaleDocument();
  const detach = useDetachScaleDocument();
  const { notify } = useToast();
  const [opening, setOpening] = useState(false);

  async function open() {
    if (!document) return;
    setOpening(true);
    const tab = openBlankTab();
    try {
      const res = await api.get(`/api/v1/scales/${scaleId}/documents/${document.id}`, { responseType: "blob" });
      openBlobInTab(tab, res.data as Blob, document.fileName || "document");
    } catch (err) {
      tab?.close();
      notify(await apiBlobErrorMessage(err, t.fileOpenError), "error");
    } finally {
      setOpening(false);
    }
  }

  async function upload(file: File | undefined) {
    if (!file) return;
    try {
      await attach.mutateAsync({ scaleId, eventId, file });
      notify(t.fileSaved, "success");
    } catch (err) {
      notify(apiErrorMessage(err, t.fileError), "error");
    } finally {
      if (input.current) input.current.value = "";
    }
  }

  async function remove() {
    if (!document) return;
    try {
      await detach.mutateAsync({ scaleId, documentId: document.id });
      notify(t.fileRemoved, "success");
    } catch (err) {
      notify(apiErrorMessage(err, t.fileError), "error");
    }
  }

  const busy = attach.isPending || detach.isPending;
  return (
    <span className="relative inline-flex flex-wrap items-center gap-1">
      {document ? (
        <Button variant="ghost" size="sm" onClick={open} disabled={opening} aria-label={`${label}: ${document.fileName ?? ""}`}>
          <FileText className="mr-1 h-3.5 w-3.5" />
          <span className="max-w-[12rem] truncate">{document.fileName || label}</span>
        </Button>
      ) : (
        !canManage && <span className="text-xs text-content-subtle">{t.noFile}</span>
      )}
      {canManage && (
        <>
          <input
            ref={input}
            type="file"
            accept="application/pdf,image/*"
            className="sr-only"
            aria-label={label}
            onChange={(e) => upload(e.target.files?.[0])}
          />
          <Button variant="ghost" size="sm" disabled={busy} onClick={() => input.current?.click()}>
            <Paperclip className="mr-1 h-3.5 w-3.5" />
            {document ? t.replaceFile : t.attachFile}
          </Button>
          {document && (
            <Button variant="ghost" size="sm" disabled={busy} onClick={remove} aria-label={t.removeFile}>
              <X className="h-3.5 w-3.5" />
            </Button>
          )}
        </>
      )}
    </span>
  );
}
