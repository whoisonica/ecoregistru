/**
 * Salvarea unui fișier venit de la server, într-un singur loc.
 *
 * <p>Cele șapte descărcări ale aplicației — fișa, declarația anuală, exportul generic, Anexa 3,
 * dosarul de control și cele două de ambalaje — scriau fiecare aceeași secvență de șase rânduri, în
 * **trei** variante ușor diferite. Două dintre ele (Anexa 3 și ambalajele) nu adăugau linkul în
 * document înainte de `click()`, ceea ce în Firefox înseamnă o descărcare care nu pornește; iar
 * `useAnexa3` revoca adresa `blob:` **sincron** după clic, deci și acolo unde pornea, putea fi
 * anulată din mers.
 *
 * <p>Revocarea se face pe următorul tick, nu imediat: browserul citește adresa după ce se întoarce
 * din handler-ul de clic.
 */
export function saveBlob(data: Blob, fileName: string): void {
  const url = URL.createObjectURL(data);
  const a = document.createElement("a");
  a.href = url;
  a.download = fileName;
  a.rel = "noopener";
  // În document înainte de clic: un `<a>` care nu e în arbore nu declanșează descărcarea în
  // Firefox. Chrome iartă asta, de aceea a trecut neobservat.
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 0);
}
