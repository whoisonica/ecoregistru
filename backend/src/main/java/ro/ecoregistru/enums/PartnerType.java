package ro.ecoregistru.enums;

/**
 * What a partner is, in relation to the waste: it produces it, or it takes it over.
 *
 * <p>There is deliberately no CARRIER. Hauling is not a category of partner but a rubric of one
 * particular transport, and that is where it lives: {@code WasteMovement.transportPartner}, next
 * to the driver and the plate number, exactly as Anexa 3 la HG 1061/2008 asks for it. Any partner
 * can be named there.
 *
 * <p>What V28 added is not a fourth value but a tick, {@code Partner.carrier}: "this one can haul
 * it". A value would have been exclusive, and the firm that both collects and hauls — the ordinary
 * case — would have had to be entered twice. A pure haulage firm is the one partner with no type
 * at all: it does nothing with the waste, so the field is null and only the tick is set.
 *
 * <p>This axis answers "what are they"; the commercial role on {@link ro.ecoregistru.entity.Partner}
 * — client / supplier — answers "which way does the invoice travel". They are independent: the
 * collector who buys our cardboard is a COLLECTOR and a client, the one who empties our bins is a
 * COLLECTOR and a supplier.
 */
public enum PartnerType {

    /** They produce the waste; we take it over. Only meaningful for a collector's own partners. */
    GENERATOR,

    /** They take waste over and pass it on: collection centre, sorting station, depot. */
    COLLECTOR,

    /**
     * They are where the waste actually ends up being recovered — a recycler, not a middleman.
     *
     * <p>Added on 24.08.2026 at the specialist's request, and it earns its place by deciding
     * something: the "Destinat:" box of Anexa 3. Her answer to A3.1 splits exactly along this
     * line — "când pleacă la colector se pot bifa valorificării şi colectării [...] iar când
     * pleacă la valorificator, doar valorificării". Without the distinction the form cannot
     * suggest anything, because the R/D code does not carry it: the same R3 goes to both.
     */
    RECOVERER
}
