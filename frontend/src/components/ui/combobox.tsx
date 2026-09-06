import { useEffect, useId, useRef, useState } from "react";
import { Check, ChevronsUpDown, Loader2, X } from "lucide-react";
import { cn } from "@/lib/utils";

export interface ComboboxItem {
  id: string;
  /** Primary label shown in the trigger and list. */
  label: string;
  /** Optional secondary line shown under the label in the list. */
  sublabel?: string;
}

interface ComboboxProps {
  value: ComboboxItem | null;
  onSelect: (item: ComboboxItem | null) => void;
  /** Called (debounced) as the user types, so the parent can fetch matches. */
  onQueryChange: (query: string) => void;
  items: ComboboxItem[];
  loading?: boolean;
  disabled?: boolean;
  placeholder?: string;
  searchPlaceholder?: string;
  emptyText?: string;
  id?: string;
  /** Numele accesibil, când eticheta vizibilă nu e legată prin `htmlFor`. */
  "aria-label"?: string;
}

/**
 * Searchable select: a trigger that opens a popover with a text input and a
 * results list. Data-agnostic — the parent supplies `items` and reacts to
 * `onQueryChange` (debounced here by ~250ms) to fetch them (e.g. waste codes).
 *
 * <p><b>Ce s-a reparat pe 07.09.2026.</b> Componenta asta alege codul de deșeu, adică rubrica de
 * la care pornește tot restul formularului — și se putea folosi doar cu mausul. Nu avea roluri
 * ARIA (un cititor de ecran anunța „buton", și atât), nu răspundea la săgeți sau Enter, iar „X"-ul
 * de ștergere era un SVG cu `onClick` **înăuntrul** butonului declanșator: HTML invalid, și o
 * țintă la care tastatura n-avea cum să ajungă. Acum e un `combobox` cu `listbox`, cu descendent
 * activ, iar ștergerea e un buton adevărat, alături — nu în burta altuia.
 */
