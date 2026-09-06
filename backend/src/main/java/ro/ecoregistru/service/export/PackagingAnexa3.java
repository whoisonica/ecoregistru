package ro.ecoregistru.service.export;

import ro.ecoregistru.enums.PackagingMaterial;
import ro.ecoregistru.enums.PackagingOperatorRole;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.WasteOperationCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Anexa 3 la Ordinul 794/2012 — the annual report of collectors, traders, recyclers and recoverers
 * of packaging waste, assembled and ready to print.
 *
 * <p><b>Two tables, one of which is filled.</b> Art. 4 alin. (1): the operators listed there report
 * "datele prevăzute în anexa nr. 3, <b>tabelul 1 sau, după caz, tabelul 2</b>". Which one follows
 * from {@link PackagingOperatorRole}, asked once on the company profile — tabelul 1 for collectors
 * and traders, tabelul 2 for recyclers and recoverers. Both are built here; {@link #role} says
 * which one the document prints, and when it is null nothing prints at all.
 *
 * <p><b>One work point per document</b>, not a page per work point. Art. 4 alin. (4): "Raportarea
 * se realizează pentru fiecare punct de lucru în parte", and alin. (3) sends it to the agency "în
 * raza căreia îşi desfăşoară activitatea" — so two work points in two counties are two reports to
 * two addressees, and binding them into one file would produce something no client can actually
 * file. This is where it departs from {@code AnnualDeclarationGenerator}, which does print a page
 * per work point: that one is filed whole, in a single place.
 *
 * <p><b>Kilograms, not tonnes.</b> Art. 8 alin. (1) lit. a): "Cantităţile de ambalaje, respectiv de
 * deşeuri de ambalaje se raportează în kilograme." The {@code .ods} model received from the
 * specialist has "(tone)" in its column headings and only one merged "metal/aluminiu" row; it is a
 * locally modified template, the same way the anexa 1 file was, and where a model contradicts the
 * primary source on a question of law the source wins (regula de lucru 2).
 *
 * <p>Reference model: {@code documente oficiale/RAPORTARE DESEURI DE AMBALAJ COLECTATE ANUAL.ods},
 * which is the blank tabelul 1. Verbatim citations: docs/surse-oficiale.md §5.
 *
 * @param role         which table prints; null means the profile question is unanswered and the
 *                     document refuses rather than asserting a legal quality on the client's behalf
 * @param intake       what came in, one row per material and provenance — the left half of both
 *                     tables, which is identical in the two
 * @param handovers    the right half of <b>tabelul 1</b>: what was sold or sent on, per operator
 * @param treatments   the right half of <b>tabelul 2</b>: recycled vs. recovered by other means
 * @param unclassified takeovers that could not be placed on a row, and what each is missing
 */
