package ro.ecoregistru.enums;

/**
 * Cine vede prețurile depozitului (D1.8, V46 {@code companies.price_visibility}). O alege adminul
 * firmei; proprietarul, 15.09.2026: „lasă configurabil”.
 *
 * <p><b>Platforma e tratată ca un consultant</b> (decizia proprietarului, 15.09.2026): vede prețurile
 * doar la {@link #COMPANY}. Motivul e chiar originea setării: „să nu zică omul că noi vedem ce prețuri
 * are el”.
 *
 * <p>Singurul loc cu regula. Serverul golește prețul din răspuns pentru cine nu îl vede, iar o
 * editare făcută de el nu îl atinge ({@code WeighingOperationService.replaceLines}).
 */
public enum PriceVisibility {
    /** Toți utilizatorii firmei, consultantul și platforma. Implicitul din V46. */
    COMPANY,
    /** Utilizatorii firmei (admin, operator, vizualizator), fără consultant și fără platformă. */
    NO_CONSULTANT,
    /** Doar adminul firmei. */
    ADMIN_ONLY;

    public boolean visibleTo(Role role) {
        return switch (this) {
            case COMPANY -> true;
            case NO_CONSULTANT -> role == Role.ADMIN || role == Role.OPERATOR || role == Role.CLIENT_VIEWER;
            case ADMIN_ONLY -> role == Role.ADMIN;
        };
    }
}
