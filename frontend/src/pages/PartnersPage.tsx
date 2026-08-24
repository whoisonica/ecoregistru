import { useState, type FormEvent } from "react";
import { Plus, Pencil, Ban } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import {
  usePartners,
  useCreatePartner,
  useUpdatePartner,
  useDeactivatePartner,
} from "@/hooks/usePartners";
import type {
  Partner,
  PartnerInput,
  PartnerType,
  PartnerWorkPointInput,
} from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Select } from "@/components/ui/select";
import { DateInput } from "@/components/ui/date-input";
import { Dialog } from "@/components/ui/dialog";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/table";
import { useToast } from "@/components/ui/toast";
import { PartnerRoleBadge } from "@/components/PartnerRoleBadge";

const t = strings.partners;
const typeLabels = strings.enums.partnerType;
const roleLabels = strings.enums.partnerRole;
const PARTNER_TYPES: PartnerType[] = ["COLLECTOR", "RECOVERER", "GENERATOR"];

/** Filter values for the commercial role. "none" surfaces the partners still to be classified. */
type RoleFilter = "" | "client" | "supplier" | "none";

/** Formats an authorization expiry as a status badge, mirroring backend `expiringSoon`. */
function ExpiryBadge({ partner }: { partner: Partner }) {
  if (!partner.authorizationExpiry) {
    return <span className="text-gray-400">{t.noAuthorization}</span>;
  }
  const date = partner.authorizationExpiry;
  const isExpired = new Date(date) < new Date(new Date().toDateString());
  if (isExpired) {
    return <Badge variant="danger">{t.expired}</Badge>;
  }
  if (partner.expiringSoon) {
    return <Badge variant="warning">{`${t.expiringSoon} · ${date}`}</Badge>;
  }
  return <Badge variant="success">{date}</Badge>;
}

