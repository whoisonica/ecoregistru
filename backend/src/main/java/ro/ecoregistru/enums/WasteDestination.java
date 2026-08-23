package ro.ecoregistru.enums;

import lombok.Getter;

/**
 * "Destinaţia" of the transport columns — HG 856/2002 anexa nr. 1, cap. 2, nota 5, verbatim:
 *
 * <pre>
 * 5) Destinaţia:
 *    DO - Depozitul de gunoi al oraşului/comunei;  HP - Haldă Proprie;
 *    HC - Haldă Industrială Comună;                I  - Incinerarea în scopul eliminării;
 *    Vr - Valorificare prin agenţi economici autorizaţi;
 *    P  - Utilizare materială sau energetică în propria întreprindere;
 *    Ve - Valorificare energetică prin agenţi economici autorizaţi;   A - Altele.
 * </pre>
 *
 * <p>Not to be confused with {@link TransportDestination}, which is the "Destinat:" box of Anexa 3
 * la HG 1061/2008: that one says what a particular transport is for and takes several ticks, this
 * one says where the waste ends up and takes exactly one value. Two rubrics on two forms.
 */
@Getter
public enum WasteDestination {

    DO("Depozitul de gunoi al oraşului/comunei"),
    HP("Haldă proprie"),
    HC("Haldă industrială comună"),
    I("Incinerarea în scopul eliminării"),
    Vr("Valorificare prin agenţi economici autorizaţi"),
    P("Utilizare materială sau energetică în propria întreprindere"),
    Ve("Valorificare energetică prin agenţi economici autorizaţi"),
    A("Altele");

    /** The wording of nota 5, verbatim. */
    private final String officialLabel;

    WasteDestination(String officialLabel) {
        this.officialLabel = officialLabel;
    }
}
