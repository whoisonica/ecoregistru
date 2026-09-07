import { useState, type FormEvent } from "react";
import { Ban, MapPin, Pencil, Plus } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import {
  useWorkPoints,
  useCreateWorkPoint,
  useUpdateWorkPoint,
  useDeactivateWorkPoint,
} from "@/hooks/useWorkPoints";
import type { WorkPoint } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { useHotkey } from "@/hooks/useHotkey";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Dialog } from "@/components/ui/dialog";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { SortableTH } from "@/components/ui/table";
import { TablePagination, TableToolbar } from "@/components/ui/table-toolbar";
import { useTableView } from "@/hooks/useTableView";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm-dialog";
import { InternalGeneratorsSection } from "@/components/InternalGeneratorsSection";
import { OwnDriversSection } from "@/components/OwnDriversSection";

const t = strings.settings.workPoints;

export function SettingsPage() {
  const { user } = useAuth();
  const canManage = user?.role === "PLATFORM_ADMIN" || user?.role === "ADMIN";

  const { data: workPoints, isLoading, isError } = useWorkPoints();
  const createMut = useCreateWorkPoint();
  const updateMut = useUpdateWorkPoint();
  const deactivateMut = useDeactivateWorkPoint();
  const { notify } = useToast();
  const [confirm, confirmDialog] = useConfirm();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<WorkPoint | null>(null);
  const [name, setName] = useState("");
  const [address, setAddress] = useState("");
  const [nameError, setNameError] = useState(false);

  const view = useTableView(workPoints ?? [], {
    searchText: (wp) => [wp.name, wp.address].filter(Boolean).join(" "),
    comparators: { name: (a, b) => a.name.localeCompare(b.name, "ro") },
  });

  const isSubmitting = createMut.isPending || updateMut.isPending;

  function openCreate() {
    setEditing(null);
    setName("");
    setAddress("");
    setNameError(false);
    setDialogOpen(true);
  }

  function openEdit(wp: WorkPoint) {
    setEditing(wp);
    setName(wp.name);
    setAddress(wp.address ?? "");
    setNameError(false);
    setDialogOpen(true);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!name.trim()) {
      setNameError(true);
      return;
    }
    const input = { name: name.trim(), address: address.trim() || null };
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

  function handleDeactivate(wp: WorkPoint) {
    confirm({
      title: t.confirmDeactivateTitle,
      message: (
        <>
          <strong className="text-content">{wp.name}</strong>
          {wp.address ? ` — ${wp.address}` : ""}. {t.confirmDeactivate}
        </>
      ),
      confirmLabel: t.deactivate,
      tone: "danger",
      onConfirm: () => deactivate(wp),
    });
  }

  function deactivate(wp: WorkPoint) {
    deactivateMut.mutate(wp.id, {
      onSuccess: () => notify(t.deactivated, "success"),
      onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
    });
  }

  // `n` deschide formularul, unde contul are voie. Scurtătura tace pe un cont care
  // n-ar putea salva oricum: o comandă care nu face nimic e mai rea decât una lipsă.
  useHotkey("n", openCreate, { enabled: Boolean(canManage) });

  return (
    <div>
      <PageHeader
        title={strings.settings.title}
        description={t.subtitle}
        actions={
          canManage && (
            <Button onClick={openCreate}>
              <Plus className="mr-2 h-4 w-4" />
              {t.add}
            </Button>
          )
        }
      />

      <section className="mt-6">
        <h2 className="mb-3 text-lg font-semibold text-content">{t.title}</h2>

        {isError && <p className="text-sm text-red-600">{t.loadError}</p>}

        {!isError && (
          <>
            <TableToolbar view={view} placeholder={t.searchPlaceholder} />
            <Table stickyHeader>
              <THead sticky>
                <TR>
                  <SortableTH sortKey="name" sort={view.sort} onSort={view.toggleSort}>
                    {t.name}
                  </SortableTH>
                  <TH>{t.address}</TH>
                  <TH>{strings.common.status}</TH>
                  {canManage && <TH sticky="right" className="text-right">{strings.common.actions}</TH>}
                </TR>
              </THead>
              <TBody>
                {(isLoading || view.visible.length === 0) && (
                  <TableFallbackRow
                    columns={canManage ? 4 : 3}
                    loading={isLoading}
                    icon={MapPin}
                    title={view.emptiedBySearch ? strings.common.noResults : t.empty}
                    description={
                      view.emptiedBySearch ? strings.common.noResultsHint : t.emptyHint
                    }
                    action={
                      canManage && (
                        <Button onClick={openCreate}>
                          <Plus className="mr-2 h-4 w-4" />
                          {t.add}
                        </Button>
                      )
                    }
                  />
                )}
                {view.visible.map((wp) => (
                  <TR key={wp.id}>
                    <TD className="font-medium text-content">{wp.name}</TD>
                    <TD>{wp.address || "—"}</TD>
                    <TD>
                      {wp.active ? (
                        <Badge variant="success">{t.active}</Badge>
                      ) : (
                        <Badge variant="muted">{t.inactive}</Badge>
                      )}
                    </TD>
                    {canManage && (
                      <TD sticky="right" className="text-right">
                        <div className="flex justify-end gap-1">
                          <Button variant="ghost" size="sm" onClick={() => openEdit(wp)}>
                            <Pencil className="mr-1 h-3.5 w-3.5" />
                            {strings.common.edit}
                          </Button>
                          {wp.active && (
                            <Button
                              variant="ghost"
                              size="sm"
                              className="text-red-600 hover:bg-red-50"
                              onClick={() => handleDeactivate(wp)}
                            >
                              <Ban className="mr-1 h-3.5 w-3.5" />
                              {t.deactivate}
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
      </section>

      <InternalGeneratorsSection workPoints={workPoints ?? []} canManage={canManage} />

      <OwnDriversSection canManage={canManage} />

      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        title={editing ? t.edit : t.add}
        footer={
          <>
            <Button variant="outline" onClick={() => setDialogOpen(false)} disabled={isSubmitting}>
              {strings.common.cancel}
            </Button>
            <Button type="submit" form="work-point-form" disabled={isSubmitting}>
              {isSubmitting ? strings.common.saving : strings.common.save}
            </Button>
          </>
        }
      >
        <form id="work-point-form" onSubmit={handleSubmit} className="space-y-4">
          <div>
            <Label htmlFor="wp-name">{t.name}</Label>
            <Input
              id="wp-name"
              value={name}
              onChange={(e) => {
                setName(e.target.value);
                if (nameError) setNameError(false);
              }}
              autoFocus
            />
            {nameError && <p className="mt-1 text-xs text-red-600">{strings.common.requiredField}</p>}
          </div>
          <div>
            <Label htmlFor="wp-address">{t.address}</Label>
            <Textarea
              id="wp-address"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              rows={2}
            />
          </div>
        </form>
      </Dialog>

      {confirmDialog}
    </div>
  );
}
