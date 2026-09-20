package com.carlosescobar30.apimicrocreditos.operational.dto;

import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.LoanStatus;
import com.carlosescobar30.apimicrocreditos.operational.enums.LoanAvailability;
import lombok.Builder;

@Builder
public record LoanAvailabilityDTO(

        LoanAvailability loanAvailability,
        LoanStatus loanStatus,
        AvailableLoanProductDTO availableLoanProductsDTO

) {
}
