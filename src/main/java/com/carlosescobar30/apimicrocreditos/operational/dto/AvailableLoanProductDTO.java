package com.carlosescobar30.apimicrocreditos.operational.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AvailableLoanProductDTO(
        UUID loanProductReference,
        String name,
        BigDecimal totalPrincipal,
        BigDecimal interestRate,
        BigDecimal dailyPenaltyRate,
        Integer installments
) {
}