export function Combobox({
  value,
  onSelect,
  onQueryChange,
  items,
  loading = false,
  disabled = false,
  placeholder = "Selectează…",
  searchPlaceholder = "Caută…",
  emptyText = "Niciun rezultat.",
  id,
  "aria-label": ariaLabel,
}: ComboboxProps) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  /** Ce rând e „sub deget" pentru tastatură. -1 = niciunul, deci Enter nu alege nimic. */
  const [activeIndex, setActiveIndex] = useState(-1);
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const listId = useId();
  const optionId = (index: number) => `${listId}-opt-${index}`;

  // Debounce the query pushed up to the parent so we don't fetch on every keystroke.
  useEffect(() => {
    const handle = window.setTimeout(() => onQueryChange(query), 250);
    return () => window.clearTimeout(handle);
  }, [query, onQueryChange]);

  // Close on outside click.
  useEffect(() => {
    if (!open) return;
    function onDown(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", onDown);
    return () => document.removeEventListener("mousedown", onDown);
  }, [open]);

  // Focus the search box when opening.
  useEffect(() => {
    if (open) inputRef.current?.focus();
  }, [open]);

  // Rezultatele noi înseamnă altă listă: evidențierea veche ar arăta spre alt rând.
  //
  // Cheia e conținutul, nu tabloul: părintele reconstruiește `items` la fiecare randare
  // (`shownCodes.map(…)`), deci o dependență pe identitatea lui ar reseta evidențierea ori de câte
  // ori se atinge orice altceva din formular.
  const itemsKey = items.map((i) => i.id).join("|");
  useEffect(() => {
    setActiveIndex(itemsKey.length > 0 ? 0 : -1);
  }, [itemsKey]);

  // Ține rândul evidențiat în raza vizibilă: cu 842 de coduri, săgeata jos ajunge repede sub
  // marginea listei, iar fără asta ai naviga în gol. Căutarea e pe `data-index`, nu pe id:
  // id-urile vin din `useId` și conțin două puncte, pe care un selector CSS le-ar lua drept
  // pseudo-clasă.
  useEffect(() => {
    if (!open || activeIndex < 0) return;
    listRef.current
      ?.querySelector(`[data-index="${activeIndex}"]`)
      ?.scrollIntoView({ block: "nearest" });
  }, [activeIndex, open]);

  function close(focusTrigger = true) {
    setOpen(false);
    setQuery("");
    setActiveIndex(-1);
    if (focusTrigger) triggerRef.current?.focus();
  }

  function choose(item: ComboboxItem) {
    onSelect(item);
    close();
  }

  function onKeyDown(e: React.KeyboardEvent) {
    switch (e.key) {
      case "ArrowDown":
        e.preventDefault();
        setActiveIndex((i) => (items.length === 0 ? -1 : (i + 1) % items.length));
        break;
      case "ArrowUp":
        e.preventDefault();
        setActiveIndex((i) => (items.length === 0 ? -1 : (i - 1 + items.length) % items.length));
        break;
      case "Home":
        if (items.length > 0) {
          e.preventDefault();
          setActiveIndex(0);
        }
        break;
      case "End":
        if (items.length > 0) {
          e.preventDefault();
          setActiveIndex(items.length - 1);
        }
        break;
      case "Enter":
        // `preventDefault` neapărat: comboboxul stă în formulare, iar Enter ar trimite formularul
        // în loc să aleagă rândul evidențiat.
        e.preventDefault();
        if (activeIndex >= 0 && items[activeIndex]) choose(items[activeIndex]);
        break;
      case "Escape":
        e.preventDefault();
        close();
        break;
      case "Tab":
        // Tab pleacă mai departe prin formular; lista se închide fără să fure focusul înapoi.
        close(false);
        break;
    }
  }

  return (
    <div ref={containerRef} className="relative">
      <button
        ref={triggerRef}
        type="button"
        id={id}
        role="combobox"
        aria-expanded={open}
        aria-haspopup="listbox"
        aria-controls={open ? listId : undefined}
        aria-label={ariaLabel}
        disabled={disabled}
        onClick={() => setOpen((o) => !o)}
        onKeyDown={(e) => {
          // Săgeata jos deschide lista direct pe primul rând, ca la un <select> nativ.
          if (!open && (e.key === "ArrowDown" || e.key === "ArrowUp")) {
            e.preventDefault();
            setOpen(true);
          }
        }}
        className={cn(
          "flex h-10 w-full items-center justify-between rounded-md border border-line-strong bg-surface px-3 py-2 text-left text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand disabled:opacity-50",
          // Loc pentru butonul de ștergere, care stă deasupra și nu mai e copilul acestuia.
          value ? "pr-16" : "pr-9",
          !value && "text-content-subtle"
        )}
      >
        <span className="truncate">{value ? value.label : placeholder}</span>
      </button>

      {/* Comenzile din dreapta, ca frați ai declanșatorului: un buton în alt buton e HTML
          invalid, iar browserul îl repară mutându-l afară — de unde și „X"-ul care nu se putea
          apăsa cu tastatura. */}
      <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center gap-1 pr-3">
        {value && !disabled && (
          <button
            type="button"
            onClick={() => {
              onSelect(null);
              triggerRef.current?.focus();
            }}
            aria-label="Șterge selecția"
            className="pointer-events-auto rounded text-content-subtle transition-colors hover:text-content-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand"
          >
            <X className="h-4 w-4" />
          </button>
        )}
        <ChevronsUpDown className="h-4 w-4 text-content-subtle" aria-hidden />
      </div>

      {open && (
        <div className="absolute z-50 mt-1 w-full animate-slide-up rounded-md border border-line bg-surface shadow-popover">
          <div className="border-b border-line p-2">
            <input
              ref={inputRef}
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={onKeyDown}
              placeholder={searchPlaceholder}
              aria-label={searchPlaceholder}
              aria-controls={listId}
              aria-activedescendant={activeIndex >= 0 ? optionId(activeIndex) : undefined}
              className="w-full rounded border border-line px-2 py-1.5 text-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-brand"
            />
          </div>
          <div
            ref={listRef}
            id={listId}
            role="listbox"
            aria-busy={loading || undefined}
            className="max-h-56 overflow-y-auto py-1"
          >
            {loading && (
              <div className="flex items-center gap-2 px-3 py-2 text-sm text-content-subtle">
                <Loader2 className="h-4 w-4 animate-spin" aria-hidden />
                Se caută…
              </div>
            )}
            {!loading && items.length === 0 && (
              <div className="px-3 py-2 text-sm text-content-subtle">{emptyText}</div>
            )}
            {!loading &&
              items.map((item, index) => (
                <div
                  key={item.id}
                  id={optionId(index)}
                  data-index={index}
                  role="option"
                  aria-selected={value?.id === item.id}
                  // Mausul mută evidențierea, ca să nu existe două „rânduri active" deodată:
                  // unul sub cursor și altul sub tastatură.
                  onMouseEnter={() => setActiveIndex(index)}
                  onClick={() => choose(item)}
                  className={cn(
                    "flex w-full cursor-pointer items-start justify-between gap-2 px-3 py-2 text-left text-sm",
                    index === activeIndex && "bg-surface-muted"
                  )}
                >
                  <span>
                    <span className="block text-content">{item.label}</span>
                    {item.sublabel && (
                      <span className="block text-xs text-content-subtle">{item.sublabel}</span>
                    )}
                  </span>
                  {value?.id === item.id && (
                    <Check className="mt-0.5 h-4 w-4 shrink-0 text-brand" aria-hidden />
                  )}
                </div>
              ))}
          </div>
        </div>
      )}
    </div>
  );
}
