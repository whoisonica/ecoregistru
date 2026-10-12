/**
 * O coadă pe cheie: sarcinile aceleiași chei rulează una după alta, în ordinea în care au venit, iar
 * cheile diferite nu se așteaptă între ele. O sarcină căzută nu oprește coada: următoarea pornește oricum.
 *
 * <p>Pe fișa de energie cheia e celula (rubrică × lună). „Pe luni” apăsat cât salvarea cifrei anului
 * era încă în zbor ștergea înaintea ei, iar salvarea punea luna 0 la loc (12.10.2026).
 */
export function createKeyedQueue() {
  const tails = new Map<string, Promise<void>>();
  return function enqueue(key: string, task: () => Promise<void> | void): Promise<void> {
    const queued = (tails.get(key) ?? Promise.resolve()).then(task);
    const tail = queued.catch(() => undefined);
    tails.set(key, tail);
    void tail.then(() => {
      if (tails.get(key) === tail) tails.delete(key);
    });
    return queued;
  };
}
