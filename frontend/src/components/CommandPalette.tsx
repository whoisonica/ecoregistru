import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { createPortal } from "react-dom";
import { CornerDownLeft, Search, type LucideIcon } from "lucide-react";
import { useHotkey } from "@/hooks/useHotkey";
import { strings } from "@/lib/strings";
import { cn } from "@/lib/utils";

export interface Command {
  id: string;
  label: string;
  /** Cuvintele după care se mai poate găsi: „fișa", „anexa 1", „cod R/D". */
  keywords?: string;
  icon?: LucideIcon;
  group: string;
  run: () => void;
}

/**
 * Paleta de comenzi — Ctrl+K.
 *
 * <p>Aplicația are nouă ecrane și se folosește toată ziua. Ajungerea pe oricare dintre ele cerea
 * o plimbare a mâinii la maus și un ochi pe bara laterală, la fiecare schimbare de context.
 *
 * <p>Nu caută **în date** — nu e un motor de căutare peste mișcări și parteneri, fiindcă acelea au
 * fiecare căutarea lor, pe ecranul lor, unde se și pot filtra. Caută în **locuri și acțiuni**:
 * unde vreau să ajung, ce vreau să încep.
 */
export function CommandPalette({ commands }: { commands: Command[] }) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [activeIndex, setActiveIndex] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLDivElement>(null);

  useHotkey("k", () => setOpen((o) => !o), { ctrl: true, whileTyping: true });
  useHotkey("Escape", () => setOpen(false), { whileTyping: true, enabled: open });

  useEffect(() => {
    if (open) {
      setQuery("");
      setActiveIndex(0);
      // Focusul după vopsire: câmpul nu există în DOM la momentul apăsării tastei.
      requestAnimationFrame(() => inputRef.current?.focus());
    }
  }, [open]);

  /**
   * Ce se potriveşte, **în ordinea cât de bine**.
   *
   * <p>Probat pe 07.09.2026: tastând „evid", paleta evidenţia *Mişcări*. Numele grupului intra în
   * textul căutat, iar grupul lui Mişcări e „Evidenţă" — deci toate cele trei intrări din grup se
   * potriveau, iar prima din listă lua evidenţierea. Apăsai Enter aşteptând Evidenţe şi rămâneai
   * unde erai.
   *
   * <p>Grupul rămâne căutabil (e util să tastezi „raportare" şi să vezi ce e acolo), dar cântăreşte
   * cel mai puţin. Ordinea: eticheta începe cu ce ai scris, eticheta conţine, cuvintele-cheie,
   * grupul.
   */
  const matches = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return commands;
    const words = q.split(/\s+/);
    const scored: { command: Command; score: number }[] = [];
    for (const c of commands) {
      const label = c.label.toLowerCase();
      const keywords = (c.keywords ?? "").toLowerCase();
      const group = c.group.toLowerCase();
      const haystack = `${label} ${keywords} ${group}`;
      if (!words.every((w) => haystack.includes(w))) continue;
      const score = label.startsWith(q)
        ? 0
        : label.includes(q)
          ? 1
          : keywords.includes(q)
            ? 2
            : 3;
      scored.push({ command: c, score });
    }
    // `sort` e stabilă în JS modern, deci la scor egal rămâne ordinea din bara laterală.
    return scored.sort((a, b) => a.score - b.score).map((x) => x.command);
  }, [commands, query]);

  // O listă nouă înseamnă alt prim rând; evidențierea veche ar arăta spre altceva.
  useEffect(() => {
    setActiveIndex(0);
  }, [query]);

  useEffect(() => {
    listRef.current
      ?.querySelector(`[data-index="${activeIndex}"]`)
      ?.scrollIntoView({ block: "nearest" });
  }, [activeIndex]);

  function run(command: Command) {
    setOpen(false);
    command.run();
  }

  if (!open) return null;

  // Grupurile păstrează ordinea în care au venit comenzile: e ordinea din bara laterală, adică
  // singura pe care omul o are deja în cap.
  const groups: { name: string; items: { command: Command; index: number }[] }[] = [];
  matches.forEach((command, index) => {
    const last = groups[groups.length - 1];
    if (last && last.name === command.group) last.items.push({ command, index });
    else groups.push({ name: command.group, items: [{ command, index }] });
  });

  return createPortal(
    <div className="fixed inset-0 z-[70] flex items-start justify-center p-4 pt-[10vh]">
      <div
        className="absolute inset-0 animate-fade-in bg-black/40"
        onClick={() => setOpen(false)}
        aria-hidden
      />
      <div
        role="dialog"
        aria-modal="true"
        aria-label={strings.common.commandPalette}
        className="relative z-10 w-full max-w-lg animate-slide-up overflow-hidden rounded-xl border border-line bg-surface shadow-xl"
      >
        <div className="flex items-center gap-2 border-b border-line px-4">
          <Search className="h-4 w-4 shrink-0 text-content-subtle" aria-hidden />
          <input
            ref={inputRef}
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "ArrowDown") {
                e.preventDefault();
                setActiveIndex((i) => (matches.length === 0 ? 0 : (i + 1) % matches.length));
              } else if (e.key === "ArrowUp") {
                e.preventDefault();
                setActiveIndex((i) =>
                  matches.length === 0 ? 0 : (i - 1 + matches.length) % matches.length
                );
              } else if (e.key === "Enter" && matches[activeIndex]) {
                e.preventDefault();
                run(matches[activeIndex]);
              }
            }}
            placeholder={strings.common.commandPalettePlaceholder}
            aria-label={strings.common.commandPalette}
            className="h-12 w-full bg-transparent text-sm outline-none placeholder:text-content-subtle"
          />
        </div>

        <div ref={listRef} role="listbox" className="max-h-80 overflow-y-auto py-2">
          {matches.length === 0 && (
            <p className="px-4 py-6 text-center text-sm text-content-subtle">
              {strings.common.noResults}
            </p>
          )}
          {groups.map((group) => (
            <div key={group.name}>
              <div className="px-4 pb-1 pt-2 text-[11px] font-semibold uppercase tracking-wide text-content-subtle">
                {group.name}
              </div>
              {group.items.map(({ command, index }) => (
                <div
                  key={command.id}
                  data-index={index}
                  role="option"
                  aria-selected={index === activeIndex}
                  onMouseEnter={() => setActiveIndex(index)}
                  onClick={() => run(command)}
                  className={cn(
                    "flex cursor-pointer items-center gap-3 px-4 py-2 text-sm",
                    index === activeIndex ? "bg-surface-muted text-content" : "text-content-muted"
                  )}
                >
                  {command.icon && <command.icon className="h-4 w-4 shrink-0" aria-hidden />}
                  <span className="flex-1 truncate">{command.label}</span>
                  {index === activeIndex && (
                    <CornerDownLeft className="h-3.5 w-3.5 shrink-0 text-content-subtle" aria-hidden />
                  )}
                </div>
              ))}
            </div>
          ))}
        </div>
      </div>
    </div>,
    document.body
  );
}

/**
 * Comenzile de navigare, construite din aceleași grupuri ca bara laterală.
 *
 * <p>Se ia lista de acolo, nu una scrisă a doua oară: două liste care trebuie să spună același
 * lucru ajung mereu să nu-l mai spună.
 */
export function useNavigationCommands(
  groups: { label?: string; items: { to: string; label: string; icon: LucideIcon }[] }[]
): Command[] {
  const navigate = useNavigate();
  return useMemo(
    () =>
      groups.flatMap((group) =>
        group.items.map((item) => ({
          id: `nav:${item.to}`,
          label: item.label,
          group: group.label ?? strings.common.goTo,
          icon: item.icon,
          run: () => navigate(item.to),
        }))
      ),
    [groups, navigate]
  );
}
