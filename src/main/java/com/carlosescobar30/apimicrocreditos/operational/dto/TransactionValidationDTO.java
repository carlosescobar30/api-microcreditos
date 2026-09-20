package com.carlosescobar30.apimicrocreditos.operational.dto;

import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.TransactionStatus;

public record TransactionValidationDTO(

        String transactionCode,
        TransactionStatus status

) {
}
