import { Component, type ErrorInfo, type ReactNode } from "react";
import { AlertOctagon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { strings } from "@/lib/strings";
import { reportError } from "@/lib/monitoring";

const t = strings.errorBoundary;

interface Props {
  children: ReactNode;
  /**
   * Când se schimbă, greșeala se uită. E adresa paginii: după o excepție pe „Ambalaje", meniul
   * rămâne viu, iar un clic pe „Mișcări" trebuie să ducă acolo — nu să lase mesajul de eroare pe
   * ecran fiindcă boundary-ul nu știe că s-a schimbat pagina de sub el.
   */
  resetKey?: string;
  /** Randează mesajul pe pagină întreagă, când nu mai există meniu în jur. */
  fullPage?: boolean;
}

interface State {
  error: Error | null;
}

/**
 * Plasa de sub o excepție de randare.
 *
 * <p>Fără ea, orice excepție dintr-o componentă **demontează tot arborele**: React lasă în urmă un
 * `<div id="root">` gol. Adică ecran alb, fără meniu, fără mesaj, fără drum înapoi — pe **orice**
 * ecran, pentru un câmp null pe care nu-l aștepta nimeni. E cel mai ieftin defect de reparat și
 * cel mai scump de trăit: omul nu are ce povesti la telefon în afară de „s-a albit".
 *
 * <p>Stă în două locuri, fiindcă apără de două lucruri diferite: în jurul paginii, sub `Layout`,
 * unde meniul rămâne viu și se poate merge în altă parte; și în jurul aplicației întregi, pentru
 * ce cade **în** `Layout` sau în context, unde nu mai e niciun meniu de păstrat.
 *
 * <p>Clasă, nu hook: `componentDidCatch` n-are echivalent în funcții — e singura bucată din
 * aplicație unde React cere încă o clasă.
 */
export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    // Consola rămâne, fiindcă e ce citește cine dezvoltă, cu tot cu componenta din care a venit.
    console.error("[EcoRegistru] excepție de randare:", error, info.componentStack);
    // Şi, de pe 09.09.2026 (P0.6), pleacă și în afară. Nota de aici spunea până azi „Consola e tot
    // ce avem, și e destul" — adevărat cât timp consola era a noastră. Consola unui client nu ne
    // spune nimic: el nu sună, ci renunță. Fără DSN, linia asta nu face nimic.
    reportError(error, { componentStack: info.componentStack });
  }

  componentDidUpdate(prev: Props) {
    if (this.state.error && prev.resetKey !== this.props.resetKey) {
      this.setState({ error: null });
    }
  }

  render() {
    const { error } = this.state;
    if (!error) return this.props.children;

    const card = (
      <Card className="mx-auto w-full max-w-lg p-8 text-center">
        <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-red-100">
          <AlertOctagon className="h-6 w-6 text-red-600" aria-hidden />
        </div>
        <h1 className="text-lg font-semibold text-content">{t.title}</h1>
        <p className="mt-2 text-sm text-content-muted">{t.body}</p>
        {/* Textul excepției, închis. Ajută la telefon — „scrie ceva cu «undefined»?" — dar nu e
            ce trebuie să citească primul cineva care voia doar să înregistreze o predare. */}
        <details className="mt-4 text-left">
          <summary className="cursor-pointer text-xs text-content-subtle">{t.details}</summary>
          <p className="mt-2 break-words rounded-md bg-surface-muted px-3 py-2 font-mono text-xs text-content-subtle">
            {error.message || String(error)}
          </p>
        </details>
        <div className="mt-6 flex flex-col justify-center gap-2 sm:flex-row">
          {/* Reîncărcare adevărată, nu `setState`: după o excepție de randare, starea din memorie
              e chiar cea care a produs-o. */}
          <Button onClick={() => window.location.reload()}>{t.reload}</Button>
          {/* `<a>`, nu `<Link>`: boundary-ul de sus stă în afara routerului. */}
          <a href="/">
            <Button variant="outline" className="w-full sm:w-auto">
              {t.toDashboard}
            </Button>
          </a>
        </div>
      </Card>
    );

    if (this.props.fullPage) {
      return <div className="flex min-h-screen items-center justify-center p-4">{card}</div>;
    }
    return <div className="py-10">{card}</div>;
  }
}
