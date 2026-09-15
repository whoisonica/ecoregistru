import { binFor, type Bin } from "@/lib/binColor";
import { cn } from "@/lib/utils";

const BIN_CLASS: Record<Bin, string> = {
  paper: "bg-bin-paper",
  plastic: "bg-bin-plastic",
  glass: "bg-bin-glass",
  bio: "bg-bin-bio",
  residual: "bg-bin-residual",
  hazard: "bg-bin-hazard",
  metal: "bg-bin-metal",
};

/**
 * Pătrățelul pubelei dinaintea unui cod de deșeu: 10×12px, cu „capac" (umbra de sus). Decorativ
 * pentru cititorul de ecran — codul de lângă el spune tot; culoarea e un reper pentru ochi.
 *
 * <p>Randează **nimic** pentru un cod care nu e în listă, nu un pătrat gol: un loc gol înaintea
 * codului ar fi o afirmație („n-are pubelă") pe care lista n-o face.
 */
export function BinSwatch({
  code,
  hazardous,
  className,
}: {
  code: string;
  hazardous?: boolean;
  className?: string;
}) {
  const bin = binFor(code, hazardous);
  if (!bin) return null;
  return (
    <span
      aria-hidden
      data-bin={bin}
      className={cn(
        "mr-2 inline-block h-3 w-2.5 shrink-0 rounded-[1px_1px_3px_3px] align-[-1px] shadow-[inset_0_2px_0_rgb(0_0_0_/_0.22)]",
        BIN_CLASS[bin],
        className
      )}
    />
  );
}
