package com.carlosescobar30.apimicrocreditos.operational.dto;

import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.FinancialMethod;

import java.math.BigDecimal;

public record PayRequestDTO(

        String transactionCode,
        FinancialMethod financialMethod,
        BigDecimal amount,
        String description

) {
}
