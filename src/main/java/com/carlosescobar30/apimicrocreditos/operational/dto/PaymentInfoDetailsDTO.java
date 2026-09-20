package com.carlosescobar30.apimicrocreditos.operational.dto;

import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.FinancialMethod;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.TransactionStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record PaymentInfoDetailsDTO(

        UUID paymentReference,
        UUID loanReference,
        BigDecimal amount,
        FinancialMethod method,
        TransactionStatus status,
        List<AllocationDetailsDTO> details,
        Instant createdAt

) {
}
