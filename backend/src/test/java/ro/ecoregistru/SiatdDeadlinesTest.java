package ro.ecoregistru;

import org.junit.jupiter.api.Test;
import ro.ecoregistru.enums.SiatdModule;
import ro.ecoregistru.enums.WeighingOperationStatus;
import ro.ecoregistru.enums.WeighingOperationType;
import ro.ecoregistru.service.SiatdDeadlines;
import ro.ecoregistru.service.SiatdDeadlines.SiatdDeadline;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static ro.ecoregistru.enums.SiatdModule.MUNICIPAL;
import static ro.ecoregistru.enums.SiatdModule.PACKAGING;
import static ro.ecoregistru.enums.WeighingOperationStatus.CANCELLED;
import static ro.ecoregistru.enums.WeighingOperationStatus.FINALIZED;
import static ro.ecoregistru.enums.WeighingOperationStatus.IN_PROGRESS;
import static ro.ecoregistru.enums.WeighingOperationType.IN;

/**
 * F6a — termenul SIATD al unei recepții, calculat la citire din data ei, codurile liniilor și înrolarea firmei. Nimic
 * nu se stochează, așa că o dată de înrolare schimbată mută corect toate termenele neconfirmate.
 */
class SiatdDeadlinesTest {

    private static final LocalDate FRIDAY = LocalDate.of(2026, 10, 2);
    private static final Map<SiatdModule, LocalDate> BOTH = Map.of(
            PACKAGING, LocalDate.of(2024, 1, 1), MUNICIPAL, LocalDate.of(2024, 1, 1));

    private static SiatdDeadline in(WeighingOperationStatus status, List<String> codes, Map<SiatdModule, LocalDate> enrolment) {
        return SiatdDeadlines.of(IN, status, FRIDAY, codes, enrolment);
    }

    @Test
    void inboundPackagingGetsFiveDays() {
        SiatdDeadline d = in(FINALIZED, List.of("15 01 01"), BOTH);
        assertThat(d.modules()).containsExactly(PACKAGING);
        assertThat(d.start()).isEqualTo(FRIDAY);
        assertThat(d.due()).isEqualTo(LocalDate.of(2026, 10, 8));
        assertThat(d.reminder()).isEqualTo(LocalDate.of(2026, 10, 7));
    }

    @Test
    void mixedTakesTheShortest() {
        SiatdDeadline d = in(FINALIZED, List.of("15 01 01", "20 01 01"), BOTH);
        assertThat(d.modules()).containsExactlyInAnyOrder(PACKAGING, MUNICIPAL);
        assertThat(d.due()).isEqualTo(LocalDate.of(2026, 10, 6));
        assertThat(d.reminder()).isEqualTo(LocalDate.of(2026, 10, 5));
    }

    @Test
    void uncheckedModuleIsIgnored() {
        assertThat(in(FINALIZED, List.of("20 01 01"), Map.of(PACKAGING, LocalDate.of(2024, 1, 1)))).isNull();
        // pe o recepție mixtă, modulul nebifat nu apare și nu scurtează termenul
        SiatdDeadline mixed = in(FINALIZED, List.of("15 01 01", "20 01 01"), Map.of(PACKAGING, LocalDate.of(2024, 1, 1)));
        assertThat(mixed.modules()).containsExactly(PACKAGING);
        assertThat(mixed.due()).isEqualTo(LocalDate.of(2026, 10, 8));
    }

    @Test
    void codeWithoutModule() {
        assertThat(in(FINALIZED, List.of("17 04 05"), BOTH)).isNull();
        assertThat(in(FINALIZED, List.of("16 06 01*"), BOTH)).isNull();
        assertThat(in(FINALIZED, List.of(), BOTH)).isNull();
    }

    @Test
    void outboundTransferAdjustmentProcessingHaveNone() {
        for (WeighingOperationType type : List.of(WeighingOperationType.OUT, WeighingOperationType.TRANSFER,
                WeighingOperationType.ADJUSTMENT, WeighingOperationType.PROCESSING)) {
            assertThat(SiatdDeadlines.of(type, FINALIZED, FRIDAY, List.of("15 01 01"), BOTH)).as(type.name()).isNull();
        }
    }

    @Test
    void cancelledHasNone() {
        assertThat(in(CANCELLED, List.of("15 01 01"), BOTH)).isNull();
    }

    /** Termenul curge de la recepție, nu de la finalizare. */
    @Test
    void inProgressCounts() {
        assertThat(in(IN_PROGRESS, List.of("15 01 01"), BOTH)).isNotNull();
    }

    /** Art. 18 alin. (10): o recepție dinainte de înrolare numără de la înrolare. */
    @Test
    void enrolmentAfterReceptionMovesStart() {
        SiatdDeadline d = in(FINALIZED, List.of("15 01 01"), Map.of(PACKAGING, LocalDate.of(2026, 10, 5)));
        assertThat(d.start()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(d.due()).isEqualTo(LocalDate.of(2026, 10, 12));   // 05 + 6 = 11.10 duminică → 12.10
    }

    @Test
    void states() {
        SiatdDeadline d = in(FINALIZED, List.of("15 01 01"), BOTH);
        LocalDate due = d.due();
        assertThat(d.state(due.plusDays(10), true)).isEqualTo(SiatdDeadline.State.CONFIRMED);
        assertThat(d.state(due, false)).isEqualTo(SiatdDeadline.State.PENDING);
        assertThat(d.state(due.plusDays(1), false)).isEqualTo(SiatdDeadline.State.MISSED);
    }
}
