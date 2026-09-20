package com.carlosescobar30.apimicrocreditos.operational.factory;

import com.carlosescobar30.apimicrocreditos.operational.domain.Loan;
import com.carlosescobar30.apimicrocreditos.operational.domain.LoanProduct;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.LoanStatus;
import com.carlosescobar30.apimicrocreditos.operational.dto.*;
import com.carlosescobar30.apimicrocreditos.operational.enums.LoanAvailability;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class LoanDTOFactory {

    public LoanAndInstallmentsInfoDTO buildAccepted(Loan loan, Page<InstallmentsInfoDTO> installmentsInfoDTOS,
                                                    long overdueInstallments){

        LoanProduct loanProduct = loan.getLoanProduct();

        return LoanAndInstallmentsInfoDTO.builder()
                .loanReference(loan.getPublicId())
                .loanProductReference(loanProduct.getPublicId())
                .userReference(loan.getUserReference())
                .principalReceivable(loan.getPrincipalReceivable())
                .totalInstallments(loanProduct.getInstallments())
                .overdueInstallments((int) overdueInstallments)
                .payday(loan.getPayday())
                .startDate(loan.getStartDate())
                .endDate(loan.getEndDate())
                .installment(installmentsInfoDTOS)
                .build();

    }

    public LoanAvailabilityDTO buildRejected (LoanAvailability loanAvailability){

        return LoanAvailabilityDTO.builder()
                .loanAvailability(loanAvailability)
                .loanStatus(LoanStatus.REJECTED)
                .availableLoanProductsDTO(null)
                .build();


    }

    public LoanAvailabilityDTO buildPreApproved(AvailableLoanProductDTO productDTO) {

        return LoanAvailabilityDTO.builder()
                .loanAvailability(LoanAvailability.AVAILABLE)
                .loanStatus(LoanStatus.PRE_APPROVED)
                .availableLoanProductsDTO(productDTO)
                .build();

    }

    public LoanInfoDTO buildLoanInfo(Loan loan, long overdueInstallments){

        LoanProduct loanProduct = loan.getLoanProduct();

        return LoanInfoDTO.builder()
                .loanReference(loan.getPublicId())
                .loanProductReference(loanProduct.getPublicId())
                .userReference(loan.getUserReference())
                .principalReceivable(loan.getPrincipalReceivable())
                .totalInstallments(loanProduct.getInstallments())
                .overdueInstallments(overdueInstallments)
                .payday(loan.getPayday())
                .startDate(loan.getStartDate())
                .endDate(loan.getEndDate())
                .build();

    }

}
