package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ETAPA 2a — the register seam. Two legal evidences share one table, and these are the rules that
 * keep them apart:
 *
 * <ul>
 *   <li>waste taken over from third parties never reaches Anexa 1 (HG 856/2002 art. 2 alin. (1));</li>
 *   <li>waste generated in the company own activity never leaves it (art. 1 alin. (1));</li>
 *   <li>a handover has to name the operation the recipient performs, because Anexa 1 cap. 1 has no
 *       "handed over" column and the quantity is reported in cap. 3 or cap. 4 together with
 *       "Operaţia de valorificare"/"de eliminare" — the code is also what places it in
 *       "valorificată" or in "eliminată final";</li>
 *   <li>only a company that actually takes waste over keeps an art. 48 register.</li>
 * </ul>
 *
 * Verbatim sources: docs/surse-oficiale.md §1.1, §1.2, §2.1. Driven through the real HTTP stack so
 * the controller → service → AdviceController envelope is covered too.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class RegisterSeamIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired ReceptionRepository receptionRepository;
    @Autowired DeliveryRepository deliveryRepository;

    /** The seeded demo tenant, type BOTH: it both generates and takes waste over. */
    private String collectorToken;
    private UUID workPointId;
    private UUID wasteCodeId;
    private UUID partnerId;

    /** A fresh tenant of type GENERATOR: Anexa 1 only, no art. 48 register. */
    private String generatorToken;
    private Company generator;
    private UUID generatorWorkPointId;

    @BeforeEach
    void setUp() {
        AppUser admin = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        collectorToken = jwtService.generateToken(admin);
        UUID demoTenant = admin.getCompany().getId();
        WasteMovement seeded = movementRepository.findAllByCompany_IdAndDeletedFalse(demoTenant).get(0);
        workPointId = seeded.getWorkPoint().getId();
        wasteCodeId = seeded.getWasteCode().getId();
        partnerId = partnerRepository.findAllByCompany_Id(demoTenant).get(0).getId();

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        generator = companyRepository.save(Company.builder()
                .name("Generator Pur SRL").cui("ROG" + suffix).type(CompanyType.GENERATOR)
                .active(true).createdAt(Instant.now()).build());
        AppUser generatorAdmin = appUserRepository.save(AppUser.builder()
                .email("admin+" + suffix + "@generator.ro").password(passwordEncoder.encode("Parola123"))
                .role(Role.ADMIN).company(generator).enabled(true).createdAt(Instant.now()).build());
        generatorToken = jwtService.generateToken(generatorAdmin);
        generatorWorkPointId = workPointRepository.save(WorkPoint.builder()
                .company(generator).name("PL Generator").active(true).createdAt(Instant.now())
                .build()).getId();
    }

    // ---------- Handing waste over is an R/D operation performed by a partner ----------

    /**
     * "Predare" is not an operation. Anexa 1 cap. 1 has no such column, and cap. 3 / cap. 4 report
     * a quantity with its R/D operation AND the operator who performed it — so the partner says it
     * was handed over, and the code says what happens to it. The enum constant no longer exists,
     * and a client still sending it gets a 400 rather than a movement in a column the form lacks.
     */
    @Test
    void handedOverIsNoLongerAnOperation() throws Exception {
        mockMvc.perform(movement(collectorToken, workPointId,
                        "  \"operation\": \"HANDED_OVER\", \"partnerId\": \"" + partnerId
                                + "\", \"operationCode\": \"R13\""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anExitWithoutAnOperationCodeIsRejected() throws Exception {
        mockMvc.perform(movement(collectorToken, workPointId,
                        "  \"operation\": \"RECOVERED\", \"partnerId\": \"" + partnerId + "\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("movement.operation.code.recovery")));
    }

    /** The ordinary case: own waste handed to a collector who stores it pending recovery. */
    @Test
    void recoveryByAPartnerLandsInTheValorificataColumn() throws Exception {
        mockMvc.perform(movement(collectorToken, workPointId,
                        "  \"operation\": \"RECOVERED\", \"partnerId\": \"" + partnerId
                                + "\", \"operationCode\": \"R13\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.register", is("ANEXA_1")))
                .andExpect(jsonPath("$.partnerId", is(partnerId.toString())))
                .andExpect(jsonPath("$.operationCode", is("R13")))
                .andExpect(jsonPath("$.treatmentPurpose", is("V")));
    }

    /**
     * The same exit to a landfill: the partner is the operator, the code says it is disposal, and
     * the "Scopul" letter stays empty. The note of cap. 2 does define an E, but no filled Anexa 1
     * we hold writes it — the D code in cap. 4 is what identifies a disposal. See
     * {@link ro.ecoregistru.enums.TreatmentPurpose}.
     */
    @Test
    void disposalByAPartnerCarriesItsDCodeAndNoScopulLetter() throws Exception {
        mockMvc.perform(movement(collectorToken, workPointId,
                        "  \"operation\": \"DISPOSED\", \"partnerId\": \"" + partnerId
                                + "\", \"operationCode\": \"D5\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationCode", is("D5")))
                .andExpect(jsonPath("$.treatmentPurpose", is(nullValue())));
    }

    /** Nothing requires a partner: an operation with none was performed on our own site. */
    @Test
    void recoveryOnOurOwnSiteNeedsNoPartner() throws Exception {
        mockMvc.perform(movement(collectorToken, workPointId,
                        "  \"operation\": \"RECOVERED\", \"operationCode\": \"R3\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partnerId", is(nullValue())));
    }

    // ---------- Which register the quantity lands in ----------

    /** Art. 2 alin. (1): a takeover is never Anexa 1, and the caller does not have to say so. */
    @Test
    void takeoverIsKeptOutOfAnexa1() throws Exception {
        mockMvc.perform(movement(collectorToken, workPointId, "  \"operation\": \"COLLECTED\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.register", is("ART_48")))
                .andExpect(jsonPath("$.operationCode", is(nullValue())))
                .andExpect(jsonPath("$.treatmentPurpose", is(nullValue())));
    }

    /** Art. 1 alin. (1): own waste is Anexa 1, and cannot be pushed out of it. */
    @Test
    void generatedWasteCannotBeForcedIntoTheArt48Register() throws Exception {
        mockMvc.perform(movement(collectorToken, workPointId,
                        "  \"operation\": \"GENERATED\", \"register\": \"ART_48\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("movement.register.invalid")));
    }

    /**
     * The case the operation alone cannot decide, and the reason the discriminator is stored:
     * passing on goods taken from a third party looks exactly like handing over own waste.
     */
    @Test
    void passingOnCollectedGoodsStaysOutOfAnexa1() throws Exception {
        mockMvc.perform(movement(collectorToken, workPointId,
                        "  \"operation\": \"RECOVERED\", \"partnerId\": \"" + partnerId
                                + "\", \"operationCode\": \"R13\", \"register\": \"ART_48\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.register", is("ART_48")));
    }

    /** A company that only generates has no art. 48 register to write into. */
    @Test
    void takeoverIsRejectedForAPlainGenerator() throws Exception {
        mockMvc.perform(movement(generatorToken, generatorWorkPointId, "  \"operation\": \"COLLECTED\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("movement.register.art48.disabled")));
    }

    /** A movement written straight through the repository still gets a register. */
    @Test
    void repositoryWritesGetTheRegisterTheirOperationImplies() {
        WasteMovement saved = movementRepository.save(WasteMovement.builder()
                .company(generator)
                .workPoint(workPointRepository.getReferenceById(generatorWorkPointId))
                .date(LocalDate.of(2026, 7, 5))
                .wasteCode(wasteCodeRepository.getReferenceById(wasteCodeId))
                .quantity(new BigDecimal("5.000")).unit(Unit.KG)
                .operation(WasteOperation.GENERATED)
                .deleted(false).createdBy(UUID.randomUUID()).build());
        assertThat(saved.getRegister()).isEqualTo(WasteRegister.ANEXA_1);
    }

    // ---------- The depot seam: schema only in Etapa 2, but tenant-scoped from day one ----------

    @Test
    void receptionsAndDeliveriesAreTenantScoped() {
        AppUser admin = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        Company demo = admin.getCompany();
        WasteCode code = wasteCodeRepository.getReferenceById(wasteCodeId);

        Reception reception = receptionRepository.save(Reception.builder()
                .company(demo)
                .workPoint(workPointRepository.getReferenceById(workPointId))
                .date(LocalDate.of(2026, 6, 2))
                .wasteCode(code)
                .quantity(new BigDecimal("500.000")).unit(Unit.KG)
                .supplierPartner(partnerRepository.getReferenceById(partnerId))
                .documentReference("Recepție 15/06")
                .unitPrice(new BigDecimal("0.4500"))
                .totalValue(new BigDecimal("225.00"))
                .deleted(false).createdBy(admin.getId()).build());

        Delivery delivery = deliveryRepository.save(Delivery.builder()
                .company(demo)
                .workPoint(workPointRepository.getReferenceById(workPointId))
                .date(LocalDate.of(2026, 6, 20))
                .wasteCode(code)
                .quantity(new BigDecimal("450.000")).unit(Unit.KG)
                .recipientPartner(partnerRepository.getReferenceById(partnerId))
                .operationCode(WasteOperationCode.R5)
                .documentReference("Aviz nr. 366")
                .deleted(false).createdBy(admin.getId()).build());

        assertThat(receptionRepository.findAllByCompany_IdAndDeletedFalse(demo.getId()))
                .extracting(Reception::getId).contains(reception.getId());
        assertThat(deliveryRepository.findAllByCompany_IdAndDeletedFalse(demo.getId()))
                .extracting(Delivery::getId).contains(delivery.getId());

        // The other tenant sees neither.
        assertThat(receptionRepository.findAllByCompany_IdAndDeletedFalse(generator.getId())).isEmpty();
        assertThat(deliveryRepository.findAllByCompany_IdAndDeletedFalse(generator.getId())).isEmpty();
        assertThat(receptionRepository.findByIdAndCompany_IdAndDeletedFalse(
                reception.getId(), generator.getId())).isEmpty();
        assertThat(deliveryRepository.findByIdAndCompany_IdAndDeletedFalse(
                delivery.getId(), generator.getId())).isEmpty();
    }

    /**
     * Builds a movement payload that is valid in every respect except the fields under test,
     * which the caller appends as raw JSON.
     */
    private MockHttpServletRequestBuilder movement(String token, UUID workPoint, String extraJson) {
        String body = """
                {
                  "workPointId": "%s",
                  "date": "2026-07-05",
                  "wasteCodeId": "%s",
                  "quantity": 5.000,
                  "unit": "KG",
                  "physicalState": "SOLID",
                %s
                }
                """.formatted(workPoint, wasteCodeId, extraJson);
        return post("/api/v1/movements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
