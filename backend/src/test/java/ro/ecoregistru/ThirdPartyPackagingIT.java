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
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PackagingMaterial;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.PackagingService;
import ro.ecoregistru.service.export.PackagingDeclaration;

import java.time.Instant;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Marfa preluată de la terţi nu e deşeul firmei, deci nu intră în Anexa 1 — nici în fişa de
 * gestiune, nici în declaraţia de ambalaje.
 *
 * <p><b>De ce există clasa asta.</b> Regula era pe jumătate aplicată. Preluarea ({@code COLLECTED})
 * era forţată pe registrul art. 48 şi rămânea corect afară. <b>Ieşirea</b> nu era întrebată nimic:
 * un reciclator care valorifica marfa preluată înregistra un {@code RECOVERED}, implicitul îl punea
 * pe Anexa 1, iar cantitatea ajungea în <i>tabelul 1</i> al Anexei 1 Ambalaje — adică se declara
 * drept ambalaj pus pe piaţă de el. Probat pe 25.08.2026 cu scenariul din întrebarea
 * utilizatorului: 1000 kg de {@code 15 01 01} luaţi de la un magazin şi valorificaţi ieşeau
 * <b>1000,000 pe rândul Hârtie carton</b>. Cu generarea dedusă din {@code V24}, fişa de gestiune îi
 * mai spunea şi „generate de tine".
 *
 * <p>Temeiul e HG 856/2002 art. 2 alin. (1): un operator autorizat ţine Anexa 1 <i>„numai pentru
 * deşeurile generate în cadrul activităţilor proprii"</i>. Raportul pentru marfa preluată e altul —
 * anexa nr. 3 la Ordinul 794/2012, a colectorilor şi comercianţilor de deşeuri de ambalaje.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class ThirdPartyPackagingIT {

    private static final int YEAR = 2026;

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PackagingService packagingService;

    private String token;
    private UUID tenantId;
    private UUID workPointId;
    private UUID cardboardId;
    private Partner generator;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Reciclator " + suffix).cui("ROR" + suffix)
                .type(CompanyType.COLLECTOR)
                .address("Cluj-Napoca, str. Fabricii nr. 3").caenCode("3832")
                .active(true).createdAt(Instant.now()).build());
        tenantId = company.getId();
        AppUser admin = appUserRepository.save(AppUser.builder()
                .email("rec+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true)
                .createdAt(Instant.now()).build());
        token = jwtService.generateToken(admin);
        workPointId = workPointRepository.save(ro.ecoregistru.entity.WorkPoint.builder()
                .company(company).name("Depozit").active(true).createdAt(Instant.now()).build())
                .getId();
        generator = partnerRepository.save(Partner.builder()
                .company(company).name("Magazin Generator SRL").cui("RO999" + suffix.substring(0, 3))
                .type(PartnerType.GENERATOR).supplier(true).active(true)
                .createdAt(Instant.now()).build());
        cardboardId = wasteCodeRepository.findByCode("15 01 01").orElseThrow().getId();
    }

    /**
     * Aceeaşi valorificare, cu acelaşi cod R, poate fi a deşeului propriu sau a mărfii preluate —
     * operaţiunea nu spune care. Deci se întreabă, la conturile care chiar pot prelua.
     */
    @Test
    void anExitWithoutTheOriginIsRefusedOnAnAccountThatTakesWasteOver() throws Exception {
        mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workPointId": "%s", "date": "%d-05-10", "wasteCodeId": "%s",
                                 "unit": "KG", "quantity": 1000, "operation": "RECOVERED",
                                 "operationCode": "R3", "packagingCategory": "SECONDARY"}
                                """.formatted(workPointId, YEAR, cardboardId)))
                .andExpect(status().isBadRequest());
    }

    /** Marfa preluată şi valorificată rămâne în afara ambelor tabele ale Anexei 1 Ambalaje. */
    @Test
    void thirdPartyPackagingStaysOutOfTheDeclaration() throws Exception {
        collect("1000");
        exit("1000", "ART_48", null);

        PackagingDeclaration d = declaration();

        assertThat(row(d, PackagingMaterial.HARTIE_CARTON).isEmpty()).isTrue();
        assertThat(d.handoverRows()).isEmpty();
        assertThat(d.unclassified()).isEmpty();
        // Se vede totuşi în registrul tabului — ambele mişcări sunt pe cod de ambalaje.
        assertThat(movements()).hasSize(2);
    }

    /** Deşeul propriu al aceleiaşi firme intră normal: regula separă registrele, nu firmele. */
    @Test
    void theCompanyOwnPackagingWasteStillCounts() throws Exception {
        collect("1000");
        exit("1000", "ART_48", null);
        exit("40", "ANEXA_1", null);

        PackagingDeclaration d = declaration();

        assertThat(row(d, PackagingMaterial.HARTIE_CARTON).secondaryTotal())
                .isEqualByComparingTo("40");
    }

    /** Preluarea în sine n-a putut niciodată să ceară Anexa 1, şi rămâne aşa. */
    @Test
    void aTakeoverMayNotClaimAnexa1() throws Exception {
        mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workPointId": "%s", "date": "%d-04-10", "wasteCodeId": "%s",
                                 "unit": "KG", "quantity": 1000, "operation": "COLLECTED",
                                 "partnerId": "%s", "register": "ANEXA_1"}
                                """.formatted(workPointId, YEAR, cardboardId, generator.getId())))
                .andExpect(status().isBadRequest());
    }

    // ---------- helpers ----------

    private void collect(String quantity) throws Exception {
        mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workPointId": "%s", "date": "%d-04-10", "wasteCodeId": "%s",
                                 "unit": "KG", "quantity": %s, "operation": "COLLECTED",
                                 "partnerId": "%s"}
                                """.formatted(workPointId, YEAR, cardboardId, quantity,
                                generator.getId())))
                .andExpect(status().isOk());
    }

    private void exit(String quantity, String register, UUID partnerId) throws Exception {
        String partner = partnerId == null ? "" : ", \"partnerId\": \"" + partnerId + "\"";
        mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workPointId": "%s", "date": "%d-05-10", "wasteCodeId": "%s",
                                 "unit": "KG", "quantity": %s, "operation": "RECOVERED",
                                 "operationCode": "R3", "register": "%s",
                                 "packagingCategory": "SECONDARY"%s}
                                """.formatted(workPointId, YEAR, cardboardId, quantity, register,
                                partner)))
                .andExpect(status().isOk());
    }

    private PackagingDeclaration.MarketRow row(PackagingDeclaration d, PackagingMaterial material) {
        return d.marketRows().stream()
                .filter(r -> r.material() == material)
                .findFirst().orElseThrow();
    }

    private PackagingDeclaration declaration() {
        TenantContext.set(tenantId);
        try {
            return packagingService.declaration(YEAR);
        } finally {
            TenantContext.clear();
        }
    }

    private java.util.List<?> movements() {
        TenantContext.set(tenantId);
        try {
            return packagingService.movements(YEAR);
        } finally {
            TenantContext.clear();
        }
    }
}
