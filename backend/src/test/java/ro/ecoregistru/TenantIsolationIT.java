package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Critical: proves cross-tenant access is impossible through the real HTTP stack
 * (JWT auth -> TenantFilter -> tenant-scoped service queries).
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class TenantIsolationIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired WasteMovementRepository movementRepository;

    private String tokenA;
    private String tokenB;
    private UUID movementA;
    private UUID movementB;

    @BeforeEach
    void setUp() {
        WasteCode code = wasteCodeRepository.findAll().get(0);

        // Tenant A already exists from the dev seeder (Demo Reciclare SRL). Grab its data.
        AppUser userA = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        tokenA = jwtService.generateToken(userA);
        movementA = movementRepository.findAllByCompany_IdAndDeletedFalse(userA.getCompany().getId())
                .get(0).getId();

        // Create a fresh Tenant B with its own user + work point + movement.
        // Unique cui/email per invocation (the embedded DB is shared across test methods).
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company companyB = companyRepository.save(Company.builder()
                .name("Tenant B SRL").cui("ROB" + suffix).type(CompanyType.GENERATOR)
                .active(true).createdAt(Instant.now()).build());
        AppUser userB = appUserRepository.save(AppUser.builder()
                .email("admin+" + suffix + "@tenantb.ro").password(passwordEncoder.encode("Parola123"))
                .role(Role.ADMIN).company(companyB).enabled(true).createdAt(Instant.now()).build());
        tokenB = jwtService.generateToken(userB);
        WorkPoint wpB = workPointRepository.save(WorkPoint.builder()
                .company(companyB).name("PL B").active(true).createdAt(Instant.now()).build());
        movementB = movementRepository.save(WasteMovement.builder()
                .company(companyB).workPoint(wpB).date(LocalDate.now()).wasteCode(code)
                .quantity(new BigDecimal("10.000")).unit(Unit.KG).operation(WasteOperation.GENERATED)
                .deleted(false).createdBy(userB.getId()).build()).getId();
    }

    @Test
    void userCannotReadAnotherTenantsMovementById() throws Exception {
        // A cannot see B's movement
        mockMvc.perform(get("/api/v1/movements/" + movementB).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
        // B cannot see A's movement
        mockMvc.perform(get("/api/v1/movements/" + movementA).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownMovementIsAccessible() throws Exception {
        mockMvc.perform(get("/api/v1/movements/" + movementA).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/movements/" + movementB).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());
    }

    @Test
    void listNeverLeaksAnotherTenantsRecords() throws Exception {
        // B's list must not contain A's movement id, and vice versa.
        mockMvc.perform(get("/api/v1/movements").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(movementA.toString()))));
        mockMvc.perform(get("/api/v1/movements").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(movementB.toString()))));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/movements")).andExpect(status().isForbidden());
    }
}
