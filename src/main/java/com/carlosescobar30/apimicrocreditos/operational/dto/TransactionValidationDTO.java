package com.carlosescobar30.apimicrocreditos.operational.dto;

import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.TransactionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TransactionValidationDTO(

        @NotBlank
        String transactionCode,

        @NotNull
        TransactionStatus status

) {
}
