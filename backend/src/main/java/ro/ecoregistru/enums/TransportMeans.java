package ro.ecoregistru.enums;

import lombok.Getter;

/**
 * "Mijlocul" of the transport columns — HG 856/2002 anexa nr. 1, cap. 2, nota 4, verbatim:
 *
 * <pre>
 * 4) Mijlocul de transport:
 *    AS - Autospeciale;  AN - Auto Nespecial;  H - Transport Hidraulic;
 *    CF - Cale Ferată;   A - Altele.
 * </pre>
 */
@Getter
public enum TransportMeans {

    AS("Autospeciale"),
    AN("Auto nespecial"),
    H("Transport hidraulic"),
    CF("Cale ferată"),
    A("Altele");

    /** The wording of nota 4, verbatim. */
    private final String officialLabel;

    TransportMeans(String officialLabel) {
        this.officialLabel = officialLabel;
    }
}
