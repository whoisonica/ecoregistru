package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.ScaleEventRequest;
import ro.ecoregistru.controller.request.ScaleRequest;
import ro.ecoregistru.controller.response.ScaleResponse;
import ro.ecoregistru.entity.Scale;
import ro.ecoregistru.entity.ScaleEvent;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.ScaleEventKind;
import ro.ecoregistru.enums.ScaleStatus;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.ScaleEventRepository;
import ro.ecoregistru.repository.ScaleRepository;
import ro.ecoregistru.repository.WeighingOperationRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.SecurityUtils;
import ro.ecoregistru.security.TenantContext;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static ro.ecoregistru.exception.ErrorMessageEnum.*;

/**
 * Cântarele depozitului (D2.3): fișa, istoricul și starea legală la o dată ({@link ScaleLegality}).
 * Aceleași drepturi ca la flotă: scrie oricine scrie în firmă, iar jurnalul de audit ține cine a
 * trecut ce buletin.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ScaleService {

    static final Set<String> ACCURACY_CLASSES = Set.of("I", "II", "III", "IIII");

    ScaleRepository scaleRepository;
    ScaleEventRepository eventRepository;
    CompanyRepository companyRepository;
    WorkPointRepository workPointRepository;
    WeighingOperationRepository operationRepository;

    @Transactional(readOnly = true)
    public List<ScaleResponse> list() {
        List<Scale> scales = scaleRepository.findAllForCompany(TenantContext.require());
        Map<UUID, List<ScaleEvent>> events = eventRepository
                .findAllByScale_IdIn(scales.stream().map(Scale::getId).toList()).stream()
                .collect(Collectors.groupingBy(e -> e.getScale().getId()));
        LocalDate today = DeadlineService.today();
        return scales.stream()
                .map(s -> toResponse(s, newestFirst(events.getOrDefault(s.getId(), List.of())), today))
                .toList();
    }

    @Transactional
    public ScaleResponse create(ScaleRequest request) {
        UUID tenantId = TenantContext.require();
        Scale scale = Scale.builder()
                .company(companyRepository.getReferenceById(tenantId))
                .createdAt(Instant.now())
                .build();
        apply(scale, request, tenantId);
        scaleRepository.save(scale);
        return toResponse(scale, List.of(), DeadlineService.today());
    }

    @Transactional
    public ScaleResponse update(UUID id, ScaleRequest request) {
        UUID tenantId = TenantContext.require();
        Scale scale = require(id, tenantId);
        apply(scale, request, tenantId);
        return response(scale);
    }

    /** Doar un cântar fără cântăriri; unul folosit se trece „Scos din uz”, ca operațiunile să-l numească. */
    @Transactional
    public void delete(UUID id) {
        Scale scale = require(id, TenantContext.require());
        if (operationRepository.existsByScale_Id(id)) {
            throw new BusinessException(SCALE_HAS_WEIGHINGS);
        }
        scaleRepository.delete(scale);
    }

    @Transactional
    public ScaleResponse addEvent(UUID scaleId, ScaleEventRequest request) {
        Scale scale = require(scaleId, TenantContext.require());
        ScaleEvent event = ScaleEvent.builder()
                .scale(scale)
                .createdBy(SecurityUtils.currentUser().getId())
                .createdAt(Instant.now())
                .build();
        apply(event, request);
        eventRepository.save(event);
        return response(scale);
    }

    @Transactional
    public ScaleResponse updateEvent(UUID scaleId, UUID eventId, ScaleEventRequest request) {
        Scale scale = require(scaleId, TenantContext.require());
        apply(requireEvent(eventId, scaleId), request);
        return response(scale);
    }

    @Transactional
    public ScaleResponse deleteEvent(UUID scaleId, UUID eventId) {
        Scale scale = require(scaleId, TenantContext.require());
        eventRepository.delete(requireEvent(eventId, scaleId));
        eventRepository.flush();
        return response(scale);
    }

    /** Starea cântarului la data unei cântăriri — pentru operațiuni și alertă, în tranzacția lor. */
    public ScaleLegality.Verdict verdictAt(Scale scale, LocalDate date) {
        return verdict(scale, eventRepository.findAllByScale_IdOrderByDateDescCreatedAtDesc(scale.getId()), date);
    }

    /** Istoricul mai multor cântare dintr-o interogare — pentru liste, ca să nu coste un rând câte una. */
    public Map<UUID, List<ScaleEvent>> eventsOf(java.util.Collection<UUID> scaleIds) {
        if (scaleIds.isEmpty()) {
            return Map.of();
        }
        return eventRepository.findAllByScale_IdIn(scaleIds).stream()
                .collect(Collectors.groupingBy(e -> e.getScale().getId()));
    }

    private void apply(Scale scale, ScaleRequest request, UUID tenantId) {
        String name = blankToNull(request.name());
        if (name == null) {
            throw new BusinessException(SCALE_NAME_REQUIRED);
        }
        WorkPoint depot = workPointRepository.findByIdAndCompany_Id(request.workPointId(), tenantId)
                .orElseThrow(() -> new NotFoundException(WORK_POINT_NOT_FOUND));
        scaleRepository.findByCompany_IdAndWorkPoint_IdAndName(tenantId, depot.getId(), name)
                .filter(other -> !other.getId().equals(scale.getId()))
                .ifPresent(other -> {
                    throw new BusinessException(SCALE_NAME_TAKEN);
                });
        String accuracyClass = blankToNull(request.accuracyClass());
        if (accuracyClass != null && !ACCURACY_CLASSES.contains(accuracyClass)) {
            throw new BusinessException(SCALE_CLASS_UNKNOWN);
        }
        if (request.divisionKg() != null && request.divisionKg().signum() <= 0) {
            throw new BusinessException(SCALE_DIVISION_NOT_POSITIVE);
        }
        scale.setWorkPoint(depot);
        scale.setName(name);
        scale.setSerialNumber(blankToNull(request.serialNumber()));
        scale.setKind(blankToNull(request.kind()));
        scale.setAccuracyClass(accuracyClass);
        scale.setDivisionKg(request.divisionKg());
        scale.setCommissionedOn(request.commissionedOn());
        scale.setBrmlDeclaredOn(request.brmlDeclaredOn());
        scale.setBrmlReference(blankToNull(request.brmlReference()));
        scale.setStatus(request.status() == null ? ScaleStatus.IN_USE : request.status());
    }

    /**
     * La verificare: rezultatul și buletinul sunt obligatorii (IML 3-05 art. 17 alin. (2)); la ADMIS,
     * „valabil până la” e data + 12 luni dacă lipsește și niciodată mai mult (L.O.-2022). Reparația și
     * incidentul n-au rezultat, buletin sau valabilitate: ce vine pe cerere pentru ele se ignoră.
     */
    private static void apply(ScaleEvent event, ScaleEventRequest request) {
        if (request.kind() == null) {
            throw new BusinessException(SCALE_EVENT_KIND_REQUIRED);
        }
        if (request.date() == null) {
            throw new BusinessException(SCALE_EVENT_DATE_REQUIRED);
        }
        Boolean admitted = null;
        String bulletin = null;
        LocalDate validUntil = null;
        if (request.kind() == ScaleEventKind.VERIFICATION) {
            if (request.admitted() == null) {
                throw new BusinessException(SCALE_RESULT_REQUIRED);
            }
            bulletin = blankToNull(request.bulletinNumber());
            if (bulletin == null) {
                throw new BusinessException(SCALE_BULLETIN_REQUIRED);
            }
            admitted = request.admitted();
            if (admitted) {
                LocalDate longest = request.date().plusMonths(ScaleLegality.VALIDITY_MONTHS);
                validUntil = request.validUntil() == null ? longest : request.validUntil();
                if (validUntil.isAfter(longest)) {
                    throw new BusinessException(SCALE_VALID_UNTIL_TOO_LATE);
                }
                if (validUntil.isBefore(request.date())) {
                    throw new BusinessException(SCALE_VALID_UNTIL_BEFORE_DATE);
                }
            }
        }
        event.setKind(request.kind());
        event.setDate(request.date());
        event.setAdmitted(admitted);
        event.setBulletinNumber(bulletin);
        event.setValidUntil(validUntil);
        event.setLaboratory(blankToNull(request.laboratory()));
        event.setVerifier(blankToNull(request.verifier()));
        event.setNotes(blankToNull(request.notes()));
    }

    private ScaleResponse response(Scale scale) {
        eventRepository.flush();
        return toResponse(scale, eventRepository.findAllByScale_IdOrderByDateDescCreatedAtDesc(scale.getId()),
                DeadlineService.today());
    }

    static ScaleLegality.Verdict verdict(Scale scale, List<ScaleEvent> events, LocalDate date) {
        return ScaleLegality.at(scale.getStatus(), scale.getCommissionedOn(), scale.getBrmlDeclaredOn(),
                events.stream().map(e -> new ScaleLegality.Event(e.getKind(), e.getDate(), e.getAdmitted(),
                        e.getValidUntil(), e.getCreatedAt())).toList(),
                date);
    }

    private static List<ScaleEvent> newestFirst(List<ScaleEvent> events) {
        return events.stream()
                .sorted(java.util.Comparator.comparing(ScaleEvent::getDate)
                        .thenComparing(ScaleEvent::getCreatedAt).reversed())
                .toList();
    }

    private Scale require(UUID id, UUID tenantId) {
        return scaleRepository.findByIdAndCompany_Id(id, tenantId)
                .orElseThrow(() -> new NotFoundException(SCALE_NOT_FOUND));
    }

    private ScaleEvent requireEvent(UUID eventId, UUID scaleId) {
        return eventRepository.findByIdAndScale_Id(eventId, scaleId)
                .orElseThrow(() -> new NotFoundException(SCALE_EVENT_NOT_FOUND));
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    static ScaleResponse toResponse(Scale s, List<ScaleEvent> events, LocalDate today) {
        ScaleLegality.Verdict now = verdict(s, events, today);
        WorkPoint depot = s.getWorkPoint();
        return new ScaleResponse(s.getId(), depot.getId(), depot.getName(), s.getName(), s.getSerialNumber(),
                s.getKind(), s.getAccuracyClass(), s.getDivisionKg(), s.getCommissionedOn(), s.getBrmlDeclaredOn(),
                s.getBrmlReference(), s.getStatus(), now.state(), now.validUntil(),
                events.stream().map(e -> new ScaleResponse.Event(e.getId(), e.getKind(), e.getDate(), e.getAdmitted(),
                        e.getBulletinNumber(), e.getValidUntil(), e.getLaboratory(), e.getVerifier(), e.getNotes()))
                        .toList());
    }
}
