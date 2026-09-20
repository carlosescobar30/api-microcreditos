package com.carlosescobar30.apimicrocreditos.operational.service;

import com.carlosescobar30.apimicrocreditos.operational.attribute.RoundingAttributes;
import com.carlosescobar30.apimicrocreditos.operational.domain.Loan;
import com.carlosescobar30.apimicrocreditos.operational.domain.LoanInstallment;
import com.carlosescobar30.apimicrocreditos.operational.domain.LoanProduct;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.ObligationStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class LoanInstallmentEngineService {

    public BigDecimal calculatePrincipalAmount (LoanProduct loanProduct) {

        BigDecimal principal = loanProduct.getTotalPrincipal();
        int installments = loanProduct.getInstallments();

        return principal.divide(BigDecimal.valueOf(installments),
                RoundingAttributes.SCALE_DEFAULT,
                RoundingAttributes.ROUNDING_DOWN);
    }

    public LoanInstallment createLoanInstallment (Loan loan, BigDecimal principalAmount,
                                                   BigDecimal interestAmount, int installmentNumber){

        LocalDate dueDate = loan.getStartDate().plusMonths(installmentNumber);

        return LoanInstallment.builder()
                .loan(loan)
                .installmentNumber(installmentNumber)
                .principalAmount(principalAmount)
                .interestAmount(interestAmount)
                .paidArrearsAmount(BigDecimal.ZERO)
                .accruedArrearsAmount(BigDecimal.ZERO)
                .totalAmount(principalAmount.add(interestAmount))
                .paymentDate(dueDate)
                .status(ObligationStatus.UNPAID)
                .build();

    }

}
