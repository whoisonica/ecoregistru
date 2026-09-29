/**
 * O singură cerere în zbor pe cheie: al doilea clic, venit înainte ca primul să se termine, nu mai
 * pleacă. Fără React, ca să se poată proba; ecranele îl țin într-un `useRef`.
 *
 * <p>De ce nu ajunge starea (`disabled={busy}`): butonul se dezactivează abia după randarea de după
 * clic, iar un dublu-clic le trimite pe amândouă înainte de ea (29.09.2026). La „Salvează” pe o
 * mișcare, `handleSubmit` aștepta întâi anul declarat, deci fereastra era de o cerere întreagă — două
 * salvări. La prima tipărire a Anexei 3, a doua cerere aloca numărul pe o versiune veche: 409.
 */
export function flightGuard() {
  const busy = new Set<string>();
  return {
    isBusy: (key: string) => busy.has(key),
    /** `false` = cheia era deja în zbor și `fn` nu s-a mai chemat. */
    async run(key: string, fn: () => Promise<void>): Promise<boolean> {
      if (busy.has(key)) return false;
      busy.add(key);
      try {
        await fn();
        return true;
      } finally {
        busy.delete(key);
      }
    },
  };
}
