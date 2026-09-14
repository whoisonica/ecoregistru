package ro.ecoregistru.enums;

/** Where a subscription invoice is (plata-abonamente.md, F2; see {@code V44}). */
public enum InvoiceStatus {
    /** Reserved here, not yet issued in FGO — or the issue failed, see {@code lastError}. */
    DRAFT,
    /** Issued in FGO, with a number, a PDF link and a due date. */
    ISSUED,
    /** FGO reports the paid amount covers the invoice. */
    PAID
}
