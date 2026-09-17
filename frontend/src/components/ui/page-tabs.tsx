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
 */
export function PageTabs({
  tabs,
  selected,
  onSelect,
  label,
}: {
  tabs: PageTab[];
  selected: string;
  onSelect: (id: string) => void;
  label: string;
}) {
  return (
    <div role="tablist" aria-label={label} className="mt-6 flex gap-1 overflow-x-auto border-b border-line">
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
  );
}
