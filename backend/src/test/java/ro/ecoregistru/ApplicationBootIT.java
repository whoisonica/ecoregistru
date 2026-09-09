package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.AttachmentRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;

import java.time.LocalDate;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the whole Spring context against a REAL embedded Postgres:
 * runs all Flyway migrations (incl. the Java waste-code seed) and the dev seeder,
 * validating that the schema, entity mappings, and seed data are consistent.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class ApplicationBootIT {

    @Autowired
    WasteCodeRepository wasteCodeRepository;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    WasteMovementRepository wasteMovementRepository;

    @Autowired
    CompanyRepository companyRepository;

    @Autowired
    AttachmentRepository attachmentRepository;

    @Autowired
    PartnerRepository partnerRepository;

    @Test
    void contextLoadsAndSeedApplied() {
        // Flyway V4 reloaded the full European List of Waste over V2's 10 placeholders.
        assertThat(wasteCodeRepository.count()).isEqualTo(842);
        // 13 02 08 is both a V2 placeholder (so V4's ON CONFLICT DO UPDATE had to overwrite
        // the hand-written name) and a code whose official name contains a comma (so the
        // line has to be split on its first and last comma, not blindly on every comma).
        assertThat(wasteCodeRepository.findByCode("13 02 08"))
                .get()
                .satisfies(code -> {
                    assertThat(code.getName()).isEqualTo("alte uleiuri de motor, de transmisie și de ungere");
                    assertThat(code.isHazardous()).isTrue();
                });
        // DevDataSeeder created the demo users and sample movements: the rich demo dataset
        // spans Feb–Jul 2026 across three work points (see DevDataSeeder). Scoped to the demo
        // tenant on purpose: the embedded database is shared with the other test classes, whose
        // fixtures would otherwise make this count depend on the order they run in.
        assertThat(appUserRepository.existsByEmail("platform@ecoregistru.ro")).isTrue();
        assertThat(appUserRepository.existsByEmail("admin@demo.ro")).isTrue();
        UUID demoTenantId = companyRepository.findAll().stream()
                .filter(c -> "Demo Reciclare SRL".equals(c.getName()))
                .findFirst().orElseThrow().getId();
        assertThat(wasteMovementRepository.findAllByCompany_IdAndDeletedFalse(demoTenantId)).hasSize(37);

        /*
         * Cele două rânduri adăugate pe 07.09.2026, și de ce sunt numărate pe nume, nu doar în
         * total: seed-ul avea **zero** mișcări în stările pentru care ecranele au reguli proprii,
         * deci fiecare probă scrisă în jurul lor trecea pe gol. Un total care crește nu spune că
         * stările există; astea două o spun.
         */
        var demoMovements = wasteMovementRepository.findAllByCompany_IdAndDeletedFalse(demoTenantId);
        assertThat(demoMovements)
                .as("o ieșire fără cod R/D — badge roșu, cantitate care nu intră în nicio coloană")
                .anySatisfy(m -> {
                    assertThat(m.getOperation()).isEqualTo(WasteOperation.UNCLASSIFIED_OUT);
                    assertThat(m.getOperationCode()).isNull();
                });
        assertThat(demoMovements)
                .as("o predare care așteaptă cântarul — badge galben, cantitate nespusă")
                .anySatisfy(m -> {
                    assertThat(m.isWeighedAtUnloading()).isTrue();
                    assertThat(m.getQuantity()).isNull();
                });
        /*
         * A patra stare, mutată aici pe 09.09.2026 din `INSERT`-ul aditiv care trăia în
         * `frontend/e2e/README.md`: o predare către un partener a cărui autorizație expirase
         * **înainte** de data predării. E starea pe care o semnalează badge-ul „Autorizație
         * expirată", iar de pe 08.09 badge-ul duce la fișa partenerului — un drum care se proba pe
         * gol cât timp rândul exista numai pe baza unei mașini.
         *
         * Numărată pe nume, ca celelalte: comparația e între data mișcării și expirarea
         * partenerului, deci un rând cu partenerul potrivit dar cu data greșită n-ar aprinde
         * nimic și ar trece la fel de tăcut ca lipsa lui.
         */
        // Partenerul se citește **întreg**, din repository, nu prin `m.getPartner()`: asocierea e
        // leneșă, iar în afara unei tranzacții citirea unui câmp de pe proxy aruncă
        // `LazyInitializationException` — cum a și aruncat prima variantă a verificării ăsteia.
        // Din proxy se poate lua doar id-ul, fără să se încarce nimic; pe el se face potrivirea.
        var expiredPartner = partnerRepository.findAll().stream()
                .filter(p -> "Salubritate Municipală SA".equals(p.getName()))
                .findFirst().orElseThrow();
        LocalDate expiry = expiredPartner.getAuthorizationExpiry();
        assertThat(expiry)
                .as("partenerul demo cu autorizația căzută are chiar o dată, și e în trecut")
                .isNotNull()
                .isBefore(LocalDate.now());
        assertThat(demoMovements)
                .as("o predare către un partener cu autorizația deja expirată la data ei")
                .anySatisfy(m -> {
                    assertThat(m.getPartner()).isNotNull();
                    assertThat(m.getPartner().getId()).isEqualTo(expiredPartner.getId());
                    assertThat(expiry).isBefore(m.getDate());
                });

        /*
         * A treia stare, adăugată pe 08.09.2026: mișcarea cu documente atașate. Coloana „📎" avea
         * zero rânduri din 36, deci vederea care le deschide se proba pe gol — aceeași datorie ca
         * cele două de mai sus. **Două** atașamente, nu unul: cu unul singur nu s-ar vedea dacă
         * lista chiar le enumeră sau tipărește primul de două ori.
         */
        UUID noCodeId = demoMovements.stream()
                .filter(m -> m.getOperation() == WasteOperation.UNCLASSIFIED_OUT)
                .findFirst().orElseThrow().getId();
        // `findAll` + filtru, nu o metodă nouă de repository: baza încorporată e împărțită cu
        // celelalte clase de test, deci se numără atașamentele mișcării ăsteia, nu toate.
        var attachments = attachmentRepository.findAll().stream()
                .filter(a -> a.getMovement().getId().equals(noCodeId))
                .toList();
        assertThat(attachments)
                .as("două atașamente pe ieșirea fără cod R/D — rândul după care întreabă inspectorul")
                .hasSize(2)
                .allSatisfy(a -> {
                    assertThat(a.getFileName()).isNotBlank();
                    assertThat(a.getUrl()).startsWith("https://");
                });
    }
}
