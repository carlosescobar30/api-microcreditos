package com.carlosescobar30.apimicrocreditos.operational.dto;

import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.FinancialMethod;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PayRequestDTO(

        @NotBlank
        @Size(max = 100)
        String transactionCode,

        @NotNull
        FinancialMethod financialMethod,

        @NotNull
        @Positive
        @Digits(integer = 15, fraction = 4)
        BigDecimal amount,

        @NotBlank
        @Size(max = 255)
        String description

) {
}
