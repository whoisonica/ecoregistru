package ro.ecoregistru.bootstrap;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * Seeds a demo tenant + sample data so the app is demonstrable immediately.
 * Dev profile only, and only if the DB has no users yet (idempotent).
 *
 * Demo roles: PLATFORM_ADMIN (no tenant; uses X-Tenant-Id to act on one), ADMIN and OPERATOR of
 * "Demo Reciclare SRL", and a CLIENT_VIEWER of the same.
 *
 * <p>The password is <b>not</b> in this file. It comes from {@code DEMO_PASSWORD}; when that is
 * unset a random one is generated per boot and written to the log. The literal that used to sit
 * here was published in a public repository, and on 09.09.2026 these accounts turned out to exist
 * in the production database too — so the repo was handing anyone a working PLATFORM_ADMIN login.
 * A fixture password is only a fixture password while it cannot reach anything real; committed, it
 * is a credential.
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


    CompanyRepository companyRepository;
    AppUserRepository appUserRepository;
    WorkPointRepository workPointRepository;
    PartnerRepository partnerRepository;
    WasteCodeRepository wasteCodeRepository;
    WasteMovementRepository wasteMovementRepository;
    InternalGeneratorRepository internalGeneratorRepository;
    AttachmentRepository attachmentRepository;
    PasswordEncoder passwordEncoder;

    /**
     * Parola conturilor demo, din mediu. Goală = se generează una la fiecare pornire şi se scrie
     * în log — deci ştie doar cine se uită la consola propriului server.
     */
    // `@NonFinal` fiindcă `@FieldDefaults(makeFinal = true)` de pe clasă l-ar face final, iar
    // `@RequiredArgsConstructor` l-ar cere atunci în constructor — unde Spring ar căuta un bean de
    // tip String şi ar cădea la pornire. Injectat pe câmp, îl completează `@Value`.
    @NonFinal
    @Value("${app.demo-password:}")
    String configuredPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (appUserRepository.existsByEmail("platform@ecoregistru.ro")) {
            log.info("Demo data already present — skipping seed.");
            return;
        }
        log.info("Seeding demo data (dev profile)...");

        String demoPassword = resolveDemoPassword();
        String encoded = passwordEncoder.encode(demoPassword);

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
        // The commercial role is the second axis: client = we hand waste over and we invoice them
        // (the metal and the plastic we sell), supplier = they do the work and they invoice us
        // (collection, haulage, landfilling). Both is common — Eco Valorificare buys our plastic
        // and hauls our glass — which is why these are two flags and not one enum.
        Partner collector = partnerRepository.save(partner(company, "Colector Autorizat SA", "RO87654321",
                "AUT-2024-555", LocalDate.now().plusDays(45), PartnerType.COLLECTOR,
                false, true)); // within 60 days -> alert
        Partner carrier = partnerRepository.save(partner(company, "Transport Deșeuri SRL", "RO11223344",
                "AUT-2023-100", LocalDate.now().plusMonths(10), PartnerType.COLLECTOR, false, true));
        Partner metalRecycler = partnerRepository.save(partner(company, "Reciclare Metale SRL", "RO55667788",
                "AUT-2024-777", LocalDate.now().plusYears(2), PartnerType.COLLECTOR, true, false));
        Partner ecoValor = partnerRepository.save(partner(company, "Eco Valorificare SA", "RO99887766",
                "AUT-2025-012", LocalDate.now().plusMonths(6), PartnerType.COLLECTOR, true, true));
        // Expirarea e o **dată fixă**, nu `now().minusDays(30)` ca înainte, fiindcă de ea atârnă
        // acum două afirmații deodată: că fișa partenerului e roșie *azi*, și că predarea din
        // 20.08.2026 de mai jos s-a făcut *după* ce autorizația căzuse. Cu o dată care se mișcă
        // odată cu ziua de azi, a doua ar fi încetat să fie adevărată pe 19.09.2026 — fără ca
        // nimeni să atingă nimic, și fără ca vreo probă să spună de ce.
        Partner expiredAuth = partnerRepository.save(partner(company, "Salubritate Municipală SA", "RO33445566",
                "AUT-2022-042", d(7, 15), PartnerType.COLLECTOR,
                false, true)); // expired -> red badge
        // A partner whose authorization expiry nobody filled in. Blank is not "expired": it means
        // we do not know, and every rule in the application treats it that way — no badge at the
        // handover, no alert mail, and sorted to the end of the column in both directions. Without
        // a row in this state the sorting rule was provable only on paper: `missingLast()` was
        // written for exactly it, and the check that would have caught the bug ran on nothing.
        partnerRepository.save(partner(company, "Depozitare Ardeal SRL", "RO77889900",
                "AUT-2023-318", null, PartnerType.COLLECTOR, true, true));

        // --- Internal generators (Anexa 1 cap. 2 "Secţia") ---
        InternalGenerator birouri = internalGeneratorRepository.save(
                internalGenerator(company, wpCluj, "birouri", "Birouri administrative"));
        InternalGenerator productie = internalGeneratorRepository.save(
                internalGenerator(company, wpTurda, "productie", "Hala de producţie"));
        internalGeneratorRepository.save(
                internalGenerator(company, wpDepozit, "sortare", "Linia de sortare"));

        seedMovements(company, operator.getId(),
                wpCluj, wpTurda, wpDepozit,
                collector, carrier, metalRecycler, ecoValor, expiredAuth,
                birouri, productie);

        log.info("Demo data seeded. Login with admin@demo.ro / {}", demoPassword);
    }

    /**
     * Sample movements across Feb–Jul 2026. Designed so the evidence engine shows meaningful
     * cumulative stock (e.g. paper "20 01 01" at Cluj: 40→30→50→50→30→50.5), and so the depot's
     * glass shows the traded-goods flow that must stay out of Anexa 1.
     */
    private void seedMovements(Company company, UUID createdBy,
                               WorkPoint wpCluj, WorkPoint wpTurda, WorkPoint wpDepozit,
                               Partner collector, Partner carrier, Partner metalRecycler, Partner ecoValor,
                               Partner expiredAuth,
                               InternalGenerator birouri, InternalGenerator productie) {
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

        // --- The account profile: what the demo client answered on the intake form ---
        // Exactly the operations the movements below use, so the demo shows the narrowing doing
        // its job: the code picker offers five entries instead of twenty-eight.
        company.setAuthorizedOperationCodes(new LinkedHashSet<>(List.of(
                WasteOperationCode.R3, WasteOperationCode.R4, WasteOperationCode.R5,
                WasteOperationCode.R13, WasteOperationCode.D5)));
        company.setAuthorizedWasteCodes(new LinkedHashSet<>(
                List.of(paper, plastic, mixed, metals, glass, oil, battery)));
        company.setTransportMeans("Autoutilitară 3,5 t · container 20 mc");
        company.setTransportLicenseNumber("LTM-2024-0912");
        company.setTransportLicenseExpiry(LocalDate.now().plusMonths(18));

        List<WasteMovement> ms = new ArrayList<>();

        // On a handover the R/D code is the operation the RECIPIENT performs — that is what cap. 3
        // and cap. 4 of Anexa 1 report, next to the operator's name. Handovers to a plain collector
        // are R13 ("stocarea deşeurilor înaintea oricărei operaţiuni R1-R12"): the collector holds
        // the waste until someone else recovers it. Handovers straight to a recycler carry that
        // recycler's own operation. See docs/intrebari-specialist.md — whether ANMAP expects R13 or
        // the final operation in this common case is the one open question here.

        // ---- Paper (20 01 01) at Cluj — the carry-over showcase ----
        // Feb: +100 -60 = 40
        ms.add(section(mv(company, wpCluj, d(2, 3), paper, "100.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, "Generat intern", createdBy), birouri));
        ms.add(mv(company, wpCluj, d(2, 20), paper, "60.000", WasteOperation.RECOVERED, PhysicalState.SOLID, WasteOperationCode.R13, collector, "Aviz nr. 201", createdBy));
        // Mar: +80 -90 = 30
        ms.add(section(mv(company, wpCluj, d(3, 5), paper, "80.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy), birouri));
        ms.add(mv(company, wpCluj, d(3, 22), paper, "90.000", WasteOperation.RECOVERED, PhysicalState.SOLID, WasteOperationCode.R13, collector, "Aviz nr. 214", createdBy));
        // Apr: +120 -100 = 50
        ms.add(section(mv(company, wpCluj, d(4, 4), paper, "120.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy), birouri));
        ms.add(mv(company, wpCluj, d(4, 25), paper, "100.000", WasteOperation.RECOVERED, PhysicalState.SOLID, WasteOperationCode.R13, collector, "Aviz nr. 233", createdBy));
        // May: +90 -90 = 50
        ms.add(section(mv(company, wpCluj, d(5, 6), paper, "90.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy), birouri));
        ms.add(mv(company, wpCluj, d(5, 24), paper, "90.000", WasteOperation.RECOVERED, PhysicalState.SOLID, WasteOperationCode.R13, collector, "Aviz nr. 251", createdBy));
        // Jun: +110 -130 = 30
        ms.add(section(mv(company, wpCluj, d(6, 7), paper, "110.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy), birouri));
        ms.add(mv(company, wpCluj, d(6, 26), paper, "130.000", WasteOperation.RECOVERED, PhysicalState.SOLID, WasteOperationCode.R13, collector, "Aviz nr. 268", createdBy));
        // Jul: +120.5 -100 = 50.5
        ms.add(section(mv(company, wpCluj, d(7, 5), paper, "120.500", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, "Generat intern", createdBy), birouri));
        ms.add(mv(company, wpCluj, d(7, 11), paper, "100.000", WasteOperation.RECOVERED, PhysicalState.SOLID, WasteOperationCode.R13, collector, "Aviz nr. 285", createdBy));

        // ---- Plastic (15 01 02) at Cluj — generation + internal recovery (R3) ----
        ms.add(section(mv(company, wpCluj, d(2, 8), plastic, "50.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy), birouri));
        ms.add(mv(company, wpCluj, d(2, 18), plastic, "20.000", WasteOperation.RECOVERED, PhysicalState.SOLID, WasteOperationCode.R3, null, "Valorificare internă", createdBy));
        ms.add(section(mv(company, wpCluj, d(4, 10), plastic, "60.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy), birouri));
        ms.add(mv(company, wpCluj, d(4, 19), plastic, "30.000", WasteOperation.RECOVERED, PhysicalState.SOLID, WasteOperationCode.R3, ecoValor, "Aviz nr. 240", createdBy));
        ms.add(section(mv(company, wpCluj, d(7, 8), plastic, "80.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy), birouri));
        ms.add(mv(company, wpCluj, d(7, 16), plastic, "30.000", WasteOperation.RECOVERED, PhysicalState.SOLID, WasteOperationCode.R3, null, "Valorificare internă", createdBy));

        // ---- Metals (20 01 40) at Turda — handed to the metal recycler ----
        ms.add(section(mv(company, wpTurda, d(3, 9), metals, "200.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy), productie));
        ms.add(mv(company, wpTurda, d(3, 21), metals, "150.000", WasteOperation.RECOVERED, PhysicalState.SOLID, WasteOperationCode.R4, metalRecycler, "Aviz nr. 310", createdBy));
        ms.add(section(mv(company, wpTurda, d(4, 12), metals, "180.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy), productie));
        ms.add(mv(company, wpTurda, d(4, 27), metals, "180.000", WasteOperation.RECOVERED, PhysicalState.SOLID, WasteOperationCode.R4, metalRecycler, "Aviz nr. 341", createdBy));
        ms.add(section(mv(company, wpTurda, d(6, 11), metals, "220.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy), productie));
        ms.add(mv(company, wpTurda, d(6, 24), metals, "100.000", WasteOperation.RECOVERED, PhysicalState.SOLID, WasteOperationCode.R4, metalRecycler, "Aviz nr. 372", createdBy));

        // ---- Mixed municipal (20 03 01) at Turda — disposal at a conforming landfill (D5) ----
        // D5 (specially engineered landfill) rather than D1 (deposit onto land): municipal waste
        // in RO goes to "depozite conforme" with sealed cells. Per specialist feedback, 2026-08-20.
        ms.add(section(mv(company, wpTurda, d(5, 3), mixed, "300.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy), productie));
        ms.add(mv(company, wpTurda, d(5, 28), mixed, "300.000", WasteOperation.DISPOSED, PhysicalState.SOLID, WasteOperationCode.D5, carrier, "Aviz nr. 355", createdBy));
        ms.add(section(mv(company, wpTurda, d(7, 4), mixed, "260.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy), productie));
        ms.add(mv(company, wpTurda, d(7, 18), mixed, "200.000", WasteOperation.DISPOSED, PhysicalState.SOLID, WasteOperationCode.D5, carrier, "Aviz nr. 388", createdBy));

        // ---- Glass (15 01 07) taken over at the depot, then passed on ----
        // The traded-goods flow, and the reason the register discriminator exists. Neither leg
        // belongs on Anexa 1: HG 856/2002 art. 2 alin. (1) keeps third-party goods out of it, so
        // both sit in the art. 48 chronological register. The takeover is classified by its
        // operation; the hand-on is not — passing on collected glass looks exactly like handing
        // over own glass, so it has to be said out loud.
        ms.add(mv(company, wpDepozit, d(6, 2), glass, "500.000", WasteOperation.COLLECTED, PhysicalState.SOLID, null, carrier, "Recepție 15/06", createdBy));
        WasteMovement glassPassedOn = mv(company, wpDepozit, d(6, 20), glass, "450.000", WasteOperation.RECOVERED, PhysicalState.SOLID, WasteOperationCode.R5, ecoValor, "Aviz nr. 366", createdBy);
        glassPassedOn.setRegister(WasteRegister.ART_48);
        ms.add(glassPassedOn);

        // ---- Hazardous: waste oils (13 02 08) at Cluj ----
        ms.add(section(mv(company, wpCluj, d(4, 15), oil, "15.000", WasteOperation.GENERATED, PhysicalState.LIQUID, null, null, "Schimb ulei utilaje", createdBy), birouri));
        ms.add(mv(company, wpCluj, d(4, 23), oil, "15.000", WasteOperation.RECOVERED, PhysicalState.LIQUID, WasteOperationCode.R13, collector, "Aviz nr. 238", createdBy));

        // ---- Hazardous: lead batteries (16 06 01) at the depot ----
        ms.add(mv(company, wpDepozit, d(5, 14), battery, "8.000", WasteOperation.GENERATED, PhysicalState.SOLID, null, null, null, createdBy));
        ms.add(mv(company, wpDepozit, d(5, 29), battery, "8.000", WasteOperation.RECOVERED, PhysicalState.SOLID, WasteOperationCode.R13, collector, "Aviz nr. 359", createdBy));

        // ---- Cele două stări pentru care ecranele au reguli proprii, dar seed-ul n-avea rânduri --
        //
        // Erau zero din 34 de mișcări fără cantitate și zero ieșiri fără cod R/D, deci fiecare
        // regulă scrisă în jurul lor se proba pe gol: roșu față de galben pe badge-uri, drumul de
        // la raport la mișcarea vinovată, sortarea care ține la coadă ce e nespus. Al treilea defect
        // din primitivele reparate pe 07.09 a scăpat exact așa — verificarea scrisă pentru el trecea
        // pe un tabel care n-avea rândul.
        //
        // Amândouă stau pe cod de plastic la Turda, dinadins: seria de hârtie de la Cluj
        // (40→30→50→50→30→50.5) e vitrina stocului cumulativ și rămâne neatinsă.

        // Ieșire fără cod R/D — badge **roșu**. Cantitatea a plecat de pe amplasament și nu intră în
        // nicio coloană oficială, deci fișa nu se poate depune așa. E cazul liniilor vechi, de
        // dinainte ca aplicația să ceară codul la orice ieșire: nu se poate ghici retroactiv, așa
        // că se **vede** în loc să se închidă tăcut.
        WasteMovement noCode = mv(company, wpTurda, d(6, 15), plastic, "40.000",
                WasteOperation.UNCLASSIFIED_OUT, PhysicalState.SOLID, null, collector,
                "Aviz nr. 369 (fără cod)", createdBy);
        ms.add(section(noCode, productie));

        // Predare care așteaptă cântarul destinatarului — badge **galben**, altă afirmație. Un
        // magazin fără cântar predă, colectorul cântărește la descărcare, iar pe modelul de Anexa 3
        // cifra e scrisă de mână. `quantity` e null dinadins: nici zero, nici o estimare nu stau în
        // locul unei măsurători, iar linia lunară se raportează provizorie până vine cifra.
        WasteMovement awaiting = mv(company, wpTurda, d(7, 22), plastic, null,
                WasteOperation.RECOVERED, PhysicalState.SOLID, WasteOperationCode.R3, ecoValor,
                "Aviz nr. 391", createdBy);
        awaiting.setWeighedAtUnloading(true);
        ms.add(section(awaiting, productie));

        // Predare către un partener a cărui autorizație **expirase înainte de data predării** —
        // starea din decizia 36, pe care badge-ul „Autorizație expirată" o semnalează pe Mișcări și
        // din care duce, de pe 08.09.2026, la fișa partenerului. Zero rânduri din 37 o aveau, deci
        // proba drumului se făcea pe gol; a trăit până azi ca `INSERT` aditiv în `e2e/README.md`,
        // adică nu exista pe o bază proaspătă.
        //
        // ⚠️ **Un kilogram, dinadins.** Prima variantă a rândului ăstuia punea 120 și a dus stocul
        // unui cod fix la zero: dala „Coduri cu stoc" a trecut de la 4 la 3 și proba 9 a căzut —
        // cea care fixează chiar cifra aia. Un rând de seed adăugat pentru o probă n-are voie să
        // miște datele pe care se sprijină alta.
        //
        // Data e **după** seria de hârtie de la Cluj (Feb–Iul), nu în ea: vitrina stocului
        // cumulativ (40→30→50→50→30→50.5) rămâne exact cum se citește mai sus, iar rândul ăsta
        // deschide o lună nouă. Nu stă pe plasticul de la Turda, lângă celelalte două stări,
        // fiindcă acolo ar muta chiar cifrele negative pe care dala de stoc e verificată.
        ms.add(mv(company, wpCluj, d(8, 20), paper, "1.000",
                WasteOperation.DISPOSED, PhysicalState.SOLID, WasteOperationCode.D5, expiredAuth,
                "Aviz nr. 402", createdBy));

        wasteMovementRepository.saveAll(ms);
        log.info("Seeded {} sample movements.", ms.size());

        // ---- A treia stare fără rânduri: mișcarea cu documente atașate ----
        //
        // Coloana „📎" avea zero din 36 de mișcări cu ceva în ea, deci vederea care le deschide se
        // proba pe gol — exact felul de gol în care s-a ascuns al treilea defect din primitive.
        // Două atașamente, nu unul: cu unul singur nu s-ar vedea dacă lista chiar le enumeră.
        //
        // Stau pe ieșirea fără cod R/D dinadins: e rândul pe care îl deschide inspectorul, iar
        // avizul lui e chiar hârtia după care întreabă.
        //
        // ⚠️ Nu s-a urcat nimic. `CLOUDINARY_URL` nu e setat nici local, nici pe dyno, deci în
        // dev nu există cale de a crea un atașament prin aplicație. URL-urile arată către cloud-ul
        // public `demo` al Cloudinary — se deschid, dar nu sunt documentele firmei.
        attachmentRepository.saveAll(List.of(
                attachment(noCode, "aviz-369.jpg", "image/jpeg",
                        "https://res.cloudinary.com/demo/image/upload/sample.jpg", "demo/sample"),
                attachment(noCode, "cantar-369.jpg", "image/jpeg",
                        "https://res.cloudinary.com/demo/image/upload/couple.jpg", "demo/couple")));
        log.info("Seeded 2 demo attachments on the movement without an R/D code.");
    }

    private Attachment attachment(WasteMovement movement, String fileName, String contentType,
                                  String url, String publicId) {
        return Attachment.builder()
                .movement(movement)
                .url(url)
                .publicId(publicId)
                .fileName(fileName)
                .contentType(contentType)
                .createdAt(Instant.now())
                .build();
    }

    /** Attaches the section the waste came from — Anexa 1 cap. 2 "Secţia". */
    private WasteMovement section(WasteMovement movement, InternalGenerator generator) {
        movement.setInternalGenerator(generator);
        return movement;
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
                // `null` e legal, și numai cu `weighedAtUnloading`: cantitatea se află la
                // descărcare. Vezi rândul „de cântărit" de mai sus.
                .quantity(qty == null ? null : new BigDecimal(qty))
                .unit(Unit.KG)
                .operation(op)
                .physicalState(physicalState)
                // Cap. 2 of Anexa 1, filled the way the model fills it: stored in plastic
                // containers, hauled by ordinary vehicle, recovery going to an authorised operator
                // and disposal to the municipal landfill.
                .storageType(StorageType.RP)
                .transportMeans(operationCode == null ? null : TransportMeans.AN)
                .wasteDestination(operationCode == null ? null
                        : operationCode.isRecovery() ? WasteDestination.Vr : WasteDestination.DO)
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

    private InternalGenerator internalGenerator(Company company, WorkPoint workPoint,
                                                String name, String description) {
        return InternalGenerator.builder()
                .company(company)
                .workPoint(workPoint)
                .name(name)
                .description(description)
                .active(true)
                .createdAt(Instant.now())
                .build();
    }

    private Partner partner(Company company, String name, String cui, String authNumber,
                            LocalDate authExpiry, PartnerType type,
                            boolean client, boolean supplier) {
        return Partner.builder()
                .company(company)
                .name(name)
                .cui(cui)
                .authorizationNumber(authNumber)
                .authorizationExpiry(authExpiry)
                .type(type)
                .client(client)
                .supplier(supplier)
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

    /**
     * Parola cu care se creează conturile demo.
     *
     * <p>Din {@code DEMO_PASSWORD} când e setată — aşa rulează suita de interfaţă, care are nevoie
     * de una ştiută dinainte. Altfel una aleatoare, generată la pornire şi scrisă în log: profilul
     * `dev` trebuie să pornească fără nicio pregătire, dar nu cu o parolă pe care o ştie toată
     * lumea care a citit repo-ul.
     */
    private String resolveDemoPassword() {
        if (configuredPassword != null && !configuredPassword.isBlank()) {
            return configuredPassword;
        }
        byte[] bytes = new byte[12];
        new SecureRandom().nextBytes(bytes);
        String generated = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        log.warn("DEMO_PASSWORD nu e setată — parola conturilor demo pe pornirea asta: {}", generated);
        return generated;
    }
}
