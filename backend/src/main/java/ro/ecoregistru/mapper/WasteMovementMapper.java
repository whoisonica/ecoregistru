package ro.ecoregistru.mapper;

import org.springframework.stereotype.Component;
import ro.ecoregistru.controller.response.AttachmentResponse;
import ro.ecoregistru.controller.response.WasteMovementResponse;
import ro.ecoregistru.entity.Attachment;
import ro.ecoregistru.entity.InternalGenerator;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.enums.PackagingMaterial;

import java.util.List;

@Component
public class WasteMovementMapper {

    public WasteMovementResponse toResponse(WasteMovement m) {
        Partner partner = m.getPartner();
        Partner carrier = m.getTransportPartner();
        InternalGenerator section = m.getInternalGenerator();
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
                m.isWeighedAtUnloading(),
                m.getVolumeM3(),
                m.getUnit(),
                m.getOperation(),
                m.getRegister(),
                m.getOperationCode() != null ? m.getOperationCode().treatmentPurpose() : null,
                m.getPhysicalState(),
                m.getStorageType(),
                m.getTreatmentMethod(),
                m.getTransportMeans(),
                m.getWasteDestination(),
                m.getOperationCode(),
                partner != null ? partner.getId() : null,
                partner != null ? partner.getName() : null,
                section != null ? section.getId() : null,
                section != null ? section.getName() : null,
                m.getDocumentReference(),
                m.getNotes(),
                attachments,
                m.getClientGeneratedId(),
                m.getUnloadDate(),
                m.getPartnerWorkPoint() == null ? null : m.getPartnerWorkPoint().getId(),
                m.getPartnerWorkPoint() == null ? null : m.getPartnerWorkPoint().label(),
                carrier != null ? carrier.getId() : null,
                carrier != null ? carrier.getName() : null,
                m.getDriverName(),
                m.getDriverIdentification(),
                m.getVehicleRegistration(),
                new java.util.LinkedHashSet<>(m.getTransportDestinations()),
                m.getAnexa3Series(),
                m.getAnexa3Number(),
                m.getAnexa3Unit(),
                m.getPackagingMaterial(),
                PackagingMaterial.resolve(m.getPackagingMaterial(), m.getWasteCode().getCode())
                        .orElse(null),
                m.getPackagingCategory(),
                m.getPackagingReusable(),
                m.getPackagingHazardousContent(),
                PackagingMaterial.isPackagingCode(m.getWasteCode().getCode()),
                m.getCreatedAt(),
                m.getUpdatedAt()
        );
    }

    public AttachmentResponse toAttachmentResponse(Attachment a) {
        return new AttachmentResponse(a.getId(), a.getUrl(), a.getFileName(), a.getContentType());
    }
}
