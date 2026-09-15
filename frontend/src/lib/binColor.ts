/**
 * Culoarea pubelei pe codul de deșeu — direcția „Cântar” (docs/stil-interfata.md).
 *
 * <p>Un pătrățel mic înaintea codului, pe liste și în formular: omul de la brutărie știe „albastru
 * = hârtie" înainte să știe „15 01 01". Culorile sunt cele ale colectării separate din România
 * (albastru hârtie-carton, galben plastic și metal, verde sticlă, maro biodegradabil, negru/gri
 * rezidual) plus roșu pentru periculoase și gri-metal pentru fierul vechi al colectorului.
 *
 * <p>⚠️ **Lista e explicită, cod cu cod — nu se ghicește din prefix.** „15 01" nu înseamnă o
 * culoare (15 01 01 e hârtie, 15 01 02 plastic), iar „20 01" cu atât mai puțin. Un cod care nu e în
 * listă n-are culoare, și asta e un răspuns corect, nu o lipsă. Galbenul pubelei **nu** înseamnă
 * „așteaptă": stările au forma lor (LED + cuvânt, `Badge`), aici e materialul.
 */
export type Bin = "paper" | "plastic" | "glass" | "bio" | "residual" | "hazard" | "metal";

const BY_CODE: Record<string, Bin> = {
  // Ambalaje (15 01)
  "15 01 01": "paper",
  "15 01 02": "plastic",
  "15 01 04": "plastic", // ambalaje metalice — pubela galbenă, ca plasticul
  "15 01 05": "plastic", // compozite (Tetra Pak) — galbenă
  "15 01 06": "plastic", // amestecuri de ambalaje — galbenă
  "15 01 07": "glass",
  // Municipale (20 01, 20 02, 20 03)
  "20 01 01": "paper",
  "20 01 02": "glass",
  "20 01 08": "bio",
  "20 01 39": "plastic",
  "20 01 40": "plastic", // metale din deșeuri municipale — galbenă
  "20 02 01": "bio",
  "20 03 01": "residual",
  "20 03 07": "residual", // voluminoase
  // Hârtie și carton din producție și comerț
  "03 03 08": "paper",
  "19 12 01": "paper",
  "19 12 02": "metal", // metale feroase din tratare mecanică
  "19 12 03": "metal", // metale neferoase
  "19 12 04": "plastic",
  "19 12 05": "glass",
  // Fier vechi și metale (17 04) — pubela nu există; gri-metal, ca la colector
  "17 04 01": "metal",
  "17 04 02": "metal",
  "17 04 03": "metal",
  "17 04 04": "metal",
  "17 04 05": "metal",
  "17 04 06": "metal",
  "17 04 07": "metal",
  "17 04 11": "metal",
  "12 01 01": "metal",
  "12 01 03": "metal",
  "16 01 17": "metal",
  "16 01 18": "metal",
  // Sticlă și plastic din construcții și producție
  "17 02 02": "glass",
  "17 02 03": "plastic",
  "16 01 19": "plastic",
  "16 01 20": "glass",
  "02 01 04": "plastic",
  "07 02 13": "plastic",
  "12 01 05": "plastic",
};

/**
 * Culoarea pubelei pentru un cod, sau `null` dacă nu e în listă.
 *
 * <p>Periculoasele (`*` în cod, sau steagul din catalog) sunt roșii **înaintea** listei: un cod
 * periculos nu se pune în nicio pubelă de stradă, oricât ar semăna cu unul de ambalaj.
 */
export function binFor(code: string, hazardous = false): Bin | null {
  const trimmed = code.trim();
  if (hazardous || trimmed.endsWith("*")) return "hazard";
  return BY_CODE[trimmed] ?? null;
}

/** Numai pentru probe: câte coduri are lista, ca o probă să știe că a avut ce verifica. */
export const BIN_CODE_COUNT = Object.keys(BY_CODE).length;
