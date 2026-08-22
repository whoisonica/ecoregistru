package ro.ecoregistru.mapper;

import org.springframework.stereotype.Component;
import ro.ecoregistru.controller.response.AttachmentResponse;
import ro.ecoregistru.controller.response.WasteMovementResponse;
import ro.ecoregistru.entity.Attachment;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteMovement;

import java.util.List;

@Component
public class WasteMovementMapper {

    public WasteMovementResponse toResponse(WasteMovement m) {
        Partner partner = m.getPartner();
        List<AttachmentResponse> attachments = m.getAttachments().stream()
                .map(this::toAttachmentResponse)
                .toList();

        return new WasteMovementResponse(
                m.getId(),
                m.getWorkPoint().getId(),
                m.getWorkPoint().getName(),
                m.getDate(),
                m.getWasteCode().getId(),
                m.getWasteCode().getCode(),
                m.getWasteCode().getName(),
                m.getWasteCode().isHazardous(),
                m.getQuantity(),
                m.getUnit(),
                m.getOperation(),
                m.getRegister(),
                m.getOperationCode() != null ? m.getOperationCode().treatmentPurpose() : null,
                m.getPhysicalState(),
                m.getOperationCode(),
                partner != null ? partner.getId() : null,
                partner != null ? partner.getName() : null,
                m.getDocumentReference(),
                m.getNotes(),
                attachments,
                m.getClientGeneratedId(),
                m.getCreatedAt(),
                m.getUpdatedAt()
        );
    }

    public AttachmentResponse toAttachmentResponse(Attachment a) {
        return new AttachmentResponse(a.getId(), a.getUrl(), a.getFileName(), a.getContentType());
    }
}
