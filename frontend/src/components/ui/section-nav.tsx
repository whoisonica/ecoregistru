import { useEffect, useMemo, useState } from "react";
import { cn } from "@/lib/utils";

export interface SectionNavItem {
  /** `id`-ul secțiunii din pagină. Secțiunea are nevoie de `scroll-mt-*`, ca bara să n-o taie. */
  id: string;
  label: string;
}

/** Zona care se derulează în jurul unui element — în aplicație e `<main>`, nu fereastra. */
function scrollParentOf(el: HTMLElement): HTMLElement | null {
  for (let node = el.parentElement; node; node = node.parentElement) {
    const overflow = getComputedStyle(node).overflowY;
    if ((overflow === "auto" || overflow === "scroll") && node.scrollHeight > node.clientHeight) {
      return node;
    }
  }
  return null;
}

/** Cât de sus trebuie să ajungă capul unei secțiuni ca să fie considerată cea curentă. */
const BAND_PX = 120;

/**
 * Cuprinsul unei pagini lungi, lipicios sub antet.
 *
 * <p>„Setări" are patru secțiuni, trei dintre ele tabele cu paginare: cine intra pentru șoferi
 * derula prin tot. Ambalaje are patru tabele și o grilă de 66 de celule, cea mai lungă pagină din
 * aplicație — de asta cuprinsul e o primitivă, nu un `<nav>` scris în „Setări".
 *
 * <p>Secțiunea curentă se marchează la derulare, nu la clic: cuprinsul trebuie să spună unde ești
 * și când derulezi cu rotița, altfel e o listă de linkuri, nu o hartă.
 */
export function SectionNav({
  items,
  label,
  className,
}: {
  items: SectionNavItem[];
  /** Citit de cititorul de ecran, și scris ca etichetă a barei. */
  label: string;
  className?: string;
}) {
  const [active, setActive] = useState(items[0]?.id ?? "");
  // Lista vine ca literal din pagină, deci are altă identitate la fiecare randare — iar un efect
  // legat de ea s-ar dezlega și s-ar relega la fiecare. Cheia e conținutul, nu tabloul.
  const key = items.map((item) => item.id).join(",");
  const ids = useMemo(() => (key ? key.split(",") : []), [key]);

  useEffect(() => {
    const elements = ids
      .map((id) => document.getElementById(id))
      .filter((el): el is HTMLElement => el !== null);
    if (elements.length === 0) return;
    const scroller = scrollParentOf(elements[0]);

    /**
     * Ultima secțiune al cărei cap a trecut de banda de sus.
     *
     * <p>Prima variantă folosea `IntersectionObserver` și marca cea mai de sus secțiune **vizibilă**
     * — ceea ce se vede greșit tocmai la capătul de jos al paginii: ultima secțiune nu ajunge
     * niciodată sus, fiindcă pagina se termină înaintea ei, deci clicul pe „Șoferii noștri" ducea
     * acolo lăsând marcajul pe „Puncte de lucru". Văzut cu ochiul pe o captură, nu de o verificare
     * de DOM.
     */
    function update() {
      const bottom = scroller
        ? scroller.scrollTop + scroller.clientHeight >= scroller.scrollHeight - 4
        : window.innerHeight + window.scrollY >= document.body.scrollHeight - 4;
      if (bottom) {
        setActive(elements[elements.length - 1].id);
        return;
      }
      let current = elements[0].id;
      for (const el of elements) {
        if (el.getBoundingClientRect().top <= BAND_PX) current = el.id;
      }
      setActive(current);
    }

    update();
    const target: HTMLElement | Window = scroller ?? window;
    target.addEventListener("scroll", update, { passive: true });
    window.addEventListener("resize", update);
    return () => {
      target.removeEventListener("scroll", update);
      window.removeEventListener("resize", update);
    };
  }, [ids]);

  return (
    <nav
      aria-label={label}
      className={cn(
        // Poziţia de lipire se numără de la **marginea conţinutului** zonei care se derulează, nu
        // de la marginea ei de sus: `<main>` are `pt-[4.5rem]` pe telefon şi `p-8` pe ecran mare,
        // iar căptuşeala aia se adună la `top`. De aceea `-top-4` sub `lg` — 72px de căptuşeală
        // minus 16px — lipeşte bara exact sub antetul `fixed` de 56px, în loc s-o lase la 128px
        // cu conţinut curgând prin gaură (măsurat pe captură, la 375px).
        //
        // Fundal opac, nu translucid: bara stă peste rânduri de tabel, iar prin ea se citeau
        // cifre pe jumătate.
        //
        // `::before` acoperă spațiul dintre marginea de sus a zonei care se derulează și bara
        // lipită — căptușeala paginii, 16px sub `lg` și 32px de la `lg` în sus. Fără el, printre
        // ele trecea o dungă de tabel, cu jumătăți de rând vizibile deasupra barei. E vopsit cu
        // fundalul paginii, nu cu alb, ca să nu se vadă cât timp bara nu e lipită.
        // Derularea laterală stă pe `<ul>`, nu aici: `overflow-x-auto` decupează și pe verticală,
        // deci pusă pe `<nav>` ar tăia chiar banda de mai jos.
        // `z-30`, nu `z-10`: coloana de acțiuni a tabelelor e și ea lipită (`sticky right-0 z-10`) și
        // vine **după** bară în DOM, deci la z egal ea câștiga — jumătatea din dreapta a barei era
        // acoperită de butoanele „Editează / Dezactivează" ale rândului care trecea pe sub ea.
        // Capul de tabel lipit e `z-20`, deci bara trebuie să treacă și de el.
        "sticky -top-4 z-30 -mx-4 mt-6 border-b border-line bg-surface px-4 py-2 shadow-sm lg:top-0",
        "before:pointer-events-none before:absolute before:inset-x-0 before:bottom-full before:h-4 before:bg-surface-muted lg:before:h-8",
        // Fără `relative`: `sticky` e deja poziționat, iar `cn` (tailwind-merge) ar păstra doar
        // ultima clasă de poziție — bara ar înceta să se lipească.
        "sm:mx-0 sm:rounded-lg sm:border sm:px-2",
        className
      )}
    >
      {/* Derularea laterală stă pe învelişul ăsta, nu pe `<nav>`: `overflow-x-auto` decupează şi
          pe verticală, iar pe `<nav>` ar tăia banda de deasupra. Nici pe `<ul>` nu poate sta —
          `min-w-max` îi dă lăţimea conţinutului, deci lista ar ieşi din bară şi ar face pagina să
          se deruleze lateral pe telefon (probat: 129px pe „Setări", la 375px). */}
      <div className="overflow-x-auto">
        <ul className="flex min-w-max gap-1">
          {items.map((item) => (
            <li key={item.id}>
              <a
                href={`#${item.id}`}
                aria-current={active === item.id ? "true" : undefined}
                className={cn(
                  "block whitespace-nowrap rounded-md px-3 py-1.5 text-sm transition-colors",
                  active === item.id
                    ? "bg-surface-sunken font-medium text-content"
                    : "text-content-muted hover:bg-surface-sunken hover:text-content"
                )}
              >
                {item.label}
              </a>
            </li>
          ))}
        </ul>
      </div>
    </nav>
  );
}
