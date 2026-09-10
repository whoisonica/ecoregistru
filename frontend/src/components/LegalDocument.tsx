import { Fragment, useEffect, type ReactNode } from "react";
import { Link, useLocation } from "react-router-dom";
import { strings } from "@/lib/strings";
import { LEGAL_DATE, type LegalBlock, type LegalDoc } from "@/lib/legal";
import { LegalFooter } from "@/components/LegalFooter";

/**
 * Marcaj de rând, cât să nu scriem documentele în JSX.
 *
 * <p>Trei semne, exact cele care apar în text: `**îngroșat**`, `` `cod` `` și `[text](adresă)`.
 * Nu e markdown și nu vrea să fie — o bibliotecă întreagă adusă pentru trei semne ar fi însemnat
 * un pachet nou în manifest și `dangerouslySetInnerHTML` pe un text pe care oricum îl scriem noi.
 * Aici textul rămâne text: nimic nu se transformă în HTML, deci nu are ce injecta nimeni.
 */
function inline(text: string): ReactNode[] {
  const out: ReactNode[] = [];
  const pattern = /\*\*(.+?)\*\*|`(.+?)`|\[(.+?)\]\((https?:\/\/[^\s)]+)\)/g;
  let last = 0;
  let m: RegExpExecArray | null;
  let key = 0;
  while ((m = pattern.exec(text)) !== null) {
    if (m.index > last) out.push(text.slice(last, m.index));
    if (m[1] !== undefined) {
      out.push(
        <strong key={key++} className="font-semibold text-content-strong">
          {m[1]}
        </strong>
      );
    } else if (m[2] !== undefined) {
      out.push(
        <code key={key++} className="rounded bg-surface-sunken px-1 py-0.5 font-mono text-[0.85em]">
          {m[2]}
        </code>
      );
    } else {
      out.push(
        <a
          key={key++}
          href={m[4]}
          target="_blank"
          rel="noreferrer noopener"
          className="text-brand underline underline-offset-2"
        >
          {m[3]}
        </a>
      );
    }
    last = pattern.lastIndex;
  }
  if (last < text.length) out.push(text.slice(last));
  return out;
}

function Block({ block }: { block: LegalBlock }) {
  switch (block.kind) {
    case "p":
      return <p className="text-content">{inline(block.text)}</p>;
    case "h3":
      return <h3 className="pt-2 font-semibold text-content-strong">{inline(block.text)}</h3>;
    case "list":
      return (
        <ul className="list-disc space-y-1.5 pl-5 text-content marker:text-content-subtle">
          {block.items.map((item, i) => (
            <li key={i}>{inline(item)}</li>
          ))}
        </ul>
      );
    case "note":
      return (
        <p className="rounded-md border-l-4 border-amber-300 bg-amber-50 px-4 py-3 text-amber-900">
          {inline(block.text)}
        </p>
      );
    case "table":
      // Tabelele astea au patru coloane de text lung, deci pe telefon nu încap oricâte clase
      // le-am pune. Derulează **containerul**, nu pagina — regula de la tabelele din aplicație.
      return (
        <div className="-mx-1 overflow-x-auto">
          <table className="w-full min-w-[36rem] border-collapse text-sm">
            <thead>
              <tr>
                {block.head.map((h, i) => (
                  <th
                    key={i}
                    scope="col"
                    className="border-b border-line px-3 py-2 text-left align-bottom font-semibold text-content-strong"
                  >
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {block.rows.map((row, i) => (
                <tr key={i} className="align-top">
                  {row.map((cell, j) => (
                    <td key={j} className="border-b border-line px-3 py-2 text-content">
                      {inline(cell)}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      );
  }
}

/**
 * Ecranul pe care se citește un document juridic — termenii sau politica.
 *
 * <p>Public, dinadins în afara lui `AppShell`: cine cere DPA-ul înainte de demo n-are cont, iar un
 * document pe care trebuie să te autentifici ca să-l citești nu e publicat. Nu cere nimic de la
 * server, deci se deschide și cu backendul căzut.
 *
 * <p>Are cuprins fiindcă termenii au șaisprezece capitole și trimiterile se fac pe număr
 * („capitolul 5"): fiecare capitol are ancoră, deci se poate da un link direct la el.
 */
export function LegalDocument({ doc }: { doc: LegalDoc }) {
  const { hash } = useLocation();

  // Un `<a href="#raspundere">` din cuprins derulează singur — dar `/termeni#raspundere` deschis
  // din afară nu, fiindcă la prima randare ținta încă nu există în pagină.
  useEffect(() => {
    if (!hash) return;
    document.getElementById(decodeURIComponent(hash.slice(1)))?.scrollIntoView();
  }, [hash]);

  return (
    <div className="min-h-screen bg-surface-muted">
      <div className="mx-auto max-w-3xl px-4 py-10">
        <header className="text-center">
          <Link to="/login" className="inline-block">
            <div className="text-2xl font-bold text-brand">{strings.appName}</div>
            <div className="text-sm text-content-muted">{strings.tagline}</div>
          </Link>
        </header>

        <h1 className="mt-10 text-2xl font-bold text-content-strong">{doc.title}</h1>
        <p className="mt-1 text-sm text-content-muted">
          {doc.dateLabel}: <strong className="font-semibold text-content">{LEGAL_DATE}</strong>
        </p>

        <nav aria-labelledby="cuprins" className="mt-8 rounded-xl border border-line bg-surface p-5">
          <h2 id="cuprins" className="text-xs font-semibold uppercase tracking-wide text-content-muted">
            {strings.legal.contents}
          </h2>
          {/* Două coloane de text, nu o grilă de două celule pe rând: o grilă umple pe rând, deci
              coloana din stânga ar fi ieșit 1, 3, 5, 7 — o numerotare care sare, pe singurul ecran
              unde numărul capitolului chiar se folosește. Coloanele CSS umplu de sus în jos și nu
              cer să știm dinainte câte capitole are documentul. */}
          <ol className="mt-3 text-sm sm:columns-2 sm:gap-x-8">
            {doc.sections.map((s) => (
              <li key={s.id} className="break-inside-avoid pb-1.5">
                <a href={`#${s.id}`} className="text-brand hover:underline">
                  {s.heading}
                </a>
              </li>
            ))}
          </ol>
        </nav>

        <article className="mt-8 space-y-10">
          {doc.sections.map((section) => (
            <section key={section.id} id={section.id} className="scroll-mt-6">
              <h2 className="text-lg font-semibold text-content-strong">{section.heading}</h2>
              <div className="mt-3 space-y-3 text-[0.9375rem] leading-relaxed">
                {section.blocks.map((block, i) => (
                  <Fragment key={i}>
                    <Block block={block} />
                  </Fragment>
                ))}
              </div>
            </section>
          ))}
        </article>

        <div className="mt-12 border-t border-line pt-6">
          <LegalFooter />
        </div>
      </div>
    </div>
  );
}
