import { useState, type FormEvent } from "react";
import { Ban, Factory, Pencil, Plus } from "lucide-react";
import {
  useInternalGenerators,
  useCreateInternalGenerator,
  useUpdateInternalGenerator,
  useDeactivateInternalGenerator,
} from "@/hooks/useInternalGenerators";
import type { InternalGenerator, WorkPoint } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Select } from "@/components/ui/select";
import { Dialog } from "@/components/ui/dialog";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm-dialog";

const t = strings.settings.internalGenerators;

/**
 * Internal generators — the third location level: company address, work point address, and then
 * the section inside the work point that actually produces the waste. It has no address of its
 * own, which is why the form asks for a name and a description and nothing else; the name is what
 * Anexa 1 cap. 2 prints in "Secţia".
 *
 * <p>The work point is fixed once chosen. Moving a section elsewhere would rewrite the "Secţia"
 * column of sheets already printed for the old work point, so the backend refuses it and the edit
 * form shows the field disabled with the reason.
 */
export function InternalGeneratorsSection({
  workPoints,
  canManage,
}: {
  workPoints: WorkPoint[];
  canManage: boolean;
}) {
  const { data: generators, isLoading, isError } = useInternalGenerators();
  const createMut = useCreateInternalGenerator();
  const updateMut = useUpdateInternalGenerator();
  const deactivateMut = useDeactivateInternalGenerator();
  const { notify } = useToast();
  const [confirm, confirmDialog] = useConfirm();

  const activeWorkPoints = workPoints.filter((w) => w.active);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<InternalGenerator | null>(null);
  const [workPointId, setWorkPointId] = useState("");
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [nameError, setNameError] = useState(false);

  const isSubmitting = createMut.isPending || updateMut.isPending;

  function openCreate() {
    setEditing(null);
    setWorkPointId(activeWorkPoints[0]?.id ?? "");
    setName("");
    setDescription("");
    setNameError(false);
    setDialogOpen(true);
  }

  function openEdit(g: InternalGenerator) {
    setEditing(g);
    setWorkPointId(g.workPointId);
    setName(g.name);
    setDescription(g.description ?? "");
    setNameError(false);
    setDialogOpen(true);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!name.trim() || !workPointId) {
      setNameError(true);
      return;
    }
    const input = {
      workPointId,
      name: name.trim(),
      description: description.trim() || null,
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

  function handleDeactivate(g: InternalGenerator) {
    confirm({
      title: t.confirmDeactivateTitle,
      message: (
        <>
          <strong className="text-content">{g.name}</strong>
          {g.workPointName ? ` — ${g.workPointName}` : ""}. {t.confirmDeactivate}
        </>
      ),
      confirmLabel: t.deactivate,
      tone: "danger",
      onConfirm: () => deactivate(g),
    });
  }

  function deactivate(g: InternalGenerator) {
    deactivateMut.mutate(g.id, {
      onSuccess: () => notify(t.deactivated, "success"),
      onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
    });
  }

  return (
    <section className="mt-10">
      <div className="mb-3 flex items-start justify-between">
        <div>
          <h2 className="text-lg font-semibold text-content">{t.title}</h2>
          <p className="mt-1 max-w-3xl text-sm text-content-muted">{t.subtitle}</p>
        </div>
        {canManage && (
          <Button onClick={openCreate} disabled={activeWorkPoints.length === 0}>
            <Plus className="mr-2 h-4 w-4" />
            {t.add}
          </Button>
        )}
      </div>

      {canManage && activeWorkPoints.length === 0 && (
        <p className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
          {t.noWorkPoints}
        </p>
      )}

      {isError && <p className="text-sm text-red-600">{t.loadError}</p>}

      {!isError && (
        <Table>
          <THead>
            <TR>
              <TH>{t.name}</TH>
              <TH>{t.workPoint}</TH>
              <TH>{t.description}</TH>
              <TH>{strings.common.status}</TH>
              {canManage && <TH className="text-right">{strings.common.actions}</TH>}
            </TR>
          </THead>
          <TBody>
            {(isLoading || (generators ?? []).length === 0) && (
              <TableFallbackRow
                columns={canManage ? 5 : 4}
                loading={isLoading}
                icon={Factory}
                title={t.empty}
                description={t.emptyHint}
              />
            )}
            {(generators ?? []).map((g) => (
              <TR key={g.id}>
                <TD className="font-medium text-content">{g.name}</TD>
                <TD>{g.workPointName}</TD>
                <TD className="max-w-xs truncate text-content-muted">{g.description || "—"}</TD>
                <TD>
                  {g.active ? (
                    <Badge variant="success">{t.active}</Badge>
                  ) : (
                    <Badge variant="muted">{t.inactive}</Badge>
                  )}
                </TD>
                {canManage && (
                  <TD className="text-right">
                    <div className="flex justify-end gap-1">
                      <Button variant="ghost" size="sm" onClick={() => openEdit(g)}>
                        <Pencil className="mr-1 h-3.5 w-3.5" />
                        {strings.common.edit}
                      </Button>
                      {g.active && (
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-red-600 hover:bg-red-50"
                          onClick={() => handleDeactivate(g)}
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

      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        title={editing ? t.editTitle : t.addTitle}
        footer={
          <>
            <Button variant="outline" onClick={() => setDialogOpen(false)} disabled={isSubmitting}>
              {strings.common.cancel}
            </Button>
            <Button type="submit" form="internal-generator-form" disabled={isSubmitting}>
              {isSubmitting ? strings.common.saving : strings.common.save}
            </Button>
          </>
        }
      >
        <form id="internal-generator-form" onSubmit={handleSubmit} className="space-y-4">
          <div>
            <Label htmlFor="ig-wp">{t.workPoint}</Label>
            <Select
              id="ig-wp"
              value={workPointId}
              onChange={(e) => setWorkPointId(e.target.value)}
              disabled={Boolean(editing)}
            >
              {activeWorkPoints.map((w) => (
                <option key={w.id} value={w.id}>
                  {w.name}
                </option>
              ))}
            </Select>
            {editing && <p className="mt-1 text-xs text-content-muted">{t.workPointLocked}</p>}
          </div>
          <div>
            <Label htmlFor="ig-name">{t.name}</Label>
            <Input
              id="ig-name"
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
            <Label htmlFor="ig-description">{t.description}</Label>
            <Textarea
              id="ig-description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={2}
            />
          </div>
        </form>
      </Dialog>

      {confirmDialog}
    </section>
  );
}
