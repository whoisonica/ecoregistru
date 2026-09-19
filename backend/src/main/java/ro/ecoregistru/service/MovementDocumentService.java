package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.response.WasteMovementResponse;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.security.TenantContext;

import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.*;

/**
 * Documentele tipărite dintr-o mișcare înregistrată: Anexa 3 și Anexa 2 (HG 1061/2008), avizul
 * de însoțire și cifra din spatele bifei „&lt; 1t/an”.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MovementDocumentService {

    WasteMovementRepository movementRepository;
    CompanyRepository companyRepository;
    Anexa3Numbering anexa3Numbering;
    ro.ecoregistru.service.export.Anexa3FormGenerator anexa3FormGenerator;
    ro.ecoregistru.service.export.Anexa2FormGenerator anexa2FormGenerator;
    ro.ecoregistru.service.export.AvizGenerator avizGenerator;
    Anexa2ThresholdCalculator anexa2ThresholdCalculator;
    SubscriptionService subscriptionService;

    /**
     * Renders Anexa 3 la HG 1061/2008 for a movement that is already recorded, allocating the
     * form's number the first time so a reprint stays the same document.
     *
     * <p>Two refusals, both legal rather than technical. The form covers a handover — it names an
     * expeditor and a destinatar — so a movement with no partner has nobody to print on the right
     * half. And its title says <em>nepericuloase</em>: a hazardous code belongs on the expedition
     * form of anexa 2, which is a different document we do not produce yet, so we say that instead
     * of printing the wrong one.
     *
     * <p><b>And one thing that is deliberately not a third refusal</b> (audit point 5, decided
     * 04.09.2026): an expired recipient authorization. Both refusals above say "this is the wrong
     * document"; a lapsed authorization does not make Anexa 3 the wrong document, because the
     * handover really did happen and the client still has to be able to reconstruct an old file.
     * Refusing would mean the application declines to document reality. The warning is carried
     * instead on {@code WasteMovementResponse#recipientAuthorizationExpired} and shown on screen.
     *
     * <p><b>It must not be printed on the form.</b> The paper goes to the recipient and, at an
     * inspection, to the authority — a note of ours in its margin would be our own accusation
     * filed in the client's own dossier. On screen it is information; on paper it would be
     * evidence against the person we built it for.
     */
    @Transactional
    public byte[] renderAnexa3(UUID id) {
        UUID tenantId = TenantContext.require();
        WasteMovement movement = requireMovement(id, tenantId);
        Company company = requireCompany(tenantId);

        requireOutsideOperation(movement);
        if (!movement.getOperation().isExit() || movement.getPartner() == null) {
            throw new BusinessException(ANEXA3_REQUIRES_HANDOVER);
        }
        if (movement.getWasteCode().isHazardous()) {
            throw new BusinessException(ANEXA3_HAZARDOUS_NOT_ALLOWED);
        }
        if (movement.getAnexa3Number() == null) {
            // BUG-064: alocarea e o scriere, deși vine printr-un GET, pe care doar-citirea îl lasă să treacă.
            if (subscriptionService.readOnlyEnabled()
                    && subscriptionService.access(ro.ecoregistru.security.SecurityUtils.currentUser(), tenantId,
                    DeadlineService.today()).readOnly()) {
                throw new BusinessException(ANEXA3_NUMBER_READ_ONLY);
            }
            movement.setAnexa3Number(anexa3Numbering.next(tenantId));
            movement.setAnexa3Series(company.getAnexa3Series());
        }
        return anexa3FormGenerator.render(movement, company);
    }

    /**
     * Avizul de însoţire a mărfii (specialista, 15.09.2026). Aceeaşi predare ca la Anexa 3, dar
     * pentru orice cod, periculos inclusiv: avizul însoţeşte marfa, nu descrie deşeul. Nu alocă
     * nimic — numărul e referinţa documentului, scrisă de client — deci e o citire.
     */
    @Transactional(readOnly = true)
    public byte[] renderAviz(UUID id) {
        UUID tenantId = TenantContext.require();
        WasteMovement movement = requireMovement(id, tenantId);
        requireOutsideOperation(movement);
        if (!movement.getOperation().isExit() || movement.getPartner() == null) {
            throw new BusinessException(AVIZ_REQUIRES_HANDOVER);
        }
        return avizGenerator.render(movement, requireCompany(tenantId));
    }

    /**
     * Anexa 2 la HG 1061/2008, the hazardous-waste consignment form, as a PDF.
     *
     * <p>The mirror image of {@link #renderAnexa3(UUID)}, refusal for refusal — and deliberately
     * so, because the two forms are the two halves of the same question and a client who lands on
     * the wrong one should be told which is right, not told "no".
     *
     * <p><b>Three refusals, and each one says "this is the wrong document":</b>
     * <ul>
     *   <li><b>No handover.</b> The form names an expeditor and a destinatar and records a
     *       consignment; without a recipient there is nothing to consign.</li>
     *   <li><b>A non-hazardous code.</b> The title says <em>periculoase</em>. This is the same
     *       refusal Anexa 3 makes in the other direction, and as of this slice both of them can
     *       finally name a form that exists.</li>
     *   <li><b>Medical waste.</b> Art. 24 is not a variant of this flow, it is a different one: the
     *       <em>carrier</em> draws the forms up — "chiar dacă acesta este şi destinatar" — on the
     *       cumulated quantity of one round through an area, with a schedule of the individual
     *       expeditors attached. A clinic that printed this form as expeditor would be holding a
     *       document nobody asked it for. We do not produce the art. 24 one either: it belongs to
     *       the carrier and is built from a route we do not record.</li>
     * </ul>
     *
     * <p><b>Nothing is allocated here</b>, which is the one structural difference from Anexa 3.
     * There is no number to hand out: the model reserves it for the county agency. So this method
     * writes nothing to the movement, and a reprint is simply the same PDF again.
     */
    @Transactional
    public byte[] renderAnexa2(UUID id) {
        UUID tenantId = TenantContext.require();
        WasteMovement movement = requireMovement(id, tenantId);
        Company company = requireCompany(tenantId);

        // Numai la colectori — răspunsul specialistei din 14.09.2026: „anexa 2 o păstrăm doar pentru
        // colectori". Art. 8 numeşte expeditorul; decizia urmează practica ei, iar temeiul e scris în
        // intrebari-specialist.md. Refuzul vine primul, fiindcă nu depinde de mişcare.
        if (!company.getType().keepsArt48Register()) {
            throw new BusinessException(ANEXA2_COLLECTORS_ONLY);
        }
        if (!movement.getOperation().isExit() || movement.getPartner() == null) {
            throw new BusinessException(ANEXA2_REQUIRES_HANDOVER);
        }
        if (!movement.getWasteCode().isHazardous()) {
            throw new BusinessException(ANEXA2_NOT_HAZARDOUS);
        }
        if (isMedicalWaste(movement)) {
            throw new BusinessException(ANEXA2_MEDICAL_WASTE);
        }
        return anexa2FormGenerator.render(movement, company,
                anexa2ThresholdCalculator.belowOneTon(movement));
    }

    /**
     * What the "&lt; 1t/an" tick is proposed from, for the screen to show beside it.
     *
     * <p>Read-only and computed on demand rather than stored: the total moves whenever a movement
     * on the same code is recorded, edited or deleted, so a figure frozen onto this row would go
     * quietly stale — and stale in the direction that costs, since a total can only grow through
     * the threshold, never back.
     */
    @Transactional
    public ro.ecoregistru.controller.response.Anexa2ThresholdResponse anexa2Threshold(UUID id) {
        UUID tenantId = TenantContext.require();
        WasteMovement movement = requireMovement(id, tenantId);
        return anexa2ThresholdCalculator.forMovement(movement);
    }

    /**
     * Whether this is hazardous waste from medical activity, which art. 24 routes away from this
     * form altogether.
     *
     * <p>Chapter 18 of the nomenclator is "deşeuri rezultate din activităţi de îngrijire a
     * sănătăţii umane sau veterinare şi/sau din cercetări conexe", so a hazardous code in it is
     * exactly the subject of art. 24. Matching on the chapter rather than on a list of codes is
     * deliberate: a list would have to be maintained against the nomenclator, and forgetting one
     * would print the wrong document for a clinic.
     */
    private boolean isMedicalWaste(WasteMovement movement) {
        String code = movement.getWasteCode().getCode();
        return code != null && code.startsWith("18");
    }

    private Company requireCompany(UUID tenantId) {
        return companyRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException(TENANT_NOT_FOUND));
    }

    private WasteMovement requireMovement(UUID id, UUID tenantId) {
        return movementRepository.findByIdAndCompany_IdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new NotFoundException(MOVEMENT_NOT_FOUND));
    }

    /**
     * D1.13 — Anexa 3 și avizul unei linii de cântar se tipăresc din operațiune, pe tot transportul.
     * Pe aici ar ieși un formular pe sortiment, cu număr propriu, pentru un singur camion.
     */
    private static void requireOutsideOperation(WasteMovement movement) {
        if (movement.getWeighingOperation() != null) {
            throw new BusinessException(WEIGHING_LINE_DOCUMENT_THROUGH_OPERATION);
        }
    }
}
