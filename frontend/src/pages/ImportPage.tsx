import { useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Navigate } from "react-router-dom";
import { useAuth } from "@/auth/AuthContext";
import { canImport } from "@/lib/roles";
import { CheckCircle2, Download, FileUp } from "lucide-react";
import { api, apiBlobErrorMessage, apiErrorMessage } from "@/lib/api";
import { saveBlob } from "@/lib/download";
import { strings } from "@/lib/strings";
import { PageHeader } from "@/components/ui/page-header";
import { Card, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm-dialog";
import { Badge } from "@/components/ui/badge";
import { formatDate } from "@/lib/utils";
import { countOf } from "@/lib/count";

/** Oglinda lui `ImportResultResponse`. */
interface ImportResult {
  saved: boolean;
  partnersNew: number;
  partnersExisting: number;
  movementsNew: number;
  movementsExisting: number;
  workPointsNew: number;
  workPointsExisting: number;
  errors: RowMessage[];
  warnings: RowMessage[];
}

interface RowMessage {
  sheet: string;
  row: number;
  message: string;
}

/** Oglinda lui `ImportBatchResponse` (V62). */
interface ImportBatch {
  id: string;
  fileName: string | null;
  createdAt: string;
  workPointsNew: number;
  partnersNew: number;
  movementsNew: number;
  movementsRemaining: number;
  movementsEdited: number;
  undoneAt: string | null;
}

/**
 * P2.15 — importul de istoric din șablonul nostru. Numai al platformei: îl facem noi, la implementare.
 *
 * <p>Două butoane, nu unul: „Verifică” rulează importul întreg pe server și îl întoarce înapoi, deci
 * erorile pe rând sunt exact cele pe care le-ar da „Importă”. „Importă” rămâne blocat până când
 * **același** fișier a trecut o verificare fără erori — altfel primul contact cu o greșeală ar fi un
 * import refuzat, nu o listă de rânduri de reparat.
 */
export function ImportPage() {
  const { user } = useAuth();
  // Numai platforma importă (16.09.2026); o adresă scrisă de mână duce acasă, nu la un ecran refuzat.
  if (!canImport(user?.role)) return <Navigate to="/" replace />;
  return <ImportScreen />;
}

function ImportScreen() {
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
        // Partenerii, mișcările, panoul, termenele și istoricul importurilor s-au schimbat toate deodată.
        qc.invalidateQueries();
        notify(
          t.saved
            .replace("{workPoints}", String(res.data.workPointsNew))
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
                [t.workPointsNew, result.workPointsNew],
                [t.workPointsExisting, result.workPointsExisting],
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
              <RowTable rows={result.errors} />
            </>
          )}

          {result.warnings.length > 0 && (
            <div data-testid="import-warnings">
              <p className="mt-4 text-sm font-medium text-state-warn-text">
                {t.warningsTitle.replace("{n}", String(result.warnings.length))}
              </p>
              <RowTable rows={result.warnings} />
            </div>
          )}
        </Card>
      )}

      <ImportHistory />
    </div>
  );
}

function RowTable({ rows }: { rows: RowMessage[] }) {
  const t = strings.importExcel;
  return (
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
          {rows.map((e, i) => (
            <TR key={i}>
              <TD>{e.sheet}</TD>
              <TD className="tabular-nums">{e.row}</TD>
              <TD>{e.message}</TD>
            </TR>
          ))}
        </TBody>
      </Table>
    </div>
  );
}

/**
 * Importurile salvate în firma aleasă și „Retrage” (V62). „Retrage”, nu „Anulează”: butonul de renunțare din
 * confirmare se numește deja „Anulează”, iar două butoane cu același nume și efecte opuse ar duce la greșeală. Anularea șterge numai mișcările neatinse de la
 * import; confirmarea spune câte pleacă și câte rămân, fiindcă asta e cifra care oprește o greșeală.
 */
function ImportHistory() {
  const t = strings.importExcel;
  const qc = useQueryClient();
  const { notify } = useToast();
  const [confirm, confirmDialog] = useConfirm();
  const history = useQuery({
    queryKey: ["import-history"],
    queryFn: async () => (await api.get<ImportBatch[]>("/api/v1/import/istoric")).data,
  });
  const undo = useMutation({
    mutationFn: async (id: string) =>
      (await api.post<{ deleted: number; kept: number }>(`/api/v1/import/${id}/anulare`)).data,
    onSuccess: (res) => {
      qc.invalidateQueries();
      notify(
        t.undone
          .replace("{deleted}", countOf(res.deleted, "mișcare", "mișcări"))
          .replace("{kept}", String(res.kept)),
        "success",
      );
    },
    onError: (err) => notify(apiErrorMessage(err, t.undoError), "error"),
  });

  return (
    <Card className="mt-4" data-testid="import-history">
      <CardHeader title={t.historyTitle} description={t.historyHint} />
      <div className="mt-4">
        {history.isError ? (
          <p className="text-sm text-state-bad-text">{t.historyError}</p>
        ) : history.data && history.data.length === 0 ? (
          <p className="text-sm text-content-muted">{t.historyEmpty}</p>
        ) : (
          <Table>
            <THead>
              <TR>
                <TH>{t.historyDate}</TH>
                <TH>{t.historyFile}</TH>
                <TH>{t.historyAdded}</TH>
                <TH>{t.historyNow}</TH>
                <TH />
              </TR>
            </THead>
            <TBody>
              {(history.data ?? []).map((b) => {
                const date = formatDate(b.createdAt);
                const deletable = b.movementsRemaining - b.movementsEdited;
                return (
                  <TR key={b.id}>
                    <TD className="tabular-nums">{date}</TD>
                    <TD>{b.fileName ?? t.unnamedFile}</TD>
                    <TD className="tabular-nums">
                      {t.historyAddedValue
                        .replace("{movements}", countOf(b.movementsNew, "mișcare", "mișcări"))
                        .replace("{partners}", countOf(b.partnersNew, "partener", "parteneri"))
                        .replace("{workPoints}", countOf(b.workPointsNew, "punct de lucru", "puncte de lucru"))}
                    </TD>
                    <TD className="tabular-nums">
                      {t.historyNowValue
                        .replace("{remaining}", countOf(b.movementsRemaining, "mișcare", "mișcări"))
                        .replace("{edited}", String(b.movementsEdited))}
                    </TD>
                    <TD className="text-right">
                      {b.undoneAt ? (
                        <Badge variant="muted">{t.historyUndone.replace("{date}", formatDate(b.undoneAt))}</Badge>
                      ) : (
                        <Button
                          variant="outline"
                          size="sm"
                          loading={undo.isPending && undo.variables === b.id}
                          disabled={undo.isPending}
                          onClick={() =>
                            confirm({
                              title: t.undoTitle,
                              message: t.undoMessage
                                .replace("{file}", b.fileName ?? t.unnamedFile)
                                .replace("{date}", date)
                                .replace("{deletable}", countOf(deletable, "mișcare", "mișcări"))
                                .replace("{edited}", String(b.movementsEdited)),
                              confirmLabel: t.undoConfirm,
                              tone: "danger",
                              onConfirm: () => undo.mutate(b.id),
                            })
                          }
                        >
                          {t.undo}
                        </Button>
                      )}
                    </TD>
                  </TR>
                );
              })}
            </TBody>
          </Table>
        )}
      </div>
      {confirmDialog}
    </Card>
  );
}
