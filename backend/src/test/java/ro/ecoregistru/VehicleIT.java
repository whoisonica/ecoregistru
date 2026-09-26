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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.controller.request.VehicleRequest;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.controller.response.VehicleResponse;
import ro.ecoregistru.controller.response.WeighingOperationResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.ErrorMessageEnum;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.VehicleRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.VehicleExpiryAlertScheduler;
import ro.ecoregistru.service.VehicleService;
import ro.ecoregistru.service.WeighingOperationService;
import ro.ecoregistru.service.notification.NotificationService;

import java.math.BigDecimal;
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
 * D2.1 — flota: numărul normalizat și unic pe firmă, licența doar peste 3,5 t, transportatorul bifat,
 * izolarea între firme, vehiculul pe operațiune (numărul lui se tipărește, fișa se poate șterge fără
 * să atingă operațiunea) și alerta de 30 de zile.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class VehicleIT {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 16);

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired VehicleService service;
    @Autowired VehicleExpiryAlertScheduler scheduler;
    @Autowired WeighingOperationService operations;
    @Autowired VehicleRepository vehicleRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired JdbcTemplate jdbc;

    @MockitoBean NotificationService notificationService;

    Company company;
    AppUser admin;
    WorkPoint depot;

    @BeforeEach
    void setUp() {
        company = company("Flota");
        admin = user(company, Role.ADMIN);
        depot = depot(company);
        actAs(company, admin);
    }

    @AfterEach
    void tearDown() {
        clearThread();
    }

    @Test
    void theRegistrationIsNormalizedAndUniqueInTheFirmOnly() {
        VehicleResponse truck = service.create(request(" cj 12-abc "));
        VehicleResponse van = service.create(request("B99XYZ"));
        assertThat(truck.registration()).isEqualTo("CJ12ABC");

        assertThatThrownBy(() -> service.create(request("CJ 12 ABC")))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.VEHICLE_REGISTRATION_TAKEN));
        assertThatThrownBy(() -> service.update(van.id(), request("cj12abc")))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.VEHICLE_REGISTRATION_TAKEN));
        assertThat(service.update(truck.id(), request("CJ12ABC")).registration())
                .as("propriul număr la editare nu e dublură").isEqualTo("CJ12ABC");
        assertThatThrownBy(() -> service.create(request("  ")))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.VEHICLE_REGISTRATION_REQUIRED));

        Company other = company("Alta");
        actAs(other, user(other, Role.ADMIN));
        assertThat(service.create(request("CJ12ABC")).registration()).isEqualTo("CJ12ABC");
    }

    /** Sub 3,5 t rubrica licenței n-are obiect: se golește la salvare, iar baza refuză un rând care o are. */
    @Test
    void theLicenceIsKeptOnlyAboveThreeAndAHalfTonnes() {
        VehicleRequest light = new VehicleRequest("CJ01AAA", "Autoutilitară", new BigDecimal("1800"), false,
                TODAY.plusYears(1), "LIC 1", TODAY.plusYears(2), depot.getId(), null);
        VehicleResponse saved = service.create(light);
        assertThat(saved.transportLicenseNumber()).isNull();
        assertThat(saved.transportLicenseExpiry()).isNull();
        assertThat(saved.itpExpiry()).isEqualTo(TODAY.plusYears(1));
        assertThat(saved.homeWorkPointName()).isEqualTo(depot.getName());

        VehicleRequest heavy = new VehicleRequest("CJ01AAA", "Camion", new BigDecimal("9000"), true,
                null, "LIC 1", TODAY.plusYears(2), null, null);
        assertThat(service.update(saved.id(), heavy).transportLicenseNumber()).isEqualTo("LIC 1");

        assertThatThrownBy(() -> jdbc.update("update vehicles set heavy = false where id = ?", saved.id()))
                .as("constrângerea din bază: licență pe un vehicul sub 3,5 t").hasMessageContaining("vehicles_license_only_heavy");
    }

    @Test
    void theCarrierMustBeTickedAndTheTareMustBePositive() {
        Partner collector = partner(company, false);
        Partner hauler = partner(company, true);

        assertThatThrownBy(() -> service.create(new VehicleRequest("CJ02BBB", null, null, false, null, null, null,
                null, collector.getId())))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.VEHICLE_PARTNER_NOT_CARRIER));
        assertThat(service.create(new VehicleRequest("CJ02BBB", null, null, false, null, null, null,
                null, hauler.getId())).partnerName()).isEqualTo(hauler.getName());

        assertThatThrownBy(() -> service.create(new VehicleRequest("CJ03CCC", null, BigDecimal.ZERO, false, null,
                null, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.VEHICLE_TARE_NOT_POSITIVE));
    }

    @Test
    void anotherFirmsVehicleDepotAndCarrierAreNotFound() {
        Company other = company("Vecina");
        WorkPoint foreignDepot = depot(other);
        Partner foreignHauler = partner(other, true);
        actAs(other, user(other, Role.ADMIN));
        UUID foreign = service.create(request("B01FFF")).id();

        actAs(company, admin);
        assertThat(service.list()).isEmpty();
        assertThatThrownBy(() -> service.update(foreign, request("B01FFF"))).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.deactivate(foreign)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.reactivate(foreign)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.delete(foreign)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.create(new VehicleRequest("CJ04DDD", null, null, false, null, null, null,
                foreignDepot.getId(), null))).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.create(new VehicleRequest("CJ04DDD", null, null, false, null, null, null,
                null, foreignHauler.getId()))).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> operations.create(operation(foreign, null)))
                .as("operațiunea nu leagă vehiculul altei firme").isInstanceOf(NotFoundException.class);
        assertThat(vehicleRepository.findById(foreign).orElseThrow().isActive()).isTrue();
    }

    /**
     * Pe operațiune se tipărește numărul vehiculului legat, chiar dacă formularul a trimis alt text; fișa
     * se șterge (după dezactivare) fără să atingă operațiunea, care își păstrează numărul.
     */
    @Test
    void theOperationPrintsTheLinkedVehicleAndOutlivesItsRecord() {
        VehicleResponse truck = service.create(request("CJ05EEE"));

        WeighingOperationResponse created = operations.create(operation(truck.id(), "SB99ZZZ"));
        assertThat(created.vehicleId()).isEqualTo(truck.id());
        assertThat(created.vehicleRegistration()).isEqualTo("CJ05EEE");

        WeighingOperationResponse typed = operations.create(operation(null, "sb 99 zzz"));
        assertThat(typed.vehicleId()).isNull();
        assertThat(typed.vehicleRegistration()).as("fără vehicul ales rămâne textul scris").isEqualTo("sb 99 zzz");

        assertThatThrownBy(() -> service.delete(truck.id()))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.VEHICLE_DELETE_REQUIRES_DEACTIVATION));
        service.deactivate(truck.id());
        service.delete(truck.id());

        WeighingOperationResponse after = operations.get(created.id());
        assertThat(after.vehicleId()).isNull();
        assertThat(after.vehicleRegistration()).isEqualTo("CJ05EEE");
    }

    @Test
    void theViewerReadsTheFleetButOnlyWritersAddToItOverHttp() throws Exception {
        AppUser viewer = user(company, Role.CLIENT_VIEWER);
        AppUser operator = user(company, Role.OPERATOR);
        String body = "{\"registration\":\"CJ06FFF\",\"heavy\":false}";
        clearThread();

        mockMvc.perform(post("/api/v1/vehicles").header("Authorization", bearer(viewer))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/vehicles").header("Authorization", bearer(operator))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        String list = mockMvc.perform(get("/api/v1/vehicles").header("Authorization", bearer(viewer)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(list).contains("CJ06FFF");
    }

    /**
     * Alerta: fereastra de 30 de zile doar înainte, cea mai apropiată dintre ITP și licență, o singură
     * dată pe dată, rearmată de o dată nouă; nu pentru un vehicul inactiv sau cu actele deja expirate.
     */
    @Test
    void theExpiryWarningGoesOutOncePerDateWithinThirtyDays() {
        UUID soon = service.create(new VehicleRequest("CJ07AAA", null, null, true,
                TODAY.plusDays(40), "LIC", TODAY.plusDays(10), null, null)).id();
        UUID far = service.create(new VehicleRequest("CJ07BBB", null, null, false,
                TODAY.plusDays(31), null, null, null, null)).id();
        UUID lapsed = service.create(new VehicleRequest("CJ07CCC", null, null, false,
                TODAY.minusDays(1), null, null, null, null)).id();
        UUID renewedLate = service.create(new VehicleRequest("CJ07EEE", null, null, true,
                TODAY.minusDays(2), "LIC", TODAY.plusDays(20), null, null)).id();
        UUID parked = service.create(new VehicleRequest("CJ07DDD", null, null, false,
                TODAY.plusDays(5), null, null, null, null)).id();
        service.deactivate(parked);
        clearThread();

        scheduler.dispatchWarnings(TODAY);

        verify(notificationService).sendVehicleExpiryWarning(
                argThat(v -> v.getId().equals(soon)), anyList(), eq(10L));
        assertThat(vehicleRepository.findById(soon).orElseThrow().getExpiryWarningSentFor()).isEqualTo(TODAY.plusDays(10));
        assertThat(vehicleRepository.findById(renewedLate).orElseThrow().getExpiryWarningSentFor())
                .as("ITP-ul expirat ieri nu ascunde licența care expiră peste 20 de zile").isEqualTo(TODAY.plusDays(20));
        for (UUID quiet : List.of(far, lapsed, parked)) {
            assertThat(vehicleRepository.findById(quiet).orElseThrow().getExpiryWarningSentFor()).isNull();
        }

        clearInvocations(notificationService);
        scheduler.dispatchWarnings(TODAY.plusDays(1));
        verify(notificationService, never()).sendVehicleExpiryWarning(
                argThat(v -> v.getId().equals(soon)), any(), any(Long.class));

        // Licența reînnoită: data care decide devine ITP-ul, care intră și el în fereastră.
        actAs(company, admin);
        service.update(soon, new VehicleRequest("CJ07AAA", null, null, true,
                TODAY.plusDays(40), "LIC", TODAY.plusYears(5), null, null));
        clearThread();
        scheduler.dispatchWarnings(TODAY.plusDays(15));
        assertThat(vehicleRepository.findById(soon).orElseThrow().getExpiryWarningSentFor()).isEqualTo(TODAY.plusDays(40));
    }

    // --- helpers ---

    private static VehicleRequest request(String registration) {
        return new VehicleRequest(registration, null, null, false, null, null, null, null, null);
    }

    private WeighingOperationRequest operation(UUID vehicleId, String typedRegistration) {
        Partner recipient = partner(company, false);
        return new WeighingOperationRequest(OUT, depot.getId(), TODAY, recipient.getId(), null, null,
                null, vehicleId, null, typedRegistration, null, null, null, null, null, null);
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
                .email("flota+" + suffix() + "@demo.ro").password("x")
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
