package ro.ecoregistru.enums;

/**
 * Which way a movement went through the gate — the question the two art. 48 screens ask.
 *
 * <p>Since 15.09.2026 the collector's register is shown on two screens, „Intrări" and „Ieșiri"
 * (the owner: „intrare și ieșire vreau să fie separat, să poți să vezi tot frumos"). The list
 * endpoint already knew „what left the site" ({@code leftSite}); this names both halves.
 *
 * <ul>
 *   <li>{@link #IN}: waste taken over from a third party — {@link WasteOperation#COLLECTED}.</li>
 *   <li>{@link #OUT}: waste that left the site — {@link WasteOperation#RECOVERED},
 *       {@link WasteOperation#DISPOSED} and the legacy {@link WasteOperation#UNCLASSIFIED_OUT},
 *       which left too and is exactly the row someone wants to repair. Same set as
 *       {@code leftSite}.</li>
 * </ul>
 *
 * <p>{@link WasteOperation#GENERATED} is in neither: it is the company's own waste sitting on the
 * site, and „Generare" shows it without a direction.
 */
public enum MovementDirection {
    IN,
    OUT;

    public boolean matches(WasteOperation operation) {
        return this == IN
                ? operation == WasteOperation.COLLECTED
                : operation == WasteOperation.RECOVERED
                        || operation == WasteOperation.DISPOSED
                        || operation == WasteOperation.UNCLASSIFIED_OUT;
    }
}
