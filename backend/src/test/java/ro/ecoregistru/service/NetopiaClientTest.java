package ro.ecoregistru.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * F3 — ce pleacă la Netopia și cum se citește răspunsul. Răspunsurile de mai jos sunt copiate din probele pe
 * sandbox din 15.09.2026 (pagina de plată, 101; cardul de test fără 3-D Secure, 00). Dacă Netopia le acceptă
 * se probează pe sandbox, cu cheia.
 */
class NetopiaClientTest {

    static final NetopiaClient.Billing BILLING = new NetopiaClient.Billing("facturi@firma.ro", "0740000000",
            "Ion Popescu", "Firma Test SRL", "Cluj-Napoca", "Cluj", "Str. Memorandumului nr. 1");

    @Test
    void withoutATokenItAsksForThePaymentPage() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://netopia.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NetopiaClient netopia = new NetopiaClient(builder.build(), "cheie-api", "POS-SEMNATURA", "https://api.test/ipn");

        server.expect(requestTo("https://netopia.test/payment/card/start"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "cheie-api"))
                .andExpect(jsonPath("$.config.notifyUrl").value("https://api.test/ipn"))
                .andExpect(jsonPath("$.config.redirectUrl").value("https://app.test/abonament?plata=1"))
                .andExpect(jsonPath("$.payment.instrument.type").value("card"))
                .andExpect(jsonPath("$.payment.instrument.token").doesNotExist())
                .andExpect(jsonPath("$.payment.instrument.account").doesNotExist())
                .andExpect(jsonPath("$.order.posSignature").value("POS-SEMNATURA"))
                .andExpect(jsonPath("$.order.orderID").value("WH7-abcd1234"))
                .andExpect(jsonPath("$.order.amount").value(389.0))
                .andExpect(jsonPath("$.order.currency").value("RON"))
                .andExpect(jsonPath("$.order.billing.country").value(642))
                .andExpect(jsonPath("$.order.billing.state").value("Cluj"))
                .andRespond(withSuccess("""
                        {"customerAction":{},"error":{"code":"101","message":"Redirect user to payment page"},
                         "payment":{"amount":389,"binding":{"expireMonth":0,"expireYear":0},"currency":"RON",
                         "instrument":{"country":0},"ntpID":"3019383",
                         "paymentURL":"https://secure-sandbox.netopia-payments.com/ui/card?p=abc","status":1}}
                        """, MediaType.APPLICATION_JSON));

        NetopiaClient.Started started = netopia.start("WH7-abcd1234", new BigDecimal("389"), "Factura WH 7",
                BILLING, "https://app.test/abonament?plata=1", null);

        server.verify();
        assertThat(started.paymentUrl()).isEqualTo("https://secure-sandbox.netopia-payments.com/ui/card?p=abc");
        assertThat(started.ntpId()).isEqualTo("3019383");
        assertThat(started.isPaid()).isFalse();
        assertThat(started.token()).isNull();
    }

    /** „token overrides all other data": nu pleacă tipul, nici vreun număr de card. */
    @Test
    void withATokenOnlyTheTokenGoes() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://netopia.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NetopiaClient netopia = new NetopiaClient(builder.build(), "cheie-api", "POS-SEMNATURA", "https://api.test/ipn");

        server.expect(requestTo("https://netopia.test/payment/card/start"))
                .andExpect(jsonPath("$.payment.instrument.token").value("eE10aklrbGxPRE05"))
                .andExpect(jsonPath("$.payment.instrument.type").doesNotExist())
                .andRespond(withSuccess("""
                        {"customerAction":{},"error":{"code":"00","message":"Approved"},
                         "payment":{"amount":99,"binding":{"expireMonth":12,"expireYear":2030,"token":"eE10aklrbGxPRE05"},
                         "currency":"RON","data":{"AuthCode":"brlN"},"instrument":{"country":0,"panMasked":"9900****5098"},
                         "ntpID":"3019439","status":3}}
                        """, MediaType.APPLICATION_JSON));

        NetopiaClient.Started started = netopia.start("WH8-x", new BigDecimal("99"), "Factura WH 8", BILLING,
                "https://app.test/abonament", "eE10aklrbGxPRE05");

        server.verify();
        assertThat(started.isPaid()).isTrue();
        assertThat(started.code()).isEqualTo("00");
        assertThat(started.token()).isEqualTo("eE10aklrbGxPRE05");
        assertThat(started.panMasked()).isEqualTo("9900****5098");
        assertThat(started.expireMonth()).isEqualTo(12);
    }

    /** Proba de pe sandbox cu cardul de test: aprobată, dar `binding` gol — tokenul lipsă e null, nu „". */
    @Test
    void anApprovedPaymentWithoutBindingHasNoToken() throws Exception {
        NetopiaClient.Started started = NetopiaClient.parse(new ObjectMapper().readTree("""
                {"customerAction":{},"error":{"code":"00","message":"Approved"},"payment":{"amount":1,
                 "binding":{"expireMonth":0,"expireYear":0},"currency":"RON","data":{"AuthCode":"brlN","RRN":"td0vr5r3N2ml"},
                 "instrument":{"country":0},"ntpID":"3019439","operationDate":"2026-09-15T12:22:29",
                 "options":{"bonus":0,"installments":0},"status":3}}
                """));
        assertThat(started.isPaid()).isTrue();
        assertThat(started.token()).isNull();
        assertThat(started.expireMonth()).isNull();
        assertThat(started.panMasked()).isNull();
    }

    /** Cheie greșită: Netopia răspunde fără `payment`. Nu e o plată refuzată, e o eroare a noastră. */
    @Test
    void anAnswerWithoutPaymentIsAnError() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://netopia.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NetopiaClient netopia = new NetopiaClient(builder.build(), "gresita", "POS", "https://api.test/ipn");
        server.expect(requestTo("https://netopia.test/payment/card/start"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON).body("{\"error\":{\"code\":\"401\",\"message\":\"Unauthorized\"}}"));

        assertThatThrownBy(() -> netopia.start("WH9-x", BigDecimal.ONE, "Proba", BILLING, "https://app.test", null))
                .isInstanceOf(NetopiaClient.NetopiaException.class)
                .hasMessageContaining("Unauthorized");
    }

    @Test
    void withoutTheNotifyUrlNothingStarts() {
        assertThat(new NetopiaClient(RestClient.create(), "k", "p", "").isConfigured()).isFalse();
        assertThat(new NetopiaClient(RestClient.create(), "k", "p", "https://api.test/ipn").isConfigured()).isTrue();
    }
}
