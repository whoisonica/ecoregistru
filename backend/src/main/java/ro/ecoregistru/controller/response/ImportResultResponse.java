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
        List<RowError> errors
) {
    /** {@code row} e numărul rândului din Excel, cum îl vede omul (antetul e rândul 1). */
    public record RowError(String sheet, int row, String message) {}
}
