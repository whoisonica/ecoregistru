package ro.ecoregistru.controller.response;

import ro.ecoregistru.entity.KeeperDeclaration;
import ro.ecoregistru.enums.CountMethod;
import ro.ecoregistru.enums.InventoryKind;
import ro.ecoregistru.enums.InventoryStatus;
import ro.ecoregistru.enums.ShortageNature;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * D3.5 — inventarul cu tot ce trebuie ecranului. {@code warnings}: KEEPER_IN_COMMISSION, THIRD_PARTY_GOODS,
 * PV_DEADLINE, BOOK_CHANGED (cu rândurile atinse în {@code changedLineIds}).
 */
public record InventoryResponse(UUID id, UUID workPointId, String workPointName, int number, String decisionNumber,
                                LocalDate decisionDate, InventoryKind kind, boolean countsAsAnnual, String mode,
                                String method, LocalDate startsOn, LocalDate endsOn, List<Member> commission,
                                String keeperName, String receivingKeeperName, String keeperRepresentative,
                                List<KeeperDeclaration.Answer> declaration, LocalDate declarationDate,
                                String lastEntryDoc, String lastExitDoc, LocalDate pvDate, String pvCauses,
                                String pvMeasures, String pvSlowStock, String pvStorageFindings, String pvOther,
                                String keeperObjections, String commissionConclusions, InventoryStatus status,
                                LocalDate closedOn, LocalDate approvedOn, String cancelReason, List<Line> lines,
                                BigDecimal surplusKg, BigDecimal shortageKg, List<String> warnings,
                                List<UUID> changedLineIds) {

    public record Member(String name, String role, boolean president) {
    }

    public record Line(UUID id, UUID articleId, String articleName, UUID wasteCodeId, String wasteCode, String wasteName,
                       boolean hazardous, BigDecimal bookKg, BigDecimal countedKg, BigDecimal differenceKg,
                       CountMethod countMethod, String technicalData, String explanation, ShortageNature shortageNature,
                       String responsiblePerson, boolean slowMoving, boolean addedManually) {
    }

    /** O operațiune a depozitului datată în perioada inventarului („primit/eliberat în timpul inventarierii”, pct. 9). */
    public record DuringOperation(UUID id, String type, int number, LocalDate date, String counterparty) {
    }
}
