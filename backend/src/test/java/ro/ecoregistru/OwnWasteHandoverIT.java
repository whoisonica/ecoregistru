package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;

import java.time.Instant;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BUG-023 — regulile predării de deșeu propriu, pe server. Până la api v108 stăteau numai în formularul web,
 * deci importul, aplicația mobilă și orice apel direct le ocoleau. Numai pe Anexa 1 (generatorul): ieșirea de
 * marfă preluată (art. 48) rămâne cum era.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class OwnWasteHandoverIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired WasteMovementRepository movementRepository;

    private String token;
    private UUID companyId;
    private UUID workPointId;
    private UUID codeId;
    private UUID authorized;
    private UUID unauthorized;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Predare " + suffix + " SRL").cui("RO" + suffix).type(CompanyType.BOTH)
                .active(true).afmObligation(false).createdAt(Instant.now()).build());
        companyId = company.getId();
        workPointId = workPointRepository.save(WorkPoint.builder()
                .company(company).name("Sediu " + suffix).active(true).createdAt(Instant.now()).build()).getId();
        codeId = wasteCodeRepository.findByCode("15 01 01").orElseThrow().getId();
        authorized = partner(company, "Autorizat " + suffix, "AUT-1/2025");
        // Un partener vechi, de dinainte ca formularul de partener să ceară autorizația (16.09.2026).
        unauthorized = partner(company, "Neautorizat " + suffix, null);
        token = jwtService.generateToken(appUserRepository.save(AppUser.builder()
                .email("predare+" + suffix + "@test.ro").password("x").role(Role.ADMIN).company(company)
                .enabled(true).createdAt(Instant.now()).build()));
    }

    private UUID partner(Company company, String name, String authorization) {
        return partnerRepository.save(Partner.builder()
                .company(company).name(name).type(PartnerType.COLLECTOR).authorizationNumber(authorization)
                .client(true).active(true).createdAt(Instant.now()).build()).getId();
    }

    @Test
    void anOwnWasteHandoverWithoutADestinationIsRefused() throws Exception {
        handover("ANEXA_1", null, authorized)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("movement.destination.required")));
        assertThat(movementRepository.findAllByCompany_IdAndDeletedFalse(companyId)).isEmpty();
    }

    @Test
    void anOwnWasteHandoverToAPartnerWithoutAnAuthorizationIsRefused() throws Exception {
        handover("ANEXA_1", "Vr", unauthorized)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("movement.partner.authorization.required")));
        assertThat(movementRepository.findAllByCompany_IdAndDeletedFalse(companyId)).isEmpty();
    }

    @Test
    void anOwnWasteHandoverWithBothIsAccepted() throws Exception {
        handover("ANEXA_1", "Vr", authorized)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wasteDestination", is("Vr")));
    }

    /** Doar generatorul: ieșirea de marfă preluată nu cere nici destinație, nici autorizație (neschimbat). */
    @Test
    void anArt48ExitKeepsItsOldRules() throws Exception {
        handover("ART_48", null, unauthorized).andExpect(status().isOk());
    }

    private ResultActions handover(String register, String destination, UUID partnerId) throws Exception {
        return mockMvc.perform(post("/api/v1/movements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"workPointId": "%s", "date": "2026-07-05", "wasteCodeId": "%s", "quantity": 120,
                         "unit": "KG", "operation": "RECOVERED", "operationCode": "R3", "register": "%s",
                         "partnerId": "%s", "wasteDestination": %s}
                        """.formatted(workPointId, codeId, register, partnerId,
                        destination == null ? "null" : "\"" + destination + "\"")));
    }
}
