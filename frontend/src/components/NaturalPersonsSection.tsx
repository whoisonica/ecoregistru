import { useEffect, useState, type FormEvent } from "react";
import { Ban, Info, Pencil, Plus, RotateCcw, Trash2, UserRound } from "lucide-react";
import { Tooltip } from "@/components/ui/tooltip";
import {
  useNaturalPersons,
  useNaturalPerson,
  useCreateNaturalPerson,
  useUpdateNaturalPerson,
  useDeactivateNaturalPerson,
  useReactivateNaturalPerson,
  useDeleteNaturalPerson,
} from "@/hooks/useNaturalPersons";
import type { NaturalPersonSummary } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Dialog } from "@/components/ui/dialog";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { SortableTH } from "@/components/ui/table";
import { TablePagination, TableToolbar } from "@/components/ui/table-toolbar";
import { useTableView } from "@/hooks/useTableView";
import { useActiveFilter } from "@/components/ui/active-filter";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm-dialog";

const t = strings.naturalPersons;

/** Aceeași regulă ca `ValidCnp` din backend: 13 cifre, cheia 279146358279, restul 10 se scrie 1. */
function isValidCnp(cnp: string): boolean {
  if (!/^\d{13}$/.test(cnp)) return false;
  const key = "279146358279";
  let sum = 0;
  for (let i = 0; i < 12; i++) sum += Number(cnp[i]) * Number(key[i]);
  const control = sum % 11 === 10 ? 1 : sum % 11;
  return control === Number(cnp[12]);
}

/**
 * Tabul „Persoane fizice” de lângă Parteneri (D1.7b). Lista vine cu CNP-ul mascat (ultimele 4 cifre),
 * iar formularul cere fișa întreagă abia la deschidere. Fișa unei persoane care apare pe operațiuni nu
 * se șterge, doar se dezactivează: borderoul se păstrează 10 ani (Legea 82/1991 art. 25).
 */
