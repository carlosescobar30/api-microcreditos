package com.carlosescobar30.apimicrocreditos.operational.service;

import com.carlosescobar30.apimicrocreditos.common.exception.not_found.ResourceNotFoundException;
import com.carlosescobar30.apimicrocreditos.operational.attribute.RoundingAttributes;
import com.carlosescobar30.apimicrocreditos.operational.domain.Loan;
import com.carlosescobar30.apimicrocreditos.operational.dto.*;
import com.carlosescobar30.apimicrocreditos.operational.factory.PaymentDTOFactory;
import com.carlosescobar30.apimicrocreditos.common.identity.UserDetailsImpl;
import com.carlosescobar30.apimicrocreditos.iam.adapter.UserAdapter;
import com.carlosescobar30.apimicrocreditos.operational.domain.Payment;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.TransactionStatus;
import com.carlosescobar30.apimicrocreditos.operational.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
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


    @Transactional
    public PaymentInfoDTO pay(UserDetailsImpl userDetails, UUID loanReference, PayRequestDTO payRequest){

        Loan loan = loanService.getOneEntity(loanReference);
        verifyOwnership(userDetails, loan);
        BigDecimal roundingAmount = payRequest.amount().setScale(
                RoundingAttributes.SCALE_DEFAULT,
                RoundingAttributes.ROUNDING_DOWN
        );
        Payment payment = repository.save(create(loan, payRequest, roundingAmount ));

        return paymentDTOFactory.create(payment);

    }

    @Transactional
    public PaymentInfoDetailsDTO validateTransaction(UserDetailsImpl userDetails, TransactionValidationDTO transactionValidation){

        Payment payment = repository.findByTransactionCode(transactionValidation.transactionCode())
                .orElseThrow(() -> new ResourceNotFoundException("The payment does not exist"));

        verifyOwnership(userDetails, payment.getLoan());

        switch (transactionValidation.status()){

            case DECLINED ->{



                payment.setStatus(TransactionStatus.DECLINED);
                return paymentDTOFactory.createDetails(payment, Collections.emptyList());

            }

            case APPROVED -> {

                if (payment.getApplied() == true){

                    List<AllocationDetailsDTO> details = paymentAllocationService.getAllByPayment(payment);
                    return paymentDTOFactory.createDetails(payment, details);

                }

                payment.setStatus(TransactionStatus.APPROVED);
                payment.setApplied(true);
                List<AllocationDetailsDTO> details = paymentAllocationService.allocatePayment(payment);
                return paymentDTOFactory.createDetails(payment, details);

            }

            default -> {
                return paymentDTOFactory.createDetails(payment, Collections.emptyList());
            }


        }

    }

    private void verifyOwnership(UserDetailsImpl userDetails, Loan loan){

        UUID userReference = userAdapter.userInfo(userDetails.getId()).userReference();

        if (!loan.getUserReference().equals(userReference)){

            throw new ResourceNotFoundException("The installment does not exist");

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
