/** Nomenclatorul de județe al FGO (`/nomenclator/judet`), scris exact ca acolo, fără diacritice. */
export const COUNTIES = [
  "Alba", "Arad", "Arges", "Bacau", "Bihor", "Bistrita-Nasaud", "Botosani", "Braila", "Brasov",
  "Bucuresti", "Buzau", "Calarasi", "Caras-Severin", "Cluj", "Constanta", "Covasna", "Dambovita",
  "Dolj", "Galati", "Giurgiu", "Gorj", "Harghita", "Hunedoara", "Ialomita", "Iasi", "Ilfov",
  "Maramures", "Mehedinti", "Mures", "Neamt", "Olt", "Prahova", "Salaj", "Satu Mare", "Sibiu",
  "Suceava", "Teleorman", "Timis", "Tulcea", "Valcea", "Vaslui", "Vrancea",
];

/**
 * Județul de la ANAF („BISTRIŢA-NĂSĂUD”, „MUNICIPIUL BUCUREŞTI”) în forma din nomenclatorul FGO, sau `null` când nu
 * se potrivește cu niciunul. F-C: o singură adresă, luată de la ANAF la primul pas, ajunge și pe factură.
 */
export function fgoCounty(raw: string | null | undefined): string | null {
  if (!raw) return null;
  const key = raw
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toLowerCase()
    .replace(/^(municipiul|mun\.|judetul|jud\.)\s*/, "")
    .trim();
  if (key.startsWith("bucuresti")) return "Bucuresti";
  return COUNTIES.find((c) => c.toLowerCase() === key) ?? null;
}
