import { useState, type FormEvent } from "react";
import { Plus, Pencil, Ban } from "lucide-react";
import {
  useDrivers,
  useCreateDriver,
  useUpdateDriver,
  useDeactivateDriver,
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
import { useToast } from "@/components/ui/toast";

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
  const { notify } = useToast();

  const drivers = (allDrivers ?? []).filter((d) => d.partnerId === null);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Driver | null>(null);
  const [name, setName] = useState("");
  const [identification, setIdentification] = useState("");
  const [vehicleRegistration, setVehicleRegistration] = useState("");
  const [nameError, setNameError] = useState(false);

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
    if (!window.confirm(t.confirmDeactivate)) return;
    deactivateMut.mutate(d.id, {
      onSuccess: () => notify(t.deactivated, "success"),
      onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
    });
  }

  return (
    <section className="mt-10">
      <div className="mb-3 flex items-start justify-between">
        <div>
          <h2 className="text-lg font-semibold text-gray-900">{t.title}</h2>
          <p className="mt-1 max-w-3xl text-sm text-gray-500">{t.subtitle}</p>
        </div>
        {canManage && (
          <Button onClick={openCreate}>
            <Plus className="mr-2 h-4 w-4" />
            {t.add}
          </Button>
        )}
      </div>

      {isLoading && <p className="text-sm text-gray-500">{strings.common.loading}</p>}
      {isError && <p className="text-sm text-red-600">{t.loadError}</p>}

      {!isLoading && !isError && (
        <Table>
          <THead>
            <TR>
              <TH>{t.name}</TH>
              <TH>{t.identification}</TH>
              <TH>{t.vehicle}</TH>
              <TH>{strings.common.status}</TH>
              {canManage && <TH className="text-right">{strings.common.actions}</TH>}
            </TR>
          </THead>
          <TBody>
            {drivers.length === 0 && (
              <TR>
                <TD colSpan={canManage ? 5 : 4} className="text-center text-gray-400">
                  {t.empty}
                </TD>
              </TR>
            )}
            {drivers.map((d) => (
              <TR key={d.id}>
                <TD className="font-medium text-gray-900">{d.name}</TD>
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
                  <TD className="text-right">
                    <div className="flex justify-end gap-1">
                      <Button variant="ghost" size="sm" onClick={() => openEdit(d)}>
                        <Pencil className="mr-1 h-3.5 w-3.5" />
                        {strings.common.edit}
                      </Button>
                      {d.active && (
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-red-600 hover:bg-red-50"
                          onClick={() => handleDeactivate(d)}
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
            <p className="mt-1 text-xs text-gray-500">{t.identificationHint}</p>
          </div>
          <div>
            <Label htmlFor="d-vehicle">{t.vehicle}</Label>
            <Input
              id="d-vehicle"
              value={vehicleRegistration}
              onChange={(e) => setVehicleRegistration(e.target.value)}
              placeholder={t.vehiclePlaceholder}
            />
            <p className="mt-1 text-xs text-gray-500">{t.vehicleHint}</p>
          </div>
        </form>
      </Dialog>
    </section>
  );
}
