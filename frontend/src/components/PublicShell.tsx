import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { BrandName } from "@/components/BrandName";
import { LegalFooter } from "@/components/LegalFooter";
import { strings } from "@/lib/strings";
import { cn } from "@/lib/utils";

/**
 * Cadrul paginilor de dinaintea contului — direcția „Poster” (aleasă de proprietar pe 16.09.2026
 * din trei machete: Aurora · **Poster** · Editorial), în **paleta landingului** (tot 16.09, seara:
 * coloana verde plină „e prea deranjantă”, „culorile din landing îmi plac”).
 *
 * <p>Stânga, pe hârtie deschisă (`surface-muted`): semnul, un rând mic verde cu linie (kicker-ul de
 * pe landing), un titlu mare negru cu ultima frază în verdele adânc al mărcii (`mark`), o
 * propoziție și, dedesubt, ce vrea pagina să arate (tile-urile de pe login, pașii de pe cerere).
 * Dreapta, alb: formularul, aerisit, cu un link în colț spre cealaltă pagină. Pe telefon coloana
 * din stânga devine o bandă în capul paginii; loginul își ascunde tile-urile acolo, ca formularul să
 * fie la un deget distanță, iar cererea își ține pașii (spun ce urmează — proba 5 îi numără).
 *
 * <p>Subsolul juridic stă o singură dată, jos în dreapta — proba 12 îl caută după `footer a`.
 */
export function PublicShell({
  headline,
  accent,
  lede,
  aside,
  corner,
  split = "half",
  align = "center",
  children,
}: {
  /** Titlul mare, negru. Cu `\n` unde vrei rândul nou. */
  headline: string;
  /** Ultima frază a titlului, în verdele mărcii — ca pe landing („Fără să le mai ții minte.”). */
  accent?: string;
  lede: string;
  /** Ce stă sub titlu (tile-uri, pași). Cine vrea să-l ascundă pe telefon o face singur. */
  aside?: ReactNode;
  /** Colțul din dreapta sus: „Nu ai cont? Cere un cont”. */
  corner?: ReactNode;
  /** `half` = două coloane egale (login); `narrow` = stânga de 480px, restul formular (cerere). */
  split?: "half" | "narrow";
  /** Conținutul din dreapta, centrat pe verticală (login) sau de sus (formular lung). */
  align?: "center" | "top";
  children: ReactNode;
}) {
  return (
    <div
      className={cn(
        "min-h-full bg-surface lg:grid",
        split === "half" ? "lg:grid-cols-2" : "lg:grid-cols-[480px_minmax(0,1fr)]"
      )}
    >
      <section className="flex flex-col border-b border-line bg-surface-muted px-6 py-6 lg:border-b-0 lg:border-r lg:px-14 lg:py-10">
        {/* `header`: proba 6 citește de aici al cui e formularul. */}
        <header>
          <BrandName className="text-lg [&_svg]:h-7 [&_svg]:w-7" />
        </header>
        <div
          className={cn(
            "mt-8 flex flex-col gap-4 lg:mt-auto lg:gap-5",
            split === "narrow" && "lg:sticky lg:top-10 lg:mt-20"
          )}
        >
          <div className="flex items-center gap-2.5 font-mono text-xs font-semibold uppercase tracking-[0.14em] text-mark">
            <span aria-hidden className="h-0.5 w-7 bg-mark" />
            {strings.publicKicker}
          </div>
          <h2
            className={cn(
              "max-w-[560px] whitespace-pre-line text-[30px] font-extrabold leading-[1.06] tracking-[-0.03em] text-content sm:text-[38px]",
              split === "half" ? "lg:text-[56px]" : "lg:text-[44px]"
            )}
          >
            {headline}
            {accent && (
              <>
                {" "}
                <span className="text-mark">{accent}</span>
              </>
            )}
          </h2>
          <p className="max-w-[460px] text-base leading-relaxed text-content-muted lg:text-lg lg:leading-7">
            {lede}
          </p>
          {aside && <div className="mt-2">{aside}</div>}
        </div>
      </section>

      <section
        className={cn(
          "flex min-h-[70vh] flex-col px-6 py-8 sm:px-10 lg:min-h-0 lg:px-14 lg:py-10",
          align === "center" && "lg:justify-between"
        )}
      >
        {corner && <div className="flex justify-end text-sm text-content-muted">{corner}</div>}
        <div className={cn("flex flex-1 flex-col", align === "center" ? "justify-center py-10" : "pt-8")}>
          {children}
        </div>
        <LegalFooter className="mt-10 lg:mt-6" />
      </section>
    </div>
  );
}

/** Butonul principal al paginilor publice: verdele landingului, nu al aplicației. */
export const publicButtonClass = "border-mark bg-mark hover:border-mark-hover hover:bg-mark-hover";

/** Linkul din colț: „Nu ai cont? · Cere un cont”. */
export function CornerLink({ prompt, label, to }: { prompt: string; label: string; to: string }) {
  return (
    <span>
      {prompt}{" "}
      <Link to={to} className="font-medium text-mark hover:underline">
        {label}
      </Link>
    </span>
  );
}

/** Tile-ul de sub titlul loginului: eticheta mică, titlul, o notă (cu punct verde, dacă are). */
export function PosterTile({
  kicker,
  title,
  note,
  led = false,
}: {
  kicker: string;
  title: string;
  note: string;
  led?: boolean;
}) {
  return (
    <div className="flex min-w-[170px] flex-col gap-1 rounded-2xl border border-line bg-surface px-4 py-3.5">
      <span className="font-mono text-[0.6875rem] uppercase tracking-[0.08em] text-content-subtle">{kicker}</span>
      <span className="text-lg font-semibold tracking-[-0.02em] text-content">{title}</span>
      <span className={cn("flex items-center gap-1.5 text-xs", led ? "text-mark" : "text-content-muted")}>
        {led && <span aria-hidden className="inline-block h-1.5 w-1.5 rounded-full bg-mark" />}
        {note}
      </span>
    </div>
  );
}

/** Pasul numerotat de pe cerere: „01 · Compania”. Un `li` — stă într-un `ol`. */
export function PosterStep({
  index,
  title,
  body,
  current = false,
  done = false,
  summary,
}: {
  index: number;
  title: string;
  body: string;
  /** Pasul pe care stă omul: cifra verde. */
  current?: boolean;
  /** Pas trecut: cifra verde, iar sub titlu ce a completat (`summary`), dacă e ceva. */
  done?: boolean;
  summary?: string;
}) {
  const lit = current || done;
  return (
    <li className="flex gap-4" aria-current={current ? "step" : undefined}>
      <span className={cn("w-9 shrink-0 font-mono text-[28px] leading-8", lit ? "text-mark" : "text-content-subtle")}>
        {String(index).padStart(2, "0")}
      </span>
      <div className="min-w-0">
        <div className={cn("text-[17px] font-semibold", lit ? "text-content" : "text-content-muted")}>{title}</div>
        <div className={cn("text-content-muted", done && summary && "truncate")}>{done && summary ? summary : body}</div>
      </div>
    </li>
  );
}

/** Rândul mic cu punct verde, ca „faptele” de sub titlul landingului. */
export function PosterFact({ children }: { children: ReactNode }) {
  return (
    <div className="flex items-center gap-2.5 text-xs text-content-muted">
      <span aria-hidden className="inline-block h-1.5 w-1.5 shrink-0 rounded-full bg-mark" />
      {children}
    </div>
  );
}
