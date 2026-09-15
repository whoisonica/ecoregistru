import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { ChevronUp, Command, FileText, LogOut, ShieldCheck } from "lucide-react";
import { Dialog } from "@/components/ui/dialog";
import { strings } from "@/lib/strings";
import { cn } from "@/lib/utils";

const t = strings.panel;

/**
 * Contul, jos în panou: inițiala, adresa, rolul; la clic, meniul cu Termeni · Confidențialitate ·
 * Scurtături · Deconectare.
 *
 * <p>Cele două documente legale stăteau ca subsol în bară (`LegalFooter compact`), iar scurtăturile
 * ca un rând de text pe care nu-l citea nimeni. Aici sunt la un clic, nu în drum la fiecare privire.
 */
export function AccountMenu({
  email,
  role,
  onLogout,
  collapsed = false,
  hasCollector,
}: {
  email?: string;
  role?: string;
  onLogout: () => void;
  collapsed?: boolean;
  /** Ca lista de scurtături să arate I/E numai unde există Intrări/Ieșiri. */
  hasCollector: boolean;
}) {
  const [open, setOpen] = useState(false);
  const [shortcuts, setShortcuts] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    function onDown(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    }
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") setOpen(false);
    }
    document.addEventListener("mousedown", onDown);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDown);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  // Prima literă a adresei. Nu e o poză de profil, dar e un reper care se ține minte mai ușor
  // decât un rând de text tăiat la jumătate.
  const initial = (email ?? "?").charAt(0).toUpperCase();
  const roleLabel = role ? (strings.enums.role[role as keyof typeof strings.enums.role] ?? role) : "";

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        aria-label={t.accountMenu}
        aria-haspopup="menu"
        aria-expanded={open}
        className={cn(
          "flex w-full items-center gap-2.5 rounded-md text-left transition-colors hover:bg-panel-hover focus-visible:ring-2 focus-visible:ring-lcd-digit focus-visible:ring-offset-0",
          collapsed ? "justify-center px-0 py-1.5" : "px-1.5 py-1.5"
        )}
      >
        <span className="grid h-8 w-8 shrink-0 place-items-center rounded-md bg-panel-key font-mono text-sm font-medium text-white">
          {initial}
        </span>
        {!collapsed && (
          <>
            <span className="min-w-0 flex-1">
              <span className="block truncate text-xs font-medium text-white">{email}</span>
              <span className="block truncate text-[0.6875rem] text-panel-dim">{roleLabel}</span>
            </span>
            <ChevronUp
              className={cn("h-4 w-4 shrink-0 text-panel-dim transition-transform", !open && "rotate-180")}
              aria-hidden
            />
          </>
        )}
      </button>
      {open && (
        <div
          role="menu"
          className={cn(
            "absolute bottom-full z-30 mb-1 animate-slide-up overflow-hidden rounded-md border border-line-strong bg-surface py-1 text-content shadow-popover",
            collapsed ? "left-0 w-56" : "left-0 w-full"
          )}
        >
          <MenuLink to="/termeni" icon={FileText} onClick={() => setOpen(false)}>
            {strings.legal.termsLink}
          </MenuLink>
          <MenuLink to="/confidentialitate" icon={ShieldCheck} onClick={() => setOpen(false)}>
            {strings.legal.privacyLink}
          </MenuLink>
          <button
            type="button"
            role="menuitem"
            onClick={() => {
              setOpen(false);
              setShortcuts(true);
            }}
            className="hidden w-full items-center gap-2.5 px-3 py-2 text-left text-sm transition-colors hover:bg-surface-muted lg:flex"
          >
            <Command className="h-4 w-4 shrink-0 text-content-subtle" aria-hidden />
            {t.shortcuts}
          </button>
          <div className="my-1 border-t border-line" />
          <button
            type="button"
            role="menuitem"
            onClick={onLogout}
            className="flex w-full items-center gap-2.5 px-3 py-2 text-left text-sm transition-colors hover:bg-surface-muted"
          >
            <LogOut className="h-4 w-4 shrink-0 text-content-subtle" aria-hidden />
            {strings.nav.logout}
          </button>
        </div>
      )}
      {shortcuts && <ShortcutsDialog onClose={() => setShortcuts(false)} hasCollector={hasCollector} />}
    </div>
  );
}

function MenuLink({
  to,
  icon: Icon,
  onClick,
  children,
}: {
  to: string;
  icon: typeof FileText;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <Link
      to={to}
      role="menuitem"
      onClick={onClick}
      className="flex w-full items-center gap-2.5 px-3 py-2 text-left text-sm transition-colors hover:bg-surface-muted"
    >
      <Icon className="h-4 w-4 shrink-0 text-content-subtle" aria-hidden />
      {children}
    </Link>
  );
}

/** Lista scurtăturilor, ca dialog. Tastele sunt cele legate în `Layout` și în ecrane. */
function ShortcutsDialog({ onClose, hasCollector }: { onClose: () => void; hasCollector: boolean }) {
  const rows: [string, string][] = [
    ["1 … 9, 0", t.shortcutNav],
    [t.searchKey, t.shortcutSearch],
    ["/", t.shortcutFind],
    ["N", t.shortcutNew],
    ...(hasCollector ? ([["I", t.shortcutIn], ["E", t.shortcutOut]] as [string, string][]) : []),
    [t.collapseKey, t.shortcutCollapse],
    ["Esc", t.shortcutEscape],
  ];
  return (
    <Dialog open onClose={onClose} title={t.shortcutsTitle} description={t.shortcutsHint} size="md">
      <dl className="divide-y divide-line">
        {rows.map(([key, what]) => (
          <div key={key} className="flex items-center gap-4 py-2.5">
            <dt className="w-24 shrink-0">
              <kbd className="key key-light h-6 px-2 text-xs">{key}</kbd>
            </dt>
            <dd className="text-sm text-content">{what}</dd>
          </div>
        ))}
      </dl>
    </Dialog>
  );
}
