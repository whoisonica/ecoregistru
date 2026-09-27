package ro.ecoregistru.service.export;

import org.junit.jupiter.api.Test;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.WasteCode;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteRegister;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F5 — stocul final al Cap. 1 cuprinde tratarea: ce a intrat la presă scade, ce a rezultat adaugă. Pe același cod (balotarea
 * de azi) cele două se anulează, deci regula se vede doar când codul se schimbă — sortarea care vine (20 01 01 → 19 12 01).
 */
class Art48TreatmentTotalsTest {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 27);

    @Test
    void theClosingStockMovesTheTreatedQuantityFromTheInputCodeToTheResultCode() {
        WasteCode paper = code("20 01 01");
        WasteCode sorted = code("19 12 01");
        Art48Register r = new Art48RegisterBuilder().build(Company.builder().name("Probă").build(), null, 2026, List.of(
                line(paper, WasteOperation.COLLECTED, "1000"),
                line(paper, WasteOperation.PROCESSING_INPUT, "1000"),
                line(sorted, WasteOperation.PROCESSING_OUTPUT, "950")), List.of(), List.of());

        assertThat(r.collection()).extracting(Art48Register.CodeTotal::wasteCode, c -> c.closingKg().stripTrailingZeros().toPlainString())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("19 12 01", "950"),
                        org.assertj.core.groups.Tuple.tuple("20 01 01", "0"));
    }

    private static WasteCode code(String code) {
        return WasteCode.builder().id(UUID.randomUUID()).code(code).name(code).hazardous(false).build();
    }

    private static WasteMovement line(WasteCode code, WasteOperation operation, String kg) {
        return WasteMovement.builder().wasteCode(code).operation(operation).register(WasteRegister.ART_48)
                .quantity(new BigDecimal(kg)).unit(Unit.KG).date(DAY).build();
    }
}
