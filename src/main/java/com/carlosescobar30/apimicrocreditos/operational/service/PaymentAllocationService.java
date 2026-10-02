package com.carlosescobar30.apimicrocreditos.operational.service;

import com.carlosescobar30.apimicrocreditos.common.exception.not_found.ResourceNotFoundException;
import com.carlosescobar30.apimicrocreditos.operational.attribute.RoundingAttributes;
import com.carlosescobar30.apimicrocreditos.operational.domain.Loan;
import com.carlosescobar30.apimicrocreditos.operational.domain.LoanInstallment;
import com.carlosescobar30.apimicrocreditos.operational.domain.Payment;
import com.carlosescobar30.apimicrocreditos.operational.domain.PaymentAllocation;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.ObligationStatus;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.PaymentApplication;
import com.carlosescobar30.apimicrocreditos.operational.domain.rate.PenaltyRateSchedule;
import com.carlosescobar30.apimicrocreditos.operational.dto.AllocationDetailsDTO;
import com.carlosescobar30.apimicrocreditos.operational.dto.AllocationAmountsDTO;
import com.carlosescobar30.apimicrocreditos.operational.factory.PaymentAllocationDTOFactory;
import com.carlosescobar30.apimicrocreditos.operational.repository.PaymentAllocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentAllocationService {

    private final PaymentAllocationRepository repository;
    private final LoanInstallmentService loanInstallmentService;
    private final LoanService loanService;
    private final PaymentAllocationDTOFactory factory;
    private final UsuryRateService usuryRateService;
    private final Clock clock;

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

        LocalDate today = LocalDate.now(clock);
        PenaltyRateSchedule penaltyRates = usuryRateService.penaltyScheduleFor(loan.getLoanProduct());
        installments.forEach(installment -> installment.accrueArrears(today, penaltyRates));

        List<AllocationDetailsDTO> details = new ArrayList<>();
        int index = 0;

        while (amount.compareTo(BigDecimal.ZERO) > 0){

            LoanInstallment installment = installments.get(index);

            int arrearCompare = installment.outstandingArrears().compareTo(BigDecimal.ZERO);
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

        BigDecimal paid = amount.min(installment.outstandingArrears());
        installment.setPaidArrearsAmount(installment.getPaidArrearsAmount().add(paid));
        installment.recalculateTotal();

        return factory.createAllocationAmounts(paid, amount.subtract(paid));

    }

    private AllocationAmountsDTO allocateInterest (BigDecimal amount, LoanInstallment installment){

        BigDecimal paid = amount.min(installment.getInterestAmount());
        installment.setInterestAmount(installment.getInterestAmount().subtract(paid));
        installment.recalculateTotal();

        return factory.createAllocationAmounts(paid, amount.subtract(paid));

    }

    private AllocationAmountsDTO allocatePrincipal (BigDecimal amount, LoanInstallment installment){

        BigDecimal paid = amount.min(installment.getPrincipalAmount());
        installment.setPrincipalAmount(installment.getPrincipalAmount().subtract(paid));

        if (installment.getPrincipalAmount().signum() == 0){

            installment.setStatus(ObligationStatus.PAID);

        }

        installment.recalculateTotal();
        loanService.deductSettledAmount(installment.getLoan(), paid);

        return factory.createAllocationAmounts(paid, amount.subtract(paid));

    }

    public List<AllocationDetailsDTO> getAllByPayment (Payment payment){

        return repository.findAllByPayment(payment);

    }

}
