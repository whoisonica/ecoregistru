package ro.ecoregistru.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * F2 — {@link FgoClient} against the real FGO <b>test</b> environment. Skipped unless the key is
 * given in the environment, so neither the key nor a network call ever reaches CI:
 *
 * <pre>FGO_LIVE_COD_UNIC=... FGO_LIVE_KEY=... FGO_LIVE_SERIE=... ./gradlew test --tests '*FgoLiveTest'</pre>
 *
 * <p>The address is fixed to api-testuat on purpose: this test issues invoices, and must not be
 * pointed at production by an environment variable.
 */
@EnabledIfEnvironmentVariable(named = "FGO_LIVE_KEY", matches = ".+")
class FgoLiveTest {

    private static final String TEST_ENVIRONMENT = "https://api-testuat.fgo.ro/v1";

    private final FgoClient fgo = new FgoClient(
            RestClient.builder().baseUrl(TEST_ENVIRONMENT).build(),
            System.getenv("FGO_LIVE_COD_UNIC"), System.getenv("FGO_LIVE_KEY"), System.getenv("FGO_LIVE_SERIE"),
            System.getenv().getOrDefault("FGO_LIVE_TIP_FACTURA", "Factura"),
            new BigDecimal(System.getenv().getOrDefault("FGO_LIVE_COTA_TVA", "0")),
            // FGO emite numai pentru o platformă înregistrată (Setări → eCommerce → Setări API).
            System.getenv().getOrDefault("FGO_LIVE_PLATFORMA_URL", "https://app.wastehouse.ro"), 1000);

    private final FgoClient.Buyer buyer = new FgoClient.Buyer("Test WasteHouse SRL", "51779887",
            "contact@wastehouse.ro", "Bucuresti", "Bucuresti", "Str. Test nr. 1");

    /** Emitere, getstatus, apoi aceeași emitere cu același IdExtern: nu trebuie să apară o a doua factură. */
    @Test
    void issueReadAndRepeatTheSameExternalId() {
        String externalId = UUID.randomUUID().toString();
        LocalDate today = LocalDate.now(BillingRunService.ZONE);
        List<BillingCalculator.Line> lines = List.of(
                new BillingCalculator.Line("Generator", 1, new BigDecimal("99"), new BigDecimal("99")),
                new BillingCalculator.Line("Implementare", 1, new BigDecimal("290"), new BigDecimal("290")));

        FgoClient.Issued first = fgo.emit(externalId, buyer, lines, today, today.plusDays(10), "Test F2");
        System.out.println("FGO emitere: " + first);
        assertThat(first.numar()).isNotBlank();

        FgoClient.Status status = fgo.status(first.serie(), first.numar());
        System.out.println("FGO getstatus: " + status);
        assertThat(status.value()).isEqualByComparingTo("389");
        assertThat(status.isPaid()).isFalse();

        Throwable repeated = catchThrowable(() -> {
            FgoClient.Issued second = fgo.emit(externalId, buyer, lines, today, today.plusDays(10), "Test F2");
            System.out.println("FGO emitere repetată: " + second);
            assertThat(second.numar()).as("același IdExtern nu emite altă factură").isEqualTo(first.numar());
        });
        System.out.println("FGO emitere repetată, excepție: " + repeated);
        if (repeated instanceof AssertionError e) {
            throw e;
        }
    }
}
