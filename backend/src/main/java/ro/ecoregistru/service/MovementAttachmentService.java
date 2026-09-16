package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ro.ecoregistru.controller.response.AttachmentResponse;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.exception.BadRequestException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.mapper.WasteMovementMapper;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.security.TenantContext;

import java.time.Instant;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.*;

/**
 * Atașamentele unei mișcări: urcarea la Cloudinary, ștergerea și livrarea prin API, după ce
 * mișcarea s-a dovedit a firmei curente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MovementAttachmentService {

    /**
     * Cât are voie să aibă un atașament. Nu e un prag ales de noi: contul Cloudinary e pe planul
     * Free, unde limita e <b>10 MB per asset</b> — și pentru {@code image}, unde ajung PDF-urile,
     * și pentru {@code raw}, unde ajung Word și Excel. Peste ea uploadul cade <i>la furnizor</i>,
     * după ce a trecut de tot ce e al nostru, iar omul primește o eroare care nu spune nimic.
     *
     * <p>Verificarea stă aici, nu doar în dropzone. Până acum singurul gardian era JavaScript-ul
     * din browser — {@code file-dropzone.tsx}, și acela la 15 MB, adică <i>peste</i> zidul de la
     * Cloudinary — deci un {@code curl} direct pe API trecea cu orice încăpea în limita de
     * multipart, iar un fișier de 12 MB trecea chiar și prin interfață ca să cadă la capăt.
     */
    public static final long MAX_ATTACHMENT_BYTES = 10L * 1024 * 1024;

    WasteMovementRepository movementRepository;
    AttachmentRepository attachmentRepository;
    CloudinaryStorageService storageService;
    WasteMovementMapper mapper;

    @Transactional
    public AttachmentResponse addAttachment(UUID movementId, MultipartFile file) {
        UUID tenantId = TenantContext.require();
        WasteMovement movement = requireMovement(movementId, tenantId);
        if (file.getSize() > MAX_ATTACHMENT_BYTES) {
            throw new BadRequestException(ATTACHMENT_TOO_LARGE);
        }

        var stored = storageService.upload(file, "movements/" + movementId);
        Attachment attachment = Attachment.builder()
                .movement(movement)
                .url(stored.url())
                .publicId(stored.publicId())
                .resourceType(stored.resourceType())
                .deliveryType(stored.deliveryType())
                .format(stored.format())
                .fileName(safeFileName(file.getOriginalFilename()))
                .contentType(file.getContentType())
                .createdAt(Instant.now())
                .build();
        movement.getAttachments().add(attachment);
        attachmentRepository.save(attachment);
        return mapper.toAttachmentResponse(attachment);
    }

    /**
     * BUG-005. Numele fişierului vine de la client şi nu e verificat nicăieri: coloana
     * {@code file_name} e {@code VARCHAR(255)} din {@code V1}, deci un nume de 300 de caractere —
     * pe care un scaner îl produce fără rea intenţie — se oprea abia în lungimea coloanei, adică
     * <b>500 + alertă Sentry</b> după ce fişierul urcase deja la furnizor.
     *
     * <p>Se păstrează doar numele, nu şi drumul: un {@code ../../etc/passwd} devine {@code passwd}.
     * Nu fiindcă ar ajunge undeva pe disc — fişierul se urcă la Cloudinary sub o cale pe care o
     * scriem noi —, ci fiindcă rubrica asta e un <em>nume de fişier</em>, iar ce se scrie în ea
     * ajunge în antetul de descărcare şi pe ecran.
     *
     * <p>Trunchiere, nu refuz: numele e metadată de afişare, iar un om care a scanat un aviz n-are
     * ce învăţa dintr-o eroare despre lungimea unei coloane.
     */
    static String safeFileName(String original) {
        if (original == null || original.isBlank()) {
            return null;
        }
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        return name.length() > 255 ? name.substring(0, 255) : name;
    }

    @Transactional
    public void deleteAttachment(UUID movementId, UUID attachmentId) {
        UUID tenantId = TenantContext.require();
        requireMovement(movementId, tenantId); // enforces tenant ownership
        Attachment attachment = attachmentRepository.findByIdAndMovement_Id(attachmentId, movementId)
                .orElseThrow(() -> new NotFoundException(MOVEMENT_NOT_FOUND));
        storageService.delete(attachment.getPublicId(), attachment.getResourceType(),
                attachment.getDeliveryType());
        attachmentRepository.delete(attachment);
    }

    /**
     * The bytes of an attachment, for a caller who has already proved they may see the movement.
     *
     * <p>This is the whole of 11-bis. Before it, {@code AttachmentResponse} carried Cloudinary's
     * {@code secure_url} and the browser fetched the file straight from the CDN — no session, no
     * tenant check, forever, on an application through which handover notes, contracts and
     * drivers' identity documents pass, and through which CNPs will pass at Etapa 9. The tenant
     * check now happens here, in the same place and the same way as every other read: {@link
     * #requireMovement} scopes by {@code TenantContext}, so another company's attachment is a 404
     * and not a file.
     */
    @Transactional(readOnly = true)
    public AttachmentContent attachmentContent(UUID movementId, UUID attachmentId) {
        UUID tenantId = TenantContext.require();
        requireMovement(movementId, tenantId); // enforces tenant ownership
        Attachment attachment = attachmentRepository.findByIdAndMovement_Id(attachmentId, movementId)
                .orElseThrow(() -> new NotFoundException(ATTACHMENT_NOT_FOUND));
        try {
            return new AttachmentContent(
                    storageService.fetch(deliveryUrl(attachment)),
                    attachment.getContentType(),
                    attachment.getFileName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ATTACHMENT_FETCH_FAILED.getMessage(), e);
        } catch (Exception e) {
            log.warn("Attachment fetch failed for id={}", attachmentId, e);
            throw new IllegalStateException(ATTACHMENT_FETCH_FAILED.getMessage(), e);
        }
    }

    /**
     * Where to read the file from. Rows uploaded since 11-bis are {@code authenticated} and get a
     * freshly signed URL; older rows have null coordinates and keep their stored URL, which is
     * public — that is the migration's own note, and the reason those files are worth re-uploading
     * rather than left alone.
     */
    private String deliveryUrl(Attachment a) {
        if (a.getDeliveryType() == null) {
            return a.getUrl();
        }
        return storageService.signedUrl(a.getPublicId(), a.getResourceType(),
                a.getDeliveryType(), a.getFormat());
    }

    public record AttachmentContent(byte[] bytes, String contentType, String fileName) {}

    private WasteMovement requireMovement(UUID id, UUID tenantId) {
        return movementRepository.findByIdAndCompany_IdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new NotFoundException(MOVEMENT_NOT_FOUND));
    }
}
