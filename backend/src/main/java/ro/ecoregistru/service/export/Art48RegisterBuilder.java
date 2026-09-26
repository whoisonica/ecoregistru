package ro.ecoregistru.service.export;

import org.springframework.stereotype.Component;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WeighingOperation;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteRegister;
import ro.ecoregistru.repository.WasteMovementRepository.Art48Opening;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Folds the {@code ART_48} movements of a company into {@link Art48Register}.
 *
 * <p><b>Only the art. 48 register.</b> The company's own waste is on anexa 1 (HG 856/2002 art. 2
 * alin. (1)) and never appears here; the register discriminator has been on every movement since
 * {@code V5}.
 *
 * <p><b>The opening stock is read from the earlier years</b> of the same register, the way the
 * questionnaire's correlation REC_002 reads it: opening + collected = recovered + disposed +
 * closing, per code. A company that started using the application mid-way will see the stock it
 * recorded, not the one it had; the note on the document says so.
 */
@Component
public class Art48RegisterBuilder {

    private static final BigDecimal KG_PER_TON = new BigDecimal("1000");

    /**
     * @param movementsOfYear mișcările anului cerut, din amândouă registrele (se alege ART_48 aici)
     * @param opening         soldul la 1 ianuarie, pe cod, adunat de bază din anii dinainte —
     *                        {@code WasteMovementRepository#art48OpeningBefore}. Anii vechi nu se mai
     *                        încarcă rând cu rând (20.09.2026); singurul lucru cerut de la ei era
     *                        chiar suma asta.
     */
    public Art48Register build(Company company, WorkPoint workPoint, int year,
                               List<WasteMovement> movementsOfYear, List<Art48Opening> opening) {
        List<WasteMovement> inYear = WasteRegister.ART_48.select(movementsOfYear).stream()
                .filter(m -> workPoint == null || (m.getWorkPoint() != null
                        && workPoint.getId().equals(m.getWorkPoint().getId())))
                // D2.5 — pe firmă transferul intern se anulează cu el însuși; pe depozit se vede.
                .filter(m -> workPoint != null || !m.getOperation().isTransfer())
                .sorted(Comparator.comparing(WasteMovement::getDate)
                        .thenComparing(WasteMovement::getCreatedAt,
                                Comparator.nullsLast(Comparator.<Instant>naturalOrder())))
                .toList();

        return new Art48Register(
                company.getName(),
                company.getCui(),
                workPoint == null ? null : workPoint.getName(),
                year,
                inYear.stream().map(this::entry).toList(),
                collection(inYear, opening),
                handovers(inYear, WasteOperation.RECOVERED),
                handovers(inYear, WasteOperation.DISPOSED),
                (int) inYear.stream().filter(m -> m.getQuantity() == null).count());
    }

    private Art48Register.Entry entry(WasteMovement m) {
        Partner partner = m.getPartner();
        return new Art48Register.Entry(
                m.getDate(),
                m.getWorkPoint() == null ? null : m.getWorkPoint().getName(),
                operationLabel(m.getOperation()),
                printedCode(m),
                m.getWasteCode().getName(),
                m.getQuantity() == null ? null : kg(m),
                partner == null ? otherDepot(m) : partner.getName(),
                partner == null ? null : partner.getCui(),
                origin(m),
                m.getOperationCode() == null ? null : m.getOperationCode().name(),
                m.getTransportMeans() == null ? null : m.getTransportMeans().getOfficialLabel(),
                m.getTreatmentMethod() == null ? null : m.getTreatmentMethod().getOfficialLabel(),
                m.getDocumentReference());
    }

    /** D2.5 — pe rândul unui transfer, „partenerul” e celălalt depozit al firmei. */
    private static String otherDepot(WasteMovement m) {
        WeighingOperation op = m.getWeighingOperation();
        if (op == null || !m.getOperation().isTransfer()) {
            return null;
        }
        WorkPoint other = m.getOperation() == WasteOperation.TRANSFERRED_OUT ? op.getTargetWorkPoint() : op.getWorkPoint();
        return other == null ? null : "Depozitul " + other.getName();
    }

    /**
     * D1.12 — „originea” of art. 48 alin. (1) lit. a), for what was taken over. A depot line reads it
     * from its weighing operation, which fixed it when the load came in (always {@code POPULATIE}
     * from a natural person, who has no partner record); a movement typed directly keeps the old
     * reading, its own choice before the partner's. Where nobody answered, the cell stays empty
     * rather than guessed — the same rule {@link PackagingOrigin#resolve} gives Anexa 3.
     */
    private static String origin(WasteMovement m) {
        if (m.getOperation() != WasteOperation.COLLECTED) {
            return null;
        }
        WeighingOperation op = m.getWeighingOperation();
        PackagingOrigin chosen = op != null ? op.getOrigin() : m.getPackagingOrigin();
        Partner partner = op != null && op.getPartner() != null ? op.getPartner() : m.getPartner();
        return PackagingOrigin.resolve(chosen, partner == null ? null : partner.getPackagingOrigin())
                .map(PackagingOrigin::getOfficialLabel)
                .orElse(null);
    }

