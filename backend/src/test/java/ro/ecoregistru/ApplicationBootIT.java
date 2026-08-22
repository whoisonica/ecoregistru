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
        // spans Feb–Jul 2026 across three work points (see DevDataSeeder).
        assertThat(appUserRepository.existsByEmail("platform@ecoregistru.ro")).isTrue();
        assertThat(appUserRepository.existsByEmail("admin@demo.ro")).isTrue();
        assertThat(wasteMovementRepository.count()).isEqualTo(34);
    }
}
