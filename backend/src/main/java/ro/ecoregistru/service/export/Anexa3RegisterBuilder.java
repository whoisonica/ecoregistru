package ro.ecoregistru.service.export;

import org.springframework.stereotype.Component;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.util.WasteCodeLabel;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Folds the handovers of a year into {@link Anexa3Register}: only the movements with an allocated
 * Anexa 3 number, in the order of those numbers.
 */
@Component
public class Anexa3RegisterBuilder {

    private static final BigDecimal KG_PER_TON = new BigDecimal("1000");

    public Anexa3Register build(Company company, WorkPoint workPoint, int year, List<WasteMovement> movements) {
        List<WasteMovement> issued = movements.stream()
                // Un formular emis = o predare către un partener căreia i s-a alocat numărul la tipărire.
                .filter(m -> m.getAnexa3Number() != null)
                .filter(m -> m.getOperation().isExit() && m.getPartner() != null)
                .filter(m -> workPoint == null || sameWorkPoint(m, workPoint))
                .sorted(Comparator.comparing(WasteMovement::getAnexa3Number)
                        .thenComparing(WasteMovement::getDate))
                .toList();

        List<Anexa3Register.Row> rows = new ArrayList<>(issued.size());
        for (WasteMovement m : issued) {
            Partner p = m.getPartner();
            rows.add(new Anexa3Register.Row(
                    rows.size() + 1,
                    m.getDate(),
                    m.getAnexa3Series(),
                    m.getAnexa3Number(),
                    m.getQuantity() == null ? null : kg(m),
                    WasteCodeLabel.official(m.getWasteCode().getCode(), m.getWasteCode().isHazardous()),
                    m.getWasteCode().getName(),
                    p.getName(),
                    p.getCui(),
                    m.getOperationCode() == null ? null : m.getOperationCode().name()));
        }
        return new Anexa3Register(company.getName(), company.getCui(),
                workPoint == null ? null : workPoint.getName(), year, List.copyOf(rows));
    }

    private static boolean sameWorkPoint(WasteMovement m, WorkPoint workPoint) {
        return m.getWorkPoint() != null && m.getWorkPoint().getId().equals(workPoint.getId());
    }

    private static BigDecimal kg(WasteMovement m) {
        return m.getUnit() == Unit.TONS ? m.getQuantity().multiply(KG_PER_TON) : m.getQuantity();
    }
}
