package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.ErrorMessageEnum;
import ro.ecoregistru.exception.ServiceUnavailableException;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.service.AnafClient;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The CUI lookup on HTTP: signed-in users only, and each of ANAF's three outcomes with its own status
 * and message — found, unknown CUI, service down. ANAF itself is mocked; its exchange is
 * {@code AnafClientTest}.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class CompanyLookupIT {

    private static final String URL = "/api/v1/company-lookup/RO12345678";

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @MockitoBean AnafClient anafClient;

    String token;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Cautare " + suffix + " SRL").cui("ROA" + suffix).type(CompanyType.GENERATOR)
                .active(true).createdAt(Instant.now()).build());
        AppUser viewer = appUserRepository.save(AppUser.builder()
                .email("anaf+" + suffix + "@demo.ro").password("x")
                .role(Role.CLIENT_VIEWER).company(company).enabled(true).createdAt(Instant.now()).build());
        token = "Bearer " + jwtService.generateToken(viewer);
    }

    @Test
    void withoutASessionAnafIsNeverAsked() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        verify(anafClient, never()).lookup(anyString());
    }

    @Test
    void aFoundCompanyComesBackForTheForm() throws Exception {
        when(anafClient.lookup("RO12345678")).thenReturn(Optional.of(new AnafClient.Company(
                "12345678", "EXEMPLU COLECT SRL", "JUD. CLUJ, MUN. CLUJ-NAPOCA, STR. EXEMPLULUI, NR.1",
                "J12/1351/2011", "3832", "CLUJ", "Mun. Cluj-Napoca", "INREGISTRAT", true)));

        mockMvc.perform(get(URL).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("EXEMPLU COLECT SRL"))
                .andExpect(jsonPath("$.tradeRegisterNumber").value("J12/1351/2011"))
                .andExpect(jsonPath("$.inactive").value(true));
    }

    @Test
    void aCuiAnafDoesNotKnowIs404WithItsOwnMessage() throws Exception {
        when(anafClient.lookup("RO12345678")).thenReturn(Optional.empty());

        mockMvc.perform(get(URL).header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$['error-code']").value("anaf.cui.not.found"));
    }

    @Test
    void anafDownIs503NotA500() throws Exception {
        when(anafClient.lookup("RO12345678"))
                .thenThrow(new ServiceUnavailableException(ErrorMessageEnum.ANAF_UNAVAILABLE));

        mockMvc.perform(get(URL).header("Authorization", token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$['error-code']").value("anaf.unavailable"));
    }

    @Test
    void textThatCannotBeACuiIs400() throws Exception {
        when(anafClient.lookup("abc")).thenThrow(new BusinessException(ErrorMessageEnum.INVALID_CUI));

        mockMvc.perform(get("/api/v1/company-lookup/abc").header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']").value("company.cui.invalid"));
    }
}
