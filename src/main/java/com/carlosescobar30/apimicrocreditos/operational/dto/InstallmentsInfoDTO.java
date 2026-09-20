package com.carlosescobar30.apimicrocreditos.operational.dto;


import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Builder
public record InstallmentsInfoDTO(

        UUID installmentReference,
        BigDecimal principalAmount,
        BigDecimal interestAmount,
        BigDecimal totalAmount,
        LocalDate paymentDate


) {
}
