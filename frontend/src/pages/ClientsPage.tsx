import { useState, type FormEvent } from "react";
import { Plus, Pencil, UserPlus } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import {
  useCompanies,
  useCreateCompany,
  useUpdateCompany,
  useInviteUser,
} from "@/hooks/useCompanies";
import type { Company, CompanyInput, CompanyType, InviteRole, InviteUserInput } from "@/lib/types";
import {
  CompanyProfileFields,
  emptyCompanyProfile,
  type CompanyProfileValue,
} from "@/components/CompanyProfileFields";
import { AccountRequestsSection } from "@/components/AccountRequestsSection";
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

const t = strings.clients;
const typeLabels = strings.enums.companyType;
const roleLabels = strings.enums.inviteRole;
const COMPANY_TYPES: CompanyType[] = ["GENERATOR", "COLLECTOR", "BOTH"];
const INVITE_ROLES: InviteRole[] = ["ADMIN", "OPERATOR", "CLIENT_VIEWER"];

export function ClientsPage() {
  const { user } = useAuth();
  const isPlatformAdmin = user?.role === "PLATFORM_ADMIN";

  const { data: companies, isLoading, isError } = useCompanies(!!isPlatformAdmin);
  const createMut = useCreateCompany();
  const updateMut = useUpdateCompany();
  const inviteMut = useInviteUser();
  const { notify } = useToast();

  // --- company create/edit dialog ---
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Company | null>(null);
  const [name, setName] = useState("");
  const [cui, setCui] = useState("");
  const [type, setType] = useState<CompanyType>("GENERATOR");
  const [afmObligation, setAfmObligation] = useState(false);
  const [environmentalAuthNumber, setEnvironmentalAuthNumber] = useState("");
  const [environmentalAuthExpiry, setEnvironmentalAuthExpiry] = useState("");
  const [address, setAddress] = useState("");
  const [contactName, setContactName] = useState("");
  const [contactEmail, setContactEmail] = useState("");
  const [contactPhone, setContactPhone] = useState("");
  const [tradeRegisterNumber, setTradeRegisterNumber] = useState("");
  const [anexa3Series, setAnexa3Series] = useState("");
  // The answers from the client's intake form. Empty is a valid answer: nothing is narrowed.
  const [profile, setProfile] = useState<CompanyProfileValue>(emptyCompanyProfile);
  const [formError, setFormError] = useState<false | "name" | "cui">(false);

  // --- invite-user dialog ---
  const [inviteOpen, setInviteOpen] = useState(false);
  const [inviteCompany, setInviteCompany] = useState<Company | null>(null);
  const [inviteEmail, setInviteEmail] = useState("");
  const [inviteRole, setInviteRole] = useState<InviteRole>("OPERATOR");
  const [inviteFirstName, setInviteFirstName] = useState("");
  const [inviteLastName, setInviteLastName] = useState("");
  const [inviteEmailError, setInviteEmailError] = useState(false);

  const isSubmitting = createMut.isPending || updateMut.isPending;

  if (!isPlatformAdmin) {
    return (
      <div>
        <h1 className="text-2xl font-bold">{t.title}</h1>
        <p className="mt-4 text-sm text-gray-500">{t.onlyPlatformAdmin}</p>
      </div>
    );
  }

  function openCreate() {
    setEditing(null);
    setName("");
    setCui("");
    setType("GENERATOR");
    setAfmObligation(false);
    setEnvironmentalAuthNumber("");
    setEnvironmentalAuthExpiry("");
    setAddress("");
    setContactName("");
    setContactEmail("");
    setContactPhone("");
    setFormError(false);
    setTradeRegisterNumber("");
    setAnexa3Series("");
    setProfile(emptyCompanyProfile);
    setDialogOpen(true);
  }

  function openEdit(c: Company) {
    setEditing(c);
    setName(c.name);
    setCui(c.cui);
    setType(c.type);
    setAfmObligation(!!c.afmObligation);
    setEnvironmentalAuthNumber(c.environmentalAuthNumber ?? "");
    setEnvironmentalAuthExpiry(c.environmentalAuthExpiry ?? "");
    setAddress(c.address ?? "");
    setContactName(c.contactName ?? "");
    setContactEmail(c.contactEmail ?? "");
    setContactPhone(c.contactPhone ?? "");
    setTradeRegisterNumber(c.tradeRegisterNumber ?? "");
    setAnexa3Series(c.anexa3Series ?? "");
    setProfile({
      authorizedOperationCodes: c.authorizedOperationCodes ?? [],
      marketRoles: c.marketRoles ?? [],
      authorizedWasteCodes: c.authorizedWasteCodes ?? [],
      transportMeans: c.transportMeans ?? "",
      transportLicenseNumber: c.transportLicenseNumber ?? "",
      transportLicenseExpiry: c.transportLicenseExpiry ?? "",
    });
    setFormError(false);
    setDialogOpen(true);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!name.trim()) {
      setFormError("name");
      return;
    }
    if (!cui.trim()) {
      setFormError("cui");
      return;
    }
    const input: CompanyInput = {
      name: name.trim(),
      cui: cui.trim(),
      type,
      afmObligation,
      environmentalAuthNumber: environmentalAuthNumber.trim() || null,
      environmentalAuthExpiry: environmentalAuthExpiry || null,
      address: address.trim() || null,
      contactName: contactName.trim() || null,
      contactEmail: contactEmail.trim() || null,
      contactPhone: contactPhone.trim() || null,
      tradeRegisterNumber: tradeRegisterNumber.trim() || null,
      anexa3Series: anexa3Series.trim() || null,
      authorizedOperationCodes: profile.authorizedOperationCodes,
      marketRoles: profile.marketRoles,
      authorizedWasteCodeIds: profile.authorizedWasteCodes.map((w) => w.id),
      transportMeans: profile.transportMeans.trim() || null,
      transportLicenseNumber: profile.transportLicenseNumber.trim() || null,
      transportLicenseExpiry: profile.transportLicenseExpiry || null,
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

  function openInvite(c: Company) {
    setInviteCompany(c);
    setInviteEmail("");
    setInviteRole("OPERATOR");
    setInviteFirstName("");
    setInviteLastName("");
    setInviteEmailError(false);
    setInviteOpen(true);
  }

  async function handleInvite(e: FormEvent) {
    e.preventDefault();
    if (!inviteEmail.trim()) {
      setInviteEmailError(true);
      return;
    }
    if (!inviteCompany) return;
    const input: InviteUserInput = {
      email: inviteEmail.trim(),
      role: inviteRole,
      firstName: inviteFirstName.trim() || null,
      lastName: inviteLastName.trim() || null,
    };
    try {
      await inviteMut.mutateAsync({ id: inviteCompany.id, input });
      notify(t.invited, "success");
      setInviteOpen(false);
    } catch (err) {
      notify(apiErrorMessage(err, t.inviteError), "error");
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">{t.title}</h1>
          <p className="mt-1 text-sm text-gray-500">{t.subtitle}</p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="mr-2 h-4 w-4" />
          {t.add}
        </Button>
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
                <TH>{t.afm}</TH>
                <TH>{strings.common.status}</TH>
                <TH className="text-right">{strings.common.actions}</TH>
              </TR>
            </THead>
            <TBody>
              {(companies ?? []).length === 0 && (
                <TR>
                  <TD colSpan={6} className="text-center text-gray-400">
                    {t.empty}
                  </TD>
                </TR>
              )}
              {(companies ?? []).map((c) => (
                <TR key={c.id}>
                  <TD className="font-medium text-gray-900">{c.name}</TD>
                  <TD>{c.cui}</TD>
                  <TD>{typeLabels[c.type]}</TD>
                  <TD>
                    {c.afmObligation ? (
                      <Badge variant="warning">{t.afmYes}</Badge>
                    ) : (
                      <span className="text-gray-400">{t.afmNo}</span>
                    )}
                  </TD>
                  <TD>
                    {c.active ? (
                      <Badge variant="success">{t.active}</Badge>
                    ) : (
                      <Badge variant="muted">{t.inactive}</Badge>
                    )}
                  </TD>
                  <TD className="text-right">
                    <div className="flex justify-end gap-1">
                      <Button variant="ghost" size="sm" onClick={() => openInvite(c)}>
                        <UserPlus className="mr-1 h-3.5 w-3.5" />
                        {t.invite}
                      </Button>
                      <Button variant="ghost" size="sm" onClick={() => openEdit(c)}>
                        <Pencil className="mr-1 h-3.5 w-3.5" />
                        {strings.common.edit}
                      </Button>
                    </div>
                  </TD>
                </TR>
              ))}
            </TBody>
          </Table>
        )}
      </section>

      {/* Create / edit company */}
      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        title={editing ? t.editTitle : t.addTitle}
        footer={
          <>
            <Button variant="outline" onClick={() => setDialogOpen(false)} disabled={isSubmitting}>
              {strings.common.cancel}
            </Button>
            <Button type="submit" form="company-form" disabled={isSubmitting}>
              {isSubmitting ? strings.common.saving : strings.common.save}
            </Button>
          </>
        }
      >
        <form id="company-form" onSubmit={handleSubmit} className="space-y-4">
          <div>
            <Label htmlFor="c-name">{t.name}</Label>
            <Input
              id="c-name"
              value={name}
              onChange={(e) => {
                setName(e.target.value);
                if (formError === "name") setFormError(false);
              }}
              placeholder={t.namePlaceholder}
              autoFocus
            />
            {formError === "name" && (
              <p className="mt-1 text-xs text-red-600">{strings.common.requiredField}</p>
            )}
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label htmlFor="c-cui">{t.cui}</Label>
              <Input
                id="c-cui"
                value={cui}
                onChange={(e) => {
                  setCui(e.target.value);
                  if (formError === "cui") setFormError(false);
                }}
                placeholder={t.cuiPlaceholder}
              />
              {formError === "cui" && (
                <p className="mt-1 text-xs text-red-600">{strings.common.requiredField}</p>
              )}
            </div>
            <div>
              <Label htmlFor="c-type">{t.type}</Label>
              <Select
                id="c-type"
                value={type}
                onChange={(e) => setType(e.target.value as CompanyType)}
              >
                {COMPANY_TYPES.map((ct) => (
                  <option key={ct} value={ct}>
                    {typeLabels[ct]}
                  </option>
                ))}
              </Select>
            </div>
          </div>
          <label className="flex items-center gap-2 text-sm text-gray-700">
            <input
              type="checkbox"
              className="h-4 w-4 rounded border-gray-300 text-brand focus:ring-brand"
              checked={afmObligation}
              onChange={(e) => setAfmObligation(e.target.checked)}
            />
            {t.afmLabel}
          </label>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label htmlFor="c-auth-number">{t.environmentalAuthNumber}</Label>
              <Input
                id="c-auth-number"
                value={environmentalAuthNumber}
                onChange={(e) => setEnvironmentalAuthNumber(e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="c-auth-expiry">{t.environmentalAuthExpiry}</Label>
              <DateInput
                id="c-auth-expiry"
                value={environmentalAuthExpiry}
                onChange={(e) => setEnvironmentalAuthExpiry(e.target.value)}
              />
            </div>
          </div>
          <div>
            <Label htmlFor="c-address">{t.address}</Label>
            <Input id="c-address" value={address} onChange={(e) => setAddress(e.target.value)} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label htmlFor="c-contact-name">{t.contactName}</Label>
              <Input
                id="c-contact-name"
                value={contactName}
                onChange={(e) => setContactName(e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="c-contact-phone">{t.contactPhone}</Label>
              <Input
                id="c-contact-phone"
                value={contactPhone}
                onChange={(e) => setContactPhone(e.target.value)}
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label htmlFor="c-reg">{strings.partners.tradeRegisterNumber}</Label>
              <Input
                id="c-reg"
                value={tradeRegisterNumber}
                onChange={(e) => setTradeRegisterNumber(e.target.value)}
                placeholder={strings.partners.tradeRegisterNumberPlaceholder}
              />
            </div>
            <div>
              <Label htmlFor="c-anexa3-series">{t.anexa3Series}</Label>
              <Input
                id="c-anexa3-series"
                value={anexa3Series}
                onChange={(e) => setAnexa3Series(e.target.value)}
                placeholder={t.anexa3SeriesPlaceholder}
              />
              <p className="mt-1 text-xs text-gray-500">{t.anexa3SeriesHint}</p>
            </div>
          </div>

          <div>
            <Label htmlFor="c-contact-email">{t.contactEmail}</Label>
            <Input
              id="c-contact-email"
              type="email"
              value={contactEmail}
              onChange={(e) => setContactEmail(e.target.value)}
            />
          </div>

          <CompanyProfileFields value={profile} onChange={setProfile} companyType={type} />
        </form>
      </Dialog>

      {/* Invite user */}
      <Dialog
        open={inviteOpen}
        onClose={() => setInviteOpen(false)}
        title={t.inviteTitle.replace("{company}", inviteCompany?.name ?? "")}
        footer={
          <>
            <Button variant="outline" onClick={() => setInviteOpen(false)} disabled={inviteMut.isPending}>
              {strings.common.cancel}
            </Button>
            <Button type="submit" form="invite-form" disabled={inviteMut.isPending}>
              {inviteMut.isPending ? strings.common.saving : t.inviteSubmit}
            </Button>
          </>
        }
      >
        <form id="invite-form" onSubmit={handleInvite} className="space-y-4">
          <p className="text-xs text-gray-500">{t.inviteHint}</p>
          <div>
            <Label htmlFor="i-email">{t.inviteEmail}</Label>
            <Input
              id="i-email"
              type="email"
              value={inviteEmail}
              onChange={(e) => {
                setInviteEmail(e.target.value);
                if (inviteEmailError) setInviteEmailError(false);
              }}
              placeholder={t.inviteEmailPlaceholder}
              autoFocus
            />
            {inviteEmailError && (
              <p className="mt-1 text-xs text-red-600">{strings.common.requiredField}</p>
            )}
          </div>
          <div>
            <Label htmlFor="i-role">{t.inviteRole}</Label>
            <Select
              id="i-role"
              value={inviteRole}
              onChange={(e) => setInviteRole(e.target.value as InviteRole)}
            >
              {INVITE_ROLES.map((r) => (
                <option key={r} value={r}>
                  {roleLabels[r]}
                </option>
              ))}
            </Select>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label htmlFor="i-first">{t.inviteFirstName}</Label>
              <Input
                id="i-first"
                value={inviteFirstName}
                onChange={(e) => setInviteFirstName(e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="i-last">{t.inviteLastName}</Label>
              <Input
                id="i-last"
                value={inviteLastName}
                onChange={(e) => setInviteLastName(e.target.value)}
              />
            </div>
          </div>
        </form>
      </Dialog>

      <AccountRequestsSection enabled={isPlatformAdmin} />
    </div>
  );
}
