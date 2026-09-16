import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { BrandName } from "@/components/BrandName";
import { LegalFooter } from "@/components/LegalFooter";
import { cn } from "@/lib/utils";

/**
 * Cadrul paginilor de dinaintea contului — direcția „Poster” (aleasă de proprietar pe 16.09.2026
 * din trei machete: Aurora · **Poster** · Editorial).
 *
 * <p>Stânga, verde: semnul, un titlu mare care spune ce e produsul, o propoziție și, dedesubt, ce
 * vrea pagina să arate (tile-urile de pe login, pașii de pe cerere). Dreapta, alb: formularul,
 * aerisit, cu un link în colț spre cealaltă pagină. Pe telefon coloana verde devine o bandă
 * în capul paginii; loginul își ascunde tile-urile acolo, ca formularul să fie la un deget
 * distanță, iar cererea își ține pașii (spun ce urmează după trimitere — proba 5 îi numără).
 *
 * <p>Culorile sunt ale mărcii (`brand-*`, `lcd-digit` pentru lumina difuză), nu scrise de mână;
 * pagina rămâne albă. Subsolul juridic stă o singură dată, jos în dreapta — proba 12 îl caută
 * după `footer a`.
 */
export function PublicShell({
  headline,
  lede,
  aside,
  corner,
  split = "half",
  align = "center",
  children,
}: {
  /** Titlul mare de pe verde. Cu `\n` unde vrei rândul nou. */
  headline: string;
  lede: string;
  /** Ce stă sub titlu pe verde (tile-uri, pași). Cine vrea să-l ascundă pe telefon o face singur. */
  aside?: ReactNode;
  /** Colțul din dreapta sus: „Nu ai cont? Cere un cont”. */
  corner?: ReactNode;
  /** `half` = două coloane egale (login); `narrow` = verde de 480px, restul formular (cerere). */
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
      <section className="relative flex flex-col overflow-hidden bg-gradient-to-br from-brand-400 via-brand-700 to-brand-900 px-6 py-6 text-white lg:px-14 lg:py-10">
        {/* Lumina difuză din colț: verdele deschis al afișajului, întins pe 90px de blur. */}
        <div
          aria-hidden
          className="pointer-events-none absolute -right-40 -top-32 h-[560px] w-[560px] rounded-full bg-lcd-digit/35 blur-[90px]"
        />
        {/* `header`: proba 6 citește de aici al cui e formularul. */}
        <header className="relative">
          <BrandName className="text-lg [&_svg]:h-7 [&_svg]:w-7" />
        </header>
        <div
          className={cn(
            "relative mt-6 flex flex-col gap-4 lg:mt-auto lg:gap-5",
            split === "narrow" && "lg:sticky lg:top-10 lg:mt-20"
          )}
        >
          <h2
            className={cn(
              "max-w-[560px] whitespace-pre-line text-[28px] font-semibold leading-[32px] tracking-[-0.02em] text-white sm:text-[36px] sm:leading-[40px] lg:tracking-[-0.025em]",
              split === "half" ? "lg:text-[56px] lg:leading-[60px]" : "lg:text-[44px] lg:leading-[48px]"
            )}
          >
            {headline}
          </h2>
          <p className="max-w-[460px] text-base leading-relaxed text-white/80 lg:text-lg lg:leading-7">
            {lede}
          </p>
          {aside && <div className="mt-2">{aside}</div>}
        </div>
        {split === "narrow" && <div className="relative mt-auto hidden lg:block" />}
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

/** Linkul din colț: „Nu ai cont? · Cere un cont”. */
export function CornerLink({ prompt, label, to }: { prompt: string; label: string; to: string }) {
  return (
    <span>
      {prompt}{" "}
      <Link to={to} className="font-medium text-brand hover:underline">
        {label}
      </Link>
    </span>
  );
}

/** Tile-ul de pe verdele loginului: eticheta mică, titlul, o notă (cu LED, dacă are). */
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
    <div className="flex min-w-[170px] flex-col gap-1 rounded-2xl border border-white/20 bg-white/10 px-4 py-3.5">
      <span className="font-mono text-[0.6875rem] uppercase tracking-[0.08em] text-white/70">{kicker}</span>
      <span className="font-semibold text-white">{title}</span>
      <span className={cn("flex items-center gap-1.5 text-xs", led ? "text-lcd-digit" : "text-white/70")}>
        {led && <span aria-hidden className="inline-block h-2 w-2 rounded-sm bg-lcd-digit" />}
        {note}
      </span>
    </div>
  );
}

/** Pasul numerotat de pe verdele cererii: „01 · Completezi formularul”. Un `li` — stă într-un `ol`. */
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
  /** Pasul pe care stă omul: cifra aprinsă. */
  current?: boolean;
  /** Pas trecut: cifra aprinsă, iar sub titlu ce a completat (`summary`), dacă e ceva. */
  done?: boolean;
  summary?: string;
}) {
  const lit = current || done;
  return (
    <li className="flex gap-4" aria-current={current ? "step" : undefined}>
      <span className={cn("w-9 shrink-0 font-mono text-[28px] leading-8", lit ? "text-lcd-digit" : "text-white/50")}>
        {String(index).padStart(2, "0")}
      </span>
      <div className="min-w-0">
        <div className={cn("text-[17px] font-semibold", lit ? "text-white" : "text-white/75")}>{title}</div>
        <div className="truncate text-white/75">{done && summary ? summary : body}</div>
      </div>
    </li>
  );
}
