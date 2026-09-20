package com.carlosescobar30.apimicrocreditos.operational.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record AllocationAmountsDTO(

        BigDecimal amountPaid,
        BigDecimal surplus

) {
}
