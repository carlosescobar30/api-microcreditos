package com.carlosescobar30.apimicrocreditos.operational.dto;

import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.CreditModality;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UsuryRateRequestDTO(

        @NotNull
        CreditModality creditModality,

        @NotNull
        LocalDate validFrom,

        @NotNull
        @DecimalMin(value = "0", inclusive = false)
        @Digits(integer = 1, fraction = 4)
        BigDecimal rateEa,

        @Size(max = 100)
        String resolution

) {
}
