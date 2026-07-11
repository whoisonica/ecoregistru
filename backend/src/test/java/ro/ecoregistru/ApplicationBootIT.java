package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;

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

    @Test
    void contextLoadsAndSeedApplied() {
        // Flyway V2 seeded the 10 placeholder waste codes from the CSV.
        assertThat(wasteCodeRepository.count()).isEqualTo(10);
        // DevDataSeeder created the demo users and sample movements
        // (incl. one RECOVERED movement with an R operation code, added in FAZA M).
        assertThat(appUserRepository.existsByEmail("platform@ecoregistru.ro")).isTrue();
        assertThat(appUserRepository.existsByEmail("admin@demo.ro")).isTrue();
        assertThat(wasteMovementRepository.count()).isEqualTo(5);
    }
}
