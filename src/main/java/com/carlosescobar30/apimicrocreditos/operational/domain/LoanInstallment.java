package com.carlosescobar30.apimicrocreditos.operational.domain;

import com.carlosescobar30.apimicrocreditos.common.domain.EntityBaseClass;
import com.carlosescobar30.apimicrocreditos.operational.domain.domain_enums.ObligationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loan_installments", schema = "operational")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LoanInstallment extends EntityBaseClass {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(nullable = false)
    private Integer installmentNumber;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal principalAmount;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal interestAmount;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal paidArrearsAmount;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal accruedArrearsAmount;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private ObligationStatus status;

}
