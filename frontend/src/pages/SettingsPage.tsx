import { useState, type FormEvent } from "react";
import { Plus, Pencil, Ban } from "lucide-react";
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
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Dialog } from "@/components/ui/dialog";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { useToast } from "@/components/ui/toast";
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

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<WorkPoint | null>(null);
  const [name, setName] = useState("");
  const [address, setAddress] = useState("");
  const [nameError, setNameError] = useState(false);

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
    if (!window.confirm(t.confirmDeactivate)) return;
    deactivateMut.mutate(wp.id, {
      onSuccess: () => notify(t.deactivated, "success"),
      onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
    });
  }

  return (
    <div>
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">{strings.settings.title}</h1>
          <p className="mt-1 text-sm text-gray-500">{t.subtitle}</p>
        </div>
        {canManage && (
          <Button onClick={openCreate}>
            <Plus className="mr-2 h-4 w-4" />
            {t.add}
          </Button>
        )}
      </div>

      <section className="mt-6">
        <h2 className="mb-3 text-lg font-semibold text-gray-900">{t.title}</h2>

        {isLoading && <p className="text-sm text-gray-500">{strings.common.loading}</p>}
        {isError && <p className="text-sm text-red-600">{t.loadError}</p>}

        {!isLoading && !isError && (
          <Table>
            <THead>
              <TR>
                <TH>{t.name}</TH>
                <TH>{t.address}</TH>
                <TH>{strings.common.status}</TH>
                {canManage && <TH className="text-right">{strings.common.actions}</TH>}
              </TR>
            </THead>
            <TBody>
              {(workPoints ?? []).length === 0 && (
                <TR>
                  <TD colSpan={canManage ? 4 : 3} className="text-center text-gray-400">
                    {t.empty}
                  </TD>
                </TR>
              )}
              {(workPoints ?? []).map((wp) => (
                <TR key={wp.id}>
                  <TD className="font-medium text-gray-900">{wp.name}</TD>
                  <TD>{wp.address || "—"}</TD>
                  <TD>
                    {wp.active ? (
                      <Badge variant="success">{t.active}</Badge>
                    ) : (
                      <Badge variant="muted">{t.inactive}</Badge>
                    )}
                  </TD>
                  {canManage && (
                    <TD className="text-right">
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
    </div>
  );
}
