package ro.ecoregistru.controller.request;

import ro.ecoregistru.enums.InventoryKind;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * D3.5 — decizia de inventariere (Normele OMFP 2861/2009 pct. 6 alin. (1)): comisia cu președintele ei, modul de
 * efectuare și metoda, gestiunea (depozitul), datele de început și de sfârșit. Data de început e data de referință.
 */
public record InventoryHeaderRequest(UUID workPointId, String decisionNumber, LocalDate decisionDate, InventoryKind kind,
                                     boolean countsAsAnnual, String mode, String method, LocalDate startsOn,
                                     LocalDate endsOn, List<Member> commission, String keeperName,
                                     String receivingKeeperName, String keeperRepresentative) {

    public record Member(String name, String role, boolean president) {
    }
}
