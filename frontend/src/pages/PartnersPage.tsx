import { useMemo, useState, type FormEvent } from "react";
import { Ban, Pencil, Plus, RotateCcw, Users } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import {
  usePartners,
  useCreatePartner,
  useUpdatePartner,
  useDeactivatePartner,
  useReactivatePartner,
} from "@/hooks/usePartners";
import type {
  DriverInput,
  Partner,
  PackagingOrigin,
  PartnerInput,
  PartnerType,
  PartnerWorkPointInput,
} from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { useHotkey } from "@/hooks/useHotkey";
import { useUrlState } from "@/hooks/useUrlState";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Select } from "@/components/ui/select";
import { DateInput } from "@/components/ui/date-input";
import { Dialog } from "@/components/ui/dialog";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { SortableTH } from "@/components/ui/table";
import { TablePagination, TableToolbar } from "@/components/ui/table-toolbar";
import { missingLast, useTableView } from "@/hooks/useTableView";
import { useActiveFilter } from "@/components/ui/active-filter";
import { fold, formatDate } from "@/lib/utils";
import { TableFallbackRow } from "@/components/ui/table-fallback";
import { useToast } from "@/components/ui/toast";
import { useConfirm } from "@/components/ui/confirm-dialog";
import { PartnerRoleBadge } from "@/components/PartnerRoleBadge";

const t = strings.partners;
const typeLabels = strings.enums.partnerType;
const roleLabels = strings.enums.partnerRole;
const PARTNER_TYPES: PartnerType[] = ["COLLECTOR", "RECOVERER", "GENERATOR"];

/**
 * Filter values. The first four are the commercial role — "none" surfaces the partners still to be
 * classified — and "carrier" is a different axis riding in the same select, because it is one value
 * and a second dropdown for it would cost more than it explains.
 */
type RoleFilter = "" | "client" | "supplier" | "none" | "carrier";

/** Formats an authorization expiry as a status badge, mirroring backend `expiringSoon`. */
function ExpiryBadge({ partner }: { partner: Partner }) {
  if (!partner.authorizationExpiry) {
    return <span className="text-content-subtle">{t.noAuthorization}</span>;
  }
  const date = partner.authorizationExpiry;
  const isExpired = new Date(date) < new Date(new Date().toDateString());
  if (isExpired) {
    return <Badge variant="danger">{t.expired}</Badge>;
  }
  // Data se scrie cum se scrie în România. Aici rămăsese ISO brut — `2026-11-15` lângă
  // `15.11.2026` pe Mișcări și pe Termene, în același produs.
  if (partner.expiringSoon) {
    return <Badge variant="warning">{`${t.expiringSoon} · ${formatDate(date)}`}</Badge>;
  }
  return <Badge variant="success">{formatDate(date)}</Badge>;
}