    /**
     * Cap. 1, tabel 1: per code, what was in stock on 1 January, what came in, what left and how.
     * A code shows when it moved during the year or still had stock from before it.
     */
    private List<Art48Register.CodeTotal> collection(List<WasteMovement> inYear, List<Art48Opening> opening) {
        Map<String, Totals> byCode = new TreeMap<>();
        // Anii dinainte, ca sold: un rând pe cod, socotit de bază. Codul tipărit se compune la fel
        // ca în `printedCode`, ca rândurile anului să cadă peste soldul lor, nu lângă el.
        for (Art48Opening o : opening) {
            byCode.computeIfAbsent(o.getCode() + (o.getHazardous() ? "*" : ""),
                    k -> new Totals(o.getName())).opening = o.getKg();
        }
        for (WasteMovement m : inYear) {
            if (m.getQuantity() == null || !countsInStock(m.getOperation())) {
                continue;
            }
            Totals t = byCode.computeIfAbsent(printedCode(m), k -> new Totals(m.getWasteCode().getName()));
            BigDecimal kg = kg(m);
            t.moved = true;
            switch (m.getOperation()) {
                case COLLECTED -> t.collected = t.collected.add(kg);
                case RECOVERED -> {
                    t.recovered = t.recovered.add(kg);
                    if (m.getOperationCode() != null) t.recoveryCodes.add(m.getOperationCode().name());
                }
                case DISPOSED -> {
                    t.disposed = t.disposed.add(kg);
                    if (m.getOperationCode() != null) t.disposalCodes.add(m.getOperationCode().name());
                }
                case UNCLASSIFIED_OUT -> t.unclassified = t.unclassified.add(kg);
                case TRANSFERRED_IN -> t.transferredIn = t.transferredIn.add(kg);
                case TRANSFERRED_OUT -> t.transferredOut = t.transferredOut.add(kg);
                default -> { }
            }
        }

        List<Art48Register.CodeTotal> rows = new ArrayList<>();
        byCode.forEach((code, t) -> {
            if (!t.moved && t.opening.signum() == 0) {
                return;
            }
            BigDecimal closing = t.opening.add(t.collected).add(t.transferredIn)
                    .subtract(t.recovered).subtract(t.disposed).subtract(t.unclassified).subtract(t.transferredOut);
            rows.add(new Art48Register.CodeTotal(code, t.name, t.opening, t.collected, t.recovered,
                    t.disposed, t.unclassified, closing,
                    List.copyOf(t.recoveryCodes), List.copyOf(t.disposalCodes), t.transferredIn, t.transferredOut));
        });
        return rows;
    }

    /** Cap. 2, tabel A or B: the same recipient, code and operation code summed into one line. */
    private List<Art48Register.Handover> handovers(List<WasteMovement> inYear, WasteOperation operation) {
        Map<String, Art48Register.Handover> byKey = new LinkedHashMap<>();
        for (WasteMovement m : inYear) {
            if (m.getOperation() != operation || m.getQuantity() == null) {
                continue;
            }
            Partner p = m.getPartner();
            String key = (p == null ? "" : p.getId()) + "|" + printedCode(m) + "|" + m.getOperationCode();
            Art48Register.Handover seen = byKey.get(key);
            byKey.put(key, new Art48Register.Handover(
                    p == null ? null : p.getName(),
                    p == null ? null : p.getCui(),
                    p == null ? null : p.getAddress(),
                    printedCode(m),
                    m.getWasteCode().getName(),
                    seen == null ? kg(m) : seen.kg().add(kg(m)),
                    m.getOperationCode() == null ? null : m.getOperationCode().name()));
        }
        List<Art48Register.Handover> rows = new ArrayList<>(byKey.values());
        rows.sort(Comparator.comparing((Art48Register.Handover h) -> h.recipient() == null ? "" : h.recipient())
                .thenComparing(Art48Register.Handover::wasteCode));
        return rows;
    }

    private static boolean countsInStock(WasteOperation operation) {
        return operation == WasteOperation.COLLECTED || operation.isExit()
                || operation == WasteOperation.UNCLASSIFIED_OUT || operation.isTransfer();
    }

    static String operationLabel(WasteOperation operation) {
        return switch (operation) {
            case COLLECTED -> "Preluare";
            case RECOVERED -> "Predare la valorificare";
            case DISPOSED -> "Predare la eliminare";
            case UNCLASSIFIED_OUT -> "Ieșire fără cod R/D";
            case GENERATED -> "Generare";
            case TRANSFERRED_OUT -> "Transfer trimis la alt depozit";
            case TRANSFERRED_IN -> "Transfer primit de la alt depozit";
        };
    }

    /** HG 856/2002 art. 4 alin. (3): a hazardous code is printed with its asterisk. */
    private static String printedCode(WasteMovement m) {
        return m.getWasteCode().getCode() + (m.getWasteCode().isHazardous() ? "*" : "");
    }

    private static BigDecimal kg(WasteMovement m) {
        return m.getUnit() == Unit.TONS ? m.getQuantity().multiply(KG_PER_TON) : m.getQuantity();
    }

    /** Running sums for one code; mutable because it is filled in a loop and then read once. */
    private static final class Totals {
        final String name;
        BigDecimal opening = BigDecimal.ZERO;
        BigDecimal collected = BigDecimal.ZERO;
        BigDecimal recovered = BigDecimal.ZERO;
        BigDecimal disposed = BigDecimal.ZERO;
        BigDecimal unclassified = BigDecimal.ZERO;
        BigDecimal transferredIn = BigDecimal.ZERO;
        BigDecimal transferredOut = BigDecimal.ZERO;
        final TreeSet<String> recoveryCodes = new TreeSet<>(Comparator.comparingInt(Art48RegisterBuilder::codeNumber));
        final TreeSet<String> disposalCodes = new TreeSet<>(Comparator.comparingInt(Art48RegisterBuilder::codeNumber));
        boolean moved;

        Totals(String name) {
            this.name = name;
        }
    }

    /** R3 before R13: the codes sort by their number, not as text. */
    private static int codeNumber(String code) {
        return Integer.parseInt(code.substring(1));
    }
}
