/**
 * Tokenii aspectului pe telefon. Din 27.09.2026 seara — paleta **A „Hârtie și smarald”** din macheta
 * `docs/machete/wastehouse-mobil-dichisit.html` (proprietarul: „să pară mult mai lucrată și dichisită;
 * Acasă e prea plin și nu-mi plac combourile de culori”): hârtie caldă, carduri albe cu contur subțire,
 * **un singur accent** — verdele semnului (`mark` din login și landing, #047857) — iar roșul, galbenul
 * și verdele de stare numai ca punct, pastilă sau contur, niciodată ca fond de bloc. Fără gradient pe
 * taste, fără bloc grafit în afara loginului.
 *
 * <p>Numele cheilor au rămas cele de dinainte (`green`, `ground`, `separator`…), ca ecranele să nu se
 * atingă toate deodată; valorile sunt ale paletei noi. Cheile `graphite*` și `lcd*` mai trăiesc doar pe
 * login și pe caseta pozei din formular.
 */
export const colors = {
  graphiteTop: "#262E29",
  graphiteBottom: "#161B18",
  onDark: "#E6ECE8",
  onDark2: "#9AA59F",
  onDark3: "#6B7670",

  /** Caseta pozei din formular (fond întunecat sub fotografie) și textul „se citește”. */
  lcd: "#101412",
  lcdDigit: "#7CF2A9",

  /** Accentul: tastele, „+” din bară, linkurile, LED-ul „ok”. `greenHi` = același verde: fără gradient. */
  green: "#047857",
  greenHi: "#047857",
  greenText: "#065F46",
  greenSoft: "#E3F1EA",
  /** Colectorul: „+” albastru, ca pe ecranul de intrare din prototip. Tot plat. */
  blue: "#1C6FD1",
  blueHi: "#1C6FD1",
  blueSoft: "#E4EFFB",
  amber: "#D08A1C",
  amberText: "#B8690A",
  amberSoft: "#FBF1E0",
  red: "#C2322A",
  redText: "#C2322A",
  redSoft: "#FBE8E6",
  /** Un rând care n-a venit / nu se știe. */
  unknown: "#B4B9B2",

  ground: "#F4F4EF",
  card: "#FFFFFF",
  /** Fondul unei dale sau pastile „liniștite” și al rândului apăsat. */
  quiet: "#ECEDE8",
  pressed: "#F5F5F1",
  /** Șina segmentelor (taburile ecranului, KG/tone): hârtia puțin scufundată. */
  track: "rgba(23,26,23,0.06)",
  ink: "#171A17",
  ink2: "#5F655E",
  ink3: "#9AA098",
  separator: "rgba(23,26,23,0.09)",
  onAccent: "#FFFFFF",
} as const;

/**
 * Pubela pe codul de deșeu. Care cod ce culoare are se hotărăște în `@/lib/binColor`, importat de pe
 * web — aici sunt doar valorile, aceleași ca `bin.*` din `frontend/tailwind.config.js`, fiindcă
 * telefonul n-are Tailwind. Un cod care nu e în listă n-are culoare, și nu se desenează nimic.
 */
export const binColors = {
  paper: "#1F5FBF",
  plastic: "#E9B600",
  glass: "#2E8B3E",
  bio: "#7A4E2D",
  residual: "#4D4D4D",
  hazard: "#C8102E",
  metal: "#8A9299",
} as const;

export const fonts = {
  sans: "IBMPlexSans_400Regular",
  sansMedium: "IBMPlexSans_500Medium",
  sansSemiBold: "IBMPlexSans_600SemiBold",
  mono: "IBMPlexMono_400Regular",
  monoMedium: "IBMPlexMono_500Medium",
} as const;

export const radius = {
  group: 18,
  lcd: 14,
  button: 16,
  hero: 16,
  sheet: 24,
} as const;
