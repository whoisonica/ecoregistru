package ro.ecoregistru.enums;

import lombok.Getter;

/**
 * "Modul" of the treatment column — HG 856/2002 anexa nr. 1, cap. 2, nota 2, verbatim:
 *
 * <pre>
 * 2) Modul de tratare:
 *    TM - Tratare Mecanică;  TC - Tratare Chimică;  TMC - Tratare Mecano-Chimică;
 *    TB - Tratare Biochimică;  TT - Tratare Termică;  D - Deshidratare;  A - Altele.
 * </pre>
 *
 * <p>{@code D} here is <em>deshidratare</em>, not a disposal code. The collision is the form's,
 * not ours: cap. 2 nota 2 uses a one-letter abbreviation that happens to match the D family of
 * {@link WasteOperationCode}. They live in different columns and mean different things, which is
 * why they are different types.
 */
@Getter
public enum TreatmentMethod {

    TM("Tratare mecanică"),
    TC("Tratare chimică"),
    TMC("Tratare mecano-chimică"),
    TB("Tratare biochimică"),
    TT("Tratare termică"),
    D("Deshidratare"),
    A("Altele");

    /** The wording of nota 2, verbatim. */
    private final String officialLabel;

    TreatmentMethod(String officialLabel) {
        this.officialLabel = officialLabel;
    }
}