export function PartnersPage() {
  const { user } = useAuth();
  const canManage =
    user?.role === "PLATFORM_ADMIN" || user?.role === "ADMIN" || user?.role === "OPERATOR";

  const { data: partners, isLoading, isError } = usePartners();
  const createMut = useCreatePartner();
  const updateMut = useUpdatePartner();
  const deactivateMut = useDeactivatePartner();
  const { notify } = useToast();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Partner | null>(null);
  const [name, setName] = useState("");
  const [cui, setCui] = useState("");
  const [authorizationNumber, setAuthorizationNumber] = useState("");
  const [authorizationExpiry, setAuthorizationExpiry] = useState("");
  const [type, setType] = useState<PartnerType>("COLLECTOR");
  const [isClient, setIsClient] = useState(false);
  const [isSupplier, setIsSupplier] = useState(true);
  const [address, setAddress] = useState("");
  const [workPoints, setWorkPoints] = useState<PartnerWorkPointInput[]>([]);
  const [tradeRegisterNumber, setTradeRegisterNumber] = useState("");
  const [transportLicenseNumber, setTransportLicenseNumber] = useState("");
  const [transportLicenseExpiry, setTransportLicenseExpiry] = useState("");
  const [nameError, setNameError] = useState(false);
  const [roleError, setRoleError] = useState(false);
  const [roleFilter, setRoleFilter] = useState<RoleFilter>("");

  /**
   * Ce parteneri are deja firma, potriviti pe ce s-a tastat. Doua litere e pragul cerut pe
   * 24.08.2026: "cand adaugi partener si scrii sa apara din db ce clienti sunt dupa primele 2
   * litere". Se cauta si la inceput, si in interiorul numelui, fiindca "SC Retim SA" se cauta la
   * fel de des dupa "re" ca dupa "sc".
   *
   * Nu e un apel nou: lista partenerilor e deja incarcata pentru tabel, si e a tenantului. La
   * editare nu se sugereaza nimic — partenerul exista deja, iar propriul nume nu e un duplicat.
   */
  const nameSuggestions =
    editing || name.trim().length < 2
      ? []
      : (partners ?? [])
          .filter((p) => p.name.toLowerCase().includes(name.trim().toLowerCase()))
          .slice(0, 5);

  const visiblePartners = (partners ?? []).filter((p) => {
    if (roleFilter === "client") return p.client;
    if (roleFilter === "supplier") return p.supplier;
    if (roleFilter === "none") return !p.client && !p.supplier;
    return true;
  });

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
    setAddress("");
    setWorkPoints([]);
    setTradeRegisterNumber("");
    setTransportLicenseNumber("");
    setTransportLicenseExpiry("");
    setNameError(false);
    setRoleError(false);
    setDialogOpen(true);
  }

  function openEdit(p: Partner) {
    setEditing(p);
    setName(p.name);
    setCui(p.cui ?? "");
    setAuthorizationNumber(p.authorizationNumber ?? "");
    setAuthorizationExpiry(p.authorizationExpiry ?? "");
    setType(p.type);
    setIsClient(p.client);
    setIsSupplier(p.supplier);
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
    const input: PartnerInput = {
      name: name.trim(),
      cui: cui.trim() || null,
      authorizationNumber: authorizationNumber.trim() || null,
      authorizationExpiry: authorizationExpiry || null,
      type,
      client: isClient,
      supplier: isSupplier,
      address: address.trim() || null,
      // Rândurile fără adresă se aruncă: un punct de lucru fără adresă nu e nimic pe Anexa 3.
      workPoints: workPoints
        .filter((wp) => wp.address.trim() !== "")
        .map((wp) => ({ id: wp.id, name: wp.name?.trim() || null, address: wp.address.trim() })),
      tradeRegisterNumber: tradeRegisterNumber.trim() || null,
      transportLicenseNumber: transportLicenseNumber.trim() || null,
      transportLicenseExpiry: transportLicenseExpiry || null,
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
    if (!window.confirm(t.confirmDeactivate)) return;
    deactivateMut.mutate(p.id, {
      onSuccess: () => notify(t.deactivated, "success"),
      onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
    });
  }

  return (
    <div>
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">{t.title}</h1>
          <p className="mt-1 text-sm text-gray-500">{t.subtitle}</p>
        </div>
        {canManage && (
          <Button onClick={openCreate}>
            <Plus className="mr-2 h-4 w-4" />
            {t.add}
          </Button>
        )}
      </div>

      <div className="mt-6 flex flex-wrap items-end gap-3">
        <div>
          <Label htmlFor="filter-role">{t.filterRole}</Label>
          <Select
            id="filter-role"
            value={roleFilter}
            onChange={(ev) => setRoleFilter(ev.target.value as RoleFilter)}
            className="w-56"
          >
            <option value="">{t.filterRoleAll}</option>
            <option value="client">{roleLabels.client}</option>
            <option value="supplier">{roleLabels.supplier}</option>
            <option value="none">{roleLabels.none}</option>
          </Select>
        </div>
      </div>

      <section className="mt-4">
        {isLoading && <p className="text-sm text-gray-500">{strings.common.loading}</p>}
        {isError && <p className="text-sm text-red-600">{t.loadError}</p>}

        {!isLoading && !isError && (
          <Table>
            <THead>
              <TR>
                <TH>{t.name}</TH>
                <TH>{t.cui}</TH>
                <TH>{t.role}</TH>
                <TH>{t.type}</TH>
                <TH>{t.authorizationNumber}</TH>
                <TH>{t.authorizationExpiry}</TH>
                <TH>{strings.common.status}</TH>
                {canManage && <TH className="text-right">{strings.common.actions}</TH>}
              </TR>
            </THead>
            <TBody>
              {visiblePartners.length === 0 && (
                <TR>
                  <TD colSpan={canManage ? 8 : 7} className="text-center text-gray-400">
                    {t.empty}
                  </TD>
                </TR>
              )}
              {visiblePartners.map((p) => (
                <TR key={p.id}>
                  <TD className="font-medium text-gray-900">{p.name}</TD>
                  <TD>{p.cui || "—"}</TD>
                  <TD>
                    <PartnerRoleBadge partner={p} />
                  </TD>
                  <TD>{typeLabels[p.type]}</TD>
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
                    <TD className="text-right">
                      <div className="flex justify-end gap-1">
                        <Button variant="ghost" size="sm" onClick={() => openEdit(p)}>
                          <Pencil className="mr-1 h-3.5 w-3.5" />
                          {strings.common.edit}
                        </Button>
                        {p.active && (
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-red-600 hover:bg-red-50"
                            onClick={() => handleDeactivate(p)}
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
            <span className="block text-sm font-medium text-gray-700">{t.role}</span>
            <div className="mt-2 space-y-2">
              <label className="flex items-start gap-2 text-sm">
                <input
                  type="checkbox"
                  className="mt-0.5 h-4 w-4 rounded border-gray-300 text-emerald-600"
                  checked={isClient}
                  onChange={(ev) => {
                    setIsClient(ev.target.checked);
                    if (roleError) setRoleError(false);
                  }}
                />
                <span>
                  <span className="font-medium text-emerald-800">{roleLabels.client}</span>
                  <span className="block text-xs text-gray-500">{roleLabels.clientHint}</span>
                </span>
              </label>
              <label className="flex items-start gap-2 text-sm">
                <input
                  type="checkbox"
                  className="mt-0.5 h-4 w-4 rounded border-gray-300 text-amber-600"
                  checked={isSupplier}
                  onChange={(ev) => {
                    setIsSupplier(ev.target.checked);
                    if (roleError) setRoleError(false);
                  }}
                />
                <span>
                  <span className="font-medium text-amber-800">{roleLabels.supplier}</span>
                  <span className="block text-xs text-gray-500">{roleLabels.supplierHint}</span>
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
              onChange={(e) => setType(e.target.value as PartnerType)}
            >
              {PARTNER_TYPES.map((pt) => (
                <option key={pt} value={pt}>
                  {typeLabels[pt]}
                </option>
              ))}
            </Select>
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

          <div className="space-y-4 border-t border-gray-200 pt-4">
            <p className="text-xs text-gray-500">{t.anexa3Hint}</p>
            <div>
              <Label htmlFor="p-address">{t.address}</Label>
              <Input id="p-address" value={address} onChange={(e) => setAddress(e.target.value)} />
            </div>
            <div>
              <span className="block text-sm font-medium text-gray-700">{t.workPoints}</span>
              <p className="mt-0.5 text-xs text-gray-500">{t.workPointsHint}</p>
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
            <div className="grid grid-cols-2 gap-3">
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
          </div>
        </form>
      </Dialog>
    </div>
  );
}
