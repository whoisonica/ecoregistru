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
import ro.ecoregistru.entity.NaturalPerson;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WeighingOperation;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.PaymentMethod;
import ro.ecoregistru.enums.PriceVisibility;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteRegister;
import ro.ecoregistru.enums.WeighingOperationStatus;
import ro.ecoregistru.enums.WeighingOperationType;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.NaturalPersonRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteArticleRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WeighingOperationRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.service.DepotRetentions;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * D1.11 — borderoul de achiziție (OUG 31/2011, anexa) și plafonul de numerar (Legea 70/2015 art. 4).
 * Textul se compară fără spații ({@link Golden#flat}), pe fraze întregi din model, ca o rubrică pierdută
 * sau o cotă veche să fie prinse.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class BorderouIT {

    private static final LocalDate DAY = LocalDate.of(2026, 3, 10);

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository userRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired NaturalPersonRepository personRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired WasteArticleRepository articleRepository;
    @Autowired WeighingOperationRepository operationRepository;
    @Autowired WasteMovementRepository movementRepository;

    Company company;
    AppUser admin;
    AppUser operator;
    AppUser viewer;
    WorkPoint depot;
    NaturalPerson person;
    WasteArticle copper;
    WasteArticle cardboard;
    int nextNumber = 1;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        company = companyRepository.save(Company.builder()
                .name("Colector Borderou " + suffix + " SRL").cui("ROB" + suffix).address("Cluj, str. Depozitului 3")
                .tradeRegisterNumber("J12/345/2020").environmentalAuthNumber("AM-CJ-77")
                .type(CompanyType.COLLECTOR).active(true).createdAt(Instant.now()).build());
        admin = user("admin", Role.ADMIN, suffix);
        operator = user("operator", Role.OPERATOR, suffix);
        viewer = user("viewer", Role.CLIENT_VIEWER, suffix);
        depot = workPointRepository.save(WorkPoint.builder()
                .company(company).name("Depozit Vest").active(true).createdAt(Instant.now()).build());
        person = personRepository.save(NaturalPerson.builder()
                .company(company).name("Ion Popescu").cnp("1900101123457").identification("CJ 123456")
                .address("Cluj, str. Mare 1").active(true).createdAt(Instant.now()).build());
        copper = article("Cupru casnic", "17 04 01", true);
        cardboard = article("Carton", "15 01 01", false);
    }

    @Test
    void aMetalBorderouCarriesEveryRubricOfTheModelWithTheRatesInForce() throws Exception {
        WeighingOperation op = fromPerson(WeighingOperationStatus.FINALIZED, PaymentMethod.NUMERAR, DAY);
        op.setReceiptNumber("CH-12");
        op = operationRepository.saveAndFlush(op);
        line(op, copper, "100", "30");
        retain(op);

        String text = Golden.flat(Golden.pdfText(borderou(admin, op)));
        assertThat(text)
                .contains(Golden.flat(company.getName()))
                .contains("CUI/CIF:" + company.getCui()).contains("Nr.Reg.Com.:J12/345/2020")
                .contains("Autorizaţiademediunr.AM-CJ-77")
                .contains("BORDEROUDEACHIZIŢIEDEDEŞEURIMETALICE")
                .contains("Nr.1dindata10.03.2026")
                .contains("IonPopescu").contains("CJ123456").contains("CNP1900101123457").contains("Cluj,str.Mare1")
                .contains("CodulconformHGnr.856/2002")
                .contains("Cuprucasnic—cupru,bronz,alamă")
                .contains("17040110030,003.000,00")
                .contains("TOTAL3.000,00")
                // 3.000 − 300 impozit − 60 AFM = 2.640 lei, cu chitanța.
                .contains("Seachităsumade2.640,00leicuchitanţanr.CH-12.")
                .contains("Impozitulpevenitde10%(300,00lei)şicontribuţiade2%(60,00lei)laAdministraţiaFonduluipentruMediu"
                        + "aufostreţinutelasursădinvaloareabrută.")
                .contains("Declarpepropriarăspunderecădeşeurilepecarelepredauprovindingospodăriaproprie.")
                .contains("Gestionarprimitor")
                .doesNotContain("16%").doesNotContain("3%");
    }

    /** C3: la hârtie, borderoul e la cerere și fără date de identitate; nici impozit, nici declarație. */
    @Test
    void aBorderouWithoutMetalNamesThePersonOnly() throws Exception {
        WeighingOperation op = fromPerson(WeighingOperationStatus.FINALIZED, PaymentMethod.VIREMENT, DAY);
        line(op, cardboard, "500", "0.5");
        retain(op);

        String text = Golden.flat(Golden.pdfText(borderou(admin, op)));
        assertThat(text)
                .contains("BORDEROUDEACHIZIŢIEDEDEŞEURI").doesNotContain("METALICE")
                .contains("IonPopescu")
                .doesNotContain("1900101123457").doesNotContain("CJ123456").doesNotContain("Cluj,str.Mare1")
                .contains("Seachităsumade245,00leiîntermendemaximum3zilelucrătoare")
                .contains("Contribuţiade2%(5,00lei)laAdministraţiaFonduluipentruMediuafostreţinută")
                .doesNotContain("Impozitul").doesNotContain("gospodăriaproprie");
    }

    /** Fără fel de plată ales, nu se alege în locul omului: fraza modelului rămâne cu ambele variante. */
    @Test
    void withoutAPaymentMethodTheModelSentenceKeepsBothWays() throws Exception {
        WeighingOperation op = fromPerson(WeighingOperationStatus.FINALIZED, null, DAY);
        line(op, cardboard, "100", "1");
        retain(op);
        assertThat(Golden.flat(Golden.pdfText(borderou(admin, op))))
                .contains("Seachităsumade98,00leicuchitanţanr...................sauîntermendemaximum3zilelucrătoare"
                        + "dela dataprezentei,prinviramentbancarîncontuldeţinătorului.".replace(" ", ""));
    }

    @Test
    void theNumberIsGivenOnceAndInOrder() throws Exception {
        WeighingOperation first = finalizedCopper(DAY);
        WeighingOperation second = finalizedCopper(DAY);
        assertThat(Golden.flat(Golden.pdfText(borderou(admin, second)))).contains("Nr.1dindata");
        assertThat(Golden.flat(Golden.pdfText(borderou(operator, first)))).contains("Nr.2dindata");
        assertThat(Golden.flat(Golden.pdfText(borderou(admin, second)))).contains("Nr.1dindata");
        assertThat(operationRepository.findById(first.getId()).orElseThrow().getBorderouNumber()).isEqualTo(2);
    }

    @Test
    void onlyAFinalizedIntakeFromAPersonHasABorderou() throws Exception {
        WeighingOperation inProgress = fromPerson(WeighingOperationStatus.IN_PROGRESS, PaymentMethod.NUMERAR, DAY);
        line(inProgress, copper, "10", "30");
        refused(inProgress, "weighing.borderou.requires.finalized");

        Partner supplier = partnerRepository.save(Partner.builder()
                .company(company).name("Magazin Alfa SRL").cui("RO" + UUID.randomUUID().toString().substring(0, 6))
                .type(PartnerType.GENERATOR).supplier(true).client(true).active(true).createdAt(Instant.now()).build());
        WeighingOperation fromPartner = operationRepository.saveAndFlush(head(WeighingOperationType.IN,
                WeighingOperationStatus.FINALIZED, DAY).partner(supplier).build());
        line(fromPartner, copper, "10", "30");
        refused(fromPartner, "weighing.borderou.requires.person");

        // Controlul pozitiv: aceeași persoană, finalizată, trece.
        borderou(admin, finalizedCopper(DAY));
        assertThat(operationRepository.findById(inProgress.getId()).orElseThrow().getBorderouNumber()).isNull();
    }

    /** Prețuri și CNP întreg: doar cine scrie și vede prețurile. */
    @Test
    void theBorderouIsForWritersWhoSeePrices() throws Exception {
        WeighingOperation op = finalizedCopper(DAY);
        mockMvc.perform(get(url(op, "borderou")).header("Authorization", bearer(viewer))).andExpect(status().isForbidden());
        company.setPriceVisibility(PriceVisibility.ADMIN_ONLY);
        companyRepository.save(company);
        mockMvc.perform(get(url(op, "borderou")).header("Authorization", bearer(operator))).andExpect(status().isForbidden());
        assertThat(operationRepository.findById(op.getId()).orElseThrow().getBorderouNumber()).isNull();
        borderou(admin, op);
    }

    /**
     * Legea 70/2015 art. 4: cel mult 10.000 lei numerar pe zi către o persoană. Se adună ce s-a dat omului
     * (după rețineri) în aceeași zi; anulatele, viramentele și altă zi nu intră.
     */
    @Test
    void cashToTheSamePersonOnTheSameDayIsAddedUpAgainstTheLimit() throws Exception {
        WeighingOperation morning = cashCardboard(DAY, "10000", WeighingOperationStatus.FINALIZED);  // 5.000 − 100 = 4.900
        cashCardboard(DAY.plusDays(1), "10000", WeighingOperationStatus.FINALIZED);
        cashCardboard(DAY, "40000", WeighingOperationStatus.CANCELLED);
        WeighingOperation transfer = fromPerson(WeighingOperationStatus.FINALIZED, PaymentMethod.VIREMENT, DAY);
        line(transfer, cardboard, "40000", "0.5");

        mockMvc.perform(get(url(morning, "cash-check")).header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aboveLimit", is(false)))
                .andExpect(jsonPath("$.paidToday", is(4900.0)));

        WeighingOperation afternoon = cashCardboard(DAY, "12000", WeighingOperationStatus.IN_PROGRESS);  // 6.000 − 120
        mockMvc.perform(get(url(afternoon, "cash-check")).header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.aboveLimit", is(true)))
                .andExpect(jsonPath("$.paidToday", is(10780.0)));

        company.setPriceVisibility(PriceVisibility.ADMIN_ONLY);
        companyRepository.save(company);
        mockMvc.perform(get(url(afternoon, "cash-check")).header("Authorization", bearer(operator)))
                .andExpect(jsonPath("$.aboveLimit", is(true)))
                .andExpect(jsonPath("$.paidToday", nullValue()));
    }

    // --- helpers ---

    private byte[] borderou(AppUser user, WeighingOperation op) throws Exception {
        return mockMvc.perform(get(url(op, "borderou")).header("Authorization", bearer(user)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
    }

    private void refused(WeighingOperation op, String code) throws Exception {
        mockMvc.perform(get(url(op, "borderou")).header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$['error-code']", is(code)));
    }

    private static String url(WeighingOperation op, String what) {
        return "/api/v1/weighing-operations/" + op.getId() + "/" + what;
    }

    private String bearer(AppUser user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    private AppUser user(String prefix, Role role, String suffix) {
        return userRepository.save(AppUser.builder()
                .email(prefix + "+borderou" + suffix + "@demo.ro").password("x")
                .role(role).company(company).enabled(true).createdAt(Instant.now()).build());
    }

    private WasteArticle article(String name, String code, boolean metal) {
        return articleRepository.save(WasteArticle.builder()
                .company(company).wasteCode(wasteCodeRepository.findByCode(code).orElseThrow())
                .name(name).metal(metal).active(true).createdAt(Instant.now()).build());
    }

    private WeighingOperation.WeighingOperationBuilder head(WeighingOperationType type, WeighingOperationStatus status,
                                                           LocalDate date) {
        WeighingOperation.WeighingOperationBuilder op = WeighingOperation.builder()
                .company(company).workPoint(depot).type(type).number(nextNumber++).date(date)
                .vehicleRegistration("CJ 10 ABC").status(status).createdBy(admin.getId());
        if (status == WeighingOperationStatus.FINALIZED) {
            op.finalizedAt(Instant.now()).finalizedBy(admin.getId());
        }
        if (status == WeighingOperationStatus.CANCELLED) {
            op.cancelledAt(Instant.now()).cancelledBy(admin.getId()).cancelReason("Dublură");
        }
        return op;
    }

    private WeighingOperation fromPerson(WeighingOperationStatus status, PaymentMethod payment, LocalDate date) {
        return operationRepository.saveAndFlush(head(WeighingOperationType.IN, status, date)
                .naturalPerson(person).origin(PackagingOrigin.POPULATIE).ownHousehold(true)
                .paymentMethod(payment).build());
    }

    private WeighingOperation finalizedCopper(LocalDate date) {
        WeighingOperation op = fromPerson(WeighingOperationStatus.FINALIZED, PaymentMethod.NUMERAR, date);
        line(op, copper, "10", "30");
        retain(op);
        return op;
    }

    private WeighingOperation cashCardboard(LocalDate date, String kg, WeighingOperationStatus status) {
        WeighingOperation op = fromPerson(status, PaymentMethod.NUMERAR, date);
        line(op, cardboard, kg, "0.5");
        if (status == WeighingOperationStatus.FINALIZED) {
            retain(op);
        }
        return op;
    }

    private void line(WeighingOperation op, WasteArticle article, String kg, String price) {
        BigDecimal quantity = new BigDecimal(kg);
        BigDecimal unitPrice = new BigDecimal(price);
        movementRepository.saveAndFlush(WasteMovement.builder()
                .company(company).workPoint(depot).date(op.getDate()).wasteCode(article.getWasteCode())
                .article(article).weighingOperation(op).lineNo(1)
                .netKg(quantity).quantity(quantity).unit(Unit.KG)
                .unitPrice(unitPrice).totalValue(unitPrice.multiply(quantity).setScale(2))
                .operation(WasteOperation.COLLECTED).register(WasteRegister.ART_48)
                .deleted(false).createdBy(admin.getId()).build());
    }

    /** Ce face finalizarea: reținerile calculate o dată și păstrate pe operațiune. */
    private void retain(WeighingOperation op) {
        List<WasteMovement> lines = movementRepository.findAllByWeighingOperation_IdOrderByLineNoAsc(op.getId());
        DepotRetentions.Amounts a = DepotRetentions.of(op, lines);
        op.setAfmBase(a.afmBase());
        op.setAfmContribution(a.afm());
        op.setIncomeTaxBase(a.incomeTaxBase());
        op.setIncomeTax(a.incomeTax());
        operationRepository.saveAndFlush(op);
    }
}
