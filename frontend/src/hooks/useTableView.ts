import { useEffect, useMemo, useState } from "react";

export type SortDirection = "asc" | "desc";
export interface SortState {
  key: string;
  direction: SortDirection;
}

interface TableViewOptions<T> {
  /**
   * Textul în care caută caseta de căutare, pentru un rând. Se compune din coloanele pe care
   * omul le-ar tasta — cod, nume, partener — nu din tot obiectul: o căutare care se potrivește
   * pe un id nu ajută pe nimeni.
   */
  searchText?: (row: T) => string;
  /** Comparatoare pe cheie de coloană. Cheia care lipsește de aici nu e sortabilă. */
  comparators?: Record<string, (a: T, b: T) => number>;
  initialSort?: SortState;
  /** 0 = fără paginare. */
  pageSize?: number;
}

/**
 * Partea din vedere de care au nevoie bara de sus și paginarea de jos — fără rândurile însele.
 *
 * <p>Separată ca `TableToolbar` și `TablePagination` să primească orice vedere, indiferent de tipul
 * rândurilor: altfel fiecare apelant ar trebui să-și tipizeze bara.
 */
export interface TableViewControls {
  query: string;
  search: (next: string) => void;
  sort: SortState | null;
  toggleSort: (key: string) => void;
  page: number;
  setPage: (page: number) => void;
  pageCount: number;
  pageSize: number;
  matchCount: number;
  totalCount: number;
  emptiedBySearch: boolean;
}

export interface TableView<T> extends TableViewControls {
  /** Rândurile paginii curente — ce se randează. */
  visible: T[];
}

/**
 * Căutare, sortare și paginare peste un tablou deja încărcat.
 *
 * <p>Toate tabelele aplicației aduc tot ce e de arătat într-o singură cerere și îl randează la
 * rând — ceea ce merge cât timp clientul are o lună de date, și nu mai merge la doi ani. Aici nu
 * se schimbă felul cum se încarcă: se schimbă cât se **arată** deodată, ce e la vârf și ce se
 * poate găsi tastând.
 *
 * <p>Ordinea e căutare → sortare → felie: căutarea restrânge întregul, sortarea așază restul, iar
 * pagina se taie la sfârșit. Invers, ai sorta ce nu se vede și ai pagina ce se aruncă.
 */
export function useTableView<T>(rows: T[], options: TableViewOptions<T> = {}): TableView<T> {
  const { searchText, comparators, initialSort, pageSize: defaultPageSize = 25 } = options;

  const [query, setQuery] = useState("");
  const [sort, setSort] = useState<SortState | null>(initialSort ?? null);
  const [page, setPage] = useState(0);
  // Fix: nimeni nu alege câte rânduri pe pagină, iar un selector în plus nu se cere.
  const pageSize = defaultPageSize;

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q || !searchText) return rows;
    // Fiecare cuvânt trebuie să se potrivească, undeva: „hamburger 15 01" găsește rândul, oricum
    // ar fi ordonate coloanele în text.
    const words = q.split(/\s+/);
    return rows.filter((row) => {
      const haystack = searchText(row).toLowerCase();
      return words.every((w) => haystack.includes(w));
    });
  }, [rows, query, searchText]);

  const sorted = useMemo(() => {
    if (!sort || !comparators?.[sort.key]) return filtered;
    const compare = comparators[sort.key];
    // Copie: `Array.prototype.sort` lucrează pe loc, iar `filtered` poate fi chiar `rows`.
    const out = [...filtered].sort(compare);
    return sort.direction === "desc" ? out.reverse() : out;
  }, [filtered, sort, comparators]);

  const pageCount = pageSize > 0 ? Math.max(1, Math.ceil(sorted.length / pageSize)) : 1;

  // O căutare care lasă mai puține pagini nu trebuie să te lase pe una goală.
  useEffect(() => {
    if (page > pageCount - 1) setPage(Math.max(0, pageCount - 1));
  }, [page, pageCount]);

  const visible = useMemo(() => {
    if (pageSize <= 0) return sorted;
    const from = page * pageSize;
    return sorted.slice(from, from + pageSize);
  }, [sorted, page, pageSize]);

  function toggleSort(key: string) {
    setPage(0);
    setSort((current) => {
      if (current?.key !== key) return { key, direction: "asc" };
      // A treia apăsare scoate sortarea: te întorci la ordinea în care le-a trimis serverul, care
      // e ordinea gândită de cineva, nu una alfabetică.
      if (current.direction === "asc") return { key, direction: "desc" };
      return null;
    });
  }

  function search(next: string) {
    setQuery(next);
    setPage(0);
  }

  return {
    query,
    search,
    sort,
    toggleSort,
    page,
    setPage,
    pageCount,
    pageSize,
    visible,
    // Câte au trecut de căutare. Diferit de `totalCount` când se caută ceva.
    matchCount: sorted.length,
    totalCount: rows.length,
    // Adevărat când căutarea a golit lista, dar existau rânduri: alt gol, alt mesaj.
    emptiedBySearch: rows.length > 0 && sorted.length === 0,
  };
}
