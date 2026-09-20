package com.carlosescobar30.apimicrocreditos.operational.factory;

import com.carlosescobar30.apimicrocreditos.operational.domain.PaymentAllocation;
import com.carlosescobar30.apimicrocreditos.operational.dto.AllocationAmountsDTO;
import com.carlosescobar30.apimicrocreditos.operational.dto.AllocationDetailsDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PaymentAllocationDTOFactory {

    public AllocationDetailsDTO createAllocationDetailsDTO (PaymentAllocation allocation) {

        return AllocationDetailsDTO.builder()
                .loanInstallmentReference(allocation.getLoanInstallment().getPublicId())
                .amount(allocation.getAmount())
                .application(allocation.getAppliedTo())
                .createdAt(allocation.getCreatedAt())
                .build();

    }

    public AllocationAmountsDTO createAllocationAmounts (BigDecimal amountPaid, BigDecimal surplus) {

        return AllocationAmountsDTO.builder()
                .amountPaid(amountPaid)
                .surplus(surplus)
                .build();

    }


}
