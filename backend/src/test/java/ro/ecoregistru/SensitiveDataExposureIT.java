package ro.ecoregistru;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.AuditLog;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.AuditLogRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WorkPointRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unde ajunge CNP-ul şoferului — P0.6 din planul de audit.
 *
 * <p><b>De unde vine întrebarea.</b> Anexa 3 la HG 1061/2008 are o singură rubrică pentru şofer,
 * „serie CI sau CNP", iar practica scrie când una, când alta. Aplicaţia o ţine aşa cum e cerută:
 * un câmp liber, {@code Driver.identification} şi geamănul lui de pe mişcare,
 * {@code WasteMovement.driverIdentification}. Amândouă pot conţine un <b>cod numeric personal</b>,
 * adică un identificator naţional — categoria pe care art. 87 GDPR şi ANSPDCP o tratează separat.
 *
 * <p><b>Ce se probează, deci.</b> Nu că valoarea există — trebuie să existe, se tipăreşte pe
 * formular. Ci că nu se <b>multiplică</b>: fiecare copie în plus e un loc în plus din care trebuie
 * ştearsă la o cerere de ştergere, şi un loc în plus care poate scăpa. Locurile cercetate sunt cele
 * din planul de audit: jurnalul de audit, mesajele de eroare, răspunsurile de validare, logurile şi
 * ce pleacă spre colectorul de erori.
 *
 * <p><b>Jurnalul de audit e suspectul principal</b>, şi din construcţie: interceptorul de flush
 * scrie perechile {@code câmp: vechi → nou} pentru <em>orice</em> câmp care nu e pe lista neagră,
 * iar lista aceea are parole şi zgomot de infrastructură, nu date personale.
 *
 * <p>⚠️ Valorile folosite mai jos sunt CNP-uri <b>inventate</b>, cu forma corectă şi cifra de
 * control greşită. Testul are nevoie de un şir care arată ca un CNP, nu de al cuiva.
 */
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class SensitiveDataExposureIT {

    /** CNP inventat, forma corectă: sex/secol 1, data 92-03-15, judeţ 12, ordinal, control. */
    private static final String CNP = "1920315123451";
    private static final String CNP_CORECTAT = "1920315123452";

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired ObjectMapper objectMapper;
    @Autowired Environment environment;
    @Autowired AppUserRepository appUserRepository;
    @Autowired AuditLogRepository auditLogRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;

    private String adminToken;
    private UUID workPointId;
    private UUID wasteCodeId;

    @BeforeEach
    void setUp() {
        AppUser admin = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        adminToken = jwtService.generateToken(admin);
        workPointId = workPointRepository.findAllByCompany_Id(admin.getCompany().getId()).get(0).getId();
        wasteCodeId = wasteCodeRepository.findAll().get(0).getId();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Jurnalul de audit
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Şoferul: se scrie cu un CNP, apoi cineva îl corectează. Aşteptat: jurnalul reţine
     * <b>fapta</b> — cine a atins câmpul şi când — fără să reţină valorile.
     *
     * <p>De ce e valoarea de prisos aici: rândul curent al şoferului e în {@code drivers} şi se
     * poate citi oricând; ce adaugă jurnalul e istoricul, iar pentru istoric „identification s-a
     * schimbat" răspunde la întreaga întrebare pe care şi-o pune cineva la un control. În schimb
     * valoarea <em>veche</em> nu mai există nicăieri altundeva — e exact ce s-a cerut să fie
     * corectat — şi din jurnal nu se poate şterge: tabela n-are drum de modificare sau ştergere,
     * prin construcţie.
     *
     * <p>🔴 <b>Cade — BUG-002.</b>
     */
    @Test
    void theDriversIdentificationNeverReachesTheAuditLog() throws Exception {
        UUID driverId = createDriverWithCnp();

        mockMvc.perform(put("/api/v1/drivers/" + driverId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ion Popescu","identification":"%s"}""".formatted(CNP_CORECTAT)))
                .andExpect(status().isOk());

        assertThat(changesFor("Driver", driverId))
                .as("jurnalul nu are voie să poarte nici CNP-ul vechi, nici pe cel nou")
                .doesNotContain(CNP)
                .doesNotContain(CNP_CORECTAT);
    }

    /**
     * Acelaşi câmp, celălalt loc: pe mişcare, unde se completează pentru un transport anume.
     * E drumul mai des umblat dintre cele două — şoferii din nomenclator se editează rar, iar
     * rubrica de pe formular se completează la fiecare transport.
     *
     * <p>🔴 <b>Cade — BUG-002.</b>
     */
    @Test
    void theMovementsDriverIdentificationNeverReachesTheAuditLog() throws Exception {
        UUID movementId = createMovement(null);

        mockMvc.perform(put("/api/v1/movements/" + movementId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movementJson(CNP)))
                .andExpect(status().isOk());

        assertThat(changesFor("WasteMovement", movementId))
                .as("rubrica de şofer de pe mişcare, la fel")
                .doesNotContain(CNP);
    }

    /**
     * Al doilea drum al aceleiaşi scurgeri, şi cel care o scoate din baza de date: ecranul de
     * jurnal. Valoarea nu stă doar într-o coloană, ci se întoarce în răspunsul API către oricine
     * are {@code ADMIN} în firmă.
     *
     * <p>🔴 <b>Cade — BUG-002.</b>
     */
    @Test
    void theAuditLogApiDoesNotHandBackTheCnp() throws Exception {
        UUID driverId = createDriverWithCnp();
        mockMvc.perform(put("/api/v1/drivers/" + driverId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ion Popescu","identification":"%s"}""".formatted(CNP_CORECTAT)))
                .andExpect(status().isOk());

        String body = mockMvc.perform(get("/api/v1/audit-log")
                        .param("entityType", "Driver")
                        .param("entityId", driverId.toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain(CNP).doesNotContain(CNP_CORECTAT);
    }

    /**
     * Ce transformă o copie în plus într-o problemă de altă mărime: câmpul {@code changes} intră
     * în căutarea liberă a jurnalului, deliberat („cine a atins cantitatea" e o întrebare bună).
     * Consecinţa nevrută e că jurnalul devine <b>căutabil după CNP</b> — adică un index de persoane
     * peste un produs care n-a vrut niciodată să ţină aşa ceva.
     *
     * <p>🔴 <b>Cade — BUG-002.</b>
     */
    @Test
    void theAuditLogCannotBeSearchedByCnp() throws Exception {
        UUID driverId = createDriverWithCnp();
        mockMvc.perform(put("/api/v1/drivers/" + driverId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ion Popescu","identification":"%s"}""".formatted(CNP_CORECTAT)))
                .andExpect(status().isOk());

        String body = mockMvc.perform(get("/api/v1/audit-log")
                        .param("search", CNP)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(body).path("totalElements").asInt())
                .as("un CNP nu are voie să fie un termen de căutare care găseşte ceva")
                .isZero();
    }

    /**
     * Cealaltă jumătate a reparaţiei, şi motivul pentru care testele de mai sus cer redactarea
     * valorilor, nu oprirea auditului: <b>fapta trebuie să rămână scrisă</b>. „Cine a schimbat
     * datele şoferului şi când" e exact întrebarea pentru care jurnalul există, iar o reparaţie
     * care scoate şoferul de pe lista auditată ar rezolva scurgerea stricând produsul.
     *
     * <p>Trece azi, şi trebuie să treacă şi după reparaţie.
     */
    @Test
    void theFactThatTheIdentificationChangedIsStillRecorded() throws Exception {
        UUID driverId = createDriverWithCnp();

        mockMvc.perform(put("/api/v1/drivers/" + driverId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ion Popescu","identification":"%s"}""".formatted(CNP_CORECTAT)))
                .andExpect(status().isOk());

        List<AuditLog> rows = rowsFor("Driver", driverId);
        assertThat(rows).as("creare + modificare").hasSizeGreaterThanOrEqualTo(2);
        assertThat(changesFor("Driver", driverId))
                .as("numele câmpului rămâne — el spune ce s-a atins")
                .contains("identification");
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.getActorEmail()).isEqualTo("admin@demo.ro");
            assertThat(row.getOccurredAt()).isNotNull();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mesaje de eroare, validare, loguri
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Răspunsul de validare spune <b>care câmp</b> şi <b>ce regulă</b>, niciodată ce s-a trimis.
     * Diferenţa contează fiindcă răspunsurile de eroare ajung, în practică, în cu totul alte
     * locuri decât datele: capturi de ecran trimise pe WhatsApp la suport, console de browser,
     * bilete de asistenţă.
     */
    @Test
    void aRejectedValueIsNotEchoedBackInTheValidationError() throws Exception {
        String tooLong = CNP.repeat(10);

        String body = mockMvc.perform(post("/api/v1/drivers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ion Popescu","identification":"%s"}""".formatted(tooLong)))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain(CNP);
        assertThat(body).as("câmpul, da; valoarea, nu").contains("identification");
    }

    /**
     * Acelaşi lucru pe drumul celălalt, când corpul cererii nici măcar nu e JSON valid: mesajul lui
     * Jackson poate purta o bucată din sursă, iar handlerul îl <b>loghează</b> şi întoarce un
     * mesaj propriu. Proba se uită la răspuns, singurul lucru care pleacă din aplicaţie.
     */
    @Test
    void aMalformedBodyIsNotEchoedBackEither() throws Exception {
        String body = mockMvc.perform(post("/api/v1/drivers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ion\",\"identification\":\"" + CNP + "\",,}"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain(CNP);
    }

    /**
     * Logurile, întrebate direct: se face tot drumul — scriere, modificare, corp stricat, apoi
     * citirea jurnalului — cu ieşirea aplicaţiei prinsă, şi se caută valoarea în ea.
     *
     * <p>Contează mai mult decât pare pe o aplicaţie pe Heroku: logurile pleacă la un colector
     * extern, se păstrează după propriul lor termen şi se citesc de cine are acces la dyno — adică
     * un cu totul alt set de oameni decât cei care au voie să vadă datele unui client. Un CNP scris
     * acolo iese din toate barierele pe care le probează restul auditului, dintr-o singură linie de
     * depanare adăugată într-o zi grea.
     */
    @Test
    void theLogsNeverCarryTheValue(CapturedOutput output) throws Exception {
        UUID driverId = createDriverWithCnp();
        mockMvc.perform(put("/api/v1/drivers/" + driverId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Ion Popescu","identification":"%s"}""".formatted(CNP_CORECTAT)));
        mockMvc.perform(post("/api/v1/drivers")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ion\",\"identification\":\"" + CNP + "\",,}"));
        mockMvc.perform(get("/api/v1/audit-log")
                .header("Authorization", "Bearer " + adminToken));

        assertThat(output.getAll())
                .as("nici în mesajele de eroare logate, nici în depanare")
                .doesNotContain(CNP)
                .doesNotContain(CNP_CORECTAT);
    }

    /**
     * Ce pleacă spre colectorul de erori nu se poate proba dintr-un test de integrare — SDK-ul nici
     * nu porneşte fără DSN, ceea ce e corect. Ce se poate proba, şi e de fapt lucrul care ţine, e
     * <b>configuraţia</b>: fără {@code send-default-pii}, Sentry nu ataşează corpul cererii şi
     * antetele la raport. Un formular de mişcare într-un raport de eroare ar scoate din firmă exact
     * ce apără restul aplicaţiei — inclusiv rubrica de şofer.
     *
     * <p>Pironit aici fiindcă e o linie de configuraţie pe care nimic altceva n-o apără, iar
     * „aprindem PII-ul ca să depanăm mai uşor" e o propunere care sună rezonabilă într-o zi grea.
     */
    @Test
    void sentryIsConfiguredNotToCarryRequestBodies() {
        assertThat(environment.getProperty("sentry.send-default-pii", Boolean.class, true))
                .as("send-default-pii trebuie să rămână false")
                .isFalse();
        assertThat(environment.getProperty("sentry.traces-sample-rate", Double.class, 1.0))
                .as("fără urme de performanţă, care poartă şi ele parametri")
                .isZero();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ajutoare
    // ─────────────────────────────────────────────────────────────────────────

    private UUID createDriverWithCnp() throws Exception {
        String body = mockMvc.perform(post("/api/v1/drivers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ion Popescu","identification":"%s","vehicleRegistration":"CJ01ABC"}"""
                                .formatted(CNP)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).path("id").asText());
    }

    private UUID createMovement(String driverIdentification) throws Exception {
        String body = mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movementJson(driverIdentification)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).path("id").asText());
    }

    private String movementJson(String driverIdentification) {
        return """
                {"workPointId":"%s","date":"%s","wasteCodeId":"%s","quantity":1.000,
                 "unit":"KG","operation":"GENERATED","physicalState":"SOLID"%s}
                """.formatted(workPointId, LocalDate.now(), wasteCodeId,
                driverIdentification == null ? ""
                        : ",\"driverName\":\"Ion Popescu\",\"driverIdentification\":\"" + driverIdentification + "\"");
    }

    /** Rândurile de jurnal ale unui singur rând de date — clasa nu rulează în tranzacţii anulate. */
    private List<AuditLog> rowsFor(String entityType, UUID entityId) {
        return auditLogRepository.findAll().stream()
                .filter(row -> entityType.equals(row.getEntityType()) && entityId.equals(row.getEntityId()))
                .toList();
    }

    /** Tot ce s-a scris în {@code changes} pentru rândul ăla, pus cap la cap. */
    private String changesFor(String entityType, UUID entityId) {
        return rowsFor(entityType, entityId).stream()
                .map(AuditLog::getChanges)
                .filter(java.util.Objects::nonNull)
                .reduce("", String::concat);
    }
}
