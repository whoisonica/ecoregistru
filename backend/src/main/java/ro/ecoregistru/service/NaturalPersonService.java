package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.NaturalPersonRequest;
import ro.ecoregistru.controller.response.NaturalPersonResponse;
import ro.ecoregistru.controller.response.NaturalPersonSummary;
import ro.ecoregistru.entity.NaturalPerson;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.NaturalPersonRepository;
import ro.ecoregistru.repository.WeighingOperationRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.util.ValidCnp;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.NATURAL_PERSON_CNP_TAKEN;
import static ro.ecoregistru.exception.ErrorMessageEnum.NATURAL_PERSON_DELETE_REQUIRES_DEACTIVATION;
import static ro.ecoregistru.exception.ErrorMessageEnum.NATURAL_PERSON_HAS_OPERATIONS;
import static ro.ecoregistru.exception.ErrorMessageEnum.NATURAL_PERSON_NAME_REQUIRED;
import static ro.ecoregistru.exception.ErrorMessageEnum.NATURAL_PERSON_NOT_FOUND;

/**
 * Fișele persoanelor fizice de la care depozitul cumpără (D1.7b).
 *
 * <p>Lista iese cu CNP-ul mascat; fișa întreagă o primește doar formularul. Persoana care apare pe o
 * operațiune nu se șterge definitiv, fiindcă borderoul se păstrează 10 ani (Legea 82/1991 art. 25):
 * rămâne dezactivată, iar datele pleacă la termen, prin {@link NaturalPersonRetentionScheduler}.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NaturalPersonService {

    NaturalPersonRepository personRepository;
    WeighingOperationRepository operationRepository;
    CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public List<NaturalPersonSummary> list() {
        UUID tenantId = TenantContext.require();
        Set<UUID> used = operationRepository.findNaturalPersonIdsWithOperations(tenantId);
        return personRepository.findAllByCompany_IdOrderByNameAsc(tenantId).stream()
                .map(p -> new NaturalPersonSummary(p.getId(), p.getName(), lastDigits(p.getCnp()),
                        metalReady(p), p.isActive(), used.contains(p.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public NaturalPersonResponse get(UUID id) {
        return toResponse(requireOwn(id));
    }

    @Transactional
    public NaturalPersonResponse create(NaturalPersonRequest request) {
        UUID tenantId = TenantContext.require();
        String cnp = blankToNull(request.cnp());
        if (cnp != null && personRepository.existsByCompany_IdAndCnp(tenantId, cnp)) {
            throw new BusinessException(NATURAL_PERSON_CNP_TAKEN);
        }
        NaturalPerson person = NaturalPerson.builder()
                .company(companyRepository.getReferenceById(tenantId))
                .name(requireName(request))
                .cnp(cnp)
                .identification(blankToNull(request.identification()))
                .address(blankToNull(request.address()))
                .active(true)
                .createdAt(Instant.now())
                .build();
        personRepository.save(person);
        return toResponse(person);
    }

    @Transactional
    public NaturalPersonResponse update(UUID id, NaturalPersonRequest request) {
        NaturalPerson person = requireOwn(id);
        String cnp = blankToNull(request.cnp());
        if (cnp != null && personRepository.existsByCompany_IdAndCnpAndIdNot(person.getCompany().getId(), cnp, id)) {
            throw new BusinessException(NATURAL_PERSON_CNP_TAKEN);
        }
        person.setName(requireName(request));
        person.setCnp(cnp);
        person.setIdentification(blankToNull(request.identification()));
        person.setAddress(blankToNull(request.address()));
        return toResponse(person);
    }

    /** Scoate persoana din lista de ales. Operațiunile vechi o păstrează. */
    @Transactional
    public void deactivate(UUID id) {
        requireOwn(id).setActive(false);
    }

    @Transactional
    public void reactivate(UUID id) {
        requireOwn(id).setActive(true);
    }

    /**
     * Șterge fișa, doar dacă persoana e dezactivată (un clic greșit să nu fie ireversibil, ca la șoferi)
     * și n-a vândut niciodată: o fișă introdusă din greșeală, nu una de pe un borderou.
     */
    @Transactional
    public void delete(UUID id) {
        NaturalPerson person = requireOwn(id);
        if (person.isActive()) {
            throw new BusinessException(NATURAL_PERSON_DELETE_REQUIRES_DEACTIVATION);
        }
        if (operationRepository.existsByNaturalPerson_Id(id)) {
            throw new BusinessException(NATURAL_PERSON_HAS_OPERATIONS);
        }
        personRepository.delete(person);
    }

    private NaturalPerson requireOwn(UUID id) {
        return personRepository.findByIdAndCompany_Id(id, TenantContext.require())
                .orElseThrow(() -> new NotFoundException(NATURAL_PERSON_NOT_FOUND));
    }

    private NaturalPersonResponse toResponse(NaturalPerson p) {
        return new NaturalPersonResponse(p.getId(), p.getName(), p.getCnp(), p.getIdentification(),
                p.getAddress(), p.isActive(), p.getId() != null && operationRepository.existsByNaturalPerson_Id(p.getId()));
    }

    /** Aceeași condiție pe care o cere {@code WeighingOperationService} la o linie de metal. */
    private static boolean metalReady(NaturalPerson p) {
        return p.getCnp() != null && ValidCnp.Validator.isValidCnp(p.getCnp())
                && p.getIdentification() != null && !p.getIdentification().isBlank()
                && p.getAddress() != null && !p.getAddress().isBlank();
    }

    private static String lastDigits(String cnp) {
        return cnp == null || cnp.length() < 4 ? null : cnp.substring(cnp.length() - 4);
    }

    private static String requireName(NaturalPersonRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new BusinessException(NATURAL_PERSON_NAME_REQUIRED);
        }
        return request.name().trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
