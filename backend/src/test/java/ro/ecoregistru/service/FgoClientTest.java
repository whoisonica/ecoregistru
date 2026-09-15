package ro.ecoregistru.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * F2 — the shape of what goes to FGO, against the v7 documentation. Whether FGO accepts it is
 * proved only on api-testuat, with a real key.
 */
class FgoClientTest {

    private static final FgoClient.Buyer BUYER = new FgoClient.Buyer(
            "Firma Test SRL", "12345678", "facturi@firma.ro", "Cluj", "Cluj-Napoca", "Str. Memorandumului nr. 1");

    /** Values computed independently with `shasum -a 1`. */
    @Test
    void theHashIsTheUppercaseSha1OfCuiKeyAndValue() {
        assertThat(FgoClient.hash("12345678", "cheiasecreta", "Firma Test SRL"))
                .isEqualTo("C0A1FA01C2EEB056327E52A5B911156539C4D3F3");
        assertThat(FgoClient.hash("12345678", "cheiasecreta", "42"))
                .isEqualTo("1FA9A297E846F33307DBB387E37095855D86E0F1");
    }

    @Test
    void anInvoiceGoesAsJsonWithTheExternalIdAndWithoutTheFreeLine() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://fgo.test/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FgoClient fgo = client(builder);

        server.expect(requestTo("https://fgo.test/v1/factura/emitere"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.CodUnic").value("12345678"))
                .andExpect(jsonPath("$.Hash").value("C0A1FA01C2EEB056327E52A5B911156539C4D3F3"))
                .andExpect(jsonPath("$.Serie").value("WH"))
                .andExpect(jsonPath("$.Valuta").value("RON"))
                .andExpect(jsonPath("$.IdExtern").value("inv-1"))
                .andExpect(jsonPath("$.VerificareDuplicat").value(true))
                .andExpect(jsonPath("$.DataEmitere").value("2026-10-17"))
                .andExpect(jsonPath("$.DataScadenta").value("2026-10-27"))
                .andExpect(jsonPath("$.Client.Tip").value("PJ"))
                .andExpect(jsonPath("$.Client.Judet").value("Cluj"))
                .andExpect(jsonPath("$.Continut.length()").value(2))
                .andExpect(jsonPath("$.Continut[1].Denumire").value("Implementare"))
                .andExpect(jsonPath("$.Continut[1].PretUnitar").value(290))
                .andRespond(withSuccess("""
                        {"Success":true,"Message":"","Factura":{"Numar":"7","Serie":"WH","Link":"https://fgo/7.pdf","LinkPlata":""}}
                        """, MediaType.APPLICATION_JSON));

        FgoClient.Issued issued = fgo.emit("inv-1", BUYER, List.of(
                        line("Generator", 99),
                        line("Implementare (client fondator, gratuită)", 0),
                        line("Implementare", 290)),
                LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 27), "Abonament");

        server.verify();
        assertThat(issued).isEqualTo(new FgoClient.Issued("WH", "7", "https://fgo/7.pdf", null));
    }

    /** FGO refuză cu un corp JSON, uneori pe 400: mesajul lui ajunge în `lastError`, nu se pierde. */
    @Test
    void aRefusalCarriesFgosMessage() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://fgo.test/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://fgo.test/v1/factura/emitere"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"Success\":false,\"Message\":\"Serie inexistenta\"}"));

