import { useEffect, useMemo, useState } from "react";
import { fold } from "@/lib/utils";

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

/**
 * Comparator de coloană. Primeşte şi direcţia, pentru rândurile care trebuie să stea la coadă
 * indiferent de ea — vezi {@link missingLast}.
 */
export type Comparator<T> = (a: T, b: T, direction: SortDirection) => number;

/**
 * Un comparator peste o valoare care poate lipsi, cu lipsa mereu la coadă — **în ambele sensuri**.
 *
 * <p>„De cântărit" nu e nici cea mai mică, nici cea mai mare cantitate: e nespusă, şi n-are ce
 * căuta printre cifre la niciun capăt. La fel autorizaţia fără dată: „nu se ştie" nu e nici
 * devreme, nici târziu.
 *
 * <p>Trei comparatoare scriau regula asta de mână, cu `return 1` pentru rândul fără valoare — şi
 * o rateau, fiindcă direcţia se aplică peste rezultatul lor: la a doua apăsare pe coloană,
 * rândurile puse dinadins la coadă ajungeau tocmai la vârf. Aici semnul se întoarce odată cu
 * direcţia, deci după ce hook-ul îl neagă, lipsa e din nou la coadă.
 *
 * <p>Două lipsuri sunt egale între ele: altfel `compare(a,b)` şi `compare(b,a)` ar fi amândouă
 * pozitive, ceea ce nu e un comparator valid.
 */
export function missingLast<T, V>(
  get: (row: T) => V | null | undefined,
  compare: (a: V, b: V) => number
): Comparator<T> {
  return (a, b, direction) => {
    const x = get(a);
    const y = get(b);
    if (x == null && y == null) return 0;
    if (x == null) return direction === "desc" ? -1 : 1;
    if (y == null) return direction === "desc" ? 1 : -1;
    return compare(x, y);
  };
}

interface TableViewOptions<T> {
  /**
   * Textul în care caută caseta de căutare, pentru un rând. Se compune din coloanele pe care
   * omul le-ar tasta — cod, nume, partener — nu din tot obiectul: o căutare care se potrivește
   * pe un id nu ajută pe nimeni.
   */
  searchText?: (row: T) => string;
  /** Comparatoare pe cheie de coloană. Cheia care lipsește de aici nu e sortabilă. */
  comparators?: Record<string, Comparator<T>>;
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
    // `fold` pe amândouă părţile: cine tastează „deseuri" caută acelaşi lucru ca cine tastează
    // „deşeuri". Vezi `fold` în `lib/utils` — până la ea, prima variantă întorcea zero rânduri.
    const q = fold(query.trim());
    if (!q || !searchText) return rows;

    const haystacks = rows.map((row) => fold(searchText(row)));
    const phrase = rows.filter((_, i) => haystacks[i].includes(q));
    if (phrase.length > 0) return phrase;

    // Cuvântul căutat trebuie să înceapă un cuvânt din rând: „02" nu mai prinde „2026".
    const words = q.split(/\s+/);
    return rows.filter((_, i) => {
      const tokens = tokenize(haystacks[i]);
      return words.every((w) => tokens.some((t) => t.startsWith(w)));
    });
  }, [rows, query, searchText]);

  /**
   * Direcţia se aplică **negând comparatorul**, nu întorcând tabloul.
   *
   * <p>`reverse()` părea acelaşi lucru şi nu era, din două motive. Întâi, inversa şi ordinea
   * rândurilor egale între ele — adică tocmai ordinea gândită de server, pe care sortarea stabilă
   * o păstrează dinadins. Apoi, şi mai rău, muta la vârf rândurile pe care un comparator le
   * pusese dinadins la coadă: „De cântărit" şi autorizaţia fără dată ajungeau primele la a doua
   * apăsare pe coloană, deşi trei comentarii scriau că stau la coadă în ambele sensuri.
   */
  const sorted = useMemo(() => {
    if (!sort || !comparators?.[sort.key]) return filtered;
    const compare = comparators[sort.key];
    const sign = sort.direction === "desc" ? -1 : 1;
    // Copie: `Array.prototype.sort` lucrează pe loc, iar `filtered` poate fi chiar `rows`.
    return [...filtered].sort((a, b) => sign * compare(a, b, sort.direction));
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
