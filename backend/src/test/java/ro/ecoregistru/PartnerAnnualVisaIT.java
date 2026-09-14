package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.service.PartnerAuthorizationAlertScheduler;
import ro.ecoregistru.service.notification.NotificationService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * V41 — viza anuală a autorizaţiei de mediu a partenerului (AH, 14.09.2026).
 *
 * <p>OUG 195/2005 art. 16 alin. (2^1): autorizaţia rămâne valabilă cât se obţine viza anuală, deci
 * la o autorizaţie de azi data care contează e sfârşitul perioadei vizei, nu o expirare. Legea
 * 219/2019 art. II alin. (3) mai lasă o expirare autorizaţiilor vechi nemodificate, şi atunci
 * decide data care vine prima. Temeiul, citat: {@code docs/surse-oficiale.md} §2.6-bis.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class PartnerAnnualVisaIT {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 1);

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired PartnerAuthorizationAlertScheduler scheduler;

    @MockBean NotificationService notificationService;

    private String token;
    private UUID workPointId;
    private UUID wasteCodeId;
    private UUID partnerId;

    @BeforeEach
    void setUp() {
        AppUser admin = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        token = jwtService.generateToken(admin);
        UUID tenantId = admin.getCompany().getId();
        workPointId = workPointRepository.findAllByCompany_Id(tenantId).get(0).getId();
        wasteCodeId = wasteCodeRepository.findByCode("20 01 01").orElseThrow().getId();
        partnerId = partnerRepository.findAllByCompany_Id(tenantId).get(0).getId();
    }

    /**
     * Cazul pentru care există felia: autorizaţia n-are dată de expirare, dar perioada vizei s-a
     * încheiat înainte de predare. Fără V41, avertismentul ar fi tăcut.
     */
    @Test
    void aHandoverAfterTheVisaPeriodIsFlaggedWithoutAnyExpiryDate() throws Exception {
        withPartnerDates(null, LocalDate.of(2026, 6, 30), () ->
                mockMvc.perform(handover()) // mişcarea e din 05.07.2026
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.recipientAuthorizationExpired", is(true)))
                        .andExpect(jsonPath("$.recipientAuthorizationExpiry", is("2026-06-30"))));
    }

    /** Amândouă datele cunoscute: decide cea care vine prima, în oricare ordine ar fi. */
    @Test
    void theEarlierOfExpiryAndVisaDecides() throws Exception {
        withPartnerDates(LocalDate.of(2026, 12, 31), LocalDate.of(2026, 6, 30), () ->
                mockMvc.perform(handover())
                        .andExpect(jsonPath("$.recipientAuthorizationExpired", is(true)))
                        .andExpect(jsonPath("$.recipientAuthorizationExpiry", is("2026-06-30"))));
        withPartnerDates(LocalDate.of(2026, 6, 30), LocalDate.of(2026, 12, 31), () ->
                mockMvc.perform(handover())
                        .andExpect(jsonPath("$.recipientAuthorizationExpired", is(true)))
                        .andExpect(jsonPath("$.recipientAuthorizationExpiry", is("2026-06-30"))));
    }

    /** O viză care acoperă ziua predării nu e o constatare. */
    @Test
    void aVisaCoveringTheHandoverDayIsNotFlagged() throws Exception {
        withPartnerDates(null, LocalDate.of(2026, 7, 5), () ->
                mockMvc.perform(handover())
                        .andExpect(jsonPath("$.recipientAuthorizationExpired", is(false))));
    }

    /** Alerta de 60 de zile pleacă şi pentru sfârşitul vizei, nu doar pentru o expirare. */
    @Test
    void theSixtyDayWarningCoversTheEndOfTheVisa() {
        Company c = companyWithUser();
        Partner visaOnly = partner(c, null, TODAY.plusDays(30));
        // Expirarea e în fereastră, dar viza s-a încheiat ieri: autorizaţia e deja depăşită, iar un
        // mail „expiră în 30 de zile" ar spune o dată care nu mai contează.
        Partner visaLapsed = partner(c, TODAY.plusDays(30), TODAY.minusDays(1));

        scheduler.dispatchWarnings(TODAY);

        assertThat(partnerRepository.findById(visaOnly.getId()).orElseThrow()
                .getAuthorizationWarningSentFor()).isEqualTo(TODAY.plusDays(30));
        assertThat(partnerRepository.findById(visaLapsed.getId()).orElseThrow()
                .getAuthorizationWarningSentFor()).isNull();
    }

    // ---------- helpers ----------

    private interface Check {
        void run() throws Exception;
    }

    private void withPartnerDates(LocalDate expiry, LocalDate visaValidUntil, Check check)
            throws Exception {
        Partner p = partnerRepository.findById(partnerId).orElseThrow();
        LocalDate oldExpiry = p.getAuthorizationExpiry();
        LocalDate oldVisa = p.getVisaValidUntil();
        try {
            p.setAuthorizationExpiry(expiry);
            p.setVisaValidUntil(visaValidUntil);
            partnerRepository.save(p);
            check.run();
        } finally {
            Partner back = partnerRepository.findById(partnerId).orElseThrow();
            back.setAuthorizationExpiry(oldExpiry);
            back.setVisaValidUntil(oldVisa);
            partnerRepository.save(back);
        }
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder handover() {
        return post("/api/v1/movements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "workPointId": "%s",
                          "date": "2026-07-05",
                          "wasteCodeId": "%s",
                          "unit": "KG",
                          "physicalState": "SOLID",
                          "operation": "RECOVERED", "register": "ANEXA_1", "operationCode": "R13",
                          "partnerId": "%s", "quantity": 100
                        }
                        """.formatted(workPointId, wasteCodeId, partnerId));
    }

    private Company companyWithUser() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Viza " + suffix).cui("ROV" + suffix).type(CompanyType.GENERATOR)
                .active(true).afmObligation(false).createdAt(Instant.now()).build());
        appUserRepository.save(AppUser.builder()
                .email("viza+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        return company;
    }

    private Partner partner(Company c, LocalDate expiry, LocalDate visaValidUntil) {
        return partnerRepository.save(Partner.builder()
                .company(c).name("Colector " + UUID.randomUUID().toString().substring(0, 6))
                .type(PartnerType.COLLECTOR).client(true).supplier(false).carrier(false)
                .authorizationNumber("AM 7")
                .authorizationExpiry(expiry).visaValidUntil(visaValidUntil)
                .active(true).createdAt(Instant.now()).build());
    }
}
