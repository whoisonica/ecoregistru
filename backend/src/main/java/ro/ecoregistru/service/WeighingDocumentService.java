package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Driver;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WeighingOperation;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WeighingOperationStatus;
import ro.ecoregistru.enums.WeighingOperationType;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WeighingOperationRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.export.Anexa3FormGenerator;
import ro.ecoregistru.service.export.AvizGenerator;

import java.util.List;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.*;

/**
 * D1.13 — documentele de transport ale unei operațiuni de depozit: <b>un formular pe camion</b>, cu
 * toate sortimentele, nu unul pe linie (HG 1061/2008 art. 20 alin. (4): „fiecare transport ... însoţit
 * de un formular”).
 *
 * <p><b>Doar la ieșire către un partener.</b> La o intrare, formularul îl completează expeditorul
 * (art. 20 alin. (2)), adică cel care a adus deșeul; la o persoană fizică nu există deloc (AX,
 * {@code surse-oficiale.md} §18.4). O operațiune anulată nu mai scoate documente; una în lucru, da,
 * fiindcă formularul pleacă odată cu camionul, adesea înainte ca biroul să finalizeze bonul.
 *
 * <p><b>Ce nu ține operațiunea se tipărește gol</b>, ca pe carnet: transportatorul (fără unul ales,
 * transportăm noi, ca la mișcare), data descărcării, bifele „Destinat:”, volumul. Numărul avizului e
 * „Nr. comandă / aviz” de pe operațiune, gol dacă nu l-a scris nimeni.
 * Nicio rubrică nu se ghicește.
 *
 * <p><b>Anexa 2 nu e aici.</b> Formularul de deșeuri periculoase are un singur „tip de deșeu” și un număr
 * înscris de agenție pe fiecare expediție; o operațiune cu linii periculoase își tipărește Anexa 3 doar
 * pentru liniile nepericuloase.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WeighingDocumentService {

    WeighingOperationRepository operationRepository;
    WasteMovementRepository movementRepository;
    CompanyRepository companyRepository;
    Anexa3Numbering anexa3Numbering;
    Anexa3FormGenerator anexa3Generator;
    AvizGenerator avizGenerator;

    /** Alocă numărul la prima tipărire și îl păstrează: retipărirea e același document. */
    @Transactional
    public byte[] renderAnexa3(UUID id) {
        UUID tenantId = TenantContext.require();
        WeighingOperation operation = requireHandover(id, tenantId);
        Company company = requireCompany(tenantId);
        List<WasteMovement> lines = movementRepository.findAllByWeighingOperation_IdOrderByLineNoAsc(id).stream()
                .filter(line -> !line.getWasteCode().isHazardous())
                .toList();
        if (lines.isEmpty()) {
            throw new BusinessException(WEIGHING_ANEXA3_ONLY_HAZARDOUS);
        }
        if (operation.getAnexa3Number() == null) {
            operation.setAnexa3Number(anexa3Numbering.next(tenantId));
            operation.setAnexa3Series(company.getAnexa3Series());
            operationRepository.saveAndFlush(operation);
        }
        return anexa3Generator.render(head(operation, company), lines, company);
    }

    /** Avizul însoțește marfa, periculoasă sau nu, deci ia toate liniile. Nu alocă nimic. */
    @Transactional(readOnly = true)
    public byte[] renderAviz(UUID id) {
        UUID tenantId = TenantContext.require();
        WeighingOperation operation = requireHandover(id, tenantId);
        Company company = requireCompany(tenantId);
        List<WasteMovement> lines = movementRepository.findAllByWeighingOperation_IdOrderByLineNoAsc(id);
        if (lines.isEmpty()) {
            throw new BusinessException(WEIGHING_OPERATION_NO_LINES);
        }
        return avizGenerator.render(head(operation, company), lines, company);
    }

    private WeighingOperation requireHandover(UUID id, UUID tenantId) {
        WeighingOperation operation = operationRepository.findByIdAndCompany_Id(id, tenantId)
                .orElseThrow(() -> new NotFoundException(WEIGHING_OPERATION_NOT_FOUND));
        if (operation.getType() != WeighingOperationType.OUT || operation.getPartner() == null) {
            throw new BusinessException(WEIGHING_DOCUMENT_REQUIRES_HANDOVER);
        }
        if (operation.getStatus() == WeighingOperationStatus.CANCELLED) {
            throw new BusinessException(WEIGHING_DOCUMENT_CANCELLED);
        }
        return operation;
    }

    private Company requireCompany(UUID tenantId) {
        return companyRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
    }

    /**
     * Capul transportului, ca mișcare nesalvată: generatoarele citesc de pe ea expeditorul, destinatarul,
     * șoferul, mașina, data și numărul. Nu se persistă și nu intră în nicio listă.
     */
    private static WasteMovement head(WeighingOperation o, Company company) {
        Driver driver = o.getDriver();
        return WasteMovement.builder()
                .company(company)
                .workPoint(o.getWorkPoint())
                .partner(o.getPartner())
                .date(o.getDate())
                .unit(Unit.KG)
                .driverName(o.getDriverName())
                .driverIdentification(driver == null ? null : driver.getIdentification())
                .driverCnp(driver == null ? null : driver.getCnp())
                .vehicleRegistration(o.getVehicleRegistration())
                .notes(o.getNotes())
                // „Nr. comandă / aviz” din formular: numărul avizului și, pe Anexa 3, rubrica „Observaţii”,
                // exact ce face referința documentului pe o mișcare.
                .documentReference(o.getOrderNumber())
                .anexa3Series(o.getAnexa3Series())
                .anexa3Number(o.getAnexa3Number())
                .build();
    }
}
