package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.CompleteDeadlineRequest;
import ro.ecoregistru.controller.response.DeadlineGenerationResponse;
import ro.ecoregistru.controller.response.DeadlineResponse;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.ReportingDeadline;
import ro.ecoregistru.enums.DeadlineStatus;
import ro.ecoregistru.enums.ReportType;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.ReportingDeadlineRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.util.UsedOilCodes;

import ro.ecoregistru.enums.AfmContribution;
import ro.ecoregistru.enums.MarketRole;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static ro.ecoregistru.exception.ErrorMessageEnum.COMPANY_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.DEADLINE_NOT_FOUND;

/**
 * Legal reporting deadlines for the current tenant (FAZA TERMENE).
 *
 * Deadline rules (docs/legislatie.md §1, high confidence):
 *  - the Environment Fund deadlines — one cadence per contribution the company owes
 *    (monthly on the 25th, quarterly after each quarter, or once on 25 January). AFM is not
 *    universal and its rhythms differ, so nothing is generated for a company that owes nothing.
 *    See {@link #afmDeadlines}.
 *  - SIM_ANNUAL — 15 March, for the previous calendar year's data. Generated for every company:
 *    this is the Anexa 1 evidence being filed, and art. 1 alin. (1) HG 856/2002 binds anyone who
 *    generates waste. See {@link ReportType#SIM_ANNUAL} for why there is no separate Anexa 1 type.
 *  - PACKAGING_ANNUAL — 25 February, the packaging report of Ordinul 794/2012 art. 6, at the
 *    county environmental agency. Only for a company whose profile says it puts packaging on the
 *    national market; an unanswered profile gets nothing. See {@link #packagingDeadline}.
 *  - PACKAGING_ANNEX3 — 25 February too, Anexa 3 of the same order (art. 4): a collector that took
 *    over a {@code 15 01} code in the reported year. Read from the movements. See
 *    {@link #packagingWasteDeadline}.
 *  - APM_ANNUAL_APRIL — 30 April, the second annual APM filing (OUG 92/2021 art. 49 alin. (9)):
 *    used oils and construction waste. Only for a company one of whose two halves signals;
 *    see {@link #aprilDeadline}.
 *
 * <p><b>Doar următorul termen, pe fiecare fel de termen (proprietarul, 16.09.2026).</b> Până atunci
 * butonul genera anul calendaristic întreg, de la 1 ianuarie: un cont făcut în septembrie primea
 * 15 martie, 30 aprilie, 31 mai și opt AFM-uri lunare, toate „depășite” din prima zi — iar ele
 * privesc anul de dinainte de cont. Termenele care contau (15 martie anul viitor, pentru datele
 * ținute acum în aplicație) nu apăreau nicăieri până în ianuarie. Acum, pentru fiecare fel de termen:
 *
 * <ul>
 *   <li>nu se creează nimic cu scadența trecută;</li>
 *   <li>se creează prima apariție de azi înainte, dacă firma o datorează;</li>
 *   <li>cât timp aceea e deschisă, nu apare următoarea („nu mă interesează 2028 dacă am încă 2027
 *       de raportat”). Apare când e bifată sau când i-a trecut data.</li>
 * </ul>
 *
 * <p>Calendarul se ține singur: {@link DeadlineCalendarScheduler} în fiecare dimineață, la
 * crearea și la modificarea firmei, și la bifarea unui termen. Butonul de pe Termene face același
 * lucru, pe loc. Generarea rămâne aditivă și idempotentă (unic pe firmă+tip+scadență): nu șterge,
 * deci termenele create după regula veche rămân. Starea efectivă (OVERDUE) se socotește la citire.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeadlineService {

    /** Ziua „de azi” a unui client e cea din România, nu cea a serverului (UTC pe Heroku). */
    public static final ZoneId ZONE = ZoneId.of("Europe/Bucharest");

    /** Cât de departe se caută următoarea apariție: un termen anual bifat devreme sare peste un an. */
    private static final int YEARS_AHEAD = 2;

    private static final List<MonthDay> EVERY_25TH = Arrays.stream(Month.values())
            .map(m -> MonthDay.of(m, 25)).toList();
    // The 25th of the month after each quarter: January, April, July, October.
    private static final List<MonthDay> AFTER_EACH_QUARTER = List.of(MonthDay.of(Month.JANUARY, 25),
            MonthDay.of(Month.APRIL, 25), MonthDay.of(Month.JULY, 25), MonthDay.of(Month.OCTOBER, 25));

    ReportingDeadlineRepository deadlineRepository;
    CompanyRepository companyRepository;
    WasteMovementRepository movementRepository;

    @Transactional(readOnly = true)
    public List<DeadlineResponse> list(int year) {
        UUID tenantId = TenantContext.require();
        LocalDate today = today();
        return deadlineRepository.findAllByCompany_IdAndDueDateBetweenOrderByDueDateAsc(
                        tenantId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
                .stream()
                // Un termen nebifat cu data trecută nu se mai arată (proprietarul, 16.09.2026): au rămas
                // din generarea veche a anului întreg și nu mai cer nimic. Cele bifate rămân, ca istoric.
                .filter(d -> d.getStatus() == DeadlineStatus.DONE || !d.getDueDate().isBefore(today))
                .map(d -> toResponse(d, today))
                .toList();
    }

    /** Butonul de pe Termene: completează pe loc ce ar completa dimineața programată. */
    @Transactional
    public DeadlineGenerationResponse regenerate() {
        return new DeadlineGenerationResponse(ensureUpcoming(TenantContext.require(), today()));
    }

    /**
     * Creează, pentru fiecare fel de termen pe care firma îl datorează, următoarea apariție, dacă
     * nu există deja una deschisă de azi înainte. Întoarce câte termene noi au apărut.
     */
    @Transactional
    public int ensureUpcoming(UUID companyId, LocalDate today) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));

        int created = 0;
        // SIM annual: 15 March, covering the previous year.
        created += ensureNext(company, ReportType.SIM_ANNUAL, List.of(MonthDay.of(Month.MARCH, 15)),
                today, due -> true);
        created += afmDeadlines(company, today);
        created += packagingDeadline(company, today);
        created += packagingWasteDeadline(company, today);
        created += aprilDeadline(company, today);
        created += mayDeadline(company, today);
        return created;
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    /**
     * Prima scadență a tipului de azi înainte. Una bifată deja (depusă devreme) se sare; una
     * deschisă oprește căutarea — ea e „următorul termen”. Pe prima care lipsește, {@code owed}
     * hotărăște dacă firma o datorează: dacă nu, nu se creează nimic acum, iar dimineața
     * următoare întreabă din nou (o mișcare cu ulei uzat poate apărea între timp).
     */
    private int ensureNext(Company company, ReportType type, List<MonthDay> days, LocalDate today,
                           Predicate<LocalDate> owed) {
        for (int year = today.getYear(); year <= today.getYear() + YEARS_AHEAD; year++) {
            for (MonthDay day : days) {
                LocalDate due = day.atYear(year);
                if (due.isBefore(today)) {
                    continue;
                }
                Optional<ReportingDeadline> existing = deadlineRepository
                        .findByCompany_IdAndReportTypeAndDueDate(company.getId(), type, due);
                if (existing.isPresent()) {
                    if (existing.get().getStatus() == DeadlineStatus.DONE) {
                        continue;
                    }
                    return 0;
                }
                return owed.test(due) ? create(company, type, due) : 0;
            }
        }
        return 0;
    }

    /**
     * The packaging report of Ordinul 794/2012, due 25 February at the county environmental agency
     * (art. 1 and art. 6). Audit point 3, built 04.09.2026.
     *
     * <p>Only for a company that <em>says</em> it puts packaging on the national market. A profile
     * with no answer produces nothing: {@link MarketRole#putsPackagingOnMarket(java.util.Collection)}
     * already returns {@code false} for an empty set, and {@link MarketRole#answered} is asserted
     * next to it so the intent reads as a decision rather than as a side effect of the default.
     * A trader gets nothing either — it sells goods somebody else packaged, so it never introduced
     * the packaging and does not file (Legea 249/2015; see {@link MarketRole}).
     */
    private int packagingDeadline(Company company, LocalDate today) {
        Set<MarketRole> roles = company.getMarketRoles();
        if (!MarketRole.answered(roles) || !MarketRole.putsPackagingOnMarket(roles)) {
            return 0;
        }
        return ensureNext(company, ReportType.PACKAGING_ANNUAL,
                List.of(MonthDay.of(Month.FEBRUARY, 25)), today, due -> true);
    }

    /**
     * Anexa 3 la Ordinul 794/2012, due 25 February for the previous year (art. 4 and art. 6).
     *
     * <p>Owed by a collector that <em>took over</em> packaging waste — a {@code 15 01} code
     * (art. 8 alin. (3)) on a {@code COLLECTED} movement of the reported year. The same positive-only
     * rule as the used-oil half of {@link #aprilDeadline}: a collector with no such takeover gets
     * nothing, and the next morning asks again. A generator never does — art. 4 does not name it.
     *
     * <p>The packaging role (collector, recycler, …) is <b>not</b> required. It decides which table
     * gets printed, not whether the report is owed; a missing role is shown on the deadline as
     * something to fill in, not used to stay silent.
     */
    private int packagingWasteDeadline(Company company, LocalDate today) {
        if (!company.getType().keepsArt48Register()) {
            return 0;
        }
        return ensureNext(company, ReportType.PACKAGING_ANNEX3, List.of(MonthDay.of(Month.FEBRUARY, 25)),
                today, due -> movementRepository.existsCollectedPackaging(company.getId(),
                        LocalDate.of(due.getYear() - 1, 1, 1), LocalDate.of(due.getYear() - 1, 12, 31)));
    }

    /**
     * The second annual APM filing, due <b>30 April</b> for the previous calendar year —
     * OUG 92/2021 art. 49 alin. (9). Missing from the calendar entirely until 10.09.2026, when the
     * framework act was re-read on its consolidated form.
     *
     * <p>Two categories owe it, and either one alone creates the deadline:
     *
     * <ul>
     *   <li><b>holders of used oils</b>, who report the measures of art. 31 alin. (1) — read from
     *       the movements of the reported year, because a used-oil code <em>is</em> the fact;</li>
     *   <li><b>holders of a building or demolition permit</b>, who report conformity with
     *       art. 17 alin. (7), the 70% target — read from the profile, because chapter 17 appears
     *       for anyone who hauls rubble while the obligation belongs to the permit holder.</li>
     * </ul>
     *
     * <p><b>The year looked at is the one before the due date</b>: the article says "până la
     * 30 aprilie a anului următor celui pentru care se raportează", so the deadline falling in
     * April {@code Y} reports {@code Y - 1} — the same relation {@link ReportType#SIM_ANNUAL}
     * has with its 15 March. Reading the due year instead would put the reminder one year
     * early for a company that has just started handling oil, and miss one that stopped.
     *
     * <p>Both signals are positive-only: a company with no oil code and an unanswered profile gets
     * nothing. That is the rule of {@link ReportType}, and here it has teeth — a wrong reminder on
     * this date sends a client to prepare a report that is not theirs.
     */
    private int aprilDeadline(Company company, LocalDate today) {
        boolean holdsPermit = Boolean.TRUE.equals(company.getConstructionPermitHolder());
        return ensureNext(company, ReportType.APM_ANNUAL_APRIL, List.of(MonthDay.of(Month.APRIL, 30)),
                today, due -> holdsPermit || holdsUsedOils(company, due.getYear() - 1));
    }

    private boolean holdsUsedOils(Company company, int reported) {
        return !UsedOilCodes.among(movementRepository.findDistinctWasteCodes(
                company.getId(),
                LocalDate.of(reported, 1, 1),
                LocalDate.of(reported, 12, 31))).isEmpty();
    }

    /**
     * The third annual APM filing, due <b>31 May</b> for the previous calendar year —
     * OUG 92/2021 art. 44 alin. (3), the waste prevention and reduction programme together with
     * the progress made on it. Sanctioned by art. 62 alin. (1) lit. a), <b>40.000–60.000 lei</b>:
     * the same bucket as the evidence itself, and six times what 30 April costs.
     *
     * <p>Found on 11.09.2026 in the consolidated act, after the April deadline had shipped. It is
     * the fourth annual term of the calendar and it was missing entirely, exactly like the third
     * one was a day earlier.
     *
     * <p><b>The signal is the permit the client already recorded.</b> Art. 44 alin. (1) binds the
     * legal person "pentru care autoritatea competentă pentru protecția mediului a emis o
     * autorizație de mediu/autorizație integrată de mediu", so a recorded permit number is not
     * correlated with the obligation — it is the condition the article writes. That makes this the
     * cleanest positive signal of the three annual APM terms: no new profile question, no
     * derivation from movements, nothing inferred from silence.
     *
     * <p><b>Expiry is not read, and that is a decision.</b> The filing belongs to the reported
     * year; a permit that has lapsed since does not cancel what was owed while it ran. Consulting
     * {@code environmentalAuthExpiry} would mute the reminder for precisely the client who is
     * behind on it.
     *
     * <p>Like {@link #aprilDeadline}, the row carries no document link, because the programme is
     * written by the client (or a third party, alin. (2)), not printed by us.
     */
    private int mayDeadline(Company company, LocalDate today) {
        String permit = company.getEnvironmentalAuthNumber();
        if (permit == null || permit.isBlank()) {
            return 0;
        }
        return ensureNext(company, ReportType.APM_ANNUAL_MAY, List.of(MonthDay.of(Month.MAY, 31)),
                today, due -> true);
    }

    @Transactional
    public DeadlineResponse complete(UUID id, CompleteDeadlineRequest request) {
        ReportingDeadline deadline = require(id);
        deadline.setStatus(DeadlineStatus.DONE);
        deadline.setCompletedAt(Instant.now());
        deadline.setCompletionNote(request != null ? request.note() : null);
        // Bifat devreme: următorul de același fel apare acum, nu abia dimineața.
        ensureUpcoming(deadline.getCompany().getId(), today());
        return toResponse(deadline, today());
    }

    @Transactional
    public DeadlineResponse reopen(UUID id) {
        ReportingDeadline deadline = require(id);
        deadline.setStatus(DeadlineStatus.UPCOMING);
        deadline.setCompletedAt(null);
        deadline.setCompletionNote(null);
        return toResponse(deadline, today());
    }

    /**
     * The Environment Fund deadlines, one cadence per contribution owed (OUG 196/2005 art. 11).
     *
     * <p>Before 24.08.2026 this generated a monthly deadline for anyone with the {@code
     * afmObligation} flag, whatever they actually owed — so a client whose only contribution is
     * the yearly packaging one received eleven wrong reminders a year. Now each contribution
     * brings its own rhythm:
     *
     * <ul>
     *   <li>the 2% withheld at source — monthly, the 25th;</li>
     *   <li>the circular-economy contribution — quarterly, the 25th after each quarter;</li>
     *   <li>the packaging contribution — once, on 25 January.</li>
     * </ul>
     *
     * <p>An account that has not answered which contributions it owes keeps the old behaviour
     * exactly: the flag alone still produces the monthly deadline. Switching an alert off
     * on an assumption is worse than leaving one that is too loud, and this way the legacy path
     * fades out as accounts are filled in rather than going quiet all at once.
     */
    private int afmDeadlines(Company company, LocalDate today) {
        // „Economia circulară" a ieşit din configurarea contului (proprietarul, 16.09.2026): e a
        // depozitelor de deşeuri, nu a clienţilor noştri, deci nu mai generează termenul trimestrial.
        // Un cont care o avea ca singur răspuns nu cade pe calea veche (12 termene lunare): a răspuns.
        if (company.getAfmContributions().equals(Set.of(AfmContribution.CIRCULAR_ECONOMY))) {
            return 0;
        }
        Set<AfmContribution> owed = company.getAfmContributions().stream()
                .filter(c -> c != AfmContribution.CIRCULAR_ECONOMY)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        if (owed.isEmpty()) {
            return company.isAfmObligation()
                    ? ensureNext(company, ReportType.AFM_MONTHLY, EVERY_25TH, today, due -> true)
                    : 0;
        }

        int created = 0;
        for (AfmContribution.AfmCadence cadence : owed.stream().map(AfmContribution::getCadence)
                .distinct().toList()) {
            created += switch (cadence) {
                case MONTHLY -> ensureNext(company, ReportType.AFM_MONTHLY, EVERY_25TH, today, due -> true);
                case QUARTERLY -> ensureNext(company, ReportType.AFM_QUARTERLY, AFTER_EACH_QUARTER, today,
                        due -> true);
                case ANNUAL -> ensureNext(company, ReportType.AFM_ANNUAL,
                        List.of(MonthDay.of(Month.JANUARY, 25)), today, due -> true);
            };
        }
        return created;
    }

    private int create(Company company, ReportType type, LocalDate dueDate) {
        deadlineRepository.save(ReportingDeadline.builder()
                .company(company)
                .reportType(type)
                .dueDate(dueDate)
                .status(DeadlineStatus.UPCOMING)
                .warned7Days(false)
                .warned1Day(false)
                .createdAt(Instant.now())
                .build());
        return 1;
    }

    private ReportingDeadline require(UUID id) {
        UUID tenantId = TenantContext.require();
        return deadlineRepository.findByIdAndCompany_Id(id, tenantId)
                .orElseThrow(() -> new NotFoundException(DEADLINE_NOT_FOUND));
    }

    private DeadlineResponse toResponse(ReportingDeadline d, LocalDate today) {
        return new DeadlineResponse(
                d.getId(),
                d.getReportType(),
                d.getDueDate(),
                effectiveStatus(d, today),
                d.getCompletedAt(),
                d.getCompletionNote());
    }

    /** DONE if completed; otherwise OVERDUE once the due date has passed; else UPCOMING. */
    private DeadlineStatus effectiveStatus(ReportingDeadline d, LocalDate today) {
        if (d.getStatus() == DeadlineStatus.DONE) {
            return DeadlineStatus.DONE;
        }
        return d.getDueDate().isBefore(today) ? DeadlineStatus.OVERDUE : DeadlineStatus.UPCOMING;
    }
}
