import type { Deadline, MonthlyEvidence } from "@/lib/types";

/**
 * Socotelile ecranului „Acasă” (varianta A, 18.09.2026), fără React, ca să se poată proba cu
 * `npm test`. Toate pleacă din listele pe care Panoul le avea deja: evidența anului și termenele.
 * Niciun endpoint nou.
 */

/** Kilogramele generate pe fiecare lună a anului, ianuarie la index 0. */
export function monthlyKg(rows: MonthlyEvidence[] | undefined): number[] {
  const kg = Array<number>(12).fill(0);
  for (const r of rows ?? []) kg[r.month - 1] += r.totalGenerated;
  return kg;
}

/**
 * Lunile trecute fără nicio cantitate, **între prima lună cu ceva și luna curentă**.
 *
 * <p>Nu de la ianuarie: un client care a început în septembrie ar vedea opt luni galbene care nu
 * spun nimic despre el. Luna curentă nu intră — e încă în lucru. Nu e o greșeală în sine (o firmă
 * mică poate să nu predea nimic o lună), deci ecranul o arată ca întrebare, nu ca blocaj.
 *
 * @param currentMonth 1–12 pentru anul în curs; 13 pentru un an încheiat (toate lunile sunt trecute)
 */
export function emptyMonths(kg: number[], currentMonth: number): number[] {
  const first = kg.findIndex((v) => v > 0);
  if (first < 0) return [];
  const out: number[] = [];
  for (let i = first + 1; i < Math.min(currentMonth - 1, 12); i++) {
    if (kg[i] === 0) out.push(i + 1);
  }
  return out;
}

export interface CodeTotal {
  code: string;
  name: string;
  hazardous: boolean;
  kg: number;
}

/** Codurile anului după cantitate, cele mai mari întâi; cele cu zero nu intră. */
export function topCodes(rows: MonthlyEvidence[] | undefined, limit = 5): CodeTotal[] {
  const byCode = new Map<string, CodeTotal>();
  for (const r of rows ?? []) {
    const c = byCode.get(r.wasteCode) ?? { code: r.wasteCode, name: r.wasteCodeName, hazardous: r.hazardous, kg: 0 };
    c.kg += r.totalGenerated;
    byCode.set(r.wasteCode, c);
  }
  return [...byCode.values()]
    .filter((c) => c.kg > 0)
    .sort((a, b) => b.kg - a.kg || a.code.localeCompare(b.code))
    .slice(0, limit);
}

/**
 * Termenele deschise pe douăsprezece coloane, de la luna curentă înainte. Un termen depășit stă în
 * prima coloană: e tot de făcut acum. Ce cade după a douăsprezecea lună nu intră.
 */
export function deadlinesByMonth(open: Deadline[], today = new Date()): Deadline[][] {
  const cols: Deadline[][] = Array.from({ length: 12 }, () => []);
  const base = today.getFullYear() * 12 + today.getMonth();
  for (const dl of open) {
    const [y, m] = dl.dueDate.split("-").map(Number);
    const idx = Math.max(0, y * 12 + (m - 1) - base);
    if (idx < 12) cols[idx].push(dl);
  }
  for (const c of cols) c.sort((a, b) => a.dueDate.localeCompare(b.dueDate));
  return cols;
}

/** Textul unui câmp iCalendar: `\`, `;`, `,` și rândul nou se scapă (RFC 5545 §3.3.11). */
function icsText(s: string): string {
  return s.replace(/\\/g, "\\\\").replace(/;/g, "\\;").replace(/,/g, "\\,").replace(/\n/g, "\\n");
}

/**
 * Termenele ca fișier `.ics`: evenimente de o zi, cu un memento cu trei zile înainte. `UID`-ul e
 * id-ul termenului, deci un calendar care importă fișierul a doua oară îl actualizează în loc să-l
 * dubleze. E o fotografie: un termen bifat după import rămâne în calendar.
 */
export function deadlinesIcs(deadlines: Deadline[], label: (d: Deadline) => string, now = new Date()): string {
  const stamp = now.toISOString().replace(/[-:]/g, "").replace(/\.\d{3}/, "");
  const lines = ["BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//WasteHouse//Termene//RO", "CALSCALE:GREGORIAN"];
  for (const d of deadlines) {
    if (!d.id) continue;
    const day = d.dueDate.replace(/-/g, "");
    const [y, m, dd] = d.dueDate.split("-").map(Number);
    const next = new Date(Date.UTC(y, m - 1, dd + 1)).toISOString().slice(0, 10).replace(/-/g, "");
    lines.push(
      "BEGIN:VEVENT",
      `UID:${d.id}@wastehouse.ro`,
      `DTSTAMP:${stamp}`,
      `DTSTART;VALUE=DATE:${day}`,
      `DTEND;VALUE=DATE:${next}`,
      `SUMMARY:${icsText(label(d))}`,
      "BEGIN:VALARM",
      "ACTION:DISPLAY",
      "TRIGGER:-P3D",
      `DESCRIPTION:${icsText(label(d))}`,
      "END:VALARM",
      "END:VEVENT"
    );
  }
  lines.push("END:VCALENDAR");
  return lines.join("\r\n") + "\r\n";
}
