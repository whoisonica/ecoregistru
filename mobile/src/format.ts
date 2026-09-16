/** Kilogramele în forma românească: 1.240 sau 12,5. Zecimala rămâne doar când există. */
export function formatKg(kg: number) {
  const [int, dec] = (Math.round(kg * 10) / 10).toFixed(1).split(".");
  const grouped = int.replace(/\B(?=(\d{3})+(?!\d))/g, ".");
  return dec === "0" ? grouped : `${grouped},${dec}`;
}

/** `yyyy-MM-dd` → `dd.MM.yyyy`, ca pe web. Fără `Date`: un șir de zi n-are fus orar. */
export function formatDate(iso: string) {
  const [y, m, d] = iso.slice(0, 10).split("-");
  return `${d}.${m}.${y}`;
}
