package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.security.TenantFilter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comutatorul de tenant al lui {@code PLATFORM_ADMIN} — §10 din planul de audit.
 *
 * <p>E singurul loc din aplicaţie unde cineva trece legitim dintr-o firmă în alta, deci e şi
 * singurul unde „datele rămase din firma dinainte" e un scenariu real şi nu o vorbă. Serviciul e
 * „done-for-you": omul de la platformă chiar face treaba mai multor clienţi într-o după-amiază, cu
 * acelaşi tab deschis. Dacă la a treia comutare vede jumătate din datele celui de-al doilea,
 * greşeala nu rămâne pe ecran — ajunge într-un document oficial depus la agenţie, pe numele
 * clientului greşit.
 *
 * <p><b>Ce se probează, deci:</b> A→B→A, cu citire completă a conţinutului după fiecare pas, nu
 * doar a codului de răspuns. Plus marginile antetului: lipsă, stricat, necunoscut, şi — cel mai
 * important — trimis de cineva care n-are voie să-l folosească.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class PlatformAdminTenantSwitchIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired PartnerRepository partnerRepository;

    private String platformToken;
    private String adminAToken;
    private UUID companyA;
    private UUID companyB;

    @BeforeEach
    void setUp() {
        AppUser platform = appUserRepository.findByEmail("platform@ecoregistru.ro").orElseThrow();
        platformToken = jwtService.generateToken(platform);

        WasteCode code = wasteCodeRepository.findAll().get(0);
        companyA = seedCompany("Alfa Salubritate SRL", "Partener Alfa", code);
        companyB = seedCompany("Beta Reciclare SRL", "Partener Beta", code);

        // Un ADMIN obişnuit al lui A, ca să probăm că antetul nu funcţionează pentru el.
        AppUser adminA = appUserRepository.save(AppUser.builder()
                .email("admin+" + UUID.randomUUID().toString().substring(0, 8) + "@alfa.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.ADMIN).company(companyRepository.findById(companyA).orElseThrow())
                .enabled(true).createdAt(Instant.now()).build());
        adminAToken = jwtService.generateToken(adminA);
    }

    private UUID seedCompany(String name, String partnerName, WasteCode code) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name(name).cui("RO" + suffix).type(CompanyType.GENERATOR)
                .active(true).createdAt(Instant.now()).build());
        WorkPoint wp = workPointRepository.save(WorkPoint.builder()
                .company(company).name("PL " + name).active(true).createdAt(Instant.now()).build());
        partnerRepository.save(Partner.builder()
                .company(company).name(partnerName).client(true).supplier(false).carrier(false)
                .type(PartnerType.COLLECTOR).active(true).createdAt(Instant.now()).build());
        movementRepository.save(WasteMovement.builder()
                .company(company).workPoint(wp).date(LocalDate.now()).wasteCode(code)
                .quantity(new BigDecimal("42.000")).unit(Unit.KG).operation(WasteOperation.GENERATED)
                .deleted(false).createdBy(UUID.randomUUID()).build());
        return company.getId();
    }

    /**
     * Drumul complet: A → B → A, cu proba în ambele sensuri la fiecare pas.
     *
     * <p>Ce caută cu adevărat testul e pasul al treilea. Primele două ar trece şi pe o aplicaţie
     * care ţine un cache lipicios; numai revenirea la A arată dacă tenantul chiar se recalculează
     * la fiecare cerere sau doar la prima.
     */
    @Test
    void switchingBackAndForthNeverMixesTheTwoTenants() throws Exception {
        seesOnly("Alfa", companyA);
        seesOnly("Beta", companyB);
        seesOnly("Alfa", companyA);
        seesOnly("Beta", companyB);
    }

    private void seesOnly(String mine, UUID tenant) throws Exception {
        String theirs = mine.equals("Alfa") ? "Beta" : "Alfa";
        for (String url : new String[]{"/api/v1/partners", "/api/v1/movements", "/api/v1/work-points"}) {
            mockMvc.perform(get(url)
                            .header("Authorization", "Bearer " + platformToken)
                            .header(TenantFilter.TENANT_HEADER, tenant.toString()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(mine)))
                    .andExpect(content().string(not(containsString(theirs))));
        }
    }

    /**
     * Fără antet, un {@code PLATFORM_ADMIN} n-are nicio firmă — şi ce contează e că răspunsul e o
     * eroare, nu o listă. O listă goală ar fi fost tot „fără scurgere", dar ar fi minţit: ar arăta
     * ca o firmă fără date, când de fapt nu s-a ales nicio firmă.
     */
    @Test
    void withoutTheHeaderThereIsNoTenantAndNoData() throws Exception {
        mockMvc.perform(get("/api/v1/partners").header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("tenant.required")));
    }

    /**
     * Antet stricat. {@code TenantFilter} prinde {@code IllegalArgumentException}, scrie un warning
     * şi merge mai departe fără tenant — deci trebuie să se termine ca şi cum antetul ar fi lipsit.
     * Ce nu are voie să se întâmple e să rămână în picioare tenantul de la cererea dinainte.
     */
    @Test
    void aMalformedHeaderFallsBackToNoTenantRatherThanTheLastOne() throws Exception {
        // Întâi o cerere reuşită pe A, ca să existe un „tenant dinainte" de moştenit.
        mockMvc.perform(get("/api/v1/partners")
                        .header("Authorization", "Bearer " + platformToken)
                        .header(TenantFilter.TENANT_HEADER, companyA.toString()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/partners")
                        .header("Authorization", "Bearer " + platformToken)
                        .header(TenantFilter.TENANT_HEADER, "nu-e-un-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("Alfa"))));
    }

    /** Un identificator valid dar necunoscut: firmă goală, nu eroare şi mai ales nu datele altcuiva. */
    @Test
    void anUnknownTenantIdYieldsNothing() throws Exception {
        mockMvc.perform(get("/api/v1/partners")
                        .header("Authorization", "Bearer " + platformToken)
                        .header(TenantFilter.TENANT_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Alfa"))))
                .andExpect(content().string(not(containsString("Beta"))));
    }

    /**
     * <b>Cel mai important test din clasă.</b> Antetul e citit numai pe ramura
     * {@code PLATFORM_ADMIN} din {@code TenantFilter}. Dacă asta s-ar strica vreodată, orice ADMIN
     * ar putea ieşi din firma lui trimiţând un antet — adică exact scurgerea completă între
     * clienţi, cu o singură linie de {@code curl} şi fără nimic de ghicit în afară de un id de
     * firmă.
     */
    @Test
    void anOrdinaryAdminCannotUseTheHeaderToLeaveTheirCompany() throws Exception {
        mockMvc.perform(get("/api/v1/partners")
                        .header("Authorization", "Bearer " + adminAToken)
                        .header(TenantFilter.TENANT_HEADER, companyB.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Alfa")))
                .andExpect(content().string(not(containsString("Beta"))));
    }
}
