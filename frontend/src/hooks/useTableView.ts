import { useEffect, useMemo, useState } from "react";

/**
 * Taie textul unui rând în cuvinte, pe orice nu e literă sau cifră.
 *
 * <p>Folosit de căutarea pe cuvinte, care cere ca fiecare cuvânt tastat să **înceapă** un cuvânt
 * din rând. Fără asta, „02" se potrivea în „2026" din data mişcării, iar o căutare după un cod de
 * deşeu întorcea alt cod de deşeu — exact felul de greşeală pe care aplicaţia asta nu şi-o
 * permite.
 */
function tokenize(text: string): string[] {
  return text.split(/[^\p{L}\p{N}]+/u).filter(Boolean);
}

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

  /**
   * Întâi expresia întreagă; dacă nimic nu se potriveşte aşa, fiecare cuvânt în parte.
   *
   * <p>Probat pe 07.09.2026, şi în două runde. Prima variantă căuta fiecare cuvânt ca subşir
   * oriunde: „15 01 02" scotea şi coduri **15 01 07**, fiindcă „02" se găseşte în „2026" din dată.
   * A doua variantă cerea ca fiecare cuvânt să **înceapă** un cuvânt din rând — mai bine, dar tot
   * scotea codul 15 01 07 de pe o mişcare din **02**.06.2026.
   *
   * <p>Fondul problemei era altul: „15 01 02" e o **expresie**, nu trei cuvinte independente. Un
   * cod de deşeu se caută întreg. Aşa că expresia are prioritate, iar căutarea pe cuvinte rămâne
   * plasa de siguranţă — acolo îşi câştigă pâinea „hamburger 15 01", care nu e o expresie din
   * niciun rând, dar descrie exact rândul căutat.
   */
  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q || !searchText) return rows;

    const haystacks = rows.map((row) => searchText(row).toLowerCase());
    const phrase = rows.filter((_, i) => haystacks[i].includes(q));
    if (phrase.length > 0) return phrase;

    // Cuvântul căutat trebuie să înceapă un cuvânt din rând: „02" nu mai prinde „2026".
    const words = q.split(/\s+/);
    return rows.filter((_, i) => {
      const tokens = tokenize(haystacks[i]);
      return words.every((w) => tokens.some((t) => t.startsWith(w)));
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

  /**
   * Coloană nouă → crescător. Aceeaşi coloană → se întoarce direcţia.
   *
   * <p>Prima variantă avea trei stări (crescător → descrescător → deloc). Probat pe 07.09.2026:
   * pe Mişcări, unde sortarea implicită e descrescător după dată, primul clic pe coloana „Data"
   * ducea la „deloc" — iar ordinea de la server fiind tot descrescătoare după dată, **nu se
   * schimba nimic pe ecran**. Utilizatorul apăsa şi credea că butonul e stricat.
   *
   * <p>Preţul e că nu se mai poate reveni la ordinea de la server dintr-un clic. Merită: aia se
   * vede la deschiderea ecranului, pe când un buton care pare mort se vede la fiecare folosire.
   */
  function toggleSort(key: string) {
    setPage(0);
    setSort((current) =>
      current?.key === key
        ? { key, direction: current.direction === "asc" ? "desc" : "asc" }
        : { key, direction: "asc" }
    );
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
