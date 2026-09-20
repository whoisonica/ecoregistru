import { useState, type FormEvent } from "react";
import { SETTINGS_CARD } from "@/components/ui/card";
import { Ban, Package, Pencil, Plus, RotateCcw } from "lucide-react";
import {
  useWasteArticles,
  useCreateWasteArticle,
  useUpdateWasteArticle,
  useDeactivateWasteArticle,
  useReactivateWasteArticle,
} from "@/hooks/useWasteArticles";
import { useWasteCodeSearch } from "@/hooks/useWasteCodes";
import type { WasteArticle } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Dialog } from "@/components/ui/dialog";
import { Combobox, type ComboboxItem } from "@/components/ui/combobox";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { SortableTH } from "@/components/ui/table";
import { TablePagination, TableToolbar } from "@/components/ui/table-toolbar";
import { useTableView } from "@/hooks/useTableView";
import { useActiveFilter } from "@/components/ui/active-filter";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm-dialog";
import { LoadError } from "@/components/ui/load-error";

const t = strings.settings.articles;

/**
 * Catalogul de sortimente al depozitului (D1.6). La cântar omul alege sortimentul, nu codul.
 *
 * <p>Bifa „metal” se propune din cod (`metalSuggested`, calculat pe server) până când omul o atinge;
 * de acolo rămâne alegerea lui. Catalogul îl personalizează oricine scrie, și operatorul (proprietarul,
 * 15.09.2026); `canManage` vine din `canWrite`, iar serverul are același prag.
 */
