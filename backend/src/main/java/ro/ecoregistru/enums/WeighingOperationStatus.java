package ro.ecoregistru.enums;

/**
 * Starea unei operațiuni de depozit (V46). Contează în stoc și în registre {@link #FINALIZED} și, la transfer, {@link #IN_TRANSIT}.
 */
public enum WeighingOperationStatus {

    /** Se cântărește încă; se poate modifica. */
    IN_PROGRESS,

    /**
     * D2.5 — transfer plecat din depozitul A, nerecepționat încă la B. Liniile de plecare contează deja (au ieșit din
     * A); cele de recepție nu există încă. Nu se mai modifică: se recepționează sau se anulează.
     */
    IN_TRANSIT,

    /** Închisă de cineva cu drept de aprobare; nu se mai modifică, doar se anulează. */
    FINALIZED,

    /** Anulată cu motiv. Rămâne în listă, cu numărul ei, dar iese din stoc și din registre. */
    CANCELLED
}
