package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
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
import ro.ecoregistru.enums.AfmContribution;
import ro.ecoregistru.entity.ReportingDeadline;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.DeadlineStatus;
import ro.ecoregistru.enums.MarketRole;
import ro.ecoregistru.enums.ReportType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.entity.WasteCode;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.ReportingDeadlineRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.service.DeadlineCalendarScheduler;
import ro.ecoregistru.service.DeadlineService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FAZA TERMENE / T4: reporting-deadline calendar. Covers the next-deadline-per-kind rule
 * (16.09.2026), additive+idempotent generation, the AFM-only-when-obligated rule, effective
 * OVERDUE derivation, completion, role gating and tenant isolation. Generation is driven with a
 * fixed day through the service, so the suite does not depend on the day it runs.
 */
@SpringBootTest(properties = "app.deadlines.missed-shown-from=2026-01-01")
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class DeadlineIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired ReportingDeadlineRepository deadlineRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired DeadlineService deadlineService;
    @Autowired DeadlineCalendarScheduler calendarScheduler;

    /** Fresh tenant so deadline counts are deterministic regardless of other test methods. */
    private TenantFixture newTenant(boolean afmObligation, AfmContribution... contributions) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Termene " + suffix).cui("ROT" + suffix).type(CompanyType.GENERATOR)
                .active(true).afmObligation(afmObligation)
                .afmContributions(new java.util.LinkedHashSet<>(java.util.List.of(contributions)))
                .createdAt(Instant.now()).build());
        AppUser admin = appUserRepository.save(AppUser.builder()
                .email("t-admin+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        return new TenantFixture(company, jwtService.generateToken(admin));
    }

    // ---------- Doar următorul termen, pe fiecare fel (16.09.2026) ----------

    /** Ziua contului din exemplul proprietarului: 15 martie tocmai a trecut. */
    private static final LocalDate MARCH_16_2027 = LocalDate.of(2027, 3, 16);

    /**
     * Bugul pe care l-a reparat felia: un cont făcut în septembrie primea anul întreg, de la
     * 1 ianuarie — 15 martie și opt AFM-uri lunare „depășite” din prima zi. Acum nimic trecut.
     */
    @Test
    void aNewAccountGetsNothingAlreadyPastDue() {
        TenantFixture t = newTenant(true);
        setEnvironmentalPermit(t, "AM 214/12.03.2024", null);
        LocalDate september16 = LocalDate.of(2026, 9, 16);

        deadlineService.ensureUpcoming(t.company.getId(), september16);

        assertThat(rows(t)).allSatisfy(d -> assertThat(d.getDueDate()).isAfterOrEqualTo(september16));
        assertThat(dates(t, ReportType.SIM_ANNUAL)).containsExactly(LocalDate.of(2027, 3, 15));
        assertThat(dates(t, ReportType.AFM_MONTHLY)).containsExactly(LocalDate.of(2026, 9, 25));
        assertThat(dates(t, ReportType.APM_ANNUAL_MAY)).containsExactly(LocalDate.of(2027, 5, 31));
    }

    /**
     * Exemplul proprietarului: cont pe 16 martie 2027. 15 martie 2027 a trecut, deci primul SIM e
     * 15 martie 2028; 31 mai 2027 e încă înainte, deci apare el — și nu și 31 mai 2028.
     */
    @Test
    void theOwnersExampleOneRowPerKindTheNextOneOnly() {
        TenantFixture t = newTenant(false);
        setEnvironmentalPermit(t, "AM 214/12.03.2024", null);

        deadlineService.ensureUpcoming(t.company.getId(), MARCH_16_2027);

        assertThat(dates(t, ReportType.SIM_ANNUAL)).containsExactly(LocalDate.of(2028, 3, 15));
        assertThat(dates(t, ReportType.APM_ANNUAL_MAY)).containsExactly(LocalDate.of(2027, 5, 31));
    }

    /** „Nu mă interesează 2028 dacă am încă 2027 de raportat”: cât e deschis, nu vine următorul. */
    @Test
    void theNextOccurrenceWaitsWhileTheCurrentOneIsOpen() {
        TenantFixture t = newTenant(false);
        UUID id = t.company.getId();

        deadlineService.ensureUpcoming(id, LocalDate.of(2026, 9, 16));
        deadlineService.ensureUpcoming(id, LocalDate.of(2027, 3, 15)); // chiar în ziua termenului

        assertThat(dates(t, ReportType.SIM_ANNUAL)).containsExactly(LocalDate.of(2027, 3, 15));
    }

    /** Scadența trecută fără bifă: termenul rămâne (depășit), iar următorul apare. */
    @Test
    void oncePastDueTheNextOccurrenceAppears() {
        TenantFixture t = newTenant(false);
        UUID id = t.company.getId();

        deadlineService.ensureUpcoming(id, LocalDate.of(2026, 9, 16));
        deadlineService.ensureUpcoming(id, MARCH_16_2027);

        assertThat(dates(t, ReportType.SIM_ANNUAL))
                .containsExactly(LocalDate.of(2027, 3, 15), LocalDate.of(2028, 3, 15));
    }

    /** Bifat devreme: următorul de același fel apare pe loc, din ruta de bifare. */
    @Test
    void completingADeadlineBringsTheNextOneOfTheSameKind() throws Exception {
        TenantFixture t = newTenant(true);
        LocalDate today = DeadlineService.today();
        deadlineService.ensureUpcoming(t.company.getId(), today);
        ReportingDeadline monthly = rows(t).stream()
                .filter(d -> d.getReportType() == ReportType.AFM_MONTHLY).findFirst().orElseThrow();

        mockMvc.perform(post("/api/v1/deadlines/" + monthly.getId() + "/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Depus la AFM\"}")
                        .header("Authorization", "Bearer " + t.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DONE")))
                .andExpect(jsonPath("$.completionNote", is("Depus la AFM")))
                .andExpect(jsonPath("$.completedAt").exists());

        assertThat(dates(t, ReportType.AFM_MONTHLY))
                .containsExactly(monthly.getDueDate(), monthly.getDueDate().plusMonths(1));
    }

    /** Crearea firmei aduce calendarul, fără buton și fără să aștepte dimineața. */
    @Test
    void creatingACompanyBringsItsUpcomingDeadlines() throws Exception {
        String platform = jwtService.generateToken(
                appUserRepository.findByEmail("platform@ecoregistru.ro").orElseThrow());
        String cui = "RO" + (10_000_000 + new java.util.Random().nextInt(89_999_999));
        mockMvc.perform(post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Termene noi SRL\",\"cui\":\"" + cui
                                + "\",\"type\":\"GENERATOR\",\"afmObligation\":false}")
                        .header("Authorization", "Bearer " + platform))
                .andExpect(status().isOk());

        Company created = companyRepository.findAll().stream()
                .filter(c -> c.getCui().equals(cui)).findFirst().orElseThrow();
        assertThat(deadlineRepository.findAll().stream()
                .filter(d -> d.getCompany().getId().equals(created.getId()))
                .map(ReportingDeadline::getReportType))
                .containsExactly(ReportType.SIM_ANNUAL);
    }

    /**
     * Dimineața programată trece prin toate firmele active și e idempotentă. Ziua e în 1990 dinadins:
     * rularea atinge toate firmele bazei de probe, iar termenele din 1990 nu cad în fereastra
     * niciunei alte probe.
     */
    @Test
    void theMorningRunCompletesEveryActiveCompanyOnce() {
        TenantFixture t = newTenant(true);
        LocalDate today = LocalDate.of(1990, 9, 16);

        calendarScheduler.run(today);
        int afterFirst = rows(t).size();
        calendarScheduler.run(today);

        assertThat(afterFirst).isEqualTo(2); // SIM 15.03.1991 + AFM 25.09.1990
        assertThat(rows(t)).hasSize(2);
    }

    @Test
    void generatesSimAndTheNextMonthlyAfmForAnObligatedCompany() {
        TenantFixture t = newTenant(true);
        assertThat(deadlineService.ensureUpcoming(t.company.getId(), LocalDate.of(2026, 9, 16))).isEqualTo(2);
    }

    @Test
    void generatesOnlySimForANonObligatedCompany() {
        TenantFixture t = newTenant(false);
        deadlineService.ensureUpcoming(t.company.getId(), LocalDate.of(2026, 9, 16));
        assertThat(rows(t)).extracting(ReportingDeadline::getReportType).containsExactly(ReportType.SIM_ANNUAL);
    }

    // ---------- The three cadences of OUG 196/2005 art. 11 ----------

    /**
     * The wrong output the cadence slice was written to fix: a company whose only Environment Fund
     * contribution is the yearly packaging one used to get <b>twelve</b> monthly deadlines. It gets
     * one, on 25 January — not 15 March, and not monthly.
     */
    @Test
    void packagingOnlyGetsOneDeadlineOn25January() {
        TenantFixture t = newTenant(true, AfmContribution.PACKAGING);
        deadlineService.ensureUpcoming(t.company.getId(), LocalDate.of(2026, 9, 16));

        assertThat(dates(t, ReportType.AFM_ANNUAL)).containsExactly(LocalDate.of(2027, 1, 25));
        assertThat(dates(t, ReportType.AFM_MONTHLY)).isEmpty();
    }

    /** The 2% a collector withholds at source is monthly: the next 25th. */
    @Test
    void theWithheldTwoPercentIsMonthly() {
        TenantFixture t = newTenant(false, AfmContribution.WITHHOLDING_2_PERCENT);
        deadlineService.ensureUpcoming(t.company.getId(), LocalDate.of(2026, 12, 26));

        assertThat(dates(t, ReportType.AFM_MONTHLY)).containsExactly(LocalDate.of(2027, 1, 25));
    }

    /**
     * „Economia circulară" a ieşit din configurare (proprietarul, 16.09.2026): nu mai generează termene
     * trimestriale, iar un cont care o avea ca singur răspuns nu cade pe cele lunare, chiar cu bifa veche.
     */
    @Test
    void theCircularEconomyContributionGeneratesNothingAnyMore() {
        TenantFixture t = newTenant(true, AfmContribution.CIRCULAR_ECONOMY);
        deadlineService.ensureUpcoming(t.company.getId(), LocalDate.of(2026, 9, 16));

        assertThat(rows(t)).extracting(ReportingDeadline::getReportType).containsExactly(ReportType.SIM_ANNUAL);
    }

    /**
     * An account nobody has filled in keeps what it had: the flag alone still means the monthly
     * deadline. Switching an alert off on an assumption is worse than leaving one that is too loud.
     */
    @Test
    void anUnansweredAccountKeepsTheOldMonthlyDeadline() {
        TenantFixture t = newTenant(true);
        deadlineService.ensureUpcoming(t.company.getId(), LocalDate.of(2026, 9, 16));

        assertThat(dates(t, ReportType.AFM_MONTHLY)).containsExactly(LocalDate.of(2026, 9, 25));
    }

    @Test
    void regenerationIsIdempotent() throws Exception {
        TenantFixture t = newTenant(true);
        regenerate(t.token);
        mockMvc.perform(post("/api/v1/deadlines/regenerate")
                        .header("Authorization", "Bearer " + t.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generated", is(0))); // nothing new the second time
    }

    /**
     * 16.09.2026: un termen nebifat rămas din generarea veche (scadența înainte de data regulii, aici
     * 01.01.2026) nu se arată; unul bifat rămâne, ca istoric.
     */
    @Test
    void pastDueUncompletedDeadlineFromBeforeTheRuleIsHiddenButACompletedOneStays() throws Exception {
        TenantFixture t = newTenant(false);
        LocalDate old = LocalDate.of(2025, 12, 1);
        saveDeadline(t, ReportType.OTHER, old, DeadlineStatus.UPCOMING);
        saveDeadline(t, ReportType.SIM_ANNUAL, old, DeadlineStatus.DONE);

        mockMvc.perform(get("/api/v1/deadlines").param("year", "2025")
                        .header("Authorization", "Bearer " + t.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.reportType=='OTHER')]", hasSize(0)))
                .andExpect(jsonPath("$[?(@.reportType=='SIM_ANNUAL')].status", is(java.util.List.of("DONE"))));
    }

    /**
     * Un termen ratat de la regulă încolo nu dispare a doua zi (api v105 îl ascundea): rămâne depășit
     * până se bifează, ca un 15 martie nedepus să se vadă.
     */
    @Test
    void aDeadlineMissedAfterTheRuleStaysOverdueUntilTicked() throws Exception {
        TenantFixture t = newTenant(false);
        LocalDate yesterday = DeadlineService.today().minusDays(1);
        saveDeadline(t, ReportType.OTHER, yesterday, DeadlineStatus.UPCOMING);

        mockMvc.perform(get("/api/v1/deadlines").param("year", String.valueOf(yesterday.getYear()))
                        .header("Authorization", "Bearer " + t.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.reportType=='OTHER')].status", is(java.util.List.of("OVERDUE"))));
    }

    private void saveDeadline(TenantFixture t, ReportType type, LocalDate due, DeadlineStatus status) {
        deadlineRepository.save(ReportingDeadline.builder()
                .company(t.company).reportType(type).dueDate(due).status(status)
                .completedAt(status == DeadlineStatus.DONE ? Instant.now() : null)
                .warned7Days(false).warned1Day(false).createdAt(Instant.now()).build());
    }

    @Test
    void viewerCannotGenerate() throws Exception {
        String viewerToken = jwtService.generateToken(
                appUserRepository.findByEmail("viewer@demo.ro").orElseThrow());
        mockMvc.perform(post("/api/v1/deadlines/regenerate")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$['error-code']", is("access.denied")));
    }

    @Test
    void deadlinesAreTenantScoped() throws Exception {
        TenantFixture a = newTenant(true);
        TenantFixture b = newTenant(true);
        LocalDate today = DeadlineService.today();
        deadlineService.ensureUpcoming(a.company.getId(), today);
        // B has generated nothing -> B's list is empty even though A has rows.
        mockMvc.perform(get("/api/v1/deadlines").param("year", String.valueOf(today.getYear() + 1))
                        .header("Authorization", "Bearer " + b.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    // ---------- 25 February: the packaging report of Ordinul 794/2012 (audit point 3) ----------

    /** A producer files the packaging report at the county agency by 25 February (art. 1 + art. 6). */
    @Test
    void aProducerGetsThe25FebruaryPackagingDeadline() {
        TenantFixture t = newTenantWithRoles(MarketRole.PRODUCER);
        deadlineService.ensureUpcoming(t.company.getId(), LocalDate.of(2026, 1, 10));

        assertThat(dates(t, ReportType.PACKAGING_ANNUAL)).containsExactly(LocalDate.of(2026, 2, 25));
    }

    /**
     * A trader sells goods somebody else packaged, so it never introduced the packaging on the
     * national market and does not file — Legea 249/2015, and {@code MarketRole#putsPackagingOnMarket}.
     */
    @Test
    void aTraderGetsNoPackagingDeadline() {
        TenantFixture t = newTenantWithRoles(MarketRole.TRADER);
        deadlineService.ensureUpcoming(t.company.getId(), LocalDate.of(2026, 1, 10));

        assertThat(dates(t, ReportType.PACKAGING_ANNUAL)).isEmpty();
    }

    /**
     * And an account that never answered the question gets nothing either — deliberately the
     * opposite of how an empty profile treats <em>screens</em>, which stay fully offered
     * (decizia 6). An alert asserts something about the client; a screen only offers.
     */
    @Test
    void anUnansweredProfileGetsNoPackagingDeadline() {
        TenantFixture t = newTenant(false); // no market roles at all
        deadlineService.ensureUpcoming(t.company.getId(), LocalDate.of(2026, 1, 10));

        assertThat(dates(t, ReportType.PACKAGING_ANNUAL)).isEmpty();
    }

    // ---------- Anexa 3 Ambalaje — Ordinul 794/2012 art. 4 și 6 (16.09.2026) ----------

    /** Un colector care a preluat ambalaje (15 01) în 2026 depune Anexa 3 pe 25.02.2027. */
    @Test
    void aCollectorThatTookOverPackagingOwesAnexa3OnThe25thOfFebruary() {
        TenantFixture t = newTenantOfType(CompanyType.COLLECTOR);
        addMovement(t, "15 01 01", 2026, WasteOperation.COLLECTED);
        deadlineService.ensureUpcoming(t.company.getId(), LocalDate.of(2026, 9, 16));

        assertThat(dates(t, ReportType.PACKAGING_ANNEX3)).containsExactly(LocalDate.of(2027, 2, 25));
    }

    /** Anul raportat e cel dinaintea termenului: pe 10.01.2026, 25.02.2026 privește 2025, gol. */
    @Test
    void anexa3LooksAtTheYearBeforeTheDeadline() {
        TenantFixture t = newTenantOfType(CompanyType.COLLECTOR);
        addMovement(t, "15 01 01", 2026, WasteOperation.COLLECTED);
        deadlineService.ensureUpcoming(t.company.getId(), LocalDate.of(2026, 1, 10));

        assertThat(dates(t, ReportType.PACKAGING_ANNEX3)).isEmpty();
    }

    /**
     * Semnalul e preluarea unui cod 15 01, nimic altceva: nici o preluare de alt deșeu, nici ambalajul
     * generat de colectorul însuși, nici generatorul (art. 4 nu-l numește).
     */
    @Test
    void anexa3NeedsAPackagingTakeoverByACollector() {
        TenantFixture otherWaste = newTenantOfType(CompanyType.COLLECTOR);
        addMovement(otherWaste, "20 01 01", 2026, WasteOperation.COLLECTED);
        TenantFixture ownPackaging = newTenantOfType(CompanyType.BOTH);
        addMovement(ownPackaging, "15 01 01", 2026, WasteOperation.GENERATED);
        TenantFixture generator = newTenantOfType(CompanyType.GENERATOR);
        addMovement(generator, "15 01 01", 2026, WasteOperation.GENERATED);

        for (TenantFixture t : java.util.List.of(otherWaste, ownPackaging, generator)) {
            deadlineService.ensureUpcoming(t.company.getId(), LocalDate.of(2026, 9, 16));
            assertThat(dates(t, ReportType.PACKAGING_ANNEX3)).isEmpty();
        }
    }

    // ---------- Termenul de 30 aprilie — OUG 92/2021 art. 49 alin. (9) ----------

    /**
     * Half one: a used-oil code in the evidence is the fact. The reported year is the one
     * <em>before</em> the deadline, so a 2026 movement produces the 30.04.2027 deadline.
     */
    @Test
    void aUsedOilMovementCreatesThe30AprilDeadlineOfTheFollowingYear() {
        TenantFixture t = newTenantWithMovementOn("13 02 08", 2026);
        deadlineService.ensureUpcoming(t.company.getId(), LocalDate.of(2026, 9, 16));

        assertThat(dates(t, ReportType.APM_ANNUAL_APRIL)).containsExactly(LocalDate.of(2027, 4, 30));
    }

    /**
     * And not in the year of the movement itself: on 1 April 2026 the next 30 April is 2026's,
     * which reports 2025 — and this company has no movements in 2025.
     */
    @Test
    void theUsedOilOfTheDueYearDoesNotCount() {
        TenantFixture t = newTenantWithMovementOn("13 02 08", 2026);
        deadlineService.ensureUpcoming(t.company.getId(), LocalDate.of(2026, 4, 1));

        assertThat(dates(t, ReportType.APM_ANNUAL_APRIL)).isEmpty();
    }

    /**
     * The morning asks again: oil that appears after 30 April 2026 has passed still brings the
     * 30 April 2027 deadline, without anybody pressing a button.
     */
    @Test
    void oilRecordedLaterStillBringsTheDeadlineOnTheNextRun() {
        TenantFixture t = newTenant(false);
        deadlineService.ensureUpcoming(t.company.getId(), LocalDate.of(2026, 5, 2));
        assertThat(dates(t, ReportType.APM_ANNUAL_APRIL)).isEmpty();

        addMovement(t, "13 02 08", 2026);
        deadlineService.ensureUpcoming(t.company.getId(), LocalDate.of(2026, 5, 3));

        assertThat(dates(t, ReportType.APM_ANNUAL_APRIL)).containsExactly(LocalDate.of(2027, 4, 30));
    }

    /** A company that moves waste but no oil gets nothing. */
    @Test
    void aCompanyWithoutOilAndWithoutAPermitGetsNo30April() {
        // Hârtie şi carton: nowhere near chapter 13.
        TenantFixture t = newTenantWithMovementOn("20 01 01", 2026);
        deadlineService.ensureUpcoming(t.company.getId(), LocalDate.of(2026, 9, 16));

        assertThat(dates(t, ReportType.APM_ANNUAL_APRIL)).isEmpty();
    }

    /** The other half: the building permit is asked, not derived. Either half alone is enough. */
    @Test
    void aBuildingPermitHolderGetsThe30AprilDeadlineWithoutAnyOil() {
        TenantFixture t = newTenantWithMovementOn("20 01 01", 2026);
        setConstructionPermitHolder(t, Boolean.TRUE);
        deadlineService.ensureUpcoming(t.company.getId(), LocalDate.of(2026, 9, 16));

        assertThat(dates(t, ReportType.APM_ANNUAL_APRIL)).containsExactly(LocalDate.of(2027, 4, 30));
    }

    /**
     * Three states, and the two that generate nothing are asserted separately on purpose: an
     * explicit "no" and an unanswered profile behave the same.
     */
    @Test
    void neitherAnsweredNoNorUnansweredCreatesTheConstructionHalf() {
        TenantFixture answeredNo = newTenantWithMovementOn("20 01 01", 2026);
        setConstructionPermitHolder(answeredNo, Boolean.FALSE);
        deadlineService.ensureUpcoming(answeredNo.company.getId(), LocalDate.of(2026, 9, 16));
        assertThat(dates(answeredNo, ReportType.APM_ANNUAL_APRIL)).isEmpty();

        TenantFixture unanswered = newTenantWithMovementOn("20 01 01", 2026);
        setConstructionPermitHolder(unanswered, null);
        deadlineService.ensureUpcoming(unanswered.company.getId(), LocalDate.of(2026, 9, 16));
        assertThat(dates(unanswered, ReportType.APM_ANNUAL_APRIL)).isEmpty();
    }

    /** Both halves signalling still produce one row, not two, and a second run creates nothing. */
    @Test
    void bothHalvesTogetherStillProduceOneRow() {
        TenantFixture t = newTenantWithMovementOn("13 02 08", 2026);
        setConstructionPermitHolder(t, Boolean.TRUE);
        deadlineService.ensureUpcoming(t.company.getId(), LocalDate.of(2026, 9, 16));
        assertThat(deadlineService.ensureUpcoming(t.company.getId(), LocalDate.of(2026, 9, 16))).isZero();

        assertThat(dates(t, ReportType.APM_ANNUAL_APRIL)).hasSize(1);
    }

    // ---------- Termenul de 31 mai — OUG 92/2021 art. 44 alin. (3) ----------

    /**
     * Semnalul e numărul autorizației de mediu din profil — chiar condiția din art. 44 alin. (1).
     */
    @Test
    void anEnvironmentalPermitCreatesThe31MayDeadline() {
        TenantFixture t = newTenant(false);
        setEnvironmentalPermit(t, "AM 214/12.03.2024", LocalDate.of(2029, 3, 12));
        deadlineService.ensureUpcoming(t.company.getId(), LocalDate.of(2026, 9, 16));

        assertThat(dates(t, ReportType.APM_ANNUAL_MAY)).containsExactly(LocalDate.of(2027, 5, 31));
    }

    /** O firmă fără autorizație de mediu în profil nu primește nimic. */
    @Test
    void aCompanyWithoutAnEnvironmentalPermitGetsNo31May() {
        TenantFixture t = newTenant(false);
        deadlineService.ensureUpcoming(t.company.getId(), LocalDate.of(2026, 9, 16));

        assertThat(dates(t, ReportType.APM_ANNUAL_MAY)).isEmpty();
    }

    /** Rubrica goală nu e un răspuns: {@code isBlank}, nu {@code != null}. */
    @Test
    void aBlankPermitNumberIsNotAPermit() {
        TenantFixture t = newTenant(false);
        setEnvironmentalPermit(t, "   ", null);
        deadlineService.ensureUpcoming(t.company.getId(), LocalDate.of(2026, 9, 16));

        assertThat(dates(t, ReportType.APM_ANNUAL_MAY)).isEmpty();
    }

    /**
     * Și decizia inversă: o autorizație <b>expirată</b> ține termenul aprins — raportarea e a anului
     * raportat, iar o autorizație stinsă între timp nu șterge ce se datora cât a ținut.
     */
    @Test
    void anExpiredPermitStillCreatesThe31MayDeadline() {
        TenantFixture t = newTenant(false);
        setEnvironmentalPermit(t, "AM 9/01.02.2019", LocalDate.of(2024, 2, 1));
        deadlineService.ensureUpcoming(t.company.getId(), LocalDate.of(2026, 9, 16));

        assertThat(dates(t, ReportType.APM_ANNUAL_MAY)).containsExactly(LocalDate.of(2027, 5, 31));
    }

    private java.util.List<ReportingDeadline> rows(TenantFixture t) {
        return deadlineRepository.findAllByCompany_IdAndDueDateBetweenOrderByDueDateAsc(
                t.company.getId(), LocalDate.of(1900, 1, 1), LocalDate.of(2100, 12, 31));
    }

    private java.util.List<LocalDate> dates(TenantFixture t, ReportType type) {
        return rows(t).stream().filter(d -> d.getReportType() == type).map(ReportingDeadline::getDueDate).toList();
    }

    private void setEnvironmentalPermit(TenantFixture t, String number, LocalDate expiry) {
        Company company = companyRepository.findById(t.company.getId()).orElseThrow();
        company.setEnvironmentalAuthNumber(number);
        company.setEnvironmentalAuthExpiry(expiry);
        companyRepository.save(company);
    }

    private TenantFixture newTenantWithMovementOn(String code, int year) {
        TenantFixture t = newTenant(false);
        addMovement(t, code, year);
        return t;
    }

    private void addMovement(TenantFixture t, String code, int year) {
        addMovement(t, code, year, WasteOperation.GENERATED);
    }

    private TenantFixture newTenantOfType(CompanyType type) {
        TenantFixture t = newTenant(false);
        Company company = companyRepository.findById(t.company.getId()).orElseThrow();
        company.setType(type);
        companyRepository.save(company);
        return t;
    }

    private void addMovement(TenantFixture t, String code, int year, WasteOperation operation) {
        WorkPoint wp = workPointRepository.save(WorkPoint.builder()
                .company(t.company).name("PL-" + UUID.randomUUID().toString().substring(0, 6))
                .active(true).createdAt(Instant.now()).build());
        WasteCode wasteCode = wasteCodeRepository.findByCode(code).orElseThrow();
        movementRepository.save(WasteMovement.builder()
                .company(t.company).workPoint(wp).date(LocalDate.of(year, 4, 15))
                .wasteCode(wasteCode).quantity(new java.math.BigDecimal("15.000")).unit(Unit.KG)
                .operation(operation).deleted(false)
                .createdBy(UUID.randomUUID()).build());
    }

    private void setConstructionPermitHolder(TenantFixture t, Boolean value) {
        Company company = companyRepository.findById(t.company.getId()).orElseThrow();
        company.setConstructionPermitHolder(value);
        companyRepository.save(company);
    }

    private TenantFixture newTenantWithRoles(MarketRole... roles) {
        TenantFixture t = newTenant(false);
        Company company = companyRepository.findById(t.company.getId()).orElseThrow();
        company.setMarketRoles(new java.util.LinkedHashSet<>(java.util.List.of(roles)));
        companyRepository.save(company);
        return t;
    }

    private void regenerate(String token) throws Exception {
        mockMvc.perform(post("/api/v1/deadlines/regenerate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private record TenantFixture(Company company, String token) {}
}
