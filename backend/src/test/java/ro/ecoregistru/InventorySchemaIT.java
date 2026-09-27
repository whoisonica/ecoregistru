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
 * D3.5 — constrângerile din V74, probate direct în bază: o singură notă de preluare confirmată și un singur inventar
 * activ pe depozit, liniile de stoc legate de documentul lor, kg pozitiv pe nota de preluare, gestionarul primitor doar
 * la predare-primire. {@link #validDocumentsAreAccepted()} e controlul pozitiv.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
@Transactional
class InventorySchemaIT {

    @Autowired JdbcTemplate jdbc;

    UUID companyId;
    UUID workPointId;

    @BeforeEach
    void setUp() {
        companyId = jdbc.queryForObject(
                "select c.id from companies c join app_users u on u.company_id = c.id where u.email = 'admin@demo.ro'",
                UUID.class);
        workPointId = jdbc.queryForObject(
                "select id from work_points where company_id = ? limit 1", UUID.class, companyId);
    }

    @Test
    void validDocumentsAreAccepted() {
        UUID opening = opening(1, "CONFIRMED");
        openingLine(opening, "500");
        UUID inventory = inventory(1, "OPEN", "ANNUAL", null);
        movement("OPENING_BALANCE", opening, null);
        movement("INVENTORY_SURPLUS", null, inventory);

        assertThat(jdbc.queryForObject("select count(*) from waste_movements where company_id = ? "
                + "and operation in ('OPENING_BALANCE', 'INVENTORY_SURPLUS')", Integer.class, companyId)).isEqualTo(2);
    }

    @Test
    void oneConfirmedOpeningPerDepot() {
        opening(1, "CONFIRMED");
        opening(2, "DRAFT");
        assertRefused(() -> opening(3, "CONFIRMED"), "uq_stock_openings_confirmed");
    }

    @Test
    void oneActiveInventoryPerDepot() {
        inventory(1, "APPROVED", "ANNUAL", null);
        inventory(2, "OPEN", "ANNUAL", null);
        assertRefused(() -> inventory(3, "CLOSED", "ANNUAL", null), "uq_inventories_active");
    }

    @Test
    void adjustmentNeedsInventory() {
        assertRefused(() -> movement("INVENTORY_SURPLUS", null, null), "waste_movements_inventory_link");
    }

    @Test
    void openingBalanceNeedsOpening() {
        assertRefused(() -> movement("OPENING_BALANCE", null, null), "waste_movements_opening_link");
    }

    @Test
    void anOrdinaryLineCannotPointToAnInventory() {
        UUID inventory = inventory(1, "OPEN", "ANNUAL", null);
        assertRefused(() -> movement("COLLECTED", null, inventory), "waste_movements_inventory_link");
    }

    @Test
    void openingLineKgPositive() {
        UUID opening = opening(1, "DRAFT");
        assertRefused(() -> openingLine(opening, "0"), "stock_opening_lines_kg");
    }

    @Test
    void receivingKeeperOnlyOnHandover() {
        assertRefused(() -> inventory(1, "OPEN", "ANNUAL", "Ion Primitor"), "inventories_receiving_keeper");
    }

    @Test
    void receivingKeeperAcceptedOnHandover() {
        inventory(1, "OPEN", "HANDOVER", "Ion Primitor");
        assertThat(jdbc.queryForObject("select count(*) from inventories where company_id = ?", Integer.class,
                companyId)).isEqualTo(1);
    }

    // --- helpers ---

    private void assertRefused(Runnable insert, String constraint) {
        assertThatThrownBy(insert::run)
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(constraint);
    }

    private UUID opening(int number, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into stock_openings (id, company_id, work_point_id, number, cut_off_date, source, status,
                                            confirmed_on, created_at, updated_at, version)
                values (?, ?, ?, ?, current_date, 'STOCK_CARDS', ?, case when ? then current_date end, now(), now(), 0)
                """, id, companyId, workPointId, number, status, "CONFIRMED".equals(status));
        return id;
    }

    private void openingLine(UUID opening, String kg) {
        jdbc.update("""
                insert into stock_opening_lines (id, opening_id, line_no, waste_code_id, kg)
                values (?, ?, 1, (select id from waste_codes where code like '15 01 01%' limit 1), ?::numeric)
                """, UUID.randomUUID(), opening, kg);
    }

    private UUID inventory(int number, String status, String kind, String receivingKeeper) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into inventories (id, company_id, work_point_id, number, kind, starts_on, ends_on, keeper_name,
                                         receiving_keeper_name, status, closed_on, approved_on,
                                         created_at, updated_at, version)
                values (?, ?, ?, ?, ?, current_date, current_date, 'Gestionar', ?, ?,
                        case when ? then current_date end, case when ? then current_date end, now(), now(), 0)
                """, id, companyId, workPointId, number, kind, receivingKeeper, status,
                !"OPEN".equals(status), "APPROVED".equals(status));
        return id;
    }

    private void movement(String operation, UUID opening, UUID inventory) {
        jdbc.update("""
                insert into waste_movements
                    (id, company_id, work_point_id, date, waste_code_id, quantity, weighed_at_unloading, unit,
                     operation, register, deleted, created_by, created_at, updated_at, stock_opening_id, inventory_id)
                values (?, ?, ?, current_date, (select id from waste_codes where code like '15 01 01%' limit 1),
                        10, false, 'KG', ?, 'ART_48', false, ?, now(), now(), ?, ?)
                """, UUID.randomUUID(), companyId, workPointId, operation, UUID.randomUUID(), opening, inventory);
    }
}