export function NaturalPersonsSection({ canManage }: { canManage: boolean }) {
  const { data: persons, isLoading, isError } = useNaturalPersons();
  const createMut = useCreateNaturalPerson();
  const updateMut = useUpdateNaturalPerson();
  const deactivateMut = useDeactivateNaturalPerson();
  const reactivateMut = useReactivateNaturalPerson();
  const deleteMut = useDeleteNaturalPerson();
  const { notify } = useToast();
  const [confirm, confirmDialog] = useConfirm();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [name, setName] = useState("");
  const [cnp, setCnp] = useState("");
  const [identification, setIdentification] = useState("");
  const [address, setAddress] = useState("");
  const [nameError, setNameError] = useState(false);
  const [cnpError, setCnpError] = useState(false);

  // Fișa întreagă, doar cât formularul e deschis pe o persoană existentă.
  const detail = useNaturalPerson(dialogOpen ? editingId : null);
  useEffect(() => {
    if (!detail.data) return;
    setName(detail.data.name);
    setCnp(detail.data.cnp ?? "");
    setIdentification(detail.data.identification ?? "");
    setAddress(detail.data.address ?? "");
  }, [detail.data]);

  const { rows: visiblePersons, control: activeFilter } = useActiveFilter(persons ?? []);
  const view = useTableView(visiblePersons, {
    searchText: (p) => [p.name, p.cnpLastDigits].filter(Boolean).join(" "),
    comparators: { name: (a, b) => a.name.localeCompare(b.name, "ro") },
  });

  const isSubmitting = createMut.isPending || updateMut.isPending;
  const waitingForDetail = editingId !== null && detail.isLoading;

  function resetForm() {
    setName("");
    setCnp("");
    setIdentification("");
    setAddress("");
    setNameError(false);
    setCnpError(false);
  }

  function openCreate() {
    setEditingId(null);
    resetForm();
    setDialogOpen(true);
  }

  function openEdit(p: NaturalPersonSummary) {
    setEditingId(p.id);
    resetForm();
    setName(p.name);
    setDialogOpen(true);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const missingName = !name.trim();
    const badCnp = cnp.trim() !== "" && !isValidCnp(cnp.trim());
    setNameError(missingName);
    setCnpError(badCnp);
    if (missingName || badCnp) return;
    const input = {
      name: name.trim(),
      cnp: cnp.trim() || null,
      identification: identification.trim() || null,
      address: address.trim() || null,
    };
    try {
      if (editingId) {
        await updateMut.mutateAsync({ id: editingId, input });
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

  function handleDeactivate(p: NaturalPersonSummary) {
    confirm({
      title: t.confirmDeactivateTitle,
      message: (
        <>
          <strong className="text-content">{p.name}</strong>. {t.confirmDeactivate}
        </>
      ),
      confirmLabel: t.deactivate,
      tone: "danger",
      onConfirm: () =>
        deactivateMut.mutate(p.id, {
          onSuccess: () => notify(t.deactivated, "success"),
          onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
        }),
    });
  }

  function handleDelete(p: NaturalPersonSummary) {
    confirm({
      title: t.confirmDeleteTitle,
      message: (
        <>
          <strong className="text-content">{p.name}</strong>. {t.confirmDelete}
        </>
      ),
      confirmLabel: t.delete,
      tone: "danger",
      onConfirm: () =>
        deleteMut.mutate(p.id, {
          onSuccess: () => notify(t.deleted, "success"),
          onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
        }),
    });
  }

  function reactivate(p: NaturalPersonSummary) {
    reactivateMut.mutate(p.id, {
      onSuccess: () => notify(strings.common.reactivated, "success"),
      onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
    });
  }

  return (
    <section className="mt-4">
      {/* Explicația listei stă în antetul paginii (PartnersPage), nu a doua oară aici. */}
      <div className="mb-3 flex flex-wrap items-start justify-end gap-3">
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
          <TableToolbar view={view} placeholder={t.searchPlaceholder}>
            {activeFilter}
          </TableToolbar>
          <Table stickyHeader>
            <THead sticky>
              <TR>
                <SortableTH sortKey="name" sort={view.sort} onSort={view.toggleSort}>
                  {t.name}
                </SortableTH>
                <TH>{t.cnp}</TH>
                <TH>{t.metalColumn}</TH>
                <TH>{strings.common.status}</TH>
                {canManage && <TH sticky="right" className="text-right">{strings.common.actions}</TH>}
              </TR>
            </THead>
            <TBody>
              {(isLoading || view.visible.length === 0) && (
                <TableFallbackRow
                  columns={canManage ? 5 : 4}
                  loading={isLoading}
                  icon={UserRound}
                  title={view.emptiedBySearch ? strings.common.noResults : t.empty}
                  description={view.emptiedBySearch ? strings.common.noResultsHint : t.emptyHint}
                />
              )}
              {view.visible.map((p) => (
                <TR key={p.id}>
                  <TD className="font-medium text-content">{p.name}</TD>
                  <TD>
                    {p.cnpLastDigits ? (
                      <span className="font-mono">•••••••••{p.cnpLastDigits}</span>
                    ) : (
                      <span className="text-content-subtle">—</span>
                    )}
                  </TD>
                  <TD>
                    {p.metalReady ? (
                      <Badge variant="success">{t.metalReady}</Badge>
                    ) : (
                      <Badge variant="muted">{t.nameOnly}</Badge>
                    )}
                  </TD>
                  <TD>
                    {p.active ? (
                      <Badge variant="success">{t.active}</Badge>
                    ) : (
                      <Badge variant="muted">{t.inactive}</Badge>
                    )}
                  </TD>
                  {canManage && (
                    <TD sticky="right" className="text-right">
                      <div className="flex justify-end gap-1">
                        <Button variant="ghost" size="sm" onClick={() => openEdit(p)}>
                          <Pencil className="mr-1 h-3.5 w-3.5" />
                          {strings.common.edit}
                        </Button>
                        {p.active ? (
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-red-600 hover:bg-red-50"
                            onClick={() => handleDeactivate(p)}
                          >
                            <Ban className="mr-1 h-3.5 w-3.5" />
                            {t.deactivate}
                          </Button>
                        ) : (
                          <>
                            <Button variant="ghost" size="sm" onClick={() => reactivate(p)}>
                              <RotateCcw className="mr-1 h-3.5 w-3.5" />
                              {strings.common.reactivate}
                            </Button>
                            {p.hasOperations ? (
                              // `title` nu se citește pe telefon și nici din tastatură; `Tooltip` da.
                              <span className="self-center px-2">
                                <Tooltip content={t.hasOperationsHint}>
                                  <Info className="h-4 w-4 text-content-subtle" aria-hidden />
                                  <span className="sr-only">{t.hasOperationsLabel}</span>
                                </Tooltip>
                              </span>
                            ) : (
                              <Button
                                variant="ghost"
                                size="sm"
                                className="text-red-600 hover:bg-red-50"
                                onClick={() => handleDelete(p)}
                              >
                                <Trash2 className="mr-1 h-3.5 w-3.5" />
                                {t.delete}
                              </Button>
                            )}
                          </>
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
        title={editingId ? t.editTitle : t.addTitle}
        footer={
          <>
            <Button variant="outline" onClick={() => setDialogOpen(false)} disabled={isSubmitting}>
              {strings.common.cancel}
            </Button>
            <Button type="submit" form="natural-person-form" disabled={isSubmitting || waitingForDetail}>
              {isSubmitting ? strings.common.saving : strings.common.save}
            </Button>
          </>
        }
      >
        {detail.isError && <p className="mb-3 text-sm text-red-600">{t.loadOneError}</p>}
        <form id="natural-person-form" onSubmit={handleSubmit} className="space-y-4">
          <div>
            <Label htmlFor="np-name">{t.name}</Label>
            <Input
              id="np-name"
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
            <Label htmlFor="np-cnp">{t.cnp}</Label>
            <Input
              id="np-cnp"
              value={cnp}
              inputMode="numeric"
              maxLength={13}
              autoComplete="off"
              onChange={(e) => {
                setCnp(e.target.value.replace(/\D/g, ""));
                if (cnpError) setCnpError(false);
              }}
              disabled={waitingForDetail}
              aria-invalid={cnpError}
              data-invalid={cnpError || undefined}
            />
            {cnpError ? (
              <p className="mt-1 text-xs text-red-600">{t.cnpInvalid}</p>
            ) : (
              <p className="mt-1 text-xs text-content-muted">{t.cnpHint}</p>
            )}
          </div>
          <div>
            <Label htmlFor="np-id">{t.identification}</Label>
            <Input
              id="np-id"
              value={identification}
              onChange={(e) => setIdentification(e.target.value)}
              placeholder={t.identificationPlaceholder}
              autoComplete="off"
              disabled={waitingForDetail}
            />
          </div>
          <div>
            <Label htmlFor="np-address">{t.address}</Label>
            <Input
              id="np-address"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              placeholder={t.addressPlaceholder}
              disabled={waitingForDetail}
            />
          </div>
        </form>
      </Dialog>

      {confirmDialog}
    </section>
  );
}
