/**
 * Pagina pe care trebuie să stea tabelul când numărul de pagini s-a micșorat sub ea — după o căutare,
 * o ștergere, un filtru. Aceeași pagină dacă încă există.
 *
 * <p>`totalPages` poate fi 0 (tabel gol): atunci prima pagină, nu „-1”.
 */
export function clampPage(page: number, totalPages: number): number {
  const last = Math.max(1, totalPages) - 1;
  return page > last ? last : page;
}
