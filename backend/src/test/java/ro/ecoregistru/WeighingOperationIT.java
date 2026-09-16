package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.controller.response.WeighingOperationResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.NaturalPerson;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.WeighingOperationStatus;
import ro.ecoregistru.enums.WeighingOperationType;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.NaturalPersonRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.WeighingOperationService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ro.ecoregistru.enums.WeighingOperationType.IN;
import static ro.ecoregistru.enums.WeighingOperationType.OUT;

/**
 * D1.2 — capul operațiunii de depozit: numerotarea sigură la concurență și regulile de bază.
 * Fiecare test are firma lui, deci numerotarea pornește de la 1.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class WeighingOperationIT {

    @Autowired WeighingOperationService service;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired NaturalPersonRepository naturalPersonRepository;
    @Autowired PlatformTransactionManager transactionManager;

    UUID tenantId;
    AppUser user;
    WorkPoint depot;
    Partner collector;
    NaturalPerson person;

    @BeforeEach
    void setUp() {
        Company company = company(CompanyType.COLLECTOR);
        tenantId = company.getId();
        user = user(company);
        depot = workPointRepository.save(WorkPoint.builder()
                .company(company).name("Depozit Baciu").active(true).createdAt(Instant.now()).build());
        collector = partnerRepository.save(Partner.builder()
                .company(company).name("Reciclator SRL").cui("RO" + suffix())
                .type(PartnerType.COLLECTOR).supplier(true).packagingOrigin(PackagingOrigin.COLECTOR)
                .active(true).createdAt(Instant.now()).build());
        person = naturalPersonRepository.save(NaturalPerson.builder()
                .company(company).name("Ion Popescu").active(true).createdAt(Instant.now()).build());
        actAs(tenantId, user);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    /**
     * Determinist, nu de noroc: prima creare rămâne necomisă cât timp a doua pornește. Fără lacăt,
     * a doua ar citi maximul fără rândul primei, ar lua tot 1 și una din ele ar pica pe indexul unic.
     */
    @Test
    void twoSimultaneousCreationsGetDifferentNumbers() throws Exception {
        CountDownLatch firstCreated = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicReference<WeighingOperationResponse> first = new AtomicReference<>();
        AtomicReference<WeighingOperationResponse> second = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread firstThread = new Thread(() -> {
            actAs(tenantId, user);
            try {
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    first.set(service.create(request(IN, collector.getId(), null)));
                    firstCreated.countDown();
                    await(releaseFirst);
                });
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            }
        });
        firstThread.start();
        assertThat(firstCreated.await(10, TimeUnit.SECONDS)).as("prima creare e scrisă").isTrue();

        Thread secondThread = new Thread(() -> {
            actAs(tenantId, user);
            try {
                second.set(service.create(request(IN, collector.getId(), null)));
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            }
        });
        secondThread.start();
        Thread.sleep(500);
        boolean secondWaited = secondThread.isAlive();
        releaseFirst.countDown();
        firstThread.join(10_000);
        secondThread.join(10_000);

        assertThat(failure.get()).as("nicio creare nu trebuia să pice").isNull();
        assertThat(secondWaited).as("a doua creare așteaptă commitul primei").isTrue();
        assertThat(List.of(first.get().number(), second.get().number())).containsExactly(1, 2);
    }

    @Test
    void numbersAreSeparatePerType() {
        assertThat(service.create(request(IN, collector.getId(), null)).number()).isEqualTo(1);
        assertThat(service.create(request(IN, null, person.getId())).number()).isEqualTo(2);
        assertThat(service.create(request(OUT, collector.getId(), null)).number()).isEqualTo(1);
    }

    @Test
    void aNewOperationIsInProgressWithTheOriginOfItsCounterparty() {
        WeighingOperationResponse fromPartner = service.create(request(IN, collector.getId(), null));
        WeighingOperationResponse fromPerson = service.create(request(IN, null, person.getId()));

        assertThat(fromPartner.status()).isEqualTo(WeighingOperationStatus.IN_PROGRESS);
        assertThat(fromPartner.origin()).isEqualTo(PackagingOrigin.COLECTOR);
        assertThat(fromPerson.origin()).isEqualTo(PackagingOrigin.POPULATIE);
        assertThat(fromPerson.naturalPersonName()).isEqualTo("Ion Popescu");
    }

    @Test
    void aNaturalPersonCannotReceiveAnExit() {
        assertThatThrownBy(() -> service.create(request(OUT, null, person.getId())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("persoană fizică doar vinde");
    }

    @Test
    void onlyInAndOutAreAvailableInThisSlice() {
        assertThatThrownBy(() -> service.create(request(WeighingOperationType.TRANSFER, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("doar intrări și ieșiri");
    }

    @Test
    void anotherCompanysDepotIsNotFound() {
        Company other = company(CompanyType.COLLECTOR);
        WorkPoint foreign = workPointRepository.save(WorkPoint.builder()
                .company(other).name("Depozit străin").active(true).createdAt(Instant.now()).build());

        assertThatThrownBy(() -> service.create(new WeighingOperationRequest(IN, foreign.getId(),
                LocalDate.of(2026, 9, 15), collector.getId(), null, null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    /** Filtrele ecranului (D1.15): direcția și luna, amândouă opționale. */
    @Test
    void theScreenListFiltersByDirectionAndMonth() {
        service.create(request(IN, collector.getId(), null));
        service.create(request(OUT, collector.getId(), null));
        UUID august = service.create(new WeighingOperationRequest(IN, depot.getId(), LocalDate.of(2026, 8, 3),
                collector.getId(), null, null, null, null, null, null, null, null, null, null)).id();

        assertThat(service.list()).hasSize(3);
        assertThat(service.list(IN, null, null)).hasSize(2);
        assertThat(service.list(null, 2026, 9)).hasSize(2);
        assertThat(service.list(IN, 2026, 8)).extracting(WeighingOperationResponse::id).containsExactly(august);
        assertThat(service.list(null, 2026, null)).hasSize(3);
        assertThat(service.list(OUT, 2026, 8)).isEmpty();
    }

    @Test
    void aGeneratorHasNoDepotOperations() {
        Company generator = company(CompanyType.GENERATOR);
        actAs(generator.getId(), user(generator));

        assertThatThrownBy(() -> service.create(request(IN, null, null)))
                .isInstanceOf(BusinessException.class);
    }

    // --- helpers ---

    private WeighingOperationRequest request(WeighingOperationType type, UUID partnerId, UUID personId) {
        return new WeighingOperationRequest(type, depot.getId(), LocalDate.of(2026, 9, 15),
                partnerId, personId, null, null, null, null, null, null, null, null, null);
    }

    private Company company(CompanyType type) {
        return companyRepository.save(Company.builder()
                .name("Depozit " + suffix() + " SRL").cui("ROD" + suffix()).type(type)
                .active(true).createdAt(Instant.now()).build());
    }

    private AppUser user(Company company) {
        return appUserRepository.save(AppUser.builder()
                .email("depozit+" + suffix() + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
    }

    private static void actAs(UUID tenant, AppUser user) {
        TenantContext.set(tenant);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
