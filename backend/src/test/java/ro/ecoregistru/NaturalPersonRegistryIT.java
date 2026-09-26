package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.controller.request.NaturalPersonRequest;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.controller.response.NaturalPersonResponse;
import ro.ecoregistru.controller.response.NaturalPersonSummary;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.ErrorMessageEnum;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.NaturalPersonRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.NaturalPersonService;
import ro.ecoregistru.service.WeighingOperationService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ro.ecoregistru.enums.WeighingOperationType.IN;

/**
 * D1.7b — tabul „Persoane fizice”: CNP-ul mascat în listă, fișa întreagă doar pentru cine scrie, CNP
 * valid și unic pe firmă, izolarea între firme și ștergerea definitivă doar fără operațiuni
 * (Legea 82/1991 art. 25: borderoul se păstrează 10 ani).
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class NaturalPersonRegistryIT {

    /** CNP-uri valide (cifra de control), aceleași ca în `NaturalPersonIT`. */
    private static final String CNP = "1900101123457";
    private static final String OTHER_CNP = "2900101123459";

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired NaturalPersonService service;
    @Autowired WeighingOperationService operations;
    @Autowired NaturalPersonRepository personRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;

    Company company;
    AppUser admin;

    @BeforeEach
    void setUp() {
        company = company("Registru PF");
        admin = user(company, Role.ADMIN);
        actAs(company, admin);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void theListShowsOnlyTheLastFourDigitsAndWhetherMetalCanBeBought() {
        service.create(new NaturalPersonRequest("Ion Popescu", CNP, "CJ 123456", "Cluj, str. X 1"));
        service.create(new NaturalPersonRequest("Ana Doar Nume", null, null, null));

        List<NaturalPersonSummary> list = service.list();

        assertThat(list).extracting(NaturalPersonSummary::name).containsExactly("Ana Doar Nume", "Ion Popescu");
        assertThat(list.get(1).cnpLastDigits()).isEqualTo("3457");
        assertThat(list.get(1).metalReady()).isTrue();
        assertThat(list.get(0).cnpLastDigits()).isNull();
        assertThat(list.get(0).metalReady()).isFalse();
    }

    /** Pe HTTP: vizualizatorul vede lista fără CNP întreg și fără act, iar fișa întreagă nu o primește. */
    @Test
    void theViewerReadsTheMaskedListButNotTheFullRecordOverHttp() throws Exception {
        UUID id = service.create(new NaturalPersonRequest("Ion Popescu", CNP, "CJ 123456", "Cluj, str. X 1")).id();
        AppUser viewer = user(company, Role.CLIENT_VIEWER);
        clearThread();

        String body = mockMvc.perform(get("/api/v1/natural-persons").header("Authorization", bearer(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cnpLastDigits").value("3457"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).as("controlul pozitiv: persoana chiar e în răspuns").contains("Ion Popescu");
        assertThat(body).doesNotContain(CNP).doesNotContain("CJ 123456");

        mockMvc.perform(get("/api/v1/natural-persons/" + id).header("Authorization", bearer(viewer)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/natural-persons/" + id).header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnp").value(CNP));
    }

    /** Operatorul de la cântar primește omul, deci îi face și fișa; vizualizatorul nu. */
    @Test
    void theOperatorAddsAPersonAndTheViewerCannotOverHttp() throws Exception {
        AppUser operator = user(company, Role.OPERATOR);
        AppUser viewer = user(company, Role.CLIENT_VIEWER);
        String body = "{\"name\":\"Maria Ionescu\",\"cnp\":\"" + CNP + "\"}";
        clearThread();

        mockMvc.perform(post("/api/v1/natural-persons").header("Authorization", bearer(viewer))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/natural-persons").header("Authorization", bearer(operator))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria Ionescu"));
    }

    /** O cifră de control greșită e CNP-ul altcuiva; formularul trebuie să se oprească, nu să salveze. */
    @Test
    void anInvalidCnpIsRefusedOverHttp() throws Exception {
        clearThread();

        mockMvc.perform(post("/api/v1/natural-persons").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Greșit\",\"cnp\":\"1900101123458\"}"))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(post("/api/v1/natural-persons").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Corect\",\"cnp\":\"" + CNP + "\"}"))
                .andExpect(status().isOk());

        actAs(company, admin);
        assertThat(service.list()).extracting(NaturalPersonSummary::name).containsExactly("Corect");
    }

    @Test
    void theNameIsRequired() {
        assertThatThrownBy(() -> service.create(new NaturalPersonRequest("  ", CNP, null, null)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.NATURAL_PERSON_NAME_REQUIRED));
    }

    /** Unic pe firmă, cu mesaj, nu cu eroarea indexului; propriul CNP la editare nu e duplicat; altă firmă e liberă. */
    @Test
    void theCnpIsUniqueInTheFirmOnly() {
        NaturalPersonResponse first = service.create(new NaturalPersonRequest("Ion", CNP, null, null));
        NaturalPersonResponse second = service.create(new NaturalPersonRequest("Vasile", OTHER_CNP, null, null));

        assertThatThrownBy(() -> service.create(new NaturalPersonRequest("Dublură", CNP, null, null)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.NATURAL_PERSON_CNP_TAKEN));
        assertThatThrownBy(() -> service.update(second.id(), new NaturalPersonRequest("Vasile", CNP, null, null)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.NATURAL_PERSON_CNP_TAKEN));
        assertThat(service.update(first.id(), new NaturalPersonRequest("Ion Pop", CNP, "CJ 1", null)).name())
                .isEqualTo("Ion Pop");

        actAs(company("Alta"), admin);
        assertThat(service.create(new NaturalPersonRequest("Ion la alt depozit", CNP, null, null)).cnp()).isEqualTo(CNP);
    }

    @Test
    void anotherFirmsPersonIsNotFoundAndNotListed() {
        UUID foreign = service.create(new NaturalPersonRequest("Străin", CNP, "CJ 9", "Adresa")).id();

        actAs(company("Vecina"), user(company, Role.ADMIN));

        assertThat(service.list()).isEmpty();
        assertThatThrownBy(() -> service.get(foreign)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.update(foreign, new NaturalPersonRequest("X", null, null, null)))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.deactivate(foreign)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.delete(foreign)).isInstanceOf(NotFoundException.class);
        assertThat(personRepository.findById(foreign)).isPresent();
    }

    /**
     * Ștergerea definitivă: întâi dezactivare, apoi doar fără operațiuni. Controlul pozitiv e o fișă
     * dezactivată și nefolosită, care chiar dispare.
     */
    @Test
    void aPersonOnAnOperationIsNeverDeletedOnlyDeactivated() {
        UUID sold = service.create(new NaturalPersonRequest("A vândut", null, null, null)).id();
        UUID mistake = service.create(new NaturalPersonRequest("Introdusă greșit", null, null, null)).id();
        WorkPoint depot = workPointRepository.save(WorkPoint.builder()
                .company(company).name("Depozit").active(true).createdAt(Instant.now()).build());
        operations.create(new WeighingOperationRequest(IN, depot.getId(), LocalDate.of(2026, 9, 15), null, sold,
                null, null, null, null, null, null, null, null, null, null, null));

        assertThatThrownBy(() -> service.delete(mistake))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.NATURAL_PERSON_DELETE_REQUIRES_DEACTIVATION));

        service.deactivate(sold);
        service.deactivate(mistake);
        assertThatThrownBy(() -> service.delete(sold))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.NATURAL_PERSON_HAS_OPERATIONS));
        service.delete(mistake);

        assertThat(personRepository.findById(sold)).isPresent();
        assertThat(personRepository.findById(mistake)).isEmpty();
        assertThat(service.list()).singleElement().satisfies(p -> {
            assertThat(p.hasOperations()).isTrue();
            assertThat(p.active()).isFalse();
        });
        service.reactivate(sold);
        assertThat(service.get(sold).active()).isTrue();
    }

    // --- helpers ---

    /** Capcana din `WeighingOperationStatusIT`: o autentificare lăsată pe thread oprește filtrul JWT. */
    private static void clearThread() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private String bearer(AppUser user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    private Company company(String name) {
        return companyRepository.save(Company.builder()
                .name(name + " " + suffix() + " SRL").cui("ROR" + suffix()).type(CompanyType.COLLECTOR)
                .active(true).createdAt(Instant.now()).build());
    }

    private AppUser user(Company owner, Role role) {
        return appUserRepository.save(AppUser.builder()
                .email("pfreg+" + suffix() + "@demo.ro").password("x")
                .role(role).company(owner).enabled(true).createdAt(Instant.now()).build());
    }

    private void actAs(Company owner, AppUser user) {
        TenantContext.set(owner.getId());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
