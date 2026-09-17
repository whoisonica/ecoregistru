package ro.ecoregistru.util;

import java.util.Set;

/**
 * Nomenclatorul de județe al FGO ({@code /nomenclator/judet}), scris exact ca acolo, fără diacritice — aceeași listă
 * ca {@code frontend/src/lib/counties.ts}. FGO refuză o factură cu alt județ, deci un județ din afara listei se oprește
 * la salvare, nu dimineața, la emitere (scanarea din 17.09.2026: {@code PUT /billing/details} primea orice text).
 */
public final class FgoCounties {

    public static final Set<String> NAMES = Set.of(
            "Alba", "Arad", "Arges", "Bacau", "Bihor", "Bistrita-Nasaud", "Botosani", "Braila", "Brasov",
            "Bucuresti", "Buzau", "Calarasi", "Caras-Severin", "Cluj", "Constanta", "Covasna", "Dambovita",
            "Dolj", "Galati", "Giurgiu", "Gorj", "Harghita", "Hunedoara", "Ialomita", "Iasi", "Ilfov",
            "Maramures", "Mehedinti", "Mures", "Neamt", "Olt", "Prahova", "Salaj", "Satu Mare", "Sibiu",
            "Suceava", "Teleorman", "Timis", "Tulcea", "Valcea", "Vaslui", "Vrancea");

    private FgoCounties() {
    }

    public static boolean isValid(String county) {
        return county != null && NAMES.contains(county.trim());
    }
}
