package ro.ecoregistru.bootstrap;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Seeds a demo tenant + sample data so the app is demonstrable immediately.
 * Dev profile only, and only if the DB has no users yet (idempotent).
 *
 * Demo credentials (password for all): Parola123
 *   platform@ecoregistru.ro  -> PLATFORM_ADMIN (no tenant; use X-Tenant-Id to act on a tenant)
 *   admin@demo.ro            -> ADMIN of "Demo Reciclare SRL"
 *   operator@demo.ro         -> OPERATOR
 *   viewer@demo.ro           -> CLIENT_VIEWER
 */
@Slf4j
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DevDataSeeder implements CommandLineRunner {

    static final String DEMO_PASSWORD = "Parola123";

    CompanyRepository companyRepository;
    AppUserRepository appUserRepository;
    WorkPointRepository workPointRepository;
    PartnerRepository partnerRepository;
    WasteCodeRepository wasteCodeRepository;
    WasteMovementRepository wasteMovementRepository;
    PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (appUserRepository.existsByEmail("platform@ecoregistru.ro")) {
            log.info("Demo data already present — skipping seed.");
            return;
        }
        log.info("Seeding demo data (dev profile)...");

        String encoded = passwordEncoder.encode(DEMO_PASSWORD);

        // Platform admin (global, no tenant)
        appUserRepository.save(AppUser.builder()
                .email("platform@ecoregistru.ro")
                .password(encoded)
                .role(Role.PLATFORM_ADMIN)
                .firstName("Platform")
                .lastName("Admin")
                .enabled(true)
                .createdAt(Instant.now())
                .build());

        // Demo tenant
        Company company = companyRepository.save(Company.builder()
                .name("Demo Reciclare SRL")
                .cui("RO12345678")
                .type(CompanyType.BOTH)
                .environmentalAuthNumber("APM-CJ-123")
                .environmentalAuthExpiry(LocalDate.now().plusMonths(8))
                .address("Str. Exemplu nr. 1, Cluj-Napoca")
                .contactName("Ion Popescu")
                .contactEmail("contact@demo.ro")
                .contactPhone("0740000000")
                .active(true)
                .afmObligation(true) // demo: enables the monthly AFM deadline (FAZA TERMENE)
                .createdAt(Instant.now())
                .build());

        AppUser admin = appUserRepository.save(user("admin@demo.ro", Role.ADMIN, company, encoded, "Ana", "Admin"));
        AppUser operator = appUserRepository.save(user("operator@demo.ro", Role.OPERATOR, company, encoded, "Ovidiu", "Operator"));
        appUserRepository.save(user("viewer@demo.ro", Role.CLIENT_VIEWER, company, encoded, "Vlad", "Viewer"));

        WorkPoint workPoint = workPointRepository.save(WorkPoint.builder()
                .company(company)
                .name("Punct de lucru Cluj")
                .address("Str. Fabricii nr. 10, Cluj-Napoca")
                .active(true)
                .createdAt(Instant.now())
                .build());

        Partner collector = partnerRepository.save(Partner.builder()
                .company(company)
                .name("Colector Autorizat SA")
                .cui("RO87654321")
                .authorizationNumber("AUT-2024-555")
                .authorizationExpiry(LocalDate.now().plusDays(45)) // within 60 days -> triggers alert
                .type(PartnerType.COLLECTOR)
                .active(true)
                .createdAt(Instant.now())
                .build());

        partnerRepository.save(Partner.builder()
                .company(company)
                .name("Transport Deșeuri SRL")
                .cui("RO11223344")
                .authorizationNumber("AUT-2023-100")
                .authorizationExpiry(LocalDate.now().plusMonths(10))
                .type(PartnerType.CARRIER)
                .active(true)
                .createdAt(Instant.now())
                .build());

        seedMovements(company, workPoint, collector, operator.getId());

        log.info("Demo data seeded. Login with admin@demo.ro / {}", DEMO_PASSWORD);
    }

    private void seedMovements(Company company, WorkPoint wp, Partner collector, java.util.UUID createdBy) {
        WasteCode paper = wasteCodeRepository.findByCode("20 01 01").orElse(null);
        WasteCode plastic = wasteCodeRepository.findByCode("15 01 02").orElse(null);
        WasteCode mixed = wasteCodeRepository.findByCode("20 03 01").orElse(null);
        if (paper == null || plastic == null || mixed == null) {
            log.warn("Waste codes not seeded yet — skipping sample movements.");
            return;
        }
        LocalDate thisMonth = LocalDate.now().withDayOfMonth(5);

        wasteMovementRepository.saveAll(List.of(
                movement(company, wp, thisMonth, paper, new BigDecimal("120.500"), WasteOperation.GENERATED, PhysicalState.SOLID, null, null, "Generat intern", createdBy),
                movement(company, wp, thisMonth.plusDays(3), plastic, new BigDecimal("80.000"), WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy),
                movement(company, wp, thisMonth.plusDays(6), paper, new BigDecimal("100.000"), WasteOperation.HANDED_OVER, PhysicalState.SOLID, null, collector, "Aviz nr. 245", createdBy),
                movement(company, wp, thisMonth.plusDays(8), mixed, new BigDecimal("2.500"), WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy),
                movement(company, wp, thisMonth.plusDays(9), plastic, new BigDecimal("30.000"), WasteOperation.RECOVERED, PhysicalState.SOLID, WasteOperationCode.R3, null, "Valorificare internă", createdBy)
        ));
    }

    private WasteMovement movement(Company company, WorkPoint wp, LocalDate date, WasteCode code,
                                   BigDecimal qty, WasteOperation op, PhysicalState physicalState,
                                   WasteOperationCode operationCode, Partner partner, String docRef, java.util.UUID createdBy) {
        return WasteMovement.builder()
                .company(company)
                .workPoint(wp)
                .date(date)
                .wasteCode(code)
                .quantity(qty)
                .unit(Unit.KG)
                .operation(op)
                .physicalState(physicalState)
                .operationCode(operationCode)
                .partner(partner)
                .documentReference(docRef)
                .deleted(false)
                .createdBy(createdBy)
                .build();
    }

    private AppUser user(String email, Role role, Company company, String encodedPassword, String first, String last) {
        return AppUser.builder()
                .email(email)
                .password(encodedPassword)
                .role(role)
                .company(company)
                .firstName(first)
                .lastName(last)
                .enabled(true)
                .createdAt(Instant.now())
                .build();
    }
}
