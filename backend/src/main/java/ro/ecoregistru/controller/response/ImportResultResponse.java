package ro.ecoregistru.controller.response;

import java.util.List;

/**
 * P2.15 — ce a găsit un import. {@code saved} e adevărat numai la „Importă" pe un fişier fără nicio
 * eroare; la „Verifică", sau cu măcar o eroare, cifrele spun ce <i>s-ar</i> fi salvat.
 */
public record ImportResultResponse(
        boolean saved,
        int partnersNew,
        int partnersExisting,
        int movementsNew,
        int movementsExisting,
        int workPointsNew,
        int workPointsExisting,
        List<RowError> errors,
        /**
         * Rânduri sărite, nu greșite: o mișcare care pare deja în firmă (aceeași dată, punct de lucru, cod,
         * cantitate, operațiune, partener și document). Nu opresc salvarea; spun ce n-a intrat și de ce.
         */
        List<RowError> warnings
) {
    /** {@code row} e numărul rândului din Excel, cum îl vede omul (antetul e rândul 1). */
    public record RowError(String sheet, int row, String message) {}
}
