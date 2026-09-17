package ro.ecoregistru.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.ServiceUnavailableException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * The shape of the ANAF v9 exchange, against its documentation (doc_WS_V9.txt, read 15.09.2026) and
 * one live answer the same day. The live answer differs from the documentation in one way these
 * fixtures follow: it carries only {@code found} and {@code notFound}, no {@code cod} or
 * {@code message} — so nothing here may depend on them. It also showed the trade register number in
 * the new ONRC form ({@code J2025…}), which is passed through untouched. The company below is invented.
 */
class AnafClientTest {

    private static final String URL = "https://anaf.test/api/PlatitorTvaRest/v9/tva";
    private static final Clock SEPT_15 = Clock.fixed(Instant.parse("2026-09-15T08:00:00Z"), ZoneId.of("Europe/Bucharest"));

    private static final String FOUND = """
            {"found":[{
               "date_generale":{"cui":12345678,"data":"2026-09-15","denumire":"EXEMPLU COLECT SRL",
                 "adresa":"JUD. CLUJ, MUN. CLUJ-NAPOCA, STR. EXEMPLULUI, NR.1","nrRegCom":"J12/1351/2011",
                 "telefon":"","cod_CAEN":"3832","stare_inregistrare":"INREGISTRAT din data 01.03.2011"},
               "stare_inactiv":{"dataInactivare":"","dataReactivare":"","dataPublicare":"","dataRadiere":"","statusInactivi":%s},
               "adresa_sediu_social":{"sdenumire_Strada":"Str. Exemplului","snumar_Strada":"1",
                 "sdenumire_Localitate":"Mun. Cluj-Napoca","sdenumire_Judet":"CLUJ"}
             }],
             "notFound":[]}
            """;

