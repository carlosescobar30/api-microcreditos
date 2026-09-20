package com.carlosescobar30.apimicrocreditos.operational.dto;

import java.math.BigDecimal;

public record LoanProductSpecificationDTO(
        BigDecimal minPrincipal,
        BigDecimal maxPrincipal,
        Integer minInstallments,
        Integer maxInstallments
) {
}
