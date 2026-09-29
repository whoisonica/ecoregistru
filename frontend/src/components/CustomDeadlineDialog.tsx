import { useState } from "react";
import { useSaveCustomDeadline } from "@/hooks/useDeadlines";
import { apiErrorMessage } from "@/lib/api";
import { todayIso } from "@/lib/dates";
import { strings } from "@/lib/strings";
import type { Deadline, DeadlineRecurrence } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { DateInput } from "@/components/ui/date-input";
import { Dialog } from "@/components/ui/dialog";
import { FieldError, invalidProps } from "@/components/ui/field-error";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PillGroup } from "@/components/ui/pill-group";
import { Textarea } from "@/components/ui/textarea";
import { useToast } from "@/components/ui/toast";

const t = strings.deadlines.custom;

const RECURRENCES: DeadlineRecurrence[] = ["ONCE", "MONTHLY", "QUARTERLY", "SEMIANNUAL", "ANNUAL"];

/**
 * Formularul unui termen propriu (V81, Andreea 29.09.2026): ce ai de făcut, data, cât de des revine,
 * detalii. `editing` = termenul modificat; fără el, unul nou. Montat doar cât e deschis, ca fiecare
 * deschidere să pornească de la valorile lui.
 */
export function CustomDeadlineDialog({ editing, onClose }: { editing: Deadline | null; onClose: () => void }) {
  const save = useSaveCustomDeadline();
  const { notify } = useToast();
  const [title, setTitle] = useState(editing?.title ?? "");
  const [dueDate, setDueDate] = useState(editing?.dueDate ?? "");
  const [recurrence, setRecurrence] = useState<DeadlineRecurrence>(editing?.recurrence ?? "ONCE");
  const [details, setDetails] = useState(editing?.details ?? "");
  const [errors, setErrors] = useState<{ title?: string; dueDate?: string }>({});

  function submit() {
    const moved = !editing || editing.dueDate !== dueDate;
    const next = {
      title: title.trim() ? undefined : t.titleRequired,
      dueDate: !dueDate ? t.dueDateRequired : moved && dueDate < todayIso() ? t.dueDatePast : undefined,
    };
    setErrors(next);
    if (next.title || next.dueDate) {
      document.getElementById(next.title ? "cd-title" : "cd-date")?.focus();
      return;
    }
    save.mutate(
      { id: editing?.id, body: { title: title.trim(), dueDate, recurrence, details: details.trim() || undefined } },
      {
        onSuccess: () => {
          notify(editing ? t.updated : t.added, "success");
          onClose();
        },
        onError: (err) => notify(apiErrorMessage(err, t.saveError), "error"),
      },
    );
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title={editing ? t.editTitle : t.addTitle}
      description={editing ? undefined : t.intro}
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            {strings.common.cancel}
          </Button>
          <Button onClick={submit} disabled={save.isPending} data-testid="custom-deadline-save">
            {save.isPending ? strings.common.saving : strings.common.save}
          </Button>
        </>
      }
    >
      <form
        className="space-y-4"
        onSubmit={(e) => {
          e.preventDefault();
          submit();
        }}
      >
        <div>
          <Label htmlFor="cd-title">{t.titleLabel}</Label>
          <Input
            id="cd-title"
            value={title}
            maxLength={120}
            autoFocus
            placeholder={t.titlePlaceholder}
            onChange={(e) => setTitle(e.target.value)}
            {...invalidProps("cd-title-err", errors.title)}
          />
          <FieldError id="cd-title-err" message={errors.title} />
          {!editing && (
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <span className="text-xs text-content-subtle">{t.suggestionsLabel}:</span>
              {t.suggestions.map((s) => (
                <button
                  key={s}
                  type="button"
                  onClick={() => setTitle(s)}
                  className="rounded-sm border border-line px-2 py-0.5 text-xs text-content-muted hover:border-content-subtle hover:text-content"
                >
                  {s}
                </button>
              ))}
            </div>
          )}
        </div>
        <div>
          <Label htmlFor="cd-date">{t.dueDateLabel}</Label>
          <DateInput
            id="cd-date"
            value={dueDate}
            min={todayIso()}
            onChange={(e) => setDueDate(e.target.value)}
            className="w-48"
            {...invalidProps("cd-date-err", errors.dueDate)}
          />
          <FieldError id="cd-date-err" message={errors.dueDate} />
        </div>
        <div>
          <Label id="cd-recurrence-label">{t.recurrenceLabel}</Label>
          <PillGroup
            name="cd-recurrence"
            aria-labelledby="cd-recurrence-label"
            options={RECURRENCES.map((r) => ({ value: r, label: strings.enums.deadlineRecurrence[r] }))}
            selected={[recurrence]}
            onToggle={setRecurrence}
          />
          {recurrence !== "ONCE" && <p className="mt-1 text-xs text-content-subtle">{t.recurrenceHint}</p>}
        </div>
        <div>
          <Label htmlFor="cd-details">{t.detailsLabel}</Label>
          <Textarea
            id="cd-details"
            value={details}
            maxLength={500}
            rows={2}
            placeholder={t.detailsPlaceholder}
            onChange={(e) => setDetails(e.target.value)}
          />
        </div>
      </form>
    </Dialog>
  );
}
