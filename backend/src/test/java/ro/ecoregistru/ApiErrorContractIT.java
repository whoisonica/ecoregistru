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
import ro.ecoregistru.repository.AppUserRepository;

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

    private String token;

    @BeforeEach
    void setUp() {
        AppUser admin = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
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
                                {"name":"Transport SRL","client":true,"supplier":false,"carrier":true,"type":"COLLECTOR"}"""))
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
}