export function PartnersPage() {
  const { user } = useAuth();
  const canManage =
    user?.role === "PLATFORM_ADMIN" || user?.role === "ADMIN" || user?.role === "OPERATOR";

  const { data: partners, isLoading, isError } = usePartners();
  const createMut = useCreatePartner();
  const updateMut = useUpdatePartner();
  const deactivateMut = useDeactivatePartner();
  const reactivateMut = useReactivatePartner();
  const { notify } = useToast();
  const [confirm, confirmDialog] = useConfirm();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Partner | null>(null);
  const [name, setName] = useState("");
  const [cui, setCui] = useState("");
  const [authorizationNumber, setAuthorizationNumber] = useState("");
  const [authorizationExpiry, setAuthorizationExpiry] = useState("");
  // "" = „doar transportator": o firmă de transport pură nu face nimic cu deșeul, deci n-are tip.
  const [type, setType] = useState<PartnerType | "">("COLLECTOR");
  const [isClient, setIsClient] = useState(false);
  const [isSupplier, setIsSupplier] = useState(true);
  const [address, setAddress] = useState("");
  const [workPoints, setWorkPoints] = useState<PartnerWorkPointInput[]>([]);
  const [isCarrier, setIsCarrier] = useState(false);
  // Provenienţa pentru Anexa 3 Ambalaje. Stă aici, nu pe mişcare, fiindcă nota 2 a anexei
  // descrie sursa, nu transportul: un colector de la care cumperi e colector la fiecare
  // transport. „Populaţie" lipseşte dinadins din listă — o persoană fizică nu e partener.
  const [packagingOrigin, setPackagingOrigin] = useState<"" | PackagingOrigin>("");
  const [drivers, setDrivers] = useState<DriverInput[]>([]);
  const [tradeRegisterNumber, setTradeRegisterNumber] = useState("");
  const [transportLicenseNumber, setTransportLicenseNumber] = useState("");
  const [transportLicenseExpiry, setTransportLicenseExpiry] = useState("");
  const [nameError, setNameError] = useState(false);
  const [roleError, setRoleError] = useState(false);
  const [typeError, setTypeError] = useState(false);
  const [roleFilterRaw, setRoleFilter] = useUrlState("rol");
  const roleFilter = roleFilterRaw as RoleFilter;

  /**
   * Ce parteneri are deja firma, potriviti pe ce s-a tastat. Doua litere e pragul cerut pe
   * 24.08.2026: "cand adaugi partener si scrii sa apara din db ce clienti sunt dupa primele 2
   * litere". Se cauta si la inceput, si in interiorul numelui, fiindca "SC Retim SA" se cauta la
   * fel de des dupa "re" ca dupa "sc".
   *
   * Nu e un apel nou: lista partenerilor e deja incarcata pentru tabel, si e a tenantului. La
   * editare nu se sugereaza nimic — partenerul exista deja, iar propriul nume nu e un duplicat.
   */
  // `fold` aici nu e doar comoditate, ca la căutarea din tabel: sugestia asta există ca să nu se
  // creeze un partener de două ori. Cine tastează „deseuri" nu vedea „Transport Deșeuri SRL", deci
  // îl adăuga încă o dată — iar duplicatul rămâne în nomenclator şi pe documentele tipărite.
  const nameSuggestions =
    editing || name.trim().length < 2
      ? []
      : (partners ?? [])
          .filter((p) => fold(p.name).includes(fold(name.trim())))
          .slice(0, 5);

  const filteredByRole = useMemo(
    () =>
      (partners ?? []).filter((p) => {
        if (roleFilter === "client") return p.client;
        if (roleFilter === "supplier") return p.supplier;
        if (roleFilter === "none") return !p.client && !p.supplier;
        if (roleFilter === "carrier") return p.carrier;
        return true;
      }),
    [partners, roleFilter]
  );

  /**
   * Filtrul de rol restrânge, apoi vederea caută, sortează și paginează ce a rămas. Ordinea
   * contează: căutarea peste tot, urmată de filtru, ar arăta un număr de potriviri din care o
   * parte nici nu se vede.
   */
  // Rolul restrânge, apoi starea: „furnizori activi" e întrebarea obișnuită, iar cimitirul de
  // parteneri scoși din uz nu trebuie să stea în calea ei.
  const { rows: activePartners, control: activeFilter } = useActiveFilter(filteredByRole);
  const view = useTableView(activePartners, {
    searchText: (p) =>
      [p.name, p.cui, p.authorizationNumber, p.address].filter(Boolean).join(" "),
    comparators: {
      name: (a, b) => a.name.localeCompare(b.name, "ro"),
      cui: (a, b) => (a.cui ?? "").localeCompare(b.cui ?? "", "ro"),
      // Autorizația fără dată stă la coadă, în ambele sensuri: „nu se știe" nu e nici devreme,
      // nici târziu. `|| null` păstrează înțelesul de dinainte, în care și șirul gol e o lipsă.
      authorizationExpiry: missingLast(
        (p) => p.authorizationExpiry || null,
        (x, y) => x.localeCompare(y)
      ),
    },
    initialSort: { key: "name", direction: "asc" },
  });
  const visiblePartners = view.visible;

  const isSubmitting = createMut.isPending || updateMut.isPending;

  function openCreate() {
    setEditing(null);
    setName("");
    setCui("");
    setAuthorizationNumber("");
    setAuthorizationExpiry("");
    setType("COLLECTOR");
    setIsClient(false);
    setIsSupplier(true);
    setIsCarrier(false);
    setPackagingOrigin("");
    setDrivers([]);
    setAddress("");
    setWorkPoints([]);
    setTradeRegisterNumber("");
    setTransportLicenseNumber("");
    setTransportLicenseExpiry("");
    setNameError(false);
    setRoleError(false);
    setTypeError(false);
    setDialogOpen(true);
  }

  function openEdit(p: Partner) {
    setEditing(p);
    setName(p.name);
    setCui(p.cui ?? "");
    setAuthorizationNumber(p.authorizationNumber ?? "");
    setAuthorizationExpiry(p.authorizationExpiry ?? "");
    setType(p.type ?? "");
    setIsClient(p.client);
    setIsSupplier(p.supplier);
    setIsCarrier(p.carrier);
    setPackagingOrigin(p.packagingOrigin ?? "");
    setDrivers((p.drivers ?? []).map((d) => ({
      id: d.id,
      name: d.name,
      identification: d.identification ?? "",
      vehicleRegistration: d.vehicleRegistration ?? "",
    })));
    setAddress(p.address ?? "");
    setWorkPoints((p.workPoints ?? []).map((wp) => ({
      id: wp.id,
      name: wp.name ?? "",
      address: wp.address,
    })));
    setTradeRegisterNumber(p.tradeRegisterNumber ?? "");
    setTransportLicenseNumber(p.transportLicenseNumber ?? "");
    setTransportLicenseExpiry(p.transportLicenseExpiry ?? "");
    setNameError(false);
    setRoleError(false);
    setTypeError(false);
    setDialogOpen(true);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!name.trim()) {
      setNameError(true);
      return;
    }
    // Mirrors the backend rule: a row the screen colours by role cannot have none.
    if (!isClient && !isSupplier) {
      setRoleError(true);
      return;
    }
    // Mirrors the backend rule: either they do something with the waste, or they haul it.
    if (!type && !isCarrier) {
      setTypeError(true);
      return;
    }
    const input: PartnerInput = {
      name: name.trim(),
      cui: cui.trim() || null,
      authorizationNumber: authorizationNumber.trim() || null,
      authorizationExpiry: authorizationExpiry || null,
      type: type || null,
      client: isClient,
      supplier: isSupplier,
      carrier: isCarrier,
      packagingOrigin: packagingOrigin || null,
      address: address.trim() || null,
      // Rândurile fără adresă se aruncă: un punct de lucru fără adresă nu e nimic pe Anexa 3.
      workPoints: workPoints
        .filter((wp) => wp.address.trim() !== "")
        .map((wp) => ({ id: wp.id, name: wp.name?.trim() || null, address: wp.address.trim() })),
      tradeRegisterNumber: tradeRegisterNumber.trim() || null,
      transportLicenseNumber: transportLicenseNumber.trim() || null,
      transportLicenseExpiry: transportLicenseExpiry || null,
      // Un rând fără nume nu e un delegat. Restul rubricilor pot lipsi: pe formular se scriu de
      // mână oricum, iar aici sunt doar ce se precompletează.
      drivers: drivers
        .filter((d) => d.name.trim() !== "")
        .map((d) => ({
          id: d.id,
          name: d.name.trim(),
          identification: d.identification?.trim() || null,
          vehicleRegistration: d.vehicleRegistration?.trim() || null,
        })),
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

  function handleDeactivate(p: Partner) {
    confirm({
      title: t.confirmDeactivateTitle,
      message: (
        <>
          <strong className="text-content">{p.name}</strong>
          {p.cui ? ` — CUI ${p.cui}` : ""}. {t.confirmDeactivate}
        </>
      ),
      confirmLabel: t.deactivate,
      tone: "danger",
      onConfirm: () => deactivate(p),
    });
  }

  function deactivate(p: Partner) {
    deactivateMut.mutate(p.id, {
      onSuccess: () => notify(t.deactivated, "success"),
      onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
    });
  }

  function reactivate(p: Partner) {
    reactivateMut.mutate(p.id, {
      onSuccess: () => notify(strings.common.reactivated, "success"),
      onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
    });
  }

  // `n` deschide formularul, unde contul are voie. Scurtătura tace pe un cont care
  // n-ar putea salva oricum: o comandă care nu face nimic e mai rea decât una lipsă.
  useHotkey("n", openCreate, { enabled: Boolean(canManage) });

  return (
    <div>
      <PageHeader
        title={t.title}
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

      <div className="mt-6 grid gap-3 sm:flex sm:flex-wrap sm:items-end">
        <div>
          <Label htmlFor="filter-role">{t.filterRole}</Label>
          <Select
            id="filter-role"
            value={roleFilter}
            onChange={(ev) => setRoleFilter(ev.target.value as RoleFilter)}
            className="w-full sm:w-56"
          >
            <option value="">{t.filterRoleAll}</option>
            <option value="client">{roleLabels.client}</option>
            <option value="supplier">{roleLabels.supplier}</option>
            <option value="none">{roleLabels.none}</option>
            {/* Altă axă, același select: e o singură valoare, iar un al doilea dropdown ar
                costa mai mult decât explică. */}
            <optgroup label={t.carrierColumn}>
              <option value="carrier">{t.filterCarrier}</option>
            </optgroup>
          </Select>
        </div>
      </div>

      <section className="mt-4">
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
                  <SortableTH sortKey="cui" sort={view.sort} onSort={view.toggleSort}>
                    {t.cui}
                  </SortableTH>
                  <TH>{t.role}</TH>
                  <TH>{t.type}</TH>
                  <TH>{t.carrierColumn}</TH>
                  <TH>{t.authorizationNumber}</TH>
                  <SortableTH
                    sortKey="authorizationExpiry"
                    sort={view.sort}
                    onSort={view.toggleSort}
                  >
                    {t.authorizationExpiry}
                  </SortableTH>
                  <TH>{strings.common.status}</TH>
                  {canManage && <TH sticky="right" className="text-right">{strings.common.actions}</TH>}
                </TR>
              </THead>
              <TBody>
                {(isLoading || visiblePartners.length === 0) && (
                  <TableFallbackRow
                    columns={canManage ? 9 : 8}
                    loading={isLoading}
                    icon={Users}
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
                {visiblePartners.map((p) => (
                  <TR key={p.id}>
                    <TD className="font-medium text-content">{p.name}</TD>
                    <TD>{p.cui || "—"}</TD>
                    <TD>
                      <PartnerRoleBadge partner={p} />
                    </TD>
                    <TD>
                      {p.type ? (
                        typeLabels[p.type]
                      ) : (
                        <span className="text-content-muted">{t.typeNoneShort}</span>
                      )}
                    </TD>
                    <TD>
                      {p.carrier ? (
                        <Badge variant="muted">{t.carrierYes}</Badge>
                      ) : (
                        <span className="text-content-subtle">{t.carrierNo}</span>
                      )}
                    </TD>
                    <TD>{p.authorizationNumber || "—"}</TD>
                    <TD>
                      <ExpiryBadge partner={p} />
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
                            <Button variant="ghost" size="sm" onClick={() => reactivate(p)}>
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
      </section>

      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        title={editing ? t.editTitle : t.addTitle}
        footer={
          <>
            <Button variant="outline" onClick={() => setDialogOpen(false)} disabled={isSubmitting}>
              {strings.common.cancel}
            </Button>
            <Button type="submit" form="partner-form" disabled={isSubmitting}>
              {isSubmitting ? strings.common.saving : strings.common.save}
            </Button>
          </>
        }
      >
        <form id="partner-form" onSubmit={handleSubmit} className="space-y-4">
          <div>
            <Label htmlFor="p-name">{t.name}</Label>
            <Input
              id="p-name"
              value={name}
              onChange={(e) => {
                setName(e.target.value);
                if (nameError) setNameError(false);
              }}
              autoFocus
            />
            {nameError && <p className="mt-1 text-xs text-red-600">{strings.common.requiredField}</p>}
            {nameSuggestions.length > 0 && (
              <div className="mt-1 rounded-md border border-amber-200 bg-amber-50 px-2 py-1.5">
                <p className="text-xs font-medium text-amber-800">{t.nameSuggestions}</p>
                <ul className="mt-0.5 space-y-0.5">
                  {nameSuggestions.map((p) => (
                    <li key={p.id}>
                      <button
                        type="button"
                        className="text-xs text-amber-900 underline underline-offset-2"
                        onClick={() => openEdit(p)}
                      >
                        {p.name}
                        {p.cui ? ` — ${p.cui}` : ""}
                        {!p.active ? ` (${t.inactive})` : ""}
                      </button>
                    </li>
                  ))}
                </ul>
                <p className="mt-0.5 text-xs text-amber-700">{t.nameSuggestionsHint}</p>
              </div>
            )}
          </div>
          <div>
            <span className="block text-sm font-medium text-content-strong">{t.role}</span>
            <div className="mt-2 space-y-2">
              <label className="flex items-start gap-2 text-sm">
                <input
                  type="checkbox"
                  className="mt-0.5 h-4 w-4 rounded border-line-strong text-emerald-600"
                  checked={isClient}
                  onChange={(ev) => {
                    setIsClient(ev.target.checked);
                    if (roleError) setRoleError(false);
                  }}
                />
                <span>
                  <span className="font-medium text-emerald-800">{roleLabels.client}</span>
                  <span className="block text-xs text-content-muted">{roleLabels.clientHint}</span>
                </span>
              </label>
              <label className="flex items-start gap-2 text-sm">
                <input
                  type="checkbox"
                  className="mt-0.5 h-4 w-4 rounded border-line-strong text-amber-600"
                  checked={isSupplier}
                  onChange={(ev) => {
                    setIsSupplier(ev.target.checked);
                    if (roleError) setRoleError(false);
                  }}
                />
                <span>
                  <span className="font-medium text-amber-800">{roleLabels.supplier}</span>
                  <span className="block text-xs text-content-muted">{roleLabels.supplierHint}</span>
                </span>
              </label>
            </div>
            {roleError && <p className="mt-1 text-xs text-red-600">{t.roleRequired}</p>}
          </div>
          <div>
            <Label htmlFor="p-type">{t.type}</Label>
            <Select
              id="p-type"
              value={type}
              onChange={(e) => {
                setType(e.target.value as PartnerType | "");
                if (typeError) setTypeError(false);
              }}
            >
              {PARTNER_TYPES.map((pt) => (
                <option key={pt} value={pt}>
                  {typeLabels[pt]}
                </option>
              ))}
              <option value="">{t.typeNone}</option>
            </Select>
            {!type && <p className="mt-1 text-xs text-content-muted">{t.typeNoneHint}</p>}
            {typeError && <p className="mt-1 text-xs text-red-600">{t.typeRequired}</p>}
          </div>

          {/* Transportatorul e o bifă, nu un tip: aceeași firmă e des și colector, și
              transportator, iar un enum exclusiv ar fi obligat-o să existe de două ori. Licența și
              șoferii apar numai bifat, ca să nu se ceară tuturor date care nu-i privesc. */}
          <div>
            <Label htmlFor="p-pkg-origin">{strings.packagingOrigin.label}</Label>
            <Select
              id="p-pkg-origin"
              value={packagingOrigin}
              onChange={(e) => setPackagingOrigin(e.target.value as "" | PackagingOrigin)}
            >
              <option value="">{strings.packagingOrigin.none}</option>
              <option value="GENERATOR_PJ">{strings.packagingOrigin.GENERATOR_PJ}</option>
              <option value="COLECTOR">{strings.packagingOrigin.COLECTOR}</option>
              <option value="COMERCIANT">{strings.packagingOrigin.COMERCIANT}</option>
            </Select>
            <p className="mt-1 text-xs text-content-muted">{strings.packagingOrigin.hintPartner}</p>
          </div>

          <div className="rounded-md border border-line bg-surface-muted p-3">
            <label className="flex items-start gap-2 text-sm">
              <input
                type="checkbox"
                className="mt-0.5 h-4 w-4 rounded border-line-strong text-sky-600"
                checked={isCarrier}
                onChange={(ev) => {
                  setIsCarrier(ev.target.checked);
                  if (typeError) setTypeError(false);
                }}
              />
              <span>
                <span className="font-medium text-sky-800">{t.carrier}</span>
                <span className="block text-xs text-content-muted">{t.carrierHint}</span>
              </span>
            </label>

            {isCarrier && (
              <div className="mt-3 space-y-3 border-t border-line pt-3">
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  <div>
                    <Label htmlFor="p-licence">{t.transportLicenseNumber}</Label>
                    <Input
                      id="p-licence"
                      value={transportLicenseNumber}
                      onChange={(e) => setTransportLicenseNumber(e.target.value)}
                    />
                  </div>
                  <div>
                    <Label htmlFor="p-licence-expiry">{t.transportLicenseExpiry}</Label>
                    <DateInput
                      id="p-licence-expiry"
                      value={transportLicenseExpiry}
                      onChange={(e) => setTransportLicenseExpiry(e.target.value)}
                    />
                  </div>
                </div>
                <div>
                  <span className="block text-sm font-medium text-content-strong">{t.drivers}</span>
                  <p className="mt-0.5 text-xs text-content-muted">{t.driversHint}</p>
                  <div className="mt-2 space-y-2">
                    {drivers.map((d, index) => (
                      <div key={d.id ?? `new-${index}`} className="flex items-end gap-2">
                        <div className="flex-1">
                          <Label htmlFor={`p-driver-name-${index}`}>{t.driverName}</Label>
                          <Input
                            id={`p-driver-name-${index}`}
                            value={d.name}
                            placeholder={t.driverNamePlaceholder}
                            onChange={(e) =>
                              setDrivers((prev) =>
                                prev.map((x, i) => (i === index ? { ...x, name: e.target.value } : x))
                              )
                            }
                          />
                        </div>
                        <div className="w-full sm:w-40">
                          <Label htmlFor={`p-driver-id-${index}`}>{t.driverIdentification}</Label>
                          <Input
                            id={`p-driver-id-${index}`}
                            value={d.identification ?? ""}
                            placeholder={t.driverIdentificationPlaceholder}
                            onChange={(e) =>
                              setDrivers((prev) =>
                                prev.map((x, i) =>
                                  i === index ? { ...x, identification: e.target.value } : x
                                )
                              )
                            }
                          />
                        </div>
                        <div className="w-36">
                          <Label htmlFor={`p-driver-plate-${index}`}>{t.driverVehicle}</Label>
                          <Input
                            id={`p-driver-plate-${index}`}
                            value={d.vehicleRegistration ?? ""}
                            placeholder={t.driverVehiclePlaceholder}
                            onChange={(e) =>
                              setDrivers((prev) =>
                                prev.map((x, i) =>
                                  i === index ? { ...x, vehicleRegistration: e.target.value } : x
                                )
                              )
                            }
                          />
                        </div>
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          className="mb-1 text-red-600 hover:bg-red-50"
                          onClick={() => setDrivers((prev) => prev.filter((_, i) => i !== index))}
                        >
                          {t.removeDriver}
                        </Button>
                      </div>
                    ))}
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="mt-2"
                    onClick={() =>
                      setDrivers((prev) => [
                        ...prev,
                        { name: "", identification: "", vehicleRegistration: "" },
                      ])
                    }
                  >
                    <Plus className="mr-1 h-3.5 w-3.5" />
                    {t.addDriver}
                  </Button>
                </div>
              </div>
            )}
          </div>
          <div>
            <Label htmlFor="p-cui">{t.cui}</Label>
            <Input
              id="p-cui"
              value={cui}
              onChange={(e) => setCui(e.target.value)}
              placeholder={t.cuiPlaceholder}
            />
          </div>
          <div>
            <Label htmlFor="p-auth-number">{t.authorizationNumber}</Label>
            <Input
              id="p-auth-number"
              value={authorizationNumber}
              onChange={(e) => setAuthorizationNumber(e.target.value)}
              placeholder={t.authorizationNumberPlaceholder}
            />
          </div>
          <div>
            <Label htmlFor="p-auth-expiry">{t.authorizationExpiry}</Label>
            <DateInput
              id="p-auth-expiry"
              value={authorizationExpiry}
              onChange={(e) => setAuthorizationExpiry(e.target.value)}
            />
          </div>

          <div className="space-y-4 border-t border-line pt-4">
            <p className="text-xs text-content-muted">{t.anexa3Hint}</p>
            <div>
              <Label htmlFor="p-address">{t.address}</Label>
              <Input id="p-address" value={address} onChange={(e) => setAddress(e.target.value)} />
            </div>
            <div>
              <span className="block text-sm font-medium text-content-strong">{t.workPoints}</span>
              <p className="mt-0.5 text-xs text-content-muted">{t.workPointsHint}</p>
              <div className="mt-2 space-y-2">
                {workPoints.map((wp, index) => (
                  <div key={wp.id ?? `new-${index}`} className="flex items-end gap-2">
                    <div className="w-52">
                      <Label htmlFor={`p-wp-name-${index}`}>{t.workPointName}</Label>
                      <Input
                        id={`p-wp-name-${index}`}
                        value={wp.name ?? ""}
                        placeholder={t.workPointNamePlaceholder}
                        onChange={(e) =>
                          setWorkPoints((prev) =>
                            prev.map((x, i) => (i === index ? { ...x, name: e.target.value } : x))
                          )
                        }
                      />
                    </div>
                    <div className="flex-1">
                      <Label htmlFor={`p-wp-address-${index}`}>{t.workPointAddress}</Label>
                      <Input
                        id={`p-wp-address-${index}`}
                        value={wp.address}
                        onChange={(e) =>
                          setWorkPoints((prev) =>
                            prev.map((x, i) => (i === index ? { ...x, address: e.target.value } : x))
                          )
                        }
                      />
                    </div>
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="mb-1 text-red-600 hover:bg-red-50"
                      onClick={() => setWorkPoints((prev) => prev.filter((_, i) => i !== index))}
                    >
                      {t.removeWorkPoint}
                    </Button>
                  </div>
                ))}
              </div>
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="mt-2"
                onClick={() => setWorkPoints((prev) => [...prev, { name: "", address: "" }])}
              >
                <Plus className="mr-1 h-3.5 w-3.5" />
                {t.addWorkPoint}
              </Button>
            </div>
            <div>
              <Label htmlFor="p-reg">{t.tradeRegisterNumber}</Label>
              <Input
                id="p-reg"
                value={tradeRegisterNumber}
                onChange={(e) => setTradeRegisterNumber(e.target.value)}
                placeholder={t.tradeRegisterNumberPlaceholder}
              />
            </div>
          </div>
        </form>
      </Dialog>

      {confirmDialog}
    </div>
  );
}
