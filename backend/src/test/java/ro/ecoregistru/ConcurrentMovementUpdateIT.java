package ro.ecoregistru;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
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
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WorkPointRepository;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P1.8, the half of concurrency that was left after the double-submit tests: <b>a lost update on
 * the same movement</b>. Both halves of the fix already exist — {@code WasteMovement.version}
 * ({@code @Version}) and {@code AdviceController.handleOptimisticLock()} — so this proves the
 * contract, it does not write it.
 *
 * <p>The trick is forcing the race deterministically. Two {@code PUT} requests started together
 * would <em>usually</em> both read the old version and one would lose, but "usually" is not a
 * proof. Instead a background thread takes a real {@code PESSIMISTIC_WRITE} row lock and holds it
 * open in its own transaction — the HTTP request's own {@code SELECT} is never blocked by that
 * lock (MVCC), so it still reads the pre-conflict version and runs its full validation, but its
 * final {@code UPDATE ... WHERE version = ?} <em>is</em> blocked by the lock. Only once the
 * background thread commits (bumping the version) is the request's update released — straight
 * into a version mismatch, deterministically, every run.
 *
 * <p>🔴 <b>BUG-007, fixed.</b> The first run of this test caught a real gap: the conflict is
 * detected, but {@code AuditWriter.writePending()} called the raw {@code entityManager.flush()}
 * directly inside a {@code beforeCommit} transaction synchronization — a plain field, not a
 * {@code @Repository} bean — so Spring's persistence exception translation never ran on it. The
 * flush threw {@code jakarta.persistence.OptimisticLockException} (the untranslated JPA type),
 * which is not an {@code org.springframework.dao.OptimisticLockingFailureException} and fell
 * through to the generic handler: {@code 500} + a Sentry alert for a conflict the API was already
 * built to answer with {@code 409}. Fixed by translating the exception in
 * {@code AuditWriter.flush()}. This test is the regression.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class ConcurrentMovementUpdateIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired ObjectMapper objectMapper;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired PlatformTransactionManager transactionManager;
    @PersistenceContext EntityManager entityManager;

    private String adminToken;
    private UUID workPointId;
    private UUID wasteCodeId;

    @BeforeEach
    void setUp() {
        AppUser admin = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        adminToken = jwtService.generateToken(admin);
        UUID companyId = admin.getCompany().getId();
        workPointId = workPointRepository.findAllByCompany_Id(companyId).get(0).getId();
        wasteCodeId = wasteCodeRepository.findByCode("20 01 01").orElseThrow().getId();
    }

    @Test
    void aWriteThatArrivesAfterAnotherHasCommittedIsRejectedNotMerged() throws Exception {
        UUID movementId = createMovement("5.000");
        long versionBeforeEither = movementRepository.findById(movementId).orElseThrow().getVersion();

        CountDownLatch lockHeld = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);
        AtomicReference<Throwable> lockerFailure = new AtomicReference<>();

        Thread locker = new Thread(() -> {
            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            try {
                tt.executeWithoutResult(status -> {
                    WasteMovement locked = entityManager.find(
                            WasteMovement.class, movementId, LockModeType.PESSIMISTIC_WRITE);
                    lockHeld.countDown();
                    await(releaseLock);
                    locked.setNotes("scris de firul care a câștigat cursa");
                });
            } catch (Throwable t) {
                lockerFailure.set(t);
            }
        });
        locker.start();
        assertThat(lockHeld.await(10, TimeUnit.SECONDS)).as("lock-ul a fost luat").isTrue();

        AtomicReference<MvcResult> requesterResult = new AtomicReference<>();
        AtomicReference<Throwable> requesterFailure = new AtomicReference<>();
        Thread requester = new Thread(() -> {
            try {
                requesterResult.set(mockMvc.perform(put("/api/v1/movements/" + movementId)
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(movementBody("9.999")))
                        .andReturn());
            } catch (Throwable t) {
                requesterFailure.set(t);
            }
        });
        requester.start();

        // Timp să treacă de toate verificările dinaintea scrierii finale (firmă, punct de lucru,
        // cod, partener, generator, registru) și să ajungă la UPDATE-ul care se blochează pe
        // lock-ul de mai sus — pe embedded Postgres, câteva zeci de milisecunde, 500 e generos.
        Thread.sleep(500);
        releaseLock.countDown();

        locker.join(10_000);
        requester.join(10_000);

        assertThat(lockerFailure.get()).as("firul câștigător nu trebuia să pice").isNull();
        assertThat(requesterFailure.get()).as("cererea nu trebuia să arunce în afara HTTP").isNull();

        MvcResult result = requesterResult.get();
        assertThat(result).isNotNull();
        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("error-code").asText()).isEqualTo("concurrent.update");

        WasteMovement afterwards = movementRepository.findById(movementId).orElseThrow();
        assertThat(afterwards.getNotes()).isEqualTo("scris de firul care a câștigat cursa");
        // Pierzătorul n-a scris nimic: nici cantitatea lui, nici o versiune în plus.
        assertThat(afterwards.getQuantity()).isEqualByComparingTo("5.000");
        assertThat(afterwards.getVersion()).isEqualTo(versionBeforeEither + 1);
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private String movementBody(String quantity) {
        return """
                {
                  "workPointId": "%s",
                  "date": "2031-07-05",
                  "wasteCodeId": "%s",
                  "quantity": %s,
                  "unit": "KG",
                  "physicalState": "SOLID",
                  "operation": "RECOVERED", "register": "ANEXA_1", "physicalState": "SOLID", "storageType": "CT", "transportMeans": "AN", "packagingCategory": "SECONDARY", "wasteDestination": "Vr", "operationCode": "R13"
                }
                """.formatted(workPointId, wasteCodeId, quantity);
    }

    private UUID createMovement(String quantity) throws Exception {
        String created = mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movementBody(quantity)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(created).get("id").asText());
    }
}