    @Test
    void aFoundCompanyIsReadFromTheV9Answer() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://anaf.test/api");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AnafClient anaf = new AnafClient(builder.build(), 0, SEPT_15);

        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].cui").value(12345678))
                .andExpect(jsonPath("$[0].data").value("2026-09-15"))
                .andRespond(withSuccess(FOUND.formatted("false"), MediaType.APPLICATION_JSON));

        AnafClient.Company company = anaf.lookup("RO12345678").orElseThrow();

        server.verify();
        assertThat(company.cui()).isEqualTo("12345678");
        assertThat(company.name()).isEqualTo("EXEMPLU COLECT SRL");
        assertThat(company.address()).isEqualTo("JUD. CLUJ, MUN. CLUJ-NAPOCA, STR. EXEMPLULUI, NR.1");
        assertThat(company.tradeRegisterNumber()).isEqualTo("J12/1351/2011");
        assertThat(company.caenCode()).isEqualTo("3832");
        assertThat(company.county()).isEqualTo("CLUJ");
        assertThat(company.city()).isEqualTo("Mun. Cluj-Napoca");
        assertThat(company.inactive()).isFalse();
    }

    @Test
    void anInactiveTaxpayerSaysSo() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://anaf.test/api");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AnafClient anaf = new AnafClient(builder.build(), 0, SEPT_15);
        server.expect(requestTo(URL)).andRespond(withSuccess(FOUND.formatted("true"), MediaType.APPLICATION_JSON));

        assertThat(anaf.lookup("12345678").orElseThrow().inactive()).isTrue();
    }

    @Test
    void spacesAndALowercaseRoPrefixAreStrippedBeforeAsking() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://anaf.test/api");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AnafClient anaf = new AnafClient(builder.build(), 0, SEPT_15);
        server.expect(requestTo(URL))
                .andExpect(jsonPath("$[0].cui").value(12345678))
                .andRespond(withSuccess(FOUND.formatted("false"), MediaType.APPLICATION_JSON));

        assertThat(anaf.lookup(" ro 1234 5678 ")).isPresent();
        server.verify();
    }

    @Test
    void aCuiAnafDoesNotKnowIsEmptyAndIsAskedAgainNextTime() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://anaf.test/api");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AnafClient anaf = new AnafClient(builder.build(), 0, SEPT_15);
        String notFound = """
                {"found":[],"notFound":[99999999]}
                """;
        server.expect(ExpectedCount.twice(), requestTo(URL))
                .andRespond(withSuccess(notFound, MediaType.APPLICATION_JSON));

        assertThat(anaf.lookup("99999999")).isEmpty();
        assertThat(anaf.lookup("99999999")).isEmpty();
        server.verify();
    }

    @Test
    void aCompanyFoundIsServedFromMemoryTheSecondTime() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://anaf.test/api");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AnafClient anaf = new AnafClient(builder.build(), 0, SEPT_15);
        server.expect(ExpectedCount.once(), requestTo(URL))
                .andRespond(withSuccess(FOUND.formatted("false"), MediaType.APPLICATION_JSON));

        Optional<AnafClient.Company> first = anaf.lookup("12345678");
        Optional<AnafClient.Company> second = anaf.lookup("RO12345678");

        server.verify();
        assertThat(second).isEqualTo(first);
    }

    @Test
    void textThatCannotBeACuiIsRefusedWithoutAskingAnaf() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://anaf.test/api");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AnafClient anaf = new AnafClient(builder.build(), 0, SEPT_15);

        assertThatThrownBy(() -> anaf.lookup("RO12AB")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> anaf.lookup("1")).isInstanceOf(BusinessException.class);
        // 13 digits is a CNP, not a company's CUI.
        assertThatThrownBy(() -> anaf.lookup("1960101123456")).isInstanceOf(BusinessException.class);
        server.verify();
    }

    /**
     * A request that would wait behind a slow ANAF call gives up as unavailable instead of holding its
     * thread: with a synchronized fetch, enough lookups stall every request thread of the API.
     */
    @Test
    void aLookupDoesNotQueueBehindASlowOneForLong() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://anaf.test/api");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        // The minimum interval stands in for a slow answer: the second call sleeps holding the lock.
        AnafClient anaf = new AnafClient(builder.build(), 3_000, SEPT_15, Duration.ofMillis(200));
        server.expect(ExpectedCount.manyTimes(), requestTo(URL))
                .andRespond(withSuccess("{\"found\":[],\"notFound\":[1]}", MediaType.APPLICATION_JSON));

        assertThat(anaf.lookup("11111111")).isEmpty();
        Thread slow = new Thread(() -> anaf.lookup("22222222"));
        slow.start();
        Thread.sleep(300);

        long started = System.nanoTime();
        assertThatThrownBy(() -> anaf.lookup("33333333")).isInstanceOf(ServiceUnavailableException.class);
        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofMillis(1_500));
        slow.join();
    }

    @Test
    void anAnafFailureIsUnavailableNotAnError() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://anaf.test/api");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AnafClient anaf = new AnafClient(builder.build(), 0, SEPT_15);
        server.expect(requestTo(URL)).andRespond(withServerError());
        // ANAF's firewall answers an HTML page, not JSON.
        server.expect(requestTo(URL)).andRespond(
                withSuccess("<html><title>Request Rejected</title></html>", MediaType.TEXT_HTML));

        assertThatThrownBy(() -> anaf.lookup("12345678")).isInstanceOf(ServiceUnavailableException.class);
        assertThatThrownBy(() -> anaf.lookup("12345678")).isInstanceOf(ServiceUnavailableException.class);
        server.verify();
    }

    /** Răspunsul real pentru un CUI inexistent (17.09.2026): HTTP 404, cu listele obișnuite. E „nu există”, nu „ANAF căzut”. */
    @Test
    void anUnknownCuiAnsweredWith404IsNotFoundNotUnavailable() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://anaf.test/api");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AnafClient anaf = new AnafClient(builder.build(), 0, SEPT_15);
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON).body("{\"found\":[],\"notFound\":[998877660]}"));

        assertThat(anaf.lookup("998877660")).isEmpty();
        server.verify();
    }

    /** Un 404 fără listele ANAF (adresa serviciului mutată) nu se citește ca „firmă inexistentă”. */
    @Test
    void a404WithoutTheListsIsStillUnavailable() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://anaf.test/api");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AnafClient anaf = new AnafClient(builder.build(), 0, SEPT_15);
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.NOT_FOUND)
                .contentType(MediaType.TEXT_HTML).body("<html>Not Found</html>"));

        assertThatThrownBy(() -> anaf.lookup("998877660")).isInstanceOf(ServiceUnavailableException.class);
    }
}
