package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.WasteMovementRepository;

import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contractul de eroare al API-ului: o greşeală a clientului trebuie să arate ca o greşeală a
 * clientului.
 *
 * <p><b>De ce contează, dincolo de curăţenie.</b> {@code AdviceController} are un handler
 * atrapă-tot pe {@code Exception} care face trei lucruri deodată: întoarce <b>500</b>, scrie
 * {@code log.error("Unexpected error")} şi cheamă {@code Sentry.captureException}. Pentru o
 * excepţie chiar neaşteptată e exact ce trebuie. Dar Spring aruncă excepţii şi pentru lucruri care
 * n-au nimic neaşteptat în ele — un parametru obligatoriu lipsă, un verb greşit —, iar pentru
 * astea nu există handler dedicat, deci cad tot acolo.
 *
 * <p>Rezultatul e dublu, şi partea a doua e cea scumpă: clientul primeşte „încearcă din nou" acolo
 * unde ar trebui să i se spună „reformulează cererea", iar <b>fiecare astfel de greşeală devine o
 * alertă în Sentry</b>. Colectorul de erori tocmai a fost aprins pentru lansare; un scaner care
 * plimbă verbe peste API sau un ecran care uită un parametru îl umple cu zgomot, şi în zgomotul
 * ăla se pierde exact 500-ul adevărat pentru care a fost aprins.
 *
 * <p>Vestea bună, probată mai jos: corpul răspunsului <b>nu</b> scurge nimic — mesajul e generic,
 * fără urmă de stivă şi fără textul excepţiei.
 *
 * @see <a href="file:../../../../../../ecoregistru-docs/docs/QA-BUGS.md">BUG-001</a>
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class ApiErrorContractIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WasteMovementRepository movementRepository;

    private String token;
    private AppUser admin;

    @BeforeEach
    void setUp() {
        admin = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        token = jwtService.generateToken(admin);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ce merge deja bine
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Partea care e în regulă, şi merită pironită ca să nu se piardă: oricât de greşită ar fi
     * cererea, răspunsul nu spune de ce s-a supărat serverul. Fără nume de clasă, fără stivă, fără
     * mesajul excepţiei — pe o aplicaţie prin care trec serii de buletin şi CNP-uri, un mesaj de
     * eroare vorbăreţ e o scurgere ca oricare alta.
     */
    @Test
    void noErrorResponseEverLeaksAnExceptionOrAStackTrace() throws Exception {
        String[] badRequests = {
                "/api/v1/deadlines",
                "/api/v1/movements/nu-e-un-uuid",
                "/api/v1/deadlines?year=nu-e-un-an",
        };
        for (String url : badRequests) {
            mockMvc.perform(get(url).header("Authorization", "Bearer " + token))
                    .andExpect(content().string(not(containsString("Exception"))))
                    .andExpect(content().string(not(containsString("ro.ecoregistru"))))
                    .andExpect(content().string(not(containsString("java."))))
                    .andExpect(content().string(not(containsString("at org.springframework"))));
        }
    }

    /** Un corp JSON stricat e tratat cum trebuie — handlerul lui există. */
    @Test
    void aMalformedBodyIsABadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/work-points")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ asta nu e json"))
                .andExpect(status().isBadRequest());
    }

    /** Şi un câmp obligatoriu lipsă din corp: 422, prin handlerul de validare. */
    @Test
    void aMissingRequiredFieldIsUnprocessable() throws Exception {
        mockMvc.perform(post("/api/v1/work-points")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUG-001 — greşelile de client cad în handlerul atrapă-tot
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Un parametru obligatoriu lipsă. Aşteptat <b>400</b>; azi dă <b>500</b> plus o alertă Sentry.
     *
     * <p>Nu e ipotetic: {@code GET /api/v1/deadlines} e ecranul de termene, iar
     * {@code /api/v1/audit-file} e dosarul de control. Amândouă cer {@code year} fără implicit.
     */
    @Test
    void aMissingRequiredQueryParameterIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/deadlines").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/audit-file").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    /**
     * Un verb care nu există pe calea aia. Aşteptat <b>405</b>; azi dă <b>500</b> plus alertă.
     *
     * <p>Ăsta e cel care umple colectorul de erori fără să facă nimeni nimic rău: orice scaner
     * plimbă {@code GET} peste căi care primesc numai {@code PUT}, iar aplicaţia are patru resurse
     * care se citesc doar prin listă — deci patru căi pe care un client cinstit poate greşi.
     */
    @Test
    void anUnsupportedMethodIsMethodNotAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/partners/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isMethodNotAllowed());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUG-003 — constrângerile de pe obiectele imbricate nu se verifică
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Controlul care dă sens testului de dedesubt: pe drumul direct, constrângerea
     * <b>funcţionează</b>. {@code DriverRequest.identification} e {@code @Size(max = 100)}, iar un
     * şir mai lung primeşte 422 de la handlerul de validare, fără să atingă baza de date.
     */
    @Test
    void aTooLongFieldIsUnprocessableOnTheDirectRoute() throws Exception {
        mockMvc.perform(post("/api/v1/drivers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ion\",\"identification\":\"" + "9".repeat(200) + "\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    /**
     * Acelaşi câmp, acelaşi {@code DriverRequest}, aceeaşi constrângere — dar ajuns acolo prin
     * lista de şoferi a unui partener. Aşteptat <b>422</b>; azi dă <b>500</b> plus alertă Sentry.
     *
     * <p><b>Cauza:</b> {@code PartnerRequest} ţine {@code List<DriverRequest> drivers} şi
     * {@code List<PartnerWorkPointRequest> workPoints} fără {@code @Valid} pe câmp, iar fără el
     * validarea nu coboară în obiectele imbricate. Adnotările {@code @Size} de pe cele două
     * înregistrări sunt scrise şi <b>nu se aplică</b> pe drumul ăsta: valoarea trece de controller,
     * ajunge la {@code INSERT} şi se opreşte în lungimea coloanei — {@code varchar(100)}, respectiv
     * {@code varchar(500)}.
     *
     * <p><b>De ce contează mai mult decât un cod de răspuns greşit:</b> lista de şoferi a unui
     * partener e locul din aplicaţie prin care trece <b>CNP-ul</b> (vezi
     * {@code SensitiveDataExposureIT}) — deci exact acolo unde o cerere prost formată devine un
     * 500 raportat automat, în loc de un refuz curat.
     *
     * <p>Aceeaşi formă a defectului ca BUG-001 — o greşeală de client care iese ca defect de
     * server — dar altă cauză: acolo lipseşte un handler, aici lipseşte o adnotare.
     */
    @Test
    void aTooLongFieldIsUnprocessableOnTheNestedRouteToo() throws Exception {
        String created = mockMvc.perform(post("/api/v1/partners")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Transport SRL","client":true,"supplier":false,"carrier":true,"type":"COLLECTOR","authorizationNumber":"AM 1/2024"}"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String partnerId = created.replaceAll("^.*?\"id\"\\s*:\\s*\"([^\"]+)\".*$", "$1");

        mockMvc.perform(put("/api/v1/partners/" + partnerId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Transport SRL","client":true,"supplier":false,"carrier":true,"type":"COLLECTOR",
                                 "drivers":[{"name":"Ion","identification":"%s"}]}""".formatted("9".repeat(200))))
                .andExpect(status().isUnprocessableEntity());
    }

    /**
     * Un UUID stricat în cale. Aşteptat <b>400</b>.
     *
     * <p>Testul stă lângă celelalte două fiindcă e din aceeaşi familie — o greşeală de client
     * cărei nu-i corespunde niciun handler — dar el e şi cel mai uşor de atins din afară: e destul
     * să scrii o adresă de mână greşit.
     */
    @Test
    void aMalformedUuidInThePathIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/movements/nu-e-un-uuid")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    /**
     * BUG-012. O adresă care nu există, cu sesiune. Aşteptat <b>404</b>.
     *
     * <p>Aceeaşi familie ca cele trei de mai sus, găsită la BUG-011: Spring aruncă
     * {@code NoResourceFoundException}, iar fără handler ea cădea în plasa de la urmă — 500 şi
     * alertă în Sentry pentru un link vechi sau o adresă tastată greşit.
     */
    @Test
    void anUnknownPathIsANotFound() throws Exception {
        mockMvc.perform(get("/api/v1/nu-exista")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("NoResourceFound"))));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // P2.14, restul — valori pe care calendarul sau cântarul nu le pot ţine
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * O lună sau un an care nu există în calendar. Aşteptat <b>400</b>.
     *
     * <p>{@code month} şi {@code year} sunt {@code int}, deci trec de conversia lui Spring —
     * {@code 13} e un număr perfect bun. Se strică abia în serviciu, la {@code YearMonth.of} /
     * {@code LocalDate.of}, care aruncă {@code DateTimeException}; pentru ea nu există handler.
     * Panoul cere {@code /movements/summary} la fiecare deschidere, deci un parametru de adresă
     * greşit e o singură tastă distanţă.
     *
     * <p>13.09.2026: <b>500</b> pe toate trei, fiecare cu {@code DateTimeException} → BUG-009.
     */
    @Test
    void aMonthOrAYearTheCalendarCannotHoldIsABadRequest() throws Exception {
        String[] urls = {
                "/api/v1/movements/summary?year=2026&month=13",
                "/api/v1/movements?year=2026&month=0",
                "/api/v1/deadlines?year=1000000000",
        };
        for (String url : urls) {
            mockMvc.perform(get(url).header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }
    }

    /**
     * Cantitate negativă sau zero, pe cele trei drumuri pe care intră o cifră. Aşteptat <b>422</b>
     * şi nimic scris. Pironeşte adnotările ({@code @DecimalMin} exclusiv, {@code @Positive},
     * {@code @PositiveOrZero}) — o cantitate negativă ar scădea din stocul tipărit pe fişă.
     */
    @Test
    void aQuantityThatIsNotPositiveIsRefusedAndNothingIsWritten() throws Exception {
        mockMvc.perform(postMovement("5", "2026-07-05", null)).andExpect(status().isOk()); // controlul
        long before = movementCount();
        for (String quantity : new String[]{"-5", "0"}) {
            mockMvc.perform(postMovement(quantity, "2026-07-05", null))
                    .andExpect(status().isUnprocessableEntity());
        }
        mockMvc.perform(post("/api/v1/movements/" + UUID.randomUUID() + "/weight")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":-1}"))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(put("/api/v1/packaging/market")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"material\":\"PET\",\"year\":2026,\"salesPackaging\":-1}"))
                .andExpect(status().isUnprocessableEntity());
        org.junit.jupiter.api.Assertions.assertEquals(before, movementCount());
    }

    /**
     * Descărcare înainte de încărcare. Aşteptat: refuz (<b>4xx</b>) şi nimic scris.
     *
     * <p>{@code unloadDate} ajunge tipărită pe Anexa 3 la HG 1061/2008 şi e data după care se
     * ordonează registrul de predări. Un transport descărcat pe 1 iulie şi încărcat pe 5 iulie nu
     * există; formularul care-l poartă e unul pe care nu-l poate apăra nimeni la un control.
     *
     * <p>13.09.2026: <b>200</b>, rândul se scrie — nicio comparaţie între cele două date, nici în
     * serviciu, nici pe ecran → BUG-010.
     */
    @Test
    void anUnloadingBeforeTheLoadingIsRefused() throws Exception {
        // Controlul: acelaşi corp, descărcat a doua zi, trece. Fără el, un refuz venit din alt
        // motiv (codul de operaţie, registrul) ar face testul verde — primul draft chiar aşa era.
        mockMvc.perform(postMovement("5", "2026-07-05", "2026-07-06"))
                .andExpect(status().isOk());
        long before = movementCount();
        mockMvc.perform(postMovement("5", "2026-07-05", "2026-07-01"))
                .andExpect(status().is4xxClientError());
        org.junit.jupiter.api.Assertions.assertEquals(before, movementCount());
    }

    private long movementCount() {
        return movementRepository.findAllByCompany_IdAndDeletedFalse(admin.getCompany().getId()).size();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder postMovement(
            String quantity, String date, String unloadDate) {
        WasteMovement seeded = movementRepository
                .findAllByCompany_IdAndDeletedFalse(admin.getCompany().getId()).get(0);
        String body = """
                {"workPointId":"%s","date":"%s","wasteCodeId":"%s","quantity":%s,"unit":"KG",
                 "operation":"RECOVERED","operationCode":"R3","register":"ANEXA_1","unloadDate":%s}"""
                .formatted(seeded.getWorkPoint().getId(), date, seeded.getWasteCode().getId(), quantity,
                        unloadDate == null ? "null" : "\"" + unloadDate + "\"");
        return post("/api/v1/movements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
