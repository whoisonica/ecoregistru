package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ro.ecoregistru.controller.response.AnalysisBulletinResponse;
import ro.ecoregistru.entity.AnalysisBulletin;
import ro.ecoregistru.entity.WasteCode;
import ro.ecoregistru.exception.BadRequestException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.AnalysisBulletinRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.security.SecurityUtils;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.WasteMovementService.AttachmentContent;
import ro.ecoregistru.util.WasteCodeLabel;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.*;
import static ro.ecoregistru.service.WasteMovementService.MAX_ATTACHMENT_BYTES;

/**
 * The analysis bulletins of a tenant — OUG 92/2021 art. 8 alin. (4) and art. 48 alin. (2).
 *
 * <p>This is felia G-7, and the thing it changes is <em>where</em> the paper hangs. Before it, a
 * bulletin could only be attached to a <b>movement</b>, so nothing tied it to the waste code it
 * characterises and nothing could say it was missing. The dossier admitted as much in its own
 * words: "Aplicația nu ține încă buletinele de analiză". Now the key is (company, waste code),
 * which is what art. 8 alin. (4) asks for, and the dossier reports coverage per code instead of
 * naming the obligation and stopping.
 *
 * <p>Storage is the movement attachment's, unchanged: {@code type=authenticated}, bytes served
 * through our own endpoint after a tenant check, no URL ever handed to a client. A bulletin names
 * the firm, its site and the chemistry of its waste; it is the last file in this application that
 * should sit at a guessable address.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AnalysisBulletinService {

    AnalysisBulletinRepository bulletinRepository;
    WasteCodeRepository wasteCodeRepository;
    CompanyRepository companyRepository;
    CloudinaryStorageService storageService;

    @Transactional(readOnly = true)
    public List<AnalysisBulletinResponse> list() {
        UUID tenantId = TenantContext.require();
        return bulletinRepository.findAllByCompany_IdOrderByIssueDateDescCreatedAtDesc(tenantId)
                .stream().map(AnalysisBulletinService::toResponse).toList();
    }

    /**
     * The codes this tenant holds a bulletin for. Read once per dossier build and once per movement
     * listing, never per row — see {@link AnalysisBulletinRepository#findCoveredWasteCodes}.
     */
    @Transactional(readOnly = true)
    public Set<String> coveredWasteCodes() {
        return Set.copyOf(bulletinRepository.findCoveredWasteCodes(TenantContext.require()));
    }

    /**
     * Uploads one bulletin against one waste code.
     *
     * <p>The file is <b>required</b>, and that is the whole object: a row without one would be a
     * claim that the characterisation exists, printed into a dossier an inspector reads, with
     * nothing behind it. Art. 48 alin. (2) obliges the holder to <em>deţină</em> the bulletin — to
     * hold the paper — not to declare that it exists somewhere.
     *
     * <p>A future issue date is refused. The date is the one on the laboratory's paper, and a
     * bulletin cannot have been issued tomorrow; letting it through would put a date into the
     * dossier that an inspector can see is impossible.
     */
    @Transactional
    public AnalysisBulletinResponse create(UUID wasteCodeId, LocalDate issueDate, String laboratory,
                                           MultipartFile file) {
        UUID tenantId = TenantContext.require();

        if (file == null || file.isEmpty()) {
            throw new BadRequestException(BULLETIN_FILE_REQUIRED);
        }
        if (file.getSize() > MAX_ATTACHMENT_BYTES) {
            throw new BadRequestException(ATTACHMENT_TOO_LARGE);
        }
        if (laboratory == null || laboratory.isBlank()) {
            throw new BadRequestException(BULLETIN_LABORATORY_REQUIRED);
        }
        if (issueDate == null) {
            throw new BadRequestException(BULLETIN_ISSUE_DATE_REQUIRED);
        }
        if (issueDate.isAfter(LocalDate.now())) {
            throw new BadRequestException(BULLETIN_ISSUE_DATE_IN_FUTURE);
        }
        WasteCode wasteCode = wasteCodeRepository.findById(wasteCodeId)
                .orElseThrow(() -> new NotFoundException(WASTE_CODE_NOT_FOUND));

        var stored = storageService.upload(file, "bulletins/" + tenantId);
        AnalysisBulletin bulletin = AnalysisBulletin.builder()
                .company(companyRepository.getReferenceById(tenantId))
                .wasteCode(wasteCode)
                .issueDate(issueDate)
                .laboratory(laboratory.trim())
                .url(stored.url())
                .publicId(stored.publicId())
                .resourceType(stored.resourceType())
                .deliveryType(stored.deliveryType())
                .format(stored.format())
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .createdAt(Instant.now())
                .createdBy(SecurityUtils.currentUser().getId())
                .build();
        return toResponse(bulletinRepository.save(bulletin));
    }

    /**
     * Deletes a bulletin uploaded by mistake.
     *
     * <p>It really deletes, unlike almost everything else here, and the reason is that nothing
     * else points at it: a bulletin is not quoted by a movement, an evidence line or a printed
     * form, so removing it rewrites no document. What the client must keep for three years
     * (art. 48 alin. (5)) is the evidence, and that is untouched.
     */
    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.require();
        AnalysisBulletin bulletin = bulletinRepository.findByIdAndCompany_Id(id, tenantId)
                .orElseThrow(() -> new NotFoundException(BULLETIN_NOT_FOUND));
        storageService.delete(bulletin.getPublicId(), bulletin.getResourceType(),
                bulletin.getDeliveryType());
        bulletinRepository.delete(bulletin);
    }

    /** The bytes, for a caller who has proved the bulletin is their company's. Same shape as 11-bis. */
    @Transactional(readOnly = true)
    public AttachmentContent content(UUID id) {
        UUID tenantId = TenantContext.require();
        AnalysisBulletin bulletin = bulletinRepository.findByIdAndCompany_Id(id, tenantId)
                .orElseThrow(() -> new NotFoundException(BULLETIN_NOT_FOUND));
        try {
            String url = bulletin.getDeliveryType() == null
                    ? bulletin.getUrl()
                    : storageService.signedUrl(bulletin.getPublicId(), bulletin.getResourceType(),
                            bulletin.getDeliveryType(), bulletin.getFormat());
            return new AttachmentContent(storageService.fetch(url), bulletin.getContentType(),
                    bulletin.getFileName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ATTACHMENT_FETCH_FAILED.getMessage(), e);
        } catch (Exception e) {
            log.warn("Bulletin fetch failed for id={}", id, e);
            throw new IllegalStateException(ATTACHMENT_FETCH_FAILED.getMessage(), e);
        }
    }

    private static AnalysisBulletinResponse toResponse(AnalysisBulletin b) {
        WasteCode code = b.getWasteCode();
        return new AnalysisBulletinResponse(
                b.getId(),
                code.getId(),
                WasteCodeLabel.official(code.getCode(), code.isHazardous()),
                code.getName(),
                code.isHazardous(),
                b.getIssueDate(),
                b.getLaboratory(),
                b.getFileName());
    }
}
