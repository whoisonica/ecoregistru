import { useState, type FormEvent } from "react";
import { Ban, Pencil, Plus, RotateCcw, Trash2, Truck } from "lucide-react";
import {
  useVehicles,
  useCreateVehicle,
  useUpdateVehicle,
  useDeactivateVehicle,
  useReactivateVehicle,
  useDeleteVehicle,
} from "@/hooks/useVehicles";
import { usePartners } from "@/hooks/usePartners";
import type { Vehicle, WorkPoint } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { formatDate } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { DateInput } from "@/components/ui/date-input";
import { Select } from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Dialog } from "@/components/ui/dialog";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { SortableTH } from "@/components/ui/table";
import { TablePagination, TableToolbar } from "@/components/ui/table-toolbar";
import { useTableView } from "@/hooks/useTableView";
import { useActiveFilter } from "@/components/ui/active-filter";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm-dialog";

const t = strings.settings.vehicles;

/** Aceeași fereastră ca mailul (`VehicleExpiryAlertScheduler.WARNING_WINDOW_DAYS`). */
const WARNING_DAYS = 30;

function daysFromToday(iso: string): number {
  const today = new Date(new Date().toDateString());
  return Math.round((new Date(`${iso.slice(0, 10)}T00:00:00`).getTime() - today.getTime()) / 86_400_000);
}

/** Câte un rând pe act (ITP, licență), cu starea lui: expirat, expiră în 30 de zile, valabil. */
function DocumentBadges({ vehicle }: { vehicle: Vehicle }) {
  const docs = [
    { label: "ITP", date: vehicle.itpExpiry },
    { label: "Licență", date: vehicle.transportLicenseExpiry },
  ].filter((d): d is { label: string; date: string } => Boolean(d.date));
  if (docs.length === 0) return <span className="text-content-subtle">{t.noDocuments}</span>;
  return (
    <div className="flex flex-col gap-0.5">
      {docs.map(({ label, date }) => {
        const days = daysFromToday(date);
        const text = `${label} · ${formatDate(date)}`;
        if (days < 0) return <Badge key={label} variant="danger">{`${t.expired} · ${text}`}</Badge>;
        if (days <= WARNING_DAYS) return <Badge key={label} variant="warning">{`${t.expiresSoon} · ${text}`}</Badge>;
        return <Badge key={label} variant="success">{text}</Badge>;
      })}
    </div>
  );
}

/**
 * Flota (D2.1). Vehiculul ales la cântar trece numărul lui pe operațiune; operațiunea îl păstrează ca
 * text, deci ștergerea fișei nu schimbă un document făcut. O scrie oricine scrie în firmă, ca la
 * sortimente; serverul are același prag.
 */
