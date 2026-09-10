import { Fragment } from "react";
import { Link } from "react-router-dom";
import { strings } from "@/lib/strings";
import { cn } from "@/lib/utils";

/**
 * Subsolul cu cele două documente publice.
 *
 * <p>Stă în același loc pe toate paginile pe care se poate ajunge **fără cont** — login, cererea de
 * cont, parola uitată, cele două documente însele — și, în varianta scurtă, în josul barei laterale
 * din aplicație. Un client care caută politica de confidențialitate n-are de unde ști pe ce ecran
 * era când i-a venit ideea, deci n-are rost s-o punem într-un singur loc.
 *
 * <p>`compact` scoate rândul cu firma: în bara laterală n-are loc, iar cine e deja autentificat are
 * datele noastre pe factură.
 */
export function LegalFooter({
  variant = "page",
  className,
}: {
  variant?: "page" | "compact";
  className?: string;
}) {
  const compact = variant === "compact";
  return (
    <footer
      className={cn(
        "text-center",
        compact ? "text-[11px] leading-relaxed" : "text-xs",
        "text-content-subtle",
        className
      )}
    >
      <div className={cn("flex flex-wrap items-center justify-center gap-x-3 gap-y-1", compact && "gap-x-2")}>
        <Link to="/termeni" className="hover:text-brand hover:underline">
          {strings.legal.termsLink}
        </Link>
        <span aria-hidden>·</span>
        <Link to="/confidentialitate" className="hover:text-brand hover:underline">
          {strings.legal.privacyLink}
        </Link>
        {!compact && (
          <>
            <span aria-hidden>·</span>
            <a href={`mailto:${strings.legal.contactEmail}`} className="hover:text-brand hover:underline">
              {strings.legal.contactEmail}
            </a>
          </>
        )}
      </div>
      {!compact && <p className="mt-2">{strings.legal.copyright}</p>}
    </footer>
  );
}

/**
 * Rândul de sub butonul de trimitere al cererii de cont.
 *
 * <p>Propoziția stă întreagă în `strings.ts`, cu două locuri marcate — altfel ar fi trebuit tăiată
 * în cinci bucăți („confirmi că ai citit", „și", „."), iar o propoziție tăiată nu se mai poate nici
 * citi, nici traduce. Se taie **aici**, la randare, exact pe cele două marcaje.
 */
export function LegalNotice({ className }: { className?: string }) {
  const parts = strings.legal.accountRequestNotice.split(/(\{termeni\}|\{confidentialitate\})/);
  return (
    <p className={cn("text-xs text-content-muted", className)}>
      {parts.map((part, i) => {
        if (part === "{termeni}") {
          return (
            <Link key={i} to="/termeni" className="text-brand hover:underline">
              {strings.legal.accountRequestTermsLabel}
            </Link>
          );
        }
        if (part === "{confidentialitate}") {
          return (
            <Link key={i} to="/confidentialitate" className="text-brand hover:underline">
              {strings.legal.accountRequestPrivacyLabel}
            </Link>
          );
        }
        return <Fragment key={i}>{part}</Fragment>;
      })}
    </p>
  );
}
