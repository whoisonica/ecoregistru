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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Seeds a demo tenant + sample data so the app is demonstrable immediately.
 * Dev profile only, and only if the DB has no users yet (idempotent).
 *
 * Demo credentials (password for all): Parola123
 *   platform@ecoregistru.ro  -> PLATFORM_ADMIN (no tenant; use X-Tenant-Id to act on a tenant)
 *   admin@demo.ro            -> ADMIN of "Demo Reciclare SRL"
 *   operator@demo.ro         -> OPERATOR
 *   viewer@demo.ro           -> CLIENT_VIEWER
 *
 * The movements span six months (Feb–Jul 2026) across three work points so the
 * monthly evidence shows cumulative stock carrying over month to month.
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

        appUserRepository.save(user("admin@demo.ro", Role.ADMIN, company, encoded, "Ana", "Admin"));
        AppUser operator = appUserRepository.save(user("operator@demo.ro", Role.OPERATOR, company, encoded, "Ovidiu", "Operator"));
        appUserRepository.save(user("viewer@demo.ro", Role.CLIENT_VIEWER, company, encoded, "Vlad", "Viewer"));

        // --- Work points (3) ---
        WorkPoint wpCluj = workPointRepository.save(workPoint(company, "Punct de lucru Cluj", "Str. Fabricii nr. 10, Cluj-Napoca"));
        WorkPoint wpTurda = workPointRepository.save(workPoint(company, "Punct de lucru Turda", "Str. Industriei nr. 5, Turda"));
        WorkPoint wpDepozit = workPointRepository.save(workPoint(company, "Depozit Central Florești", "Str. Depozitelor nr. 22, Florești"));

        // --- Partners (5) ---
        Partner collector = partnerRepository.save(partner(company, "Colector Autorizat SA", "RO87654321",
                "AUT-2024-555", LocalDate.now().plusDays(45), PartnerType.COLLECTOR)); // within 60 days -> alert
        Partner carrier = partnerRepository.save(partner(company, "Transport Deșeuri SRL", "RO11223344",
                "AUT-2023-100", LocalDate.now().plusMonths(10), PartnerType.CARRIER));
        Partner metalRecycler = partnerRepository.save(partner(company, "Reciclare Metale SRL", "RO55667788",
                "AUT-2024-777", LocalDate.now().plusYears(2), PartnerType.COLLECTOR));
        Partner ecoValor = partnerRepository.save(partner(company, "Eco Valorificare SA", "RO99887766",
                "AUT-2025-012", LocalDate.now().plusMonths(6), PartnerType.BOTH));
        partnerRepository.save(partner(company, "Salubritate Municipală SA", "RO33445566",
                "AUT-2022-042", LocalDate.now().minusDays(30), PartnerType.CARRIER)); // expired -> red badge

        seedMovements(company, operator.getId(),
                wpCluj, wpTurda, wpDepozit,
                collector, carrier, metalRecycler, ecoValor);

        log.info("Demo data seeded. Login with admin@demo.ro / {}", DEMO_PASSWORD);
    }

    /**
     * Sample movements across Feb–Jul 2026. Designed so the evidence engine shows meaningful
     * cumulative stock (e.g. paper "20 01 01" at Cluj: 40→30→50→50→30→50.5), and so the depot's
     * glass shows the traded-goods flow that must stay out of Anexa 1.
     */
    private void seedMovements(Company company, UUID createdBy,
                               WorkPoint wpCluj, WorkPoint wpTurda, WorkPoint wpDepozit,
                               Partner collector, Partner carrier, Partner metalRecycler, Partner ecoValor) {
        WasteCode paper = wasteCodeRepository.findByCode("20 01 01").orElse(null);
        WasteCode plastic = wasteCodeRepository.findByCode("15 01 02").orElse(null);
        WasteCode mixed = wasteCodeRepository.findByCode("20 03 01").orElse(null);
        WasteCode metals = wasteCodeRepository.findByCode("20 01 40").orElse(null);
        WasteCode glass = wasteCodeRepository.findByCode("15 01 07").orElse(null);
        WasteCode oil = wasteCodeRepository.findByCode("13 02 08").orElse(null);   // hazardous
        WasteCode battery = wasteCodeRepository.findByCode("16 06 01").orElse(null); // hazardous
        if (paper == null || plastic == null || mixed == null || metals == null
                || glass == null || oil == null || battery == null) {
            log.warn("Waste codes not seeded yet — skipping sample movements.");
            return;
        }

        List<WasteMovement> ms = new ArrayList<>();

        // On a handover the R/D code is the operation the RECIPIENT performs — that is what cap. 3
        // and cap. 4 of Anexa 1 report, next to the operator's name. Handovers to a plain collector
        // are R13 ("stocarea deşeurilor înaintea oricărei operaţiuni R1-R12"): the collector holds
        // the waste until someone else recovers it. Handovers straight to a recycler carry that
        // recycler's own operation. See docs/intrebari-specialist.md — whether ANPM expects R13 or
        // the final operation in this common case is the one open question here.

        // ---- Paper (20 01 01) at Cluj — the carry-over showcase ----
        // Feb: +100 -60 = 40
        ms.add(mv(company, wpCluj, d(2, 3), paper, "100.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, "Generat intern", createdBy));
        ms.add(mv(company, wpCluj, d(2, 20), paper, "60.000", WasteOperation.HANDED_OVER, PhysicalState.SOLID, WasteOperationCode.R13, collector, "Aviz nr. 201", createdBy));
        // Mar: +80 -90 = 30
        ms.add(mv(company, wpCluj, d(3, 5), paper, "80.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy));
        ms.add(mv(company, wpCluj, d(3, 22), paper, "90.000", WasteOperation.HANDED_OVER, PhysicalState.SOLID, WasteOperationCode.R13, collector, "Aviz nr. 214", createdBy));
        // Apr: +120 -100 = 50
        ms.add(mv(company, wpCluj, d(4, 4), paper, "120.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy));
        ms.add(mv(company, wpCluj, d(4, 25), paper, "100.000", WasteOperation.HANDED_OVER, PhysicalState.SOLID, WasteOperationCode.R13, collector, "Aviz nr. 233", createdBy));
        // May: +90 -90 = 50
        ms.add(mv(company, wpCluj, d(5, 6), paper, "90.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy));
        ms.add(mv(company, wpCluj, d(5, 24), paper, "90.000", WasteOperation.HANDED_OVER, PhysicalState.SOLID, WasteOperationCode.R13, collector, "Aviz nr. 251", createdBy));
        // Jun: +110 -130 = 30
        ms.add(mv(company, wpCluj, d(6, 7), paper, "110.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy));
        ms.add(mv(company, wpCluj, d(6, 26), paper, "130.000", WasteOperation.HANDED_OVER, PhysicalState.SOLID, WasteOperationCode.R13, collector, "Aviz nr. 268", createdBy));
        // Jul: +120.5 -100 = 50.5
        ms.add(mv(company, wpCluj, d(7, 5), paper, "120.500", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, "Generat intern", createdBy));
        ms.add(mv(company, wpCluj, d(7, 11), paper, "100.000", WasteOperation.HANDED_OVER, PhysicalState.SOLID, WasteOperationCode.R13, collector, "Aviz nr. 285", createdBy));

        // ---- Plastic (15 01 02) at Cluj — generation + internal recovery (R3) ----
        ms.add(mv(company, wpCluj, d(2, 8), plastic, "50.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy));
        ms.add(mv(company, wpCluj, d(2, 18), plastic, "20.000", WasteOperation.RECOVERED, PhysicalState.SOLID, WasteOperationCode.R3, null, "Valorificare internă", createdBy));
        ms.add(mv(company, wpCluj, d(4, 10), plastic, "60.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy));
        ms.add(mv(company, wpCluj, d(4, 19), plastic, "30.000", WasteOperation.HANDED_OVER, PhysicalState.SOLID, WasteOperationCode.R3, ecoValor, "Aviz nr. 240", createdBy));
        ms.add(mv(company, wpCluj, d(7, 8), plastic, "80.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy));
        ms.add(mv(company, wpCluj, d(7, 16), plastic, "30.000", WasteOperation.RECOVERED, PhysicalState.SOLID, WasteOperationCode.R3, null, "Valorificare internă", createdBy));

        // ---- Metals (20 01 40) at Turda — handed to the metal recycler ----
        ms.add(mv(company, wpTurda, d(3, 9), metals, "200.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy));
        ms.add(mv(company, wpTurda, d(3, 21), metals, "150.000", WasteOperation.HANDED_OVER, PhysicalState.SOLID, WasteOperationCode.R4, metalRecycler, "Aviz nr. 310", createdBy));
        ms.add(mv(company, wpTurda, d(4, 12), metals, "180.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy));
        ms.add(mv(company, wpTurda, d(4, 27), metals, "180.000", WasteOperation.HANDED_OVER, PhysicalState.SOLID, WasteOperationCode.R4, metalRecycler, "Aviz nr. 341", createdBy));
        ms.add(mv(company, wpTurda, d(6, 11), metals, "220.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy));
        ms.add(mv(company, wpTurda, d(6, 24), metals, "100.000", WasteOperation.HANDED_OVER, PhysicalState.SOLID, WasteOperationCode.R4, metalRecycler, "Aviz nr. 372", createdBy));

        // ---- Mixed municipal (20 03 01) at Turda — disposal at a conforming landfill (D5) ----
        // D5 (specially engineered landfill) rather than D1 (deposit onto land): municipal waste
        // in RO goes to "depozite conforme" with sealed cells. Per specialist feedback, 2026-08-20.
        ms.add(mv(company, wpTurda, d(5, 3), mixed, "300.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy));
        ms.add(mv(company, wpTurda, d(5, 28), mixed, "300.000", WasteOperation.DISPOSED, PhysicalState.SOLID, WasteOperationCode.D5, carrier, "Aviz nr. 355", createdBy));
        ms.add(mv(company, wpTurda, d(7, 4), mixed, "260.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy));
        ms.add(mv(company, wpTurda, d(7, 18), mixed, "200.000", WasteOperation.DISPOSED, PhysicalState.SOLID, WasteOperationCode.D5, carrier, "Aviz nr. 388", createdBy));

        // ---- Glass (15 01 07) taken over at the depot, then passed on ----
        // The traded-goods flow, and the reason the register discriminator exists. Neither leg
        // belongs on Anexa 1: HG 856/2002 art. 2 alin. (1) keeps third-party goods out of it, so
        // both sit in the art. 48 chronological register. The takeover is classified by its
        // operation; the hand-on is not — passing on collected glass looks exactly like handing
        // over own glass, so it has to be said out loud.
        ms.add(mv(company, wpDepozit, d(6, 2), glass, "500.000", WasteOperation.COLLECTED, PhysicalState.SOLID, null, carrier, "Recepție 15/06", createdBy));
        WasteMovement glassPassedOn = mv(company, wpDepozit, d(6, 20), glass, "450.000", WasteOperation.HANDED_OVER, PhysicalState.SOLID, WasteOperationCode.R5, ecoValor, "Aviz nr. 366", createdBy);
        glassPassedOn.setRegister(WasteRegister.ART_48);
        ms.add(glassPassedOn);

        // ---- Hazardous: waste oils (13 02 08) at Cluj ----
        ms.add(mv(company, wpCluj, d(4, 15), oil, "15.000", WasteOperation.GENERATED, PhysicalState.LIQUID, null, null, "Schimb ulei utilaje", createdBy));
        ms.add(mv(company, wpCluj, d(4, 23), oil, "15.000", WasteOperation.HANDED_OVER, PhysicalState.LIQUID, WasteOperationCode.R13, collector, "Aviz nr. 238", createdBy));

        // ---- Hazardous: lead batteries (16 06 01) at the depot ----
        ms.add(mv(company, wpDepozit, d(5, 14), battery, "8.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy));
        ms.add(mv(company, wpDepozit, d(5, 29), battery, "8.000", WasteOperation.HANDED_OVER, PhysicalState.SOLID, WasteOperationCode.R13, collector, "Aviz nr. 359", createdBy));

        wasteMovementRepository.saveAll(ms);
        log.info("Seeded {} sample movements.", ms.size());
    }

    /** A 2026 date at the given month/day (movements span Feb–Jul 2026). */
    private LocalDate d(int month, int day) {
        return LocalDate.of(2026, month, day);
    }

    private WasteMovement mv(Company company, WorkPoint wp, LocalDate date, WasteCode code,
                             String qty, WasteOperation op, PhysicalState physicalState,
                             WasteOperationCode operationCode, Partner partner, String docRef,
                             UUID createdBy) {
        return WasteMovement.builder()
                .company(company)
                .workPoint(wp)
                .date(date)
                .wasteCode(code)
                .quantity(new BigDecimal(qty))
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

    private WorkPoint workPoint(Company company, String name, String address) {
        return WorkPoint.builder()
                .company(company)
                .name(name)
                .address(address)
                .active(true)
                .createdAt(Instant.now())
                .build();
    }

    private Partner partner(Company company, String name, String cui, String authNumber,
                            LocalDate authExpiry, PartnerType type) {
        return Partner.builder()
                .company(company)
                .name(name)
                .cui(cui)
                .authorizationNumber(authNumber)
                .authorizationExpiry(authExpiry)
                .type(type)
                .active(true)
                .createdAt(Instant.now())
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
