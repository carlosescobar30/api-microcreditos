package com.carlosescobar30.apimicrocreditos.operational.dto;

import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.PaymentApplication;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record AllocationDetailsDTO(

        UUID loanInstallmentReference,
        BigDecimal amount,
        PaymentApplication application,
        Instant createdAt



) {
}
