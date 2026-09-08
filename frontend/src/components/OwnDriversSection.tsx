import { useState, type FormEvent } from "react";
import { Ban, UserCircle, Pencil, Plus, RotateCcw } from "lucide-react";
import {
  useDrivers,
  useCreateDriver,
  useUpdateDriver,
  useDeactivateDriver,
  useReactivateDriver,
} from "@/hooks/useDrivers";
import type { Driver } from "@/lib/types";
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

const t = strings.settings.drivers;

/**
 * Șoferii firmei — cazul „— transportăm noi —" de pe formularul de mișcare.
 *
 * <p>Aici sunt doar ai noștri. Șoferii unui transportator se editează în fișa lui, din Parteneri,
 * unde lista se înlocuiește la salvare; două drumuri de scriere către aceleași rânduri ar însemna
 * că un șofer adăugat de aici dispare data viitoare când cineva deschide și salvează partenerul.
 * Ecranul îi și ascunde, ca lista să fie ce zice titlul.
 */
export function OwnDriversSection({ canManage }: { canManage: boolean }) {
  const { data: allDrivers, isLoading, isError } = useDrivers();
  const createMut = useCreateDriver();
  const updateMut = useUpdateDriver();
  const deactivateMut = useDeactivateDriver();
  const reactivateMut = useReactivateDriver();
  const { notify } = useToast();
  const [confirm, confirmDialog] = useConfirm();

  const drivers = (allDrivers ?? []).filter((d) => d.partnerId === null);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Driver | null>(null);
  const [name, setName] = useState("");
  const [identification, setIdentification] = useState("");
  const [vehicleRegistration, setVehicleRegistration] = useState("");
  const [nameError, setNameError] = useState(false);

  const { rows: visibleDrivers, control: activeFilter } = useActiveFilter(drivers);
  const view = useTableView(visibleDrivers, {
    searchText: (d) => [d.name, d.identification, d.vehicleRegistration].filter(Boolean).join(" "),
    comparators: { name: (a, b) => a.name.localeCompare(b.name, "ro") },
  });

  const isSubmitting = createMut.isPending || updateMut.isPending;

  function openCreate() {
    setEditing(null);
    setName("");
    setIdentification("");
    setVehicleRegistration("");
    setNameError(false);
    setDialogOpen(true);
  }

  function openEdit(d: Driver) {
    setEditing(d);
    setName(d.name);
    setIdentification(d.identification ?? "");
    setVehicleRegistration(d.vehicleRegistration ?? "");
    setNameError(false);
    setDialogOpen(true);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!name.trim()) {
      setNameError(true);
      return;
    }
    const input = {
      name: name.trim(),
      identification: identification.trim() || null,
      vehicleRegistration: vehicleRegistration.trim() || null,
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

  function handleDeactivate(d: Driver) {
    confirm({
      title: t.confirmDeactivateTitle,
      message: (
        <>
          <strong className="text-content">{d.name}</strong>
          {d.vehicleRegistration ? ` — ${d.vehicleRegistration}` : ""}. {t.confirmDeactivate}
        </>
      ),
      confirmLabel: t.deactivate,
      tone: "danger",
      onConfirm: () => deactivate(d),
    });
  }

  function deactivate(d: Driver) {
    deactivateMut.mutate(d.id, {
      onSuccess: () => notify(t.deactivated, "success"),
      onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
    });
  }

  function reactivate(d: Driver) {
    reactivateMut.mutate(d.id, {
      onSuccess: () => notify(strings.common.reactivated, "success"),
      onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
    });
  }

  return (
    <section id="soferi" className="mt-10 scroll-mt-20">
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
                <TH>{t.identification}</TH>
                <TH>{t.vehicle}</TH>
                <TH>{strings.common.status}</TH>
                {canManage && <TH sticky="right" className="text-right">{strings.common.actions}</TH>}
              </TR>
            </THead>
            <TBody>
              {(isLoading || view.visible.length === 0) && (
                <TableFallbackRow
                  columns={canManage ? 5 : 4}
                  loading={isLoading}
                  icon={UserCircle}
                  title={view.emptiedBySearch ? strings.common.noResults : t.empty}
                  description={
                    view.emptiedBySearch ? strings.common.noResultsHint : t.emptyHint
                  }
                />
              )}
              {view.visible.map((d) => (
                <TR key={d.id}>
                  <TD className="font-medium text-content">{d.name}</TD>
                  <TD>{d.identification || "—"}</TD>
                  <TD>{d.vehicleRegistration || "—"}</TD>
                  <TD>
                    {d.active ? (
                      <Badge variant="success">{t.active}</Badge>
                    ) : (
                      <Badge variant="muted">{t.inactive}</Badge>
                    )}
                  </TD>
                  {canManage && (
                    <TD sticky="right" className="text-right">
                      <div className="flex justify-end gap-1">
                        <Button variant="ghost" size="sm" onClick={() => openEdit(d)}>
                          <Pencil className="mr-1 h-3.5 w-3.5" />
                          {strings.common.edit}
                        </Button>
                        {d.active ? (
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-red-600 hover:bg-red-50"
                            onClick={() => handleDeactivate(d)}
                          >
                            <Ban className="mr-1 h-3.5 w-3.5" />
                            {t.deactivate}
                          </Button>
                        ) : (
                          <Button variant="ghost" size="sm" onClick={() => reactivate(d)}>
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
            <Button type="submit" form="own-driver-form" disabled={isSubmitting}>
              {isSubmitting ? strings.common.saving : strings.common.save}
            </Button>
          </>
        }
      >
        <form id="own-driver-form" onSubmit={handleSubmit} className="space-y-4">
          <div>
            <Label htmlFor="d-name">{t.name}</Label>
            <Input
              id="d-name"
              value={name}
              onChange={(e) => {
                setName(e.target.value);
                if (nameError) setNameError(false);
              }}
              placeholder={t.namePlaceholder}
              autoFocus
            />
            {nameError && <p className="mt-1 text-xs text-red-600">{strings.common.requiredField}</p>}
          </div>
          <div>
            <Label htmlFor="d-identification">{t.identification}</Label>
            <Input
              id="d-identification"
              value={identification}
              onChange={(e) => setIdentification(e.target.value)}
              placeholder={t.identificationPlaceholder}
            />
            <p className="mt-1 text-xs text-content-muted">{t.identificationHint}</p>
          </div>
          <div>
            <Label htmlFor="d-vehicle">{t.vehicle}</Label>
            <Input
              id="d-vehicle"
              value={vehicleRegistration}
              onChange={(e) => setVehicleRegistration(e.target.value)}
              placeholder={t.vehiclePlaceholder}
            />
            <p className="mt-1 text-xs text-content-muted">{t.vehicleHint}</p>
          </div>
        </form>
      </Dialog>

      {confirmDialog}
    </section>
  );
}
