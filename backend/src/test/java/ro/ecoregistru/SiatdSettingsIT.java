package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.SiatdModule;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.CompanyService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ro.ecoregistru.enums.SiatdModule.MUNICIPAL;
import static ro.ecoregistru.enums.SiatdModule.PACKAGING;

/**
 * F6a — modulele SIATD în care e înrolată firma, fiecare cu data înrolării („înrolat din”). O dată nenulă înseamnă
 * „bifat”; ce lipsește din cerere se debifează. O schimbă doar adminul firmei, ca vizibilitatea prețurilor: data
 * înrolării o știe doar firma, iar de ea atârnă termenele pe care le vede toată lumea.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class SiatdSettingsIT {

    private static final String URL = "/api/v1/companies/current/siatd";

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired CompanyService companyService;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;

    Company company;
    Map<Role, AppUser> users;

    @BeforeEach
    void setUp() {
        company = companyRepository.save(Company.builder()
                .name("SIATD " + suffix() + " SRL").cui("RO" + TestCui.random())
                .type(CompanyType.COLLECTOR).active(true).createdAt(Instant.now()).build());
        users = Map.of(
                Role.ADMIN, user(Role.ADMIN),
                Role.OPERATOR, user(Role.OPERATOR),
                Role.CONSULTANT, unsaved(Role.CONSULTANT),
                Role.PLATFORM_ADMIN, unsaved(Role.PLATFORM_ADMIN));
        actAs(Role.ADMIN);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminSetsTheModules() {
        var saved = companyService.updateSiatd(Map.of(
                PACKAGING, LocalDate.of(2024, 1, 1), MUNICIPAL, LocalDate.of(2025, 6, 5)));
        assertThat(saved.siatdEnrolledFrom()).containsOnly(
                Map.entry(PACKAGING, LocalDate.of(2024, 1, 1)), Map.entry(MUNICIPAL, LocalDate.of(2025, 6, 5)));
        assertThat(stored()).containsOnlyKeys(PACKAGING, MUNICIPAL);

        var again = companyService.updateSiatd(Map.of(PACKAGING, LocalDate.of(2024, 1, 1)));
        assertThat(again.siatdEnrolledFrom()).containsOnlyKeys(PACKAGING);
        assertThat(stored()).as("ce lipsește din cerere se debifează").containsOnlyKeys(PACKAGING);
        assertThat(companyService.current().siatdEnrolledFrom()).containsOnlyKeys(PACKAGING);
    }

    @Test
    void operatorAndConsultantAreRefused() throws Exception {
        for (Role role : List.of(Role.OPERATOR, Role.CONSULTANT, Role.PLATFORM_ADMIN)) {
            actAs(role);
            assertThatThrownBy(() -> companyService.updateSiatd(Map.of(PACKAGING, LocalDate.of(2024, 1, 1))))
                    .as(role.name()).isInstanceOf(AccessDeniedException.class);
        }
        assertThat(stored()).isEmpty();

        // Autentificarea de pe thread ar opri filtrul JWT, iar 403-ul n-ar dovedi nimic (capcana din D1.5).
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        mockMvc.perform(put(URL).header("Authorization", "Bearer " + jwtService.generateToken(users.get(Role.OPERATOR)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enrolledFrom\":{\"PACKAGING\":\"2024-01-01\"}}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(URL).header("Authorization", "Bearer " + jwtService.generateToken(users.get(Role.ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enrolledFrom\":{\"PACKAGING\":\"2024-01-01\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siatdEnrolledFrom.PACKAGING").value("2024-01-01"));
        assertThat(stored()).containsOnlyKeys(PACKAGING);
    }

    /** O înrolare planificată — firma știe de pe acum data de la care intră în modul. */
    @Test
    void futureDateIsAccepted() {
        LocalDate next = LocalDate.of(2027, 1, 1);
        assertThat(companyService.updateSiatd(Map.of(SiatdModule.WEEE, next)).siatdEnrolledFrom())
                .containsEntry(SiatdModule.WEEE, next);
    }

    @Test
    void nullMapIsRefused() throws Exception {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        mockMvc.perform(put(URL).header("Authorization", "Bearer " + jwtService.generateToken(users.get(Role.ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']").value("siatd.settings.required"));
    }

    private Map<SiatdModule, LocalDate> stored() {
        return companyRepository.findById(company.getId()).orElseThrow().siatdEnrolment();
    }

    private AppUser user(Role role) {
        return appUserRepository.save(AppUser.builder()
                .email(role.name().toLowerCase() + "+" + suffix() + "@demo.ro").password("x")
                .role(role).company(company).enabled(true).createdAt(Instant.now()).build());
    }

    private static AppUser unsaved(Role role) {
        return AppUser.builder().id(UUID.randomUUID()).email(role.name().toLowerCase() + "@demo.ro")
                .role(role).enabled(true).build();
    }

    private void actAs(Role role) {
        TenantContext.set(company.getId());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(users.get(role), null, List.of()));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
