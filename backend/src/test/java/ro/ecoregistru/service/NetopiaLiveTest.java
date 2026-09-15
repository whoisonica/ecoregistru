package ro.ecoregistru.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F3 — codul nostru, pe sandboxul Netopia de-adevăratelea. Pornește numai cu cheile în mediu, niciodată
 * dintr-un fișier: {@code NETOPIA_LIVE_KEY}, {@code NETOPIA_LIVE_POS}, opțional {@code NETOPIA_LIVE_NOTIFY}.
 *
 * <p>Probează că cererea construită de {@link NetopiaClient} e acceptată și că răspunsul se citește: fără
 * card, Netopia dă pagina de plată (101). Plata în sine o face un om pe pagina aceea.
 */
@EnabledIfEnvironmentVariable(named = "NETOPIA_LIVE_KEY", matches = ".+")
class NetopiaLiveTest {

    static final String SANDBOX = "https://secure-sandbox.netopia-payments.com";

    private final NetopiaClient netopia = new NetopiaClient(
            RestClient.builder().baseUrl(SANDBOX).build(),
            System.getenv("NETOPIA_LIVE_KEY"),
            System.getenv("NETOPIA_LIVE_POS"),
            System.getenv().getOrDefault("NETOPIA_LIVE_NOTIFY", "https://example.com/ipn"));

    @Test
    void theSandboxGivesThePaymentPageForOurRequest() {
        String orderId = "WHLIVE-" + UUID.randomUUID().toString().substring(0, 8);
        NetopiaClient.Started started = netopia.start(orderId, new BigDecimal("389"), "Factura WH proba - abonament WasteHouse",
                new NetopiaClient.Billing("contact@wastehouse.ro", "0740000000", "Proba", "Test WasteHouse SRL",
                        "Oradea", "Bihor", "Str. Test nr. 1"),
                "https://app.wastehouse.ro/abonament?plata=proba", null);

        System.out.println("Netopia sandbox: " + orderId + " → code " + started.code() + ", status " + started.status()
                + ", ntpID " + started.ntpId() + ", paymentURL " + started.paymentUrl());
        assertThat(started.code()).isEqualTo("101");
        assertThat(started.paymentUrl()).startsWith(SANDBOX);
        assertThat(started.ntpId()).isNotBlank();
        assertThat(started.isPaid()).isFalse();
    }
}
