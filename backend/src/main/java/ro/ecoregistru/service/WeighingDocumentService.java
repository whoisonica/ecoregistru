package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Driver;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WeighingOperation;
import ro.ecoregistru.entity.WorkPoint;
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
    ro.ecoregistru.service.export.BorderouGenerator borderouGenerator;
    DepotAccess depotAccess;

    /** Alocă numărul la prima tipărire și îl păstrează: retipărirea e același document. */
    @Transactional
    public byte[] renderAnexa3(UUID id) {
        UUID tenantId = TenantContext.require();
        WeighingOperation operation = requireHandover(id, tenantId);
        Company company = requireCompany(tenantId);
        List<WasteMovement> lines = departureLines(id).stream()
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
        List<WasteMovement> lines = departureLines(id);
        if (lines.isEmpty()) {
            throw new BusinessException(WEIGHING_OPERATION_NO_LINES);
        }
        return avizGenerator.render(head(operation, company), lines, company);
    }

    /**
     * D1.11 — borderoul de achiziție al unei intrări de la o persoană fizică, după finalizare (reținerile
     * de pe el se calculează atunci). Numărul se dă la prima tipărire și se păstrează: regim intern de
     * numerotare (OUG 31/2011 art. 1 alin. (1^3)). Obligatoriu la metal, la cerere la rest (C3).
     *
     * <p>Poartă prețuri și, la metal, CNP-ul întreg: îl tipărește doar cine scrie (controllerul) <b>și</b>
     * vede prețurile (D1.8).
     */
    @Transactional
    public byte[] renderBorderou(UUID id) {
        UUID tenantId = TenantContext.require();
        WeighingOperation operation = requireOperation(id, tenantId);
        Company company = requireCompany(tenantId);
        requirePricesVisible(company, "Borderoul arată prețurile depozitului.");
        if (operation.getType() != WeighingOperationType.IN || operation.getNaturalPerson() == null) {
            throw new BusinessException(WEIGHING_BORDEROU_REQUIRES_PERSON);
        }
        if (operation.getStatus() != WeighingOperationStatus.FINALIZED) {
            throw new BusinessException(WEIGHING_BORDEROU_REQUIRES_FINALIZED);
        }
        if (operation.getBorderouNumber() == null) {
            operationRepository.lockNumbering(tenantId + ":borderou");
            Integer max = operationRepository.findMaxBorderouNumber(tenantId);
            operation.setBorderouNumber(max == null ? 1 : max + 1);
            operationRepository.saveAndFlush(operation);
        }
        return borderouGenerator.render(operation,
                movementRepository.findAllByWeighingOperation_IdOrderByLineNoAsc(id), company);
    }

    /** Plafonul zilnic de plăți în numerar către o persoană fizică (Legea 70/2015 art. 4 alin. (1)). */
    public static final java.math.BigDecimal CASH_DAILY_LIMIT = new java.math.BigDecimal("10000");

    /**
     * D1.11 — cât s-a plătit în numerar persoanei acestei operațiuni în aceeași zi, cu ea inclusă, și dacă
     * trece de 10.000 lei. Un avertisment pe ecran, nu un refuz și nimic pe hârtie: plata s-a făcut deja
     * sau se face la ghișeu, iar aplicația spune doar ce vede. Suma e ce se dă omului, după rețineri.
     * Cine nu vede prețurile află doar dacă s-a trecut plafonul, fără sumă.
     */
    @Transactional(readOnly = true)
    public CashCheck cashCheck(UUID id) {
        UUID tenantId = TenantContext.require();
        WeighingOperation operation = requireOperation(id, tenantId);
        if (operation.getNaturalPerson() == null) {
            return new CashCheck(false, null, CASH_DAILY_LIMIT);
        }
        java.math.BigDecimal paid = java.math.BigDecimal.ZERO;
        for (WeighingOperation o : operationRepository.findCashToPersonOnDay(tenantId,
                operation.getNaturalPerson().getId(), operation.getDate())) {
            List<WasteMovement> lines = movementRepository.findAllByWeighingOperation_IdOrderByLineNoAsc(o.getId());
            DepotRetentions.Amounts retained = o.getAfmContribution() != null && o.getIncomeTax() != null
                    ? new DepotRetentions.Amounts(o.getAfmBase(), o.getAfmContribution(), o.getIncomeTaxBase(), o.getIncomeTax())
                    : DepotRetentions.of(o, lines);
            java.math.BigDecimal value = lines.stream().map(WasteMovement::getTotalValue)
                    .filter(java.util.Objects::nonNull).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            paid = paid.add(value)
                    .subtract(retained.afm()).subtract(retained.incomeTax());
        }
        boolean visible = operation.getCompany().getPriceVisibility()
                .visibleTo(ro.ecoregistru.security.SecurityUtils.currentUser().getRole());
        return new CashCheck(paid.compareTo(CASH_DAILY_LIMIT) > 0, visible ? paid : null, CASH_DAILY_LIMIT);
    }

    public record CashCheck(boolean aboveLimit, java.math.BigDecimal paidToday, java.math.BigDecimal limit) {
    }

    private static void requirePricesVisible(Company company, String why) {
        if (!company.getPriceVisibility().visibleTo(ro.ecoregistru.security.SecurityUtils.currentUser().getRole())) {
            throw new org.springframework.security.access.AccessDeniedException(why);
        }
    }

    /** D2.4 — documentele unei operațiuni din alt depozit decât ale utilizatorului: 404, ca operațiunea. */
    private WeighingOperation requireOperation(UUID id, UUID tenantId) {
        WeighingOperation operation = operationRepository.findByIdAndCompany_Id(id, tenantId)
                .orElseThrow(() -> new NotFoundException(WEIGHING_OPERATION_NOT_FOUND));
        if (!WeighingOperationService.visible(operation, depotAccess.allowed())) {
            throw new NotFoundException(WEIGHING_OPERATION_NOT_FOUND);
        }
        return operation;
    }

    private WeighingOperation requireHandover(UUID id, UUID tenantId) {
        WeighingOperation operation = requireOperation(id, tenantId);
        // D2.5 — și transferul între depozite are aviz și Anexa 3 (HG 1061/2008 art. 20 alin. (4), fără scutire internă).
        boolean transfer = operation.getType() == WeighingOperationType.TRANSFER;
        if (!transfer && (operation.getType() != WeighingOperationType.OUT || operation.getPartner() == null)) {
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

    /** Ce a plecat: la transfer, fără liniile de recepție ale lui B (D2.5). */
    private List<WasteMovement> departureLines(UUID id) {
        return movementRepository.findAllByWeighingOperation_IdOrderByLineNoAsc(id).stream()
                .filter(l -> l.getOperation() != ro.ecoregistru.enums.WasteOperation.TRANSFERRED_IN)
                .toList();
    }

    /**
     * D2.5 — la transfer destinatarul e chiar firma, la depozitul B: aceeași denumire și CUI, adresa și autorizația
     * depozitului (altfel ale firmei). Un partener nesalvat, doar pentru hârtie.
     */
    private static Partner internalRecipient(WeighingOperation o, Company company) {
        WorkPoint target = o.getTargetWorkPoint();
        boolean own = target.getEnvironmentalAuthNumber() != null;
        return Partner.builder()
                .company(company)
                .name(company.getName())
                .cui(company.getCui())
                .tradeRegisterNumber(company.getTradeRegisterNumber())
                .address(company.getAddress())
                .authorizationNumber(own ? target.getEnvironmentalAuthNumber() : company.getEnvironmentalAuthNumber())
                .authorizationExpiry(own ? target.getEnvironmentalAuthExpiry() : company.getEnvironmentalAuthExpiry())
                .build();
    }

    /**
     * Pe Anexa 3 cantitatea e a expeditorului (art. 20 alin. (2)); B își notează greutatea la „Observații”
     * (`surse-oficiale.md` §4, runda 2), cu NIR-ul când diferența a trecut de toleranță.
     */
    private static String transferObservations(WeighingOperation o, List<WasteMovement> received) {
        String order = o.getOrderNumber();
        if (o.getReceivedOn() == null) {
            return order;
        }
        java.math.BigDecimal kg = received.stream().map(WasteMovement::getQuantity)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        String note = "Recepționat la " + o.getTargetWorkPoint().getName() + " pe "
                + o.getReceivedOn().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) + ": "
                + kg.stripTrailingZeros().toPlainString() + " kg"
                + (o.getNirNumber() == null ? "" : o.getNirNumber().toUpperCase().startsWith("NIR")
                        ? " (" + o.getNirNumber() + ")" : " (NIR " + o.getNirNumber() + ")");
        return order == null ? note : order + ". " + note;
    }

    /**
     * Capul transportului, ca mișcare nesalvată: generatoarele citesc de pe ea expeditorul, destinatarul,
     * șoferul, mașina, data și numărul. Nu se persistă și nu intră în nicio listă.
     */
    private WasteMovement head(WeighingOperation o, Company company) {
        Driver driver = o.getDriver();
        boolean transfer = o.getType() == WeighingOperationType.TRANSFER;
        WorkPoint target = o.getTargetWorkPoint();
        return WasteMovement.builder()
                .company(company)
                .workPoint(o.getWorkPoint())
                .partner(transfer ? internalRecipient(o, company) : o.getPartner())
                .partnerWorkPoint(!transfer ? null : ro.ecoregistru.entity.PartnerWorkPoint.builder()
                        .name(target.getName())
                        .address(target.getAddress() != null ? target.getAddress() : company.getAddress())
                        .build())
                .date(o.getDate())
                // D2.5 — la transfer, data descărcării e cea a recepției la B.
                .unloadDate(transfer ? o.getReceivedOn() : null)
                .unit(Unit.KG)
                .driverName(o.getDriverName())
                .driverIdentification(driver == null ? null : driver.getIdentification())
                .driverCnp(driver == null ? null : driver.getCnp())
                .vehicleRegistration(o.getVehicleRegistration())
                .notes(o.getNotes())
                // „Nr. comandă / aviz” din formular: numărul avizului și, pe Anexa 3, rubrica „Observaţii”,
                // exact ce face referința documentului pe o mișcare.
                .documentReference(transfer ? transferObservations(o, movementRepository
                        .findAllByWeighingOperation_IdOrderByLineNoAsc(o.getId()).stream()
                        .filter(l -> l.getOperation() == ro.ecoregistru.enums.WasteOperation.TRANSFERRED_IN).toList())
                        : o.getOrderNumber())
                .anexa3Series(o.getAnexa3Series())
                .anexa3Number(o.getAnexa3Number())
                .build();
    }
}
