import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";
import { TD, TR } from "@/components/ui/table";
import { EmptyState } from "@/components/ui/empty-state";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

/**
 * Ce se vede într-un tabel când nu sunt rânduri de arătat: schelet cât se încarcă, o stare goală
 * care spune ceva după.
 *
 * <p>Aplicația avea două feluri de a răspunde la asta, și amândouă puse pe lângă tabel, nu în el.
 * Încărcarea era `{isLoading && <p>Se încarcă…</p>}` **deasupra** locului unde avea să apară
 * tabelul, deci pagina sărea când veneau datele. Golul era când o casetă punctată cu explicație
 * (Termene, Evidențe), când un `<td colSpan>` gri fără nicio ieșire (Mișcări, Setări, Parteneri) —
 * iar al doilea lasă omul să se întrebe dacă s-a stricat ceva.
 *
 * <p>Stă **înăuntrul** lui `<TBody>`, ca rânduri: de asta întoarce un fragment de `<TR>`, nu un
 * `<TBody>` propriu. Un tabel cu două `tbody`-uri e valid, dar dungile alternate și chenarele
 * dintre rânduri o iau razna la granița dintre ele.
 */
export function TableFallbackRow({
  columns,
  loading,
  icon,
  title,
  description,
  action,
  skeletonRows = 5,
}: {
  columns: number;
  loading: boolean;
  icon?: LucideIcon;
  title: string;
  description?: ReactNode;
  /** Butonul care umple golul. Lipsește la un cont care oricum n-ar avea voie să-l apese. */
  action?: ReactNode;
  skeletonRows?: number;
}) {
  if (loading) return <TableSkeletonRows columns={columns} rows={skeletonRows} />;

  return (
    <TR className="hover:bg-transparent">
      <TD colSpan={columns} className="px-4 py-10">
        <EmptyState
          icon={icon}
          title={title}
          description={description}
          action={action}
          // Chenarul îl dă tabelul din jur; aici ar fi o casetă într-o casetă.
          className="border-0 bg-transparent px-0 py-0"
        />
      </TD>
    </TR>
  );
}

/**
 * Doar rândurile-fantomă, pentru tabelele care n-au niciodată stare goală — tabelul 1 de la
 * Ambalaje randează mereu cele opt materiale, pline sau nu.
 *
 * <p>Lățimile alternează: un tabel real n-are toate celulele la fel de pline, iar un dreptunghi
 * perfect arată a grilă tipărită, nu a date care vin.
 */
export function TableSkeletonRows({ columns, rows = 5 }: { columns: number; rows?: number }) {
  const widths = ["w-24", "w-32", "w-20", "w-28", "w-16", "w-36", "w-24", "w-20", "w-28"];
  return (
    <>
      {Array.from({ length: rows }, (_, r) => (
        <TR key={`sk-${r}`} className="hover:bg-transparent">
          {Array.from({ length: columns }, (_, c) => (
            <TD key={c}>
              <Skeleton className={cn("h-4", widths[(r + c) % widths.length])} />
            </TD>
          ))}
        </TR>
      ))}
    </>
  );
}
