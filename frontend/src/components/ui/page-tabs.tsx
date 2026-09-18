import type { ReactNode } from "react";

export interface PageTab {
  /** `""` = tabul implicit, care nu se scrie în adresă. */
  id: string;
  label: string;
  count?: number;
  /** LED galben înaintea numelui: ceva așteaptă acolo. */
  alert?: boolean;
}

/**
 * Taburile unei pagini, cu tabul ales în adresă (`useUrlState`). Clienți (F-B2) și pagina firmei (F-D): o singură
 * secțiune pe ecran, ca pagina să se termine sub ea.
 *
 * <p>`right` pune filtrele tabului pe **aceeași linie** cu taburile, lipite la dreapta (Generare →
 * „Totalul anului", 18.09.2026): două liste derulante cu eticheta deasupra ocupau o bandă întreagă
 * sub taburi, iar proprietarul le-a spus „arată rău acolo". Stau **în afara** lui `role="tablist"`,
 * ca între taburi să nu se plimbe cu săgețile peste ceva ce nu e tab.
 */
export function PageTabs({
  tabs,
  selected,
  onSelect,
  label,
  right,
}: {
  tabs: PageTab[];
  selected: string;
  onSelect: (id: string) => void;
  label: string;
  right?: ReactNode;
}) {
  return (
    <div className="mt-6 flex flex-wrap items-end justify-between gap-x-6 gap-y-2 border-b border-line">
    <div role="tablist" aria-label={label} className="flex gap-1 overflow-x-auto">
      {tabs.map((item) => {
        const isSelected = item.id === selected;
        return (
          <button
            key={item.id || "implicit"}
            type="button"
            role="tab"
            aria-selected={isSelected}
            onClick={() => onSelect(item.id)}
            className={
              "-mb-px inline-flex shrink-0 items-center gap-2 whitespace-nowrap border-b-2 px-3 py-2 text-sm font-medium " +
              (isSelected
                ? "border-brand-600 text-content-strong"
                : "border-transparent text-content-muted hover:text-content")
            }
          >
            {item.alert && <span className="h-2 w-2 rounded-sm bg-state-warn" aria-hidden />}
            {item.label}
            {item.count !== undefined && <span className="font-mono text-xs opacity-70">{item.count}</span>}
          </button>
        );
      })}
    </div>
      {right && <div className="flex flex-wrap items-center gap-3 pb-2">{right}</div>}
    </div>
  );
}
