package ro.ecoregistru.enums;

/**
 * The kind of waste movement recorded. HANDED_OVER requires a Partner.
 */
public enum WasteOperation {
    GENERATED,
    COLLECTED,
    HANDED_OVER,
    RECOVERED,
    DISPOSED
}
