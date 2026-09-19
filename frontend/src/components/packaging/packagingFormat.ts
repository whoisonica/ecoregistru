/*
 * Cele trei formatări pe care le citesc și pagina de ambalaje, și Anexa 3: erau în pagină, iar când
 * secțiunea Anexa 3 a plecat în fișierul ei (18.09.2026) ar fi trebuit ori duplicate, ori importate
 * dintr-o pagină — două lucruri pe care le repară același fișier mic.
 */
import { strings } from "@/lib/strings";
import { formatKg } from "@/lib/units";
import { withCount } from "@/lib/utils";

export const materialLabels = strings.enums.packagingMaterial;

/**
 * Kilograme, prin formatorul din `lib/units` — trei zecimale întotdeauna (G06): o a doua
 * formatare aici ar scrie aceeași cantitate altfel decât o scrie ecranul de alături.
 * Gol se scrie „—”, niciodată „0” (vezi CLAUDE.md).
 */
export function kg(value: number | null | undefined) {
  return value == null ? "—" : formatKg(value);
}

/** „1 mișcare” · „2 mișcări” · „20 de mișcări” — numeralul românesc din `lib/count.ts`. */
export function countMovements(template: string, n: number) {
  return withCount(template, n, "mișcare", "mișcări");
}
