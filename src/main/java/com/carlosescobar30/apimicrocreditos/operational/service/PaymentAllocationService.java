package com.carlosescobar30.apimicrocreditos.operational.service;

import com.carlosescobar30.apimicrocreditos.common.exception.not_found.ResourceNotFoundException;
import com.carlosescobar30.apimicrocreditos.operational.attribute.RoundingAttributes;
import com.carlosescobar30.apimicrocreditos.operational.domain.Loan;
import com.carlosescobar30.apimicrocreditos.operational.domain.LoanInstallment;
import com.carlosescobar30.apimicrocreditos.operational.domain.Payment;
import com.carlosescobar30.apimicrocreditos.operational.domain.PaymentAllocation;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.ObligationStatus;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.PaymentApplication;
import com.carlosescobar30.apimicrocreditos.operational.dto.AllocationDetailsDTO;
import com.carlosescobar30.apimicrocreditos.operational.dto.AllocationAmountsDTO;
import com.carlosescobar30.apimicrocreditos.operational.factory.PaymentAllocationDTOFactory;
import com.carlosescobar30.apimicrocreditos.operational.repository.PaymentAllocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentAllocationService {

    private final PaymentAllocationRepository repository;
    private final LoanInstallmentService loanInstallmentService;
    private final LoanService loanService;
    private final PaymentAllocationDTOFactory factory;

    @Transactional
    public List<AllocationDetailsDTO>  allocatePayment (Payment payment){

        Loan loan = payment.getLoan();
        BigDecimal amount = payment.getAmount().setScale(
                RoundingAttributes.SCALE_DEFAULT,
                RoundingAttributes.ROUNDING_DOWN);

        List<LoanInstallment> installments = loanInstallmentService.getAllDue(loan);

        if (installments.isEmpty()){

            throw new ResourceNotFoundException("The installments to be paid were not found");

        }
        List<AllocationDetailsDTO> details = new ArrayList<>();
        int index = 0;

        while (amount.compareTo(BigDecimal.ZERO) > 0){

            LoanInstallment installment = installments.get(index);

            int arrearCompare = installment
                    .getAccruedArrearsAmount()
                    .subtract(installment.getPaidArrearsAmount())
                    .compareTo(BigDecimal.ZERO);
            if (installment.getStatus().equals(ObligationStatus.OVERDUE) && arrearCompare > 0) {
                AllocationAmountsDTO arrearAmounts = allocateArrear(amount, installment);
                amount = arrearAmounts.surplus();
                PaymentAllocation allocationArrear = repository.save(create(
                        payment,
                        installment,
                        arrearAmounts.amountPaid(),
                        PaymentApplication.ARREAR
                ));
                details.add(factory.createAllocationDetailsDTO(allocationArrear));
            }

            int interestCompare = installment.getInterestAmount().compareTo(BigDecimal.ZERO);
            if (amount.compareTo(BigDecimal.ZERO) > 0 && interestCompare > 0){

                AllocationAmountsDTO interestAmounts = allocateInterest(amount, installment);
                amount = interestAmounts.surplus();
                PaymentAllocation allocationInterest= repository.save(create(
                        payment,
                        installment,
                        interestAmounts.amountPaid(),
                        PaymentApplication.INTEREST
                ));
                details.add(factory.createAllocationDetailsDTO(allocationInterest));


            }

            if (amount.compareTo(BigDecimal.ZERO) > 0){

                AllocationAmountsDTO principalAmounts = allocatePrincipal(amount, installment);
                amount = principalAmounts.surplus();
                PaymentAllocation allocationPrincipal = repository.save(create(
                        payment,
                        installment,
                        principalAmounts.amountPaid(),
                        PaymentApplication.PRINCIPAL
                ));

                details.add(factory.createAllocationDetailsDTO(allocationPrincipal));

            }



            if (index + 1 >= installments.size() && amount.compareTo(BigDecimal.ZERO) > 0){

                PaymentAllocation allocationPrincipal = repository.save(create(
                        payment,
                        installment,
                        amount,
                        PaymentApplication.SURPLUS
                ));
                details.add(factory.createAllocationDetailsDTO(allocationPrincipal));
                amount = BigDecimal.ZERO;

                break;

            }

            index++;

        }

        return details;

    }

    private PaymentAllocation create(Payment payment,
                                     LoanInstallment installment,
                                     BigDecimal amount,
                                     PaymentApplication application){

        return PaymentAllocation.builder()
                .payment(payment)
                .loanInstallment(installment)
                .amount(amount)
                .appliedTo(application)
                .build();

    }

    private AllocationAmountsDTO allocateArrear (BigDecimal amount, LoanInstallment installment){

        BigDecimal arrear = installment
                .getAccruedArrearsAmount()
                .subtract(installment.getPaidArrearsAmount());
        BigDecimal total = installment.getTotalAmount();
        int compareArrear = amount.compareTo(arrear);

        if (compareArrear <= 0){

            installment.setPaidArrearsAmount(installment.getPaidArrearsAmount().add(amount));
            installment.setTotalAmount(total.subtract(amount));
            return factory.createAllocationAmounts(amount, BigDecimal.ZERO);

        }

        installment.setPaidArrearsAmount(installment.getAccruedArrearsAmount());
        installment.setTotalAmount(installment.getTotalAmount().subtract(arrear));
        amount = amount.subtract(arrear);


        return factory.createAllocationAmounts(arrear, amount);

    }

    private AllocationAmountsDTO allocateInterest (BigDecimal amount, LoanInstallment installment){

        BigDecimal interest = installment.getInterestAmount();
        BigDecimal total = installment.getTotalAmount();
        int compareArrear = amount.compareTo(interest);

        if (compareArrear <= 0){

            total = total.subtract(amount);
            installment.setTotalAmount(total);

            interest = interest.subtract(amount);
            installment.setInterestAmount(interest);
            return factory.createAllocationAmounts(amount, BigDecimal.ZERO);

        }

        BigDecimal newTotalAmount = total.subtract(interest);
        installment.setTotalAmount(newTotalAmount);
        installment.setInterestAmount(BigDecimal.ZERO);
        amount = amount.subtract(interest);

        return factory.createAllocationAmounts(interest, amount);

    }

    private AllocationAmountsDTO allocatePrincipal (BigDecimal amount, LoanInstallment installment){


        BigDecimal principal = installment.getPrincipalAmount();
        BigDecimal total = installment.getTotalAmount();
        int compareArrear = amount.compareTo(principal);

        if (compareArrear < 0){


            total = total.subtract(amount);
            installment.setTotalAmount(total);
            principal = principal.subtract(amount);
            installment.setPrincipalAmount(principal);

            loanService.deductSettledAmount(installment.getLoan(), amount);
            return factory.createAllocationAmounts(amount, BigDecimal.ZERO);

        }



        installment.setPrincipalAmount(BigDecimal.ZERO);
        installment.setTotalAmount(BigDecimal.ZERO);
        installment.setStatus(ObligationStatus.PAID);
        amount = amount.subtract(principal);

        loanService.deductSettledAmount(installment.getLoan(),principal);

        return factory.createAllocationAmounts(principal,amount);

    }

    public List<AllocationDetailsDTO> getAllByPayment (Payment payment){

        return repository.findAllByPayment(payment);

    }

}