public record PackagingAnexa3(
        PackagingOperatorRole role,
        String companyName,
        String county,
        String address,
        String contact,
        String cui,
        String caenCode,
        String authorization,
        String workPointName,
        String workPointAddress,
        int year,
        List<IntakeRow> intake,
        List<HandoverRow> handovers,
        List<TreatmentRow> treatments,
        List<UnclassifiedRow> unclassified,
        String preparedBy,
        String preparedByRole
) {

    /**
     * One line of the left half: a material, where it came from, how much of it, and how much of
     * that was hazardous.
     *
     * <p>Provenance is a <b>sub-row under the material</b>, not a column with one value — that is
     * how the received model draws it (hârtie-carton followed by populaţie / colectori / generatori
     * persoane juridice, then a "total hârtie-carton" line), and it follows from nota 2, which asks
     * the question of every quantity taken over rather than of the reporter.
     *
     * @param hazardous col. 2, "din care periculoase". Derived from the waste code's own asterisk,
     *                  never asked again: the European List already says which codes are hazardous.
     */
    public record IntakeRow(
            PackagingMaterial material,
            PackagingOrigin origin,
            BigDecimal total,
            BigDecimal hazardous
    ) {}

    /**
     * One line of tabelul 1's right half: packaging waste "comercializate/trimise la reciclare/
     * valorificare/exportate", with the operator that took it.
     *
     * <p>One line per operator, as anexa 1 asks in writing for its own tabelul 2 and as the
     * specialist confirmed on 24.08.2026 (answer B). The country is carried for exports, which the
     * column heading names explicitly; it stays null otherwise rather than repeating "România" on
     * every line.
     */
    public record HandoverRow(
            PackagingMaterial material,
            BigDecimal quantity,
            String operatorName,
            String operatorCui,
            String country
    ) {}

    /**
     * One line of tabelul 2's right half: of what this operator took over in a material, how much
     * it <b>recycled</b> and how much it recovered <b>by other means</b>, plus the method.
     *
     * <p>The split is read from the law, not guessed. OUG 92/2021 anexa nr. 3 names three
     * operations "Reciclarea/Recuperarea" in their own titles — R3, R4 and R5 — and everything else
     * under R is recovery by some other method, R1 ("întrebuinţarea în principal drept combustibil")
     * being the clearest case. See {@link WasteOperationCode#isRecycling()}.
     *
     * @param methods the R codes behind the two figures, in the order they appear, so the "metoda"
     *                cell names what was actually done instead of a single guessed code
     */
    public record TreatmentRow(
            PackagingMaterial material,
            BigDecimal recycled,
            BigDecimal otherRecovery,
            List<WasteOperationCode> methods
    ) {}

    /**
     * A takeover the tables could not use, and why — the same treatment anexa 1 gives its own
     * unplaceable movements: shown, not hidden and not guessed.
     *
     * @param missingMaterial the code does not settle the material and nobody chose one
     *                        ({@code 15 01 04} is aluminium and steel at once)
     * @param missingOrigin   nobody said where it came from, on the movement or on the partner, so
     *                        there is no sub-row to put the quantity in
     * @param missingQuantity the load has not been weighed yet, so there are no kilograms to put
     *                        anywhere. Quantity is nullable by design (decision 5) and such a
     *                        movement used to be filtered out of this document entirely — which is
     *                        the one thing the house rule forbids: a gap has to show as a gap
     */
    public record UnclassifiedRow(
            UUID movementId,
            LocalDate date,
            String wasteCode,
            BigDecimal quantity,
            String partnerName,
            boolean missingMaterial,
            boolean missingOrigin,
            boolean missingQuantity
    ) {}

    /**
     * The four figures a summed line prints, whichever table is being drawn.
     *
     * <p>Computed here rather than in each generator, and that is the point: the {@code .xls} and
     * the PDF used to add up their own totals, and they drifted. The PDF folded both of tabelul 2's
     * outbound figures into the "reciclata" column — printing 24.100 kg recycled where 22.000 were
     * recycled and 2.100 recovered by other means — while the spreadsheet had them right. On this
     * form that column is the one with legal consequences, so the two documents cannot be allowed
     * to disagree about it. One source, two printers.
     *
     * @param first  col. 4: on tabelul 1 the quantity that left, on tabelul 2 the quantity recycled
     * @param second col. 5 of tabelul 2 only: recovered by means other than recycling. Zero on
     *               tabelul 1, whose neighbouring column holds an operator's name and totals nothing
     */
    public record Totals(BigDecimal intake, BigDecimal hazardous,
                         BigDecimal first, BigDecimal second) {

        static final Totals EMPTY = new Totals(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        Totals plus(Totals other) {
            return new Totals(intake.add(other.intake), hazardous.add(other.hazardous),
                    first.add(other.first), second.add(other.second));
        }

        /** True when there is nothing at all to print — the line is then left out entirely. */
        public boolean isEmpty() {
            return intake.signum() == 0 && hazardous.signum() == 0
                    && first.signum() == 0 && second.signum() == 0;
        }
    }

    /** The summed line of one material. */
    public Totals totalsFor(PackagingMaterial material) {
        return totalsOver(List.of(material));
    }

    /** The summed line of a group — "total plastic", "total metal". */
    public Totals totalsOver(List<PackagingMaterial> materials) {
        BigDecimal intakeTotal = BigDecimal.ZERO;
        BigDecimal hazardous = BigDecimal.ZERO;
        BigDecimal first = BigDecimal.ZERO;
        BigDecimal second = BigDecimal.ZERO;

        for (IntakeRow row : intake) {
            if (materials.contains(row.material())) {
                intakeTotal = intakeTotal.add(nz(row.total()));
                hazardous = hazardous.add(nz(row.hazardous()));
            }
        }
        if (usesTable2()) {
            for (TreatmentRow row : treatments) {
                if (materials.contains(row.material())) {
                    first = first.add(nz(row.recycled()));
                    second = second.add(nz(row.otherRecovery()));
                }
            }
        } else {
            for (HandoverRow row : handovers) {
                if (materials.contains(row.material())) {
                    first = first.add(nz(row.quantity()));
                }
            }
        }
        return new Totals(intakeTotal, hazardous, first, second);
    }

    /** "TOTAL ambalaje" — every material, counted once. */
    public Totals grandTotals() {
        Totals sum = Totals.EMPTY;
        for (PackagingMaterial material : PackagingMaterial.values()) {
            sum = sum.plus(totalsFor(material));
        }
        return sum;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** True when the profile question has been answered and a table can therefore be printed. */
    public boolean printable() {
        return role != null;
    }

    /** Whether the answered role puts this company on tabelul 2 rather than tabelul 1. */
    public boolean usesTable2() {
        return role != null && role.getTable() == PackagingOperatorRole.Anexa3Table.TABEL_2;
    }
}
