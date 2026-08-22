package ro.ecoregistru.enums;

/**
 * Waste treatment operation codes from the EU Waste Framework Directive / OUG 92/2021 annexes 3
 * and 7. R codes = recovery (valorificare), D codes = disposal (eliminare). These are stable.
 *
 * <p>Captured on every movement that takes waste off the site, because that is exactly what the
 * form asks for: HG 856/2002 anexa nr. 1 cap. 3 has the columns <em>"Cantitatea de deşeu
 * valorificată | Operaţia de valorificare | Agentul economic care efectuează operaţia"</em>, and
 * cap. 4 the same for disposal. A quantity cannot appear there without its operation and its
 * operator — see docs/surse-oficiale.md §1.2, §2.2 and §2.3.
 *
 * <p>The code also decides which cap. 1 column the quantity lands in, which is why the V/E purpose
 * is derived from it rather than asked for twice.
 */
public enum WasteOperationCode {
    R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13,
    D1, D2, D3, D4, D5, D6, D7, D8, D9, D10, D11, D12, D13, D14, D15;

    public boolean isRecovery() {
        return name().charAt(0) == 'R';
    }

    public boolean isDisposal() {
        return name().charAt(0) == 'D';
    }

    /**
     * The "Scopul" letter this operation implies: an R code is recovery, a D code is disposal.
     * Anexa 1 cap. 1 has no "handed over" column, so this is what places the quantity in
     * "valorificată" or in "eliminată final".
     */
    public TreatmentPurpose treatmentPurpose() {
        return isRecovery() ? TreatmentPurpose.V : TreatmentPurpose.E;
    }
}
