import type { WasteMovement, WasteMovementInput } from "@web/types";

/**
 * Corectura unei predări de pe telefon (M1f). `PUT /movements/{id}` înlocuiește predarea întreagă
 * (`WasteMovementService.update`): o rubrică lipsă din cerere se golește pe server. Formularul de pe
 * telefon are doar o parte din rubrici, deci cererea pornește de la predarea citită de pe server și
 * pune peste ea numai ce e pe ecran — notele, volumul, tratarea, Anexa 2 și restul rămân cum erau.
 */
export function movementToInput(mv: WasteMovement): Omit<WasteMovementInput, "clientGeneratedId"> {
  return {
    workPointId: mv.workPointId,
    date: mv.date,
    wasteCodeId: mv.wasteCodeId,
    quantity: mv.quantity,
    weighedAtUnloading: mv.weighedAtUnloading,
    volumeM3: mv.volumeM3,
    unit: mv.unit,
    operation: mv.operation,
    register: mv.register,
    physicalState: mv.physicalState,
    storageType: mv.storageType,
    treatmentMethod: mv.treatmentMethod,
    transportMeans: mv.transportMeans,
    wasteDestination: mv.wasteDestination,
    operationCode: mv.operationCode,
    partnerId: mv.partnerId,
    internalGeneratorId: mv.internalGeneratorId,
    documentReference: mv.documentReference,
    notes: mv.notes,
    loadDate: mv.loadDate,
    unloadDate: mv.unloadDate,
    partnerWorkPointId: mv.partnerWorkPointId,
    transportPartnerId: mv.transportPartnerId,
    driverName: mv.driverName,
    driverIdentification: mv.driverIdentification,
    driverCnp: mv.driverCnp,
    vehicleRegistration: mv.vehicleRegistration,
    transportDestinations: mv.transportDestinations,
    anexa3Unit: mv.anexa3Unit,
    anexa2Number: mv.anexa2Number,
    anexa2ApprovalNumber: mv.anexa2ApprovalNumber,
    anexa2Packaging: mv.anexa2Packaging,
    anexa2BelowOneTon: mv.anexa2BelowOneTon,
    packagingOnMarket: mv.packagingOnMarket,
    packagingMaterial: mv.packagingMaterial,
    packagingCategory: mv.packagingCategory,
    packagingReusable: mv.packagingReusable,
    packagingHazardousContent: mv.packagingHazardousContent,
    packagingOrigin: mv.packagingOrigin,
  };
}

/** Cererea corecturii: predarea de pe server, cu rubricile de pe ecran deasupra. */
export function editBody(mv: WasteMovement, onScreen: Partial<WasteMovementInput>) {
  return { ...movementToInput(mv), ...onScreen };
}

/**
 * Ce poate corecta telefonul: predările proprii de pe Anexa 1 (valorificare sau eliminare) — singurele pe
 * care le știe formularul lui —, nu rândurile din cântar (se schimbă numai prin operațiune, BUG-018) și
 * numai cine scrie.
 */
export function canEditOnPhone(mv: WasteMovement, writer: boolean) {
  return (
    writer &&
    !mv.weighingOperationId &&
    mv.register === "ANEXA_1" &&
    (mv.operation === "RECOVERED" || mv.operation === "DISPOSED")
  );
}
