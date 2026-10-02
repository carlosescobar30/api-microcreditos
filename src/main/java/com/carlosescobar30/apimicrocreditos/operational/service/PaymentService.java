package com.carlosescobar30.apimicrocreditos.operational.service;

import com.carlosescobar30.apimicrocreditos.common.exception.bad_request.ActionNotPermitted;
import com.carlosescobar30.apimicrocreditos.common.exception.conflict.PaymentAlreadyProcessedException;
import com.carlosescobar30.apimicrocreditos.common.exception.conflict.TransactionCodeConflictException;
import com.carlosescobar30.apimicrocreditos.common.exception.not_found.ResourceNotFoundException;
import com.carlosescobar30.apimicrocreditos.operational.attribute.RoundingAttributes;
import com.carlosescobar30.apimicrocreditos.operational.domain.Loan;
import com.carlosescobar30.apimicrocreditos.operational.dto.*;
import com.carlosescobar30.apimicrocreditos.operational.factory.PaymentDTOFactory;
import com.carlosescobar30.apimicrocreditos.common.identity.UserDetailsImpl;
import com.carlosescobar30.apimicrocreditos.iam.adapter.UserAdapter;
import com.carlosescobar30.apimicrocreditos.operational.domain.Payment;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.FinancialMethod;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.LoanStatus;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.TransactionStatus;
import com.carlosescobar30.apimicrocreditos.operational.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository repository;
    private final LoanInstallmentService loanInstallmentService;
    private final LoanService loanService;
    private final PaymentAllocationService paymentAllocationService;
    private final PaymentDTOFactory paymentDTOFactory;
    private final UserAdapter userAdapter;

    private static final EnumSet<LoanStatus> STATUSES_ACCEPTING_PAYMENTS = EnumSet.of(LoanStatus.ACTIVE, LoanStatus.IN_ARREARS);


    @Transactional
    public PaymentInfoDTO pay(UserDetailsImpl userDetails, UUID loanReference, PayRequestDTO payRequest){

        if (payRequest.financialMethod() == FinancialMethod.ADJUSTMENT){

            throw new ActionNotPermitted("Adjustments can only be registered by an administrator");

        }

        Loan loan = loanService.getOneEntity(loanReference);
        UUID userReference = userAdapter.userInfo(userDetails.getId()).userReference();
        verifyOwnership(userReference, loan);

        if (!STATUSES_ACCEPTING_PAYMENTS.contains(loan.getStatus())){

            throw new ActionNotPermitted("The loan does not accept payments in status " + loan.getStatus());

        }

        if (repository.existsByTransactionCode(payRequest.transactionCode())){

            throw new TransactionCodeConflictException();

        }

        BigDecimal roundingAmount = payRequest.amount().setScale(
                RoundingAttributes.SCALE_DEFAULT,
                RoundingAttributes.ROUNDING_DOWN
        );
        Payment payment = repository.save(create(loan, payRequest, roundingAmount ));

        return paymentDTOFactory.create(payment);

    }

    @Transactional
    public PaymentInfoDetailsDTO validateTransaction(TransactionValidationDTO transactionValidation){

        TransactionStatus requestedStatus = transactionValidation.status();

        if (requestedStatus == TransactionStatus.PENDING){

            throw new ActionNotPermitted("A payment can only be validated as APPROVED or DECLINED");

        }

        Payment payment = repository.findByTransactionCodeForUpdate(transactionValidation.transactionCode())
                .orElseThrow(() -> new ResourceNotFoundException("The payment does not exist"));

        loanService.getOneEntityForUpdate(payment.getLoan().getId());

        if (!payment.isPending()){

            if (payment.getStatus() == requestedStatus){

                return currentResult(payment);

            }

            throw new PaymentAlreadyProcessedException(payment.getStatus().name());

        }

        if (requestedStatus == TransactionStatus.APPROVED){

            payment.approve();
            List<AllocationDetailsDTO> details = paymentAllocationService.allocatePayment(payment);
            return paymentDTOFactory.createDetails(payment, details);

        }

        payment.decline();
        return paymentDTOFactory.createDetails(payment, Collections.emptyList());

    }

    private PaymentInfoDetailsDTO currentResult(Payment payment){

        List<AllocationDetailsDTO> details = payment.getStatus() == TransactionStatus.APPROVED
                ? paymentAllocationService.getAllByPayment(payment)
                : Collections.emptyList();

        return paymentDTOFactory.createDetails(payment, details);

    }

    private void verifyOwnership(UUID userReference, Loan loan){

        if (!loan.getUserReference().equals(userReference)){

            throw new ResourceNotFoundException("The loan does not exist");

        }

    }

    private Payment create(Loan loan, PayRequestDTO payRequest, BigDecimal roundingAmount){

        return Payment.builder()
                .loan(loan)
                .transactionCode(payRequest.transactionCode())
                .amount(roundingAmount)
                .financialMethod(payRequest.financialMethod())
                .description(payRequest.description())
                .status(TransactionStatus.PENDING)
                .applied(false)
                .build();


    }

}
