import { useState, type FormEvent } from "react";
import { Plus, Pencil, Ban } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import {
  usePartners,
  useCreatePartner,
  useUpdatePartner,
  useDeactivatePartner,
} from "@/hooks/usePartners";
import type { Partner, PartnerInput, PartnerType } from "@/lib/types";
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

const t = strings.partners;
const typeLabels = strings.enums.partnerType;
const PARTNER_TYPES: PartnerType[] = ["COLLECTOR", "CARRIER", "BOTH"];

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
  const [nameError, setNameError] = useState(false);

  const isSubmitting = createMut.isPending || updateMut.isPending;

  function openCreate() {
    setEditing(null);
    setName("");
    setCui("");
    setAuthorizationNumber("");
    setAuthorizationExpiry("");
    setType("COLLECTOR");
    setNameError(false);
    setDialogOpen(true);
  }

  function openEdit(p: Partner) {
    setEditing(p);
    setName(p.name);
    setCui(p.cui ?? "");
    setAuthorizationNumber(p.authorizationNumber ?? "");
    setAuthorizationExpiry(p.authorizationExpiry ?? "");
    setType(p.type);
    setNameError(false);
    setDialogOpen(true);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!name.trim()) {
      setNameError(true);
      return;
    }
    const input: PartnerInput = {
      name: name.trim(),
      cui: cui.trim() || null,
      authorizationNumber: authorizationNumber.trim() || null,
      authorizationExpiry: authorizationExpiry || null,
      type,
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

      <section className="mt-6">
        {isLoading && <p className="text-sm text-gray-500">{strings.common.loading}</p>}
        {isError && <p className="text-sm text-red-600">{t.loadError}</p>}

        {!isLoading && !isError && (
          <Table>
            <THead>
              <TR>
                <TH>{t.name}</TH>
                <TH>{t.cui}</TH>
                <TH>{t.type}</TH>
                <TH>{t.authorizationNumber}</TH>
                <TH>{t.authorizationExpiry}</TH>
                <TH>{strings.common.status}</TH>
                {canManage && <TH className="text-right">{strings.common.actions}</TH>}
              </TR>
            </THead>
            <TBody>
              {(partners ?? []).length === 0 && (
                <TR>
                  <TD colSpan={canManage ? 7 : 6} className="text-center text-gray-400">
                    {t.empty}
                  </TD>
                </TR>
              )}
              {(partners ?? []).map((p) => (
                <TR key={p.id}>
                  <TD className="font-medium text-gray-900">{p.name}</TD>
                  <TD>{p.cui || "—"}</TD>
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
        </form>
      </Dialog>
    </div>
  );
}
