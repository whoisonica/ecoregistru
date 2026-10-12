package ro.ecoregistru.service.export;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import ro.ecoregistru.controller.response.EnvironmentalReportResponse;
import ro.ecoregistru.entity.Company;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The RAM's Word file on hand-made data: what the waste table marks, and what an unanswered chapter prints. */
class EnvironmentalReportDocxGeneratorTest {

    private static final int YEAR = 2025;

    private final EnvironmentalReportDocxGenerator generator = new EnvironmentalReportDocxGenerator();

    private static EnvironmentalReportResponse.Permit permit(LocalDate revisedOn) {
        return new EnvironmentalReportResponse.Permit("17", null, "APM Cluj", LocalDate.of(2015, 12, 20), revisedOn,
                null, 1, 15, null, null, 10, 21, null, "Maria Ionescu");
    }

    private static EnvironmentalReportResponse report(LocalDate revisedOn,
                                                      List<EnvironmentalReportResponse.Monitoring> monitorings) {
        return new EnvironmentalReportResponse(YEAR, permit(revisedOn), false, null, false, null, false, false, null,
                null, List.of(), monitorings);
    }

    private static AnnualDeclaration.Row row(String code, String generated, String closing, String unclassifiedOut,
                                             boolean awaitingWeighing) {
        return new AnnualDeclaration.Row(code, "Ambalaje de hârtie și carton", false, BigDecimal.ZERO,
                new BigDecimal(generated), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal(closing), null, null,
                unclassifiedOut == null ? null : new BigDecimal(unclassifiedOut), awaitingWeighing);
    }

    private static AnnualDeclaration declaration(List<AnnualDeclaration.Row> rows) {
        return new AnnualDeclaration("Mediu SRL", null, "RO1", null, "17", null, YEAR, "Cluj", null, null, null,
                null, rows);
    }

    private static final PackagingDeclaration NO_PACKAGING = new PackagingDeclaration("Mediu SRL", null, null, null,
            null, "RO1", YEAR, List.of(), List.of(), List.of(), null, null);

    private String text(EnvironmentalReportResponse report, List<AnnualDeclaration> waste) throws IOException {
        Company company = Company.builder().name("Mediu SRL").cui("RO1").build();
        byte[] body = generator.render(new EnvironmentalReportDocument(company, YEAR, report, List.of(), waste,
                NO_PACKAGING));
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(body));
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }

    @Test
    void aRowThatDoesNotAddUpSaysWhyAsTheDeclarationDoes() throws IOException {
        // 0 + 40 − 0 − 0 = 0: the 40 kg left without an R/D code. And a load still waiting for its weight.
        String text = text(report(null, List.of()), List.of(declaration(List.of(
                row("15 01 01", "40", "0", "40", false),
                row("15 01 02", "120", "0", null, true)))));
        assertThat(text)
                .contains("0 (*)")
                .contains("120 (**)")
                .contains(EnvironmentalReportDocxGenerator.UNCLASSIFIED_NOTE)
                .contains(EnvironmentalReportDocxGenerator.AWAITING_WEIGHING_NOTE);
    }

    @Test
    void aRowThatAddsUpHasNoMarkAndNoNote() throws IOException {
        String text = text(report(null, List.of()), List.of(declaration(List.of(row("15 01 01", "40", "40", null,
                false)))));
        assertThat(text).doesNotContain("(*)").doesNotContain("(**)");
    }

    @Test
    void anEmptyMonitoringListAndAMissingRevisionAreLinesToFill() throws IOException {
        String text = text(report(null, List.of()), List.of());
        String monitorings = text.substring(text.indexOf("9. MONITORIZĂRI"));
        assertThat(monitorings).doesNotContain("Nu a fost cazul").contains("……");
        assertThat(text).doesNotContain("nu a fost revizuită").contains("Ultima revizuire: ……");

        String revised = text(report(LocalDate.of(2022, 3, 10), List.of(new EnvironmentalReportResponse.Monitoring(
                "Cluj", "Zgomot", LocalDate.of(2025, 11, 14), "Z-1", null, "Fără depășiri."))), List.of());
        assertThat(revised).contains("Ultima revizuire s-a făcut la data de 10.03.2022.", "Zgomot");
    }
}
