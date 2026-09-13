package ro.ecoregistru;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
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
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteCode;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteOperationCode;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WorkPointRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P1.12 — golden test pe conţinutul <b>tipărit</b>, nu pe DTO. {@code Anexa1FormIT} probează deja
 * exhaustiv cifrele din {@code Anexa1Sheet} (cap. 2, seam-ul, codul periculos) — dar niciun test
 * de acolo n-a citit vreodată o cifră din <b>bytes-ii PDF-ului</b>. Motorul de calcul şi
 * {@code Anexa1FormGenerator} sunt două drumuri complet separate: primul poate rămâne corect în
 * timp ce al doilea scrie o cifră în coloana greşită, o rotunjeşte diferit, sau o omite — şi
 * niciun test existent n-ar cădea, fiindcă toate citesc {@code Anexa1Sheet}, nu pagina.
 *
 * <p><b>Tehnica.</b> Fişă proprie, tenant izolat, cifre alese <b>toate distincte între ele</b> —
 * generat, recuperat, eliminat, ieşire neclasificată şi cele patru solduri de închidere n-au nicio
 * valoare în comun. Dacă generatorul ar interschimba două coloane, sau ar propaga soldul greşit,
 * cifra care ar trebui să apară pur şi simplu n-ar mai fi în text — nu s-ar confunda cu alta.
 *
 * <p>Formatul e cel din generator ({@code Anexa1FormGenerator.KG}, {@code "#0.000"}, locale
 * {@code ROOT}): fără separator de mii, punct zecimal, exact trei zecimale.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class Anexa1FormGoldenIT {

    private static final int YEAR = 2025;

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteMovementRepository movementRepository;

    private String token;
    private UUID workPointId;
    private UUID creatorId;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Golden Anexa1 SRL").cui("ROG" + suffix).type(CompanyType.BOTH)
                .active(true).createdAt(Instant.now()).build());
        UUID tenantId = company.getId();
        AppUser user = appUserRepository.save(AppUser.builder()
                .email("golden+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        token = jwtService.generateToken(user);
        creatorId = user.getId();
        WorkPoint workPoint = workPointRepository.save(WorkPoint.builder()
                .company(company).name("PL Golden").active(true).createdAt(Instant.now()).build());
        workPointId = workPoint.getId();
        // Non-hazardous, ca să nu se complice textul tipărit cu asteriscul — deja probat separat
        // în Anexa1FormIT.
        WasteCode code = wasteCodeRepository.findAll().stream()
                .filter(c -> !c.isHazardous()).findFirst().orElseThrow();
        Partner collector = partnerRepository.save(Partner.builder()
                .company(company).name("Colector Golden SRL").cui("RO" + suffix)
                .type(PartnerType.COLLECTOR).supplier(true).active(true)
                .createdAt(Instant.now()).build());

        // Patru cifre de intrare, toate distincte, şi cele patru solduri de închidere care rezultă
        // din ele — tot distincte. Stoc: 2500 (ian) -> 1600 (feb, -900) -> 1400 (mar, -200)
        // -> 1250 (apr, -150), neschimbat până în decembrie.
        save(company, workPoint, code, LocalDate.of(YEAR, 1, 10), "2500.000",
                WasteOperation.GENERATED, null, null);
        save(company, workPoint, code, LocalDate.of(YEAR, 2, 5), "900.000",
                WasteOperation.RECOVERED, WasteOperationCode.R3, collector);
        save(company, workPoint, code, LocalDate.of(YEAR, 3, 8), "200.000",
                WasteOperation.DISPOSED, WasteOperationCode.D5, null);
        save(company, workPoint, code, LocalDate.of(YEAR, 4, 12), "150.000",
                WasteOperation.UNCLASSIFIED_OUT, null, collector);

        mockMvc.perform(post("/api/v1/evidences/regenerate?year=" + YEAR)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    /**
     * Rânduri întregi, nu cifre izolate — dintr-un dump real al textului extras. O simplă
     * {@code contains} pe fiecare cifră, separat, nu prinde o coloană interschimbată: o probă
     * făcută aşa a fost rulată cu "Generat" scriind {@code row.disposed()} în loc de
     * {@code row.generated()}, şi tot a trecut — fiindcă fiecare cifră scrisă greşit se găsea
     * oricum <em>altundeva</em> pe pagină (soldul, sau coloana ei corectă din altă lună). Un rând
     * întreg, cu cele patru coloane în ordinea lor exactă, cade la orice interschimbare, fiindcă
     * un rând cu coloane schimbate nu se potriveşte cu niciun şir aşteptat, chiar dacă fiecare
     * cifră a lui, luată separat, apare undeva.
     */
    @Test
    void theStockLedgerPrintsTheExactFiguresTheEngineComputed() throws Exception {
        String text = pageText();

        // Cap. 1: Nr. | Luna | Generat | Recuperat | Eliminat | Stoc — patru cifre distincte, şi
        // soldul cumulativ care le urmează pe fiecare.
        assertThat(text).contains("1 Ianuarie 2500.000 0.000 0.000 2500.000");
        assertThat(text).contains("2 Februarie 0.000 900.000 0.000 1600.000");
        assertThat(text).contains("3 Martie 0.000 0.000 200.000 1400.000");
        // Ieşirea neclasificată de 150 kg (aprilie) n-are coloană proprie în cap. 1 — formularul
        // tipăreşte doar Generat | Recuperat | Eliminat | Stoc — deci efectul ei se vede numai în
        // soldul de închidere (1250, nu 1400), nu într-o cifră separată de 150.
        assertThat(text).contains("4 Aprilie 0.000 0.000 0.000 1250.000");
        // Neschimbat până în decembrie — nicio mişcare după aprilie.
        assertThat(text).contains("12 Decembrie 0.000 0.000 0.000 1250.000");
        // Rândul TOTAL AN: suma anuală pe fiecare coloană, şi soldul final — scris fără spaţii
        // între celule în extragere, spre deosebire de rândurile lunare.
        assertThat(text).contains("TOTAL AN2500.000900.000200.0001250.000");

        // Soldul de deschidere al anului, din antet: zero, fiindcă tenantul e nou.
        assertThat(text).contains("Stoc/kg: 0.000");
    }

    /** Ce n-a intrat în calcul nu trebuie să apară tipărit — a doua jumătate a probei. */
    @Test
    void figuresThatWereNeverEnteredNeverAppearPrinted() throws Exception {
        String text = pageText();
        // Nicio lună n-a avut 3000 sau 1000 kg pe nicio coloană a acestei fişe. (Nu şi 500 — e
        // sub-şir al lui 2500.000, care chiar e tipărit.)
        assertThat(text).doesNotContain("3000.000");
        assertThat(text).doesNotContain("1000.000");
    }

    private String pageText() throws Exception {
        byte[] pdf = mockMvc.perform(get("/api/v1/evidences/anexa1?year=" + YEAR
                                + "&workPointId=" + workPointId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        PdfReader reader = new PdfReader(pdf);
        try {
            StringBuilder all = new StringBuilder();
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                all.append(extractor.getTextFromPage(page)).append('\n');
            }
            return all.toString();
        } finally {
            reader.close();
        }
    }

    private void save(Company company, WorkPoint workPoint, WasteCode code, LocalDate date,
                       String qty, WasteOperation op, WasteOperationCode opCode, Partner partner) {
        movementRepository.save(WasteMovement.builder()
                .company(company).workPoint(workPoint).date(date).wasteCode(code)
                .quantity(new BigDecimal(qty)).unit(Unit.KG).operation(op).operationCode(opCode)
                .partner(partner).deleted(false).createdBy(creatorId).build());
    }
}
