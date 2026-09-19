import { useState, type FormEvent } from "react";
import { notifyInvited } from "@/lib/inviteNotice";
import { useInviteUser } from "@/hooks/useCompanies";
import type { Company, InviteRole, InviteUserInput } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { useToast } from "@/components/ui/toast";

const t = strings.clients;
const roleLabels = strings.enums.inviteRole;
const INVITE_ROLES: InviteRole[] = ["ADMIN", "OPERATOR", "CLIENT_VIEWER"];

/**
 * Invită un om într-o firmă numită după id (`POST /companies/{id}/users`), fără să muți comutatorul. Din „⋯” pe rândul
 * din Clienți și din capul paginii firmei (F-D). Se montează doar deschis, deci rubricile pornesc goale de fiecare dată.
 */
export function InviteUserDialog({ company, onClose }: { company: Company; onClose: () => void }) {
  const inviteMut = useInviteUser();
  const { notify } = useToast();
  const [email, setEmail] = useState("");
  const [role, setRole] = useState<InviteRole>("OPERATOR");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [emailError, setEmailError] = useState(false);

  async function handleInvite(e: FormEvent) {
    e.preventDefault();
    if (!email.trim()) {
      setEmailError(true);
      return;
    }
    const input: InviteUserInput = {
      email: email.trim(),
      role,
      firstName: firstName.trim() || null,
      lastName: lastName.trim() || null,
    };
    try {
      const invited = await inviteMut.mutateAsync({ id: company.id, input });
      notifyInvited(notify, invited, t.invited);
      onClose();
    } catch (err) {
      notify(apiErrorMessage(err, t.inviteError), "error");
    }
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title={t.inviteTitle.replace("{company}", company.name)}
      footer={
        <>
          <Button variant="outline" onClick={onClose} disabled={inviteMut.isPending}>
            {strings.common.cancel}
          </Button>
          <Button type="submit" form="invite-form" disabled={inviteMut.isPending}>
            {inviteMut.isPending ? strings.common.saving : t.inviteSubmit}
          </Button>
        </>
      }
    >
      <form id="invite-form" onSubmit={handleInvite} className="space-y-4">
        <p className="text-xs text-content-muted">{t.inviteHint}</p>
        <div>
          <Label htmlFor="i-email">{t.inviteEmail}</Label>
          <Input
            id="i-email"
            type="email"
            value={email}
            onChange={(e) => {
              setEmail(e.target.value);
              if (emailError) setEmailError(false);
            }}
            placeholder={t.inviteEmailPlaceholder}
            autoFocus
          />
          {emailError && <p className="mt-1 text-xs text-state-bad-text">{strings.common.requiredField}</p>}
        </div>
        <div>
          <Label htmlFor="i-role">{t.inviteRole}</Label>
          <Select id="i-role" value={role} onChange={(e) => setRole(e.target.value as InviteRole)}>
            {INVITE_ROLES.map((r) => (
              <option key={r} value={r}>
                {roleLabels[r]}
              </option>
            ))}
          </Select>
        </div>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div>
            <Label htmlFor="i-first">{t.inviteFirstName}</Label>
            <Input id="i-first" value={firstName} onChange={(e) => setFirstName(e.target.value)} />
          </div>
          <div>
            <Label htmlFor="i-last">{t.inviteLastName}</Label>
            <Input id="i-last" value={lastName} onChange={(e) => setLastName(e.target.value)} />
          </div>
        </div>
      </form>
    </Dialog>
  );
}
