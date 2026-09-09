import { useEffect, useRef, useState, type ReactNode } from "react";
import { ChevronDown, MoreHorizontal } from "lucide-react";
import { cn } from "@/lib/utils";
import { strings } from "@/lib/strings";

/**
 * Un meniu care se deschide dintr-un buton: se închide la Escape și la un clic în afară.
 *
 * <p>Implementarea stătea în `table-toolbar.tsx`, sub numele `RowActions`, fiindcă acolo a apărut —
 * pe Mișcări, unde fiecare rând purta patru butoane cu text. Antetul de pe Evidențe avea nevoie de
 * exact același lucru cu o etichetă în loc de „⋯", iar alegerea era între a-l scrie a doua oară și
 * a-l muta aici. Al doilea meniu ar fi însemnat două comportamente de Escape și de clic-în-afară
 * care trebuie să rămână la fel — adică două ocazii să difere.
 *
 * <p>`RowActions` și `RowAction` rămân exportate din `table-toolbar`, ca învelișuri, deci niciun
 * apelant existent nu s-a schimbat.
 */
export function Menu({
  children,
  /** Când lipsește, declanșatorul e „⋯" — meniul de rând. Când e dat, un buton cu text. */
  label,
  /** Spre ce margine se deschide caseta. Pe un rând de tabel e „right": butonul stă lipit acolo. */
  align = "right",
  disabled = false,
}: {
  children: ReactNode;
  label?: string;
  align?: "left" | "right";
  disabled?: boolean;
}) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    function onDown(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    }
    function onKey(e: KeyboardEvent) {
      if (e.key !== "Escape") return;
      // `preventDefault` e contractul pe care `Dialog` îl citește: un strat dinăuntru care s-a
      // ocupat de Escape îl marchează, iar dialogul din jur rămâne deschis (la fel face lista
      // comboboxului). Fără el, un meniu deschis într-un dialog s-ar închide împreună cu dialogul
      // de sub el — adică o apăsare ar face două lucruri. Azi nu există combinația, dar `Dialog`
      // susține dinadins tiparul, iar meniul e o primitivă: cine îl pune într-un dialog mâine
      // n-are de unde ști că trebuie să repare asta întâi.
      e.preventDefault();
      setOpen(false);
    }
    document.addEventListener("mousedown", onDown);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDown);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  /* Un meniu dezactivat trebuie și să se închidă, nu doar să refuze deschiderea: butonul se poate
     dezactiva (o descărcare pornește) cât timp caseta e deschisă sub degetul cuiva. */
  useEffect(() => {
    if (disabled) setOpen(false);
  }, [disabled]);

  return (
    <div ref={ref} className="relative inline-block text-left">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        disabled={disabled}
        aria-label={label ? undefined : strings.common.moreActions}
        aria-haspopup="menu"
        aria-expanded={open}
        className={
          label
            ? cn(
                "inline-flex items-center gap-2 whitespace-nowrap rounded-md border border-line bg-surface px-3 py-2 text-sm font-medium text-content transition-colors hover:bg-surface-muted",
                "disabled:cursor-not-allowed disabled:opacity-50"
              )
            : "rounded-md p-1.5 text-content-muted transition-colors hover:bg-surface-sunken disabled:opacity-40"
        }
      >
        {label ? (
          <>
            {label}
            <ChevronDown
              className={cn("h-4 w-4 transition-transform", open && "rotate-180")}
              aria-hidden
            />
          </>
        ) : (
          <MoreHorizontal className="h-4 w-4" />
        )}
      </button>
      {open && (
        <div
          role="menu"
          className={cn(
            "absolute z-30 mt-1 w-64 animate-slide-up overflow-hidden rounded-md border border-line bg-surface py-1 shadow-popover",
            align === "right" ? "right-0" : "left-0"
          )}
          onClick={() => setOpen(false)}
        >
          {children}
        </div>
      )}
    </div>
  );
}

/**
 * Un rând din meniu. `tone="danger"` pentru fapta care nu se ia înapoi.
 *
 * <p>`hint` scrie sub etichetă ce **este** lucrul ales — pe Evidențe, că exporturile generice sunt
 * un rezumat neoficial, nu documentul care se depune. Regula casei: un ecran spune ce urmează, nu
 * lasă clientul să afle din fișierul descărcat.
 */
export function MenuItem({
  icon: Icon,
  children,
  hint,
  onClick,
  disabled = false,
  tone = "default",
}: {
  icon?: React.ComponentType<{ className?: string }>;
  children: ReactNode;
  hint?: string;
  onClick: () => void;
  disabled?: boolean;
  tone?: "default" | "danger";
}) {
  return (
    <button
      type="button"
      role="menuitem"
      onClick={onClick}
      disabled={disabled}
      className={cn(
        "flex w-full items-start gap-2.5 px-3 py-2 text-left text-sm transition-colors disabled:opacity-40",
        tone === "danger" ? "text-red-600 hover:bg-red-50" : "text-content hover:bg-surface-muted"
      )}
    >
      {Icon && <Icon className={cn("h-4 w-4 shrink-0", hint ? "mt-0.5" : "")} />}
      <span className="min-w-0">
        <span className="block">{children}</span>
        {hint && <span className="mt-0.5 block text-xs text-content-subtle">{hint}</span>}
      </span>
    </button>
  );
}
