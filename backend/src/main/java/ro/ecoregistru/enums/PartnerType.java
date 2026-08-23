package ro.ecoregistru.enums;

/**
 * What a partner is, in relation to the waste: it produces it, or it takes it over.
 *
 * <p>There is deliberately no CARRIER. Hauling is not a category of partner but a rubric of one
 * particular transport, and that is where it lives: {@code WasteMovement.transportPartner}, next
 * to the driver and the plate number, exactly as Anexa 3 la HG 1061/2008 asks for it. Any partner
 * can be named there.
 *
 * <p>This axis answers "what are they"; the commercial role on {@link ro.ecoregistru.entity.Partner}
 * — client / supplier — answers "which way does the invoice travel". They are independent: the
 * collector who buys our cardboard is a COLLECTOR and a client, the one who empties our bins is a
 * COLLECTOR and a supplier.
 */
public enum PartnerType {

    /** They produce the waste; we take it over. Only meaningful for a collector's own partners. */
    GENERATOR,

    /** They take waste over: collection centre, sorting station, treatment plant, landfill. */
    COLLECTOR
}
