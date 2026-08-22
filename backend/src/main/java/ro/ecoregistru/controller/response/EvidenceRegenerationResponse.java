package ro.ecoregistru.controller.response;

import java.util.List;

/**
 * Result of regenerating a tenant's monthly evidence.
 *
 * @param year           the year that was asked for
 * @param linesGenerated lines written, this year and the cascaded ones together
 * @param cascadedYears  later years rebuilt because stock carries across years
 */
public record EvidenceRegenerationResponse(int year, int linesGenerated, List<Integer> cascadedYears) {}
