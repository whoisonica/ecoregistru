import { useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, Download, FileUp } from "lucide-react";
import { api, apiBlobErrorMessage, apiErrorMessage } from "@/lib/api";
import { saveBlob } from "@/lib/download";
import { strings } from "@/lib/strings";
import { PageHeader } from "@/components/ui/page-header";
import { Card, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { useToast } from "@/components/ui/toast";

/** Oglinda lui `ImportResultResponse`. */
interface ImportResult {
  saved: boolean;
  partnersNew: number;
  partnersExisting: number;
  movementsNew: number;
  movementsExisting: number;
  errors: { sheet: string; row: number; message: string }[];
}

/**
 * P2.15 — importul de istoric din șablonul nostru.
 *
 * <p>Două butoane, nu unul: „Verifică” rulează importul întreg pe server și îl întoarce înapoi, deci
 * erorile pe rând sunt exact cele pe care le-ar da „Importă”. „Importă” rămâne blocat până când
 * **același** fișier a trecut o verificare fără erori — altfel primul contact cu o greșeală ar fi un
 * import refuzat, nu o listă de rânduri de reparat.
 */
export function ImportPage() {
  const t = strings.importExcel;
  const qc = useQueryClient();
  const { notify } = useToast();
  const input = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [verified, setVerified] = useState<File | null>(null);
  const [result, setResult] = useState<ImportResult | null>(null);
  const [busy, setBusy] = useState<"template" | "verify" | "import" | null>(null);

  async function downloadTemplate() {
    setBusy("template");
    try {
      const res = await api.get("/api/v1/import/sablon", { responseType: "blob" });
      saveBlob(res.data as Blob, "sablon-import-wastehouse.xlsx");
    } catch (err) {
      notify(await apiBlobErrorMessage(err, t.templateError), "error");
    } finally {
      setBusy(null);
    }
  }

  function choose(picked: File | null) {
    setFile(picked);
    setVerified(null);
    setResult(null);
  }

  async function send(save: boolean) {
    if (!file) return;
    const form = new FormData();
    form.append("file", file);
    setBusy(save ? "import" : "verify");
    try {
      const res = await api.post<ImportResult>(save ? "/api/v1/import" : "/api/v1/import/verificare", form);
      setResult(res.data);
      setVerified(!save && res.data.errors.length === 0 ? file : null);
      if (res.data.saved) {
        // Partenerii, mișcările, panoul și termenele s-au schimbat toate deodată.
        qc.invalidateQueries();
        notify(
          t.saved
            .replace("{partners}", String(res.data.partnersNew))
            .replace("{movements}", String(res.data.movementsNew)),
          "success",
        );
      }
    } catch (err) {
      setResult(null);
      setVerified(null);
      notify(apiErrorMessage(err, save ? t.importError : t.verifyError), "error");
    } finally {
      setBusy(null);
    }
  }

  return (
    <div>
      <PageHeader title={t.title} description={t.subtitle} />

      <Card className="mt-6">
        <CardHeader
          title={t.templateTitle}
          description={t.templateHint}
          action={
            <Button variant="outline" onClick={downloadTemplate} loading={busy === "template"}>
              <Download className="mr-2 h-4 w-4" />
              {t.templateButton}
            </Button>
          }
        />
      </Card>

      <Card className="mt-4">
        <CardHeader title={t.fileTitle} description={t.fileHint} />
        <div className="mt-4 flex flex-wrap items-center gap-3">
          <input
            ref={input}
            type="file"
            accept=".xlsx"
            className="hidden"
            onChange={(e) => {
              choose(e.target.files?.[0] ?? null);
              e.target.value = "";
            }}
          />
          <Button variant="outline" onClick={() => input.current?.click()} disabled={busy !== null}>
            <FileUp className="mr-2 h-4 w-4" />
            {t.chooseFile}
          </Button>
          <span className="text-sm text-content-muted" data-testid="import-file-name">
            {file?.name ?? t.noFile}
          </span>
          <div className="ml-auto flex gap-2">
            <Button variant="outline" onClick={() => send(false)} disabled={!file || busy !== null} loading={busy === "verify"}>
              {t.verify}
            </Button>
            <Button onClick={() => send(true)} disabled={verified !== file || !file || busy !== null} loading={busy === "import"}>
              {t.import}
            </Button>
          </div>
        </div>
      </Card>

      {result && (
        <Card className="mt-4">
          <CardHeader title={result.saved ? t.resultSaved : t.resultTitle} />
          <dl className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-4">
            {(
              [
                [t.partnersNew, result.partnersNew],
                [t.partnersExisting, result.partnersExisting],
                [t.movementsNew, result.movementsNew],
                [t.movementsExisting, result.movementsExisting],
              ] as const
            ).map(([label, value]) => (
              <div key={label}>
                <dt className="text-xs text-content-muted">{label}</dt>
                <dd className="text-2xl font-semibold text-content">{value}</dd>
              </div>
            ))}
          </dl>

          {result.errors.length === 0 ? (
            <p className="mt-4 flex items-center gap-2 text-sm text-green-700">
              <CheckCircle2 className="h-4 w-4" />
              {result.saved ? t.afterImport : t.noErrors}
            </p>
          ) : (
            <>
              <p className="mt-4 text-sm font-medium text-red-700">
                {t.errorsTitle.replace("{n}", String(result.errors.length))}
              </p>
              <div className="mt-2">
                <Table>
                  <THead>
                    <TR>
                      <TH>{t.sheet}</TH>
                      <TH>{t.row}</TH>
                      <TH>{t.problem}</TH>
                    </TR>
                  </THead>
                  <TBody>
                    {result.errors.map((e, i) => (
                      <TR key={i}>
                        <TD>{e.sheet}</TD>
                        <TD className="tabular-nums">{e.row}</TD>
                        <TD>{e.message}</TD>
                      </TR>
                    ))}
                  </TBody>
                </Table>
              </div>
            </>
          )}
        </Card>
      )}
    </div>
  );
}
