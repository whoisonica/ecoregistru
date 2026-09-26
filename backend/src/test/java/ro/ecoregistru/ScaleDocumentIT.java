package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.controller.request.ScaleEventRequest;
import ro.ecoregistru.controller.request.ScaleRequest;
import ro.ecoregistru.controller.request.UserWorkPointsRequest;
import ro.ecoregistru.controller.response.ScaleResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.ScaleEventKind;
import ro.ecoregistru.exception.BadRequestException;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.ErrorMessageEnum;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.CloudinaryStorageService;
import ro.ecoregistru.service.CompanyUserService;
import ro.ecoregistru.service.DeadlineService;
import ro.ecoregistru.service.ScaleService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * D2.3, restul — fișierele cântarului (V70): buletinul pe rândul verificării și dovada BRML pe cântar. Câte unul
 * pe loc (al doilea îl înlocuiește și șterge activul vechi), buletinul doar la verificare, conținutul prin API,
 * ștergerea rândului sau a cântarului ia și fișierul, iar un operator restrâns la alt depozit nu-l vede (D2.4).
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class ScaleDocumentIT {

    static final byte[] PDF = "%PDF-1.4 buletin".getBytes();

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired ScaleService service;
    @Autowired CompanyUserService users;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;

    @MockitoBean CloudinaryStorageService storage;

    LocalDate today;
    Company company;
    AppUser admin;
    WorkPoint depot;
    int uploads;

    @BeforeEach
    void setUp() throws Exception {
        today = DeadlineService.today();
        company = companyRepository.save(Company.builder()
                .name("Fișiere " + suffix() + " SRL").cui("ROF" + suffix()).type(CompanyType.COLLECTOR)
                .active(true).createdAt(Instant.now()).build());
        admin = user(Role.ADMIN);
        depot = depot();
        when(storage.upload(any(), anyString())).thenAnswer(inv -> new CloudinaryStorageService.StoredFile(
                "https://res.cloudinary.com/x/f.pdf", "wh/scales/f" + (++uploads), "image", "authenticated", "pdf"));
        when(storage.signedUrl(anyString(), any(), any(), any())).thenReturn("https://signed");
        when(storage.fetch(anyString())).thenReturn(PDF);
        actAs(admin);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void theBrmlProofIsOnePerScaleAndASecondOneReplacesTheFirst() {
        ScaleResponse scale = service.create(scale(depot));
        ScaleResponse withProof = service.attach(scale.id(), null, pdf("declaratie-brml.pdf"));
        assertThat(withProof.brmlProof().fileName()).isEqualTo("declaratie-brml.pdf");

        ScaleResponse replaced = service.attach(scale.id(), null, pdf("declaratie-noua.pdf"));
        assertThat(replaced.brmlProof().fileName()).isEqualTo("declaratie-noua.pdf");
        assertThat(replaced.brmlProof().id()).isNotEqualTo(withProof.brmlProof().id());
        verify(storage).delete(eq("wh/scales/f1"), any(), any());
        assertThat(service.list()).filteredOn(s -> s.id().equals(scale.id())).singleElement()
                .satisfies(s -> assertThat(s.brmlProof().fileName()).isEqualTo("declaratie-noua.pdf"));
    }

    @Test
    void theBulletinGoesOnAVerificationOnly() {
        ScaleResponse scale = service.create(scale(depot));
        ScaleResponse verified = service.addEvent(scale.id(), event(ScaleEventKind.VERIFICATION));
        UUID verification = verified.events().get(0).id();
        ScaleResponse withBulletin = service.attach(scale.id(), verification, pdf("buletin.pdf"));
        assertThat(withBulletin.events().get(0).bulletinFile().fileName()).isEqualTo("buletin.pdf");
        assertThat(withBulletin.brmlProof()).isNull();

        UUID repair = service.addEvent(scale.id(), event(ScaleEventKind.REPAIR)).events().stream()
                .filter(e -> e.kind() == ScaleEventKind.REPAIR).findFirst().orElseThrow().id();
        assertThatThrownBy(() -> service.attach(scale.id(), repair, pdf("x.pdf")))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.SCALE_BULLETIN_ONLY_VERIFICATION));
        assertThatThrownBy(() -> service.attach(scale.id(), verification,
                new MockMultipartFile("file", "gol.pdf", "application/pdf", new byte[0])))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.attach(scale.id(), verification,
                new MockMultipartFile("file", "mare.pdf", "application/pdf", new byte[10 * 1024 * 1024 + 1])))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void deletingTheRowOrTheFileTakesTheStoredAssetToo() {
        ScaleResponse scale = service.create(scale(depot));
        UUID verification = service.addEvent(scale.id(), event(ScaleEventKind.VERIFICATION)).events().get(0).id();
        UUID bulletin = service.attach(scale.id(), verification, pdf("buletin.pdf")).events().get(0).bulletinFile().id();
        service.deleteEvent(scale.id(), verification);
        verify(storage).delete(eq("wh/scales/f1"), any(), any());

        UUID proof = service.attach(scale.id(), null, pdf("brml.pdf")).brmlProof().id();
        assertThat(service.detach(scale.id(), proof).brmlProof()).isNull();
        verify(storage).delete(eq("wh/scales/f2"), any(), any());
        assertThatThrownBy(() -> service.content(scale.id(), bulletin)).isInstanceOf(NotFoundException.class);

        service.attach(scale.id(), null, pdf("brml.pdf"));
        service.delete(scale.id());
        verify(storage).delete(eq("wh/scales/f3"), any(), any());
    }

    @Test
    void theFileIsReadThroughTheApiAndHiddenFromAnotherDepot() throws Exception {
        ScaleResponse scale = service.create(scale(depot));
        UUID proof = service.attach(scale.id(), null, pdf("brml.pdf")).brmlProof().id();
        assertThat(service.content(scale.id(), proof).bytes()).isEqualTo(PDF);

        AppUser operator = user(Role.OPERATOR);
        WorkPoint other = depot();
        users.changeWorkPoints(operator.getId(), new UserWorkPointsRequest(false, List.of(other.getId())));
        actAs(operator);
        assertThatThrownBy(() -> service.content(scale.id(), proof))
                .isInstanceOfSatisfying(NotFoundException.class,
                        e -> assertThat(e.getError()).isEqualTo(ErrorMessageEnum.SCALE_NOT_FOUND));

        // HTTP: PDF-ul se arată în pagină; vizualizatorul citește, dar nu urcă.
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        AppUser viewer = user(Role.CLIENT_VIEWER);
        mockMvc.perform(get("/api/v1/scales/" + scale.id() + "/documents/" + proof)
                        .header("Authorization", "Bearer " + jwtService.generateToken(viewer)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
        mockMvc.perform(multipart("/api/v1/scales/" + scale.id() + "/brml-proof").file(pdf("x.pdf"))
                        .header("Authorization", "Bearer " + jwtService.generateToken(viewer)))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------------------------------------

    private static MockMultipartFile pdf(String name) {
        return new MockMultipartFile("file", name, "application/pdf", PDF);
    }

    private ScaleRequest scale(WorkPoint wp) {
        return new ScaleRequest(wp.getId(), "Pod " + suffix(), "SN-" + suffix(), "Pod basculă 60 t", "III",
                new BigDecimal("20"), today.minusYears(2), today.minusYears(2), "BRML-" + suffix(), null);
    }

    private ScaleEventRequest event(ScaleEventKind kind) {
        return new ScaleEventRequest(kind, today.minusMonths(1), kind == ScaleEventKind.VERIFICATION ? true : null,
                kind == ScaleEventKind.VERIFICATION ? "B-1" : null, null, "Laborator", "ing. Pop", null);
    }

    private WorkPoint depot() {
        return workPointRepository.save(WorkPoint.builder()
                .company(company).name("Depozit " + suffix()).active(true).createdAt(Instant.now()).build());
    }

    private AppUser user(Role role) {
        return appUserRepository.save(AppUser.builder()
                .email("fisier+" + suffix() + "@demo.ro").password("x")
                .role(role).company(company).enabled(true).createdAt(Instant.now()).build());
    }

    private void actAs(AppUser user) {
        TenantContext.set(company.getId());
        AppUser fresh = appUserRepository.findById(user.getId()).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(fresh, null, List.of()));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
