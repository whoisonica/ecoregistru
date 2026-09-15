package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import ro.ecoregistru.controller.response.ConsultancyOverviewResponse;
import ro.ecoregistru.controller.response.ConsultancyOverviewResponse.NextDeadline;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Consultancy;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.ReportingDeadline;
import ro.ecoregistru.enums.DeadlineStatus;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.ReportingDeadlineRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.security.SecurityUtils;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static ro.ecoregistru.exception.ErrorMessageEnum.CONSULTANCY_NOT_FOUND;

/**
 * P2.13, felia 2 — panoul „Toate firmele mele".
 *
 * <p><b>De ce există.</b> Un consultant cu 20 de firme comuta firma de 20 de ori ca să afle cine e în
 * întârziere. Panoul pune pe un singur ecran exact ce ar fi văzut pe Panoul fiecărei firme: termene,
 * blocaje la depunere, autorizații de parteneri.
 *
 * <p><b>Accesul nu trece prin {@code TenantContext}</b>, dinadins: cererea nu are o firmă aleasă, are
 * un cabinet. Firmele se iau din cabinetul sesiunii ({@link CompanyRepository#findAllByConsultancy_Id}),
 * aceeași listă pe care {@code TenantFilter} o acceptă în antet, deci panoul nu poate numi o firmă pe
 * care consultantul n-ar putea-o și alege.
 *
 * <p><b>Nu e o tranzacție.</b> Evidența se reconstruiește la citire când e învechită, sub un lacăt pe
 * firmă ({@link EvidenceCalculator#blockers}); o tranzacție peste toată bucla ar ține lacătul primei
 * firme până la ultima. Fiecare firmă își ia și își eliberează lacătul pe rând.
 *
 * <p>⚠️ Cost: câteva interogări pe firmă, deci liniar în mărimea portofoliului. La zeci de firme e
 * sub o secundă; la sute, cifrele trebuie ținute în cache.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConsultancyOverviewService {

    /**
     * Aceleași 60 de zile ca badge-ul din lista de parteneri ({@code PartnerService.EXPIRY_WARNING_DAYS})
     * și ca alerta pe mail: panoul și firma nu au voie să nu fie de acord ce înseamnă „expiră curând".
     */
    static final int PARTNER_WARNING_DAYS = 60;

    CompanyRepository companyRepository;
    ReportingDeadlineRepository deadlineRepository;
    WasteMovementRepository movementRepository;
    PartnerRepository partnerRepository;
    EvidenceCalculator evidenceCalculator;

    public List<ConsultancyOverviewResponse> overview() {
        return overview(LocalDate.now());
    }

    /**
     * Ordinea e cea în care se lucrează: întâi ce curge deja (termene depășite), apoi ce nu e în
     * regulă, apoi după cel mai apropiat termen; firmele fără nimic de făcut la coadă.
     */
    public List<ConsultancyOverviewResponse> overview(LocalDate today) {
        Consultancy consultancy = SecurityUtils.currentUser().getConsultancy();
        if (consultancy == null) {
            throw new NotFoundException(CONSULTANCY_NOT_FOUND);
        }
        return companyRepository.findAllByConsultancy_Id(consultancy.getId()).stream()
                .filter(Company::isActive)
                .map(c -> row(c, today))
                .sorted(Comparator
                        .comparing((ConsultancyOverviewResponse r) -> -r.overdueDeadlines())
                        .thenComparing(ConsultancyOverviewResponse::clear)
                        .thenComparing(r -> r.nextDeadline() == null ? null : r.nextDeadline().dueDate(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ConsultancyOverviewResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private ConsultancyOverviewResponse row(Company company, LocalDate today) {
        int year = today.getYear();

        // Un termen de anul trecut rămas deschis e tot depășit; unul de anul viitor poate fi „următorul"
        // în decembrie. Mai departe de atât nu se generează nimic.
        List<ReportingDeadline> open = deadlineRepository
                .findAllByCompany_IdAndStatusNotAndDueDateBetweenOrderByDueDateAsc(company.getId(),
                        DeadlineStatus.DONE, LocalDate.of(year - 1, 1, 1), LocalDate.of(year + 1, 12, 31));
        int overdue = (int) open.stream().filter(d -> d.getDueDate().isBefore(today)).count();
        NextDeadline next = open.stream()
                .filter(d -> !d.getDueDate().isBefore(today))
                .findFirst()
                .map(d -> new NextDeadline(d.getReportType(), d.getDueDate()))
                .orElse(null);
        boolean generated = deadlineRepository.existsByCompany_IdAndDueDateBetween(
                company.getId(), LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));

        EvidenceCalculator.EvidenceBlockers blockers = evidenceCalculator.blockers(company.getId(), year);
        long mirror = movementRepository.countUnprovenMirrorClassifications(
                company.getId(), LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));

        LocalDate cutoff = today.plusDays(PARTNER_WARNING_DAYS);
        int partners = (int) partnerRepository.findAllByCompany_Id(company.getId()).stream()
                .filter(Partner::isActive)
                .map(Partner::authorizationValidUntil)
                .filter(Objects::nonNull)
                .filter(until -> !until.isAfter(cutoff))
                .count();

        return new ConsultancyOverviewResponse(company.getId(), company.getName(), company.getCui(),
                company.getType(), overdue, next, generated,
                blockers.linesWithoutOperationCode(), blockers.linesAwaitingWeighing(), (int) mirror,
                partners);
    }
}
