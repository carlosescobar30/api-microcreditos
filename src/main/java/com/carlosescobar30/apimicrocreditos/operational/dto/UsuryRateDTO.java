package com.carlosescobar30.apimicrocreditos.operational.dto;

import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.CreditModality;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UsuryRateDTO(
        UUID usuryRateReference,
        CreditModality creditModality,
        LocalDate validFrom,
        BigDecimal rateEa,
        String resolution
) {
}
