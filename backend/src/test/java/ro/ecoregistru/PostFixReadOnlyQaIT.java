package ro.ecoregistru;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.service.CloudinaryStorageService;
import ro.ecoregistru.service.EmailService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * QA de securitate după BUG-031…044: doar-citirea la abonament (flagul aprins). {@code SubscriptionAccessFilter}
 * lasă să treacă orice GET; un GET care scrie trece deci și el.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
@TestPropertySource(properties = "app.billing.read-only-enabled=true")
class PostFixReadOnlyQaIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired SubscriptionRepository subscriptionRepository;
    @MockitoBean EmailService emailService;
    @MockitoBean CloudinaryStorageService storageService;

    /**
     * Anexa 3 se descarcă prin GET, dar la prima tipărire alocă numărul din seria firmei ({@code Anexa3Numbering}):
     * o scriere. Pe un cont doar-citire, numărul se alocă totuși.
     */
    @Test
    void aReadOnlyAccountDoesNotAllocateAnAnexa3Number() throws Exception {
        Company c = companyRepository.save(Company.builder()
                .name("Doar Citire QA SRL").cui(TestCui.random()).type(CompanyType.GENERATOR)
                .anexa3Series("QA").active(true).createdAt(Instant.now()).build());
        AppUser operator = appUserRepository.save(AppUser.builder()
                .email("op+" + UUID.randomUUID().toString().substring(0, 8) + "@qa.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.OPERATOR).company(c).enabled(true).createdAt(Instant.now()).build());
        String token = jwtService.generateToken(operator);
        UUID wp = workPointRepository.save(WorkPoint.builder()
                .company(c).name("PL").active(true).createdAt(Instant.now()).build()).getId();
        UUID partner = partnerRepository.save(Partner.builder()
                .company(c).name("Colector").authorizationNumber("AM 6/2025")
                .type(PartnerType.COLLECTOR).supplier(true).active(true).createdAt(Instant.now()).build()).getId();

        MvcResult created = mockMvc.perform(post("/api/v1/movements").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"workPointId": "%s", "date": "2026-07-05", "wasteCodeId": "%s", "quantity": 5,
                         "unit": "KG", "physicalState": "SOLID", "operation": "RECOVERED",
                         "physicalState": "SOLID", "storageType": "CT", "transportMeans": "AN", "packagingCategory": "SECONDARY", "wasteDestination": "Vr", "operationCode": "R13", "partnerId": "%s"}
                        """.formatted(wp, wasteCodeRepository.findByCode("20 01 01").orElseThrow().getId(), partner)))
                .andReturn();
        assertThat(created.getResponse().getStatus()).as(created.getResponse().getContentAsString()).isEqualTo(200);
        UUID id = UUID.fromString(objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText());

        subscriptionRepository.save(Subscription.builder()
                .company(c).plan(SubscriptionPlan.GENERATOR).status(SubscriptionStatus.READ_ONLY)
                .monthlyPrice(SubscriptionPlan.GENERATOR.monthlyPrice())
                .implementationFee(SubscriptionPlan.GENERATOR.implementationFee())
                .extraWorkPointPrice(SubscriptionPlan.EXTRA_WORK_POINT_PRICE)
                .startedAt(LocalDate.of(2026, 1, 1)).createdAt(Instant.now())
                .billingEmail("f@qa.ro").billingCounty("Cluj").billingCity("Cluj").billingAddress("Str. 1").build());

        int write = mockMvc.perform(post("/api/v1/partners").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"P\",\"type\":\"COLLECTOR\"}"))
                .andReturn().getResponse().getStatus();
        assertThat(write).as("controlul: o scriere obișnuită e refuzată").isEqualTo(403);

        int anexa3 = mockMvc.perform(get("/api/v1/movements/" + id + "/anexa3").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getStatus();
        Integer number = movementRepository.findById(id).orElseThrow().getAnexa3Number();
        System.out.println("QA read-only anexa3 → " + anexa3 + ", number=" + number);
        assertThat(number).as("numărul Anexei 3 alocat pe un cont doar-citire (GET → " + anexa3 + ")").isNull();
    }
}
