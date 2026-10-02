package com.carlosescobar30.apimicrocreditos.operational.dto;

import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.CreditModality;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record LoanProductRequestDTO(

        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        @Positive
        @Digits(integer = 15, fraction = 4)
        BigDecimal totalPrincipal,

        @NotNull
        @DecimalMin(value = "0", inclusive = false)
        @Digits(integer = 1, fraction = 4)
        BigDecimal interestRate,

        @NotNull
        @DecimalMin(value = "0", inclusive = false)
        @Digits(integer = 1, fraction = 4)
        BigDecimal penaltyRateEa,

        @NotNull
        CreditModality creditModality,

        @NotNull
        @Min(1)
        @Max(120)
        Integer installments,

        @NotNull
        @PositiveOrZero
        Integer minimumUserScore

) {
}
