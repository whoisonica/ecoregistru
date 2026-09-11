import { saveBlob } from "@/lib/download";

/**
 * Deschide într-un tab un fișier care vine **prin sesiune**, nu de la un URL public.
 *
 * <p>Regula asta a fost scrisă pentru atașamente la 11-bis și e refolosită de buletinele de
 * analiză (G-7): amândouă sunt documente ale unei firme, livrate printr-un endpoint al nostru care
 * verifică tenantul, deci niciunul nu poate fi un `<a href>` simplu — un `<a>` nu duce cu el
 * antetul `Authorization`. Stă într-un singur loc fiindcă poartă două capcane pe care le-am plătit
 * deja o dată, iar două copii ar fi două ocazii ca numai una să fie reparată.
 *
 * <p><b>Tabul se deschide înainte de `await`, nu după.</b> Browserele leagă permisiunea de a
 * deschide o fereastră de gestul care a produs-o; un `window.open` de după o cerere de rețea nu
 * mai e al clicului și e blocat ca reclamă.
 *
 * <p>⚠️ <b>Fără `noopener`.</b> Specificația spune că atunci `window.open` întoarce `null` — n-ai
 * cum să primești un mâner către o fereastră de care tocmai te-ai lepădat. Prima versiune îl cerea,
 * deci `tab` era null de fiecare dată, ramura care punea adresa era cod mort și fiecare fișier se
 * **descărca** în loc să se deschidă. `tsc` n-avea ce obiecta: tipul chiar include `null`. S-a
 * văzut numai cu ochiul, pe aplicația pornită. Ce voia `noopener` să apere nu se aplică unui
 * `blob:` din propria origine; îl tăiem oricum cu `tab.opener = null`, ca intenția să rămână
 * scrisă.
 */
export function openBlobInTab(tab: Window | null, blob: Blob, fallbackName: string) {
  const url = URL.createObjectURL(blob);
  if (tab) {
    tab.location.href = url;
  } else {
    saveBlob(blob, fallbackName);
  }
  // Adresa `blob:` trăiește cât tabul care o citește; o eliberăm târziu, nu pe tickul următor ca
  // la descărcări, fiindcă acolo browserul termină de citit în aceeași secundă.
  setTimeout(() => URL.revokeObjectURL(url), 60_000);
}

/** Tabul gol, deschis pe gestul utilizatorului. Vezi de ce, mai sus. */
export function openBlankTab(): Window | null {
  const tab = window.open("", "_blank");
  if (tab) tab.opener = null;
  return tab;
}
