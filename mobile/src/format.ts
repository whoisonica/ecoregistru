/** Kilogramele în forma românească: 1.240 sau 12,5. Zecimala rămâne doar când există. */
export function formatKg(kg: number) {
  const [int, dec] = (Math.round(kg * 10) / 10).toFixed(1).split(".");
  const grouped = int.replace(/\B(?=(\d{3})+(?!\d))/g, ".");
  return dec === "0" ? grouped : `${grouped},${dec}`;
}

/**
 * Cantitatea unui rând în unitatea lui. Tonele merg până la trei zecimale — cât primește serverul —,
 * altfel 0,045 t (45 kg) apărea „0 t”, iar 1,25 t apărea „1,3 t”. Kilogramele rămân cu una.
 */
export function formatQuantity(quantity: number, unit: string) {
  if (unit !== "TONS") return formatKg(quantity);
  const [int, dec = ""] = String(Math.round(quantity * 1000) / 1000).split(".");
  const grouped = int.replace(/\B(?=(\d{3})+(?!\d))/g, ".");
  return dec ? `${grouped},${dec}` : grouped;
}

/**
 * `yyyy-MM-dd` → `dd.MM.yyyy`, ca pe web. Un șir de zi n-are fus orar și se taie; un moment
 * (`2027-03-12T22:30:00Z`, bifa unui termen) se citește în ora telefonului — tăiat, o bifă de la 00:30
 * arăta ziua de ieri (BUG-062 pe web, `frontend/src/lib/dates.ts`).
 */
export function formatDate(iso: string) {
  if (iso.includes("T")) {
    const t = new Date(iso);
    if (!Number.isNaN(t.getTime())) {
      const pad = (n: number) => String(n).padStart(2, "0");
      return `${pad(t.getDate())}.${pad(t.getMonth() + 1)}.${t.getFullYear()}`;
    }
  }
  const [y, m, d] = iso.slice(0, 10).split("-");
  return `${d}.${m}.${y}`;
}
