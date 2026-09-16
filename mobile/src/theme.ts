/**
 * Tokenii aspectului pe telefon, luați din prototipul aprobat v2 (`docs/machete/wastehouse-mobil-prototip.html`,
 * `:root`) și din `todo-mobil.md` §6. Colțurile mari sunt abaterea aprobată față de webul „Cântar”.
 */
export const colors = {
  graphiteTop: "#262E29",
  graphiteBottom: "#161B18",
  onDark: "#E6ECE8",
  onDark2: "#9AA59F",
  onDark3: "#6B7670",

  lcd: "#080C0A",
  lcdDigit: "#7CF2A9",
  lcdUnit: "#4FB57C",
  lcdGhost: "rgba(124,242,169,0.075)",
  lcdAlert: "#FFB020",

  green: "#009A44",
  greenHi: "#10B457",
  greenText: "#007A36",
  greenSoft: "#E3F4EA",
  blue: "#1C6FD1",
  blueHi: "#2F83E8",
  blueSoft: "#E4EFFB",
  amberText: "#8A4B00",
  amberSoft: "#FFF3DC",
  red: "#D92D20",
  redText: "#B42318",
  redSoft: "#FDE8E6",

  ground: "#EEF1EE",
  card: "#FFFFFF",
  ink: "#111513",
  ink2: "#5E6862",
  ink3: "#939C97",
  separator: "rgba(17,21,19,0.09)",
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
  group: 16,
  lcd: 14,
  button: 15,
  hero: 18,
  sheet: 24,
} as const;
