import { RotateCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { strings } from "@/lib/strings";
import { cn } from "@/lib/utils";

interface LoadErrorProps {
  /** De ce e gol, în cuvintele ecranului: „Nu am putut încărca mișcările." */
  message: string;
  /** Reîncercarea. Lipsește numai acolo unde chiar n-avem ce reîncerca. */
  onRetry?: () => void;
  className?: string;
}

/**
 * „Nu am putut încărca …”, cu o ieșire.
 *
 * <p>Până acum fiecare ecran scria o propoziție roșie și atât. Cu `retry: 1` pe TanStack Query,
 * asta însemna că un hiccup de rețea lăsa ecranul mort până la F5 — iar omul nu avea de unde ști
 * că F5 ajută: pagina nu spunea că mai e ceva de încercat. Butonul face din fundătură un pas.
 *
 * <p>`role="alert"` fiindcă textul apare după ce omul se uită deja la ecran, deci un cititor de
 * ecran n-ar afla altfel că s-a schimbat ceva. Culorile vin din tokeni (`state.bad`), nu scrise de
 * mână: locurile astea erau împărțite între `text-red-600` și `text-state-bad-text`, pentru exact
 * aceeași stare.
 */
export function LoadError({ message, onRetry, className }: LoadErrorProps) {
  return (
    <div
      role="alert"
      className={cn("flex flex-wrap items-center gap-3 text-sm text-state-bad-text", className)}
    >
      <span>{message}</span>
      {onRetry && (
        <Button type="button" variant="outline" size="sm" onClick={() => onRetry()}>
          <RotateCw className="h-4 w-4" aria-hidden />
          {strings.common.retry}
        </Button>
      )}
    </div>
  );
}
