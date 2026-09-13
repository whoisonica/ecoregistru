package ro.ecoregistru;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * P1.9 — constrângeri de bază de date: nu FK-urile în sine (garantate structural de JPA şi de
 * schemă, şi neatinse de niciun drum din serviciu — nicio ştergere dură nu există pentru
 * work-point/partener/cod, doar soft delete), ci cele două unicităţi care se verifică <b>în
 * aplicaţie, nu în tranzacţie</b>: {@code companies.cui} şi {@code app_users.email}.
 *
 * <p>Ambele servicii fac exact acelaşi lucru: {@code existsBy...()}, apoi {@code save()}, fără
 * niciun lacăt între cele două — un TOCTOU clasic. Sub o cerere pe rând, verificarea şi scrierea
 * par atomice. Sub două cereri care ajung în aceeaşi fereastră, nu sunt: amândouă trec de
 * {@code existsBy...()} fiindcă niciuna n-a comis încă, şi abia atunci constrângerea {@code UNIQUE}
 * a bazei decide — corect, dar cu o excepţie de tip greşit pentru cine o prinde.
 *
 * <p><b>Determinist, nu de noroc — acelaşi principiu ca la BUG-007, altă unealtă.</b> Nu e nevoie
 * de un lacăt explicit: Postgres însuşi blochează al doilea {@code INSERT} pe o valoare unică cât
 * timp primul e încă necomis, şi îl deblochează abia la commit sau rollback. Un fir de test ţine
 * primul {@code INSERT} deschis (flush, fără commit); cererea reală — prin stiva HTTP, ca să se
 * vadă exact ce primeşte clientul — ajunge garantat în fereastră, fiindcă nu se eliberează
 * lacătul-momeală decât după ce cererea a avut timp să treacă de verificare şi să se blocheze ea
 * însăşi pe INSERT.
 *
 * <p>🔴 <b>BUG-008, găsit.</b> Constrângerea DB ţine — nicio coliziune n-a produs două rânduri —
 * dar cererea care pierde ia <b>500</b>, nu 422/409: {@code DataIntegrityViolationException} n-are
 * handler în {@code AdviceController} (doar {@code OptimisticLockingFailureException}, de la
 * BUG-007), deci pică în cel atrapă-tot — acelaşi tipar ca BUG-001/003/007, a patra oară.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class DbConstraintsIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired ObjectMapper objectMapper;
    @Autowired AppUserRepository appUserRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired PlatformTransactionManager transactionManager;
    @PersistenceContext EntityManager entityManager;

    private String platformToken;
    private UUID demoCompanyId;

    @BeforeEach
    void setUp() {
        AppUser platform = appUserRepository.findByEmail("platform@ecoregistru.ro").orElseThrow();
        platformToken = jwtService.generateToken(platform);
        demoCompanyId = appUserRepository.findByEmail("admin@demo.ro").orElseThrow()
                .getCompany().getId();
    }

    /**
     * Invariantul care contează mai mult decât codul de răspuns: constrângerea unică n-a lăsat
     * să treacă două firme cu acelaşi CUI, indiferent cum a răspuns API-ul cererii care a pierdut.
     */
    @Test
    void aCuiRaceNeverProducesTwoCompanies() throws Exception {
        String cui = randomCui();
        raceCompanyCreation(cui);

        Long companiesWithThisCui = entityManager
                .createQuery("select count(c) from Company c where c.cui = :cui", Long.class)
                .setParameter("cui", cui).getSingleResult();
        assertThat(companiesWithThisCui).isEqualTo(1L);
    }

    /**
     * 🔴 BUG-008. Comportamentul corect: cererea care ajunge a doua pe acelaşi CUI, deşi a trecut
     * de {@code existsByCui}, trebuie să iasă tot cu {@code 422 company.cui.exists} — exact ce ar
     * fi primit dacă verificarea din service ar fi prins-o, nu un {@code 500} de la excepţia
     * netradusă a bazei.
     */
    @Test
    void theCuiRaceIsRejectedAsAClientErrorNotAServerError() throws Exception {
        MvcResult result = raceCompanyCreation(randomCui());
        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("error-code").asText()).isEqualTo("company.cui.exists");
    }

    /** Aceeaşi coliziune, pe cealaltă unicitate globală. */
    @Test
    void anEmailRaceNeverProducesTwoUsers() throws Exception {
        String email = randomEmail();
        raceInvite(email);

        Long usersWithThisEmail = entityManager
                .createQuery("select count(u) from AppUser u where u.email = :email", Long.class)
                .setParameter("email", email).getSingleResult();
        assertThat(usersWithThisEmail).isEqualTo(1L);
    }

    /** 🔴 Aceeaşi formă ca BUG-008, prin {@code AuthenticationService.inviteUser}. */
    @Test
    void theEmailRaceIsRejectedAsAClientErrorNotAServerError() throws Exception {
        MvcResult result = raceInvite(randomEmail());
        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("error-code").asText()).isEqualTo("account.already.exists");
    }

    // --- helpers ---

    /**
     * Ţine un {@code INSERT} necomis pe {@code companies.cui = cui} într-un fir separat, apoi
     * porneşte o cerere HTTP reală de creare cu acelaşi CUI şi o lasă să se blocheze pe INSERT-ul
     * ei — abia atunci eliberează momeala. Întoarce rezultatul cererii care a pierdut cursa.
     */
    private MvcResult raceCompanyCreation(String cui) throws Exception {
        return race(
                () -> companyRepository.save(Company.builder()
                        .name("Momeală SRL").cui(cui).type(CompanyType.GENERATOR)
                        .active(true).createdAt(Instant.now()).build()),
                companyRepository::flush,
                () -> mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(companyBody(cui))).andReturn());
    }

    /** Aceeaşi tehnică, pe {@code app_users.email}, prin invitaţia de platformă. */
    private MvcResult raceInvite(String email) throws Exception {
        return race(
                () -> appUserRepository.save(AppUser.builder()
                        .email(email).password("x").role(Role.OPERATOR)
                        .company(companyRepository.getReferenceById(demoCompanyId))
                        .enabled(false).createdAt(Instant.now()).build()),
                appUserRepository::flush,
                () -> mockMvc.perform(post("/api/v1/companies/" + demoCompanyId + "/users")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteBody(email))).andReturn());
    }

    private interface HttpCall {
        MvcResult call() throws Exception;
    }

    private MvcResult race(Runnable decoyInsert, Runnable decoyFlush, HttpCall racerCall)
            throws Exception {
        CountDownLatch decoyInserted = new CountDownLatch(1);
        CountDownLatch releaseDecoy = new CountDownLatch(1);
        AtomicReference<Throwable> decoyFailure = new AtomicReference<>();
        Thread decoy = new Thread(() -> {
            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            try {
                tt.executeWithoutResult(status -> {
                    decoyInsert.run();
                    decoyFlush.run(); // INSERT chiar acum, necomis — ţine indexul unic
                    decoyInserted.countDown();
                    await(releaseDecoy);
                });
            } catch (Throwable t) {
                decoyFailure.set(t);
            }
        });
        decoy.start();
        assertThat(decoyInserted.await(10, TimeUnit.SECONDS)).as("rândul-momeală e scris").isTrue();

        AtomicReference<MvcResult> raceResult = new AtomicReference<>();
        AtomicReference<Throwable> raceFailure = new AtomicReference<>();
        Thread racer = new Thread(() -> {
            try {
                raceResult.set(racerCall.call());
            } catch (Throwable t) {
                raceFailure.set(t);
            }
        });
        racer.start();
        // Timp să treacă de `existsBy...` (necomis încă la momeală, deci vede fals) şi să se
        // blocheze el însuşi pe INSERT-ul care ţine aceeaşi valoare unică.
        Thread.sleep(500);
        releaseDecoy.countDown();

        decoy.join(10_000);
        racer.join(10_000);

        assertThat(decoyFailure.get()).as("momeala nu trebuia să pice").isNull();
        assertThat(raceFailure.get()).as("cererea nu trebuia să arunce în afara HTTP").isNull();
        assertThat(raceResult.get()).isNotNull();
        return raceResult.get();
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static String randomCui() {
        return "RO" + UUID.randomUUID().toString().replaceAll("\\D", "").substring(0, 8);
    }

    private static String randomEmail() {
        return "coliziune+" + UUID.randomUUID().toString().substring(0, 8) + "@demo.ro";
    }

    private String companyBody(String cui) {
        return """
                {
                  "name": "Coliziune SRL",
                  "cui": "%s",
                  "type": "GENERATOR",
                  "afmObligation": false
                }
                """.formatted(cui);
    }

    private String inviteBody(String email) {
        return """
                {
                  "email": "%s",
                  "role": "OPERATOR",
                  "firstName": "Test",
                  "lastName": "Coliziune"
                }
                """.formatted(email);
    }
}
