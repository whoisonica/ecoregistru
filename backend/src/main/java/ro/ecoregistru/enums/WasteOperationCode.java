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
        // Null, not E: disposal leaves the "Scopul" cell empty on every filled form we have.
        // See TreatmentPurpose for the evidence.
        return isRecovery() ? TreatmentPurpose.V : null;
    }

    /**
     * Whether this operation is <b>recycling</b>, as opposed to recovery by some other means.
     *
     * <p>Needed by tabelul 2 of Anexa 3 la Ordinul 794/2012, which splits what a recycler did into
     * two columns: "cantitatea reciclată" and "cantitatea valorificată (numai prin alte metode
     * decât reciclarea)". The annex does not define the boundary, but it does not have to — the
     * operations are defined in <b>OUG 92/2021, anexa nr. 3</b>, and exactly three of them are
     * named "Reciclarea" in their own titles:
     *
     * <pre>
     *   R3  Reciclarea/Recuperarea substanţelor organice care nu sunt utilizate ca solvenţi
     *   R4  Reciclarea/Recuperarea metalelor şi compuşilor metalici
     *   R5  Reciclarea/Recuperarea altor materiale anorganice
     * </pre>
     *
     * <p>So the split is <b>read</b>, not guessed: R3, R4 and R5 are recycling and everything else
     * under R is the other column. The three cover the packaging materials the form has rows for —
     * paper, wood and plastics under R3, aluminium and steel under R4, glass under R5 — which is
     * a good sign the reading is the intended one rather than a coincidence of wording.
     *
     * <p>The clearest counter-example is the one the ordinance and the Waste Framework Directive
     * agree on: <b>R1 is "întrebuinţarea în principal drept combustibil"</b>, burning it for
     * energy, which art. 3 pct. 17 of the directive excludes from recycling by name. A quantity
     * under R1 belongs in "valorificată prin alte metode" and nowhere else.
     *
     * <p>False for every D code: disposal is not recovery at all, so it belongs in neither column.
     * See docs/surse-oficiale.md §2.2.
     */
    public boolean isRecycling() {
        return this == R3 || this == R4 || this == R5;
    }
}
