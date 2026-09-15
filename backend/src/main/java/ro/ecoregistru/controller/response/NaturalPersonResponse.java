package ro.ecoregistru.controller.response;

import java.util.UUID;

/**
 * Fișa întreagă a unei persoane fizice, pentru formular (D1.7b). O primește doar cine poate scrie;
 * lista întoarce {@link NaturalPersonSummary}, cu CNP-ul mascat.
 *
 * @param hasOperations persoana apare pe cel puțin o operațiune, deci nu se mai șterge definitiv
 */
public record NaturalPersonResponse(
        UUID id,
        String name,
        String cnp,
        String identification,
        String address,
        boolean active,
        boolean hasOperations) {
}
