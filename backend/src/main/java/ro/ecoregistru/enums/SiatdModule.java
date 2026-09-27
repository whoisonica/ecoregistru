package ro.ecoregistru.enums;

/**
 * Modulele SIATD în care se poate înrola o firmă (Ordinul 701/2024; F6a, V78 {@code companies.siatd_*_from}), cu
 * termenul de confirmare al celui care preia, în zile (Instrucțiunile, art. 18 alin. (6)–(10)). Codurile care intră în
 * fiecare: {@link ro.ecoregistru.util.SiatdFlow}.
 */
public enum SiatdModule {
    MUNICIPAL(3),
    PACKAGING(5),
    WEEE(15),
    BATTERY(15),
    TYRE(5);

    private final int days;

    SiatdModule(int days) {
        this.days = days;
    }

    public int days() {
        return days;
    }
}
