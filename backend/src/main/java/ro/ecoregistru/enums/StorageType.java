package ro.ecoregistru.enums;

import lombok.Getter;

/**
 * "Tipul" of the storage column — HG 856/2002 anexa nr. 1, cap. 2, nota 1, verbatim:
 *
 * <pre>
 * 1) Tipul de stocare:
 *    RM - Recipient Metalic;  RP - Recipient de Plastic;  BZ - Bazin Decantor;
 *    CT - Container Transportabil;  CF - Container Fix;  S - Saci;
 *    PD - Platformă de Deshidratare;  VN - în Vrac, Neacoperit;
 *    VA - în Vrac, incintă Acoperită;  RL - Recipient din Lemn;  A - Altele.
 * </pre>
 *
 * The letters are the abbreviations the form itself prints, so they are the enum names.
 */
@Getter
public enum StorageType {

    RM("Recipient metalic"),
    RP("Recipient de plastic"),
    BZ("Bazin decantor"),
    CT("Container transportabil"),
    CF("Container fix"),
    S("Saci"),
    PD("Platformă de deshidratare"),
    VN("În vrac, neacoperit"),
    VA("În vrac, incintă acoperită"),
    RL("Recipient din lemn"),
    A("Altele");

    /** The wording of nota 1, verbatim. */
    private final String officialLabel;

    StorageType(String officialLabel) {
        this.officialLabel = officialLabel;
    }
}
