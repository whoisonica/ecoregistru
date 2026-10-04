import { useState } from "react";
import { FileSpreadsheet, FileText, Info, Paperclip, Upload } from "lucide-react";
import {
  downloadEnergyAnnex1,
  downloadEnergyDeclaration,
  openEnergyReceipt,
  useUploadEnergyReceipt,
} from "@/hooks/useEnergy";
import { apiBlobErrorMessage, apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import type { EnergySheet } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { FileDropzone } from "@/components/ui/file-dropzone";
import { Tooltip } from "@/components/ui/tooltip";
import { useToast } from "@/components/ui/toast";
import { EnergyContactDialog } from "@/components/energy/EnergyContactDialog";

const t = strings.energy;

type Doc = "annex1" | "declaration" | "receipt";

const isPdf = (f: File) => f.type === "application/pdf" || f.name.toLowerCase().endsWith(".pdf");

/**
 * Rândul de documente: butoane lipite, cu numele documentului și fără text dedesubt (ca pe tabul
 * „Ambalaje”). Peste prag, „Anexa 1” e stinsă, iar de ce stă în `Tooltip` lângă ea, nu în jurul ei.
 * „Încarcă recipisa” schimbă date, deci e `muted` și ultima dintre ele.
 */
export function EnergyDocuments({ sheet, canWrite }: { sheet: EnergySheet; canWrite: boolean }) {
  const [busy, setBusy] = useState<Doc | null>(null);
  const [uploading, setUploading] = useState(false);
  const [contactOpen, setContactOpen] = useState(false);
  const { notify } = useToast();
  const year = sheet.year;

  async function run(doc: Doc, action: () => Promise<void>, fallback: string) {
    setBusy(doc);
    try {
      await action();
    } catch (err) {
      notify(await apiBlobErrorMessage(err, fallback), "error");
    } finally {
      setBusy(null);
    }
  }

  return (
    <div className="flex flex-wrap items-center gap-2">
      <Button
        variant="outline"
        disabled={sheet.overThreshold || busy !== null}
        loading={busy === "annex1"}
        onClick={() => run("annex1", () => downloadEnergyAnnex1(year), t.annex1Error)}
      >
        {busy !== "annex1" && <FileSpreadsheet className="mr-2 h-4 w-4" aria-hidden />}
        {t.annex1}
      </Button>
      {sheet.overThreshold && (
        <Tooltip content={t.annex1Disabled}>
          <Info className="h-4 w-4 text-content-subtle" aria-label={t.annex1Disabled} />
        </Tooltip>
      )}
      <Button
        variant="outline"
        disabled={busy !== null}
        loading={busy === "declaration"}
        onClick={() => run("declaration", () => downloadEnergyDeclaration(year), t.declarationDocError)}
      >
        {busy !== "declaration" && <FileText className="mr-2 h-4 w-4" aria-hidden />}
        {t.declaration}
      </Button>
      {sheet.receipt && (
        <Button
          variant="outline"
          disabled={busy !== null}
          loading={busy === "receipt"}
          onClick={() => run("receipt", () => openEnergyReceipt(year), t.receiptOpenError)}
        >
          {busy !== "receipt" && <Paperclip className="mr-2 h-4 w-4" aria-hidden />}
          {t.receipt}
        </Button>
      )}
      {canWrite && (
        <>
          <Button variant="muted" onClick={() => setUploading(true)}>
            <Upload className="mr-2 h-4 w-4" aria-hidden />
            {t.uploadReceipt}
          </Button>
          <Button variant="outline" onClick={() => setContactOpen(true)}>
            {t.contactTitle}
          </Button>
          <ReceiptDialog year={year} open={uploading} onClose={() => setUploading(false)} />
          {contactOpen && <EnergyContactDialog open onClose={() => setContactOpen(false)} contact={sheet.contact} />}
        </>
      )}
    </div>
  );
}

/** „Încarcă recipisa”: un singur PDF, prin `FileDropzone`. */
function ReceiptDialog({ year, open, onClose }: { year: number; open: boolean; onClose: () => void }) {
  const [files, setFiles] = useState<File[]>([]);
  const [error, setError] = useState<string | null>(null);
  const upload = useUploadEnergyReceipt();
  const { notify } = useToast();
  const file = files[0];

  const close = () => {
    setFiles([]);
    setError(null);
    onClose();
  };

  function submit() {
    if (!file) return;
    upload.mutate(
      { year, file },
      {
        onSuccess: () => {
          notify(t.receiptSaved, "success");
          close();
        },
        onError: (err) => setError(apiErrorMessage(err, t.receiptError)),
      }
    );
  }

  return (
    <Dialog
      open={open}
      onClose={close}
      title={t.receiptDialogTitle}
      description={t.receiptDialogDescription}
      busy={upload.isPending}
      footer={
        <>
          <Button variant="outline" onClick={close} disabled={upload.isPending}>
            {strings.common.cancel}
          </Button>
          <Button onClick={submit} loading={upload.isPending} disabled={!file}>
            {t.receiptSubmit}
          </Button>
        </>
      }
    >
      <FileDropzone
        files={files}
        hint={t.receiptDrop}
        accept="application/pdf,.pdf"
        limitHint={t.receiptHint}
        disabled={upload.isPending}
        onReject={setError}
        onChange={(next) => {
          // O recipisă, nu mai multe: ultima aleasă o înlocuiește pe cea de dinainte.
          const last = next[next.length - 1];
          if (last && !isPdf(last)) {
            setError(t.receiptNotPdf);
            return;
          }
          setError(null);
          setFiles(last ? [last] : []);
        }}
      />
      {error && (
        <p role="alert" className="mt-2 text-sm text-state-bad-text">
          {error}
        </p>
      )}
    </Dialog>
  );
}
