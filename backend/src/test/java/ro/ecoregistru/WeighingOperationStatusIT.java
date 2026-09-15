package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.controller.request.WeighingLinesRequest;
import ro.ecoregistru.controller.request.WeighingLinesRequest.Line;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.controller.response.WeighingOperationResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.WeighingOperationStatus;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteArticleRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.WeighingOperationService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ro.ecoregistru.enums.WeighingOperationType.IN;

/**
 * D1.5 — stările. În lucru → Finalizată (doar admin și consultant) → nemodificabilă; Anulată cu
 * motiv, cine și când, fără ștergere. Liniile contează doar cât operațiunea e finalizată.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class WeighingOperationStatusIT {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 15);

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired WeighingOperationService service;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired WasteArticleRepository articleRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;

    Company company;
    AppUser admin;
    AppUser operator;
    AppUser consultant;
    WorkPoint depot;
    Partner partner;
    WasteArticle cardboard;

    @BeforeEach
    void setUp() {
        company = companyRepository.save(Company.builder()
                .name("Stări " + suffix() + " SRL").cui("ROT" + suffix()).type(CompanyType.COLLECTOR)
                .active(true).createdAt(Instant.now()).build());
        admin = user(Role.ADMIN);
        operator = user(Role.OPERATOR);
        // Un consultant are cabinet, nu firmă (`app_users_consultant_scope`), deci nu se salvează aici:
        // serviciul citește doar rolul din principal, iar `finalized_by` n-are cheie străină.
        consultant = AppUser.builder().id(UUID.randomUUID()).email("consultant@demo.ro")
                .role(Role.CONSULTANT).enabled(true).build();
        depot = workPointRepository.save(WorkPoint.builder()
                .company(company).name("Depozit Baciu").active(true).createdAt(Instant.now()).build());
        partner = partnerRepository.save(Partner.builder()
                .company(company).name("Magazin Alfa SRL").cui("RO" + suffix())
                .type(PartnerType.GENERATOR).packagingOrigin(PackagingOrigin.GENERATOR_PJ)
                .active(true).createdAt(Instant.now()).build());
        cardboard = articleRepository.save(WasteArticle.builder()
                .company(company).wasteCode(wasteCodeRepository.findAll().get(0))
                .name("Carton").active(true).createdAt(Instant.now()).build());
        actAs(admin);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void finalizingMakesTheLinesCount() {
        UUID id = weighedOperation("300");
        assertThat(counted()).isEmpty();

        WeighingOperationResponse done = service.finalizeOperation(id);

        assertThat(done.status()).isEqualTo(WeighingOperationStatus.FINALIZED);
        assertThat(counted()).containsExactly(300);
    }

    @Test
    void anOperationWithoutLinesIsNotFinalized() {
        UUID id = service.create(head()).id();
        assertThatThrownBy(() -> service.finalizeOperation(id))
                .isInstanceOf(BusinessException.class).hasMessageContaining("fără linii");
    }

    @Test
    void aFinalizedOperationIsNotFinalizedAgain() {
        UUID id = weighedOperation("300");
        service.finalizeOperation(id);
        assertThatThrownBy(() -> service.finalizeOperation(id))
                .isInstanceOf(BusinessException.class).hasMessageContaining("nu se mai modifică");
    }

    @Test
    void aConsultantMayFinalize() {
        UUID id = weighedOperation("300");
        actAs(consultant);
        assertThat(service.finalizeOperation(id).status()).isEqualTo(WeighingOperationStatus.FINALIZED);
    }

    @Test
    void anOperatorWeighsButNeitherFinalizesNorCancels() {
        actAs(operator);
        UUID id = weighedOperation("300");

        assertThatThrownBy(() -> service.finalizeOperation(id)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.cancel(id, "greșit")).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cancellingAFinalizedOperationTakesItOutButKeepsEverything() {
        UUID id = weighedOperation("300");
        service.finalizeOperation(id);

        WeighingOperationResponse cancelled = service.cancel(id, "  Cântărire dublă  ");

        assertThat(cancelled.status()).isEqualTo(WeighingOperationStatus.CANCELLED);
        assertThat(cancelled.lines()).hasSize(1);
        assertThat(counted()).isEmpty();
        var stored = service.get(id);
        assertThat(stored.status()).isEqualTo(WeighingOperationStatus.CANCELLED);
        assertThat(movementRepository.findAllByWeighingOperation_IdOrderByLineNoAsc(id)).hasSize(1);
    }

    @Test
    void aCancellationNeedsAReason() {
        UUID id = weighedOperation("300");
        assertThatThrownBy(() -> service.cancel(id, "   "))
                .isInstanceOf(BusinessException.class).hasMessageContaining("motivul");
    }

    @Test
    void aCancelledOperationIsNotCancelledTwice() {
        UUID id = weighedOperation("300");
        service.cancel(id, "Dublură");
        assertThatThrownBy(() -> service.cancel(id, "Din nou"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("deja anulată");
    }

    /** Izolarea între firme: operațiunea altei firme nu există pentru nimic din ce scrie sau citește. */
    @Test
    void anotherCompanysOperationIsNotFoundAnywhere() {
        UUID foreign = weighedOperation("300");
        Company other = companyRepository.save(Company.builder()
                .name("Altă firmă " + suffix() + " SRL").cui("ROX" + suffix()).type(CompanyType.COLLECTOR)
                .active(true).createdAt(Instant.now()).build());
        AppUser otherAdmin = appUserRepository.save(AppUser.builder()
                .email("alt+" + suffix() + "@demo.ro").password("x")
                .role(Role.ADMIN).company(other).enabled(true).createdAt(Instant.now()).build());
        TenantContext.set(other.getId());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(otherAdmin, null, List.of()));

        assertThatThrownBy(() -> service.get(foreign)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.replaceLines(foreign, new WeighingLinesRequest(null, null, List.of(
                new Line(cardboard.getId(), null, null, new BigDecimal("1"), null, null, null, null)))))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.finalizeOperation(foreign)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.cancel(foreign, "furt")).isInstanceOf(NotFoundException.class);
        assertThat(service.list()).isEmpty();

        actAs(admin);
        assertThat(service.get(foreign).status()).isEqualTo(WeighingOperationStatus.IN_PROGRESS);
    }

    /** Aceeași regulă pe HTTP: operatorul primește 403, adminul trece. */
    @Test
    void theEndpointRefusesTheOperatorAndAcceptsTheAdmin() throws Exception {
        UUID id = weighedOperation("300");
        // Autentificarea pusă de `actAs` ar rămâne pe thread și filtrul JWT n-ar mai citi tokenul:
        // toate cererile ar pleca fără autorități, iar 403-ul operatorului n-ar dovedi nimic.
        SecurityContextHolder.clearContext();
        TenantContext.clear();

        mockMvc.perform(post("/api/v1/weighing-operations/" + id + "/finalize")
                        .header("Authorization", "Bearer " + jwtService.generateToken(operator)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/weighing-operations/" + id + "/cancel")
                        .header("Authorization", "Bearer " + jwtService.generateToken(operator))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"x\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/weighing-operations/" + id + "/finalize")
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZED"));
    }

    // --- helpers ---

    private UUID weighedOperation(String kg) {
        UUID id = service.create(head()).id();
        service.replaceLines(id, new WeighingLinesRequest(null, null, List.of(
                new Line(cardboard.getId(), null, null, new BigDecimal(kg), null, null, null, null))));
        return id;
    }

    private List<Integer> counted() {
        return movementRepository.findCountedBetween(company.getId(), DAY, DAY).stream()
                .map(m -> m.getQuantity().intValue()).toList();
    }

    private WeighingOperationRequest head() {
        return new WeighingOperationRequest(IN, depot.getId(), DAY, partner.getId(),
                null, null, null, null, null, null, null);
    }

    private AppUser user(Role role) {
        return appUserRepository.save(AppUser.builder()
                .email(role.name().toLowerCase() + "+" + suffix() + "@demo.ro").password("x")
                .role(role).company(company).enabled(true).createdAt(Instant.now()).build());
    }

    private void actAs(AppUser user) {
        TenantContext.set(company.getId());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
