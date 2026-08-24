package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.PackagingMaterial;

import java.math.BigDecimal;

/** One material row of tabelul 1; nulls are rubrics nobody has answered yet. */
public record PackagingMarketResponse(
        PackagingMaterial material,
        int year,
        BigDecimal salesPackaging,
        BigDecimal primaryTotal,
        BigDecimal primaryReusable,
        BigDecimal secondaryTotal,
        BigDecimal secondaryReusable,
        BigDecimal hazardousContent
) {}
