import { useState } from "react";
import { Search } from "lucide-react";
import { useCompanyLookup, type CompanyLookup } from "@/hooks/useCompanyLookup";
import { apiErrorMessage } from "@/lib/api";
import { strings } from "@/lib/strings";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

const t = strings.partners;

/** O rubrică pe care ANAF o poate completa: ce scrie acum în ea, ce ar pune ANAF, cum se scrie. */
export interface AnafTarget {
  /** Cum se numește în mesaj: „denumirea", „adresa". */
  label: string;
  current: string;
  pick: (found: CompanyLookup) => string | null;
  set: (value: string) => void;
}

type Note = { tone: "ok" | "warn" | "error"; text: string };

/**
 * Rubrica de CUI cu butonul „Completează din ANAF”, oriunde se scrie un CUI (proprietarul,
 * 16.09.2026). Până atunci butonul era numai la Parteneri.
 *
 * <p>Completează **numai rubricile goale**: ce a scris omul nu se rescrie — poate a scris numele cu
 * care îi spun ei firmei, iar adresa de lucru poate fi alta decât sediul din registru. Căutarea
 * pleacă doar la apăsare, nu la fiecare cifră: ANAF sancționează suprasolicitarea.
 */
export function CuiField({
  id,
  label,
  value,
  onChange,
  placeholder,
  targets,
  required,
  invalid,
  error,
}: {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  targets: AnafTarget[];
  required?: boolean;
  /** Proprietățile de rubrică greșită, din `invalidProps`. */
  invalid?: Record<string, unknown>;
  /** Mesajul de eroare al formularului, pus sub rubrică în locul explicației. */
  error?: React.ReactNode;
}) {
  const [note, setNote] = useState<Note | null>(null);
  const lookup = useCompanyLookup();

  async function fill() {
    if (!value.trim()) {
      setNote({ tone: "error", text: t.anafCuiFirst });
      return;
    }
    setNote(null);
    try {
      const found = await lookup.mutateAsync(value.trim());
      const filled: string[] = [];
      for (const target of targets) {
        const next = target.pick(found);
        if (!target.current.trim() && next) {
          target.set(next);
          filled.push(target.label);
        }
      }
      const fields =
        filled.length > 1 ? `${filled.slice(0, -1).join(", ")} și ${filled[filled.length - 1]}` : filled[0];
      const text = fields
        ? t.anafFilled.replace("{fields}", fields)
        : t.anafNothingToFill.replace("{name}", found.name ?? found.cui);
      setNote(found.inactive ? { tone: "warn", text: `${text} ${t.anafInactive}` } : { tone: "ok", text });
    } catch (err) {
      setNote({ tone: "error", text: apiErrorMessage(err, t.anafError) });
    }
  }

  return (
    <div>
      <Label htmlFor={id} required={required}>
        {label}
      </Label>
      <div className="flex flex-col gap-2 sm:flex-row">
        <Input
          id={id}
          className="min-w-0 flex-1"
          value={value}
          onChange={(e) => {
            onChange(e.target.value);
            if (note) setNote(null);
          }}
          placeholder={placeholder}
          {...invalid}
        />
        <Button type="button" variant="outline" className="shrink-0" onClick={fill} loading={lookup.isPending}>
          {!lookup.isPending && <Search className="mr-2 h-4 w-4" aria-hidden />}
          {t.anafLookup}
        </Button>
      </div>
      {error}
      {note ? (
        <p
          role="status"
          className={
            note.tone === "error"
              ? "mt-1 text-xs text-red-600"
              : note.tone === "warn"
                ? "mt-1 rounded-md border border-amber-200 bg-amber-50 px-2 py-1.5 text-xs text-amber-900"
                : "mt-1 rounded-md border border-emerald-200 bg-emerald-50 px-2 py-1.5 text-xs text-emerald-900"
          }
        >
          {note.text}
        </p>
      ) : (
        !error && <p className="mt-1 text-xs text-content-muted">{t.anafLookupHint}</p>
      )}
    </div>
  );
}
