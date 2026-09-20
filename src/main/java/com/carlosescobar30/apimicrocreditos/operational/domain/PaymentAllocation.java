package com.carlosescobar30.apimicrocreditos.operational.domain;

import com.carlosescobar30.apimicrocreditos.common.domain.EntityBaseClass;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.PaymentApplication;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "payment_allocations", schema = "operational")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PaymentAllocation extends EntityBaseClass {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_installment_id")
    private LoanInstallment loanInstallment;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(value = EnumType.STRING)
    private PaymentApplication appliedTo;

}
