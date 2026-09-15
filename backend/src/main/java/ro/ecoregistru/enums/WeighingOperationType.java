package ro.ecoregistru.enums;

/**
 * Ce face o operațiune de depozit cu stocul (V46). Numerotarea e separată pe fiecare tip.
 *
 * <p>Felia 1 folosește doar {@link #IN} și {@link #OUT}. Celelalte sunt în constrângerea din bază de
 * acum, ca numerotarea și filtrele să nu se schimbe la fiecare felie, dar serviciul le refuză până
 * vine felia lor.
 */
public enum WeighingOperationType {

    /** Marfă primită în depozit: de la un partener sau de la o persoană fizică. */
    IN,

    /** Marfă predată unui operator autorizat. */
    OUT,

    /** Între două depozite ale firmei, cu marfa „în tranzit” între plecare și recepție (F2). */
    TRANSFER,

    /** Diferența de inventar dintre scriptic și faptic, cu motiv (F3). */
    ADJUSTMENT,

    /** Sortare sau balotare: consumă sortimente și produce altele (F5). */
    PROCESSING;

    /** Ce se poate înregistra în felia de acum. */
    public boolean isAvailable() {
        return this == IN || this == OUT;
    }
}
