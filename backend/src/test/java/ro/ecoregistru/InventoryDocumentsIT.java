package ro.ecoregistru;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.controller.request.InventoryDeclarationRequest;
import ro.ecoregistru.controller.request.InventoryHeaderRequest;
import ro.ecoregistru.controller.request.InventoryLinesRequest;
import ro.ecoregistru.controller.request.InventoryPvRequest;
import ro.ecoregistru.controller.request.StockOpeningRequest;
import ro.ecoregistru.controller.request.WeighingLinesRequest;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.controller.response.InventoryResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.KeeperDeclaration;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WasteCode;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.CountMethod;
import ro.ecoregistru.enums.InventoryKind;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.StockOpeningSource;
import ro.ecoregistru.enums.WeighingOperationType;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteArticleRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.DeadlineService;
import ro.ecoregistru.service.InventoryService;
import ro.ecoregistru.service.StockOpeningService;
import ro.ecoregistru.service.WeighingOperationService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * D3.5 — documentele inventarului, citite de pe pagina tipărită: decizia, declarația gestionarului (pct. 8 lit. a)),
 * lista 14-3-12 cu semnăturile pe fiecare filă și mențiunea gestionarului pe ultima (pct. 33), stocurile depreciate pe
 * listă separată (pct. 20), procesul-verbal cu elementele pct. 42, nota de preluare; toate cu numele și versiunea
 * aplicației (OMFP 2634/2015 anexa 1 pct. 58 lit. k)).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class InventoryDocumentsIT {

    @Autowired InventoryService inventories;
    @Autowired StockOpeningService openings;
    @Autowired WeighingOperationService operations;
    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteArticleRepository articleRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;

    LocalDate start;
    Company company;
    AppUser admin;
    WorkPoint depot;
    Partner partner;
    WasteCode cardboardCode;

    @BeforeEach
    void setUp() {
        start = DeadlineService.today().minusDays(2);
        company = companyRepository.save(Company.builder()
                .name("Documente " + suffix() + " SRL").cui("ROD" + suffix()).type(CompanyType.COLLECTOR)
                .environmentalAuthNumber("AM-" + suffix()).active(true).createdAt(Instant.now()).build());
        admin = appUserRepository.save(AppUser.builder()
                .email("doc+" + suffix() + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        depot = workPointRepository.save(WorkPoint.builder()
                .company(company).name("Baciu " + suffix()).active(true).createdAt(Instant.now()).build());
        partner = partnerRepository.save(Partner.builder()
                .company(company).name("Magazin " + suffix() + " SRL").cui("RO" + suffix())
                .type(PartnerType.GENERATOR).packagingOrigin(PackagingOrigin.GENERATOR_PJ)
                .active(true).createdAt(Instant.now()).build());
        cardboardCode = wasteCodeRepository.findAll().stream().filter(c -> c.getCode().startsWith("15 01 01"))
                .findFirst().orElseThrow();
        TenantContext.set(company.getId());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                appUserRepository.findById(admin.getId()).orElseThrow(), null, List.of()));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void listHasColumnsAndSignaturesOnEveryPage() throws Exception {
        InventoryResponse inv = inventory(40, false);

        List<String> pages = pages(pdf(inv.id(), "lista"));

        assertThat(pages.size()).isGreaterThanOrEqualTo(2);
        assertThat(flat(pages.get(0))).contains(flat("LISTĂ DE INVENTARIERE"), flat("Stocuri scriptice"),
                flat("Stocuri faptice"), flat("Loc de depozitare"), flat("Preț unitar"), flat("Contabilitate"));
        for (int p = 0; p < pages.size(); p++) {
            assertThat(flat(pages.get(p))).as("pagina " + (p + 1))
                    .contains(flat("Comisia de inventariere"), flat("Gestionar"), flat("Ion Gestionar"),
                            flat("Ana Preşedinte"), flat("Dan Membru"), flat("Contabilitate"),
                            flat("Pagina " + (p + 1) + " din " + pages.size()));
        }
    }

    @Test
    void lastPageHasKeeperStatement() throws Exception {
        InventoryResponse inv = inventory(3, false);
        List<String> pages = pages(pdf(inv.id(), "lista"));
        assertThat(flat(pages.get(pages.size() - 1))).contains(
                flat("au fost inventariate şi consemnate în listele de inventariere în prezenţa sa"),
                flat("Obiecţiile gestionarului"), flat("fără obiecţii"));
    }

    @Test
    void slowMovingOnSeparateList() throws Exception {
        InventoryResponse inv = inventory(3, true);
        assertThat(flat(String.join("\n", pages(pdf(inv.id(), "lista")))))
                .contains(flat("Stocuri depreciate, fără mişcare sau greu vandabile"));
    }

    @Test
    void pvHasThirteenElements() throws Exception {
        InventoryResponse inv = inventory(2, false);
        String text = flat(String.join("\n", pages(pdf(inv.id(), "pv"))));
        assertThat(text).contains(flat("PROCES-VERBAL DE INVENTARIERE"), flat("1. Data întocmirii"),
                flat("2. Comisia de inventariere"), flat("3. Decizia"), flat("4. Gestiunea"),
                flat("5. Data începerii şi data terminării"), flat("6. Rezultatele"), flat("7. Cauzele"),
                flat("8. Propunerile de măsuri"), flat("9. Stocurile depreciate"), flat("10. "), flat("11. "),
                flat("nu e cazul (mijloace fixe)"), flat("12. Constatări"), flat("13. Alte aspecte"),
                flat("primit/eliberat în timpul inventarierii"), flat("Intrare nr."),
                flat("Aviz financiar-contabil"), flat("Aviz juridic"), flat("Aprobat, administrator"));
    }

    @Test
    void declarationHasSevenQuestions() throws Exception {
        InventoryResponse inv = inventory(1, false);
        String text = flat(String.join("\n", pages(pdf(inv.id(), "declaratie"))));
        assertThat(text).contains(flat("DECLARAŢIA GESTIONARULUI"), flat("Ultimul document de intrare"));
        for (String q : KeeperDeclaration.QUESTIONS) {
            assertThat(text).contains(flat(q));
        }
    }

    @Test
    void decisionHasCommissionAndPresident() throws Exception {
        InventoryResponse inv = inventory(1, false);
        String text = flat(String.join("\n", pages(pdf(inv.id(), "decizie"))));
        assertThat(text).contains(flat("DECIZIE"), flat("Ana Preşedinte"), flat("preşedinte"), flat("Dan Membru"),
                flat("Modul de efectuare"), flat("Metoda"));
    }

    @Test
    void everyPdfHasAppVersion() throws Exception {
        InventoryResponse inv = inventory(1, false);
        for (String doc : List.of("decizie", "declaratie", "lista", "pv")) {
            assertThat(flat(String.join("\n", pages(pdf(inv.id(), doc))))).as(doc)
                    .contains(flat("Generat cu WasteHouse"), flat("versiunea"));
        }
    }

    @Test
    void openingNoteHasSourceAndSignatures() throws Exception {
        UUID id = openings.create(new StockOpeningRequest(depot.getId(), start, StockOpeningSource.STOCK_CARDS,
                "Ion Gestionar", "Ana Contabil", null, List.of(new StockOpeningRequest.Line(null, cardboardCode.getId(),
                new BigDecimal("500"))))).id();
        byte[] body = mockMvc.perform(get("/api/v1/stock-openings/" + id + "/pdf")
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        String text = flat(String.join("\n", pages(body)));
        assertThat(text).contains(flat("NOTĂ DE PRELUARE A SOLDURILOR"), flat("fişele de magazie"),
                flat("Ion Gestionar"), flat("Ana Contabil"), flat("Generat cu WasteHouse"), flat("500"));
    }

    // --- helpers ---

    private InventoryResponse inventory(int manualLines, boolean slowMoving) {
        UUID op = operations.create(new WeighingOperationRequest(WeighingOperationType.IN, depot.getId(), start,
                partner.getId(), null, null, null, null, null, null, null, null, null, null, null, null, null)).id();
        WasteArticle first = article(0);
        operations.replaceLines(op, new WeighingLinesRequest(null, null, List.of(new WeighingLinesRequest.Line(
                first.getId(), null, null, new BigDecimal("10"), null, null, null, null))));
        operations.finalizeOperation(op);
        InventoryResponse inv = inventories.open(new InventoryHeaderRequest(depot.getId(), "12", start.minusDays(3),
                InventoryKind.ANNUAL, false, "inventariere integrală", "cântărire", start, start,
                List.of(new InventoryHeaderRequest.Member("Ana Preşedinte", "economist", true),
                        new InventoryHeaderRequest.Member("Dan Membru", "șef depozit", false)),
                "Ion Gestionar", null, null));
        inventories.saveDeclaration(inv.id(), new InventoryDeclarationRequest(
                Collections.nCopies(7, new KeeperDeclaration.Answer(false, null)), "NIR 7", "Aviz 3", start));
        List<InventoryLinesRequest.Line> rows = new ArrayList<>();
        for (int i = 0; i < manualLines; i++) {
            WasteArticle a = article(i + 1);
            rows.add(new InventoryLinesRequest.Line(null, a.getId(), null, new BigDecimal("5"), CountMethod.WEIGHED,
                    null, "găsit la numărare", null, null, slowMoving && i == 0));
        }
        inventories.saveLines(inv.id(), new InventoryLinesRequest(rows));
        inventories.savePv(inv.id(), new InventoryPvRequest(start, "plusuri din cântăriri neînregistrate", "instruire",
                null, "depozitare corectă", null, null, null));
        return inventories.get(inv.id());
    }

    private WasteArticle article(int i) {
        return articleRepository.save(WasteArticle.builder()
                .company(company).wasteCode(cardboardCode).name("Carton " + i).active(true).createdAt(Instant.now()).build());
    }

    private byte[] pdf(UUID id, String doc) throws Exception {
        return mockMvc.perform(get("/api/v1/inventories/" + id + "/pdf/" + doc)
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
    }

    private static List<String> pages(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            List<String> pages = new ArrayList<>();
            for (int p = 1; p <= reader.getNumberOfPages(); p++) {
                pages.add(extractor.getTextFromPage(p));
            }
            return pages;
        } finally {
            reader.close();
        }
    }

    private static String flat(String s) {
        return Golden.flat(s);
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