export function VehiclesSection({ workPoints, canManage }: { workPoints: WorkPoint[]; canManage: boolean }) {
  const { data: vehicles, isLoading, isError } = useVehicles();
  const partners = usePartners();
  const createMut = useCreateVehicle();
  const updateMut = useUpdateVehicle();
  const deactivateMut = useDeactivateVehicle();
  const reactivateMut = useReactivateVehicle();
  const deleteMut = useDeleteVehicle();
  const { notify } = useToast();
  const [confirm, confirmDialog] = useConfirm();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Vehicle | null>(null);
  const [registration, setRegistration] = useState("");
  const [kind, setKind] = useState("");
  const [tare, setTare] = useState("");
  const [heavy, setHeavy] = useState(false);
  const [itpExpiry, setItpExpiry] = useState("");
  const [licenseNumber, setLicenseNumber] = useState("");
  const [licenseExpiry, setLicenseExpiry] = useState("");
  const [homeWorkPointId, setHomeWorkPointId] = useState("");
  const [partnerId, setPartnerId] = useState("");
  const [registrationError, setRegistrationError] = useState(false);

  const { rows, control: activeFilter } = useActiveFilter(vehicles ?? []);
  const view = useTableView(rows, {
    searchText: (v) => [v.registration, v.kind, v.homeWorkPointName, v.partnerName].filter(Boolean).join(" "),
    comparators: { registration: (a, b) => a.registration.localeCompare(b.registration, "ro") },
  });

  const carriers = (partners.data ?? []).filter((p) => (p.carrier && p.active) || p.id === partnerId);
  const depots = workPoints.filter((w) => w.active || w.id === homeWorkPointId);
  const isSubmitting = createMut.isPending || updateMut.isPending;

  function openWith(v: Vehicle | null) {
    setEditing(v);
    setRegistration(v?.registration ?? "");
    setKind(v?.kind ?? "");
    setTare(v?.standardTareKg == null ? "" : String(v.standardTareKg));
    setHeavy(v?.heavy ?? false);
    setItpExpiry(v?.itpExpiry ?? "");
    setLicenseNumber(v?.transportLicenseNumber ?? "");
    setLicenseExpiry(v?.transportLicenseExpiry ?? "");
    setHomeWorkPointId(v?.homeWorkPointId ?? "");
    setPartnerId(v?.partnerId ?? "");
    setRegistrationError(false);
    setDialogOpen(true);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!registration.trim()) {
      setRegistrationError(true);
      return;
    }
    const input = {
      registration: registration.trim(),
      kind: kind.trim() || null,
      standardTareKg: tare.trim() ? Number(tare.replace(",", ".")) : null,
      heavy,
      itpExpiry: itpExpiry || null,
      transportLicenseNumber: heavy ? licenseNumber.trim() || null : null,
      transportLicenseExpiry: heavy ? licenseExpiry || null : null,
      homeWorkPointId: homeWorkPointId || null,
      partnerId: partnerId || null,
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

  function handleDeactivate(v: Vehicle) {
    confirm({
      title: t.confirmDeactivateTitle,
      message: (
        <>
          <strong className="text-content">{v.registration}</strong>. {t.confirmDeactivate}
        </>
      ),
      confirmLabel: t.deactivate,
      tone: "danger",
      onConfirm: () =>
        deactivateMut.mutate(v.id, {
          onSuccess: () => notify(t.deactivated, "success"),
          onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
        }),
    });
  }

  function handleDelete(v: Vehicle) {
    confirm({
      title: t.confirmDeleteTitle,
      message: (
        <>
          <strong className="text-content">{v.registration}</strong>. {t.confirmDelete}
        </>
      ),
      confirmLabel: t.delete,
      tone: "danger",
      onConfirm: () =>
        deleteMut.mutate(v.id, {
          onSuccess: () => notify(t.deleted, "success"),
          onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
        }),
    });
  }

  function reactivate(v: Vehicle) {
    reactivateMut.mutate(v.id, {
      onSuccess: () => notify(strings.common.reactivated, "success"),
      onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
    });
  }

  const columns = canManage ? 6 : 5;

  return (
    <section id="flota" className="mt-10 scroll-mt-20">
      <div className="mb-3 flex items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-content">{t.title}</h2>
          <p className="mt-1 max-w-3xl text-sm text-content-muted">{t.subtitle}</p>
        </div>
        {canManage && (
          <Button onClick={() => openWith(null)}>
            <Plus className="mr-2 h-4 w-4" />
            {t.add}
          </Button>
        )}
      </div>

      {isError && <p className="text-sm text-state-bad-text">{t.loadError}</p>}

      {!isError && (
        <>
          <TableToolbar view={view} placeholder={t.searchPlaceholder}>
            {activeFilter}
          </TableToolbar>
          <Table stickyHeader>
            <THead sticky>
              <TR>
                <SortableTH sortKey="registration" sort={view.sort} onSort={view.toggleSort}>
                  {t.registration}
                </SortableTH>
                <TH>{t.kind}</TH>
                <TH>{t.homeWorkPoint}</TH>
                <TH>{t.documents}</TH>
                <TH>{strings.common.status}</TH>
                {canManage && <TH sticky="right" className="text-right">{strings.common.actions}</TH>}
              </TR>
            </THead>
            <TBody>
              {(isLoading || view.visible.length === 0) && (
                <TableFallbackRow
                  columns={columns}
                  loading={isLoading}
                  icon={Truck}
                  title={view.emptiedBySearch ? strings.common.noResults : t.empty}
                  description={view.emptiedBySearch ? strings.common.noResultsHint : t.emptyHint}
                />
              )}
              {view.visible.map((v) => (
                <TR key={v.id}>
                  <TD className="font-mono font-medium text-content">{v.registration}</TD>
                  <TD>
                    {v.kind || "—"}
                    {v.partnerName && <span className="block text-xs text-content-muted">{v.partnerName}</span>}
                  </TD>
                  <TD>{v.homeWorkPointName || "—"}</TD>
                  <TD>
                    <DocumentBadges vehicle={v} />
                  </TD>
                  <TD>
                    {v.active ? <Badge variant="success">{t.active}</Badge> : <Badge variant="muted">{t.inactive}</Badge>}
                  </TD>
                  {canManage && (
                    <TD sticky="right" className="text-right">
                      <div className="flex justify-end gap-1">
                        <Button variant="ghost" size="sm" onClick={() => openWith(v)}>
                          <Pencil className="mr-1 h-3.5 w-3.5" />
                          {strings.common.edit}
                        </Button>
                        {v.active ? (
                          <Button variant="ghost" size="sm" onClick={() => handleDeactivate(v)}>
                            <Ban className="mr-1 h-3.5 w-3.5" />
                            {t.deactivate}
                          </Button>
                        ) : (
                          <>
                            <Button variant="ghost" size="sm" onClick={() => reactivate(v)}>
                              <RotateCcw className="mr-1 h-3.5 w-3.5" />
                              {strings.common.reactivate}
                            </Button>
                            <Button variant="ghost" size="sm" onClick={() => handleDelete(v)}>
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
            <Button type="submit" form="vehicle-form" disabled={isSubmitting}>
              {isSubmitting ? strings.common.saving : strings.common.save}
            </Button>
          </>
        }
      >
        <form id="vehicle-form" onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <Label htmlFor="v-registration">{t.registration}</Label>
              <Input
                id="v-registration"
                className="font-mono uppercase"
                value={registration}
                onChange={(e) => {
                  setRegistration(e.target.value);
                  if (registrationError) setRegistrationError(false);
                }}
                placeholder={t.registrationPlaceholder}
                autoFocus
              />
              {registrationError ? (
                <p className="mt-1 text-xs text-state-bad-text">{strings.common.requiredField}</p>
              ) : (
                <p className="mt-1 text-xs text-content-muted">{t.registrationHint}</p>
              )}
            </div>
            <div>
              <Label htmlFor="v-kind">{t.kind}</Label>
              <Input id="v-kind" value={kind} onChange={(e) => setKind(e.target.value)} placeholder={t.kindPlaceholder} />
            </div>
            <div>
              <Label htmlFor="v-tare">{t.standardTare}</Label>
              <Input
                id="v-tare"
                inputMode="decimal"
                className="font-mono"
                value={tare}
                onChange={(e) => setTare(e.target.value)}
              />
              <p className="mt-1 text-xs text-content-muted">{t.standardTareHint}</p>
            </div>
            <div>
              <Label htmlFor="v-itp">{t.itpExpiry}</Label>
              <DateInput id="v-itp" value={itpExpiry} onChange={(e) => setItpExpiry(e.target.value)} />
            </div>
          </div>

          <Switch id="v-heavy" checked={heavy} onChange={setHeavy} label={t.heavy} description={t.heavyHint} />
          {heavy && (
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <Label htmlFor="v-license">{t.licenseNumber}</Label>
                <Input id="v-license" value={licenseNumber} onChange={(e) => setLicenseNumber(e.target.value)} />
              </div>
              <div>
                <Label htmlFor="v-license-expiry">{t.licenseExpiry}</Label>
                <DateInput id="v-license-expiry" value={licenseExpiry} onChange={(e) => setLicenseExpiry(e.target.value)} />
              </div>
            </div>
          )}

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <Label htmlFor="v-home">{t.homeWorkPoint}</Label>
              <Select id="v-home" value={homeWorkPointId} onChange={(e) => setHomeWorkPointId(e.target.value)}>
                <option value="">{t.noHomeWorkPoint}</option>
                {depots.map((w) => (
                  <option key={w.id} value={w.id}>
                    {w.name}
                  </option>
                ))}
              </Select>
            </div>
            <div>
              <Label htmlFor="v-carrier">{t.carrier}</Label>
              <Select id="v-carrier" value={partnerId} onChange={(e) => setPartnerId(e.target.value)}>
                <option value="">{t.ownFleet}</option>
                {carriers.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </Select>
              <p className="mt-1 text-xs text-content-muted">{t.carrierHint}</p>
            </div>
          </div>
        </form>
      </Dialog>

      {confirmDialog}
    </section>
  );
}
