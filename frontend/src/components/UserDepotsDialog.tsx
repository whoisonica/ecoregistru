import { useState } from "react";
import { useChangeUserWorkPoints } from "@/hooks/useUsers";
import type { CompanyUser, WorkPoint } from "@/lib/types";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { ChoiceCards } from "@/components/ui/choice-cards";
import { Dialog } from "@/components/ui/dialog";
import { useToast } from "@/components/ui/toast";

const t = strings.settings.users;

type Scope = "all" | "chosen";

/**
 * D2.4 — pe ce depozite lucrează un operator sau un cont de vizualizare. „Toate” le include și pe cele
 * deschise mai târziu (decizia proprietarului: implicit toate); „Doar cele alese” cere cel puțin unul,
 * ca serverul. Depozitele inactive rămân în listă doar dacă sunt deja bifate, ca să se poată scoate.
 */
export function UserDepotsDialog({
  user,
  workPoints,
  companyId,
  onClose,
}: {
  /** Montat doar cât e deschis, cu `key={user.id}`: starea pornește din rândul utilizatorului. */
  user: CompanyUser;
  workPoints: WorkPoint[];
  companyId?: string;
  onClose: () => void;
}) {
  const mutation = useChangeUserWorkPoints(companyId);
  const { notify } = useToast();
  const [scope, setScope] = useState<Scope>(user.allWorkPoints ? "all" : "chosen");
  const [chosen, setChosen] = useState<string[]>(user.workPointIds);
  const [missing, setMissing] = useState(false);

  const listed = workPoints.filter((wp) => wp.active || chosen.includes(wp.id));

  function toggle(id: string, on: boolean) {
    setChosen((prev) => (on ? [...prev, id] : prev.filter((x) => x !== id)));
    if (on) setMissing(false);
  }

  async function save() {
    if (scope === "chosen" && chosen.length === 0) {
      setMissing(true);
      return;
    }
    try {
      await mutation.mutateAsync({
        id: user.id,
        allWorkPoints: scope === "all",
        workPointIds: scope === "all" ? [] : chosen,
      });
      notify(t.depotsSaved, "success");
      onClose();
    } catch (err) {
      notify(apiErrorMessage(err, t.saveError), "error");
    }
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title={t.depotsTitle}
      footer={
        <>
          <Button variant="outline" onClick={onClose} disabled={mutation.isPending}>
            {strings.common.cancel}
          </Button>
          <Button onClick={save} disabled={mutation.isPending}>
            {mutation.isPending ? strings.common.saving : strings.common.save}
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <p className="text-sm text-content-muted">
          <strong className="text-content">{user.email}</strong>. {t.depotsHint}
        </p>
        <ChoiceCards<Scope>
          name="user-depots-scope"
          value={scope}
          onChange={setScope}
          columns={2}
          options={[
            { value: "all", label: t.depotsAllOption, description: t.depotsAllDescription },
            { value: "chosen", label: t.depotsChosenOption, description: t.depotsChosenDescription },
          ]}
        />
        {scope === "chosen" && (
          <fieldset className="space-y-2" aria-label={t.depots}>
            {listed.map((wp) => (
              <label key={wp.id} className="flex items-center gap-2 text-sm text-content-strong">
                <input
                  type="checkbox"
                  className="h-4 w-4 rounded border-line-strong text-brand focus:ring-brand"
                  checked={chosen.includes(wp.id)}
                  onChange={(e) => toggle(wp.id, e.target.checked)}
                />
                <span>{wp.name}</span>
              </label>
            ))}
            {missing && <p className="text-xs text-red-600">{t.depotsRequired}</p>}
          </fieldset>
        )}
      </div>
    </Dialog>
  );
}
