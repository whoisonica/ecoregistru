package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.controller.request.DriverRequest;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.controller.response.DriverResponse;
import ro.ecoregistru.controller.response.WeighingOperationResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.DriverRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.DriverAttestationAlertScheduler;
import ro.ecoregistru.service.DriverService;
import ro.ecoregistru.service.WeighingOperationService;
import ro.ecoregistru.service.notification.NotificationService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ro.ecoregistru.enums.WeighingOperationType.OUT;

/**
 * D2.2 — șoferii extinși: depozitul implicit al firmei, atestatul cu alerta de 30 de zile, șoferul ales din
 * listă pe operațiune (numele și mașina de la el) și șoferul ocazional scris liber.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class DriverFleetIT {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 16);

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired DriverService service;
    @Autowired DriverAttestationAlertScheduler scheduler;
    @Autowired WeighingOperationService operations;
    @Autowired DriverRepository driverRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;

    @MockitoBean NotificationService notificationService;

    Company company;
    AppUser admin;
    WorkPoint depot;

    @BeforeEach
    void setUp() {
        company = company("Soferi");
        admin = user(company, Role.ADMIN);
        depot = depot(company);
        actAs(company, admin);
    }

    @AfterEach
    void tearDown() {
        clearThread();
    }

    @Test
    void theHomeDepotAndAttestationAreSavedAndCleared() {
        DriverResponse saved = service.create(new DriverRequest(null, "Ion Popescu", null, null, "CJ12ABC",
                depot.getId(), " ATS 77 ", TODAY.plusYears(2)));
        assertThat(saved.homeWorkPointId()).isEqualTo(depot.getId());
        assertThat(saved.homeWorkPointName()).isEqualTo(depot.getName());
        assertThat(saved.attestationNumber()).isEqualTo("ATS 77");
        assertThat(saved.attestationExpiry()).isEqualTo(TODAY.plusYears(2));

        DriverResponse cleared = service.update(saved.id(), request("Ion Popescu"));
        assertThat(cleared.homeWorkPointId()).isNull();
        assertThat(cleared.attestationNumber()).isNull();
        assertThat(cleared.attestationExpiry()).isNull();
        assertThat(cleared.vehicleRegistration()).isNull();
    }

    @Test
    void anotherFirmsDepotAndDriverAreNotFound() {
        Company other = company("Vecina");
        WorkPoint foreignDepot = depot(other);
        actAs(other, user(other, Role.ADMIN));
        UUID foreign = service.create(request("Străin")).id();

        actAs(company, admin);
        assertThatThrownBy(() -> service.create(new DriverRequest(null, "Ion", null, null, null,
                foreignDepot.getId(), null, null))).isInstanceOf(NotFoundException.class);
        UUID own = service.create(request("Ion")).id();
        assertThatThrownBy(() -> service.update(own, new DriverRequest(null, "Ion", null, null, null,
                foreignDepot.getId(), null, null))).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> operations.create(operation(foreign, null, null)))
                .as("operațiunea nu leagă șoferul altei firme").isInstanceOf(NotFoundException.class);
    }

    /**
     * Ales din listă: numele și mașina vin de la șofer când formularul nu le scrie; ce scrie formularul are
     * întâietate. Ocazionalul, fără fișă, rămâne text. Fișa ștearsă nu atinge operațiunea.
     */
    @Test
    void theOperationTakesThePickedDriverAndKeepsTheOccasionalOneAsText() {
        DriverResponse ion = service.create(new DriverRequest(null, "Ion Popescu", null, null, "CJ12ABC",
                depot.getId(), null, null));

        WeighingOperationResponse picked = operations.create(operation(ion.id(), null, null));
        assertThat(picked.driverName()).isEqualTo("Ion Popescu");
        assertThat(picked.vehicleRegistration()).isEqualTo("CJ12ABC");

        WeighingOperationResponse otherTruck = operations.create(operation(ion.id(), null, "B01XYZ"));
        assertThat(otherTruck.vehicleRegistration()).as("mașina scrisă bate mașina implicită").isEqualTo("B01XYZ");

        WeighingOperationResponse occasional = operations.create(operation(null, "Gheorghe Ocazional", null));
        assertThat(occasional.driverName()).isEqualTo("Gheorghe Ocazional");
        assertThat(occasional.vehicleRegistration()).isNull();

        service.deactivate(ion.id());
        service.delete(ion.id());
        assertThat(operations.get(picked.id()).driverName()).isEqualTo("Ion Popescu");
    }

    @Test
    void theViewerReadsTheDepotAndAttestationButCannotWrite() throws Exception {
        service.create(new DriverRequest(null, "Vasile", null, null, null, depot.getId(), "ADR 9", TODAY.plusDays(90)));
        AppUser viewer = user(company, Role.CLIENT_VIEWER);
        clearThread();

        mockMvc.perform(post("/api/v1/drivers").header("Authorization", bearer(viewer))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\"}"))
                .andExpect(status().isForbidden());
        String list = mockMvc.perform(get("/api/v1/drivers").header("Authorization", bearer(viewer)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(list).contains("ADR 9").contains(depot.getName()).contains("2026-12-15");
    }

    /**
     * Alerta: 30 de zile doar înainte, o dată pe dată, rearmată de o dată nouă; nu pentru un șofer inactiv,
     * cu atestatul deja expirat sau al unui transportator.
     */
    @Test
    void theAttestationWarningGoesOutOncePerDateWithinThirtyDays() {
        UUID soon = service.create(attested("Aproape", TODAY.plusDays(10))).id();
        UUID far = service.create(attested("Departe", TODAY.plusDays(31))).id();
        UUID lapsed = service.create(attested("Expirat", TODAY.minusDays(1))).id();
        UUID parked = service.create(attested("Inactiv", TODAY.plusDays(5))).id();
        service.deactivate(parked);
        Partner hauler = partner(company, true);
        UUID carriers = driverRepository.save(ro.ecoregistru.entity.Driver.builder().company(company).partner(hauler)
                .name("Al transportatorului").attestationExpiry(TODAY.plusDays(3))
                .active(true).createdAt(Instant.now()).build()).getId();
        clearThread();

        scheduler.dispatchWarnings(TODAY);

        verify(notificationService).sendDriverAttestationWarning(
                argThat(d -> d.getId().equals(soon)), anyList(), eq(10L));
        assertThat(driverRepository.findById(soon).orElseThrow().getAttestationWarningSentFor()).isEqualTo(TODAY.plusDays(10));
        for (UUID quiet : List.of(far, lapsed, parked, carriers)) {
            assertThat(driverRepository.findById(quiet).orElseThrow().getAttestationWarningSentFor()).isNull();
        }

        clearInvocations(notificationService);
        scheduler.dispatchWarnings(TODAY.plusDays(1));
        verify(notificationService, never()).sendDriverAttestationWarning(
                argThat(d -> d.getId().equals(soon)), any(), any(Long.class));

        actAs(company, admin);
        service.update(soon, attested("Aproape", TODAY.plusDays(25)));
        clearThread();
        scheduler.dispatchWarnings(TODAY.plusDays(1));
        assertThat(driverRepository.findById(soon).orElseThrow().getAttestationWarningSentFor()).isEqualTo(TODAY.plusDays(25));
    }

    // --- helpers ---

    private static DriverRequest request(String name) {
        return new DriverRequest(null, name, null, null, null, null, null, null);
    }

    private static DriverRequest attested(String name, LocalDate expiry) {
        return new DriverRequest(null, name, null, null, null, null, "ATS", expiry);
    }

    private WeighingOperationRequest operation(UUID driverId, String driverName, String typedRegistration) {
        Partner recipient = partner(company, false);
        return new WeighingOperationRequest(OUT, depot.getId(), TODAY, recipient.getId(), null, null,
                driverId, null, driverName, typedRegistration, null, null, null, null, null, null);
    }

    private static void clearThread() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private String bearer(AppUser user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    private Company company(String name) {
        return companyRepository.save(Company.builder()
                .name(name + " " + suffix() + " SRL").cui("ROV" + suffix()).type(CompanyType.COLLECTOR)
                .active(true).createdAt(Instant.now()).build());
    }

    private WorkPoint depot(Company owner) {
        return workPointRepository.save(WorkPoint.builder()
                .company(owner).name("Depozit " + suffix()).active(true).createdAt(Instant.now()).build());
    }

    private Partner partner(Company owner, boolean carrier) {
        return partnerRepository.save(Partner.builder()
                .company(owner).name("Partener " + suffix()).cui("RO" + suffix())
                .type(PartnerType.COLLECTOR).client(true).carrier(carrier)
                .active(true).createdAt(Instant.now()).build());
    }

    private AppUser user(Company owner, Role role) {
        return appUserRepository.save(AppUser.builder()
                .email("soferi+" + suffix() + "@demo.ro").password("x")
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
