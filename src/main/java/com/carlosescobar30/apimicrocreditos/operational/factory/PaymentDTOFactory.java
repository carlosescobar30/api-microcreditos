package com.carlosescobar30.apimicrocreditos.operational.factory;

import com.carlosescobar30.apimicrocreditos.operational.domain.Payment;
import com.carlosescobar30.apimicrocreditos.operational.dto.AllocationDetailsDTO;
import com.carlosescobar30.apimicrocreditos.operational.dto.PaymentInfoDTO;
import com.carlosescobar30.apimicrocreditos.operational.dto.PaymentInfoDetailsDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentDTOFactory {

    public PaymentInfoDTO create (Payment payment){

        return PaymentInfoDTO.builder()
                .paymentReference(payment.getPublicId())
                .loanReference(payment.getLoan().getPublicId())
                .amount(payment.getAmount())
                .method(payment.getFinancialMethod())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .build();

    }

    public PaymentInfoDetailsDTO createDetails (Payment payment, List<AllocationDetailsDTO> details){

        return PaymentInfoDetailsDTO.builder()
                .paymentReference(payment.getPublicId())
                .loanReference(payment.getLoan().getPublicId())
                .amount(payment.getAmount())
                .method(payment.getFinancialMethod())
                .status(payment.getStatus())
                .details(details)
                .createdAt(payment.getCreatedAt())
                .build();

    }

}
