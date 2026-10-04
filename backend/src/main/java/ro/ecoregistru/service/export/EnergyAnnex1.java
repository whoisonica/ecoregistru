package ro.ecoregistru.service.export;

import ro.ecoregistru.controller.response.EnergySheetResponse;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.enums.EnergyCarrier;

import java.math.BigDecimal;
import java.util.Map;

/**
 * What Anexa 1 (consum sub 1000 tep) prints for one year.
 *
 * @param quantities annual quantity per carrier: a carrier missing from the map is not used (printed 0); a carrier
 *                   mapped to {@code null} is used but some months are missing (printed blank)
 * @param coalTep    the hand-written tep of coal for the year; {@code null} when months are missing
 * @param otherTep   the hand-written tep of "alți combustibili"; {@code null} when months are missing
 */
public record EnergyAnnex1(
        int year,
        Company company,
        Map<EnergyCarrier, BigDecimal> quantities,
        BigDecimal coalTep,
        BigDecimal otherTep,
        EnergySheetResponse.Declaration declaration) {
}
