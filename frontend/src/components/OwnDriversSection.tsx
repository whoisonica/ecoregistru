import { useState, type FormEvent } from "react";
import { SETTINGS_CARD } from "@/components/ui/card";
import { Ban, UserCircle, Pencil, Plus, RotateCcw, Trash2 } from "lucide-react";
import {
  useDrivers,
  useCreateDriver,
  useUpdateDriver,
  useDeactivateDriver,
  useReactivateDriver,
  useDeleteDriver,
} from "@/hooks/useDrivers";
import { useVehicles } from "@/hooks/useVehicles";
import type { Driver, WorkPoint } from "@/lib/types";
import { formatDate } from "@/lib/utils";
import { DateInput } from "@/components/ui/date-input";
import { Select } from "@/components/ui/select";
import { WARNING_DAYS, daysFromToday } from "@/components/VehiclesSection";
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

/** D2.2 — atestatul cu starea lui, ca actele unui vehicul: expirat, expiră în 30 de zile, valabil. */
function AttestationBadge({ driver }: { driver: Driver }) {
  if (!driver.attestationExpiry) return <>{driver.attestationNumber || t.noAttestation}</>;
  const days = daysFromToday(driver.attestationExpiry);
  const text = formatDate(driver.attestationExpiry);
  if (days < 0) return <Badge variant="danger">{`${t.attestationExpired} · ${text}`}</Badge>;
  if (days <= WARNING_DAYS) return <Badge variant="warning">{`${t.attestationExpiresSoon} · ${text}`}</Badge>;
  return <Badge variant="success">{text}</Badge>;
}

/**
 * Șoferii firmei — cazul „— transportăm noi —" de pe formularul de mișcare.
 *
 * <p>Aici sunt doar ai noștri. Șoferii unui transportator se editează în fișa lui, din Parteneri,
 * unde lista se înlocuiește la salvare; două drumuri de scriere către aceleași rânduri ar însemna
 * că un șofer adăugat de aici dispare data viitoare când cineva deschide și salvează partenerul.
 * Ecranul îi și ascunde, ca lista să fie ce zice titlul.
 */
