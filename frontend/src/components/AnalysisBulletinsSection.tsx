import { useMemo, useState, type FormEvent } from "react";
import { FlaskConical, Plus, Trash2, FileText } from "lucide-react";
import {
  useAnalysisBulletins,
  useCreateAnalysisBulletin,
  useDeleteAnalysisBulletin,
  useBulletinOpen,
} from "@/hooks/useAnalysisBulletins";
import { useWasteCodeSearch } from "@/hooks/useWasteCodes";
import type { AnalysisBulletin } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Dialog } from "@/components/ui/dialog";
import { Combobox, type ComboboxItem } from "@/components/ui/combobox";
import { FieldError } from "@/components/ui/field-error";
import { Table, THead, TBody, TR, TH, TD, SortableTH } from "@/components/ui/table";
import { TablePagination, TableToolbar } from "@/components/ui/table-toolbar";
import { useTableView } from "@/hooks/useTableView";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm-dialog";
import { formatDate } from "@/lib/utils";

const t = strings.settings.bulletins;

/**
 * Buletinele de analiză ale firmei — G-7.
 *
 * <p>Se atașează unui **cod de deșeu**, nu unei mișcări, fiindcă art. 8 alin. (4) cere „o
 * caracterizare a deșeurilor periculoase generate din propria activitate", iar scopurile pe care
 * le enumeră sunt proprietăți ale tipului de deșeu. Până la felia asta se putea atașa orice fișier
 * la o mișcare, dar nimic nu lega buletinul de cod și nimic nu semnala absența lui.
 *
 * <p>Nu există buton de editare, și e deliberat: un buletin e un document primit, nu o fișă pe
 * care o ținem noi. Dacă data sau laboratorul sunt greșite, documentul se șterge și se încarcă din
 * nou — altfel rubricile din dosar ar putea ajunge să descrie alt fișier decât cel atașat.
 */
