package com.carlosescobar30.apimicrocreditos.operational.dto;

import lombok.Builder;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Builder
public record LoanInfoDTO(

        UUID loanReference,
        UUID loanProductReference,
        UUID userReference,
        BigDecimal principalReceivable,
        Integer totalInstallments,
        Long overdueInstallments,
        Integer payday,
        LocalDate startDate,
        LocalDate endDate

) {
}
