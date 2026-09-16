package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * D1.1 — constrângerile din V46, probate direct în bază, înainte să existe vreun serviciu care să le
 * ocolească sau să le acopere. Fiecare refuz numește constrângerea, iar {@link #aValidOperationIsAccepted()}
 * e controlul pozitiv: dacă inserarea de bază ar pica din alt motiv, toate refuzurile ar ieși verzi
 * degeaba.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
@Transactional
class WeighingOperationSchemaIT {

    @Autowired JdbcTemplate jdbc;

    UUID companyId;
    UUID workPointId;
    UUID partnerId;

    @BeforeEach
    void setUp() {
        companyId = jdbc.queryForObject(
                "select c.id from companies c join app_users u on u.company_id = c.id where u.email = 'admin@demo.ro'",
                UUID.class);
        workPointId = jdbc.queryForObject(
                "select id from work_points where company_id = ? limit 1", UUID.class, companyId);
        partnerId = jdbc.queryForObject(
                "select id from partners where company_id = ? limit 1", UUID.class, companyId);
    }

    @Test
    void aValidOperationIsAccepted() {
        UUID person = person();
        insert("IN", 1, null, person, "POPULATIE", "IN_PROGRESS", null);
        insert("OUT", 1, partnerId, null, "COLECTOR", "IN_PROGRESS", null);

        assertThat(jdbc.queryForObject(
                "select count(*) from weighing_operations where company_id = ?", Integer.class, companyId))
                .isEqualTo(2);
    }

    @Test
    void aNaturalPersonCannotReceiveAnExit() {
        assertRefused(() -> insert("OUT", 1, null, person(), "POPULATIE", "IN_PROGRESS", null),
                "weighing_operations_person_only_in");
    }

    @Test
    void aNaturalPersonIsAlwaysPopulationOrigin() {
        assertRefused(() -> insert("IN", 1, null, person(), "GENERATOR_PJ", "IN_PROGRESS", null),
                "weighing_operations_person_origin");
    }

    @Test
    void aPartnerAndANaturalPersonAreNotBothTheCounterparty() {
        assertRefused(() -> insert("IN", 1, partnerId, person(), "POPULATIE", "IN_PROGRESS", null),
                "weighing_operations_one_counterparty");
    }

    @Test
    void aCancellationNeedsAReason() {
        assertRefused(() -> insert("IN", 1, partnerId, null, null, "CANCELLED", null),
                "weighing_operations_cancel_reason");
    }

    @Test
    void theNumberIsUniquePerTypeNotAcrossTypes() {
        insert("IN", 7, partnerId, null, null, "IN_PROGRESS", null);
        insert("OUT", 7, partnerId, null, null, "IN_PROGRESS", null);

        assertRefused(() -> insert("IN", 7, partnerId, null, null, "IN_PROGRESS", null),
                "uq_weighing_operations_number");
    }

    @Test
    void theFinalQuantityNeverExceedsTheNetWeight() {
        UUID operation = insert("IN", 1, partnerId, null, null, "IN_PROGRESS", null);
        insertLine(operation, "95.000", "95.000");

        assertRefused(() -> insertLine(operation, "100.500", "100.000"), "waste_movements_final_within_net");
    }

    /**
     * D1.9 și D1.10 — o sumă reținută fără baza ei ar fi o cifră fără document (V51). Fiecare refuz
     * stă în testul lui: în Postgres primul refuz avortează toată tranzacția, deci două într-una
     * n-ar mai proba nimic al doilea.
     */
    @Test
    void theAfmContributionAlwaysCarriesItsBase() {
        UUID operation = insert("IN", 1, partnerId, null, null, "IN_PROGRESS", null);
        assertRefused(() -> jdbc.update(
                        "update weighing_operations set afm_contribution = 10 where id = ?", operation),
                "weighing_operations_afm_base");
    }

    @Test
    void theIncomeTaxAlwaysCarriesItsBase() {
        UUID operation = insert("IN", 1, partnerId, null, null, "IN_PROGRESS", null);
        assertRefused(() -> jdbc.update(
                        "update weighing_operations set income_tax = 200 where id = ?", operation),
                "weighing_operations_income_tax_base");
    }

    /** Controlul pozitiv al celor două de mai sus: cu bazele scrise, aceleași sume trec. */
    @Test
    void amountsWithTheirBasesAreAccepted() {
        UUID operation = insert("IN", 1, partnerId, null, null, "IN_PROGRESS", null);

        jdbc.update("update weighing_operations set afm_base = 500, afm_contribution = 10, "
                + "income_tax_base = 2000, income_tax = 200 where id = ?", operation);

        assertThat(jdbc.queryForObject("select afm_contribution from weighing_operations where id = ?",
                java.math.BigDecimal.class, operation)).isEqualByComparingTo("10");
    }

    // --- helpers ---

    private void assertRefused(Runnable insert, String constraint) {
        assertThatThrownBy(insert::run)
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(constraint);
    }

    private UUID person() {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into natural_persons (id, company_id, name, created_at) values (?, ?, 'Ion Popescu', now())",
                id, companyId);
        return id;
    }

    private UUID insert(String type, int number, UUID partner, UUID person, String origin, String status,
                        String cancelReason) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into weighing_operations
                    (id, company_id, work_point_id, type, number, date, partner_id, natural_person_id, origin,
                     status, cancelled_at, cancel_reason, created_by, created_at, updated_at)
                values (?, ?, ?, ?, ?, current_date, ?, ?, ?, ?, ?, ?, ?, now(), now())
                """,
                id, companyId, workPointId, type, number, partner, person, origin, status,
                "CANCELLED".equals(status) ? java.sql.Timestamp.from(java.time.Instant.now()) : null,
                cancelReason, UUID.randomUUID());
        return id;
    }

    private void insertLine(UUID operation, String finalKg, String netKg) {
        jdbc.update("""
                insert into waste_movements
                    (id, company_id, work_point_id, date, waste_code_id, quantity, weighed_at_unloading, unit,
                     operation, register, deleted, created_by, created_at, updated_at, weighing_operation_id, net_kg)
                values (?, ?, ?, current_date, (select id from waste_codes where code like '17 04 01%' limit 1),
                        ?::numeric, false, 'KG', 'COLLECTED', 'ART_48', false, ?, now(), now(), ?, ?::numeric)
                """,
                UUID.randomUUID(), companyId, workPointId, finalKg, UUID.randomUUID(), operation, netKg);
    }
}
