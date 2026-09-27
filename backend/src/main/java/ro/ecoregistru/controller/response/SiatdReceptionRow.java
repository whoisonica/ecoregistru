package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.SiatdModule;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * F6a — un rând din Cântar → SIATD: recepția, modulele ei bifate, kilogramele și termenul. La o persoană fizică doar
 * numele, fără CNP.
 */
public record SiatdReceptionRow(UUID operationId,
                                int number,
                                LocalDate date,
                                UUID workPointId,
                                String workPointName,
                                String partnerName,
                                boolean naturalPerson,
                                Set<SiatdModule> modules,
                                BigDecimal netKg,
                                LocalDate due,
                                LocalDate reminder,
                                Instant confirmedAt,
                                String confirmedByName,
                                String code) {
}
