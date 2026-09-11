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
import java.util.Set;

@Component
public class WasteMovementMapper {

    /**
     * @param codesWithBulletin the waste codes this tenant holds an analysis bulletin for, read
     *                          once by the caller. Passed in rather than looked up here: the mapper
     *                          runs per row, and a query per row would be an N+1 on the busiest
     *                          screen in the application.
     */
    public WasteMovementResponse toResponse(WasteMovement m, Set<String> codesWithBulletin) {
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
                mirrorClassificationUnproven(m, codesWithBulletin),
                m.getWasteCode().getMirrorOf(),
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
                m.getAnexa2Number(),
                m.getAnexa2ApprovalNumber(),
                m.getAnexa2Packaging(),
                m.getAnexa2BelowOneTon(),
                m.getPackagingOnMarket(),
                PackagingMaterial.isPackagingCode(m.getWasteCode().getCode())
                        && !Boolean.FALSE.equals(m.getPackagingOnMarket()),
                m.getPackagingMaterial(),
                PackagingMaterial.resolve(m.getPackagingMaterial(), m.getWasteCode().getCode())
                        .orElse(null),
                m.getPackagingCategory(),
                m.getPackagingReusable(),
                m.getPackagingHazardousContent(),
                m.getPackagingOrigin(),
                // What the form will print: the movement's answer, or the partner's.
                ro.ecoregistru.enums.PackagingOrigin.resolve(
                                m.getPackagingOrigin(),
                                partner == null ? null : partner.getPackagingOrigin())
                        .orElse(null),
                PackagingMaterial.isPackagingCode(m.getWasteCode().getCode()),
                m.getCreatedAt(),
                m.getUpdatedAt()
        );
    }

    public AttachmentResponse toAttachmentResponse(Attachment a) {
        return new AttachmentResponse(a.getId(), a.getFileName(), a.getContentType());
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

    /**
     * A mirror code declared non-hazardous, with nothing attached to justify it — G-4, built
     * 11.09.2026.
     *
     * <p>OUG 92/2021 art. 8 alin. (2): where the same waste falls under two codes depending on the
     * possible presence of hazardous properties, classifying it as <b>non-hazardous</b> is allowed
     * <i>"numai în baza unei analize a originii, testelor, buletinelor de analiză şi a altor
     * documente relevante"</i>. Every other check in this module looks for a missing field; this one
     * looks at a claim the client has made, which is why it exists at all — it is the kind of thing
     * an inspector goes looking for, because a mirror code declared non-hazardous makes disposal
     * cheaper.
     *
     * <p>Three deliberate restrictions:
     * <ul>
     *   <li><b>Mirror codes only.</b> {@code mirrorOf} is null for 681 of the 842 codes, and always
     *       null on a hazardous one — the article conditions the classification <em>as
     *       non-hazardous</em>, so a code declared hazardous needs nothing proved.</li>
     *   <li><b>Two things clear it, and the article is why there are two.</b> An <b>analysis
     *       bulletin on this code</b> (felia G-7, 11.09.2026) is the proof the article names, and
     *       the clean source: it hangs off the code, exactly as art. 8 alin. (4) asks. An
     *       <b>attachment on the movement</b> is the weaker one, and it stays — the same sentence
     *       admits "alte documente relevante", and a supplier declaration or an origin note is one.
     *       Dropping it when G-7 arrived would have narrowed the rule past what the act says, and
     *       would have lit the badge on movements that were already documented.</li>
     *   <li><b>Neither is verification.</b> The application cannot read a PDF and decide whether it
     *       is a laboratory report, and pretending otherwise would turn a warning into a lie. The
     *       question stays "where is the paper?", not "is it the right paper?" — G-7 gave the
     *       strong half of the answer somewhere to live, not a way to check it.</li>
     *   <li><b>All operations, not just exits.</b> Unlike the expired-authorization warning, this
     *       one is not about a handover: the classification travels with the waste from the moment
     *       it is written down, and it is the holder who answers for it (art. 8 alin. (1)).</li>
     * </ul>
     *
     * <p>It constată, nu blochează — same family as the expired authorization above, and shown on
     * screen only. It must never be printed on an official form: Anexa 1 and Anexa 3 carry what the
     * act asks for, not our reading of it.
     */
    private boolean mirrorClassificationUnproven(WasteMovement m, Set<String> codesWithBulletin) {
        return m.getWasteCode().getMirrorOf() != null
                && m.getAttachments().isEmpty()
                && !codesWithBulletin.contains(m.getWasteCode().getCode());
    }
}