export function WasteArticlesSection({ canManage }: { canManage: boolean }) {
  const { data: articles, isLoading, isError, refetch } = useWasteArticles();
  const createMut = useCreateWasteArticle();
  const updateMut = useUpdateWasteArticle();
  const deactivateMut = useDeactivateWasteArticle();
  const reactivateMut = useReactivateWasteArticle();
  const { notify } = useToast();
  const [confirm, confirmDialog] = useConfirm();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<WasteArticle | null>(null);
  const [name, setName] = useState("");
  const [code, setCode] = useState<ComboboxItem | null>(null);
  const [metal, setMetal] = useState(false);
  const [metalTouched, setMetalTouched] = useState(false);
  const [forbidden, setForbidden] = useState(false);
  const [nameError, setNameError] = useState(false);
  const [codeError, setCodeError] = useState(false);
  const [codeQuery, setCodeQuery] = useState("");
  const codeSearch = useWasteCodeSearch(codeQuery);

  const { rows: visibleArticles, control: activeFilter } = useActiveFilter(articles ?? []);
  const view = useTableView(visibleArticles, {
    searchText: (a) => [a.name, a.wasteCode, a.wasteCodeName].join(" "),
    comparators: { name: (a, b) => a.name.localeCompare(b.name, "ro") },
  });

  const isSubmitting = createMut.isPending || updateMut.isPending;
  const codeItems: ComboboxItem[] = (codeSearch.data ?? []).map((w) => ({
    id: w.id,
    label: `${w.code} — ${w.name}`,
  }));

  function openCreate() {
    setEditing(null);
    setName("");
    setCode(null);
    setMetal(false);
    setMetalTouched(false);
    setForbidden(false);
    setNameError(false);
    setCodeError(false);
    setDialogOpen(true);
  }

  function openEdit(a: WasteArticle) {
    setEditing(a);
    setName(a.name);
    setCode({ id: a.wasteCodeId, label: `${a.wasteCode} — ${a.wasteCodeName}` });
    setMetal(a.metal);
    // Un sortiment salvat are deja alegerea omului; schimbarea codului nu i-o mai rescrie.
    setMetalTouched(true);
    setForbidden(a.forbiddenFromIndividuals);
    setNameError(false);
    setCodeError(false);
    setDialogOpen(true);
  }

  function selectCode(item: ComboboxItem | null) {
    setCode(item);
    if (item) setCodeError(false);
    const found = (codeSearch.data ?? []).find((w) => w.id === item?.id);
    if (found && !metalTouched) setMetal(found.metalSuggested);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const missingName = !name.trim();
    setNameError(missingName);
    setCodeError(!code);
    if (missingName || !code) return;
    const input = {
      name: name.trim(),
      wasteCodeId: code.id,
      metal,
      forbiddenFromIndividuals: forbidden,
    };
    try {
      if (editing) {
        await updateMut.mutateAsync({ id: editing.id, input });
        notify(t.updated, "success");
      } else {
        await createMut.mutateAsync(input);
        notify(t.created, "success");
      }
      setDialogOpen(false);
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  function handleDeactivate(a: WasteArticle) {
    confirm({
      title: t.confirmDeactivateTitle,
      message: (
        <>
          <strong className="text-content">{a.name}</strong> — {a.wasteCode}. {t.confirmDeactivate}
        </>
      ),
      confirmLabel: t.deactivate,
      tone: "danger",
      onConfirm: () =>
        deactivateMut.mutate(a.id, {
          onSuccess: () => notify(t.deactivated, "success"),
          onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
        }),
    });
  }

  function reactivate(a: WasteArticle) {
    reactivateMut.mutate(a.id, {
      onSuccess: () => notify(strings.common.reactivated, "success"),
      onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
    });
  }

  return (
    <section id="sortimente" className={SETTINGS_CARD}>
      <div className="mb-3 flex items-start justify-between">
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

      {isError && <LoadError message={t.loadError} onRetry={refetch} />}

      {!isError && (
        <>
          <TableToolbar view={view} placeholder={t.searchPlaceholder}>
            {activeFilter}
          </TableToolbar>
          <Table stickyHeader>
            <THead sticky>
              <TR>
                <SortableTH sortKey="name" sort={view.sort} onSort={view.toggleSort}>
                  {t.name}
                </SortableTH>
                <TH>{t.code}</TH>
                <TH>{strings.common.status}</TH>
                {canManage && <TH sticky="right" className="text-right">{strings.common.actions}</TH>}
              </TR>
            </THead>
            <TBody>
              {(isLoading || view.visible.length === 0) && (
                <TableFallbackRow
                  columns={canManage ? 4 : 3}
                  loading={isLoading}
                  icon={Package}
                  title={view.emptiedBySearch ? strings.common.noResults : t.empty}
                  description={view.emptiedBySearch ? strings.common.noResultsHint : t.emptyHint}
                />
              )}
              {view.visible.map((a) => (
                <TR key={a.id}>
                  <TD className="font-medium text-content">
                    <div className="flex flex-wrap items-center gap-1.5">
                      {a.name}
                      {a.metal && <Badge variant="muted">{t.badgeMetal}</Badge>}
                      {a.forbiddenFromIndividuals && <Badge variant="danger">{t.badgeForbidden}</Badge>}
                    </div>
                  </TD>
                  <TD>
                    <span className="font-mono">{a.wasteCode}{a.hazardous ? "*" : ""}</span>
                    <span className="block text-xs text-content-muted">{a.wasteCodeName}</span>
                  </TD>
                  <TD>
                    {a.active ? (
                      <Badge variant="success">{t.active}</Badge>
                    ) : (
                      <Badge variant="muted">{t.inactive}</Badge>
                    )}
                  </TD>
                  {canManage && (
                    <TD sticky="right" className="text-right">
                      <div className="flex justify-end gap-1">
                        <Button variant="ghost" size="sm" onClick={() => openEdit(a)}>
                          <Pencil className="mr-1 h-3.5 w-3.5" />
                          {strings.common.edit}
                        </Button>
                        {a.active ? (
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-red-600 hover:bg-red-50"
                            onClick={() => handleDeactivate(a)}
                          >
                            <Ban className="mr-1 h-3.5 w-3.5" />
                            {t.deactivate}
                          </Button>
                        ) : (
                          <Button variant="ghost" size="sm" onClick={() => reactivate(a)}>
                            <RotateCcw className="mr-1 h-3.5 w-3.5" />
                            {strings.common.reactivate}
                          </Button>
                        )}
                      </div>
                    </TD>
                  )}
                </TR>
              ))}
            </TBody>
          </Table>
          <TablePagination view={view} />
        </>
      )}

      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        title={editing ? t.editTitle : t.addTitle}
        footer={
          <>
            <Button variant="outline" onClick={() => setDialogOpen(false)} disabled={isSubmitting}>
              {strings.common.cancel}
            </Button>
            <Button type="submit" form="waste-article-form" disabled={isSubmitting}>
              {isSubmitting ? strings.common.saving : strings.common.save}
            </Button>
          </>
        }
      >
        <form id="waste-article-form" onSubmit={handleSubmit} className="space-y-4">
          <div>
            <Label htmlFor="wa-name">{t.name}</Label>
            <Input
              id="wa-name"
              value={name}
              onChange={(e) => {
                setName(e.target.value);
                if (nameError) setNameError(false);
              }}
              placeholder={t.namePlaceholder}
              aria-invalid={nameError}
              data-invalid={nameError || undefined}
              autoFocus
            />
            {nameError && <p className="mt-1 text-xs text-red-600">{strings.common.requiredField}</p>}
          </div>
          <div>
            <Label htmlFor="wa-code">{t.code}</Label>
            <Combobox
              id="wa-code"
              value={code}
              onSelect={selectCode}
              onQueryChange={setCodeQuery}
              items={codeItems}
              loading={codeSearch.isFetching}
              placeholder={t.codePlaceholder}
              searchPlaceholder={strings.movements.wasteCodeSearch}
              invalid={codeError ? "wa-code-error" : undefined}
            />
            {codeError && (
              <p id="wa-code-error" className="mt-1 text-xs text-red-600">
                {strings.common.requiredField}
              </p>
            )}
          </div>
          <label className="flex items-start gap-2 text-sm">
            <input
              type="checkbox"
              className="mt-0.5 h-4 w-4 rounded border-line-strong"
              checked={metal}
              onChange={(e) => {
                setMetal(e.target.checked);
                setMetalTouched(true);
              }}
            />
            <span>
              <span className="font-medium text-content-strong">{t.metal}</span>
              <span className="block text-xs text-content-muted">{t.metalHint}</span>
            </span>
          </label>
          <label className="flex items-start gap-2 text-sm">
            <input
              type="checkbox"
              className="mt-0.5 h-4 w-4 rounded border-line-strong"
              checked={forbidden}
              onChange={(e) => setForbidden(e.target.checked)}
            />
            <span>
              <span className="font-medium text-content-strong">{t.forbidden}</span>
              <span className="block text-xs text-content-muted">{t.forbiddenHint}</span>
            </span>
          </label>
        </form>
      </Dialog>

      {confirmDialog}
    </section>
  );
}