export function OwnDriversSection({
  canManage,
  workPoints,
  hasDepot,
}: {
  canManage: boolean;
  workPoints: WorkPoint[];
  /** Depozitul implicit și flota au sens doar la firmele cu registrul art. 48 (D2.2). */
  hasDepot: boolean;
}) {
  const { data: allDrivers, isLoading, isError } = useDrivers();
  const vehicles = useVehicles();
  const createMut = useCreateDriver();
  const updateMut = useUpdateDriver();
  const deactivateMut = useDeactivateDriver();
  const reactivateMut = useReactivateDriver();
  const deleteMut = useDeleteDriver();
  const { notify } = useToast();
  const [confirm, confirmDialog] = useConfirm();

  const drivers = (allDrivers ?? []).filter((d) => d.partnerId === null);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Driver | null>(null);
  const [name, setName] = useState("");
  const [identification, setIdentification] = useState("");
  const [cnp, setCnp] = useState("");
  const [vehicleRegistration, setVehicleRegistration] = useState("");
  const [homeWorkPointId, setHomeWorkPointId] = useState("");
  const [attestationNumber, setAttestationNumber] = useState("");
  const [attestationExpiry, setAttestationExpiry] = useState("");
  const [nameError, setNameError] = useState(false);

  const { rows: visibleDrivers, control: activeFilter } = useActiveFilter(drivers);
  const view = useTableView(visibleDrivers, {
    searchText: (d) =>
      [d.name, d.identification, d.vehicleRegistration, d.homeWorkPointName, d.attestationNumber]
        .filter(Boolean)
        .join(" "),
    comparators: { name: (a, b) => a.name.localeCompare(b.name, "ro") },
  });

  const isSubmitting = createMut.isPending || updateMut.isPending;
  const depots = workPoints.filter((w) => w.active || w.id === homeWorkPointId);

  function openCreate() {
    setEditing(null);
    setName("");
    setIdentification("");
    setCnp("");
    setVehicleRegistration("");
    setHomeWorkPointId("");
    setAttestationNumber("");
    setAttestationExpiry("");
    setNameError(false);
    setDialogOpen(true);
  }

  function openEdit(d: Driver) {
    setEditing(d);
    setName(d.name);
    setIdentification(d.identification ?? "");
    setCnp(d.cnp ?? "");
    setVehicleRegistration(d.vehicleRegistration ?? "");
    setHomeWorkPointId(d.homeWorkPointId ?? "");
    setAttestationNumber(d.attestationNumber ?? "");
    setAttestationExpiry(d.attestationExpiry ?? "");
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
      cnp: cnp.trim() || null,
      vehicleRegistration: vehicleRegistration.trim() || null,
      homeWorkPointId: homeWorkPointId || null,
      attestationNumber: attestationNumber.trim() || null,
      attestationExpiry: attestationExpiry || null,
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

  /**
   * AO — ștergerea fișei. Numai după dezactivare, ca un clic greșit să nu fie ireversibil. Mișcările
   * nu se ating: Anexa 3 tipărește instantaneul de atunci până la termenul de păstrare.
   */
  function handleDelete(d: Driver) {
    confirm({
      title: t.confirmDeleteTitle,
      message: (
        <>
          <strong className="text-content">{d.name}</strong>. {t.confirmDelete}
        </>
      ),
      confirmLabel: t.delete,
      tone: "danger",
      onConfirm: () =>
        deleteMut.mutate(d.id, {
          onSuccess: () => notify(t.deleted, "success"),
          onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
        }),
    });
  }

  function reactivate(d: Driver) {
    reactivateMut.mutate(d.id, {
      onSuccess: () => notify(strings.common.reactivated, "success"),
      onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
    });
  }

  return (
    <section id="soferi" className={SETTINGS_CARD}>
      <div className="mb-3 flex items-start justify-between">
        <div>
          <h2 className="text-lg font-semibold text-content">{t.title}</h2>
          <p className="mt-1 max-w-3xl text-sm text-content-muted">{t.subtitle}</p>
          {/* Actul de identitate al șoferului e singurul dat personal al cuiva din afara firmei pe
              care aplicația îl ține — și singurul care se tipărește. Nota spune de ce se ține, cât
              se ține și ce nu șterge dezactivarea. Vezi `strings.common.driversPrivacy`. */}
          <p className="mt-1 max-w-3xl text-xs text-content-subtle">{strings.common.driversPrivacy}</p>
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
                {hasDepot && <TH>{t.homeWorkPoint}</TH>}
                <TH>{t.attestation}</TH>
                <TH>{strings.common.status}</TH>
                {canManage && <TH sticky="right" className="text-right">{strings.common.actions}</TH>}
              </TR>
            </THead>
            <TBody>
              {(isLoading || view.visible.length === 0) && (
                <TableFallbackRow
                  columns={(canManage ? 6 : 5) + (hasDepot ? 1 : 0)}
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
                  {hasDepot && <TD>{d.homeWorkPointName || "—"}</TD>}
                  <TD>
                    <AttestationBadge driver={d} />
                  </TD>
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
                          <>
                            <Button variant="ghost" size="sm" onClick={() => reactivate(d)}>
                              <RotateCcw className="mr-1 h-3.5 w-3.5" />
                              {strings.common.reactivate}
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="text-red-600 hover:bg-red-50"
                              onClick={() => handleDelete(d)}
                            >
                              <Trash2 className="mr-1 h-3.5 w-3.5" />
                              {t.delete}
                            </Button>
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
              maxLength={255}
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
              maxLength={100}
              value={identification}
              onChange={(e) => setIdentification(e.target.value)}
              placeholder={t.identificationPlaceholder}
            />
            <p className="mt-1 text-xs text-content-muted">{t.identificationHint}</p>
          </div>
          <div>
            <Label htmlFor="d-cnp">{strings.common.cnp}</Label>
            <Input
              id="d-cnp"
              inputMode="numeric"
              maxLength={13}
              value={cnp}
              onChange={(e) => setCnp(e.target.value)}
            />
            <p className="mt-1 text-xs text-content-muted">{strings.common.cnpHint}</p>
          </div>
          <div>
            <Label htmlFor="d-vehicle">{t.vehicle}</Label>
            <Input
              id="d-vehicle"
              maxLength={50}
              list={hasDepot ? "d-fleet" : undefined}
              value={vehicleRegistration}
              onChange={(e) => setVehicleRegistration(e.target.value)}
              placeholder={t.vehiclePlaceholder}
            />
            {/* D2.2 — vehiculul implicit se alege din flotă; la cântar numărul lui e recunoscut (D2.1). */}
            {hasDepot && (
              <datalist id="d-fleet">
                {(vehicles.data ?? [])
                  .filter((v) => v.active)
                  .map((v) => (
                    <option key={v.id} value={v.registration}>
                      {v.kind ?? ""}
                    </option>
                  ))}
              </datalist>
            )}
            <p className="mt-1 text-xs text-content-muted">{t.vehicleHint}</p>
          </div>
          {hasDepot && (
            <div>
              <Label htmlFor="d-home">{t.homeWorkPoint}</Label>
              <Select id="d-home" value={homeWorkPointId} onChange={(e) => setHomeWorkPointId(e.target.value)}>
                <option value="">{t.noHomeWorkPoint}</option>
                {depots.map((w) => (
                  <option key={w.id} value={w.id}>
                    {w.name}
                  </option>
                ))}
              </Select>
            </div>
          )}
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <Label htmlFor="d-attestation">{t.attestationNumber}</Label>
              <Input
                id="d-attestation"
                maxLength={100}
                value={attestationNumber}
                onChange={(e) => setAttestationNumber(e.target.value)}
                placeholder={t.attestationNumberPlaceholder}
              />
            </div>
            <div>
              <Label htmlFor="d-attestation-expiry">{t.attestationExpiry}</Label>
              <DateInput
                id="d-attestation-expiry"
                value={attestationExpiry}
                onChange={(e) => setAttestationExpiry(e.target.value)}
              />
            </div>
          </div>
          <p className="-mt-2 text-xs text-content-muted">{t.attestationHint}</p>
        </form>
      </Dialog>

      {confirmDialog}
    </section>
  );
}
