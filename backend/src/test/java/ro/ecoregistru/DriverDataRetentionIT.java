package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Driver;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.DriverRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.service.DriverDataRetentionScheduler;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AO, 14.09.2026 — datele şoferului, sub GDPR. Două reguli: fişa unui şofer al nostru se şterge
 * definitiv numai după dezactivare, iar de pe mişcări numele şi actul lui pleacă după trei ani
 * calendaristici întregi de la încheierea anului (OUG 92/2021 art. 48 alin. (5)).
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class DriverDataRetentionIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired DriverRepository driverRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired DriverDataRetentionScheduler scheduler;

    private AppUser admin;
    private UUID tenantId;
    private String token;

    @BeforeEach
    void setUp() {
        admin = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        tenantId = admin.getCompany().getId();
        token = jwtService.generateToken(admin);
    }

    /**
     * Graniţa e 1 ianuarie: pe 1 iunie 2026, o mişcare din 31.12.2022 are trei ani întregi în urmă
     * (2023–2025) şi îşi pierde numele şi actul şoferului; una din 01.01.2023 nu, încă. Numărul
     * maşinii rămâne pe amândouă.
     */
    @Test
    void theDriversNameAndIdLeaveAMovementOnlyAfterThreeFullYears() {
        WasteMovement old = movement(LocalDate.of(2022, 12, 31));
        WasteMovement kept = movement(LocalDate.of(2023, 1, 1));

        int cleared = scheduler.purge(LocalDate.of(2026, 6, 1));

        assertThat(cleared).isPositive();
        WasteMovement oldAfter = movementRepository.findById(old.getId()).orElseThrow();
        assertThat(oldAfter.getDriverName()).isNull();
        assertThat(oldAfter.getDriverIdentification()).isNull();
        assertThat(oldAfter.getDriverCnp()).isNull();
        assertThat(oldAfter.getVehicleRegistration()).isEqualTo("CJ 01 ABC");
        WasteMovement keptAfter = movementRepository.findById(kept.getId()).orElseThrow();
        assertThat(keptAfter.getDriverName()).isEqualTo("Ion Popescu");
        assertThat(keptAfter.getDriverIdentification()).isEqualTo("CJ 123456");
        assertThat(keptAfter.getDriverCnp()).isEqualTo("1900101123457");
    }

    /** Un şofer activ nu se şterge; dezactivat, da — şi dispare de tot. */
    @Test
    void onlyADeactivatedDriverIsDeletedForGood() throws Exception {
        Driver driver = driverRepository.save(Driver.builder()
                .company(companyRepository.getReferenceById(tenantId))
                .name("Sofer De Sters " + UUID.randomUUID().toString().substring(0, 6))
                .identification("CJ 999999").active(true).createdAt(Instant.now()).build());

        mockMvc.perform(delete("/api/v1/drivers/" + driver.getId() + "/definitiv")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("driver.delete.requires.deactivation")));
        assertThat(driverRepository.findById(driver.getId())).isPresent();

        driver.setActive(false);
        driverRepository.save(driver);
        mockMvc.perform(delete("/api/v1/drivers/" + driver.getId() + "/definitiv")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        assertThat(driverRepository.findById(driver.getId())).isEmpty();
    }

    /** Şoferul unui transportator se editează în fişa partenerului, nu se şterge de aici. */
    @Test
    void aCarriersDriverIsNotDeletedFromHere() throws Exception {
        Driver driver = driverRepository.save(Driver.builder()
                .company(companyRepository.getReferenceById(tenantId))
                .partner(partnerRepository.findAllByCompany_Id(tenantId).get(0))
                .name("Sofer Transportator").identification("CJ 111111")
                .active(false).createdAt(Instant.now()).build());
        try {
            mockMvc.perform(delete("/api/v1/drivers/" + driver.getId() + "/definitiv")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$['error-code']", is("driver.belongs.to.partner")));
            assertThat(driverRepository.findById(driver.getId())).isPresent();
        } finally {
            driverRepository.deleteById(driver.getId());
        }
    }

    private WasteMovement movement(LocalDate date) {
        Company company = companyRepository.getReferenceById(tenantId);
        return movementRepository.save(WasteMovement.builder()
                .company(company)
                .workPoint(workPointRepository.findAllByCompany_Id(tenantId).get(0))
                .date(date)
                .wasteCode(wasteCodeRepository.findByCode("20 01 01").orElseThrow())
                .quantity(new BigDecimal("10.000")).unit(Unit.KG).operation(WasteOperation.GENERATED)
                .driverName("Ion Popescu").driverIdentification("CJ 123456")
                .driverCnp("1900101123457")
                .vehicleRegistration("CJ 01 ABC")
                .deleted(false).createdBy(admin.getId()).build());
    }
}
