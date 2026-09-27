package ro.ecoregistru.controller.response;

/**
 * F6a — cifrele benzii de pe Operațiuni. {@code pending} le cuprinde și pe cele care expiră azi sau mâine; ratatele sunt
 * separat. {@code anyModule} fals = firma n-a bifat niciun modul, iar banda nu apare.
 */
public record SiatdSummary(boolean anyModule, int pending, int dueToday, int dueTomorrow, int missed) {
}