        assertThatThrownBy(() -> client(builder).emit("inv-2", BUYER, List.of(line("Generator", 99)),
                LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 27), null))
                .isInstanceOf(FgoClient.FgoException.class)
                .hasMessageContaining("Serie inexistenta");
    }

    /**
     * Răspunsul real al api-testuat la un IdExtern repetat (15.09.2026): refuz, dar cu factura deja
     * emisă. E factura noastră, deci rândul trece ISSUED în loc să rămână DRAFT la nesfârșit.
     */
    @Test
    void aRepeatedExternalIdAnswersWithTheInvoiceAlreadyIssued() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://fgo.test/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://fgo.test/v1/factura/emitere"))
                .andRespond(withSuccess("""
                        {"Success":false,"Message":"Factura WH 1 exista deja salvata.","Factura":{"Numar":"1","Serie":"WH","Link":"https://fgo/1.pdf","LinkPlata":null}}
                        """, MediaType.APPLICATION_JSON));

        FgoClient.Issued issued = client(builder).emit("inv-1", BUYER, List.of(line("Generator", 99)),
                LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 27), null);

        assertThat(issued).isEqualTo(new FgoClient.Issued("WH", "1", "https://fgo/1.pdf", null));
    }

    @Test
    void theStatusHashesOnTheInvoiceNumberAndComparesPaidWithValue() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://fgo.test/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://fgo.test/v1/factura/getstatus"))
                .andExpect(jsonPath("$.Hash").value("1FA9A297E846F33307DBB387E37095855D86E0F1"))
                .andExpect(jsonPath("$.Numar").value("42"))
                .andRespond(withSuccess("""
                        {"Success":true,"Factura":{"Numar":"42","Serie":"WH","Valoare":"389.00","ValoareAchitata":"389.00"}}
                        """, MediaType.APPLICATION_JSON));

        FgoClient.Status status = client(builder).status("WH", "42");

        assertThat(status.isPaid()).isTrue();
        assertThat(new FgoClient.Status(new BigDecimal("389"), new BigDecimal("200")).isPaid()).isFalse();
    }

    @Test
    void withoutKeyOrSeriesItIsNotConfigured() {
        RestClient http = RestClient.create();
        assertThat(new FgoClient(http, "12345678", "", "WH", "Factura", BigDecimal.ZERO, "x", 0).isConfigured()).isFalse();
        assertThat(new FgoClient(http, "12345678", "k", "", "Factura", BigDecimal.ZERO, "x", 0).isConfigured()).isFalse();
        assertThat(new FgoClient(http, "12345678", "k", "WH", "Factura", BigDecimal.ZERO, "x", 0).isConfigured()).isTrue();
    }

    /** F3 — încasarea cu cardul: hash-ul pe număr, tipul „Banca", suma cu două zecimale, data cu ora. */
    @Test
    void aCardPaymentIsRecordedAsACollectionOnTheInvoice() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://fgo.test/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FgoClient fgo = client(builder);

        server.expect(requestTo("https://fgo.test/v1/factura/incasare"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.CodUnic").value("12345678"))
                .andExpect(jsonPath("$.Hash").value("1FA9A297E846F33307DBB387E37095855D86E0F1"))
                .andExpect(jsonPath("$.SerieFactura").value("WH"))
                .andExpect(jsonPath("$.NumarFactura").value("42"))
                .andExpect(jsonPath("$.TipIncasare").value("Banca"))
                .andExpect(jsonPath("$.SumaIncasata").value("389.00"))
                .andExpect(jsonPath("$.DataIncasare").value("2026-10-17 14:05:09"))
                .andExpect(jsonPath("$.PlatformaUrl").value("https://app.wastehouse.ro"))
                .andRespond(withSuccess("{\"Success\":true,\"Message\":\"\"}", MediaType.APPLICATION_JSON));

        fgo.collect("WH", "42", new BigDecimal("389"), java.time.LocalDateTime.of(2026, 10, 17, 14, 5, 9));

        server.verify();
    }

    private static FgoClient client(RestClient.Builder builder) {
        return new FgoClient(builder.build(), "12345678", "cheiasecreta", "WH", "Factura",
                BigDecimal.ZERO, "https://app.wastehouse.ro", 0);
    }

    private static BillingCalculator.Line line(String label, int price) {
        BigDecimal p = BigDecimal.valueOf(price);
        return new BillingCalculator.Line(label, 1, p, p);
    }
}
