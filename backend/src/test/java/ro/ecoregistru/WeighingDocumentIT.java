package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.NaturalPerson;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteCode;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WeighingOperation;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteOperationCode;
import ro.ecoregistru.enums.WasteRegister;
import ro.ecoregistru.enums.WeighingOperationStatus;
import ro.ecoregistru.enums.WeighingOperationType;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.NaturalPersonRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WeighingOperationRepository;
import ro.ecoregistru.repository.WorkPointRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * D1.13 — documentele de transport pe operațiune: un camion, un formular, cu toate sortimentele. Fiecare
 * refuz are controlul lui pozitiv în același fixture, ca un refuz verde să nu vină din altă cauză.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class WeighingDocumentIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository userRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired NaturalPersonRepository personRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired WeighingOperationRepository operationRepository;
    @Autowired WasteMovementRepository movementRepository;

    Company company;
    AppUser admin;
    AppUser operator;
    AppUser viewer;
    WorkPoint depot;
    Partner recycler;
    List<WasteCode> plain;
    WasteCode hazardous;
    int nextNumber = 1;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        company = companyRepository.save(Company.builder()
                .name("Depozit Documente " + suffix + " SRL").cui("ROD" + suffix).anexa3Series("DEP")
                .type(CompanyType.COLLECTOR).active(true).createdAt(Instant.now()).build());
        admin = user("admin", Role.ADMIN, suffix);
        operator = user("operator", Role.OPERATOR, suffix);
        viewer = user("viewer", Role.CLIENT_VIEWER, suffix);
        depot = workPointRepository.save(WorkPoint.builder()
                .company(company).name("Depozit Est").active(true).createdAt(Instant.now()).build());
        recycler = partnerRepository.save(Partner.builder()
                .company(company).name("Reciclator Beta SA").cui("RO" + suffix).type(PartnerType.RECOVERER)
                .supplier(true).client(true).active(true).createdAt(Instant.now()).build());
        List<WasteCode> all = wasteCodeRepository.findAll();
        plain = all.stream().filter(c -> !c.isHazardous()).limit(12).toList();
        hazardous = all.stream().filter(c -> c.isHazardous() && !c.getCode().startsWith("18")).findFirst().orElseThrow();
    }

    /** Douăsprezece sortimente într-un camion ies pe un singur formular, cu fiecare cod și cantitatea lui. */
    @Test
    void twelveLinesMakeOneAnexa3() throws Exception {
        WeighingOperation out = exit(WeighingOperationStatus.FINALIZED);
        for (int i = 0; i < 12; i++) {
            line(out, i + 1, plain.get(i), String.valueOf(100 + i));
        }

        String text = Golden.flat(Golden.pdfText(pdf(admin, "/api/v1/weighing-operations/" + out.getId() + "/anexa3")));
        assertThat(occurrences(text, "ANEXA3")).isEqualTo(1);
        assertThat(occurrences(text, "Serieşinumăr")).isEqualTo(1);
        for (int i = 0; i < 12; i++) {
            String code = plain.get(i).getCode().replace(" ", "");
            assertThat(text).contains("Cod:" + code).contains(code + ":" + (100 + i));
        }
        assertThat(text).contains("ReciclatorBetaSA");
    }

    /** Numărul se dă o dată, iar seria e a firmei, comună cu a mișcărilor: carnetul e unul singur. */
    @Test
    void theNumberIsAllocatedOnceFromTheCompanysOneSeries() throws Exception {
        WasteMovement loose = movementRepository.saveAndFlush(movement(null, 0, plain.get(0), "50")
                .operation(WasteOperation.RECOVERED).operationCode(WasteOperationCode.R3).build());
        pdf(admin, "/api/v1/movements/" + loose.getId() + "/anexa3");

        WeighingOperation out = exit(WeighingOperationStatus.IN_PROGRESS);
        line(out, 1, plain.get(1), "70");
        String first = Golden.flat(Golden.pdfText(pdf(operator, "/api/v1/weighing-operations/" + out.getId() + "/anexa3")));
        String again = Golden.flat(Golden.pdfText(pdf(admin, "/api/v1/weighing-operations/" + out.getId() + "/anexa3")));

        assertThat(number(first)).isEqualTo("DEPNr:2");
        assertThat(number(again)).isEqualTo("DEPNr:2");
        assertThat(operationRepository.findById(out.getId()).orElseThrow().getAnexa3Number()).isEqualTo(2);

        WasteMovement later = movementRepository.saveAndFlush(movement(null, 0, plain.get(2), "30")
                .operation(WasteOperation.RECOVERED).operationCode(WasteOperationCode.R3).build());
        pdf(admin, "/api/v1/movements/" + later.getId() + "/anexa3");
        assertThat(movementRepository.findById(later.getId()).orElseThrow().getAnexa3Number()).isEqualTo(3);
    }

    /** Anexa 3 ia doar nepericuloasele; avizul însoțește toată marfa. */
    @Test
    void anexa3LeavesHazardousLinesToAnexa2WhileTheAvizCarriesThemAll() throws Exception {
        WeighingOperation out = exit(WeighingOperationStatus.FINALIZED);
        line(out, 1, plain.get(0), "100");
        line(out, 2, hazardous, "40");
        String hazardousCode = hazardous.getCode().replace(" ", "");

        String anexa3 = Golden.flat(Golden.pdfText(pdf(admin, "/api/v1/weighing-operations/" + out.getId() + "/anexa3")));
        assertThat(anexa3).contains("Cod:" + plain.get(0).getCode().replace(" ", "")).doesNotContain(hazardousCode);

        String aviz = Golden.flat(Golden.pdfText(pdf(viewer, "/api/v1/weighing-operations/" + out.getId() + "/aviz")));
        assertThat(aviz).contains(plain.get(0).getCode().replace(" ", "")).contains(hazardousCode);
        // „Nr. comandă / aviz” de pe operațiune e numărul avizului.
        assertThat(aviz).contains("Serie/număraviz" + "AVZ-77");
    }

    @Test
    void anOperationWithOnlyHazardousLinesHasNoAnexa3() throws Exception {
        WeighingOperation out = exit(WeighingOperationStatus.FINALIZED);
        line(out, 1, hazardous, "40");
        refused(admin, "/api/v1/weighing-operations/" + out.getId() + "/anexa3", "weighing.anexa3.only.hazardous");
        pdf(admin, "/api/v1/weighing-operations/" + out.getId() + "/aviz");
    }

    /** AX: persoana fizică își aduce singură deșeul, fără formular; la o intrare îl face expeditorul. */
    @Test
    void anInboundOperationPrintsNoTransportDocument() throws Exception {
        NaturalPerson person = personRepository.save(NaturalPerson.builder()
                .company(company).name("Ion Popescu").active(true).createdAt(Instant.now()).build());
        WeighingOperation fromPerson = operationRepository.saveAndFlush(head(WeighingOperationType.IN,
                WeighingOperationStatus.FINALIZED).naturalPerson(person).origin(PackagingOrigin.POPULATIE).build());
        line(fromPerson, 1, plain.get(0), "10");
        WeighingOperation fromPartner = operationRepository.saveAndFlush(head(WeighingOperationType.IN,
                WeighingOperationStatus.FINALIZED).partner(recycler).build());
        line(fromPartner, 1, plain.get(0), "10");

        for (WeighingOperation in : List.of(fromPerson, fromPartner)) {
            refused(admin, "/api/v1/weighing-operations/" + in.getId() + "/anexa3", "weighing.document.requires.handover");
            refused(admin, "/api/v1/weighing-operations/" + in.getId() + "/aviz", "weighing.document.requires.handover");
        }
    }

    @Test
    void aCancelledOperationPrintsNothing() throws Exception {
        WeighingOperation out = exit(WeighingOperationStatus.CANCELLED);
        line(out, 1, plain.get(0), "10");
        refused(admin, "/api/v1/weighing-operations/" + out.getId() + "/anexa3", "weighing.document.cancelled");
        refused(admin, "/api/v1/weighing-operations/" + out.getId() + "/aviz", "weighing.document.cancelled");
    }

    /** Un rând de cântar nu-și mai scoate formularul lui: ar fi un număr pe sortiment pentru un singur camion. */
    @Test
    void aWeighingLineHasNoDocumentOfItsOwn() throws Exception {
        WeighingOperation out = exit(WeighingOperationStatus.FINALIZED);
        WasteMovement line = line(out, 1, plain.get(0), "10");
        refused(admin, "/api/v1/movements/" + line.getId() + "/anexa3", "weighing.line.document.through.operation");
        refused(admin, "/api/v1/movements/" + line.getId() + "/aviz", "weighing.line.document.through.operation");
        assertThat(movementRepository.findById(line.getId()).orElseThrow().getAnexa3Number()).isNull();
    }

    /** Prima tipărire a Anexei 3 scrie numărul, deci cere drept de scriere; avizul e o citire. */
    @Test
    void aViewerReadsTheAvizButCannotAllocateAnAnexa3Number() throws Exception {
        WeighingOperation out = exit(WeighingOperationStatus.FINALIZED);
        line(out, 1, plain.get(0), "10");
        mockMvc.perform(get("/api/v1/weighing-operations/" + out.getId() + "/anexa3").header("Authorization", bearer(viewer)))
                .andExpect(status().isForbidden());
        pdf(viewer, "/api/v1/weighing-operations/" + out.getId() + "/aviz");
        assertThat(operationRepository.findById(out.getId()).orElseThrow().getAnexa3Number()).isNull();
    }

    // --- helpers ---

    private byte[] pdf(AppUser user, String url) throws Exception {
        return mockMvc.perform(get(url).header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private void refused(AppUser user, String url, String code) throws Exception {
        ResultActions result = mockMvc.perform(get(url).header("Authorization", bearer(user)));
        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$['error-code']", is(code)));
    }

    private String bearer(AppUser user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    private static int occurrences(String text, String needle) {
        Matcher m = Pattern.compile(Pattern.quote(needle)).matcher(text);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    /** „DEP Nr: 2” din antetul formularului, fără spații. */
    private static String number(String flat) {
        Matcher m = Pattern.compile("Serieşinumăr:(DEPNr:\\d+)").matcher(flat);
        assertThat(m.find()).as("antetul are seria și numărul: " + flat).isTrue();
        return m.group(1);
    }

    private AppUser user(String prefix, Role role, String suffix) {
        return userRepository.save(AppUser.builder()
                .email(prefix + "+documente" + suffix + "@demo.ro").password("x")
                .role(role).company(company).enabled(true).createdAt(Instant.now()).build());
    }

    private WeighingOperation.WeighingOperationBuilder head(WeighingOperationType type, WeighingOperationStatus status) {
        WeighingOperation.WeighingOperationBuilder op = WeighingOperation.builder()
                .company(company).workPoint(depot).type(type).number(nextNumber++).date(LocalDate.of(2026, 3, 10))
                .driverName("Vasile Șofer").vehicleRegistration("B 01 ABC")
                .status(status).createdBy(admin.getId());
        if (status == WeighingOperationStatus.FINALIZED) {
            op.finalizedAt(Instant.now()).finalizedBy(admin.getId());
        }
        if (status == WeighingOperationStatus.CANCELLED) {
            op.cancelledAt(Instant.now()).cancelledBy(admin.getId()).cancelReason("Dublură");
        }
        return op;
    }

    private WeighingOperation exit(WeighingOperationStatus status) {
        return operationRepository.saveAndFlush(head(WeighingOperationType.OUT, status).partner(recycler)
                .orderNumber("AVZ-77").build());
    }

    private WasteMovement line(WeighingOperation op, int lineNo, WasteCode code, String kg) {
        boolean out = op.getType() == WeighingOperationType.OUT;
        return movementRepository.saveAndFlush(movement(op, lineNo, code, kg)
                .operation(out ? WasteOperation.RECOVERED : WasteOperation.COLLECTED)
                .operationCode(out ? WasteOperationCode.R3 : null)
                .partner(op.getPartner()).build());
    }

    private WasteMovement.WasteMovementBuilder movement(WeighingOperation op, int lineNo, WasteCode code, String kg) {
        return WasteMovement.builder()
                .company(company).workPoint(depot).date(LocalDate.of(2026, 3, 10)).wasteCode(code)
                .quantity(new BigDecimal(kg)).netKg(op == null ? null : new BigDecimal(kg)).unit(Unit.KG)
                .weighingOperation(op).lineNo(op == null ? null : lineNo)
                .partner(recycler).register(WasteRegister.ART_48)
                .deleted(false).createdBy(admin.getId());
    }
}