export function AnalysisBulletinsSection({ canManage }: { canManage: boolean }) {
  const { data: bulletins, isLoading, isError } = useAnalysisBulletins();
  const createMut = useCreateAnalysisBulletin();
  const deleteMut = useDeleteAnalysisBulletin();
  const { open: openFile, openingId } = useBulletinOpen();
  const { notify } = useToast();
  const [confirm, confirmDialog] = useConfirm();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [wasteCode, setWasteCode] = useState<ComboboxItem | null>(null);
  const [codeQuery, setCodeQuery] = useState("");
  const [issueDate, setIssueDate] = useState("");
  const [laboratory, setLaboratory] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const codeSearch = useWasteCodeSearch(codeQuery);
  const codeItems: ComboboxItem[] = (codeSearch.data ?? []).map((w) => ({
    id: w.id,
    label: `${w.code} — ${w.name}`,
    sublabel: w.hazardous ? strings.movements.hazardous : undefined,
  }));

  const rows = bulletins ?? [];

  /**
   * Al câtelea buletin e fiecare pe codul lui. Lista vine deja ordonată descrescător după dată,
   * deci primul pe care îl vedem pentru un cod e cel mai recent — cel care răspunde la „ai
   * caracterizarea?". Restul sunt istoricul, și se marchează ca atare.
   */
  const latestPerCode = useMemo(() => {
    const seen = new Set<string>();
    const latest = new Set<string>();
    for (const b of rows) {
      if (!seen.has(b.wasteCode)) {
        seen.add(b.wasteCode);
        latest.add(b.id);
      }
    }
    return latest;
  }, [rows]);

  const view = useTableView(rows, {
    searchText: (b) => [b.wasteCode, b.wasteCodeName, b.laboratory].filter(Boolean).join(" "),
    comparators: {
      wasteCode: (a, b) => a.wasteCode.localeCompare(b.wasteCode, "ro"),
      issueDate: (a, b) => a.issueDate.localeCompare(b.issueDate),
    },
  });

  function openCreate() {
    setWasteCode(null);
    setCodeQuery("");
    setIssueDate("");
    setLaboratory("");
    setFile(null);
    setErrors({});
    setDialogOpen(true);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const next: Record<string, string> = {};
    if (!wasteCode) next.wasteCode = strings.common.requiredField;
    if (!issueDate) next.issueDate = strings.common.requiredField;
    if (!laboratory.trim()) next.laboratory = strings.common.requiredField;
    if (!file) next.file = strings.common.requiredField;
    setErrors(next);
    if (Object.keys(next).length > 0) return;

    try {
      await createMut.mutateAsync({
        wasteCodeId: wasteCode!.id,
        issueDate,
        laboratory: laboratory.trim(),
        file: file!,
      });
      notify(t.created, "success");
      setDialogOpen(false);
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  function handleDelete(b: AnalysisBulletin) {
    confirm({
      title: t.confirmDeleteTitle,
      message: t.confirmDelete,
      confirmLabel: t.delete,
      tone: "danger",
      onConfirm: async () => {
        try {
          await deleteMut.mutateAsync(b.id);
          notify(t.deleted, "success");
        } catch (err) {
          notify(apiErrorMessage(err, t.saveError), "error");
        }
      },
    });
  }

  return (
    <section id="buletine-analiza" className="mt-8 scroll-mt-20">
      <div className="mb-3 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-content">{t.title}</h2>
          <p className="mt-1 max-w-3xl text-sm text-content-muted">{t.subtitle}</p>
        </div>
        {canManage && (
          <Button onClick={openCreate}>
            <Plus className="mr-2 h-4 w-4" />
            {t.add}
          </Button>
        )}
      </div>

      {isError && <p className="text-sm text-red-600">{t.loadError}</p>}

      {!isError && (
        <>
          <TableToolbar view={view} placeholder={t.searchPlaceholder} />
          <Table stickyHeader>
            <THead sticky>
              <TR>
                <SortableTH sortKey="wasteCode" sort={view.sort} onSort={view.toggleSort}>
                  {t.wasteCode}
                </SortableTH>
                <SortableTH sortKey="issueDate" sort={view.sort} onSort={view.toggleSort}>
                  {t.issueDate}
                </SortableTH>
                <TH>{t.laboratory}</TH>
                <TH>{t.file}</TH>
                {canManage && (
                  <TH sticky="right" className="text-right">
                    {strings.common.actions}
                  </TH>
                )}
              </TR>
            </THead>
            <TBody>
              {(isLoading || view.visible.length === 0) && (
                <TableFallbackRow
                  columns={canManage ? 5 : 4}
                  loading={isLoading}
                  icon={FlaskConical}
                  title={view.emptiedBySearch ? strings.common.noResults : t.empty}
                  description={
                    view.emptiedBySearch ? strings.common.noResultsHint : t.emptyHint
                  }
                  action={
                    canManage && !view.emptiedBySearch && (
                      <Button onClick={openCreate}>
                        <Plus className="mr-2 h-4 w-4" />
                        {t.add}
                      </Button>
                    )
                  }
                />
              )}
              {view.visible.map((b) => (
                <TR key={b.id}>
                  <TD>
                    <span className="font-medium text-content">{b.wasteCode}</span>
                    <span className="block text-xs text-content-muted">{b.wasteCodeName}</span>
                  </TD>
                  <TD>
                    {formatDate(b.issueDate)}
                    <span className="ml-2 align-middle">
                      {latestPerCode.has(b.id) ? (
                        <Badge variant="success">{t.latest}</Badge>
                      ) : (
                        <Badge variant="muted">{t.history}</Badge>
                      )}
                    </span>
                  </TD>
                  <TD>{b.laboratory}</TD>
                  <TD>
                    <button
                      type="button"
                      onClick={() => openFile(b)}
                      disabled={openingId === b.id}
                      className="inline-flex items-center gap-1 text-brand underline-offset-2 hover:underline disabled:opacity-60"
                    >
                      <FileText className="h-4 w-4" />
                      {b.fileName || t.openFile}
                    </button>
                  </TD>
                  {canManage && (
                    <TD sticky="right" className="text-right whitespace-nowrap">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleDelete(b)}
                        aria-label={`${t.delete} — ${b.wasteCode}`}
                      >
                        <Trash2 className="mr-1 h-4 w-4" />
                        {t.delete}
                      </Button>
                    </TD>
                  )}
                </TR>
              ))}
            </TBody>
          </Table>
          <TablePagination view={view} />
          <p className="mt-2 text-xs text-content-muted">{t.noValidity}</p>
        </>
      )}

      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        title={t.addTitle}
        busy={createMut.isPending}
        footer={
          <>
            <Button variant="outline" onClick={() => setDialogOpen(false)}>
              {strings.common.cancel}
            </Button>
            <Button type="submit" form="bulletin-form" loading={createMut.isPending}>
              {strings.common.save}
            </Button>
          </>
        }
      >
        <form id="bulletin-form" onSubmit={handleSubmit} className="space-y-4">
          <div>
            <Label htmlFor="bl-code" required>
              {t.wasteCode}
            </Label>
            <Combobox
              id="bl-code"
              value={wasteCode}
              onSelect={setWasteCode}
              onQueryChange={setCodeQuery}
              items={codeItems}
              loading={codeSearch.isFetching}
              placeholder={strings.movements.wasteCodePlaceholder}
              searchPlaceholder={strings.movements.wasteCodeSearch}
              invalid={errors.wasteCode ? "bl-code-err" : undefined}
            />
            <FieldError id="bl-code-err" message={errors.wasteCode} />
            <p className="mt-1 text-xs text-content-muted">{t.wasteCodeHint}</p>
          </div>

          <div>
            <Label htmlFor="bl-date" required>
              {t.issueDate}
            </Label>
            <Input
              id="bl-date"
              type="date"
              value={issueDate}
              max={new Date().toISOString().slice(0, 10)}
              onChange={(e) => setIssueDate(e.target.value)}
              {...(errors.issueDate ? { "aria-invalid": true, "aria-describedby": "bl-date-err" } : {})}
            />
            <FieldError id="bl-date-err" message={errors.issueDate} />
            <p className="mt-1 text-xs text-content-muted">{t.issueDateHint}</p>
          </div>

          <div>
            <Label htmlFor="bl-lab" required>
              {t.laboratory}
            </Label>
            <Input
              id="bl-lab"
              value={laboratory}
              onChange={(e) => setLaboratory(e.target.value)}
              placeholder={t.laboratoryPlaceholder}
              {...(errors.laboratory ? { "aria-invalid": true, "aria-describedby": "bl-lab-err" } : {})}
            />
            <FieldError id="bl-lab-err" message={errors.laboratory} />
          </div>

          <div>
            <Label htmlFor="bl-file" required>
              {t.file}
            </Label>
            <Input
              id="bl-file"
              type="file"
              accept="application/pdf,image/*"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              {...(errors.file ? { "aria-invalid": true, "aria-describedby": "bl-file-err" } : {})}
            />
            <FieldError id="bl-file-err" message={errors.file} />
            <p className="mt-1 text-xs text-content-muted">{t.fileHint}</p>
          </div>
        </form>
      </Dialog>
      {confirmDialog}
    </section>
  );
}
