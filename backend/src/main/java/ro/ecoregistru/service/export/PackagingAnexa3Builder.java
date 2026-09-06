package ro.ecoregistru.service.export;

import org.springframework.stereotype.Component;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.PackagingMaterial;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteOperationCode;
import ro.ecoregistru.enums.WasteRegister;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assembles Anexa 3 la Ordinul 794/2012 out of the movements of one work point and one year.
 *
 * <p><b>The other end of the chain from {@link PackagingDeclarationBuilder}.</b> Anexa 1 reports
 * what the company <em>put on the market</em>; anexa 3 reports what it <em>took over from third
 * parties</em> and what became of it. The two read disjoint sets of movements, which is exactly
 * what the corpus shows for the specialist's own firm: the 2021 anexa 1 of HRR reports only steel,
 * a figure absent from all of their waste records, while their {@code 15 01 02} appears in the
 * records and not in the declaration.
 *
 * <p><b>Which movements.</b> Only the {@code ART_48} register: goods taken over from third parties.
 * HG 856/2002 art. 2 alin. (1) keeps those out of anexa 1 entirely, and the register discriminator
 * has been on every movement since {@code V5}. Within that, only {@code 15 01 xx} codes, the same
 * narrow filter anexa 1 uses and for the same reason.
 *
 * <ul>
 *   <li>the left half of both tables comes from {@link WasteOperation#COLLECTED}, the takeover
 *       itself, which is the only operation that can carry a provenance;</li>
 *   <li>tabelul 1's right half comes from the exits with an operator named, what was sold or sent
 *       on;</li>
 *   <li>tabelul 2's right half comes from the exits' R codes, split by
 *       {@link WasteOperationCode#isRecycling()}.</li>
 * </ul>
 *
 * <p><b>Where the data lives today, and where it is going.</b> Takeovers are {@code COLLECTED} rows
 * in {@code waste_movements}. The depot module (Etapa 8) will move them into the
 * {@code receptions} / {@code deliveries} tables that {@code V5} created empty, and when it does,
 * only the two queries at the top of {@link #build} change; everything below them works on the
 * aggregated rows. That is deliberate. This document is buildable now because its <em>form</em> is
 * legible from the act and from a filled model, while the art. 48 register's is not (question AD),
 * and the two should not be held hostage to each other.
 */
@Component
public class PackagingAnexa3Builder {

    private static final BigDecimal KG_PER_TON = new BigDecimal("1000");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public PackagingAnexa3 build(Company company,
                                 WorkPoint workPoint,
                                 int year,
                                 List<WasteMovement> movements) {

        List<WasteMovement> scoped = movements.stream()
                .filter(m -> m.getRegister() == WasteRegister.ART_48)
                .filter(m -> PackagingMaterial.isPackagingCode(m.getWasteCode().getCode()))
                .filter(m -> workPoint == null || sameWorkPoint(m, workPoint))
                .sorted(Comparator.comparing(WasteMovement::getDate))
                .toList();

        List<PackagingAnexa3.UnclassifiedRow> unclassified = new ArrayList<>();

        List<WasteMovement> takeovers = scoped.stream()
                .filter(m -> m.getOperation() == WasteOperation.COLLECTED)
                .toList();
        List<WasteMovement> exits = scoped.stream()
                .filter(m -> m.getOperation().isExit())
                .toList();

        // Computed before the constructor call, not inside its argument list: all three of these
        // append to `unclassified`, and relying on Java's left-to-right argument evaluation to get
        // the order right would be a trap for the next person to reorder the record.
        List<PackagingAnexa3.IntakeRow> intake = intake(takeovers, unclassified);
        List<PackagingAnexa3.HandoverRow> handovers = handovers(exits);
        List<PackagingAnexa3.TreatmentRow> treatments = treatments(exits);
        // An exit still waiting for its weight is just as invisible on the right half as an
        // unweighed takeover is on the left, so both are reported the same way.
        collectUnweighed(exits, unclassified);

        return new PackagingAnexa3(
                company.getPackagingOperatorRole(),
                company.getName(),
                // Same rubric as on anexa 1, and the same reason it stays empty: we keep one free
                // address, and printing it twice would look like two answers to two questions.
                null,
                company.getAddress(),
                contact(company),
                company.getCui(),
                company.getCaenCode(),
                authorization(company),
                workPoint == null ? null : workPoint.getName(),
                workPoint == null ? null : workPoint.getAddress(),
                year,
                intake,
                handovers,
                treatments,
                unclassified,
                company.getContactName(),
                company.getContactRole());
    }

    // ------------------------------------------------------------ left half (both tables)

    /**
     * One row per material and provenance that has a quantity, in the order the form draws them:
     * materials as {@link PackagingMaterial} declares them, and within each material the
     * provenances in the order of nota 2.
     *
     * <p>A takeover whose material or provenance nobody answered stays <b>out</b> of the table and
     * is listed as unclassified. Both halves of that are the house rule: a form filed with an
     * authority gets no guessed rubric, and what is missing is shown to be missing.
     */
    private List<PackagingAnexa3.IntakeRow> intake(
            List<WasteMovement> takeovers,
            List<PackagingAnexa3.UnclassifiedRow> unclassified) {

        Map<PackagingMaterial, Map<PackagingOrigin, Sums>> grid = new LinkedHashMap<>();

        for (WasteMovement m : takeovers) {
            PackagingMaterial material = PackagingDeclarationBuilder.materialOf(m).orElse(null);
            PackagingOrigin origin = PackagingOrigin
                    .resolve(m.getPackagingOrigin(), originOf(m.getPartner()))
                    .orElse(null);

            if (m.getQuantity() == null || material == null || origin == null) {
                unclassified.add(new PackagingAnexa3.UnclassifiedRow(
                        m.getId(), m.getDate(), m.getWasteCode().getCode(),
                        m.getQuantity() == null ? null : kg(m),
                        m.getPartner() == null ? null : m.getPartner().getName(),
                        material == null, origin == null, m.getQuantity() == null));
                continue;
            }

            Sums sums = grid.computeIfAbsent(material, k -> new LinkedHashMap<>())
                    .computeIfAbsent(origin, k -> new Sums());
            sums.total = sums.total.add(kg(m));
            if (m.getWasteCode().isHazardous()) {
                sums.hazardous = sums.hazardous.add(kg(m));
            }
        }

        List<PackagingAnexa3.IntakeRow> rows = new ArrayList<>();
        for (PackagingMaterial material : PackagingMaterial.values()) {
            Map<PackagingOrigin, Sums> byOrigin = grid.get(material);
            if (byOrigin == null) {
                continue;
            }
            for (PackagingOrigin origin : PackagingOrigin.values()) {
                Sums sums = byOrigin.get(origin);
                if (sums != null) {
                    rows.add(new PackagingAnexa3.IntakeRow(
                            material, origin, sums.total, sums.hazardous));
                }
            }
        }
        return rows;
    }

    // ------------------------------------------------------------ tabelul 1, right half

    /**
     * What left, per material and operator: the column headed "Deseuri de ambalaje comercializate/
     * trimise la reciclare/valorificare/exportate", with "Operatorul economic" beside it.
     *
     * <p>One line per operator, which is how anexa 1 asks it in writing for its own tabelul 2 and
     * what the specialist confirmed on 24.08.2026 (answer B). An exit with nobody named is left
     * out: the column is the operator's name, and there is nothing to write in it.
     */
    private List<PackagingAnexa3.HandoverRow> handovers(List<WasteMovement> exits) {
        Map<String, PackagingAnexa3.HandoverRow> byKey = new LinkedHashMap<>();

        for (WasteMovement m : exits) {
            Partner partner = m.getPartner();
            PackagingMaterial material = PackagingDeclarationBuilder.materialOf(m).orElse(null);
            if (partner == null || material == null || m.getQuantity() == null) {
                continue;
            }
            String key = material.name() + "|" + partner.getId();
            PackagingAnexa3.HandoverRow existing = byKey.get(key);
            BigDecimal quantity = existing == null ? kg(m) : existing.quantity().add(kg(m));
            byKey.put(key, new PackagingAnexa3.HandoverRow(
                    material, quantity, partner.getName(), partner.getCui(),
                    // The heading names exports separately and asks for the country there. We do
                    // not model a partner's country, so it stays empty rather than printing
                    // "Romania" on lines nobody said were domestic.
                    null));
        }
        return sortedByMaterial(new ArrayList<>(byKey.values()),
                PackagingAnexa3.HandoverRow::material);
    }

    // ------------------------------------------------------------ tabelul 2, right half

    /**
     * Per material: how much was recycled, how much was recovered by other means, and the R codes
     * behind the two figures.
     *
     * <p>D codes are counted in neither column, and that is not an omission: disposal is not
     * recovery, and tabelul 2 has no cell for it. A quantity disposed of still shows in the left
     * half, as taken over, and then simply has no line on the right, which is the truthful picture.
     */
    private List<PackagingAnexa3.TreatmentRow> treatments(List<WasteMovement> exits) {
        Map<PackagingMaterial, Sums> recycled = new LinkedHashMap<>();
        Map<PackagingMaterial, Sums> other = new LinkedHashMap<>();
        Map<PackagingMaterial, List<WasteOperationCode>> methods = new LinkedHashMap<>();

        for (WasteMovement m : exits) {
            WasteOperationCode code = m.getOperationCode();
            PackagingMaterial material = PackagingDeclarationBuilder.materialOf(m).orElse(null);
            if (code == null || material == null || !code.isRecovery()
                    || m.getQuantity() == null) {
                continue;
            }
            Map<PackagingMaterial, Sums> bucket = code.isRecycling() ? recycled : other;
            Sums sums = bucket.computeIfAbsent(material, k -> new Sums());
            sums.total = sums.total.add(kg(m));

            List<WasteOperationCode> seen = methods.computeIfAbsent(material, k -> new ArrayList<>());
            if (!seen.contains(code)) {
                seen.add(code);
            }
        }

        List<PackagingAnexa3.TreatmentRow> rows = new ArrayList<>();
        for (PackagingMaterial material : PackagingMaterial.values()) {
            Sums r = recycled.get(material);
            Sums o = other.get(material);
            if (r == null && o == null) {
                continue;
            }
            rows.add(new PackagingAnexa3.TreatmentRow(
                    material,
                    r == null ? null : r.total,
                    o == null ? null : o.total,
                    methods.getOrDefault(material, List.of())));
        }
        return rows;
    }

    /**
     * Exits whose weight has not come back yet. They carry no kilograms, so neither right-hand half
     * can use them; naming them here is what keeps the omission visible instead of silent.
     */
    private void collectUnweighed(List<WasteMovement> exits,
                                  List<PackagingAnexa3.UnclassifiedRow> unclassified) {
        for (WasteMovement m : exits) {
            if (m.getQuantity() == null) {
                unclassified.add(new PackagingAnexa3.UnclassifiedRow(
                        m.getId(), m.getDate(), m.getWasteCode().getCode(), null,
                        m.getPartner() == null ? null : m.getPartner().getName(),
                        false, false, true));
            }
        }
    }

    // ------------------------------------------------------------ helpers

    /** The provenance answered once on the partner. Null for a movement with no partner at all. */
    private PackagingOrigin originOf(Partner partner) {
        return partner == null ? null : partner.getPackagingOrigin();
    }

    private boolean sameWorkPoint(WasteMovement m, WorkPoint workPoint) {
        return m.getWorkPoint() != null && workPoint.getId().equals(m.getWorkPoint().getId());
    }

    /**
     * The header rubric of the model: "Autorizatie de mediu/nr inregistrare/data/valabilitate".
     * Built from what we hold, and empty when the client has filled in neither, because a lone
     * slash on an official form reads as a mistake rather than as an unanswered question.
     */
    private String authorization(Company company) {
        String number = company.getEnvironmentalAuthNumber();
        LocalDate expiry = company.getEnvironmentalAuthExpiry();
        if (number == null && expiry == null) {
            return null;
        }
        if (expiry == null) {
            return number;
        }
        String valid = "valabila pana la " + expiry.format(DATE);
        return number == null ? valid : number + " / " + valid;
    }

    private String contact(Company company) {
        StringBuilder sb = new StringBuilder();
        if (company.getContactPhone() != null) {
            sb.append(company.getContactPhone());
        }
        if (company.getContactEmail() != null) {
            if (!sb.isEmpty()) {
                sb.append(" / ");
            }
            sb.append(company.getContactEmail());
        }
        return sb.toString();
    }

    /** Art. 8 alin. (1) lit. a): everything on this form is reported in kilograms. */
    private BigDecimal kg(WasteMovement m) {
        return m.getUnit() == Unit.TONS ? m.getQuantity().multiply(KG_PER_TON) : m.getQuantity();
    }

    /** Keeps the printed order of {@link PackagingMaterial} whatever order the rows arrived in. */
    private <T> List<T> sortedByMaterial(List<T> rows,
                                         java.util.function.Function<T, PackagingMaterial> key) {
        rows.sort(Comparator.comparingInt(r -> key.apply(r).ordinal()));
        return rows;
    }

    /** A running pair of totals; mutable because it is summed in a loop and then read once. */
    private static final class Sums {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal hazardous = BigDecimal.ZERO;
    }
}
