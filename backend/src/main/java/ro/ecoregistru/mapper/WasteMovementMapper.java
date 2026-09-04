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
                authorizationExpiredAtHandover(m, partner),
                partner != null ? partner.getAuthorizationExpiry() : null,
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
                m.getPackagingOnMarket(),
                PackagingMaterial.isPackagingCode(m.getWasteCode().getCode())
                        && !Boolean.FALSE.equals(m.getPackagingOnMarket()),
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

    /**
     * Whether the recipient's environmental authorization had already lapsed on the day the waste
     * left the site — audit point 5, built 04.09.2026.
     *
     * <p>OUG 92/2021 art. 23 alin. (1) makes the handover legal only towards an <em>authorized</em>
     * operator, and art. 24 alin. (1) adds that handing over does not discharge responsibility. Yet
     * the only check in the application was {@code PartnerService}'s {@code expiringSoon} badge,
     * computed against <b>today</b> at 60 days and shown in the partner list. Nothing compared the
     * expiry with the date of the movement, so Anexa 3 could print — silently — a transport
     * document towards an operator who was not authorized on the day it names.
     *
     * <p>Three deliberate restrictions:
     * <ul>
     *   <li><b>Exits only.</b> An intake or a generation has no recipient to be authorized.</li>
     *   <li><b>A recorded expiry only.</b> A blank field means the client has not filled it in;
     *       regula de lucru 1 says a gap must be visible as a gap, not converted into a finding
     *       against the client.</li>
     *   <li><b>Strictly before the movement date.</b> An authorization valid <em>on</em> its expiry
     *       day is valid, so the comparison is {@code expiry.isBefore(date)} — the last day counts
     *       for the client, which is the direction an ambiguity on an official form should fall.</li>
     * </ul>
     */
    private boolean authorizationExpiredAtHandover(WasteMovement m, Partner partner) {
        if (partner == null || !m.getOperation().isExit()) {
            return false;
        }
        return partner.getAuthorizationExpiry() != null
                && m.getDate() != null
                && partner.getAuthorizationExpiry().isBefore(m.getDate());
    }
}
