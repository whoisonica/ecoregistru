package ro.ecoregistru.enums;

/**
 * Starea unei operațiuni de depozit (V46). Doar {@link #FINALIZED} contează în stoc și în registre.
 */
public enum WeighingOperationStatus {

    /** Se cântărește încă; se poate modifica. */
    IN_PROGRESS,

    /** Închisă de cineva cu drept de aprobare; nu se mai modifică, doar se anulează. */
    FINALIZED,

    /** Anulată cu motiv. Rămâne în listă, cu numărul ei, dar iese din stoc și din registre. */
    CANCELLED
}
