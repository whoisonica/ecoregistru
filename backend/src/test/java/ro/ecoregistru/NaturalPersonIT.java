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
import ro.ecoregistru.controller.request.WeighingLinesRequest;
import ro.ecoregistru.controller.request.WeighingLinesRequest.Line;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.NaturalPerson;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WeighingOperation;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.WeighingOperationStatus;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.ErrorMessageEnum;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.NaturalPersonRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteArticleRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WeighingOperationRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.NaturalPersonRetentionScheduler;
import ro.ecoregistru.service.WeighingOperationService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ro.ecoregistru.enums.WeighingOperationType.IN;

/**
 * D1.7 — persoanele fizice: identitatea completă se cere doar la metal (OUG 31/2011 art. 1 alin. (1^2)),
 * iar datele pleacă după 10 ani întregi fără nicio operațiune (Legea 82/1991 art. 25).
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class NaturalPersonIT {

    /** CNP valid (cifra de control 7), folosit și în `DriverDataRetentionIT`. */
    private static final String CNP = "1900101123457";
    private static final LocalDate DAY = LocalDate.of(2026, 9, 15);

    @Autowired WeighingOperationService service;
    @Autowired NaturalPersonRetentionScheduler retention;
    @Autowired NaturalPersonRepository personRepository;
    @Autowired WeighingOperationRepository operationRepository;
    @Autowired WasteArticleRepository articleRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired ro.ecoregistru.repository.AuditLogRepository auditLogRepository;

    Company company;
    WorkPoint depot;
    WasteArticle copper;
    WasteArticle paper;

    @BeforeEach
    void setUp() {
        company = companyRepository.save(Company.builder()
                .name("PF " + suffix() + " SRL").cui("ROP" + suffix()).type(CompanyType.COLLECTOR)
                .active(true).createdAt(Instant.now()).build());
        AppUser admin = appUserRepository.save(AppUser.builder()
                .email("pf+" + suffix() + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        depot = workPointRepository.save(WorkPoint.builder()
                .company(company).name("Depozit").active(true).createdAt(Instant.now()).build());
        copper = article("Cupru", "17 04 01", true);
        paper = article("Hârtie", "20 01 01", false);
        TenantContext.set(company.getId());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void metalFromAPersonWithOnlyANameIsRefused() {
        UUID id = fromPerson(person("Ion Popescu", null, null, null));

        assertThatThrownBy(() -> service.replaceLines(id, lines(copper)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.NATURAL_PERSON_METAL_IDENTITY_REQUIRED));
    }

    /** Controlul pozitiv al regulii: aceeași persoană, doar cu nume, vinde hârtie și operațiunea se finalizează. */
    @Test
    void paperFromAPersonWithOnlyANameIsAccepted() {
        UUID id = fromPerson(person("Ion Popescu", null, null, null));

        service.replaceLines(id, lines(paper));

        assertThat(service.finalizeOperation(id).status()).isEqualTo(WeighingOperationStatus.FINALIZED);
    }

    @Test
    void metalNeedsAValidCnpTheIdAndTheAddress() {
        assertRefused(person("A", "1900101123458", "CJ 123456", "Cluj, str. X 1")); // cifra de control greșită
        // CNP-uri diferite: indexul `uq_natural_persons_cnp` e unic pe firmă.
        assertRefused(person("B", "2900101123459", " ", "Cluj, str. X 1"));
        assertRefused(person("C", "5000101123457", "CJ 123456", null));

        UUID id = fromPerson(person("D", CNP, "CJ 123456", "Cluj, str. X 1"));
        service.replaceLines(id, lines(copper, paper));
        assertThat(service.finalizeOperation(id).status()).isEqualTo(WeighingOperationStatus.FINALIZED);
    }

    /** Fișa se poate goli între cântărire și finalizare; finalizarea verifică din nou. */
    @Test
    void finalizingRechecksTheIdentity() {
        NaturalPerson person = person("Ion Popescu", CNP, "CJ 123456", "Cluj, str. X 1");
        UUID id = fromPerson(person);
        service.replaceLines(id, lines(copper));

        person.setCnp(null);
        personRepository.saveAndFlush(person);

        assertThatThrownBy(() -> service.finalizeOperation(id))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.NATURAL_PERSON_METAL_IDENTITY_REQUIRED));
    }

    @Test
    void metalFromAPartnerNeedsNoPersonalData() {
        Partner partner = partnerRepository.save(Partner.builder()
                .company(company).name("Fier Vechi SRL").cui("RO" + suffix())
                .type(PartnerType.COLLECTOR).supplier(true).packagingOrigin(PackagingOrigin.COLECTOR)
                .active(true).createdAt(Instant.now()).build());
        UUID id = service.create(new WeighingOperationRequest(IN, depot.getId(), DAY, partner.getId(),
                null, null, null, null, null, null, null)).id();

        service.replaceLines(id, lines(copper));
        assertThat(service.finalizeOperation(id).status()).isEqualTo(WeighingOperationStatus.FINALIZED);
    }

    /**
     * Granița e 1 ianuarie, pe ani întregi: pe 1 iunie 2037, o operațiune din 31.12.2026 are zece ani
     * încheiați în urmă (2027–2036) și persoana se anonimizează; una din 01.01.2027 nu, încă. O persoană
     * creată de curând și nefolosită nu se atinge.
     */
    @Test
    void personalDataLeavesAfterTenFullYearsWithoutAnOperation() {
        NaturalPerson old = backdated(person("Vechi", CNP, "CJ 1", "Adresa 1"));
        NaturalPerson kept = backdated(person("Recent", "2900101123459", "CJ 2", "Adresa 2"));
        operation(old, LocalDate.of(2026, 12, 31));
        operation(kept, LocalDate.of(2027, 1, 1));
        // Creată după granița din 2027 și nefolosită: o fișă nouă nu se șterge doar fiindcă n-a vândut încă.
        NaturalPerson fresh = person("Nou", "5000101123457", "CJ 3", "Adresa 3");
        fresh.setCreatedAt(Instant.parse("2030-01-01T00:00:00Z"));
        personRepository.saveAndFlush(fresh);

        assertThat(retention.purge(LocalDate.of(2037, 6, 1))).isPositive();

        NaturalPerson oldAfter = personRepository.findById(old.getId()).orElseThrow();
        assertThat(oldAfter.getName()).isEqualTo(NaturalPersonRepository.ERASED_NAME);
        assertThat(oldAfter.getCnp()).isNull();
        assertThat(oldAfter.getIdentification()).isNull();
        assertThat(oldAfter.getAddress()).isNull();
        assertThat(oldAfter.isActive()).isFalse();
        assertThat(personRepository.findById(kept.getId()).orElseThrow().getCnp()).isEqualTo("2900101123459");
        assertThat(personRepository.findById(fresh.getId()).orElseThrow().getCnp()).isEqualTo("5000101123457");
    }

    /** CNP-ul și actul nu ajung în jurnalul de audit, nici la creare, nici la editare. */
    @Test
    void theAuditLogNeverHoldsTheCnpOrTheId() {
        NaturalPerson person = person("Ion Jurnal", CNP, "CJ 777777", "Cluj, str. Y 2");
        person.setIdentification("CJ 888888");
        personRepository.saveAndFlush(person);

        List<String> changes = auditLogRepository.findAll().stream()
                .filter(l -> person.getId().equals(l.getEntityId()))
                .map(l -> String.valueOf(l.getChanges()))
                .toList();
        assertThat(changes).as("controlul pozitiv: persoana chiar e auditată").isNotEmpty();
        assertThat(changes).noneMatch(c -> c.contains(CNP) || c.contains("CJ 777777") || c.contains("CJ 888888"));
        assertThat(String.join("", changes)).contains("•••");
    }

    /** Prin intrarea programată, cu tranzacția ei (capcana din 15.09.2026 la șoferi). */
    @Test
    void theScheduledEntryPointActuallyAnonymizes() {
        NaturalPerson old = backdated(person("Uitat", CNP, "CJ 9", "Adresa 9"));

        retention.runDaily();

        assertThat(personRepository.findById(old.getId()).orElseThrow().getCnp()).isNull();
    }

    // --- helpers ---

    private void assertRefused(NaturalPerson person) {
        UUID id = fromPerson(person);
        assertThatThrownBy(() -> service.replaceLines(id, lines(copper)))
                .as(person.getName())
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.NATURAL_PERSON_METAL_IDENTITY_REQUIRED));
    }

    private UUID fromPerson(NaturalPerson person) {
        return service.create(new WeighingOperationRequest(IN, depot.getId(), DAY, null, person.getId(),
                null, null, null, null, null, null)).id();
    }

    /** O operațiune a persoanei la data cerută, direct în bază: regula de identitate nu e subiectul aici. */
    private void operation(NaturalPerson person, LocalDate date) {
        UUID id = fromPerson(person);
        WeighingOperation o = operationRepository.findById(id).orElseThrow();
        o.setDate(date);
        operationRepository.saveAndFlush(o);
    }

    private NaturalPerson person(String name, String cnp, String identification, String address) {
        return personRepository.saveAndFlush(NaturalPerson.builder()
                .company(company).name(name).cnp(cnp).identification(identification).address(address)
                .active(true).createdAt(Instant.now()).build());
    }

    private NaturalPerson backdated(NaturalPerson person) {
        person.setCreatedAt(Instant.now().minus(20 * 366, ChronoUnit.DAYS));
        return personRepository.saveAndFlush(person);
    }

    private WasteArticle article(String name, String code, boolean metal) {
        return articleRepository.save(WasteArticle.builder()
                .company(company).wasteCode(wasteCodeRepository.findByCode(code).orElseThrow())
                .name(name).metal(metal).active(true).createdAt(Instant.now()).build());
    }

    private static WeighingLinesRequest lines(WasteArticle... articles) {
        return new WeighingLinesRequest(null, null, java.util.Arrays.stream(articles)
                .map(a -> new Line(a.getId(), null, null, new BigDecimal("25"), null, null, null, null))
                .toList());
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
