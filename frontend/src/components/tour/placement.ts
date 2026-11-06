/**
 * Unde stă caseta turului față de ținta luminată. Funcție pură (fără DOM), ca să se probeze sub
 * `npm test`; `Tour.tsx` îi dă dreptunghiul țintei, mărimea casetei și a ferestrei.
 *
 * <p>Pe ecran lat caseta caută loc întâi sub țintă, apoi deasupra, la dreapta, la stânga, și nu iese
 * niciodată la mai puțin de 16px de marginea ferestrei. Pe ecran îngust (sub `lg`, ca în `Layout`)
 * stă lipită jos, pe toată lățimea: lângă o țintă n-ar avea loc, iar degetul o găsește acolo.
 */

export interface Rect {
  top: number;
  left: number;
  width: number;
  height: number;
}

export interface Size {
  width: number;
  height: number;
}

export type BoxSide = "below" | "above" | "right" | "left" | "overlap" | "center" | "pinned";

export interface BoxPosition {
  top: number;
  left: number;
  /** Lățimea casetei: fixă pe ecran lat, fereastra minus marginile pe ecran îngust. */
  width: number;
  side: BoxSide;
}

/** Pragul `lg` din `Layout.tsx` și din `tour.ts` (`desktopOnly`). */
export const WIDE_MIN_WIDTH = 1024;
export const WIDE_QUERY = `(min-width: ${WIDE_MIN_WIDTH}px)`;
export const TOUR_BOX_WIDTH = 352;
export const VIEWPORT_MARGIN = 16;
/** Cât aer lasă decupajul în jurul țintei. */
export const CUTOUT_PADDING = 6;
/** `rounded-md` din `tailwind.config.js`. */
export const CUTOUT_RADIUS = 5;
/** Distanța dintre marginea decupajului și casetă. */
export const BOX_GAP = 12;

export function isWideViewport(viewportWidth: number): boolean {
  return viewportWidth >= WIDE_MIN_WIDTH;
}

/** Lățimea casetei pentru fereastra dată. */
export function boxWidthFor(viewportWidth: number): number {
  const available = Math.max(0, viewportWidth - 2 * VIEWPORT_MARGIN);
  return isWideViewport(viewportWidth) ? Math.min(TOUR_BOX_WIDTH, available) : available;
}

/** Dreptunghiul decupat din întuneric: ținta cu aerul ei. */
export function cutoutRect(target: Rect): Rect {
  return {
    top: target.top - CUTOUT_PADDING,
    left: target.left - CUTOUT_PADDING,
    width: target.width + 2 * CUTOUT_PADDING,
    height: target.height + 2 * CUTOUT_PADDING,
  };
}

/** Partea din dreptunghi care se vede în fereastră; `null` dacă nu se vede deloc. */
export function visiblePart(rect: Rect, viewport: Size): Rect | null {
  const top = Math.max(rect.top, 0);
  const left = Math.max(rect.left, 0);
  const bottom = Math.min(rect.top + rect.height, viewport.height);
  const right = Math.min(rect.left + rect.width, viewport.width);
  if (bottom <= top || right <= left) return null;
  return { top, left, width: right - left, height: bottom - top };
}

function clamp(value: number, min: number, max: number): number {
  // O casetă mai mare decât locul dintre margini stă lipită de marginea de sus / din stânga.
  return Math.max(min, Math.min(value, Math.max(min, max)));
}

/**
 * Poziția casetei (colțul de sus-stânga, în pixeli de fereastră).
 *
 * @param target dreptunghiul țintei (`getBoundingClientRect`), `null` = fără țintă (caseta în mijloc)
 * @param box mărimea casetei, măsurată
 * @param viewport mărimea ferestrei
 */
export function placeBox(target: Rect | null, box: Size, viewport: Size): BoxPosition {
  const m = VIEWPORT_MARGIN;
  const { width: vw, height: vh } = viewport;

  if (!isWideViewport(vw)) {
    const width = boxWidthFor(vw);
    return { top: Math.max(m, vh - m - box.height), left: m, width, side: "pinned" };
  }

  const w = box.width;
  const h = box.height;
  const clampX = (x: number) => clamp(x, m, vw - m - w);
  const clampY = (y: number) => clamp(y, m, vh - m - h);

  const seen = target ? visiblePart(cutoutRect(target), viewport) : null;
  if (!seen) {
    return { top: clampY((vh - h) / 2), left: clampX((vw - w) / 2), width: w, side: "center" };
  }

  const centerX = seen.left + seen.width / 2 - w / 2;
  const centerY = seen.top + seen.height / 2 - h / 2;

  const below = seen.top + seen.height + BOX_GAP;
  if (below + h <= vh - m) return { top: below, left: clampX(centerX), width: w, side: "below" };

  const above = seen.top - BOX_GAP - h;
  if (above >= m) return { top: above, left: clampX(centerX), width: w, side: "above" };

  const right = seen.left + seen.width + BOX_GAP;
  if (right + w <= vw - m) return { top: clampY(centerY), left: right, width: w, side: "right" };

  const left = seen.left - BOX_GAP - w;
  if (left >= m) return { top: clampY(centerY), left, width: w, side: "left" };

  // Ținta umple fereastra (un tabel lung): caseta stă jos, peste ea, în dreptul ei.
  return { top: clampY(vh - m - h), left: clampX(centerX), width: w, side: "overlap" };
}
